package com.magic.maw.ui.post

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magic.maw.R
import com.magic.maw.ui.components.NestedScaffold
import com.magic.maw.ui.components.rememberNestedScaffoldState
import com.magic.maw.ui.theme.TableLayout
import com.magic.maw.ui.theme.WaterLayout
import com.magic.maw.util.UiUtils.hideSystemBars
import com.magic.maw.util.UiUtils.showSystemBars
import kotlinx.coroutines.launch
import kotlin.math.max

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
    AnimatedVisibility(
        visible = postViewModel.viewIndex < 0,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        PostScreen(
            postViewModel = postViewModel,
            isExpandedScreen = isExpandedScreen,
            onSystemBarsHide = onSystemBarsHide,                                                
            openDrawer = openDrawer
        )
    }
    AnimatedVisibility(
        visible = postViewModel.viewIndex >= 0,
        enter = fadeIn(),
        exit = fadeOut()
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
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    var currentOrientation by remember { mutableStateOf(isPortrait) }
    if (currentOrientation != isPortrait) {
        currentOrientation = isPortrait
        if (state.scrollValue == state.minPx) {
            onSystemBarsHide.invoke(true)
        } else if (state.scrollValue == state.maxPx) {
            onSystemBarsHide.invoke(false)
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
private fun ScaffoldBody(
    postViewModel: PostViewModel,
    lazyState: LazyStaggeredGridState,
    refreshState: PullToRefreshState,
    openDrawer: () -> Unit
) {
    val scrollToTop: () -> Unit = {
        postViewModel.viewModelScope.launch {
            lazyState.scrollToItem(0, 0)
        }
    }
    val topAppBarState = postViewModel.getState { rememberTopAppBarState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        state = topAppBarState,
        canScroll = { postViewModel.dataList.isNotEmpty() }
    )

    Scaffold(
        topBar = {
            PostTopBar(
                postViewModel = postViewModel,
                scrollToTop = scrollToTop,
                openDrawer = openDrawer,
                scrollBehavior = if (postViewModel.dataList.isNotEmpty()) scrollBehavior else null
            )
        }
    ) { innerPadding ->
        PostRefreshBody(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
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
            "正在加载"
        } else if (postViewModel.loadFailed) {
            "加载失败"
        } else {
            "没有数据"
        }
        Text(text = text, modifier = Modifier.clickable() {
            postViewModel.refresh()
        })
    }
}

@Composable
private fun PostBody(
    modifier: Modifier = Modifier,
    postViewModel: PostViewModel,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState()
) {
    BoxWithConstraints(modifier = modifier) {
        if (state.isScrollInProgress) {
            checkLoadMore(postViewModel, state)
        }

        val columns = max((maxWidth / 200.dp).toInt(), 2)
        val contentPadding = getContentPadding(columns = columns)

        StaggeredGridCells.Adaptive(200.dp)
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
                    staggered = postViewModel.staggered
                )
            }

            if (postViewModel.noMore) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = "没有更多数据",
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
private fun getContentPadding(columns: Int): Dp {
    val density = LocalDensity.current.density
    val defaultPadding = (PostDefaults.ContentPadding.value * density).toInt()
    val remainder = if (defaultPadding % columns >= columns / 2) columns else 0
    val targetPadding = defaultPadding / columns * columns + remainder
    return (targetPadding / density).dp
}

private fun checkRefresh(postViewModel: PostViewModel) {
    if (postViewModel.dataList.isEmpty() && !postViewModel.noMore && !postViewModel.refreshing) {
        postViewModel.refresh()
    }
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

@Preview("PostRouteExpanded", widthDp = 800, heightDp = 600)
@Composable
fun PostRoutePreview() {
    val postViewModel: PostViewModel = viewModel(factory = PostViewModel.providerFactory())
    PostRoute(postViewModel, true) {}
}

object PostDefaults {
    val ContentPadding: Dp = 3.5.dp
    val ActionsIconWidth: Dp = 40.dp
    val NoMoreItemHeight: Dp = 36.dp
}