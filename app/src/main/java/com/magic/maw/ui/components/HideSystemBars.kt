package com.magic.maw.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.magic.maw.util.UiUtils.hideSystemBars
import com.magic.maw.util.UiUtils.isShowStatusBars
import com.magic.maw.util.UiUtils.showSystemBars

@Composable
fun HideSystemBars() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val isShowStatusBar = context.isShowStatusBars()
        if (isShowStatusBar) {
            context.hideSystemBars()
        }
        onDispose {
            if (isShowStatusBar) {
                context.showSystemBars()
            }
        }
    }
}

@Composable
fun ShowSystemBars() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val isShowStatusBar = context.isShowStatusBars()
        if (!isShowStatusBar) {
            context.showSystemBars()
        }
        onDispose {
            if (!isShowStatusBar) {
                context.hideSystemBars()
            }
        }
    }
}

@Composable
fun RememberSystemBars() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val isShow = context.isShowStatusBars()
        onDispose {
            context.showSystemBars(isShow)
        }
    }
}