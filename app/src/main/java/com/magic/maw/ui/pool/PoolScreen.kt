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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import com.magic.maw.R
import com.magic.maw.data.PoolData
import com.magic.maw.data.loader.LoadState
import com.magic.maw.data.loader.PoolDataUiState
import com.magic.maw.ui.components.EmptyView
import com.magic.maw.ui.components.LoadMoreChecker
import com.magic.maw.ui.components.NestedScaffold
import com.magic.maw.ui.components.NestedScaffoldState
import com.magic.maw.ui.components.rememberNestedScaffoldState
import com.magic.maw.ui.main.AppRoute
import com.magic.maw.ui.post.PostDefaults
import com.magic.maw.ui.post.UiStateType
import com.magic.maw.util.UiUtils
import com.magic.maw.util.UiUtils.getStatusBarHeight
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.launch
import kotlin.math.max

private const val TAG = "PoolScreen"

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
        topBar = { PoolTopBar(onNegative = openDrawer) },
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
fun PoolScreen(
    modifier: Modifier = Modifier,
    viewModel: PoolViewModel2 = viewModel(),
    navController: NavController = rememberNavController(),
    lazyState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    scaffoldState: NestedScaffoldState = rememberNestedScaffoldState(),
    refreshState: PullToRefreshState = rememberPullToRefreshState(),
    onNegative: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val dataState by viewModel.loader.uiState.collectAsStateWithLifecycle()
    val scrollToTop: () -> Unit = {
        Logger.d(TAG) { "scrollToTop() called" }
        scope.launch { lazyState.scrollToItem(0, 0) }
    }
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = topAppBarState)

//    RefreshScrollToTopChecker(items = dataState.items, scrollToTop = scrollToTop)

    Scaffold(
        modifier = modifier,
        topBar = {
            PoolTopBar(
                onNegative = onNegative,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        PoolRefreshBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            uiState = dataState,
            refreshState = refreshState,
            lazyState = lazyState,
            onRefresh = { viewModel.loader.refresh() },
            onLoadMore = { viewModel.loader.loadMore() },
            onItemClick = { index, poolData ->
                Logger.d(TAG) { "onItemClick $poolData" }
                viewModel.setViewPoolPost(poolData.id)
                navController.navigate(route = AppRoute.PoolPost(poolId = poolData.id))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolTopBar(
    modifier: Modifier = Modifier,
    shadowEnable: Boolean = true,
    onNegative: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.pool)) },
        modifier = modifier.let { if (shadowEnable) it.shadow(3.dp) else it },
        navigationIcon = {
            onNegative?.let { onNegative ->
                IconButton(onClick = onNegative) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "",
                    )
                }
            }
        },
        windowInsets = WindowInsets(top = LocalContext.current.getStatusBarHeight()),
        colors = UiUtils.topAppBarColors,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoolRefreshBody(
    uiState: PoolDataUiState,
    modifier: Modifier = Modifier,
    lazyState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    refreshState: PullToRefreshState = rememberPullToRefreshState(),
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onItemClick: (Int, PoolData) -> Unit = { _, _ -> },
) {
    val isRefreshing by remember(uiState.loadState) {
        derivedStateOf { uiState.loadState == LoadState.Refreshing }
    }
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = refreshState
    ) {
        val isEmpty by remember(uiState.items) {
            derivedStateOf { uiState.items.isEmpty() }
        }
        if (isEmpty) {
            EmptyView(
                modifier = Modifier.fillMaxSize(),
                loadState = uiState.loadState,
                onRefresh = onRefresh
            )
        } else {
            LoadMoreChecker(
                uiState = uiState,
                state = lazyState,
                onLoadMore = onLoadMore
            )
            PoolBody(
                modifier = Modifier.fillMaxSize(),
                items = uiState.items,
                canClick = !isRefreshing,
                hasNoMore = uiState.hasNoMore,
                lazyState = lazyState,
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

@Composable
private fun PoolBody(
    modifier: Modifier = Modifier,
    items: PersistentList<PoolData>,
    hasNoMore: Boolean = false,
    canClick: Boolean = true,
    lazyState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onItemClick: (Int, PoolData) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val columns = max((this.maxWidth / 320.dp).toInt(), 1)
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = lazyState,
            contentPadding = PaddingValues(3.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.id }
            ) { index, data ->
                PoolItem(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    poolData = data,
                    canClick = canClick,
                    onClick = { onItemClick(index, data) }
                )
            }
            if (hasNoMore) {
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
