package com.magic.maw.ui.features.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.paging.compose.LazyPagingItems
import co.touchlab.kermit.Logger
import coil3.ImageLoader
import com.magic.maw.R
import com.magic.maw.data.api.manager.loadDLFile
import com.magic.maw.data.local.store.SettingsStore
import com.magic.maw.data.model.LoadStatus
import com.magic.maw.data.model.LoadType
import com.magic.maw.data.model.site.PostData
import com.magic.maw.ui.common.UgoiraPlayer
import com.magic.maw.ui.common.throttle
import com.magic.maw.util.DisableHapticLocalProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import org.koin.compose.koinInject
import java.io.File
import kotlin.math.abs

private const val TAG = "ViewContent"

@Composable
fun ViewContent(
    pagerState: PagerState,
    dataList: List<PostData>,
    playerState: VideoPlayerState,
    onLoadMore: () -> Unit,
    onExit: () -> Unit,
    onTab: () -> Unit = {},
) {
    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) { index ->
        if (index >= dataList.size || index < 0) {
            onExit()
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
            playerState = playerState,
            focusOn = pagerState.settledPage == index,
            onTab = onTab
        )
    }
}

@Composable
fun ViewContent(
    pagerState: PagerState,
    lazyPagingItems: LazyPagingItems<PostData>,
    playerState: VideoPlayerState,
    onTab: () -> Unit = {},
) {
    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) { index ->
        val data = lazyPagingItems[index]
        if (data != null) {
            ViewScreenItem(
                data = data,
                playerState = playerState,
                focusOn = pagerState.settledPage == index,
                onTab = onTab
            )
        }
    }
}

@Composable
private fun ViewScreenItem(
    data: PostData,
    playerState: VideoPlayerState,
    focusOn: Boolean = true,
    onTab: () -> Unit = {},
) {
    val quality = data.quality
    val type = remember { mutableStateOf(LoadType.Waiting) }
    val progress = remember { mutableFloatStateOf(0f) }
    val model = remember { mutableStateOf<Any?>(null) }
    var retryCount by remember { mutableIntStateOf(0) }
    val info = data.getInfo(quality) ?: data.originalInfo
    LaunchedEffect(data, quality, retryCount) {
        type.value = LoadType.Waiting
        withContext(Dispatchers.IO) {
            loadDLFile(data, quality)
        }.collect {
            Logger.d(TAG) { "load status: $it" }
            // 下载失败后重试，若重试三次仍然失败则显示错误
            if (it == LoadStatus.Waiting) {
                type.value = LoadType.Waiting
            } else if (it is LoadStatus.Error) {
                if (retryCount < 3) {
                    // 下载失败后重试，若重试三次仍然失败则显示错误
                    type.value = LoadType.Waiting
                    retryCount++
                } else {
                    type.value = LoadType.Error
                }
            } else if (it is LoadStatus.Loading) {
                type.value = LoadType.Loading
                progress.floatValue = it.progress
            } else if (it is LoadStatus.Success) {
                type.value = LoadType.Success
                model.value = it.result
            }
        }
    }
    if (focusOn) {
        playerState.isEnable.value = info.type.isVideo
        if (!playerState.isEnable.value && playerState.isPlaying.value) {
            playerState.togglePlayPause()
        }
    }

    when (type.value) {
        LoadType.Waiting -> LoadingView(onTab = onTab)
        LoadType.Loading -> LoadingView(progress = { progress.floatValue }, onTab = onTab)
        LoadType.Error -> ErrorPlaceHolder { retryCount++ }
        LoadType.Success -> {
            Logger.d(TAG) { "load success file type: ${info.type}" }
            if (info.type.isVideo) {
                if (!focusOn) return
                val file = (model.value as? File) ?: return
                VideoPlayerView(
                    videoUri = file.toUri(),
                    state = playerState,
                    width = info.width,
                    height = info.height,
                    onTab = onTab
                )
            } else if (info.type.isPicture) {
                val zoomableState = rememberZoomableState()
                val state = rememberZoomableImageState(zoomableState)
                LaunchedEffect(focusOn) { if (!focusOn) zoomableState.resetZoom() }
                val file = (model.value as? File) ?: return
                DisableHapticLocalProvider {
                    val imageLoader = koinInject<ImageLoader>()
                    ZoomableAsyncImage(
                        model = file.toUri(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        imageLoader = imageLoader,
                        state = state,
                        onClick = { onTab() }
                    )
                }
            } else if (info.type.isUgoira) {
                val file = (model.value as? File) ?: return
                val settingsState by SettingsStore.settingsState.collectAsState()
                val frameRate by remember {
                    derivedStateOf {
                        settingsState.websiteSettings.ugoiraFrameRate
                    }
                }
                UgoiraPlayer(
                    zipFile = file,
                    frameRate = frameRate,
                    onTab = onTab
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.unknown) + " " + stringResource(R.string.file_type) + " " + info.type.name,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
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
private fun LoadingView(progress: (() -> Float)? = null, onTab: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = throttle(func = onTab)
            ),
        contentAlignment = Alignment.Center
    ) {
        progress?.let {
            CircularProgressIndicator(progress = it)
        } ?: CircularProgressIndicator()
    }
}
