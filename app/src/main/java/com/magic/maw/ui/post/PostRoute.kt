package com.magic.maw.ui.post

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val TAG = "PostRoute"

@Composable
fun PostRoute(
    postViewModel: PostViewModel,
    openDrawer: () -> Unit,
    openSearch: (String) -> Unit,
) {
    val uiState by postViewModel.uiState.collectAsStateWithLifecycle()

    PostRoute(
        uiState = uiState,
        openDrawer = openDrawer,
        openSearch = openSearch,
        onRefresh = { postViewModel.refresh() },
        onLoadMore = { postViewModel.loadMore() },
        onItemClick = { postViewModel.setViewIndex(it) },
        onExitView = { postViewModel.exitView() },
        onSearch = { postViewModel.search(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRoute(
    uiState: PostUiState,
    openDrawer: () -> Unit,
    openSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (Int) -> Unit,
    onExitView: () -> Unit,
    onSearch: (String) -> Unit,
) {
    val fadeIn = fadeIn(animationSpec = tween(500))
    val fadeOut = fadeOut(animationSpec = tween(500))

    val lazyState = rememberLazyStaggeredGridState()
    val refreshState = rememberPullToRefreshState()
    AnimatedVisibility(
        visible = uiState is PostUiState.Post,
        enter = fadeIn,
        exit = fadeOut
    ) {
        (uiState as? PostUiState.Post)?.let {
            PostScreen(
                uiState = it,
                lazyState = lazyState,
                refreshState = refreshState,
                openDrawer = openDrawer,
                openSearch = { openSearch.invoke("") },
                onRefresh = onRefresh,
                onLoadMore = onLoadMore,
                onItemClick = onItemClick
            )
        }
    }
    AnimatedVisibility(
        visible = uiState is PostUiState.View,
        enter = fadeIn,
        exit = fadeOut
    ) {
        (uiState as? PostUiState.View)?.let {
            ViewScreen(
                uiState = it,
                onLoadMore = onLoadMore,
                onExit = onExitView,
                onTagClick = { tag, justSearch ->
                    onExitView.invoke()
                    if (justSearch) {
                        onSearch.invoke(tag.name)
                    } else {
                        openSearch.invoke(tag.name)
                    }
                }
            )
        }
    }
}