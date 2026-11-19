package com.kingzcheung.xime.ui.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.keyboard.KeyboardPage
import com.kingzcheung.xime.keyboard.MainType
import com.kingzcheung.xime.settings.KeysConfigHelper
import com.kingzcheung.xime.ui.isTablet
import com.kingzcheung.xime.viewmodel.KeyboardUiState

/**
 * 候选栏下方、最上一排字母键上方的阿拉伯数字行（`1234567890`）。
 *
 * 位置固定在键盘内容区顶部，因此对全键盘（26 键 / 合并键 14·17·18 键）、
 * 九键、笔画三类主键盘同时生效；数字面板、常用符号面板与符号/表情 overlay
 * 自身就是符号输入界面，不叠加这一行（见 [shouldShowNumberRow]）。
 *
 * 按键行为与物理键盘数字键完全一致：中文组合态下由 Rime 的 key_binder 决定
 * （有候选时选词），空闲态直接上屏数字——不在 UI 层自行判分支，避免与引擎
 * 的候选选择语义打架。
 */
internal val NUMBER_ROW_DIGITS: List<String> = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

/**
 * 该行高度（dp）。比字母行矮，避免把主键区压得太扁；
 * 横屏键盘整体紧凑，平板键面更大，各自取一档。
 */
@Composable
internal fun numberRowHeight(): Dp {
    val landscape = LocalConfiguration.current.let { it.screenWidthDp > it.screenHeightDp }
    return numberRowHeightDp(isTablet = isTablet(), landscape = landscape).dp
}

/** 与 [numberRowHeight] 同源的纯函数版本：供服务层计算容器高度时使用（无 Composable 上下文）。 */
internal fun numberRowHeightDp(isTablet: Boolean, landscape: Boolean): Int = when {
    isTablet -> 46
    landscape -> 30
    else -> 36
}

/**
 * 是否显示数字行。
 *
 * 只在主键盘（全键盘 / 九键 / 笔画）显示；手写页没有按键行，数字/符号面板
 * 本身就是符号界面，均不显示。服务层算容器高度时调用同一函数，保证
 * UI 与 IME insets 口径一致（不一致会多/少一截空白）。
 */
internal fun shouldShowNumberRow(page: KeyboardPage?, layoutState: KeyboardLayoutState?): Boolean {
    if (page !is KeyboardPage.Main) return false
    if (page.type == MainType.HANDWRITING) return false
    return when (layoutState) {
        is KeyboardLayoutState.Chinese,
        is KeyboardLayoutState.English,
        is KeyboardLayoutState.T9Pinyin,
        is KeyboardLayoutState.Stroke -> true
        else -> false
    }
}

/**
 * 数字行本体。配色与圆角全部走主键盘同一套解析（[resolveKeyboardPalette] /
 * [keyCornerRadiusFor]），因此换任何主题都会跟着变，不需要为该行单独配色。
 */
@Composable
internal fun NumberKeyRow(
    uiState: KeyboardUiState,
    onKeyPress: (String) -> Unit,
    onKeyPressDown: ((String) -> Unit)?,
    onKeyRelease: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val tablet = isTablet()
    val palette = resolveKeyboardPalette(uiState.themeId, uiState.isDarkTheme)
    val kbKey = KeysConfigHelper.getKeyboardKeyConfig()
    val shadow = KeysConfigHelper.getKeyboardShadow()
    val radius = keyCornerRadiusFor(kbKey, "qwerty", tablet)
    val spacing = kbKey.spacingFor("qwerty").first?.dp ?: if (tablet) 4.dp else 2.dp
    val sidePadding = if (tablet) 16.dp else 4.dp

    CompositionLocalProvider(
        LocalKeyCornerRadius provides radius,
        LocalHintExtraScale provides hintExtraScaleFor(tablet),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(numberRowHeight())
                .padding(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            NUMBER_ROW_DIGITS.forEach { digit ->
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    SwipeableKeyButton(
                        text = digit,
                        onClick = { onKeyPress(digit) },
                        backgroundColor = palette.keyBackgroundColor,
                        textColor = palette.keyTextColor,
                        modifier = Modifier.fillMaxSize(),
                        onPress = { onKeyPressDown?.invoke(digit) },
                        onRelease = { onKeyRelease?.invoke(digit) },
                        shadowEnabled = shadow.enabled,
                        shadowElevation = shadow.elevation.dp,
                        shadowShapeRadius = shadow.shapeRadius.dp,
                    )
                }
            }
        }
    }
}
