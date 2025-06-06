package com.magic.maw.ui.pool

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magic.maw.data.PoolData
import com.magic.maw.ui.components.ConfigChangeChecker
import com.magic.maw.ui.components.RegisterView
import com.magic.maw.ui.components.changeSystemBarStatus
import com.magic.maw.ui.components.rememberNestedScaffoldState
import com.magic.maw.ui.post.PostRoute
import com.magic.maw.ui.post.PostViewModel
import com.magic.maw.ui.post.UiStateType
import com.magic.maw.util.configFlow
import com.magic.maw.website.RequestOption
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.launch

private const val viewName = "Pool"

@Composable
fun PoolRoute(
    poolViewModel: PoolViewModel,
    openDrawer: () -> Unit,
    onOpenSubView: (Boolean) -> Unit
) {
    val uiState by poolViewModel.uiState.collectAsStateWithLifecycle()
    onOpenSubView(uiState.isPostView)

    LaunchedEffect(Unit) {
        if (uiState.isPoolView
            && uiState.dataList.isEmpty()
            && !uiState.noMore
            && uiState.type != UiStateType.Refresh
        ) {
            poolViewModel.refresh()
        }
    }

    PoolRoute(
        uiState = uiState,
        openDrawer = openDrawer,
        onRefresh = { poolViewModel.refresh() },
        onForceRefresh = { poolViewModel.refresh(true) },
        onLoadMore = { poolViewModel.loadMore() },
        onItemClick = { poolViewModel.setViewIndex(it) },
        onExitPost = { poolViewModel.exitPostView() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolRoute(
    uiState: PoolUiState,
    openDrawer: () -> Unit,
    onRefresh: () -> Unit,
    onForceRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (Int) -> Unit,
    onExitPost: () -> Unit,
) {
    val fadeIn = fadeIn(animationSpec = tween(700))
    val fadeOut = fadeOut(animationSpec = tween(700))
    val slideIn = slideInHorizontally(animationSpec = tween(700), initialOffsetX = { it })
    val slideOut = slideOutHorizontally(animationSpec = tween(700), targetOffsetX = { it })

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lazyState = rememberLazyStaggeredGridState()
    val scaffoldState = rememberNestedScaffoldState()
    val refreshState = rememberPullToRefreshState()

    RegisterView(name = viewName)

    ConfigChangeChecker {
        changeSystemBarStatus(context, viewName, true)
        scope.launch {
            scaffoldState.snapTo(scaffoldState.maxPx)
            lazyState.scrollToItem(0, 0)
        }
        onForceRefresh()
        onExitPost()
    }

    AnimatedVisibility(
        visible = uiState.isPoolView,
        enter = fadeIn,
        exit = fadeOut
    ) {
        PoolScreen(
            uiState = uiState,
            lazyState = lazyState,
            scaffoldState = scaffoldState,
            refreshState = refreshState,
            openDrawer = openDrawer,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            onShowSystemBar = { changeSystemBarStatus(context, viewName, it) },
            onItemClick = onItemClick
        )
    }

    AnimatedVisibility(
        visible = uiState.isPostView,
        enter = slideIn + fadeIn,
        exit = slideOut + fadeOut
    ) {
        val (poolData, factory) = getPoolPostInfo(uiState)
        val postViewModel: PostViewModel = viewModel(factory = factory)
        val title = remember { mutableStateOf("") }
        poolData?.let {
            postViewModel.update(it.posts)
            title.value = "#${it.id}"
        }
        PostRoute(
            postViewModel = postViewModel,
            searchEnable = false,
            negativeIcon = Icons.AutoMirrored.Filled.ArrowBack,
            titleText = title.value,
            onNegative = onExitPost,
        )
        BackHandler { onExitPost() }
    }
}

private fun getPoolPostInfo(uiState: PoolUiState): Pair<PoolData?, ViewModelProvider.Factory> {
    if (uiState.isPoolView || uiState.viewIndex >= uiState.dataList.size)
        return Pair(null, PostViewModel.providerFactory())
    val parser = BaseParser.get(configFlow.value.source)
    val poolData = uiState.dataList[uiState.viewIndex]
    val option = RequestOption(page = parser.firstPageIndex, poolId = poolData.id)
    val factory = PostViewModel.providerFactory(option, poolData.posts)
    return Pair(poolData, factory)
}
