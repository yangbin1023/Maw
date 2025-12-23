package com.magic.maw.ui.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.magic.maw.data.PostData
import com.magic.maw.data.TagInfo
import com.magic.maw.ui.components.RegisterView
import com.magic.maw.ui.components.changeSystemBarStatus
import com.magic.maw.ui.main.AppRoute
import com.magic.maw.ui.main.POST_INDEX
import com.magic.maw.ui.post.PostUiState
import com.magic.maw.ui.post.PostViewModel
import com.magic.maw.ui.theme.ViewDetailBarFold
import com.magic.maw.ui.theme.ViewDetailBarHalfFold
import com.magic.maw.util.UiUtils
import com.magic.maw.util.UiUtils.isShowStatusBars
import kotlinx.coroutines.delay

private const val viewName = "View"

@Composable
fun ViewScreen(
    uiState: PostUiState.View,
    pagerState: PagerState,
    onLoadMore: () -> Unit,
    onExit: () -> Unit,
    onTagClick: (TagInfo, Boolean) -> Unit
) {
    val topBarMaxHeight = UiUtils.getTopBarHeight()
    val context = LocalContext.current
    var showTopBar by remember { mutableStateOf(context.isShowStatusBars()) }
    val offsetValue = if (showTopBar) topBarMaxHeight else 0.dp
    val onTap: () -> Unit = { showTopBar = !showTopBar }
    val playerState = remember { VideoPlayerState(context = context) }
    val topAppBarOffset by animateDpAsState(
        targetValue = offsetValue - topBarMaxHeight,
        label = "showTopAppBar"
    )
    LaunchedEffect(Unit) {
        delay(1500)
        showTopBar = false
    }
    DisposableEffect(Unit) {
        onDispose { playerState.release() }
    }
    RegisterView(name = viewName, showTopBar)

    LaunchedEffect(showTopBar) {
        changeSystemBarStatus(context, viewName, showTopBar)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val postData = try {
            uiState.dataList[pagerState.currentPage]
        } catch (_: Throwable) {
            onExit()
            return@BoxWithConstraints
        }
        val draggableHeight = this.maxHeight - offsetValue

        ViewContent(
            pagerState = pagerState,
            dataList = uiState.dataList,
            playerState = playerState,
            onLoadMore = onLoadMore,
            onExit = onExit,
            onTab = onTap
        )

        ViewTopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .offset {
                    val y = topAppBarOffset.toPx()
                    IntOffset(0, y.toInt())
                },
            postData = postData,
            onExit = onExit
        )

        ViewDetailBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            postData = postData,
            isScrollInProgress = pagerState.isScrollInProgress,
            playerState = playerState,
            maxDraggableHeight = draggableHeight,
            onTagClick = onTagClick
        )
    }
}

@Composable
fun ViewScreen(
    modifier: Modifier = Modifier,
    viewModel: PostViewModel = viewModel(),
    navController: NavController = rememberNavController(),
    postIndex: Int = 0,
) {
    val uiState by viewModel.loader.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = postIndex) { uiState.items.size }
    val context = LocalContext.current
    val playerState = remember { VideoPlayerState(context = context) }
    val onExit: () -> Unit = {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set(POST_INDEX, pagerState.currentPage)
        navController.popBackStack()
    }
    val onLoadMore: () -> Unit = {
        viewModel.loadMore()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val postData = try {
            uiState.items[pagerState.currentPage]
        } catch (_: Throwable) {
            onExit()
            return@BoxWithConstraints
        }
        val draggableHeight = this.maxHeight

        ViewContent(
            pagerState = pagerState,
            dataList = uiState.items,
            playerState = playerState,
            onLoadMore = onLoadMore,
            onExit = onExit,
//            onTab = onTap
        )

        ViewTopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
            /*.offset {
                val y = topAppBarOffset.toPx()
                IntOffset(0, y.toInt())
            }*/,
            postData = postData,
            onExit = onExit
        )

        ViewDetailBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            postData = postData,
            isScrollInProgress = pagerState.isScrollInProgress,
            playerState = playerState,
            maxDraggableHeight = draggableHeight,
            onTagClick = { navController.navigate(route = AppRoute.PostSearch(text = it.name)) },
            onSearchTag = {
                viewModel.search(text = it.name)
                navController.navigate(route = AppRoute.Post) { popUpTo(route = AppRoute.Post) }
            }
        )
    }

    BackHandler(onBack = onExit)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewTopBar(
    modifier: Modifier = Modifier,
    postData: PostData,
    onExit: () -> Unit
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = { Text(text = postData.id.toString()) },
        colors = ViewScreenDefaults.topBarColors(),
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "",
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
object ViewScreenDefaults {
    private const val TOP_BAR_COLORS_ALPHA = 0.75f
    private var defaultTopBarColorsCache: TopAppBarColors? = null
    private val ColorScheme.defaultTopBarColors: TopAppBarColors
        get() {
            return defaultTopBarColorsCache ?: TopAppBarColors(
                containerColor = surface.copy(alpha = TOP_BAR_COLORS_ALPHA),
                scrolledContainerColor = surfaceContainer.copy(alpha = TOP_BAR_COLORS_ALPHA),
                navigationIconContentColor = onSurface.copy(alpha = TOP_BAR_COLORS_ALPHA),
                titleContentColor = onSurface.copy(alpha = TOP_BAR_COLORS_ALPHA),
                actionIconContentColor = onSurfaceVariant.copy(alpha = TOP_BAR_COLORS_ALPHA),
                subtitleContentColor = onSurface.copy(alpha = TOP_BAR_COLORS_ALPHA),
            ).also { defaultTopBarColorsCache = it }
        }

    @Composable
    fun topBarColors(): TopAppBarColors {
        LaunchedEffect(isSystemInDarkTheme()) {
            defaultTopBarColorsCache = null
        }
        return MaterialTheme.colorScheme.defaultTopBarColors
    }

    val detailBarFoldColor = Brush.verticalGradient(
        listOf(Color.Transparent, ViewDetailBarHalfFold, ViewDetailBarFold)
    )
}