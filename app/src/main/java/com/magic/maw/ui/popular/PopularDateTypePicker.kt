package com.magic.maw.ui.popular

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.magic.maw.data.SettingsService
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopularDateTypePicker(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val scope = rememberCoroutineScope()
    val settingState by SettingsService.settingsState.collectAsStateWithLifecycle()
    val popularDateTypes by remember {
        derivedStateOf {
            BaseParser.get(settingState.website).supportedPopularDateTypes
        }
    }
    val colorScheme = MaterialTheme.colorScheme

    val targetColor by remember(scrollBehavior) {
        derivedStateOf {
            val overlappingFraction = scrollBehavior?.state?.overlappedFraction ?: 0f
            val colorTransitionFraction = if (overlappingFraction > 0.01f) 1f else 0f
            lerp(
                colorScheme.surface,
                colorScheme.surfaceContainer,
                FastOutLinearInEasing.transform(colorTransitionFraction)
            )
        }
    }
    val appBarContainerColor = animateColorAsState(
        targetColor,
        animationSpec = spring(dampingRatio = 1f, stiffness = 1600f),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp)
            .drawBehind {
                val color = appBarContainerColor.value
                if (color != Color.Unspecified) {
                    drawRect(color = color)
                }
            }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 使用 BoxWithConstraints 来获取容器总宽度以便计算偏移
        Box(modifier = Modifier.wrapContentSize()) {
            val tabWidth = PopularDateTypePickerDefaults.itemWidth // 假设每个 Tab 固定宽度，也可以动态测量
            val indicatorWidth = tabWidth // 背景方块宽度

            // 1. 底层滑动的方块背景
            Box(
                modifier = Modifier
                    .offset {
                        // 计算偏移量：(当前页 + 滑动比例) * 单个Tab宽度
                        val totalOffset =
                            (pagerState.currentPage + pagerState.currentPageOffsetFraction) * tabWidth.toPx()
                        IntOffset(totalOffset.toInt(), 0)
                    }
                    .size(indicatorWidth, PopularDateTypePickerDefaults.itemHeight)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )

            // 2. 上层文字选项
            Row {
                popularDateTypes.forEachIndexed { index, popularType ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(tabWidth, PopularDateTypePickerDefaults.itemHeight)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null // 去除点击波纹效果
                            ) {
                                scope.launch { pagerState.scrollToPage(index) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = popularType.getResStrId()),
                            // 字体加粗动画：根据选中状态切换
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

object PopularDateTypePickerDefaults {
    val itemWidth = 60.dp
    val itemHeight = 28.dp
}
