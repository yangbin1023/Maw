package com.magic.maw.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import co.touchlab.kermit.Logger

object UiUtils {
    fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

    fun Context.hideSystemBars() {
        val window = findActivity()?.window ?: return
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        insetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun Context.showSystemBars() {
        val window = findActivity()?.window ?: return
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        insetsController.apply {
            show(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    fun Context.showSystemBars(toShow: Boolean) {
        val isShow = isShowStatusBars()
        if (toShow && !isShow) {
            showSystemBars()
        } else if (!toShow && isShow) {
            hideSystemBars()
        }
    }

    fun Context.isShowStatusBars(): Boolean {
        val window = findActivity()?.window ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsets = window.decorView.rootWindowInsets
            return windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top > 0
        } else {
            return window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0
        }
    }

    fun Context.getStatusBarHeight(): Int {
        findActivity()?.window?.let { window ->
            ViewCompat.getRootWindowInsets(window.decorView)?.let { insets ->
                // 这里使用getInsetsIgnoringVisibility而不用getInsets的原因是
                // 若当前隐藏系统栏，使用getInsets获取到的top为0，
                // 使用getInsetsIgnoringVisibility则不受系统栏状态的影响
                return insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars()).top
            }
        }
        return 0
    }

    @Composable
    fun shouldShowSystemBar(): Boolean {
        val height = with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height.toDp()
        }
        return height > 600.dp
    }

    @Composable
    fun updateSystemBarStatus(showBar: Boolean? = null) {
        val showBar = shouldShowSystemBar() && showBar != false
        LocalContext.current.showSystemBars(showBar)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun getTopBarHeight(): Dp {
        val height = LocalContext.current.getStatusBarHeight()
        val heightDp = with(LocalDensity.current) { (if (height > 0) height else 24).toDp() }
        return heightDp + TopAppBarDefaults.TopAppBarExpandedHeight
    }

    @Composable
    fun getTopBarHeight2(): Dp {
        if (!shouldShowSystemBar()) {
            return TopAppBarDefaults.TopAppBarExpandedHeight
        }
        val height = LocalContext.current.getStatusBarHeight()
        val heightDp = with(LocalDensity.current) { (if (height > 0) height else 24).toDp() }
        return heightDp + TopAppBarDefaults.TopAppBarExpandedHeight
    }

    val NavController.currentRoute: String?
        @Composable get() = currentBackStackEntryAsState().value?.destination?.route

    @SuppressLint("RestrictedApi")
    fun NavHostController.checkTopRoute(route: String): Boolean {
        try {
            val list = currentBackStack.value
            list.lastOrNull()?.let {
                return it.destination.route == route
            }
        } catch (e: Exception) {
            Logger.e(e) { "checkTopRoute failed. ${e.message}" }
        }
        return false
    }

    private var _topAppBarColors: TopAppBarColors? = null
    val topAppBarColors: TopAppBarColors
        @Composable get() {
            val colorScheme = MaterialTheme.colorScheme
            return TopAppBarColors(
                containerColor = colorScheme.surface,
                scrolledContainerColor = colorScheme.surface,
                navigationIconContentColor = colorScheme.onSurface,
                titleContentColor = colorScheme.onSurface,
                actionIconContentColor = colorScheme.onSurfaceVariant,
                subtitleContentColor = colorScheme.onSurfaceVariant,
            )
        }
}