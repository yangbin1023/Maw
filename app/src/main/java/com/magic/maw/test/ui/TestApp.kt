package com.magic.maw.test.ui

import androidx.compose.runtime.Composable
import com.magic.maw.ui.theme.MawTheme

@Composable
fun TestApp() {
    MawTheme {
        TestNavHost()
    }
}