package com.magic.maw.ui.pool

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magic.maw.data.PoolData
import com.magic.maw.ui.components.rememberNestedScaffoldState
import com.magic.maw.ui.post.PostRoute
import com.magic.maw.ui.post.PostViewModel
import com.magic.maw.util.configFlow
import com.magic.maw.website.RequestOption
import com.magic.maw.website.parser.BaseParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolRoute(poolViewModel: PoolViewModel, modifier: Modifier = Modifier, openDrawer: () -> Unit) {
    val uiState by poolViewModel.uiState.collectAsStateWithLifecycle()
    PoolRoute(
        uiState = uiState,
        modifier = modifier,
        openDrawer = openDrawer,
        onRefresh = { poolViewModel.refresh() },
        onLoadMore = { poolViewModel.loadMore() },
        onItemClick = { poolViewModel.setViewIndex(it) },
        onExitPost = { poolViewModel.setViewIndex(-1) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolRoute(
    modifier: Modifier = Modifier,
    uiState: PoolUiState,
    openDrawer: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (Int) -> Unit,
    onExitPost: () -> Unit,
) {
    val fadeIn = fadeIn(animationSpec = tween(700))
    val fadeOut = fadeOut(animationSpec = tween(700))
    val slideIn = slideInHorizontally(animationSpec = tween(700), initialOffsetX = { it })
    val slideOut = slideOutHorizontally(animationSpec = tween(700), targetOffsetX = { it })

    val lazyState = rememberLazyStaggeredGridState()
    val scaffoldState = rememberNestedScaffoldState()
    val refreshState = rememberPullToRefreshState()

    AnimatedVisibility(
        visible = uiState.viewIndex < 0,
        enter = fadeIn,
        exit = fadeOut
    ) {
        PoolScreen(
            uiState = uiState,
            modifier = modifier,
            lazyState = lazyState,
            scaffoldState = scaffoldState,
            refreshState = refreshState,
            openDrawer = openDrawer,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            onItemClick = onItemClick
        )
    }

    AnimatedVisibility(
        visible = uiState.viewIndex >= 0,
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
            searchText = "",
            isSubView = true,
            titleText = title.value,
            onNegative = onExitPost,
        )
        BackHandler { onExitPost.invoke() }
    }
}

private fun getPoolPostInfo(uiState: PoolUiState): Pair<PoolData?, ViewModelProvider.Factory> {
    val parser = BaseParser.get(configFlow.value.source)
    if (uiState.viewIndex < 0 || uiState.viewIndex >= uiState.dataList.size)
        return Pair(null, PostViewModel.providerFactory(parser))
    val poolData = uiState.dataList[uiState.viewIndex]
    val option = RequestOption(page = parser.firstPageIndex, poolId = poolData.id)
    val factory = PostViewModel.providerFactory(parser, option, poolData.posts)
    return Pair(poolData, factory)
}
