package com.kingzcheung.xime.service

/**
 * 单个显示候选的上屏动作（与 CandidateState.candidates 平行）。
 *
 * [engineIndex] >= 0：引擎候选引用，点击走引擎选词。
 *
 * 空列表 = 纯引擎语义（显示 index 即引擎 index），所有既有路径零影响。
 */
data class CandidateAction(
    val engineIndex: Int,
    val commitText: String,
) {
    companion object {
        fun engine(index: Int) = CandidateAction(engineIndex = index, commitText = "")
    }
}
