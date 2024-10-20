package com.magic.maw.ui.post

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hjq.toast.Toaster
import com.magic.maw.R
import com.magic.maw.ui.components.rememberNestedScaffoldState
import com.magic.maw.util.UiUtils.showSystemBars
import kotlinx.coroutines.launch

private const val TAG = "PostRoute"

@Composable
fun PostRoute(
    postViewModel: PostViewModel,
    openDrawer: () -> Unit,
    openSearch: (String) -> Unit,
    onOpenView: (Boolean) -> Unit,
) {
    val uiState by postViewModel.uiState.collectAsStateWithLifecycle()
    onOpenView.invoke(uiState is PostUiState.View)

    PostRoute(
        uiState = uiState,
        openDrawer = openDrawer,
        openSearch = openSearch,
        onRefresh = { postViewModel.refresh() },
        onLoadMore = { postViewModel.loadMore() },
        onItemClick = { postViewModel.setViewIndex(it) },
        onExitView = { postViewModel.exitView() },
        onClearSearch = {
            postViewModel.clearTags()
            postViewModel.refresh(true)
        },
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
    onClearSearch: () -> Unit,
    onSearch: (String) -> Unit,
) {
    val fadeIn = fadeIn(animationSpec = tween(700))
    val fadeOut = fadeOut(animationSpec = tween(700))
    val slideIn = slideInHorizontally(animationSpec = tween(700), initialOffsetX = { it })
    val slideOut = slideOutHorizontally(animationSpec = tween(700), targetOffsetX = { it })

    val lazyState = rememberLazyStaggeredGridState()
    val refreshState = rememberPullToRefreshState()
    val scaffoldState = rememberNestedScaffoldState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val resetTopBar: suspend () -> Unit = {
        lazyState.scrollToItem(0, 0)
        scaffoldState.animateTo(scaffoldState.maxPx)
        context.showSystemBars()
    }

    AnimatedVisibility(
        visible = uiState is PostUiState.Post,
        enter = fadeIn,
        exit = fadeOut
    ) {
        val currentState = uiState as? PostUiState.Post
        var postState by remember { mutableStateOf<PostUiState.Post?>(null) }
        if (postState != currentState && currentState != null) {
            postState = currentState
        }
        postState?.let {
            LaunchedEffect(uiState.type) {
                if (uiState.type == UiStateType.Refresh) {
                    resetTopBar.invoke()
                }
            }
            PostScreen(
                uiState = it,
                lazyState = lazyState,
                refreshState = refreshState,
                scaffoldState = scaffoldState,
                openDrawer = openDrawer,
                openSearch = { openSearch.invoke("") },
                onRefresh = onRefresh,
                onLoadMore = onLoadMore,
                onItemClick = onItemClick
            )
            BackHandler(enabled = it.isSearch, onBack = {
                onClearSearch.invoke()
                scope.launch { resetTopBar.invoke() }
                Toaster.show(R.string.click_again_to_exit)
            })
        }
    }
    AnimatedVisibility(
        visible = uiState is PostUiState.View,
        enter = slideIn + fadeIn,
        exit = slideOut + fadeOut
    ) {
        val currentState = uiState as? PostUiState.View
        var viewState by remember { mutableStateOf<PostUiState.View?>(null) }
        if (viewState != currentState && currentState != null) {
            viewState = currentState
        }
        viewState?.let {
            ViewScreen(
                uiState = it,
                onLoadMore = onLoadMore,
                onExit = onExitView,
                onTagClick = { tag, justSearch ->
                    if (justSearch) {
                        onExitView.invoke()
                        onSearch.invoke(tag.name)
                        scope.launch { resetTopBar.invoke() }
                    } else {
                        openSearch.invoke(tag.name)
                    }
                }
            )
        }
    }
}