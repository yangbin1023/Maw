package com.magic.maw.ui.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.hjq.toast.Toaster
import com.magic.maw.R
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import com.magic.maw.data.Quality.Companion.toQuality
import com.magic.maw.data.TagInfo
import com.magic.maw.data.TagType
import com.magic.maw.data.UserInfo
import com.magic.maw.data.toSizeString
import com.magic.maw.ui.components.MenuSettingItem
import com.magic.maw.ui.components.ScrollableView
import com.magic.maw.ui.components.ScrollableViewDefaults
import com.magic.maw.ui.components.SettingItem
import com.magic.maw.ui.components.TagItem
import com.magic.maw.ui.components.rememberScrollableViewState
import com.magic.maw.ui.components.throttle
import com.magic.maw.ui.theme.PreviewTheme
import com.magic.maw.util.ProgressNotification
import com.magic.maw.util.TimeUtils.toFormatStr
import com.magic.maw.util.configFlow
import com.magic.maw.util.getNotificationChannelId
import com.magic.maw.util.hasPermission
import com.magic.maw.util.isTextFile
import com.magic.maw.util.needNotificationPermission
import com.magic.maw.util.needStoragePermission
import com.magic.maw.util.saveToPicture
import com.magic.maw.website.LoadStatus
import com.magic.maw.website.TagManager
import com.magic.maw.website.loadDLFile
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ViewTAG"

@Composable
fun ViewDetailBar(
    modifier: Modifier = Modifier,
    postData: PostData,
    isScrollInProgress: Boolean,
    playerState: VideoPlayerState,
    maxDraggableHeight: Dp,
    onTagClick: (TagInfo, Boolean) -> Unit
) {
    var currentPostData by remember { mutableStateOf(postData) }
    if (!isScrollInProgress) {
        if (currentPostData != postData) {
            currentPostData = postData
        }
    }
    val scrollableViewState = rememberScrollableViewState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val showSaveDialog = remember { mutableStateOf(false) }
    val qualityList = ArrayList<Quality>()
    val qualityItems = ArrayList<String>()
    currentPostData.sampleInfo?.let {
        qualityList.add(Quality.Sample)
        val resolutionStr = "${it.width}x${it.height}"
        val sizeStr = if (it.size > 0) " " + it.size.toSizeString() else ""
        qualityItems.add("$resolutionStr$sizeStr")
    }
    currentPostData.largeInfo?.let {
        qualityList.add(Quality.Large)
        val resolutionStr = "${it.width}x${it.height}"
        val sizeStr = if (it.size > 0) " " + it.size.toSizeString() else ""
        qualityItems.add("$resolutionStr$sizeStr")
    }
    currentPostData.originalInfo.let {
        qualityList.add(Quality.File)
        val resolutionStr = "${it.width}x${it.height}"
        val sizeStr = if (it.size > 0) " " + it.size.toSizeString() else ""
        qualityItems.add("$resolutionStr$sizeStr")
    }

    LaunchedEffect(maxDraggableHeight, currentPostData.fileType) {
        val changed = scrollableViewState.updateData(
            density = density,
            showVideoControllerBar = currentPostData.fileType.isVideo,
            maxDraggableHeight = maxDraggableHeight
        )
        if (changed) {
            scrollableViewState.resetOffset()
        }
    }
    val onSave = getOnSaveCallback()
    ScrollableView(
        modifier = modifier.fillMaxWidth(),
        state = scrollableViewState,
        toolbarModifier = Modifier.let {
            if (!scrollableViewState.hideContent) {
                it.background(MaterialTheme.colorScheme.primaryContainer.copy(0.85f))
            } else {
                it.background(ViewScreenDefaults.detailBarFoldColor)
            }
        },
        toolbar = { state ->
            var isFavorite by remember { mutableStateOf(false) }
            DetailBar(
                modifier = Modifier.height(state.toolbarHeightDp.dp),
                postData = currentPostData,
                expand = scrollableViewState.expand,
                enabled = !isScrollInProgress,
                isFavorite = isFavorite,
                playerState = playerState,
                onFavorite = { isFavorite = !isFavorite },
                onSave = {
                    val websiteConfig = configFlow.value.websiteConfig
                    if (!websiteConfig.showSaveDialog) {
                        onSave(currentPostData, websiteConfig.saveQuality.toQuality())
                    } else {
                        showSaveDialog.value = true
                    }
                },
                onExpand = {
                    scrollableViewState.expand = !scrollableViewState.expand
                    scope.launch { scrollableViewState.animateToExpand() }
                }
            )
        },
        contentModifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(0.7f)),
        content = {
            DetailContent(
                modifier = Modifier.align(Alignment.TopCenter),
                postData = currentPostData,
                qualityItems = qualityItems,
                qualityList = qualityList,
                onTagClick = onTagClick
            )
            SaveDialog(
                showDialog = showSaveDialog,
                itemList = qualityItems,
                qualityList = qualityList,
                onConfirm = { index -> onSave(currentPostData, qualityList[index]) }
            )
        }
    )
}

