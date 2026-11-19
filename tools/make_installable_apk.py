#!/usr/bin/env python3
"""把 AGP 注入构建产出的 testOnly APK 转成可用文件管理器正常安装的 APK。

背景：用 `-Pandroid.injected.build.abi=xxx` 构建时，AGP 会自动给清单写入
`android:testOnly="true"`。这种包只能用 `adb install -t` 安装，用文件管理器
点开安装会失败（INSTALL_FAILED_TEST_ONLY）。

本脚本做三件事：
  1. 直接改写 APK 内 AndroidManifest.xml 的 testOnly 属性为 false（二进制 AXML 原地改值）；
  2. 按原压缩方式重打包（resources.arsc 必须保持 Stored，否则 API 30+ 安装失败）；
  3. zipalign 4 字节对齐后用 apksigner 重新签名。

用法：
  python tools/make_installable_apk.py \
      --in app/build/intermediates/apk/release/Xime-x.y.z-arm64-v8a.apk \
      --out out/Xime-x.y.z-arm64-v8a.apk \
      --ks S:/Outputs/xime-cmg/debug.jks --ks-pass android --key-pass android

依赖：Android SDK 的 zipalign / apksigner，默认从 ANDROID_HOME 或
      S:/DevEnv/android-sdk 查找。
"""

from __future__ import annotations

import argparse
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import zipfile

TEST_ONLY_ATTR_ID = 0x01010272
ATTR_TYPE_INT_BOOLEAN = 0x12
SKIP_SIGN_FILES = (
    "META-INF/MANIFEST.MF",
    "META-INF/CERT.SF",
    "META-INF/CERT.RSA",
    "META-INF/ANDROIDD.SF",
    "META-INF/ANDROIDD.RSA",
)


def parse_string_pool(data: bytes, off: int) -> list[str]:
    ctype, hsize, _csize = struct.unpack_from("<HHI", data, off)
    if ctype != 0x0001:
        raise ValueError("string pool chunk expected")
    count, _styles, flags, strings_start, _ss = struct.unpack_from("<IIIII", data, off + 8)
    utf8 = bool(flags & 0x100)
    offsets = struct.unpack_from("<%dI" % count, data, off + hsize)
    base = off + strings_start
    out: list[str] = []
    for rel in offsets:
        p = base + rel
        if utf8:
            n = data[p]
            p += 1
            if n & 0x80:
                p += 1
            n2 = data[p]
            p += 1
            if n2 & 0x80:
                n2 = ((n2 & 0x7F) << 8) | data[p]
                p += 1
            out.append(data[p : p + n2].decode("utf-8", "replace"))
        else:
            n = struct.unpack_from("<H", data, p)[0]
            p += 2
            if n & 0x8000:
                n = ((n & 0x7FFF) << 16) | struct.unpack_from("<H", data, p)[0]
                p += 2
            out.append(data[p : p + n * 2].decode("utf-16-le", "replace"))
    return out


def patch_test_only(manifest: bytes) -> bytes:
    """把 AndroidManifest 里的 testOnly 改成 false；返回新的字节串。"""
    _ftype, fhsize, fsize = struct.unpack_from("<HHI", manifest, 0)
    off = fhsize
    strings: list[str] | None = None
    target_offsets: list[int] = []
    while off < fsize:
        ctype, _hsize, csize = struct.unpack_from("<HHI", manifest, off)
        if ctype == 0x0001:
            strings = parse_string_pool(manifest, off)
        elif ctype == 0x0102:  # START_ELEMENT
            attr_start, attr_size, attr_count = struct.unpack_from("<HHH", manifest, off + 24)
            aoff = off + 16 + attr_start
            for k in range(attr_count):
                p = aoff + k * attr_size
                # 属性记录：ns(4) name(4) rawValue(4) size(2) res0(1) dataType(1) data(4)
                _a_ns, a_name, a_raw = struct.unpack_from("<III", manifest, p)
                dtype = manifest[p + 15]
                if not strings or a_name >= len(strings):
                    continue
                if strings[a_name] == "testOnly":
                    if dtype == ATTR_TYPE_INT_BOOLEAN:
                        target_offsets.append(p + 16)
                    elif a_raw == 0xFFFFFFFF:
                        target_offsets.append(p + 12)  # 退化情况：改 rawValue
        off += csize

    if not target_offsets:
        raise SystemExit("没有找到 testOnly 属性，可能这个包本来就是可安装的")

    buf = bytearray(manifest)
    for p in target_offsets:
        struct.pack_into("<I", buf, p, 0)
    return bytes(buf)


