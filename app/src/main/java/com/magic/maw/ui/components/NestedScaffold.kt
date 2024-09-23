package com.magic.maw.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.magic.maw.util.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

typealias OnScrollStop = (CoroutineScope, NestedScaffoldState, Float) -> Boolean
typealias OnScrollToCritical = () -> Unit

class NestedScaffoldState(
    val maxPx: Float,
    val minPx: Float,
    initialScrollValue: Float = 0f,
) {
    private val scrollAnimate = Animatable(initialScrollValue)

    val scrollValue: Float get() = scrollAnimate.value

    val scrollPercent: Float get() = abs(scrollValue / (maxPx - minPx))

    suspend fun snapTo(value: Float) {
        scrollAnimate.snapTo(value)
    }

    suspend fun animateTo(value: Float) {
        scrollAnimate.animateTo(value)
    }

    companion object {
        val Saver = listSaver(
            save = { state ->
                listOf(state.minPx, state.maxPx, state.scrollValue)
            },
            restore = {
                NestedScaffoldState(it[0], it[1], it[2])
            }
        )
    }
}

private class NestedScaffoldConnection(
    val state: NestedScaffoldState,
    private val coroutineScope: CoroutineScope,
    private val canScroll: () -> Boolean = { true },
    private val onScrollToTop: OnScrollToCritical? = null,
    private val onScrollToBottom: OnScrollToCritical? = null,
    private val onScrollStop: OnScrollStop? = null,
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (!canScroll())
            return Offset.Zero
        val delta = available.y
        val newOffset = state.scrollValue + delta
        val targetOffset = newOffset.coerceIn(state.minPx, state.maxPx)
        if (state.scrollValue != targetOffset) {
            if (targetOffset == state.minPx) {
                onScrollToTop?.invoke()
            } else if (targetOffset == state.maxPx) {
                onScrollToBottom?.invoke()
            }
        }
        coroutineScope.launch { state.snapTo(targetOffset) }
        val consumedOffset = delta - (newOffset - targetOffset)
        println("delta: $delta, source: $source, consumedOffset: $consumedOffset")
        return available.copy(y = consumedOffset)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return if (canScroll() && callOnScrollStop(available.y)) available else Velocity.Zero
    }

    private fun callOnScrollStop(delta: Float): Boolean {
        return onScrollStop?.invoke(coroutineScope, state, delta) == true
    }
}

@Composable
fun rememberNestedScaffoldState(
    maxDp: Dp = UiUtils.getTopBarHeight(),
    minDp: Dp = NestedScaffoldDefaults.TopBarMinDp,
    initialScrollValue: Float = 0f
): NestedScaffoldState {
    if (minDp > maxDp) {
        throw RuntimeException("The minimum height is greater than the maximum height.")
    }
    val maxPx = with(LocalDensity.current) { maxDp.toPx() }
    val minPx = with(LocalDensity.current) { minDp.toPx() }
    return rememberSaveable(saver = NestedScaffoldState.Saver) {
        NestedScaffoldState(0f, minPx - maxPx, initialScrollValue)
    }
}

@Composable
fun NestedScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (IntOffset) -> Unit,
    state: NestedScaffoldState = rememberNestedScaffoldState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    canScroll: () -> Boolean = { true },
    onScrollToTop: OnScrollToCritical? = null,
    onScrollToBottom: OnScrollToCritical? = null,
    onScrollStop: OnScrollStop? = getDefaultOnScrollStop(onScrollToTop, onScrollToBottom),
    content: @Composable (PaddingValues) -> Unit,
) {
    val connection = remember(
        state, coroutineScope, canScroll,
        onScrollToTop, onScrollToBottom, onScrollStop
    ) {
        NestedScaffoldConnection(
            state = state,
            coroutineScope = coroutineScope,
            canScroll = canScroll,
            onScrollToTop = onScrollToTop,
            onScrollToBottom = onScrollToBottom,
            onScrollStop = onScrollStop
        )
    }

    Scaffold(
        modifier = modifier
            .nestedScroll(connection)
            .clipScrollableContainer(Orientation.Vertical),
        topBar = {
            topBar(IntOffset(0, state.scrollValue.roundToInt()))
        }
    ) { innerPadding ->
        val density = LocalDensity.current
        val newPadding = remember(innerPadding) {
            object : PaddingValues {
                override fun calculateBottomPadding(): Dp = innerPadding.calculateBottomPadding()

                override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
                    innerPadding.calculateLeftPadding(layoutDirection)

                override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
                    innerPadding.calculateRightPadding(layoutDirection)

                override fun calculateTopPadding(): Dp = with(density) {
                    (abs(state.maxPx - state.minPx) - abs(state.scrollValue)).roundToInt().toDp()
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
    topBar: @Composable (IntOffset) -> Unit,
    state: NestedScaffoldState = rememberNestedScaffoldState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    canScroll: () -> Boolean = { true },
    onScrollToTop: OnScrollToCritical? = null,
    onScrollToBottom: OnScrollToCritical? = null,
    onScrollStop: OnScrollStop? = getDefaultOnScrollStop(onScrollToTop, onScrollToBottom),
    content: @Composable BoxScope.() -> Unit,
) {
    NestedScaffold(
        modifier = modifier,
        topBar = topBar,
        state = state,
        coroutineScope = coroutineScope,
        canScroll = canScroll,
        onScrollToTop = onScrollToTop,
        onScrollToBottom = onScrollToBottom,
        onScrollStop = onScrollStop,
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

fun getDefaultOnScrollStop(
    onScrollToTop: OnScrollToCritical? = null,
    onScrollToBottom: OnScrollToCritical? = null
): OnScrollStop = func@{ scope, state, _ ->
    val percent = state.scrollPercent
    if (percent == 0f || percent == 1.0f)
        return@func false
    scope.launch {
        if (percent > 0.5f) {
            state.animateTo(state.minPx)
            onScrollToTop?.invoke()
        } else {
            state.animateTo(state.maxPx)
            onScrollToBottom?.invoke()
        }
    }
    true
}

object NestedScaffoldDefaults {
    val TopBarMinDp = 0.dp
    val defaultOnScrollStop: OnScrollStop = func@{ scope, state, _ ->
        val percent = state.scrollPercent
        if (percent == 0f || percent == 1.0f)
            return@func false
        scope.launch { state.animateTo(if (percent > 0.5f) state.minPx else state.maxPx) }
        true
    }
}