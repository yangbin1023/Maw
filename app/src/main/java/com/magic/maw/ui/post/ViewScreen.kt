package com.magic.maw.ui.post

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magic.maw.R
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import com.magic.maw.data.TagInfo
import com.magic.maw.data.TagType
import com.magic.maw.data.toSizeString
import com.magic.maw.ui.components.MenuSettingItem
import com.magic.maw.ui.components.RememberSystemBars
import com.magic.maw.ui.components.ScaleAsyncImage
import com.magic.maw.ui.components.ScrollableView
import com.magic.maw.ui.components.SettingItem
import com.magic.maw.ui.components.TagItem
import com.magic.maw.ui.components.rememberScaleState
import com.magic.maw.ui.components.rememberScrollableViewState
import com.magic.maw.ui.theme.ViewDetailBarExpand
import com.magic.maw.ui.theme.ViewDetailBarFold
import com.magic.maw.util.TimeUtils.toFormatStr
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
        val draggableHeight = if (showTopBar) this.maxHeight - topBarMaxHeight else this.maxHeight
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
            maxDraggableHeight = draggableHeight,
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
        ViewScreenItem(
            data = dataList[index],
            reset = pagerState.settledPage != index,
            onTab = onTab
        )
    }
}

@Composable
private fun ViewScreenItem(
    data: PostData,
    reset: Boolean = false,
    onTab: () -> Unit = {},
) {
    val quality = data.quality
    val info = data.getInfo(quality) ?: data.originalInfo
    val coroutineScope = rememberCoroutineScope()
    val status by loadDLFile(data, quality, coroutineScope).collectAsState()
    val size = Size(info.width.toFloat(), info.height.toFloat())
    var model by remember { mutableStateOf<Pair<Any?, Size?>>(Pair(null, size)) }
    var retryCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(quality) { model = Pair(null, size) }
    when (status) {
        is LoadStatus.Waiting -> LoadingView()
        is LoadStatus.Loading -> LoadingView { (status as? LoadStatus.Loading)?.progress ?: 0.99f }
        is LoadStatus.Error -> ErrorPlaceHolder(onRetry = { retryCount++ })
        is LoadStatus.Success<File> -> {
            retryCount = 0
            val scaleState = rememberScaleState(contentSize = size)
            LaunchedEffect(reset) { if (reset) scaleState.resetImmediately() }
            ScaleAsyncImage(
                model = (status as? LoadStatus.Success<File>)?.result,
                scaleState = scaleState,
                onTap = { onTab.invoke() },
                onDoubleTap = { coroutineScope.launch { scaleState.toggleScale(it) } }
            )
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
            if (!scrollableViewState.hideContent) {
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
        contentModifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(0.7f)),
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

        val info = postData.getInfo(postData.quality) ?: postData.originalInfo
        val text = "${postData.id} (${info.width}x${info.height})"
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
    val context = LocalContext.current
    val qualityList = ArrayList<Quality>()
    val qualityItems = ArrayList<String>()
    postData.sampleInfo?.let {
        qualityList.add(Quality.Sample)
        val resolutionStr = "${it.width}x${it.height}"
        val sizeStr = if (it.size > 0) " " + it.size.toSizeString() else ""
        qualityItems.add("$resolutionStr$sizeStr")
    }
    postData.largeInfo?.let {
        qualityList.add(Quality.Large)
        val resolutionStr = "${it.width}x${it.height}"
        val sizeStr = if (it.size > 0) " " + it.size.toSizeString() else ""
        qualityItems.add("$resolutionStr$sizeStr")
    }
    postData.originalInfo.let {
        qualityList.add(Quality.File)
        val resolutionStr = "${it.width}x${it.height}"
        val sizeStr = if (it.size > 0) " " + it.size.toSizeString() else ""
        qualityItems.add("$resolutionStr$sizeStr")
    }
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

        var qualityIndex by remember { mutableIntStateOf(qualityList.indexOf(postData.quality)) }
        if (qualityIndex < 0) {
            qualityIndex = 0
        }
        MenuSettingItem(
            title = stringResource(R.string.quality),
            items = qualityItems,
            checkItem = qualityIndex,
            onItemChanged = {
                postData.quality = qualityList[it]
                qualityIndex = it
            }
        )
        SettingItem(
            title = stringResource(R.string.author),
            tips = postData.uploader,
            showIcon = false
        )
        SettingItem(
            title = stringResource(R.string.rating),
            tips = postData.rating.name,
            showIcon = false
        )
        postData.uploadTime?.let {
            SettingItem(
                title = stringResource(R.string.upload_time),
                tips = it.toFormatStr(),
                showIcon = false
            )
        }

        val srcUrl = postData.srcUrl ?: ""
        if (srcUrl.isNotEmpty()) {
            SettingItem(
                title = stringResource(R.string.source),
                tips = srcUrl,
                contentDescription = srcUrl,
                showIcon = false,
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(srcUrl))) }
            )
        }
        postData.score?.let {
            SettingItem(
                title = stringResource(R.string.score),
                tips = it.toString(),
                showIcon = false
            )
        }
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
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.onBackground.copy(0.85f)
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
        val searchColor = MaterialTheme.colorScheme.onBackground
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = searchColor,
                    shape = CircleShape
                )
                .padding(horizontal = 4.5.dp)
                .clickable {
                    onTagClick(info, true)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = searchColor
            )
            Text(
                modifier = Modifier.padding(end = 1.dp),
                text = info.count.toString(),
                color = searchColor,
                fontSize = 14.sp
            )
        }
    }
}
