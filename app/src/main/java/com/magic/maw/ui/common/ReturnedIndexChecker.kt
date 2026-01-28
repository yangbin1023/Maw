package com.magic.maw.ui.common

import androidx.collection.IntIntMap
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import co.touchlab.kermit.Logger

/**
 * 用于从View界面返回Post界面时，若查看项的索引发生变化时，将对应索引滚动到中央
 */
@Composable
fun ReturnedIndexChecker(
    lazyState: LazyStaggeredGridState,
    itemHeights: IntIntMap,
    postIndex: Int? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    LaunchedEffect(lifecycleOwner, postIndex) {
        if (lifecycleState == Lifecycle.State.STARTED
            || lifecycleState == Lifecycle.State.RESUMED
        ) {
            if (postIndex != null) {
                val index: Int = postIndex
                val itemHeight = itemHeights.getOrDefault(index, 0)
                val viewportHeight = lazyState.layoutInfo.viewportSize.height
                val offset = -(viewportHeight - itemHeight) / 2
                lazyState.scrollToItem(index, offset)
                Logger.d("ReturnedIndexChecker") { "scroll to postIndex: $index" }
            }
        }
    }
}
