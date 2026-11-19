package com.kingzcheung.xime.ui.keyboard

import androidx.compose.runtime.mutableStateOf

/**
 * 14 键双拼的小字韵母助记显示状态。
 *
 * 进程内全局：键盘服务长期存活，切换后所有键帽同步重组；进程重启恢复默认显示。
 */
object MnemonicHintState {
    val visible = mutableStateOf(true)
}
