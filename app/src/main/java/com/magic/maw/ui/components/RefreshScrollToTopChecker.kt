package com.magic.maw.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.magic.maw.data.PostData
import kotlinx.collections.immutable.PersistentList


/**
 * 用于刷新时，若有新内容，则滚动到顶部
 */
@Composable
fun RefreshScrollToTopChecker(
    items: PersistentList<PostData>,
    scrollToTop: suspend () -> Unit
) {
    val topItemId by remember(items) {
        derivedStateOf { items.firstOrNull()?.id }
    }
    var isFirstTime by remember { mutableStateOf(true) }

    LaunchedEffect(topItemId) {
        if (isFirstTime) {
            isFirstTime = false
        } else if (topItemId != null) {
            scrollToTop()
        }
    }
}
