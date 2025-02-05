package com.magic.maw.ui.post

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.rememberPagerState
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
import com.magic.maw.ui.components.RatingChangeChecker
import com.magic.maw.ui.components.RegisterView
import com.magic.maw.ui.components.SourceChangeChecker
import com.magic.maw.ui.components.changeSystemBarStatus
import com.magic.maw.ui.components.rememberNestedScaffoldState
import com.magic.maw.ui.view.ViewScreen
import kotlinx.coroutines.launch

private const val viewName = "Post"

@Composable
fun PostRoute(
    postViewModel: PostViewModel,
    titleText: String = stringResource(R.string.post),
    isSubView: Boolean = false,
    staggeredEnable: Boolean = true,
    onNegative: () -> Unit = {},
    openSearch: ((String) -> Unit)? = null,
    onOpenSubView: (Boolean) -> Unit = {},
) {
    val uiState by postViewModel.uiState.collectAsStateWithLifecycle()
    onOpenSubView.invoke(uiState is PostUiState.View)

    PostRoute(
        uiState = uiState,
        titleText = titleText,
        isSubView = isSubView,
        staggeredEnable = staggeredEnable,
        onNegative = onNegative,
        openSearch = openSearch,
        onRefresh = { postViewModel.refresh() },
        onForceRefresh = { postViewModel.refresh(true) },
        onLoadMore = { postViewModel.loadMore() },
        onItemClick = { postViewModel.setViewIndex(it) },
        onExitView = { postViewModel.exitView() },
        onClearSearch = {
            postViewModel.clearTags()
            postViewModel.refresh(true)
        },
        onSearch = {
            postViewModel.search(it)
            postViewModel.exitView()
        }
    )
}

@SuppressLint("MutableCollectionMutableState")
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
    onForceRefresh: () -> Unit,
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
    val itemHeights by remember { mutableStateOf(mutableMapOf<Int, Int>()) }

    val resetTopBar: () -> Unit = {
        changeSystemBarStatus(context, viewName, true)
        scope.launch {
            lazyState.scrollToItem(0, 0)
            scaffoldState.snapTo(scaffoldState.maxPx)
        }
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
    RegisterView(name = viewName)

    LaunchedEffect(uiState.type) {
        if (uiState.type == UiStateType.Refresh) {
            resetTopBar.invoke()
            onExitView.invoke()
        }
    }
    SourceChangeChecker {
        resetTopBar.invoke()
        onExitView.invoke()
        onClearSearch.invoke()
    }
    RatingChangeChecker {
        resetTopBar.invoke()
        onExitView.invoke()
        onForceRefresh.invoke()
    }
    AnimatedVisibility(
        visible = uiState is PostUiState.Post,
        enter = fadeIn,
        exit = fadeOut
    ) {
        val state = postState ?: return@AnimatedVisibility
        PostScreen(
            uiState = state,
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
            onShowSystemBar = { changeSystemBarStatus(context, viewName, it) },
            onGloballyPositioned = { index, height -> itemHeights[index] = height },
            onItemClick = onItemClick
        )
        BackHandler(enabled = state.isSearch, onBack = {
            onClearSearch.invoke()
            resetTopBar.invoke()
            Toaster.show(R.string.click_again_to_exit)
        })
    }
    AnimatedVisibility(
        visible = uiState is PostUiState.View,
        enter = slideIn + fadeIn,
        exit = slideOut + fadeOut
    ) {
        val state = viewState ?: return@AnimatedVisibility
        val pagerState = rememberPagerState(state.initIndex) { uiState.dataList.size }
        val onExit: () -> Unit = {
            scope.launch {
                if (state.initIndex != pagerState.currentPage) {
                    // 退出View时将最后查看的item移动到视野中央
                    val itemHeight = itemHeights[pagerState.currentPage] ?: 0
                    val viewportHeight = lazyState.layoutInfo.viewportSize.height
                    val offset = -(viewportHeight - itemHeight) / 2
                    lazyState.scrollToItem(pagerState.currentPage, offset)
                }
                onExitView()
            }
        }
        ViewScreen(
            uiState = state,
            pagerState = pagerState,
            onLoadMore = onLoadMore,
            onExit = onExit,
            onTagClick = onTagClick@{ tag, justSearch ->
                if (isSubView)
                    return@onTagClick
                if (justSearch || openSearch == null) {
                    resetTopBar.invoke()
                    onSearch.invoke(tag.name)
                } else {
                    openSearch.invoke(tag.name)
                }
            }
        )
        BackHandler(onBack = onExit)
    }
}
