package com.magic.maw.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun CustomSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    dpSize: DpSize = DpSize(48.dp, 28.dp),
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    animationDuration: Int = 300
) {
    BoxWithConstraints(
        modifier = modifier
            .size(dpSize)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange?.invoke(!checked) }
            )
    ) {
        // 获取实际测量到的轨道尺寸
        val trackWidth = maxWidth
        val trackHeight = maxHeight

        // 计算拇指尺寸（轨道高度的80%）
        val thumbSize = trackHeight * 0.8f

        // 计算偏移量
        val thumbPadding = (trackHeight - thumbSize) / 2
        val thumbOffsetX: Dp = if (checked) {
            trackWidth - thumbSize - thumbPadding
        } else {
            thumbPadding
        }

        // 动画效果
        val animatedThumbOffset by animateDpAsState(
            targetValue = thumbOffsetX,
            animationSpec = tween(durationMillis = animationDuration)
        )

        // 轨道背景颜色
        val trackColor by animateColorAsState(
            targetValue = colors.trackColor(enabled, checked),
            animationSpec = tween(durationMillis = animationDuration)
        )

        // 按钮缩放
        val thumbScale by animateFloatAsState(
            targetValue = if (checked) 1f else 0.7f,
            animationSpec = tween(durationMillis = animationDuration)
        )

        // 按钮颜色
        val thumbColor by animateColorAsState(
            targetValue = colors.thumbColor(enabled, checked),
            animationSpec = tween(durationMillis = animationDuration)
        )

        // 边框颜色
        val borderColor by animateColorAsState(
            targetValue = colors.borderColor(enabled, checked),
            animationSpec = tween(durationMillis = animationDuration)
        )

        // 轨道背景
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(trackHeight / 2))
                .border(1.5.dp, borderColor, CircleShape)
                .background(trackColor)
        ) {
            // 拇指
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(x = animatedThumbOffset.toPx().toInt(), y = 0) }
                    .size(thumbSize)
                    .scale(thumbScale)
                    .background(thumbColor, CircleShape)
            )
        }
    }
}

@Stable
internal fun SwitchColors.thumbColor(enabled: Boolean, checked: Boolean): Color =
    if (enabled) {
        if (checked) checkedThumbColor else uncheckedThumbColor
    } else {
        if (checked) disabledCheckedThumbColor else disabledUncheckedThumbColor
    }

@Stable
internal fun SwitchColors.trackColor(enabled: Boolean, checked: Boolean): Color =
    if (enabled) {
        if (checked) checkedTrackColor else uncheckedTrackColor
    } else {
        if (checked) disabledCheckedTrackColor else disabledUncheckedTrackColor
    }

@Stable
internal fun SwitchColors.borderColor(enabled: Boolean, checked: Boolean): Color =
    if (enabled) {
        if (checked) checkedBorderColor else uncheckedBorderColor
    } else {
        if (checked) disabledCheckedBorderColor else disabledUncheckedBorderColor
    }

@Stable
internal fun SwitchColors.iconColor(enabled: Boolean, checked: Boolean): Color =
    if (enabled) {
        if (checked) checkedIconColor else uncheckedIconColor
    } else {
        if (checked) disabledCheckedIconColor else disabledUncheckedIconColor
    }