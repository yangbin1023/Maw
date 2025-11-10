package com.magic.maw.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 自定义的 HapticFeedback 实现，用于禁用触感反馈
 */
object DisabledHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        // 留空，不执行任何震动操作
    }
}

@Composable
fun DisableHapticLocalProvider(content: @Composable (() -> Unit)) {
    CompositionLocalProvider(LocalHapticFeedback provides DisabledHapticFeedback) {
        content()
    }
}
