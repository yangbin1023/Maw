package com.magic.maw.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import co.touchlab.kermit.Logger
import com.magic.maw.data.SettingsService
import com.magic.maw.data.ThemeMode
import com.magic.maw.util.UiUtils
import com.magic.maw.util.UiUtils.findActivity

private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()
val supportDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun MawTheme(content: @Composable () -> Unit) {
    val settingState by SettingsService.settingsState.collectAsState()
    val themeMode by remember { derivedStateOf { settingState.themeSettings.themeMode } }
    val dynamicColor by remember { derivedStateOf { settingState.themeSettings.dynamicColor } }

    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }

    val colorScheme = when {
        dynamicColor && supportDynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        view.context.findActivity()?.window?.let { window ->
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }
    UiUtils.updateSystemBarStatus()

    Logger.d("MawTAG") { "MawTheme compose1" }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun PreviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) darkColorScheme() else lightColorScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}