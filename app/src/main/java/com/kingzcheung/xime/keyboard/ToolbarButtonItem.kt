package com.kingzcheung.xime.keyboard

/**
 * 工具栏按钮统一模型：候选栏顶部工具栏的渲染/分发单元。
 *
 * 展示链路：内置按钮枚举（[ToolbarButton]）→ 用户勾选
 * （toolbar_buttons 偏好 + ToolbarCustomizeView）→ 显示。
 */
sealed class ToolbarButtonItem {
    abstract val id: String
    abstract val label: String

    data class Builtin(val button: ToolbarButton) : ToolbarButtonItem() {
        override val id: String get() = button.id
        override val label: String get() = button.label
    }
}

/** 按 id 解析工具栏按钮；非内置 id（历史偏好残留）返回 null，自然不显示。 */
fun resolveToolbarButtonItem(id: String): ToolbarButtonItem.Builtin? =
    ToolbarButton.fromId(id)?.let { ToolbarButtonItem.Builtin(it) }
