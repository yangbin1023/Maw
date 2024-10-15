package com.magic.maw.ui.post

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.magic.maw.R
import com.magic.maw.ui.components.NestedScaffold
import com.magic.maw.ui.components.rememberNestedScaffoldState
import com.magic.maw.ui.theme.TableLayout
import com.magic.maw.ui.theme.WaterLayout
import com.magic.maw.util.UiUtils.getStatusBarHeight
import com.magic.maw.util.UiUtils.hideSystemBars
import com.magic.maw.util.UiUtils.showSystemBars
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "PostRoute"

@Composable
fun PostRoute(
    postViewModel: PostViewModel,
    isExpandedScreen: Boolean = false,
    openDrawer: () -> Unit,
) {
    val context = LocalContext.current
    var systemBarsHide by remember { mutableStateOf(false) }
    val onSystemBarsHide: (Boolean) -> Unit = {
        systemBarsHide = it
        if (systemBarsHide) {
            context.hideSystemBars()
        } else {
            context.showSystemBars()
        }
    }
    val showView by postViewModel.showView.collectAsState()
    val fadeIn = fadeIn(animationSpec = tween(500))
    val fadeOut = fadeOut(animationSpec = tween(500))
    AnimatedVisibility(
        visible = !showView,
        enter = fadeIn,
        exit = fadeOut
    ) {
        PostScreen(
            postViewModel = postViewModel,
            isExpandedScreen = isExpandedScreen,
            onSystemBarsHide = onSystemBarsHide,
            openDrawer = openDrawer
        )
    }
    AnimatedVisibility(
        visible = showView,
        enter = slideInHorizontally(animationSpec = tween(500), initialOffsetX = { it }) + fadeIn,
        exit = slideOutHorizontally(animationSpec = tween(500), targetOffsetX = { it }) + fadeOut
    ) {
        ViewScreen(
            postViewModel = postViewModel,
            isExpandedScreen = isExpandedScreen,
            systemBarsHide = { systemBarsHide },
            onSystemBarsHide = onSystemBarsHide
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostScreen(
    postViewModel: PostViewModel,
    isExpandedScreen: Boolean,
    onSystemBarsHide: (Boolean) -> Unit = {},
    openDrawer: () -> Unit,
) {
    val lazyState = postViewModel.getState { rememberLazyStaggeredGridState() }
    val refreshState = postViewModel.getState { rememberPullToRefreshState() }
    if (isExpandedScreen) {
        PostRefreshBody(
            postViewModel = postViewModel,
            refreshState = refreshState,
            lazyState = lazyState
        )
    } else {
        NestedScaffoldBody(
            postViewModel = postViewModel,
            lazyState = lazyState,
            refreshState = refreshState,
            onSystemBarsHide = onSystemBarsHide,
            openDrawer = openDrawer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NestedScaffoldBody(
    postViewModel: PostViewModel,
    lazyState: LazyStaggeredGridState,
    refreshState: PullToRefreshState,
    onSystemBarsHide: (Boolean) -> Unit = {},
    openDrawer: () -> Unit = {}
) {
    val state = postViewModel.getState { rememberNestedScaffoldState() }
    val scrollToTop: () -> Unit = {
        postViewModel.viewModelScope.launch {
            lazyState.scrollToItem(0, 0)
        }
    }
    LaunchedEffect(state) {
        if (state.scrollValue == state.minPx) {
            onSystemBarsHide.invoke(true)
        } else if (state.scrollValue == state.maxPx) {
            onSystemBarsHide.invoke(false)
        }
    }
    NestedScaffold(
        topBar = { offset ->
            PostTopBar(
                modifier = Modifier
                    .offset { offset }
                    .shadow(5.dp),
                postViewModel = postViewModel,
                scrollToTop = scrollToTop,
                openDrawer = openDrawer
            )
        },
        state = state,
        canScroll = {
            refreshState.distanceFraction <= 0 && postViewModel.dataList.isNotEmpty()
        },
        onScrollToTop = { onSystemBarsHide.invoke(true) },
        onScrollToBottom = { onSystemBarsHide.invoke(false) },
    ) { innerPadding ->
        PostRefreshBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            postViewModel = postViewModel,
            refreshState = refreshState,
            lazyState = lazyState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostTopBar(
    postViewModel: PostViewModel,
    modifier: Modifier = Modifier,
    enableShadow: Boolean = true,
    scrollToTop: () -> Unit = {},
    openDrawer: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.post)) },
        modifier = modifier.apply { if (enableShadow) shadow(5.dp) },
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
                    postViewModel.staggered = !postViewModel.staggered
                    scrollToTop()
                },
                modifier = Modifier.width(PostDefaults.ActionsIconWidth)
            ) {
                val imageVector = if (!postViewModel.staggered) TableLayout else WaterLayout
                Icon(imageVector = imageVector, contentDescription = "")
            }
            IconButton(
                onClick = {},
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
    postViewModel: PostViewModel,
    refreshState: PullToRefreshState,
    lazyState: LazyStaggeredGridState
) {
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = postViewModel.refreshing,
        onRefresh = { postViewModel.refresh() },
        state = refreshState
    ) {
        if (postViewModel.dataList.isEmpty()) {
            PostEmptyView(modifier = Modifier.fillMaxSize(), postViewModel = postViewModel)
        } else {
            PostBody(
                modifier = Modifier.fillMaxSize(),
                postViewModel = postViewModel,
                state = lazyState
            )
        }
    }
}

@Composable
private fun PostEmptyView(modifier: Modifier = Modifier, postViewModel: PostViewModel) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),//
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val text = if (postViewModel.refreshing) {
            stringResource(R.string.loading)
        } else if (postViewModel.loadFailed) {
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
                    onClick = {
                        if (!postViewModel.refreshing) {
                            postViewModel.refresh()
                        }
                    }
                )
        )
    }
}

@Composable
private fun PostBody(
    modifier: Modifier = Modifier,
    postViewModel: PostViewModel,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState()
) {
    BoxWithConstraints(modifier = modifier) {

        val columns = max((maxWidth / 210.dp).toInt(), 2)
        val contentPadding = getContentPadding(maxWidth = maxWidth, columns = columns)

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = state,
            contentPadding = PaddingValues(contentPadding)
        ) {
            itemsIndexed(postViewModel.dataList) { index, item ->
                PostItem(
                    modifier = Modifier
                        .padding(contentPadding)
                        .clickable { postViewModel.viewIndex = index },
                    postData = item,
                    postViewModel = postViewModel,
                    staggered = postViewModel.staggered
                )
                if ((postViewModel.dataList.size - index) < columns * 3) {
                    checkLoadMore(postViewModel, state)
                }
            }

            if (postViewModel.noMore) {
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
        Log.w(TAG, "No suitable space found")
    }
    currentSpace.toDp() / 2
}

private fun checkLoadMore(postViewModel: PostViewModel, state: LazyStaggeredGridState) {
    if (postViewModel.noMore || postViewModel.loading) {
        return
    }
    val totalItemsCount = state.layoutInfo.totalItemsCount
    val visibleItemsInfo = state.layoutInfo.visibleItemsInfo
    if (visibleItemsInfo.isNotEmpty()) {
        val lastVisibleIndex = visibleItemsInfo.last().index
        if ((totalItemsCount - lastVisibleIndex) < visibleItemsInfo.size * 1.5) {
            postViewModel.loadMore()
        }
    }
}

object PostDefaults {
    val ContentPadding: Dp = 3.5.dp
    val ActionsIconWidth: Dp = 40.dp
    val NoMoreItemHeight: Dp = 36.dp
}