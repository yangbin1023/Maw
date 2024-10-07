package com.magic.maw.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ScrollableView(
    modifier: Modifier = Modifier,
    state: ScrollableViewState,
    toolbarModifier: Modifier = Modifier,
    toolbar: @Composable BoxScope.(ScrollableViewState) -> Unit,
    contentModifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        contentColor = contentColorFor(MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Box(
                modifier = toolbarModifier
                    .fillMaxWidth()
                    .height(state.toolbarHeightDp.dp)
                    .draggable(
                        state = rememberDraggableState { scope.launch { state.onDelta(it) } },
                        orientation = Orientation.Vertical,
                        onDragStopped = { state.onDragEnd() }
                    )
            ) {
                toolbar(state)
            }
            Box(
                modifier = contentModifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { state.offsetValue.toDp() }),
                contentAlignment = Alignment.TopStart
            ) {
                content()
            }
        }
    }
}

@Composable
fun rememberScrollableViewState(
    toolbarHeight: Dp = ScrollableViewDefaults.ToolbarHeight,
    maxDraggableHeight: Dp = 500.dp,
    minDraggableHeight: Dp = 0.dp,
    draggableStages: Int = 0
): ScrollableViewState {
    val density = LocalDensity.current
    val state = rememberSaveable(saver = ScrollableViewState.Saver) {
        ScrollableViewState().updateData(
            density,
            toolbarHeight,
            maxDraggableHeight,
            minDraggableHeight,
            draggableStages
        )
    }
    return state
}

class ScrollableViewState(
    private var maxPx: Float = 0f,
    private var minPx: Float = 0f,
    private var stages: Int = 1,
    private var toolbarHeightValue: Float = 0f,
    initialOffsetValue: Float = 0f,
) {
    private val offsetAnimate = Animatable(initialOffsetValue)

    val toolbarHeightDp: Float get() = toolbarHeightValue

    val offsetValue: Float get() = offsetAnimate.value

    var expand by mutableStateOf(false)

    private suspend fun snapTo(value: Float) {
        offsetAnimate.snapTo(value)
        if (value == minPx) {
            expand = false
        } else if (value == (maxPx - minPx)) {
            expand = true
        }
    }

    private suspend fun animateTo(value: Float) {
        offsetAnimate.animateTo(value)
    }

    private suspend fun animateTo(stage: Int) {
        expand = (stage == stages)
        animateTo((maxPx - minPx) * stage / stages)
    }

    suspend fun animateToExpand() {
        animateTo(if (expand) stages else 0)
    }

    suspend fun onDelta(delta: Float) {
        snapTo((offsetValue - delta).coerceIn(minPx, maxPx))
    }

    suspend fun onDragEnd() {
        val percent = abs(offsetAnimate.value / (maxPx - minPx))
        animateTo((percent * stages).roundToInt())
    }

    fun updateData(
        density: Density,
        toolbarHeight: Dp = ScrollableViewDefaults.ToolbarHeight,
        maxDraggableHeight: Dp = 500.dp,
        minDraggableHeight: Dp = 0.dp,
        draggableStages: Int = 0
    ) = apply {
        val maxHeight = maxDraggableHeight - toolbarHeight
        toolbarHeightValue = toolbarHeight.value
        maxPx = with(density) { maxHeight.toPx() }
        minPx = with(density) { minDraggableHeight.toPx() }
        stages = if (draggableStages > 0) {
            draggableStages
        } else {
            (maxHeight / toolbarHeight / 3).coerceIn(1f, 5f).toInt()
        }
    }

    companion object {
        val Saver = listSaver(save = { state ->
            listOf(
                state.maxPx,
                state.minPx,
                state.stages,
                state.toolbarHeightValue,
                state.offsetValue,
                if (state.expand) 1 else 0
            )
        }, restore = { list ->
            ScrollableViewState(
                list[0] as Float,
                list[1] as Float,
                list[2] as Int,
                list[3] as Float,
                list[4] as Float,
            ).apply {
                expand = (list[5] as Int) == 1
            }
        })
    }
}

@Composable
@Preview(widthDp = 360, heightDp = 480)
private fun ScrollableViewPreview() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val state = rememberScrollableViewState(
            maxDraggableHeight = maxHeight
        )
        ScrollableView(
            modifier = Modifier.align(Alignment.BottomCenter),
            state = state,
            toolbar = { _ ->
                Icon(
                    modifier = Modifier.align(Alignment.CenterStart),
                    imageVector = Icons.Default.Star,
                    contentDescription = ""
                )
                Icon(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = ""
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                SettingItem(title = "标签", subTitle = "副标题", tips = "数量")
                SettingItem(title = "标签", subTitle = "副标题", tips = "数量")
                SettingItem(title = "标签", subTitle = "副标题", tips = "数量")
            }
        }
    }
}

object ScrollableViewDefaults {
    val ToolbarHeight: Dp = 64.dp
}