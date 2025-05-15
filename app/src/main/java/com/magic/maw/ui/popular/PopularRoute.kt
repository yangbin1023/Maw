package com.magic.maw.ui.popular

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.magic.maw.R
import com.magic.maw.data.PopularType
import com.magic.maw.data.PopularType.Companion.toPopularTypes
import com.magic.maw.data.PostData
import com.magic.maw.ui.components.throttle
import com.magic.maw.ui.post.PostRoute
import com.magic.maw.util.configFlow
import com.magic.maw.website.parser.BaseParser

@Composable
fun PopularRoute(
    modifier: Modifier = Modifier,
    popularViewModel: PopularViewModel,
    openDrawer: () -> Unit
) {
    val popularOption by popularViewModel.uiState.collectAsState()
    val datePickerEnable = remember { mutableStateOf(true) }

    LaunchedEffect(popularOption) {
        popularViewModel.postViewModel.update(emptyList<PostData>())
        popularViewModel.postViewModel.update(popularOption)
    }

    Box(modifier = modifier.fillMaxSize()) {
        PostRoute(
            postViewModel = popularViewModel.postViewModel,
            titleText = stringResource(R.string.popular),
            viewName = "Popular",
            shadowEnable = false,
            searchEnable = false,
            enhancedBar = {
                EnhancedBar(
                    modifier = it,
                    popularType = popularOption.type,
                    onTypeChanged = { popularViewModel.update(it) }
                )
            },
            onNegative = openDrawer,
            onOpenSubView = { datePickerEnable.value = !it }
        )

        AnimatedVisibility(
            visible = datePickerEnable.value,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PopularDatePicker(
                enabled = datePickerEnable.value,
                popularType = popularOption.type,
                focusDate = popularOption.date,
                onDateChanged = { popularViewModel.update(it) }
            )
        }
    }
}

@Composable
private fun EnhancedBar(
    modifier: Modifier,
    popularType: PopularType,
    onTypeChanged: (PopularType) -> Unit
) {
    val config by configFlow.collectAsState()
    val parser = BaseParser.get(config.source)
    val supportTypes = parser.supportPopular.toPopularTypes()
    val state = rememberLazyListState()
    val dividerHeight = with(LocalDensity.current) { minOf(1.dp, 2.toDp()) }
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(0.8f)

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 8.dp, bottom = 8.dp, start = 15.dp, end = 15.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
        ) {
            items(supportTypes) { item ->
                PopularTypeItem(
                    popularType = item,
                    selected = popularType == item
                ) {
                    onTypeChanged(item)
                }
            }
        }
        // 顶部分割线
        HorizontalDivider(
            modifier = Modifier.align(Alignment.TopCenter),
            thickness = dividerHeight,
            color = dividerColor
        )
        // 底部分割线
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = dividerHeight,
            color = dividerColor
        )
    }
}

@Composable
private fun PopularTypeItem(
    modifier: Modifier = Modifier,
    popularType: PopularType,
    selected: Boolean,
    onClicked: () -> Unit
) {
    val backModifier = if (selected) {
        val color = MaterialTheme.colorScheme.primaryContainer
        val shape = RoundedCornerShape(20)
        Modifier.background(color = color, shape = shape)
    } else {
        Modifier
    }
    Text(
        text = stringResource(popularType.getResStrId()),
        modifier = modifier
            .padding(horizontal = 10.dp)
            .clickable(onClick = throttle(func = onClicked))
            .then(backModifier)
            .padding(horizontal = 8.dp)
    )
}
