package com.magic.maw.ui.post

import androidx.activity.compose.BackHandler
import androidx.collection.mutableIntIntMapOf
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import com.magic.maw.R
import com.magic.maw.data.PostData
import com.magic.maw.data.loader.LoadState
import com.magic.maw.data.loader.PostDataLoader
import com.magic.maw.data.loader.PostDataUiState
import com.magic.maw.ui.components.EmptyView
import com.magic.maw.ui.components.LoadMoreChecker
import com.magic.maw.ui.components.RefreshScrollToTopChecker
import com.magic.maw.ui.components.ReturnedIndexChecker
import com.magic.maw.ui.main.AppRoute
import com.magic.maw.ui.main.MainNavRail
import com.magic.maw.ui.main.POST_INDEX
import com.magic.maw.ui.main.useNavRail
import com.magic.maw.util.UiUtils
import com.magic.maw.util.UiUtils.getStatusBarHeight
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "PostScreen"

@Composable
fun PostScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    onOpenDrawer: (() -> Unit)? = null
) {
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry<AppRoute.Post>()
    }
    val viewModel: PostViewModel = viewModel(parentEntry)
    val postIndex = backStackEntry.getPostIndex()
    Logger.d(TAG) { "parent entry: ${parentEntry.id}, viewModel: $viewModel, postIndex: $postIndex" }

    if (viewModel.loader.hasTags) {
        PostScreen(
            modifier = modifier,
            loader = viewModel.loader,
            navController = navController,
            postIndex = postIndex,
            negativeIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNegative = { navController.popBackStack() },
        )

        // WARNING:此处需要拦截返回键，如果不拦截会将旧的AppRoute.PostList出栈
        BackHandler {
            navController.popBackStack()
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail()) {
                MainNavRail(navController = navController, topRoute = AppRoute.Post())
            }
            PostScreen(
                modifier = modifier,
                loader = viewModel.loader,
                navController = navController,
                postIndex = postIndex,
                onNegative = onOpenDrawer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    modifier: Modifier = Modifier,
    loader: PostDataLoader = viewModel<PostViewModel>().loader,
    navController: NavController = rememberNavController(),
    lazyState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    refreshState: PullToRefreshState = rememberPullToRefreshState(),
    titleText: String = stringResource(R.string.post),
    postIndex: Int? = null,
    staggeredEnable: Boolean = true,
    shadowEnable: Boolean = true,
    searchEnable: Boolean = true,
    negativeIcon: ImageVector = Icons.Default.Menu,
    onNegative: (() -> Unit)? = null,
    onItemClick: (Int) -> Unit = {
        loader.setViewIndex(it)
        navController.navigate(route = AppRoute.PostView(postId = it))
    }
) {
    val scope = rememberCoroutineScope()
    val uiState by loader.uiState.collectAsStateWithLifecycle()
    val staggeredState = if (staggeredEnable) remember { mutableStateOf(false) } else null
    val scrollToTop: () -> Unit = {
        Logger.d(TAG) { "scrollToTop() called" }
        scope.launch { lazyState.scrollToItem(0, 0) }
    }
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = topAppBarState)
    val itemHeights = remember { mutableIntIntMapOf() }

    ReturnedIndexChecker(
        loader = loader,
        lazyState = lazyState,
        itemHeights = itemHeights,
        postIndex = postIndex
    )

    RefreshScrollToTopChecker(items = uiState.items, scrollToTop = scrollToTop)

    Scaffold(
        modifier = modifier,
        topBar = {
            PostTopBar(
                titleText = titleText,
                negativeIcon = negativeIcon,
                shadowEnable = shadowEnable,
                searchEnable = searchEnable,
                staggeredState = staggeredState,
                scrollToTop = scrollToTop,
                onNegative = onNegative,
                onSearch = { navController.navigate(route = AppRoute.PostSearch()) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        PostRefreshBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            uiState = uiState,
            refreshState = refreshState,
            lazyState = lazyState,
            staggeredState = staggeredState,
            onRefresh = { loader.refresh() },
            onLoadMore = { loader.loadMore() },
            onGloballyPositioned = { index, height -> itemHeights[index] = height },
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
    onNegative: (() -> Unit)? = null,
    onSearch: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = { Text(text = titleText) },
        modifier = modifier.let { if (shadowEnable) it.shadow(3.dp) else it },
        navigationIcon = {
            onNegative?.let { onNegative ->
                IconButton(onClick = onNegative) {
                    Icon(
                        imageVector = negativeIcon,
                        contentDescription = "",
                    )
                }
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
                    val id = if (!it.value) {
                        R.drawable.ic_table_layout
                    } else {
                        R.drawable.ic_water_layout
                    }
                    Icon(painter = painterResource(id), contentDescription = "")
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
        colors = UiUtils.topAppBarColors,
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRefreshBody(
    modifier: Modifier = Modifier,
    uiState: PostDataUiState,
    refreshState: PullToRefreshState,
    lazyState: LazyStaggeredGridState,
    staggeredState: MutableState<Boolean>? = null,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onGloballyPositioned: (Int, Int) -> Unit,
    onItemClick: (Int) -> Unit,
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
            PostBody(
                modifier = Modifier.fillMaxSize(),
                items = uiState.items,
                canClick = !isRefreshing,
                hasNoMore = uiState.hasNoMore,
                state = lazyState,
                staggeredState = staggeredState,
                onGloballyPositioned = onGloballyPositioned,
                onItemClick = onItemClick,
            )
        }
    }
}

@Composable
private fun PostBody(
    modifier: Modifier = Modifier,
    items: PersistentList<PostData>,
    hasNoMore: Boolean,
    canClick: Boolean,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    staggeredState: MutableState<Boolean>? = null,
    onGloballyPositioned: (Int, Int) -> Unit,
    onItemClick: (Int) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val columns = max((this.maxWidth / 210.dp).toInt(), 2)
        val contentPadding = getContentPadding(maxWidth = maxWidth, columns = columns)

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = state,
            contentPadding = PaddingValues(contentPadding)
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.id }
            ) { index, item ->
                PostItem(
                    modifier = Modifier
                        .padding(contentPadding)
                        .onGloballyPositioned { onGloballyPositioned(index, it.size.height) },
                    canClick = canClick,
                    onClick = { onItemClick(index) },
                    postData = item,
                    staggered = staggeredState?.value == true
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

fun NavBackStackEntry.getPostIndex(): Int? {
    val postIndex = savedStateHandle.getLiveData<Int>(POST_INDEX).value
    if (postIndex != null) {
        savedStateHandle.remove<Int>(POST_INDEX)
    }
    return postIndex
}

object PostDefaults {
    val ContentPadding: Dp = 3.5.dp
    val ActionsIconWidth: Dp = 40.dp
    val NoMoreItemHeight: Dp = 36.dp
}