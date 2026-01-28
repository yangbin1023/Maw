package com.magic.maw.ui.features.popular

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.paging.compose.collectAsLazyPagingItems
import co.touchlab.kermit.Logger
import com.magic.maw.R
import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.ui.common.ReturnedIndexChecker
import com.magic.maw.ui.features.main.AppRoute
import com.magic.maw.ui.features.post.PostDefaults
import com.magic.maw.ui.features.post.PostRefreshBody
import com.magic.maw.util.UiUtils
import com.magic.maw.util.UiUtils.getStatusBarHeight
import kotlinx.coroutines.launch

private const val TAG = "PopularScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopularScreen(
    modifier: Modifier = Modifier,
    viewModel: PopularViewModel = viewModel(),
    navController: NavController = rememberNavController(),
    postIndex: Int? = null,
    onNegative: (() -> Unit)? = null,
) {
    val staggeredState = remember { mutableStateOf(false) }
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = topAppBarState)

    Scaffold(
        modifier = modifier,
        topBar = {
            PopularTopBar(
                staggeredState = staggeredState,
                scrollToTop = { viewModel.itemScrollToTop() },
                onNegative = onNegative,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        PopularBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            viewModel = viewModel,
            navController = navController,
            staggeredState = staggeredState,
            postIndex = postIndex
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PopularTopBar(
    modifier: Modifier = Modifier,
    titleText: String = stringResource(id = R.string.popular),
    negativeIcon: ImageVector = Icons.Default.Menu,
    shadowEnable: Boolean = false,
    staggeredState: MutableState<Boolean>? = null,
    scrollToTop: () -> Unit = {},
    onNegative: (() -> Unit)? = null,
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
        },
        windowInsets = WindowInsets(top = LocalContext.current.getStatusBarHeight()),
        colors = UiUtils.topAppBarColors,
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PopularBody(
    modifier: Modifier = Modifier,
    viewModel: PopularViewModel,
    pagerState: PagerState = rememberPopularPagerState(viewModel),
    navController: NavController = rememberNavController(),
    staggeredState: MutableState<Boolean>? = null,
    postIndex: Int? = null,
) {
    val popularType = getCurrentPopularDateType(pagerState, viewModel)
    val currentData = viewModel.getItemData(popularType)
    val currentDate by currentData.localDateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(popularType) {
        viewModel.setCurrentPopularType(popularType)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    LaunchedEffect(lifecycleOwner) {
        if (lifecycleState == Lifecycle.State.STARTED
            || lifecycleState == Lifecycle.State.RESUMED
        ) {
            viewModel.checkAndRefresh()
        }
    }

    Box(
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PopularDateTypePicker(
                pagerState = pagerState,
                viewModel = viewModel
            )

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
            ) { index ->
                PopularPagerItemBody(
                    index = index,
                    viewModel = viewModel,
                    navController = navController,
                    pagerState = pagerState,
                    staggeredState = staggeredState,
                    postIndex = postIndex
                )
            }
        }

        PopularDatePicker(
            modifier = Modifier.align(Alignment.BottomCenter),
            popularType = popularType,
            focusDate = currentDate,
            onDateChanged = { currentData.setPopularDate(it) }
        )
    }
}

@Composable
private fun PopularPagerItemBody(
    index: Int,
    viewModel: PopularViewModel,
    navController: NavController,
    pagerState: PagerState,
    staggeredState: MutableState<Boolean>? = null,
    postIndex: Int? = null,
) {
    val supportedPopularDateTypes by viewModel.currentPopularTypes.collectAsStateWithLifecycle()
    val popularType = try {
        supportedPopularDateTypes[index]
    } catch (_: ArrayIndexOutOfBoundsException) {
        LaunchedEffect(Unit) {
            pagerState.scrollToPage(page = 0)
        }
        return
    }
    val itemData = viewModel.getItemData(popularType)
    val lazyPagingItems = itemData.dataSource.dataFlow.collectAsLazyPagingItems()

    if (index == pagerState.currentPage) {
        ReturnedIndexChecker(
            lazyState = itemData.lazyState,
            itemHeights = itemData.itemHeights,
            postIndex = postIndex
        )
    }

    PostRefreshBody(
        modifier = Modifier.fillMaxSize(),
        lazyPagingItems = lazyPagingItems,
        refreshState = rememberPullToRefreshState(),
        lazyState = itemData.lazyState,
        staggeredState = staggeredState,
        onRefresh = { lazyPagingItems.refresh() },
        onGloballyPositioned = { index, height -> itemData.itemHeights[index] = height },
        onItemClick = {
            Logger.d(TAG) { "onItemClick $it" }
            navController.navigate(route = AppRoute.PopularViewer(postIndex = it))
        }
    )
}

@Composable
private fun rememberPopularPagerState(viewModel: PopularViewModel): PagerState {
    val currentPopularTypes by viewModel.currentPopularTypes.collectAsStateWithLifecycle()
    return remember(currentPopularTypes) {
        PagerState(pageCount = { currentPopularTypes.size })
    }
}

@Composable
private fun getCurrentPopularDateType(pagerState: PagerState, viewModel: PopularViewModel): PopularType {
    val scope = rememberCoroutineScope()
    val currentPopularTypes by viewModel.currentPopularTypes.collectAsStateWithLifecycle()
    val popularType by remember {
        derivedStateOf {
            try {
                currentPopularTypes[pagerState.currentPage]
            } catch (_: okio.ArrayIndexOutOfBoundsException) {
                scope.launch { pagerState.scrollToPage(0) }
                currentPopularTypes[0]
            }
        }
    }
    return popularType
}