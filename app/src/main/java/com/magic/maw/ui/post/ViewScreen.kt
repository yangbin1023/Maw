package com.magic.maw.ui.post

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import com.jvziyaoyao.scale.image.sampling.SamplingDecoder
import com.jvziyaoyao.scale.image.sampling.createSamplingDecoder
import com.jvziyaoyao.scale.image.sampling.samplingProcessorPair
import com.jvziyaoyao.scale.image.viewer.ImageViewer
import com.jvziyaoyao.scale.image.viewer.ModelProcessor
import com.jvziyaoyao.scale.zoomable.zoomable.ZoomableGestureScope
import com.jvziyaoyao.scale.zoomable.zoomable.rememberZoomableState
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import com.magic.maw.ui.components.ScrollableView
import com.magic.maw.ui.components.ScrollableViewState
import com.magic.maw.ui.components.rememberScrollableViewState
import com.magic.maw.ui.theme.ViewDetailBarExpand
import com.magic.maw.ui.theme.ViewDetailBarFold
import com.magic.maw.util.UiUtils
import com.magic.maw.website.loadDLFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun ViewScreen(
    postViewModel: PostViewModel,
    isExpandedScreen: Boolean,
    systemBarsHide: () -> Boolean = { false },
    onSystemBarsHide: (Boolean) -> Unit = {}
) {
    val pagerState = rememberPagerState(postViewModel.viewIndex) { postViewModel.dataList.size }
    val onExit: () -> Unit = {
        postViewModel.viewIndex = -1
    }
    val topBarMaxHeight = UiUtils.getTopBarHeight()
    var topAppBarHide by remember { mutableStateOf(systemBarsHide.invoke()) }
    val targetOffset = if (topAppBarHide) -topBarMaxHeight else 0.dp
    val topAppBarOffset by animateDpAsState(
        targetValue = targetOffset,
        label = "showTopAppBar"
    )
    val scrollableViewState = rememberScrollableViewState()
    LaunchedEffect(Unit) {
        delay(1500)
        topAppBarHide = true
    }
    LaunchedEffect(topAppBarHide) {
        onSystemBarsHide.invoke(topAppBarHide)
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        ViewContent(
            postViewModel = postViewModel,
            pagerState = pagerState,
            onExit = onExit,
            onTab = { topAppBarHide = !topAppBarHide }
        )
        ViewTopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .offset {
                    val y = topAppBarOffset.toPx()
                    IntOffset(0, y.toInt())
                }
                .shadow(5.dp),
            postViewModel = postViewModel,
            pagerState = pagerState,
            onExit = onExit
        )
        scrollableViewState.updateData(
            density = LocalDensity.current,
            maxDraggableHeight = maxHeight - topBarMaxHeight - targetOffset
        )
        ScrollableView(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            state = scrollableViewState,
            toolbarModifier = Modifier.let {
                if (scrollableViewState.expand) {
                    it.background(ViewDetailBarExpand)
                } else {
                    it.background(
                        Brush.verticalGradient(listOf(Color.Transparent, ViewDetailBarFold))
                    )
                }
            },
            toolbar = {
                ScrollableBar(
                    postViewModel = postViewModel,
                    pagerState = pagerState,
                    scrollableViewState = it
                )
            },
            contentModifier = Modifier.background(ViewDetailBarFold),
            content = {
                ScrollableContent(
                    postViewModel = postViewModel,
                    pagerState = pagerState
                )
            }
        )
        BackHandler(enabled = postViewModel.viewIndex >= 0) { onExit.invoke() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewTopBar(
    modifier: Modifier = Modifier,
    postViewModel: PostViewModel,
    pagerState: PagerState,
    onExit: () -> Unit
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            val index = pagerState.currentPage
            if (index >= 0 && index < postViewModel.dataList.size) {
                val data = postViewModel.dataList[index]
                Text(text = data.id.toString())
            }
        },
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

@Composable
private fun BoxScope.ViewContent(
    postViewModel: PostViewModel,
    pagerState: PagerState,
    onExit: () -> Unit,
    onTab: () -> Unit = {},
) {
    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .align(Alignment.Center),
    ) { index ->
        if (index >= postViewModel.dataList.size || index < 0) {
            onExit.invoke()
            return@HorizontalPager
        }
        val data = postViewModel.dataList[index]
        val info = data.getInfo(data.quality) ?: let {
            data.quality = Quality.File
            data.originalInfo
        }
        ViewScreenItem(data, info, data.quality, onTab)
    }
}

private suspend fun loadModel(
    context: Context,
    data: PostData,
    info: PostData.Info,
    quality: Quality
): Pair<Any?, Size?> {
    val file = loadDLFile(data, info, quality) ?: return Pair(null, null)
    try {
        createSamplingDecoder(file)?.let {
            return Pair(it, it.intrinsicSize)
        }
    } catch (e: Exception) {
        println(e)
    }
    loadPainter(context, file)?.let {
        val painter = BitmapPainter(it.toBitmap().asImageBitmap())
        return Pair(painter, painter.intrinsicSize)
    }
    return Pair(null, null)
}

private suspend fun loadPainter(context: Context, data: Any) = suspendCoroutine { c ->
    val imageRequest = ImageRequest.Builder(context)
        .data(data)
        .size(coil.size.Size.ORIGINAL)
        .target(
            onSuccess = {
                c.resume(it)
            },
            onError = {
                c.resume(null)
            }
        )
        .build()
    context.imageLoader.enqueue(imageRequest)
}

@Composable
private fun ViewScreenItem(
    data: PostData, info: PostData.Info, quality: Quality,
    onTab: () -> Unit = {},
) {
    val size = Size(info.width.toFloat(), info.height.toFloat())
    var model by remember { mutableStateOf<Pair<Any?, Size?>>(Pair(null, size)) }
    var loadFailed by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        model = loadModel(context, data, info, quality)
        loadFailed = model.first == null
        println("model: $model")
    }
    DisposableEffect(Unit) { onDispose { (model.first as? SamplingDecoder)?.release() } }
    val coroutineScope = rememberCoroutineScope()
    if (loadFailed) {
        ErrorPlaceHolder()
    } else if (model.first != null) {
        val zoomableViewState = rememberZoomableState(contentSize = model.second)
        val processor = if (model.first is SamplingDecoder) {
            ModelProcessor(samplingProcessorPair)
        } else {
            ModelProcessor()
        }
        ImageViewer(
            model = model.first,
            state = zoomableViewState,
            processor = processor,
            detectGesture = ZoomableGestureScope(onDoubleTap = {
                coroutineScope.launch { zoomableViewState.toggleScale(it) }
            }, onTap = {
                onTab.invoke()
            })
        )
    } else {
        LoadingView()
    }
}

