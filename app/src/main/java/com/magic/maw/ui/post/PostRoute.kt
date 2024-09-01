package com.magic.maw.ui.post

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
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magic.maw.R
import com.magic.maw.ui.NestedScaffold
import com.magic.maw.ui.theme.TableLayout
import com.magic.maw.ui.theme.WaterLayout
import kotlinx.coroutines.launch
import kotlin.math.max

private const val TAG = "PostRoute"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRoute(
    postViewModel: PostViewModel,
    isExpandedScreen: Boolean = false,
    openDrawer: () -> Unit,
) {
    val lazyState = rememberLazyStaggeredGridState()
    val refreshState = rememberPullToRefreshState()
    if (isExpandedScreen) {
        PostRefreshBody(
            postViewModel = postViewModel,
            refreshState = refreshState,
            lazyState = lazyState
        )
    } else {
        val scrollToTop: () -> Unit = {
            postViewModel.viewModelScope.launch {
                lazyState.scrollToItem(0, 0)
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
            isEnable = {
                refreshState.distanceFraction <= 0 && postViewModel.dataList.isNotEmpty()
            }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostTopBar(
    postViewModel: PostViewModel,
    modifier: Modifier = Modifier,
    enableShadow: Boolean = true,
    scrollToTop: () -> Unit = {},
    openDrawer: () -> Unit = {}
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
        }
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
        Text(text = "没有数据", modifier = Modifier.clickable() {
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
            items(postViewModel.dataList) {
                PostItem(
                    modifier = Modifier.padding(contentPadding),
                    staggered = postViewModel.staggered,
                    postData = it
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