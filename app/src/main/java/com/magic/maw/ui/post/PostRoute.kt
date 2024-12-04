package com.magic.maw.ui.post

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hjq.toast.Toaster
import com.magic.maw.R
import com.magic.maw.ui.components.rememberNestedScaffoldState
import com.magic.maw.ui.view.ViewScreen
import com.magic.maw.util.UiUtils.showSystemBars
import kotlinx.coroutines.launch

@Composable
fun PostRoute(
    postViewModel: PostViewModel,
    searchText: String = "",
    titleText: String = stringResource(R.string.post),
    isSubView: Boolean = false,
    staggeredEnable: Boolean = true,
    onNegative: () -> Unit = {},
    openSearch: ((String) -> Unit)? = null,
    onOpenView: (Boolean) -> Unit = {},
) {
    val uiState by postViewModel.uiState.collectAsStateWithLifecycle()
    onOpenView.invoke(uiState !is PostUiState.View)

    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty()) {
            postViewModel.exitView()
            postViewModel.search(searchText)
        }
    }
    PostRoute(
        uiState = uiState,
        titleText = titleText,
        isSubView = isSubView,
        staggeredEnable = staggeredEnable,
        onNegative = onNegative,
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
    titleText: String = stringResource(R.string.post),
    isSubView: Boolean = false,
    staggeredEnable: Boolean = true,
    onNegative: () -> Unit,
    openSearch: ((String) -> Unit)? = null,
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

    var postState by remember { mutableStateOf<PostUiState.Post?>(null) }
    var viewState by remember { mutableStateOf<PostUiState.View?>(null) }
    when (uiState) {
        is PostUiState.Post -> postState = uiState
        is PostUiState.View -> viewState = uiState
    }
    val negativeIcon = if (isSubView) {
        Icons.AutoMirrored.Filled.ArrowBack
    } else {
        Icons.Default.Menu
    }
    AnimatedVisibility(
        visible = uiState is PostUiState.Post,
        enter = fadeIn,
        exit = fadeOut
    ) {
        postState?.let {
            LaunchedEffect(it.type) {
                if (it.type == UiStateType.Refresh) {
                    resetTopBar.invoke()
                }
            }
            PostScreen(
                uiState = it,
                lazyState = lazyState,
                refreshState = refreshState,
                scaffoldState = scaffoldState,
                titleText = titleText,
                negativeIcon = negativeIcon,
                staggeredEnable = staggeredEnable,
                onNegative = onNegative,
                openSearch = openSearch?.let { { it.invoke("") } },
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
        viewState?.let {
            ViewScreen(
                uiState = it,
                onLoadMore = onLoadMore,
                onExit = onExitView,
                onTagClick = onTagClick@{ tag, justSearch ->
                    if (isSubView)
                        return@onTagClick
                    if (justSearch || openSearch == null) {
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
