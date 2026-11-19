package com.kingzcheung.xime.service

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * 键盘视图容器。
 *
 * 只负责承载 Compose 视图、按 dp 调整高度，并在系统截走触摸流（如三指截图）时
 * 把 cancel 上抛，让 Composable 侧取消进行中的手势。
 */
class KeyboardContainer(
    context: Context,
    private val onTouchCancel: () -> Unit = {},
) : FrameLayout(context) {

    fun updateHeight(heightDp: Int) {
        val heightPx = (heightDp * resources.displayMetrics.density).toInt()
        val params = layoutParams ?: return
        if (params.height != heightPx) {
            params.height = heightPx
            layoutParams = params
            requestLayout()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_CANCEL) {
            onTouchCancel()
        }
        return super.dispatchTouchEvent(ev)
    }
}
