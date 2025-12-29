package com.magic.maw.ui.popular

import androidx.collection.mutableIntIntMapOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import com.magic.maw.R
import com.magic.maw.data.PopularType
import com.magic.maw.data.SettingsService
import com.magic.maw.ui.main.AppRoute
import com.magic.maw.ui.post.PostDefaults
import com.magic.maw.ui.post.PostRefreshBody
import com.magic.maw.ui.post.RefreshScrollToTopChecker
import com.magic.maw.util.UiUtils
import com.magic.maw.util.UiUtils.getStatusBarHeight
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.launch

private const val TAG = "PopularScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopularScreen(
    modifier: Modifier = Modifier,
    viewModel: PopularViewModel2 = viewModel(),
    navController: NavController = rememberNavController(),
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
    viewModel: PopularViewModel2,
    pagerState: PagerState = rememberPopularPagerState(),
    navController: NavController = rememberNavController(),
    staggeredState: MutableState<Boolean>? = null,
) {
    val scope = rememberCoroutineScope()
    val supportedPopularDateTypes = getSupportedPopularDateTypes()
    val popularType by remember {
        derivedStateOf {
            try {
                supportedPopularDateTypes[pagerState.currentPage]
            } catch (_: okio.ArrayIndexOutOfBoundsException) {
                scope.launch { pagerState.scrollToPage(0) }
                supportedPopularDateTypes[0]
            }
        }
    }
    val currentDate by viewModel.getPopularDate(popularType).collectAsStateWithLifecycle()

    LaunchedEffect(popularType) {
        viewModel.setCurrentPopularType(popularType)
    }
    Box(
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PopularDateTypePicker(pagerState = pagerState)

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
            ) { index ->
                val popularType = try {
                    supportedPopularDateTypes[index]
                } catch (_: ArrayIndexOutOfBoundsException) {
                    LaunchedEffect(Unit) {
                        pagerState.scrollToPage(page = 0)
                    }
                    return@HorizontalPager
                }
                val loader = viewModel.getLoader(popularType)
                Logger.d(TAG) { "popularOption: ${loader.popularOption}" }
                val uiState by loader.uiState.collectAsStateWithLifecycle()
                val lazyState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
                val itemHeights = remember { mutableIntIntMapOf() }

                LaunchedEffect(staggeredState) {
                    lazyState.scrollToItem(0, 0)
                }

                RefreshScrollToTopChecker(items = uiState.items) {
                    lazyState.scrollToItem(0, 0)
                }

                PostRefreshBody(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    refreshState = rememberPullToRefreshState(),
                    lazyState = lazyState,
                    staggeredState = staggeredState,
                    onRefresh = { loader.refresh(true) },
                    onLoadMore = { loader.loadMore() },
                    onGloballyPositioned = { index, height -> itemHeights[index] = height },
                    onItemClick = {
                        Logger.d(TAG) { "onItemClick $it" }
                        loader.setViewIndex(it)
                        navController.navigate(route = AppRoute.PopularView(postId = it))
                    }
                )
            }
        }

        PopularDatePicker(
            modifier = Modifier.align(Alignment.BottomCenter),
            popularType = popularType,
            focusDate = currentDate,
            onDateChanged = { viewModel.setPopularDate(popularType, date = it) }
        )
    }
}

@Composable
fun rememberPopularPagerState() = rememberPagerState {
    BaseParser.get(SettingsService.settings.website).supportedPopularDateTypes.size
}

@Composable
fun getSupportedPopularDateTypes(): List<PopularType> {
    val settingState by SettingsService.settingsState.collectAsStateWithLifecycle()
    val types by remember {
        derivedStateOf {
            BaseParser.get(settingState.website).supportedPopularDateTypes
        }
    }
    return types
}