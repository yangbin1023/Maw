package com.magic.maw.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.magic.maw.ui.main.MainScreen
import com.magic.maw.ui.main.MainViewModel

class MainActivity : ComponentActivity() {
    private val mainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            val classSize = calculateWindowSizeClass(this)
            MainScreen(mainViewModel, classSize)
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Preview
    @Composable
    fun MainScreenPreview() {
        val classSize = calculateWindowSizeClass(this)
        MainScreen(mainViewModel, classSize)
    }
}