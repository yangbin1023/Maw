package com.magic.maw.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

typealias OnScrollStop = (NestedScaffoldState, Float) -> Boolean

class NestedScaffoldState(
    val maxPx: Float,
    val minPx: Float,
    private val coroutineScope: CoroutineScope,
    private val isEnable: () -> Boolean = { true },
    private val onScrollStop: OnScrollStop? = null,
) {
    private val scrollAnimate = Animatable(0f)

    val enable: Boolean get() = isEnable.invoke()

    val scrollValue: Float get() = scrollAnimate.value

    val scrollPercent: Float get() = abs(scrollValue / (maxPx - minPx))

    fun snapTo(value: Float) {
        coroutineScope.launch {
            scrollAnimate.snapTo(value)
        }
    }

    fun animateTo(value: Float) {
        coroutineScope.launch {
            scrollAnimate.animateTo(value)
        }
    }

    fun callOnScrollStop(delta: Float): Boolean {
        return enable && onScrollStop?.invoke(this, delta) == true
    }
}

private class NestedScaffoldConnection(val state: NestedScaffoldState) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (!state.enable)
            return Offset.Zero
        val delta = available.y
        val newOffset = state.scrollValue + delta
        val targetOffset = newOffset.coerceIn(state.minPx, state.maxPx)
        state.snapTo(targetOffset)
        val preOffset = delta - (newOffset - targetOffset)
        println("delta: $delta, source: $source, preOffset: $preOffset")
        return available.copy(y = preOffset)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return if (state.callOnScrollStop(available.y)) available else Velocity.Zero
    }
}

@Composable
fun NestedScaffold(
    modifier: Modifier = Modifier,
    isEnable: () -> Boolean = { true },
    onScrollStop: OnScrollStop? = NestedScaffoldDefaults.defaultOnScrollStop,
    topBar: @Composable (IntOffset) -> Unit,
    topBarMaxHeight: Dp = NestedScaffoldDefaults.TopBarMaxDp,
    topBarMinHeight: Dp = NestedScaffoldDefaults.TopBarMinDp,
    content: @Composable (PaddingValues) -> Unit,
) {
    if (topBarMinHeight > topBarMaxHeight) {
        throw RuntimeException("The minimum height is greater than the maximum height.")
    }
    val maxPx = with(LocalDensity.current) { topBarMaxHeight.toPx() }
    val minPx = with(LocalDensity.current) { topBarMinHeight.toPx() }

    val coroutineScope = rememberCoroutineScope()

    val state = remember(maxPx, minPx, coroutineScope) {
        NestedScaffoldState(
            maxPx = 0f,
            minPx = minPx - maxPx,
            coroutineScope = coroutineScope,
            isEnable = isEnable,
            onScrollStop = onScrollStop
        )
    }
    val connection = remember(state) {
        NestedScaffoldConnection(state)
    }

    val density = LocalDensity.current

    Scaffold(
        modifier = modifier
            .nestedScroll(connection)
            .clipScrollableContainer(Orientation.Vertical),
        topBar = {
            topBar(IntOffset(0, state.scrollValue.roundToInt()))
        }
    ) { innerPadding ->
        val newPadding = remember(innerPadding) {
            object : PaddingValues {
                override fun calculateBottomPadding(): Dp = innerPadding.calculateBottomPadding()

                override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
                    innerPadding.calculateLeftPadding(layoutDirection)

                override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
                    innerPadding.calculateRightPadding(layoutDirection)

                override fun calculateTopPadding(): Dp = with(density) {
                    topBarMaxHeight - abs(state.scrollValue).roundToInt().toDp()
                }
            }
        }
        content(newPadding)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NestedRefreshScaffold(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshState: PullToRefreshState = rememberPullToRefreshState(),
    onScrollStop: OnScrollStop? = NestedScaffoldDefaults.defaultOnScrollStop,
    topBar: @Composable (IntOffset) -> Unit,
    topBarMaxHeight: Dp = NestedScaffoldDefaults.TopBarMaxDp,
    topBarMinHeight: Dp = NestedScaffoldDefaults.TopBarMinDp,
    content: @Composable BoxScope.() -> Unit,
) {
    NestedScaffold(
        modifier = modifier,
        isEnable = { refreshState.distanceFraction <= 0.0f },
        onScrollStop = onScrollStop,
        topBar = topBar,
        topBarMaxHeight = topBarMaxHeight,
        topBarMinHeight = topBarMinHeight
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier.padding(innerPadding),
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = refreshState
        ) {
            content()
        }
    }
}

private object NestedScaffoldDefaults {
    val TopBarMaxDp = 88.dp
    val TopBarMinDp = 0.dp
    val defaultOnScrollStop: OnScrollStop = func@{ state, _ ->
        val percent = state.scrollPercent
        if (percent == 0f || percent == 1.0f)
            return@func false
        if (percent > 0.5f) {
            state.animateTo(state.minPx)
        } else {
            state.animateTo(state.maxPx)
        }
        true
    }
}