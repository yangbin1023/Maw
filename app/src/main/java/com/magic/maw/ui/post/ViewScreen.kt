package com.magic.maw.ui.post

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.magic.maw.R
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import com.magic.maw.data.TagInfo
import com.magic.maw.data.TagType
import com.magic.maw.ui.components.ScrollableView
import com.magic.maw.ui.components.ScrollableViewState
import com.magic.maw.ui.components.SettingItem
import com.magic.maw.ui.components.TagItem
import com.magic.maw.ui.components.rememberScrollableViewState
import com.magic.maw.ui.theme.ViewDetailBarExpand
import com.magic.maw.ui.theme.ViewDetailBarFold
import com.magic.maw.util.UiUtils
import com.magic.maw.util.configFlow
import com.magic.maw.website.LoadStatus
import com.magic.maw.website.TagManager
import com.magic.maw.website.loadDLFileWithTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs

@Composable
fun ViewScreen(
    postViewModel: PostViewModel,
    isExpandedScreen: Boolean,
    systemBarsHide: () -> Boolean = { false },
    onSystemBarsHide: (Boolean) -> Unit = {}
) {
    val pagerState = rememberPagerState(postViewModel.viewIndex) { postViewModel.dataList.size }
    val onExit: () -> Unit = {
        postViewModel.showView.update { false }
    }
    val topBarMaxHeight = UiUtils.getTopBarHeight()
    var topAppBarHide by remember { mutableStateOf(systemBarsHide.invoke()) }
    val targetOffset = if (topAppBarHide) -topBarMaxHeight else 0.dp
    val topAppBarOffset by animateDpAsState(
        targetValue = targetOffset,
        label = "showTopAppBar"
    )
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
        ViewDetailBar(
            postViewModel = postViewModel,
            pagerState = pagerState,
            maxDraggableHeight = maxHeight - topBarMaxHeight - targetOffset
        )
        BackHandler(enabled = postViewModel.showView.collectAsState().value) { onExit.invoke() }
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
        if (abs(pagerState.currentPage - index) > 1) {
            return@HorizontalPager
        }
        if (postViewModel.dataList.size - index < 5) {
            postViewModel.loadMore()
        }
        val data = postViewModel.dataList[index]
        val info = data.getInfo(data.quality) ?: let {
            data.quality = Quality.File
            data.originalInfo
        }
        ViewScreenItem(data, info, data.quality, onTab)
    }
}

private suspend fun loadModel(context: Context, file: File): Pair<Any?, Size?> {
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
            onSuccess = { c.resume(it) },
            onError = { c.resume(null) }
        )
        .build()
    context.imageLoader.enqueue(imageRequest)
}

@Composable
private fun ViewScreenItem(
    data: PostData, info: PostData.Info, quality: Quality,
    onTab: () -> Unit = {},
) {
    val task = loadDLFileWithTask(data, quality)
    val status by task.statusFlow.collectAsState()
    if (status !is LoadStatus.Success) {
        LaunchedEffect(data, quality) { task.start() }
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val size = Size(info.width.toFloat(), info.height.toFloat())
    var model by remember { mutableStateOf<Pair<Any?, Size?>>(Pair(null, size)) }
    var retryCount by remember { mutableIntStateOf(0) }
    when (status) {
        is LoadStatus.Waiting -> LoadingView()
        is LoadStatus.Loading -> LoadingView { (status as? LoadStatus.Loading)?.progress ?: 0.99f }
        is LoadStatus.Error -> ErrorPlaceHolder(onRetry = { retryCount++ })
        is LoadStatus.Success -> {
            retryCount = 0
            if (model.first == null && model.second == null) {
                // 解码失败
                ErrorPlaceHolder()
            } else if (model.first == null) {
                LaunchedEffect(status) {
                    model = loadModel(context, (status as LoadStatus.Success<File>).result)
                }
                LoadingView()
            } else if (model.second != null) {
                val zoomableViewState = rememberZoomableState(contentSize = model.second)
                val processor = if (model.first is SamplingDecoder) {
                    ModelProcessor(samplingProcessorPair)
                } else {
                    ModelProcessor()
                }
                DisposableEffect(Unit) { onDispose { (model.first as? SamplingDecoder)?.release() } }
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
            }
        }
    }
}

@Composable
private fun ErrorPlaceHolder(onRetry: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onRetry
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val color = MaterialTheme.colorScheme.onSurface.copy(0.4F)
        Icon(
            modifier = Modifier
                .size(40.dp),
            imageVector = Icons.Filled.Error,
            tint = color,
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(R.string.loading_failed), color = color)
    }
}

@Composable
private fun LoadingView(progress: (() -> Float)? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        progress?.let {
            CircularProgressIndicator(progress = it)
        } ?: CircularProgressIndicator()
    }
}

@Composable
private fun BoxScope.ViewDetailBar(
    modifier: Modifier = Modifier,
    postViewModel: PostViewModel,
    pagerState: PagerState,
    maxDraggableHeight: Dp,
) {
    val scrollableViewState = rememberScrollableViewState()
    scrollableViewState.updateData(
        density = LocalDensity.current,
        maxDraggableHeight = maxDraggableHeight
    )
    ScrollableView(
        modifier = modifier
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
    val scope = rememberCoroutineScope()
    val config by configFlow.collectAsState()
    val tagManager = TagManager.get(config.source)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .verticalScroll(rememberScrollState())
    ) {
        ContentHeader(stringResource(R.string.tags))

        val data = postViewModel.dataList[pagerState.currentPage]
        var tagChanged = false
        for (tagInfo in data.tags) {
            if (tagInfo.type == TagType.None) {
                val info by tagManager.getInfoStatus(tagInfo.name, scope).collectAsState()
                (info as? LoadStatus.Success)?.let {
                    data.updateTag(it.result)
                    tagChanged = true
                }
            }
        }
        if (tagChanged) {
            data.tags.sort()
            tagChanged = false
        }
        for (tagInfo in data.tags) {
            TagInfoItem(info = tagInfo)
        }

        ContentHeader(stringResource(R.string.others))

        SettingItem(title = stringResource(R.string.author), tips = data.uploader)
        SettingItem(title = stringResource(R.string.rating), tips = data.rating.name)
//        SettingItem(title = stringResource(R.string.source), tips = data.srcUrl)
    }
}

@Composable
private fun ContentHeader(title: String) {
    Text(
        modifier = Modifier.padding(top = 10.dp, start = 15.dp, end = 15.dp, bottom = 5.dp),
        text = title,
        fontSize = 20.sp
    )
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
        thickness = 2.dp
    )
}

@Composable
private fun TagInfoItem(
    modifier: Modifier = Modifier,
    info: TagInfo,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(start = 5.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            TagItem(
                text = info.name,
                type = info.type,
                onClick = onClick
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = Color.White,
                    shape = CircleShape
                )
                .padding(vertical = 2.dp, horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(21.dp),
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.White
            )
            Text(
                modifier = Modifier.padding(horizontal = 1.dp),
                text = info.count.toString(),
                color = Color.White,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
@Preview
private fun TagInfoItemPreview() {
    Column {
        ContentHeader(stringResource(R.string.tags))
        TagInfoItem(
            info = TagInfo(
                id = 10,
                source = "yande",
                name = "tokifokiz_bosotto_roshia-go_deklahsdf_hfkeu-nasfhuhsfjieadsf",
                type = TagType.Artist,
                count = 64810
            )
        )
        TagInfoItem(
            info = TagInfo(
                id = 10,
                source = "yande",
                name = "tokifokiz_bosottosf",
                type = TagType.Copyright,
                count = 6516
            )
        )
    }
}