@Composable
private fun DetailBar(
    modifier: Modifier = Modifier,
    postData: PostData,
    expand: Boolean,
    enabled: Boolean = true,
    isFavorite: Boolean = false,
    playerState: VideoPlayerState,
    onFavorite: () -> Unit,
    onSave: () -> Unit,
    onExpand: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 15.dp)
                .height(ScrollableViewDefaults.ToolbarHeight)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val info = postData.getInfo(postData.quality) ?: postData.originalInfo
            val text = "${postData.id} (${info.width}x${info.height})"
            Text(text = text)

            Spacer(modifier = Modifier.weight(1.0f))

            // Like
            IconButton(
                onClick = throttle(func = onFavorite),
                modifier = Modifier.width(40.dp),
                enabled = enabled
            ) {
                Icon(
                    modifier = Modifier
                        .height(40.dp)
                        .width(40.dp)
                        .scale(0.8f),
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    tint = if (isFavorite) Color.Red else LocalContentColor.current,
                    contentDescription = null
                )
            }

            // Save
            IconButton(
                onClick = throttle(func = onSave),
                modifier = Modifier.width(40.dp),
                enabled = enabled
            ) {
                Icon(
                    modifier = Modifier
                        .height(40.dp)
                        .width(40.dp)
                        .scale(0.8f),
                    painter = painterResource(R.drawable.ic_save),
                    contentDescription = null
                )
            }

            // Expand
            val expandIconDegree by animateFloatAsState(
                targetValue = if (expand) 90f else 270f, label = ""
            )

            IconButton(
                onClick = throttle(func = onExpand),
                modifier = Modifier.width(40.dp),
                enabled = enabled
            ) {
                Icon(
                    modifier = Modifier
                        .rotate(expandIconDegree)
                        .height(40.dp)
                        .width(40.dp),
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }
        }

        VideoPlayerControllerBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(VideoPlayerViewDefaults.ControllerBarHeight),
            state = playerState,
        )
    }
}

