package com.magic.maw.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.magic.maw.R

@SuppressLint("InflateParams")
@Composable
fun <T : ScrollableState> RefreshView(
    modifier: Modifier = Modifier,
    lazyState: T,
    refreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    context: @Composable (T) -> Unit
) {
    AndroidView(factory = {
        LayoutInflater.from(it).inflate(R.layout.layout_refresh, null).apply {
            val refreshLayout = findViewById<SwipeRefreshLayout>(R.id.refresh_layout)?.apply {
                setOnRefreshListener { onRefresh() }
                isRefreshing = refreshing
            }
            findViewById<ComposeView>(R.id.compose_view)?.setContent {
                refreshLayout?.isEnabled =
                    refreshLayout?.isRefreshing == true or lazyState.canScrollBackward.not()
                context.invoke(lazyState)
            }
        }
    }, modifier = modifier) { root ->
        root.findViewById<SwipeRefreshLayout>(R.id.refresh_layout)?.apply {
            if (isRefreshing && !refreshing && lazyState.canScrollBackward) {
                // 等待刷新完成动画结束后再禁用刷新
                postDelayed({
                    isEnabled = false
                }, 200)
            } else {
                isEnabled = refreshing or lazyState.canScrollBackward.not()
            }
            isRefreshing = refreshing
        }
    }
}
