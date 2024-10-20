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
import com.magic.maw.ui.components.RememberSystemBars
import com.magic.maw.ui.components.ScrollableView
import com.magic.maw.ui.components.SettingItem
import com.magic.maw.ui.components.TagItem
import com.magic.maw.ui.components.rememberScrollableViewState
import com.magic.maw.ui.theme.ViewDetailBarExpand
import com.magic.maw.ui.theme.ViewDetailBarFold
import com.magic.maw.util.UiUtils
import com.magic.maw.util.UiUtils.isShowStatusBars
import com.magic.maw.util.UiUtils.showSystemBars
import com.magic.maw.util.configFlow
import com.magic.maw.website.LoadStatus
import com.magic.maw.website.TagManager
import com.magic.maw.website.loadDLFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs

@Composable
fun ViewScreen(
    uiState: PostUiState.View,
    onLoadMore: () -> Unit,
    onExit: () -> Unit,
    onTagClick: (TagInfo, Boolean) -> Unit
) {
    val pagerState = rememberPagerState(uiState.initIndex) { uiState.dataList.size }
    val topBarMaxHeight = UiUtils.getTopBarHeight()
    val context = LocalContext.current
    var showTopBar by remember { mutableStateOf(context.isShowStatusBars()) }
    val topAppBarOffset by animateDpAsState(
        targetValue = if (showTopBar) 0.dp else -topBarMaxHeight,
        label = "showTopAppBar"
    )
    RememberSystemBars()
    LaunchedEffect(Unit) {
        delay(1500)
        showTopBar = false
    }
    LaunchedEffect(showTopBar) {
        context.showSystemBars(showTopBar)
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val postData = try {
            uiState.dataList[pagerState.currentPage]
        } catch (_: Throwable) {
            onExit.invoke()
            return@BoxWithConstraints
        }
        ViewContent(
            pagerState = pagerState,
            dataList = uiState.dataList,
            onLoadMore = onLoadMore,
            onExit = onExit,
            onTab = { showTopBar = !showTopBar }
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
            postData = postData,
            onExit = onExit
        )
        ViewDetailBar(
            postData = postData,
            maxDraggableHeight = if (showTopBar) maxHeight - topBarMaxHeight else maxHeight,
            onTagClick = onTagClick
        )
        BackHandler { onExit.invoke() }
    }
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
        title = {
            Text(text = postData.id.toString())
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
    pagerState: PagerState,
    dataList: List<PostData>,
    onLoadMore: () -> Unit,
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
        if (index >= dataList.size || index < 0) {
            onExit.invoke()
            return@HorizontalPager
        }
        if (abs(pagerState.settledPage - index) > 1) {
            return@HorizontalPager
        }
        if (dataList.size - index < 5) {
            onLoadMore()
        }
        val data = dataList[index]
        val info = data.getInfo(data.quality) ?: let {
            data.quality = Quality.File
            data.originalInfo
        }
        ViewScreenItem(
            data = data,
            info = info,
            quality = data.quality,
            reset = pagerState.settledPage != index,
            onTab = onTab
        )
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
    data: PostData,
    info: PostData.Info,
    quality: Quality,
    reset: Boolean = false,
    onTab: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val status by loadDLFile(data, quality, coroutineScope).collectAsState()
    val context = LocalContext.current
    val size = Size(info.width.toFloat(), info.height.toFloat())
    var model by remember { mutableStateOf<Pair<Any?, Size?>>(Pair(null, size)) }
    var retryCount by remember { mutableIntStateOf(0) }
    when (status) {
        is LoadStatus.Waiting -> LoadingView()
        is LoadStatus.Loading -> LoadingView { (status as? LoadStatus.Loading)?.progress ?: 0.99f }
        is LoadStatus.Error -> ErrorPlaceHolder(onRetry = { retryCount++ })
        is LoadStatus.Success<File> -> {
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
                LaunchedEffect(reset) { if (reset) zoomableViewState.reset() }
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
    postData: PostData,
    maxDraggableHeight: Dp,
    onTagClick: (TagInfo, Boolean) -> Unit
) {
    val scrollableViewState = rememberScrollableViewState()
    val scope = rememberCoroutineScope()
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
                postData = postData,
                expand = scrollableViewState.expand,
                onExpandClick = {
                    scrollableViewState.expand = !scrollableViewState.expand
                    scope.launch { scrollableViewState.animateToExpand() }
                }
            )
        },
        contentModifier = Modifier.background(ViewDetailBarFold),
        content = {
            ScrollableContent(
                postData = postData,
                onTagClick = onTagClick
            )
        }
    )
}

@Composable
private fun BoxScope.ScrollableBar(
    modifier: Modifier = Modifier,
    postData: PostData,
    expand: Boolean,
    onExpandClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .align(Alignment.Center)
            .padding(horizontal = 15.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        val expandIconDegree by animateFloatAsState(
            targetValue = if (expand) 90f else 270f, label = ""
        )
        Icon(
            modifier = Modifier
                .rotate(expandIconDegree)
                .height(40.dp)
                .width(40.dp)
                .align(Alignment.CenterEnd)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onExpandClick
                ),
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null
        )

        val info = postData.originalInfo
        val quality = postData.quality
        val context = LocalContext.current
        val text = "${postData.id} (${info.width}x${info.height}) ${quality.toResString(context)}"
        Text(
            modifier = Modifier.align(Alignment.CenterStart),
            text = text
        )
    }
}

@Composable
private fun BoxScope.ScrollableContent(
    modifier: Modifier = Modifier,
    postData: PostData,
    onTagClick: (TagInfo, Boolean) -> Unit
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

        var tagChanged = false
        for (tagInfo in postData.tags) {
            if (tagInfo.type == TagType.None) {
                val info by tagManager.getInfoStatus(tagInfo.name, scope).collectAsState()
                (info as? LoadStatus.Success)?.let {
                    postData.updateTag(it.result)
                    tagChanged = true
                }
            }
        }
        if (tagChanged) {
            postData.tags.sort()
            tagChanged = false
        }
        for (tagInfo in postData.tags) {
            TagInfoItem(info = tagInfo, onTagClick = onTagClick)
        }

        ContentHeader(stringResource(R.string.others))

        SettingItem(title = stringResource(R.string.author), tips = postData.uploader)
        SettingItem(title = stringResource(R.string.rating), tips = postData.rating.name)
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
    onTagClick: (TagInfo, Boolean) -> Unit
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
                onClick = { onTagClick(info, false) }
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
                .padding(vertical = 2.dp, horizontal = 5.dp)
                .clickable {
                    onTagClick(info, true)
                },
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
            ),
            onTagClick = { _, _ -> }
        )
        TagInfoItem(
            info = TagInfo(
                id = 10,
                source = "yande",
                name = "tokifokiz_bosottosf",
                type = TagType.Copyright,
                count = 6516
            ),
            onTagClick = { _, _ -> }
        )
    }
}