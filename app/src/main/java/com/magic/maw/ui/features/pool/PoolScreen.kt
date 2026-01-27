package com.magic.maw.ui.features.pool

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import co.touchlab.kermit.Logger
import com.magic.maw.R
import com.magic.maw.data.api.loader.LoadState
import com.magic.maw.data.api.loader.PoolDataUiState
import com.magic.maw.data.model.site.PoolData
import com.magic.maw.ui.common.EmptyView
import com.magic.maw.ui.common.LoadMoreChecker
import com.magic.maw.ui.features.main.AppRoute
import com.magic.maw.ui.features.post.PostDefaults
import com.magic.maw.ui.features.post.PostItem
import com.magic.maw.util.UiUtils
import com.magic.maw.util.UiUtils.getStatusBarHeight
import kotlinx.collections.immutable.PersistentList
import kotlin.math.max

private const val TAG = "PoolScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolScreen(
    modifier: Modifier = Modifier,
    viewModel: PoolViewModel = viewModel(),
    navController: NavController = rememberNavController(),
    lazyState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    refreshState: PullToRefreshState = rememberPullToRefreshState(),
    onNegative: (() -> Unit)? = null,
) {
    val lazyPagingItems = viewModel.poolFlow.collectAsLazyPagingItems()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = topAppBarState)

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    LaunchedEffect(lifecycleOwner) {
        if (lifecycleState == Lifecycle.State.STARTED
            || lifecycleState == Lifecycle.State.RESUMED
        ) {
            viewModel.checkAndRefresh()
        }
    }

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
            lazyPagingItems = lazyPagingItems,
            refreshState = refreshState,
            lazyState = lazyState,
            onRefresh = { lazyPagingItems.refresh() },
            onItemClick = { _, poolData ->
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
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<PoolData>,
    lazyState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    refreshState: PullToRefreshState = rememberPullToRefreshState(),
    onRefresh: () -> Unit = {},
    onItemClick: (Int, PoolData) -> Unit = { _, _ -> },
) {
    val isRefreshing by remember(lazyPagingItems) {
        derivedStateOf { lazyPagingItems.loadState.refresh is androidx.paging.LoadState.Loading }
    }
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = refreshState
    ) {
        val isEmpty by remember(lazyPagingItems) {
            derivedStateOf { lazyPagingItems.itemCount <= 0 }
        }
        if (isEmpty) {
            EmptyView(
                modifier = Modifier.fillMaxSize(),
                loadState = lazyPagingItems.loadState,
                onRefresh = onRefresh
            )
        } else {
            PoolBody(
                modifier = Modifier.fillMaxSize(),
                lazyPagingItems = lazyPagingItems,
                canClick = !isRefreshing,
                lazyState = lazyState,
                onItemClick = onItemClick,
            )
        }
    }
}

@Composable
private fun PoolBody(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<PoolData>,
    canClick: Boolean = true,
    lazyState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onItemClick: (Int, PoolData) -> Unit
) {
    val loadState = lazyPagingItems.loadState
    val hasNoMore by remember(loadState) {
        derivedStateOf {
            loadState.append is androidx.paging.LoadState.NotLoading &&
                    loadState.append.endOfPaginationReached &&
                    lazyPagingItems.itemCount > 0
        }
    }
    BoxWithConstraints(modifier = modifier) {
        val columns = max((this.maxWidth / 320.dp).toInt(), 1)
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = lazyState,
            contentPadding = PaddingValues(3.dp)
        ) {

            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.id }
            ) { index ->
                val item = lazyPagingItems[index]
                if (item != null) {
                    PoolItem(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp),
                        poolData = item,
                        canClick = canClick,
                        onClick = { onItemClick(index, item) }
                    )
                }
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                if (hasNoMore) {
                    Text(
                        text = stringResource(R.string.no_more_data),
                        modifier = Modifier
                            .height(PostDefaults.NoMoreItemHeight)
                            .wrapContentSize(Alignment.Center)
                    )
                } else {
                    val loadState = lazyPagingItems.loadState.append
                    if (loadState is androidx.paging.LoadState.Loading) {
                        CircularProgressIndicator()
                    } else if (loadState is androidx.paging.LoadState.Error) {
                        Text(text = "加载失败，请重试", modifier = Modifier.clickable(onClick = {
                            lazyPagingItems.retry()
                        }))
                    }
                }
            }
        }
    }
}
