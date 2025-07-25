package com.magic.maw.ui.post

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.magic.maw.R
import com.magic.maw.ui.components.NestedScaffold
import com.magic.maw.ui.components.NestedScaffoldState
import com.magic.maw.ui.components.rememberNestedScaffoldState
import com.magic.maw.ui.theme.TableLayout
import com.magic.maw.ui.theme.WaterLayout
import com.magic.maw.util.UiUtils.getStatusBarHeight
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "PostScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    uiState: PostUiState.Post,
    lazyState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    refreshState: PullToRefreshState = rememberPullToRefreshState(),
    scaffoldState: NestedScaffoldState = rememberNestedScaffoldState(),
    titleText: String = stringResource(R.string.post),
    staggeredEnable: Boolean = true,
    shadowEnable: Boolean = true,
    searchEnable: Boolean = true,
    negativeIcon: ImageVector = Icons.Default.Menu,
    enhancedBar: (@Composable (Modifier) -> Unit)? = null,
    onNegative: () -> Unit = {},
    onSearch: () -> Unit = {},
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onShowSystemBar: (Boolean) -> Unit,
    onGloballyPositioned: (Int, Int) -> Unit,
    onItemClick: (Int) -> Unit,
) {
    val staggeredState = if (staggeredEnable) remember { mutableStateOf(false) } else null
    NestedScaffoldBody(
        uiState = uiState,
        lazyState = lazyState,
        refreshState = refreshState,
        titleText = titleText,
        shadowEnable = shadowEnable,
        searchEnable = searchEnable,
        negativeIcon = negativeIcon,
        staggeredState = staggeredState,
        scaffoldState = scaffoldState,
        enhancedBar = enhancedBar,
        onNegative = onNegative,
        onSearch = onSearch,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
        onShowSystemBar = onShowSystemBar,
        onGloballyPositioned = onGloballyPositioned,
        onItemClick = onItemClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NestedScaffoldBody(
    uiState: PostUiState.Post,
    lazyState: LazyStaggeredGridState,
    refreshState: PullToRefreshState,
    scaffoldState: NestedScaffoldState = rememberNestedScaffoldState(),
    titleText: String = stringResource(R.string.post),
    shadowEnable: Boolean = true,
    searchEnable: Boolean = true,
    negativeIcon: ImageVector = Icons.Default.Menu,
    staggeredState: MutableState<Boolean>? = null,
    enhancedBar: (@Composable (Modifier) -> Unit)? = null,
    onNegative: () -> Unit = {},
    onSearch: () -> Unit = {},
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onShowSystemBar: (Boolean) -> Unit,
    onGloballyPositioned: (Int, Int) -> Unit,
    onItemClick: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scrollToTop: () -> Unit = { scope.launch { lazyState.scrollToItem(0, 0) } }
    LaunchedEffect(scaffoldState) {
        if (scaffoldState.scrollValue == scaffoldState.minPx) {
            onShowSystemBar(false)
        } else if (scaffoldState.scrollValue == scaffoldState.maxPx) {
            onShowSystemBar(true)
        }
    }
    NestedScaffold(
        topBar = {
            PostTopBar(
                titleText = titleText,
                negativeIcon = negativeIcon,
                shadowEnable = shadowEnable,
                searchEnable = searchEnable,
                staggeredState = staggeredState,
                scrollToTop = scrollToTop,
                onNegative = onNegative,
                onSearch = onSearch
            )
        },
        enhancedBar = enhancedBar,
        state = scaffoldState,
        canScroll = {
            refreshState.distanceFraction <= 0 && uiState.dataList.isNotEmpty()
        },
        onScrollToTop = { onShowSystemBar(false) },
        onScrollToBottom = { onShowSystemBar(true) },
    ) { innerPadding ->
        PostRefreshBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            uiState = uiState,
            refreshState = refreshState,
            lazyState = lazyState,
            staggeredState = staggeredState,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            onGloballyPositioned = onGloballyPositioned,
            onItemClick = onItemClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostTopBar(
    modifier: Modifier = Modifier,
    titleText: String = stringResource(id = R.string.post),
    negativeIcon: ImageVector = Icons.Default.Menu,
    shadowEnable: Boolean = true,
    searchEnable: Boolean = true,
    staggeredState: MutableState<Boolean>? = null,
    scrollToTop: () -> Unit = {},
    onNegative: () -> Unit = {},
    onSearch: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = { Text(text = titleText) },
        modifier = modifier.let { if (shadowEnable) it.shadow(3.dp) else it },
        navigationIcon = {
            IconButton(onClick = onNegative) {
                Icon(
                    imageVector = negativeIcon,
                    contentDescription = "",
                )
            }
        },
        actions = {
            staggeredState?.let {
                IconButton(
                    onClick = {
                        it.value = !it.value
                        scrollToTop()
                    },
                    modifier = Modifier.width(PostDefaults.ActionsIconWidth)
                ) {
                    val imageVector = if (!it.value) TableLayout else WaterLayout
                    Icon(imageVector = imageVector, contentDescription = "")
                }
            }
            if (searchEnable) {
                IconButton(
                    onClick = onSearch,
                    modifier = Modifier.width(PostDefaults.ActionsIconWidth)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "")
                }
            }
        },
        windowInsets = WindowInsets(top = LocalContext.current.getStatusBarHeight()),
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostRefreshBody(
    modifier: Modifier = Modifier,
    uiState: PostUiState.Post,
    refreshState: PullToRefreshState,
    lazyState: LazyStaggeredGridState,
    staggeredState: MutableState<Boolean>? = null,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onGloballyPositioned: (Int, Int) -> Unit,
    onItemClick: (Int) -> Unit,
) {
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = uiState.type == UiStateType.Refresh,
        onRefresh = onRefresh,
        state = refreshState
    ) {
        if (uiState.dataList.isEmpty()) {
            PostEmptyView(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                onRefresh = onRefresh
            )
        } else {
            PostBody(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                state = lazyState,
                staggeredState = staggeredState,
                onGloballyPositioned = onGloballyPositioned,
                onItemClick = onItemClick,
                onLoadMore = onLoadMore,
            )
        }
    }
}

@Composable
private fun PostEmptyView(
    modifier: Modifier = Modifier,
    uiState: PostUiState.Post,
    onRefresh: () -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),//
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val text = if (uiState.type.isLoading()) {
            stringResource(R.string.loading)
        } else if (uiState.type == UiStateType.LoadFailed) {
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
private fun PostBody(
    modifier: Modifier = Modifier,
    uiState: PostUiState.Post,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    staggeredState: MutableState<Boolean>? = null,
    onGloballyPositioned: (Int, Int) -> Unit,
    onItemClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val columns = max((this.maxWidth / 210.dp).toInt(), 2)
        val contentPadding = getContentPadding(maxWidth = maxWidth, columns = columns)

        // 检测加载进度，实现自动加载更多
        LaunchedEffect(uiState, state) {
            snapshotFlow {
                state.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            }.distinctUntilChanged().collect { lastVisibleIndex ->
                if (uiState.noMore || uiState.type.isLoading() || lastVisibleIndex == null) {
                    return@collect
                }
                val totalItemsCount = state.layoutInfo.totalItemsCount
                val remainingItemsCount = totalItemsCount - (lastVisibleIndex + 1)
                val visibleItemsCount = state.layoutInfo.visibleItemsInfo.size
                if (remainingItemsCount < visibleItemsCount * 2) {
                    Logger.d(TAG) { "call post on load more" }
                    onLoadMore()
                }
            }
        }
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = state,
            contentPadding = PaddingValues(contentPadding)
        ) {
            itemsIndexed(uiState.dataList) { index, item ->
                PostItem(
                    modifier = Modifier
                        .padding(contentPadding)
                        .onGloballyPositioned { onGloballyPositioned(index, it.size.height) },
                    onClick = {
                        if (uiState.type != UiStateType.Refresh) {
                            onItemClick(index)
                        }
                    },
                    postData = item,
                    staggered = staggeredState?.value == true
                )
            }

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
private fun getContentPadding(maxWidth: Dp, columns: Int): Dp = with(LocalDensity.current) {
    val totalWidth = maxWidth.toPx().toInt()
    val targetSpace = (PostDefaults.ContentPadding * 2).toPx().roundToInt()
    val itemMaxWidth = totalWidth / columns
    var currentSpace = Int.MAX_VALUE
    for (space in 1 until itemMaxWidth) {
        if ((totalWidth - space * (columns + 1)) % columns == 0) {
            if (abs(space - targetSpace) < abs(currentSpace - targetSpace)) {
                currentSpace = space
            } else {
                break
            }
        }
    }
    if (currentSpace == Int.MAX_VALUE) {
        currentSpace = targetSpace
        Logger.w(TAG) { "No suitable space found" }
    }
    currentSpace.toDp() / 2
}

object PostDefaults {
    val ContentPadding: Dp = 3.5.dp
    val ActionsIconWidth: Dp = 40.dp
    val NoMoreItemHeight: Dp = 36.dp
}