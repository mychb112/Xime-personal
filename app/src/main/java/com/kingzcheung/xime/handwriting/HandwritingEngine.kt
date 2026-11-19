package com.kingzcheung.xime.handwriting

import android.content.Context

data class HandwritingCandidate(
    val char: String,
    val score: Float,
)

/**
 * 手写识别入口。
 *
 * 1.0 起不再内置手写识别：手写依赖 ONNX 推理服务与外部模型包，属于已移除的模型功能，
 * 这里保留同名空实现，让上层界面（手写键盘、手写找字）走「无模型」分支而不是崩溃。
 */
object HandwritingEngine {

    fun initialize(context: Context): Boolean = false

    fun isInitialized(): Boolean = false

    /** 手写模型是否可用：始终 false（不再支持手写识别）。 */
    fun hasModel(context: Context): Boolean = false

    fun predict(
        strokes: List<List<Pair<Float, Float>>>,
        topK: Int = 10,
    ): List<HandwritingCandidate> = emptyList()

    fun release() = Unit
}
