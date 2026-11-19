package com.kingzcheung.xime.ui.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.settings.KeyBinding
import com.kingzcheung.xime.settings.KeyColor
import com.kingzcheung.xime.settings.KeysConfigHelper

/**
 * 当前主题下可用于按键级样式的颜色。
 * 在 KeyboardLayoutScreen 统一解析一次，避免每个键重复算主题色。
 */
internal data class KeyStyleColors(
    val accent: Color = Color.Unspecified,
    val special: Color = Color.Unspecified,
    val key: Color = Color.Unspecified,
    val keyboard: Color = Color.Unspecified,
    /** 主题普通键文字色（空格键等使用），供 `background: key` 的按键级样式引用。 */
    val keyText: Color = Color.Unspecified,
)

internal val LocalKeyStyleColors = staticCompositionLocalOf { KeyStyleColors() }

/**
 * 单个按键的外观覆盖（`keyboard.<section>.keys.<id>` 的 corner_radius / background）。
 * 全部为 null 表示未配置，渲染完全沿用键盘级/主题默认。
 */
internal data class KeyStyleOverride(
    val cornerRadius: Dp? = null,
    val background: Color? = null,
    val textColor: Color? = null,
) {
    val isEmpty: Boolean get() = cornerRadius == null && background == null
}

@Composable
internal fun keyStyleOverride(
    section: String,
    id: String,
    isAsciiMode: Boolean,
    isDarkTheme: Boolean,
): KeyStyleOverride =
    KeysConfigHelper.getGestureFor(section, id, isAsciiMode)
        ?.toKeyStyle(isDarkTheme, LocalKeyStyleColors.current)
        ?: KeyStyleOverride()

internal fun KeyBinding.toKeyStyle(isDarkTheme: Boolean, colors: KeyStyleColors): KeyStyleOverride {
    val spec = if (isDarkTheme) backgroundDark ?: background else background
    val bg = spec?.resolve(colors)
    // background: key 表示"与空格等普通键同色"，文字色也应沿用主题的普通键文字色，
    // 而不是按背景亮度重算对比色（否则会与空格键文字色不一致）。其他背景仍用对比色保证可读性。
    val textColor = if (spec is KeyColor.Token && spec.name == "key" &&
        colors.keyText != Color.Unspecified
    ) {
        colors.keyText
    } else {
        bg?.let(::contrastTextColor)
    }
    return KeyStyleOverride(
        cornerRadius = cornerRadius?.takeIf { it >= 0 }?.dp,
        background = bg,
        textColor = textColor,
    )
}

private fun KeyColor.resolve(colors: KeyStyleColors): Color? = when (this) {
    is KeyColor.Hex -> keyColor(argb)
    is KeyColor.Token -> when (name) {
        "accent", "primary" -> colors.accent.takeIf { it != Color.Unspecified }
        "special" -> colors.special.takeIf { it != Color.Unspecified }
        "key" -> colors.key.takeIf { it != Color.Unspecified }
        "keyboard" -> colors.keyboard.takeIf { it != Color.Unspecified }
        else -> null
    }
}

/** 0xRRGGBB 补足不透明通道；0xAARRGGBB 原样使用。 */
private fun keyColor(hex: Long): Color =
    if (hex > 0xFFFFFF) Color(hex) else Color(0xFF000000 or hex)

/** 按键背景改色后按亮度选前景，避免深底深字/浅底浅字。 */
private fun contrastTextColor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF202124) else Color(0xFFE8EAED)

/** 把按键级背景/前景覆盖应用到共享渲染环境；圆角由 CompositionLocal 单独提供。 */
internal fun QwertyRowEnv.withKeyStyle(style: KeyStyleOverride): QwertyRowEnv {
    if (style.isEmpty) return this
    return copy(
        keyBackgroundColor = style.background ?: keyBackgroundColor,
        specialKeyBackgroundColor = style.background ?: specialKeyBackgroundColor,
        keyTextColor = style.textColor ?: keyTextColor,
        specialKeyTextColor = style.textColor ?: specialKeyTextColor,
        // 阴影与键面用同一圆角，避免按键级圆角和默认阴影半径不一致时出现「双层不同圆角」
        shadowShapeRadius = style.cornerRadius ?: shadowShapeRadius,
    )
}