def repack(src: str, dst: str, manifest: bytes) -> None:
    zin = zipfile.ZipFile(src, "r")
    zout = zipfile.ZipFile(dst, "w")
    dropped = []
    for item in zin.infolist():
        if item.filename in SKIP_SIGN_FILES:
            dropped.append(item.filename)
            continue
        data = manifest if item.filename == "AndroidManifest.xml" else zin.read(item.filename)
        info = zipfile.ZipInfo(item.filename, date_time=item.date_time)
        info.compress_type = item.compress_type  # resources.arsc 必须保持 Stored
        info.external_attr = item.external_attr
        if item.compress_type == zipfile.ZIP_DEFLATED:
            zout.writestr(info, data, compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)
        else:
            zout.writestr(info, data, compress_type=zipfile.ZIP_STORED)
    zout.close()
    zin.close()
    if dropped:
        print("  已移除旧签名文件:", ", ".join(dropped))


def find_build_tool(name: str) -> str:
    roots = [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT"), r"S:\DevEnv\android-sdk"]
    exe = name + (".exe" if os.name == "nt" else "")
    bat = name + ".bat"
    for root in filter(None, roots):
        bt = os.path.join(root, "build-tools")
        if not os.path.isdir(bt):
            continue
        for ver in sorted(os.listdir(bt), reverse=True):
            for cand in (os.path.join(bt, ver, exe), os.path.join(bt, ver, bat)):
                if os.path.isfile(cand):
                    return cand
    raise SystemExit("找不到 %s，请设置 ANDROID_HOME" % name)


def run(cmd: list[str]) -> None:
    print("  $", " ".join(cmd))
    subprocess.run(cmd, check=True)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--in", dest="src", required=True)
    ap.add_argument("--out", dest="dst", required=True)
    ap.add_argument("--ks")
    ap.add_argument("--ks-pass", default="android")
    ap.add_argument("--key-pass", default="android")
    args = ap.parse_args()

    with zipfile.ZipFile(args.src) as z:
        manifest = z.read("AndroidManifest.xml")
    print("改写 testOnly 标记 ...")
    fixed = patch_test_only(manifest)

    tmpdir = tempfile.mkdtemp(prefix="xime-apk-")
    try:
        unsigned = os.path.join(tmpdir, "unsigned.apk")
        aligned = os.path.join(tmpdir, "aligned.apk")
        print("重打包 ...")
        repack(args.src, unsigned, fixed)
        print("对齐 ...")
        run([find_build_tool("zipalign"), "-f", "-p", "4", unsigned, aligned])
        os.makedirs(os.path.dirname(os.path.abspath(args.dst)) or ".", exist_ok=True)
        if args.ks:
            print("签名 ...")
            run([
                find_build_tool("apksigner"), "sign",
                "--ks", args.ks, "--ks-pass", "pass:" + args.ks_pass,
                "--key-pass", "pass:" + args.key_pass,
                "--out", args.dst, aligned,
            ])
        else:
            shutil.copyfile(aligned, args.dst)
            print("未提供 --ks，输出的是未签名包")
    finally:
        shutil.rmtree(tmpdir, ignore_errors=True)

    print("完成:", args.dst, os.path.getsize(args.dst), "bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
