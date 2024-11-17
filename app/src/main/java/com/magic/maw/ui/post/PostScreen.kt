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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.magic.maw.R
import com.magic.maw.ui.components.NestedScaffold
import com.magic.maw.ui.components.NestedScaffoldState
import com.magic.maw.ui.components.rememberNestedScaffoldState
import com.magic.maw.ui.theme.TableLayout
import com.magic.maw.ui.theme.WaterLayout
import com.magic.maw.util.UiUtils.getStatusBarHeight
import com.magic.maw.util.UiUtils.hideSystemBars
import com.magic.maw.util.UiUtils.showSystemBars
import com.magic.maw.util.Logger
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val logger = Logger("PostScreen")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    uiState: PostUiState.Post,
    lazyState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    refreshState: PullToRefreshState = rememberPullToRefreshState(),
    scaffoldState: NestedScaffoldState = rememberNestedScaffoldState(),
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (Int) -> Unit,
) {
    val staggeredState = remember { mutableStateOf(false) }
    NestedScaffoldBody(
        uiState = uiState,
        lazyState = lazyState,
        refreshState = refreshState,
        staggeredState = staggeredState,
        scaffoldState = scaffoldState,
        openDrawer = openDrawer,
        openSearch = openSearch,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
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
    staggeredState: MutableState<Boolean>,
    openDrawer: () -> Unit = {},
    openSearch: () -> Unit = {},
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollToTop: () -> Unit = { scope.launch { lazyState.scrollToItem(0, 0) } }
    LaunchedEffect(scaffoldState) {
        if (scaffoldState.scrollValue == scaffoldState.minPx) {
            context.hideSystemBars()
        } else if (scaffoldState.scrollValue == scaffoldState.maxPx) {
            context.showSystemBars()
        }
    }
    NestedScaffold(
        topBar = { offset ->
            PostTopBar(
                modifier = Modifier
                    .offset { offset }
                    .shadow(5.dp),
                staggeredState = staggeredState,
                scrollToTop = scrollToTop,
                openDrawer = openDrawer,
                openSearch = openSearch
            )
        },
        state = scaffoldState,
        canScroll = {
            refreshState.distanceFraction <= 0 && uiState.dataList.isNotEmpty()
        },
        onScrollToTop = { context.hideSystemBars() },
        onScrollToBottom = { context.showSystemBars() },
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
            onItemClick = onItemClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostTopBar(
    modifier: Modifier = Modifier,
    staggeredState: MutableState<Boolean>,
    scrollToTop: () -> Unit = {},
    openDrawer: () -> Unit = {},
    openSearch: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.post)) },
        modifier = modifier.shadow(3.dp),
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "",
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    staggeredState.value = !staggeredState.value
                    scrollToTop()
                },
                modifier = Modifier.width(PostDefaults.ActionsIconWidth)
            ) {
                val imageVector = if (!staggeredState.value) TableLayout else WaterLayout
                Icon(imageVector = imageVector, contentDescription = "")
            }
            IconButton(
                onClick = openSearch,
                modifier = Modifier.width(PostDefaults.ActionsIconWidth)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "")
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
    staggeredState: MutableState<Boolean>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
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
                onLoadMore = onLoadMore,
                onItemClick = onItemClick
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
    staggeredState: MutableState<Boolean>,
    onItemClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {

        val columns = max((this.maxWidth / 210.dp).toInt(), 2)
        val contentPadding = getContentPadding(maxWidth = maxWidth, columns = columns)

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = state,
            contentPadding = PaddingValues(contentPadding)
        ) {
            itemsIndexed(uiState.dataList) { index, item ->
                PostItem(
                    modifier = Modifier
                        .padding(contentPadding)
                        .clickable {
                            if (uiState.type != UiStateType.Refresh) {
                                onItemClick.invoke(index)
                            }
                        },
                    postData = item,
                    staggered = staggeredState.value
                )
            }
            checkLoadMore(uiState = uiState, state = state, onLoadMore = onLoadMore)

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
        logger.warning("No suitable space found")
    }
    currentSpace.toDp() / 2
}

private fun checkLoadMore(
    uiState: PostUiState.Post,
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
            onLoadMore.invoke()
        }
    }
}

object PostDefaults {
    val ContentPadding: Dp = 3.5.dp
    val ActionsIconWidth: Dp = 40.dp
    val NoMoreItemHeight: Dp = 36.dp
}