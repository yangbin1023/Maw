package com.magic.maw.util

import androidx.compose.foundation.gestures.Orientation

enum class ComposePosition(private val orientation: Orientation) {
    Start(Orientation.Horizontal),
    End(Orientation.Horizontal),
    Top(Orientation.Vertical),
    Bottom(Orientation.Vertical);

    fun isHorizontal() = orientation == Orientation.Horizontal
}