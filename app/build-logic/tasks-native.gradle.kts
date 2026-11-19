import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import org.gradle.api.tasks.PathSensitivity

// GitHub 加速镜像（按顺序尝试；不可用时请替换/删除）
val ghMirrors = listOf(
    "https://ghfast.top",
    "https://gh-proxy.com",
    "https://ghproxy.net",
)

// 纯 Java（HttpURLConnection）实现下载，跨平台且不依赖外部 curl/powershell：
//  - 不依赖构建环境 PATH 中是否有 curl（F-Droid 构建服务器曾因找不到 curl 而构建失败）
//  - 天然规避 Windows 下 curl/schannel TLS 握手失败（SSL error 35）问题
// 单个 URL 下载，支持断点续传与自动重试
fun downloadFile(url: String, target: File, workDir: File, desc: String): Boolean {
    println("Downloading $desc: $url")
    repeat(5) { attempt ->
        try {
            if (downloadOnce(url, target, desc)) {
                if (target.exists() && target.length() == 0L) {
                    target.delete()
                    return false
                }
                println("Downloaded ${target.name} (${target.length()} bytes)")
                return true
            }
        } catch (e: Exception) {
            System.err.println("Download error for $desc: ${e.message}")
        }
        // 失败后清空残留半成品，避免续传污染下一源
        target.delete()
        if (attempt < 4) Thread.sleep(3000)
    }
    return false
}

// 单次下载；返回 true 表示 HTTP 成功且内容已写入，false 表示 HTTP 层失败（如 404/403）
fun downloadOnce(url: String, target: File, desc: String): Boolean {
    val resumeFrom = if (target.exists()) target.length() else 0L
    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    try {
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 30_000
        conn.readTimeout = 600_000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64)")
        if (resumeFrom > 0) {
            conn.setRequestProperty("Range", "bytes=$resumeFrom-")
        }
        val code = conn.responseCode
        if (code !in 200..299 && code != 206) {
            System.err.println("Download failed for $desc (HTTP $code)")
            return false
        }
        // 服务端支持 Range（206）才追加续传；否则（200）从头覆盖
        val append = code == 206 && resumeFrom > 0
        target.parentFile.mkdirs()
        conn.inputStream.use { input ->
            java.io.FileOutputStream(target, append).use { out ->
                val buf = ByteArray(64 * 1024)
                var n: Int
                while (input.read(buf).also { n = it } != -1) {
                    out.write(buf, 0, n)
                }
            }
        }
        return true
    } finally {
        conn.disconnect()
    }
}

// 按顺序尝试多个 URL（镜像优先，最后官方源），成功即停止，失败自动切换到下一个
fun downloadWithMirrors(urls: List<String>, target: File, workDir: File, desc: String): Boolean {
    for (url in urls) {
        // 已完整下载则跳过
        if (target.exists() && target.length() > 0) return true
        if (downloadFile(url, target, workDir, desc)) return true
        // 失败后清空残留半成品，避免续传污染下一源
        target.delete()
    }
    return false
}

fun githubUrls(url: String): List<String> = buildList {
    ghMirrors.forEach { add("${it.trimEnd('/')}/$url") }
    add(url) // 官方源兜底
}

val buildTrie by tasks.registering {
    val inputFile = file("src/main/assets/english.txt")
    val outputFile = file("src/main/assets/english_trie.bin")

    inputs.file(inputFile)
    outputs.file(outputFile)

    doLast {
        val words = inputFile.readLines()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        println("Loaded ${words.size} words from ${inputFile.name}")

        val nodes = mutableListOf<MutableMap<Char, Int>>()
        val nodeWords = mutableListOf<String?>()
        val nodeFreqs = mutableListOf<Int>()
        nodes.add(mutableMapOf())
        nodeWords.add(null)
        nodeFreqs.add(0)

        fun getOrCreateChild(parentIndex: Int, char: Char): Int {
            val existing = nodes[parentIndex][char]
            if (existing != null) return existing

            val newIndex = nodes.size
            nodes.add(mutableMapOf())
            nodeWords.add(null)
            nodeFreqs.add(0)
            nodes[parentIndex][char] = newIndex
            return newIndex
        }

        words.forEachIndexed { lineNum, word ->
            var current = 0
            for (char in word) {
                current = getOrCreateChild(current, char)
            }
            if (nodeWords[current] == null) {
                nodeWords[current] = word
                nodeFreqs[current] = lineNum + 1
            }
        }

        println("Built trie with ${nodes.size} nodes")

        val buffer = java.nio.ByteBuffer.allocate(512 * 1024)
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)

        buffer.put("TRIE".toByteArray())
        buffer.put(1)
        buffer.putInt(nodes.size)

        for (i in nodes.indices) {
            val children = nodes[i]
            buffer.put(children.size.toByte())
            for ((char, childIndex) in children) {
                buffer.put(char.code.toByte())
                buffer.putInt(childIndex)
            }

            val word = nodeWords[i]
            buffer.put(if (word != null) 1 else 0)
            if (word != null) {
                val bytes = word.toByteArray(Charsets.UTF_8)
                buffer.put(bytes.size.toByte())
                buffer.put(bytes)
                buffer.putInt(nodeFreqs[i])
            }
        }

        val data = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(data)
        outputFile.writeBytes(data)

        println("Wrote ${data.size} bytes (${data.size / 1024}KB) to ${outputFile.name}")
    }
}

// 1) librime-t9 权威源码在顶层 jni/librime-t9；构建期把它符号链接为
//    plugins/librime-t9（物理单份），既让 librime 插件构建能找到它，
//    又避免拷贝副本被改错/被静默覆盖。Rime.cmake 在 configure 时也会
//    幂等创建该链接，这里额外保障（如链接被误删时）。
val syncT9Plugin by tasks.registering {
    val authoritative = file("src/main/jni/librime-t9")
    val mirror = file("src/main/jni/librime/plugins/librime-t9")
    val expectedTarget = Paths.get("../../librime-t9")
    inputs.dir(authoritative)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .optional(true)
    doLast {
        val link = mirror.toPath()
        if (Files.isSymbolicLink(link) && Files.readSymbolicLink(link) == expectedTarget) {
            return@doLast
        }
        if (Files.isSymbolicLink(link)) {
            Files.delete(link)
        } else if (mirror.exists()) {
            mirror.deleteRecursively()
        }
        Files.createSymbolicLink(link, expectedTarget)
    }
}

// 2) 把 librime 核心/插件源码目录声明为 native 构建任务的显式输入：
//    AGP 对 CMake 任务的 up-to-date 判断不覆盖 submodule 源文件，
//    缺失该输入时 Android Studio 的 Run 会直接跳过 ninja，
//    改动（尤其 librime/src 核心改动）不会进 APK。
//    声明后：源码一变任务必然执行，ninja 按依赖增量重编，仍然很快。
tasks.configureEach {
    if (name.startsWith("buildCMake")) {
        dependsOn(syncT9Plugin)
        inputs.dir("src/main/jni/librime/src")
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .optional(true)
        inputs.dir("src/main/jni/librime-t9")
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .optional(true)
    }
}

tasks.named("preBuild").configure {
    dependsOn(buildTrie)
}