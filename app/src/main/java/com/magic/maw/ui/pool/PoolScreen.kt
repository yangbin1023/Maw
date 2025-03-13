package com.magic.maw.ui.pool

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.magic.maw.R
import com.magic.maw.ui.components.NestedScaffold
import com.magic.maw.ui.components.NestedScaffoldState
import com.magic.maw.ui.components.rememberNestedScaffoldState
import com.magic.maw.ui.post.PostDefaults
import com.magic.maw.ui.post.UiStateType
import com.magic.maw.util.UiUtils.getStatusBarHeight
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolScreen(
    uiState: PoolUiState,
    modifier: Modifier = Modifier,
    lazyState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    scaffoldState: NestedScaffoldState = rememberNestedScaffoldState(),
    refreshState: PullToRefreshState = rememberPullToRefreshState(),
    openDrawer: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onShowSystemBar: (Boolean) -> Unit,
    onItemClick: (Int) -> Unit,
) {
    LaunchedEffect(scaffoldState) {
        if (scaffoldState.scrollValue == scaffoldState.minPx) {
            onShowSystemBar(false)
        } else if (scaffoldState.scrollValue == scaffoldState.maxPx) {
            onShowSystemBar(true)
        }
    }
    NestedScaffold(
        modifier = modifier,
        topBar = { offset ->
            PoolTopBar(
                modifier = Modifier.offset { offset },
                openDrawer = openDrawer
            )
        },
        state = scaffoldState,
        canScroll = {
            refreshState.distanceFraction <= 0 && uiState.dataList.isNotEmpty()
        },
        onScrollToTop = { onShowSystemBar(false) },
        onScrollToBottom = { onShowSystemBar(true) },
    ) { innerPadding ->
        PoolRefreshBody(
            uiState = uiState,
            modifier = Modifier.padding(innerPadding),
            lazyState = lazyState,
            refreshState = refreshState,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            onItemClick = onItemClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolTopBar(
    modifier: Modifier = Modifier,
    enableShadow: Boolean = true,
    openDrawer: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.pool)) },
        modifier = modifier.let { if (enableShadow) it.shadow(3.dp) else it },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "",
                )
            }
        },
        windowInsets = WindowInsets(top = LocalContext.current.getStatusBarHeight()),
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolRefreshBody(
    uiState: PoolUiState,
    modifier: Modifier = Modifier,
    lazyState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    refreshState: PullToRefreshState = rememberPullToRefreshState(),
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onItemClick: (Int) -> Unit = {},
) {
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = uiState.type == UiStateType.Refresh,
        onRefresh = onRefresh,
        state = refreshState
    ) {
        if (uiState.dataList.isEmpty()) {
            PoolEmptyView(
                modifier = Modifier.fillMaxSize(),
                type = uiState.type,
                onRefresh = onRefresh
            )
        } else {
            PoolBody(
                uiState = uiState,
                modifier = Modifier.fillMaxSize(),
                lazyState = lazyState,
                onLoadMore = onLoadMore,
                onItemClick = onItemClick,
            )
        }
    }
}

@Composable
private fun PoolEmptyView(
    modifier: Modifier = Modifier,
    type: UiStateType,
    onRefresh: () -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),//
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val text = if (type.isLoading()) {
            stringResource(R.string.loading)
        } else if (type == UiStateType.LoadFailed) {
            stringResource(R.string.loading_failed)
        } else {
            stringResource(R.string.no_data)
        }
        Text(
            text = text,
            modifier = Modifier
                .padding(15.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRefresh
                )
        )
    }
}

@Composable
private fun PoolBody(
    modifier: Modifier = Modifier,
    uiState: PoolUiState,
    lazyState: LazyStaggeredGridState,
    onLoadMore: () -> Unit,
    onItemClick: (Int) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val columns = max((this.maxWidth / 320.dp).toInt(), 1)
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = lazyState,
            contentPadding = PaddingValues(3.dp)
        ) {
            itemsIndexed(uiState.dataList) { index, data ->
                PoolItem(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    poolData = data,
                    onClick = { onItemClick(index) }
                )
            }
            checkLoadMore(uiState = uiState, state = lazyState, onLoadMore = onLoadMore)
            if (uiState.noMore) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = stringResource(R.string.no_more_data),
                        modifier = Modifier
                            .height(PostDefaults.NoMoreItemHeight)
                            .wrapContentSize(Alignment.Center)
                    )
                }
            }
        }
    }
}

private fun checkLoadMore(
    uiState: PoolUiState,
    state: LazyStaggeredGridState,
    onLoadMore: () -> Unit
) {
    if (uiState.noMore || uiState.type.isLoading()) {
        return
    }
    val totalItemsCount = state.layoutInfo.totalItemsCount
    val visibleItemsInfo = state.layoutInfo.visibleItemsInfo
    if (visibleItemsInfo.isNotEmpty()) {
        val lastVisibleIndex = visibleItemsInfo.last().index
        if ((totalItemsCount - lastVisibleIndex) < visibleItemsInfo.size * 1.5) {
            onLoadMore()
        }
    }
}
