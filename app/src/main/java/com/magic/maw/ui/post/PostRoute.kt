package com.magic.maw.ui.post

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.magic.maw.R
import com.magic.maw.ui.components.rememberNestedScaffoldState
import kotlinx.coroutines.launch

private const val TAG = "PostRoute"

@Composable
fun PostRoute(
    postViewModel: PostViewModel,
    openDrawer: () -> Unit,
    openSearch: (String) -> Unit,
) {
    val uiState by postViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
            postViewModel.viewModelScope.launch {
                val message = context.getString(R.string.click_again_to_exit)
                snackbarHostState.showSnackbar(message = message)
            }
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
    val fadeIn = fadeIn(animationSpec = tween(1200))
    val fadeOut = fadeOut(animationSpec = tween(1200))

    val lazyState = rememberLazyStaggeredGridState()
    val refreshState = rememberPullToRefreshState()
    val scaffoldState = rememberNestedScaffoldState()
    val scope = rememberCoroutineScope()

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
                scope.launch {
                    lazyState.scrollToItem(0, 0)
                    scaffoldState.animateTo(scaffoldState.maxPx)
                }
            })
        }
    }
    AnimatedVisibility(
        visible = uiState is PostUiState.View,
        enter = fadeIn,
        exit = fadeOut
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
                    onExitView.invoke()
                    if (justSearch) {
                        onSearch.invoke(tag.name)
                        scope.launch {
                            lazyState.scrollToItem(0, 0)
                            scaffoldState.animateTo(scaffoldState.maxPx)
                        }
                    } else {
                        openSearch.invoke(tag.name)
                    }
                }
            )
        }
    }
}