@Composable
private fun DetailContent(
    modifier: Modifier = Modifier,
    postData: PostData,
    qualityList: List<Quality>,
    qualityItems: List<String>,
    onTagClick: (TagInfo, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val config by configFlow.collectAsState()
    val tagManager = TagManager.get(config.source)
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        ContentHeader(stringResource(R.string.tags))

        var tagChanged = false
        for (tagInfo in postData.tags) {
            if (tagInfo.type == TagType.None || tagInfo.count == 0) {
                val info = tagManager.getInfoStatus(tagInfo.name, scope).collectAsState().value
                if (info is LoadStatus.Success<TagInfo>) {
                    if (tagInfo.type != info.result.type) {
                        tagChanged = true
                    }
                    postData.updateTag(info.result)
                }
            }
        }
        if (tagChanged) {
            postData.tags.sort()
        }
        for (tagInfo in postData.tags) {
            TagInfoItem(info = tagInfo, onTagClick = onTagClick)
        }

        ContentHeader(stringResource(R.string.others))

        // Quality
        var qualityIndex by remember { mutableIntStateOf(qualityList.indexOf(postData.quality)) }
        val localDataState = remember { mutableStateOf(postData) }
        if (localDataState.value != postData) {
            qualityIndex = qualityList.indexOf(postData.quality)
            if (qualityIndex < 0) {
                qualityIndex = 0
            } else if (qualityIndex >= qualityItems.size) {
                qualityIndex = 0
            }
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
        // Author
        if (postData.uploader.isNullOrEmpty()) {
            postData.createId?.let { createId ->
                val userManager = BaseParser.get(config.source).userManager
                val status = userManager.getStatus(createId).collectAsState().value
                if (status is LoadStatus.Success<UserInfo>) {
                    postData.uploader = status.result.name
                }
            }
        }
        SettingItem(
            title = stringResource(R.string.author),
            tips = postData.uploader ?: stringResource(R.string.unknown),
            showIcon = false
        )
        // Rating
        SettingItem(
            title = stringResource(R.string.rating),
            tips = postData.rating.name,
            showIcon = false
        )
        // Upload time
        postData.uploadTime?.let {
            SettingItem(
                title = stringResource(R.string.upload_time),
                tips = it.toFormatStr(),
                showIcon = false
            )
        }
        // FileType
        SettingItem(
            title = stringResource(R.string.file_type),
            tips = postData.fileType.name,
            showIcon = false
        )
        // source
        val srcUrl = postData.srcUrl ?: ""
        if (srcUrl.isNotEmpty()) {
            SettingItem(
                title = stringResource(R.string.source),
                tips = srcUrl,
                contentDescription = srcUrl,
                showIcon = false,
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, srcUrl.toUri())) }
            )
        }
        // score
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
                .clickable { onTagClick(info, true) }
                .border(
                    width = 1.dp,
                    color = searchColor,
                    shape = CircleShape
                )
                .padding(horizontal = 4.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .size(16.dp),
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = searchColor
            )
            if (info.count > 0) {
                Text(
                    modifier = Modifier.padding(end = 1.dp),
                    text = info.count.toString(),
                    color = searchColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun getOnSaveCallback(): (PostData, Quality) -> Unit {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val channel = remember { mutableStateOf(Channel<Boolean>()) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            channel.value.trySend(it)
        }
    )
    return { postData, quality ->
        scope.launch {
            if (needNotificationPermission && !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                // 没有通知权限，申请通知权限
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                channel.value.receive()
            }
            if (!needStoragePermission || context.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                saveFile(context, postData, quality)
            } else {
                launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (channel.value.receive()) {
                    saveFile(context, postData, quality)
                } else {
                    Toaster.show(context.getString(R.string.no_storage_permission))
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun saveFile(context: Context, postData: PostData, quality: Quality) {
    var found = false
    var info: PostData.Info? = null
    var infoQuality: Quality = quality
    for (item in Quality.SaveList) {
        if (item == quality) {
            found = true
        }
        if (found) {
            info = postData.getInfo(item)
            if (info != null) {
                infoQuality = item
                break
            }
        }
    }
    info ?: return
    var notification: ProgressNotification? = null
    var lastProgress = -1
    var lastUpdateTime = 0L
    if (!needNotificationPermission || context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
        val channelId = getNotificationChannelId(context, postData, infoQuality)
        notification = ProgressNotification(context, channelId)
    }
    withContext(Dispatchers.IO) {
        loadDLFile(postData, infoQuality)
    }.collect { status ->
        if (status is LoadStatus.Error) {
            val msg = context.getString(R.string.download_failed)
            Toaster.show(msg)
            notification?.finish(text = msg)
        } else if (status is LoadStatus.Loading) {
            // 限制更新频率
            if (lastProgress == -1) {
                Toaster.show("开始下载")
            }
            val now = System.currentTimeMillis()
            val progress = (status.progress * 100).toInt()
            if (now - lastUpdateTime > 1000 || progress / 10 > lastProgress / 10) {
                lastProgress = progress
                lastUpdateTime = now
                notification?.update(progress)
            }
        } else if (status is LoadStatus.Success) {
            withContext(Dispatchers.IO) {
                try {
                    if (status.result.isTextFile()) {
                        status.result.delete()
                        Logger.e(TAG) { "保存文件失败，文本文件" }
                        val msg = context.getString(R.string.download_failed)
                        Toaster.show(msg)
                        notification?.finish(text = msg)
                        return@withContext
                    }
                    val uri = saveToPicture(context, postData, infoQuality, status.result)
                    val text = context.getString(R.string.save_success_click_to_open)
                    notification?.finish(text = text, uri = uri)
                    Toaster.show(context.getString(R.string.save_success))
                    Logger.d(TAG) { "保存文件成功, uri: $uri" }
                } catch (e: Exception) {
                    val msg = context.getString(R.string.save_failed)
                    Logger.e(TAG) { "$msg: ${e.message}" }
                    notification?.finish(text = msg)
                    Toaster.show(msg)
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 480)
@Composable
private fun DetailBarPreview() {
    val postData = PostData(
        source = "yande",
        id = 1210648,
        uploader = "BattleQueen",
        sampleInfo = PostData.Info(width = 927, height = 1500, size = 2889300),
        originalInfo = PostData.Info(width = 1500, height = 2427, size = 22900000),
    )
    postData.quality = Quality.Sample
    val context = LocalContext.current
    val playerState = remember { VideoPlayerState(context = context) }

    PreviewTheme {
        Scaffold {
            Box(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .height(64.dp)
                ) {
                    DetailBar(
                        modifier = Modifier,
                        postData = postData,
                        expand = false,
                        isFavorite = true,
                        playerState = playerState,
                        onFavorite = {},
                        onSave = {},
                        onExpand = {}
                    )
                }
            }
        }
    }
}