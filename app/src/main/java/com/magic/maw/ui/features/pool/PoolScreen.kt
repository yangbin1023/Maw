package com.magic.maw.ui.features.pool

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
import co.touchlab.kermit.Logger
import com.magic.maw.R
import com.magic.maw.data.api.loader.LoadState
import com.magic.maw.data.api.loader.PoolDataUiState
import com.magic.maw.data.model.site.PoolData
import com.magic.maw.ui.common.EmptyView
import com.magic.maw.ui.common.LoadMoreChecker
import com.magic.maw.ui.features.main.AppRoute
import com.magic.maw.ui.features.post.PostDefaults
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
    val dataState by viewModel.uiState.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = topAppBarState)

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    LaunchedEffect(lifecycleOwner) {
        if (lifecycleState == Lifecycle.State.STARTED
            || lifecycleState == Lifecycle.State.RESUMED) {
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
            uiState = dataState,
            refreshState = refreshState,
            lazyState = lazyState,
            onRefresh = { viewModel.refresh() },
            onLoadMore = { viewModel.loadMore() },
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
