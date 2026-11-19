package com.kingzcheung.xime.ui.keyboard

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kingzcheung.xime.keyboard.ToolbarButtonItem

/** 工具栏按钮图标渲染：内置按钮用 ImageVector 图标。 */
@Composable
fun ToolbarButtonIcon(
    item: ToolbarButtonItem,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    when (item) {
        is ToolbarButtonItem.Builtin -> Icon(
            imageVector = item.button.icon,
            contentDescription = contentDescription ?: item.label,
            tint = tint,
            modifier = modifier,
        )
    }
}
