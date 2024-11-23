package com.magic.maw.ui.view

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.magic.maw.R
import com.magic.maw.data.PostData
import com.magic.maw.ui.components.ScaleImageView
import com.magic.maw.ui.components.loadModel
import com.magic.maw.ui.components.scale.ScaleDecoder
import com.magic.maw.ui.components.scale.rememberScaleState
import com.magic.maw.website.LoadStatus
import com.magic.maw.website.LoadType
import com.magic.maw.website.loadDLFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
fun ViewContent(
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
            .background(MaterialTheme.colorScheme.background),
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
    val defaultSize = Size(info.width.toFloat(), info.height.toFloat())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val type = remember { mutableStateOf(LoadType.Waiting) }
    val progress = remember { mutableFloatStateOf(0f) }
    val model = remember { mutableStateOf<Any?>(null) }
    val size = remember { mutableStateOf<Size>(defaultSize) }
    var retryCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(data, quality, retryCount) {
        type.value = LoadType.Waiting
        withContext(Dispatchers.IO) {
            loadDLFile(data, quality)
        }.collect {
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
                withContext(Dispatchers.IO) {
                    loadModel(context, it.result, defaultSize)
                }.collect { pair ->
                    if (pair is LoadStatus.Success) {
                        model.value.let {
                            if (it is ScaleDecoder && it != pair.result.first)
                                it.release()
                        }
                        type.value = LoadType.Success
                        model.value = pair.result.first
                        size.value = pair.result.second
                    } else if (pair is LoadStatus.Loading) {
                        type.value = LoadType.Loading
                        progress.floatValue = pair.progress
                    } else if (pair == LoadStatus.Waiting) {
                        type.value = LoadType.Waiting
                    } else {
                        type.value = LoadType.Error
                    }
                }
            }
        }
    }

    when (type.value) {
        LoadType.Waiting -> LoadingView()
        LoadType.Loading -> LoadingView { progress.floatValue }
        LoadType.Error -> ErrorPlaceHolder { retryCount++ }
        LoadType.Success -> {
            val state = rememberScaleState(contentSize = size.value)
            LaunchedEffect(reset) { if (reset) state.resetImmediately() }
            ScaleImageView(
                model = model.value,
                scaleState = state,
                onTap = { onTab.invoke() },
                onDoubleTap = { scope.launch { (state.toggleScale(it)) } }
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
