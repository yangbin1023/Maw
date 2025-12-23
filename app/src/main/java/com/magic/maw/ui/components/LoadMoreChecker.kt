package com.magic.maw.ui.components

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.magic.maw.data.loader.ListUiState
import kotlinx.coroutines.flow.distinctUntilChanged


/**
 * 用于检测是否需要加载更多
 */
@Composable
fun LoadMoreChecker(
    uiState: ListUiState<*>,
    state: LazyStaggeredGridState,
    onLoadMore: () -> Unit,
) {
    // 检测加载进度，实现自动加载更多
    LaunchedEffect(uiState, state) {
        snapshotFlow {
            state.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.distinctUntilChanged().collect { lastVisibleIndex ->
            if (uiState.hasNoMore || uiState.isLoading || lastVisibleIndex == null) {
                return@collect
            }
            val totalItemsCount = state.layoutInfo.totalItemsCount
            val remainingItemsCount = totalItemsCount - (lastVisibleIndex + 1)
            val visibleItemsCount = state.layoutInfo.visibleItemsInfo.size
            if (remainingItemsCount < visibleItemsCount * 2) {
                onLoadMore()
            }
        }
    }
}