@Composable
private fun ErrorPlaceHolder() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val color = Color.White.copy(0.4F)
        Icon(
            modifier = Modifier
                .size(40.dp),
            imageVector = Icons.Filled.Error,
            tint = color,
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "图片加载失败", color = color)
    }
}

@Composable
private fun LoadingView(progress: Float? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        progress?.let {
            CircularProgressIndicator(progress = { it })
        } ?: CircularProgressIndicator()
    }
}

@Composable
private fun BoxScope.ScrollableBar(
    modifier: Modifier = Modifier,
    postViewModel: PostViewModel,
    pagerState: PagerState,
    scrollableViewState: ScrollableViewState,
) {
    Box(
        modifier = modifier
            .align(Alignment.Center)
            .padding(horizontal = 15.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        val scope = rememberCoroutineScope()
        val expandIconDegree by animateFloatAsState(
            targetValue = if (scrollableViewState.expand) 90f else 270f, label = ""
        )
        Icon(
            modifier = Modifier
                .rotate(expandIconDegree)
                .height(40.dp)
                .width(40.dp)
                .align(Alignment.CenterEnd)
                .clickable {
                    scrollableViewState.expand = !scrollableViewState.expand
                    scope.launch { scrollableViewState.animateToExpand() }
                },
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null
        )

        if (postViewModel.dataList.size <= pagerState.currentPage)
            return
        val data = postViewModel.dataList[pagerState.currentPage]
        val info = data.originalInfo
        val quality = data.quality
        val context = LocalContext.current
        val text = "${data.id} (${info.width}x${info.height}) ${quality.toResString(context)}"
        Text(
            modifier = Modifier.align(Alignment.CenterStart),
            text = text
        )
    }
}

@Composable
private fun BoxScope.ScrollableContent(
    modifier: Modifier = Modifier,
    postViewModel: PostViewModel,
    pagerState: PagerState,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {

    }
}