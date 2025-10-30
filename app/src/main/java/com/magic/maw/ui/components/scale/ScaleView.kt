package com.magic.maw.ui.components.scale

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.math.withSign

@Composable
fun ScaleView(
    boundClip: Boolean = true, scaleState: ScaleState = rememberScaleState(),
    onTap: (Offset) -> Unit = {},
    onDoubleTap: (Offset) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { clip = boundClip }
            .pointerInput(scaleState) {
                detectTransformGestures(
                    onTap = onTap,
                    onDoubleTap = onDoubleTap,
                    gestureStart = { scaleState.onGestureStart(scope) },
                    gestureEnd = { scaleState.onGestureEnd(scope, it) },
                    onGesture = { center, pan, zoom, rotate, event ->
                        scaleState.onGesture(scope, center, pan, zoom, rotate, event)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val maxWidth = this.maxWidth
        val maxHeight = this.maxHeight
        scaleState.updateContainerSize(
            density.run { Size(maxWidth.toPx(), maxHeight.toPx()) }
        )
        Box(
            modifier = Modifier
                .graphicsLayer {
                    transformOrigin = TransformOrigin.Center
                    scaleX = scaleState.scale.value
                    scaleY = scaleState.scale.value
                    translationX = scaleState.offsetX.value
                    translationY = scaleState.offsetY.value
                    rotationZ = scaleState.rotation.value
                }
                .width(density.run { scaleState.displayWidth.toDp() })
                .height(density.run { scaleState.displayHeight.toDp() })
        ) {
            content()
        }
    }
}

@Composable
fun rememberScaleState(
    contentSize: Size? = null,
    @FloatRange(from = 1.0) maxScale: Float = ScaleDefaults.MAX_SCALE_RATE,
    animationSpec: AnimationSpec<Float>? = null,
): ScaleState {
    val scope = rememberCoroutineScope()
    return rememberSaveable(saver = ScaleState.Saver) {
        ScaleState(
            maxScale = maxScale,
            animationSpec = animationSpec,
        )
    }.apply {
        contentSize?.let {
            this.contentSize = it
            // 旋转后如果超出边界了要修复回去
            scope.launch { fixToBound() }
        }
    }
}

class ScaleState(
    val maxScale: Float = ScaleDefaults.MAX_SCALE_RATE,
    offsetX: Float = ScaleDefaults.DEFAULT_OFFSET_X,
    offsetY: Float = ScaleDefaults.DEFAULT_OFFSET_Y,
    scale: Float = ScaleDefaults.DEFAULT_SCALE,
    rotation: Float = ScaleDefaults.DEFAULT_ROTATION,
    animationSpec: AnimationSpec<Float>? = null,
) {
    // 默认动画窗格
    private var defaultAnimateSpec: AnimationSpec<Float> = animationSpec ?: SpringSpec()

    // x偏移
    val offsetX = Animatable(offsetX)

    // y偏移
    val offsetY = Animatable(offsetY)

    // 放大倍率
    val scale = Animatable(scale)

    // 旋转
    val rotation = Animatable(rotation)

    // 是否允许手势输入
    var allowGestureInput = true

    private val contentSizeState = mutableStateOf<Size?>(null)

    var contentSize: Size
        set(value) {
            contentSizeState.value = value
        }
        get() {
            return if (contentSizeState.value?.isSpecified == true) {
                contentSizeState.value!!
            } else {
                Size.Zero
            }
        }

    // 容器大小
    var containerSize = mutableStateOf(Size.Zero)

    val containerWidth: Float
        get() = containerSize.value.width

    val containerHeight: Float
        get() = containerSize.value.height

    private val containerRatio: Float
        get() = containerSize.value.run {
            width.div(height)
        }

    private val contentRatio: Float
        get() = contentSize.run {
            width.div(height)
        }

    // 宽度是否对齐视口
    private val widthFixed: Boolean
        get() = contentRatio > containerRatio

    // 1倍缩放率
    internal val scale1x: Float
        get() {
            return if (widthFixed) {
                containerSize.value.width.div(contentSize.width)
            } else {
                containerSize.value.height.div(contentSize.height)
            }
        }

    val displayWidth: Float
        get() {
            return contentSize.width.times(scale1x)
        }

    val displayHeight: Float
        get() {
            return contentSize.height.times(scale1x)
        }

    val realSize: Size
        get() {
            return Size(
                width = displayWidth.times(scale.value),
                height = displayHeight.times(scale.value)
            )
        }

    // 手势的中心点
    val gestureCenter = mutableStateOf(Offset.Zero)

    // 手势加速度
    var velocityTracker = VelocityTracker()

    // 最后一次偏移运动
    var lastPan = Offset.Zero

    // 减速运动动画曲线
    val decay = FloatExponentialDecaySpec(2f).generateDecayAnimationSpec<Float>()

    // 手势实时的偏移范围
    var boundX = Pair(0F, 0F)
    var boundY = Pair(0F, 0F)

    // 计算边界使用的缩放率
    var boundScale = 1F

    // 记录触摸事件中手指的个数
    var eventChangeCount = 0

    // 触摸时中心位置
    var centroid = Offset.Zero

    /**
     * 判断是否有动画正在运行
     * @return Boolean
     */
    fun isRunning(): Boolean {
        return scale.isRunning
                || offsetX.isRunning
                || offsetY.isRunning
                || rotation.isRunning
    }

    internal fun updateContainerSize(size: Size) {
        containerSize.value = size
    }

    /**
     * 立即设置回初始值
     */
    suspend fun resetImmediately() {
        rotation.snapTo(ScaleDefaults.DEFAULT_ROTATION)
        offsetX.snapTo(ScaleDefaults.DEFAULT_OFFSET_X)
        offsetY.snapTo(ScaleDefaults.DEFAULT_OFFSET_Y)
        scale.snapTo(ScaleDefaults.DEFAULT_SCALE)
    }

    /**
     * 修正offsetX,offsetY的位置
     */
    suspend fun fixToBound() {
        boundX = getBound(
            scale.value,
            containerWidth,
            displayWidth,
        )
        boundY = getBound(
            scale.value,
            containerHeight,
            displayHeight,
        )
        val limitX = limitToBound(offsetX.value, boundX)
        val limitY = limitToBound(offsetY.value, boundY)
        coroutineScope {
            launch {
                offsetX.animateTo(limitX)
            }
            launch {
                offsetY.animateTo(limitY)
            }
        }
    }

    /**
     * 设置回初始值
     */
    suspend fun reset(animationSpec: AnimationSpec<Float> = defaultAnimateSpec) {
        coroutineScope {
            listOf(
                async { rotation.animateTo(ScaleDefaults.DEFAULT_ROTATION, animationSpec) },
                async { offsetX.animateTo(ScaleDefaults.DEFAULT_OFFSET_X, animationSpec) },
                async { offsetY.animateTo(ScaleDefaults.DEFAULT_OFFSET_Y, animationSpec) },
                async { scale.animateTo(ScaleDefaults.DEFAULT_SCALE, animationSpec) },
            ).awaitAll()
        }
    }

    /**
     * 放大到最大
     */
    private suspend fun scaleToMax(
        offset: Offset,
        animationSpec: AnimationSpec<Float>? = null
    ) {
        val currentAnimateSpec = animationSpec ?: defaultAnimateSpec

        var nextOffsetX = (containerWidth / 2 - offset.x) * maxScale
        var nextOffsetY = (containerHeight / 2 - offset.y) * maxScale

        val boundX = getBound(maxScale, containerWidth, displayWidth)
        val boundY = getBound(maxScale, containerHeight, displayHeight)

        nextOffsetX = limitToBound(nextOffsetX, boundX)
        nextOffsetY = limitToBound(nextOffsetY, boundY)

        // 启动
        coroutineScope {
            listOf(
                async {
                    offsetX.updateBounds(null, null)
                    offsetX.animateTo(nextOffsetX, currentAnimateSpec)
                    offsetX.updateBounds(boundX.first, boundX.second)
                },
                async {
                    offsetY.updateBounds(null, null)
                    offsetY.animateTo(nextOffsetY, currentAnimateSpec)
                    offsetY.updateBounds(boundY.first, boundY.second)
                },
                async {
                    scale.animateTo(maxScale, currentAnimateSpec)
                },
            ).awaitAll()
        }
    }

    /**
     * 放大或缩小
     */
    suspend fun toggleScale(
        offset: Offset,
        animationSpec: AnimationSpec<Float> = defaultAnimateSpec
    ) {
        // 如果不等于1，就调回1
        if (scale.value != 1F) {
            reset(animationSpec)
        } else {
            scaleToMax(offset, animationSpec)
        }
    }

    companion object {
        val Saver: Saver<ScaleState, *> = listSaver(save = {
            listOf(it.offsetX.value, it.offsetY.value, it.scale.value, it.rotation.value)
        }, restore = {
            ScaleState(
                offsetX = it[0],
                offsetY = it[1],
                scale = it[2],
                rotation = it[3],
            )
        })
    }
}

/**
 * 标记手势事件开始
 *
 * @param scope 用于进行变换的协程作用域
 */
private fun ScaleState.onGestureStart(scope: CoroutineScope) {
    if (allowGestureInput) {
        eventChangeCount = 0
        velocityTracker = VelocityTracker()
        scope.launch {
            offsetX.stop()
            offsetY.stop()
            offsetX.updateBounds(null, null)
            offsetY.updateBounds(null, null)
        }
    }
}

/**
 * 标记手势事件结束
 *
 * @param scope 用于进行变换的协程作用域
 * @param transformOnly 仅转换
 */
private fun ScaleState.onGestureEnd(scope: CoroutineScope, transformOnly: Boolean) = scope.apply {
    if (!transformOnly || !allowGestureInput || isRunning()) return@apply
    var velocity = try {
        velocityTracker.calculateVelocity()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
    // 如果缩放比小于1，要自动回到1
    // 如果缩放比大于最大显示缩放比，就设置回去，并且避免加速度
    val nextScale = when {
        scale.value < 1 -> 1F
        scale.value > maxScale -> {
            velocity = null
            maxScale
        }

        else -> null
    }
    launch {
        if (inBound(offsetX.value, boundX) && velocity != null) {
            val velocityX = if (velocity.x.isNaN()) 0F else velocity.x
            val vx = velocityX.withSign(lastPan.x.sign)
            offsetX.updateBounds(boundX.first, boundX.second)
            offsetX.animateDecay(vx, decay)
        } else {
            val targetX = if (nextScale != maxScale) {
                offsetX.value
            } else {
                panTransformAndScale(
                    offset = offsetX.value,
                    center = centroid.x,
                    bh = containerWidth,
                    uh = displayWidth,
                    fromScale = scale.value,
                    toScale = nextScale
                )
            }
            offsetX.animateTo(limitToBound(targetX, boundX))
        }
    }
    launch {
        if (inBound(offsetY.value, boundY) && velocity != null) {
            val velocityY = if (velocity.y.isNaN()) 0F else velocity.y
            val vy = velocityY.withSign(lastPan.y.sign)
            offsetY.updateBounds(boundY.first, boundY.second)
            offsetY.animateDecay(vy, decay)
        } else {
            val targetY = if (nextScale != maxScale) {
                offsetY.value
            } else {
                panTransformAndScale(
                    offset = offsetY.value,
                    center = centroid.y,
                    bh = containerHeight,
                    uh = displayHeight,
                    fromScale = scale.value,
                    toScale = nextScale
                )
            }
            offsetY.animateTo(limitToBound(targetY, boundY))
        }
    }
    launch {
        rotation.animateTo(0F)
    }
    nextScale?.let {
        launch {
            scale.animateTo(nextScale)
        }
    }
}

/**
 * 输入手势事件
 *
 * @param scope 用于进行变换的协程作用域
 * @param center 手势中心坐标
 * @param pan 手势移动距离
 * @param zoom 手势缩放率
 * @param rotate 旋转角度
 * @param event 事件对象
 * @return 是否消费这次事件
 */
private fun ScaleState.onGesture(
    scope: CoroutineScope,
    center: Offset,
    pan: Offset,
    zoom: Float,
    rotate: Float,
    event: PointerEvent
): Boolean {
    if (!allowGestureInput) return true
    // 这里只记录最大手指数
    if (eventChangeCount <= event.changes.size) {
        eventChangeCount = event.changes.size
    } else {
        // 如果手指数从多个变成一个，就结束本次手势操作
        return false
    }

    var checkRotate = rotate
    var checkZoom = zoom
    // 如果是双指的情况下，手指距离小于一定值时，缩放和旋转的值会很离谱，所以在这种极端情况下就不要处理缩放和旋转了
    if (event.changes.size == 2) {
        val fingerDistanceOffset =
            event.changes[0].position - event.changes[1].position
        if (
            fingerDistanceOffset.x.absoluteValue < ScaleDefaults.MIN_GESTURE_FINGER_DISTANCE
            && fingerDistanceOffset.y.absoluteValue < ScaleDefaults.MIN_GESTURE_FINGER_DISTANCE
        ) {
            checkRotate = 0F
            checkZoom = 1F
        }
    }

    gestureCenter.value = center

    val currentOffsetX = offsetX.value
    val currentOffsetY = offsetY.value
    val currentScale = scale.value
    val currentRotation = rotation.value

    var nextScale = currentScale.times(checkZoom)
    // 检查最小放大倍率
    if (nextScale < ScaleDefaults.MIN_SCALE) nextScale = ScaleDefaults.MIN_SCALE

    // 最后一次的偏移量
    lastPan = pan
    // 记录手势的中点
    centroid = center
    // 计算边界，如果目标缩放值超过最大显示缩放值，边界就要用最大缩放值来计算，否则手势结束时会导致无法归位
    boundScale =
        if (nextScale > maxScale) maxScale else nextScale
    boundX =
        getBound(
            boundScale,
            containerWidth,
            displayWidth,
        )
    boundY =
        getBound(
            boundScale,
            containerHeight,
            displayHeight,
        )

    var nextOffsetX = panTransformAndScale(
        offset = currentOffsetX,
        center = center.x,
        bh = containerWidth,
        uh = displayWidth,
        fromScale = currentScale,
        toScale = nextScale
    ) + pan.x
    var nextOffsetY = panTransformAndScale(
        offset = currentOffsetY,
        center = center.y,
        bh = containerHeight,
        uh = displayHeight,
        fromScale = currentScale,
        toScale = nextScale
    ) + pan.y

    // 如果手指数1，就是拖拽，拖拽受范围限制
    // 如果手指数大于1，即有缩放事件，则支持中心点放大
    if (eventChangeCount == 1) {
        nextOffsetX = limitToBound(nextOffsetX, boundX)
        nextOffsetY = limitToBound(nextOffsetY, boundY)
    }

    val nextRotation = if (nextScale < 1) {
        currentRotation + checkRotate
    } else currentRotation

    // 添加到手势加速度
    velocityTracker.addPosition(
        event.changes[0].uptimeMillis,
        Offset(nextOffsetX, nextOffsetY),
    )

    if (!isRunning()) scope.launch {
        scale.snapTo(nextScale)
        offsetX.snapTo(nextOffsetX)
        offsetY.snapTo(nextOffsetY)
        rotation.snapTo(nextRotation)
    }

    // 这里判断是否已运动到边界，如果到了边界，就不消费事件，让上层界面获取到事件
    val canConsumeX = reachSide(pan.x, nextOffsetX, boundX)
    val canConsumeY = reachSide(pan.y, nextOffsetY, boundY)
    // 判断主要活动方向
    val canConsume = if (pan.x.absoluteValue > pan.y.absoluteValue) {
        canConsumeX
    } else {
        canConsumeY
    }
    if (canConsume || scale.value < 1) {
        event.changes.fastForEach {
            if (it.positionChanged()) {
                it.consume()
            }
        }
    }
    // 返回true，继续下一次手势
    return true
}

/**
 * 追踪缩放过程中的中心点
 */
private fun panTransformAndScale(
    offset: Float,
    center: Float,
    bh: Float,
    uh: Float,
    fromScale: Float,
    toScale: Float,
): Float {
    val srcH = uh * fromScale
    val desH = uh * toScale
    val gapH = (bh - uh) / 2

    val py = when {
        uh >= bh -> {
            val upy = (uh * fromScale - uh).div(2)
            (upy - offset + center) / (fromScale * uh)
        }

        srcH > bh || bh > uh -> {
            val upy = (srcH - uh).div(2)
            (upy - gapH - offset + center) / (fromScale * uh)
        }

        else -> {
            val upy = -(bh - srcH).div(2)
            (upy - offset + center) / (fromScale * uh)
        }
    }
    return when {
        uh >= bh -> {
            val upy = (uh * toScale - uh).div(2)
            upy + center - py * toScale * uh
        }

        desH > bh -> {
            val upy = (desH - uh).div(2)
            upy - gapH + center - py * toScale * uh
        }

        else -> {
            val upy = -(bh - desH).div(2)
            upy + center - py * desH
        }
    }
}

/**
 * 判断手势移动是否已经到达边缘
 *
 * @param pan 手势移动的距离
 * @param offset 当前偏移量
 * @param bound 限制位移的范围
 * @return 是否到底边缘
 */
internal fun reachSide(pan: Float, offset: Float, bound: Pair<Float, Float>): Boolean {
    val reachRightSide = offset <= bound.first
    val reachLeftSide = offset >= bound.second
    return !(reachLeftSide && pan > 0)
            && !(reachRightSide && pan < 0)
            && !(reachLeftSide && reachRightSide)
}

/**
 * 把位移限制在边界内
 *
 * @param offset 偏移量
 * @param bound 限制位移的范围
 * @return
 */
private fun limitToBound(offset: Float, bound: Pair<Float, Float>): Float {
    return when {
        offset <= bound.first -> {
            bound.first
        }

        offset > bound.second -> {
            bound.second
        }

        else -> {
            offset
        }
    }
}

/**
 * 判断位移是否在边界内
 */
private fun inBound(offset: Float, bound: Pair<Float, Float>): Boolean {
    return if (offset > 0) {
        offset < bound.second
    } else if (offset < 0) {
        offset > bound.first
    } else {
        true
    }
}

/**
 * 获取移动边界
 */
private fun getBound(scale: Float, bw: Float, dw: Float): Pair<Float, Float> {
    val rw = scale.times(dw)
    val bound = if (rw > bw) {
        var xb = (rw - bw).div(2)
        if (xb < 0) xb = 0F
        xb
    } else {
        0F
    }
    return Pair(-bound, bound)
}

private suspend fun PointerInputScope.detectTransformGestures(
    panZoomLock: Boolean = false,
    gestureStart: () -> Unit = {},
    gestureEnd: (Boolean) -> Unit = {},
    onTap: (Offset) -> Unit = {},
    onDoubleTap: (Offset) -> Unit = {},
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float, event: PointerEvent) -> Boolean,
) {
    var lastReleaseTime = 0L
    var scope: CoroutineScope? = null
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        val t0 = System.currentTimeMillis()
        var releasedEvent: PointerEvent? = null
        var moveCount = 0
        // 这里开始事件
        gestureStart()
        do {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Release) releasedEvent = event
            if (event.type == PointerEventType.Move) moveCount++
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }
                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        if (!onGesture(
                                centroid,
                                panChange,
                                zoomChange,
                                effectiveRotation,
                                event
                            )
                        ) break
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })

        var t1 = System.currentTimeMillis()
        val dt = t1 - t0
        val dlt = t1 - lastReleaseTime

        if (moveCount == 0) releasedEvent?.let { e ->
            if (e.changes.isEmpty()) return@let
            val offset = e.changes.first().position
            if (dlt < 272) {
                t1 = 0L
                scope?.cancel()
                onDoubleTap(offset)
            } else if (dt < 200) {
                scope = MainScope().apply {
                    launch(Dispatchers.Main) {
                        delay(272)
                        onTap(offset)
                    }
                }
            }
            lastReleaseTime = t1
        }

        // 这里是事件结束
        gestureEnd(moveCount != 0)
    }
}

object ScaleDefaults {
    // 默认X轴偏移量
    const val DEFAULT_OFFSET_X = 0F

    // 默认Y轴偏移量
    const val DEFAULT_OFFSET_Y = 0F

    // 默认缩放率
    const val DEFAULT_SCALE = 1F

    // 默认旋转角度
    const val DEFAULT_ROTATION = 0F

    // 图片最小缩放率
    const val MIN_SCALE = 0.5F

    // 图片最大缩放率
    const val MAX_SCALE_RATE = 4F

    // 最小手指手势间距
    const val MIN_GESTURE_FINGER_DISTANCE = 200
}
