package com.magic.maw.ui.view

import android.content.Context
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.magic.maw.data.Quality
import com.magic.maw.ui.components.ScaleImageView
import com.magic.maw.ui.components.loadModel
import com.magic.maw.ui.components.scale.rememberScaleState
import com.magic.maw.util.logger
import com.magic.maw.website.LoadStatus
import com.magic.maw.website.loadDLFile
import com.magic.maw.website.loadDLFileWithTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val stateFlow = loadDLFile(data, quality, coroutineScope).collectAsState()
    val size = Size(info.width.toFloat(), info.height.toFloat())
    var model by remember { mutableStateOf<Pair<Any?, Size?>>(Pair(null, size)) }
    var retryCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(quality) { model = Pair(null, size) }
    val state = stateFlow.value
    when (state) {
        is LoadStatus.Waiting -> LoadingView()
        is LoadStatus.Loading -> LoadingView { state.progress }
        is LoadStatus.Error -> ErrorPlaceHolder(onRetry = { retryCount++ })
        is LoadStatus.Success<File> -> {
            retryCount = 0
            var pair by remember { mutableStateOf<Pair<Any?, Size?>>(Pair(null, size)) }
            LaunchedEffect(state.result) {
                pair = loadModel(context = context, state.result)
            }
            if (pair.second == null) {
                ErrorPlaceHolder()
            } else {
                val scaleState = rememberScaleState(contentSize = pair.second)
                LaunchedEffect(reset) { if (reset) scaleState.resetImmediately() }
                ScaleImageView(
                    model = pair.first,
                    scaleState = scaleState,
                    onTap = { onTab.invoke() },
                    onDoubleTap = { coroutineScope.launch { scaleState.toggleScale(it) } }
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
private fun loadImageState(
    data: PostData,
    quality: Quality,
    defaultSize: Size,
    scope: CoroutineScope
): MutableState<LoadStatus<Pair<Any, Size>>> {
    val state = remember { mutableStateOf<LoadStatus<Pair<Any, Size>>>(LoadStatus.Waiting) }
    val dlState = loadDLFile(data, quality, scope).collectAsState().value
    val context = LocalContext.current
    state.value = when (dlState) {
        is LoadStatus.Error -> LoadStatus.Error(dlState.exception)
        is LoadStatus.Loading -> LoadStatus.Loading(dlState.progress)
        is LoadStatus.Waiting -> LoadStatus.Waiting
        is LoadStatus.Success -> {
            val pair = loadModel(context, dlState.result, defaultSize).collectAsState().value
            if (pair is LoadStatus.Success) pair else LoadStatus.Loading(1.0f)
        }
    }
    return state
}

private fun loadImageState(
    data: PostData,
    quality: Quality,
    defaultSize: Size,
    context: Context,
    scope: CoroutineScope
): StateFlow<LoadStatus<Pair<Any, Size>>> {
    val state = MutableStateFlow<LoadStatus<Pair<Any, Size>>>(LoadStatus.Waiting).apply {
        stateIn(scope, SharingStarted.Eagerly, this.value)
    }
    val task = loadDLFileWithTask(data, quality)
    val dlStateFlow = task.statusFlow
    val updateFunc2: (LoadStatus<Pair<Any, Size>>) -> Unit = { newValue ->
        logger.info("update func 2: $newValue")
        if (newValue is LoadStatus.Success) {
            state.update { it }
        } else {
            state.update { LoadStatus.Loading(1.0f) }
        }
    }
    val updateFunc: (LoadStatus<File>) -> Unit = { newValue ->
        logger.info("update func 1: $newValue")
        when (newValue) {
            is LoadStatus.Error -> state.update { LoadStatus.Error(newValue.exception) }
            is LoadStatus.Loading -> state.update { LoadStatus.Loading(newValue.progress) }
            is LoadStatus.Waiting -> state.update { LoadStatus.Waiting }
            is LoadStatus.Success -> {
                scope.launch(Dispatchers.IO) {
                    val flow = loadModel(context, newValue.result, defaultSize)
                    flow.onEach { updateFunc2.invoke(it) }
                    updateFunc2.invoke(flow.value)
                }
            }
        }
    }
    dlStateFlow.onEach { updateFunc.invoke(it) }
    updateFunc.invoke(dlStateFlow.value)
    task.start()
    return state
}