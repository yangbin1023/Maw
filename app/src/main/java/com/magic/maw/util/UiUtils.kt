package com.magic.maw.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.util.Size
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

object UiUtils {
    fun dp2px(context: Context, value: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (value * scale + 0.5f).toInt()
    }

    fun px2dp(context: Context, value: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (value / scale + 0.5f).toInt()
    }

    fun Int.pxToDp(context: Context): Dp {
        val scale = context.resources.displayMetrics.density
        return (this / scale + 0.5f).toInt().dp
    }

    fun Context.isLandscape(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun Context.isPortrait(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    fun Context.getAppScreenSize(): Size {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val currentWindowMetrics = wm.currentWindowMetrics
            val width = currentWindowMetrics.bounds.width()
            val height = currentWindowMetrics.bounds.height()
            return Size(width, height)
        } else {
            val point = Point()
            wm.defaultDisplay.getSize(point)
            return Size(point.x, point.y)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun showInCutout(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
    }

    fun Activity.fixStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            view.setBackgroundColor(Color.TRANSPARENT)
            val mask = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            val systemWindowInsets = insets.getInsets(mask)
            view.updatePadding(
                left = systemWindowInsets.left,
                top = systemWindowInsets.top,
                right = systemWindowInsets.right,
            )
            insets
        }
        showInCutout(window)
    }

    fun Context.startActivity(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
    }

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
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun Context.showSystemBars() {
        val window = findActivity()?.window ?: return
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        insetsController.apply {
            show(WindowInsetsCompat.Type.statusBars())
            show(WindowInsetsCompat.Type.navigationBars())
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
        return ViewCompat.getRootWindowInsets(window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.statusBars()) == true
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun getTopBarHeight(): Dp {
        val height = LocalContext.current.getStatusBarHeight()
        val heightDp = with(LocalDensity.current) { (if (height > 0) height else 24).toDp() }
        return heightDp + TopAppBarDefaults.TopAppBarExpandedHeight
    }

    val NavController.currentRoute: String?
        @Composable get() = currentBackStackEntryAsState().value?.destination?.route
}