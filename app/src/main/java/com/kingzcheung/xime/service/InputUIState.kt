package com.kingzcheung.xime.service

import com.kingzcheung.xime.R

import com.kingzcheung.xime.i18n.tr

import com.kingzcheung.xime.settings.SchemaInfo
import com.kingzcheung.xime.settings.SettingsPreferences
import com.kingzcheung.xime.keyboard.ToolbarButton
import com.kingzcheung.xime.viewmodel.SchemaSwitchUiState

data class InputUIState(
    val isAsciiMode: Boolean = false,
    val schemaName: String = "",
    val currentSchemaId: String = "",
    val schemas: List<SchemaInfo> = emptyList(),
    val schemaSwitches: List<SchemaSwitchUiState> = emptyList(),
    val enterKeyText: String = tr(R.string.s_fasong),
    val darkMode: Int = 0,
    val themeId: String = "ocean_blue",
    val keyboardHeightDp: Int = 0,
    val keyboardBottomPaddingDp: Int = 0,
    /** 键盘贴底显示：忽略底部系统 inset（隐藏导航栏/手势条时把键盘下移到屏幕底边）。 */
    val keyboardFlushBottom: Boolean = false,
    /** 键盘左边距（dp）：>0 时键盘左缘内收（宽度调节的落盘值，左右独立可偏移）。 */
    val keyboardMarginStartDp: Int = 0,
    /** 键盘右边距（dp）：>0 时键盘右缘内收。 */
    val keyboardMarginEndDp: Int = 0,
    val showKeyboardResize: Boolean = false,
    val resizePreviewHeightDp: Int = 0,
    /** 键盘调节进行中的左右边距预览值（确认后写入 keyboardMarginStart/EndDp）。 */
    val resizePreviewMarginStartDp: Int = 0,
    val resizePreviewMarginEndDp: Int = 0,
    val stretchFactor: Float = 1f,
    val isDeploying: Boolean = false,
    val deploymentMessage: String = "",
    val inputSessionId: Long = 0,
    /** 本次 inputSessionId 变化是否为 restarting（同一编辑框刷新）：键盘 UI 据此跳过 ascii 记忆 reset。 */
    val isInputSessionRestarting: Boolean = false,
    val t9ResetSignal: Long = 0,
    val swipeCancelEpoch: Long = 0,
    val t9RightCandidateSelectedCount: Long = 0,
    val t9SelectedCandidatePinyin: String = "",
    val toolbarButtons: List<String> = ToolbarButton.DEFAULT_VISIBLE.map { it.id },
    val isCompact: Boolean = false,
    val isFloatingMode: Boolean = false,
    val floatingOffsetX: Int = 0,
    val floatingOffsetY: Int = 0,
    val cursorX: Int = 0,
    val cursorY: Int = 0,
    val cursorVisible: Boolean = false,
    val showQuickSendForm: Boolean = false,
    val quickSendFormFocused: Boolean = false,
    /** 快捷发送表单内焦点是否在"触发编码"输入框（false=在文本输入框）；决定按键输入路由目标。 */
    val quickSendCodeFocused: Boolean = false,
    val quickSendEditingItemId: Long? = null,
    val quickSendEditingItemText: String = "",
    val quickSendEditingItemCode: String = "",
)
