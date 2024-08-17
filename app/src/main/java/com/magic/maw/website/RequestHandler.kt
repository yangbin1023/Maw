package com.magic.maw.website

import androidx.compose.runtime.snapshots.SnapshotStateList

interface RequestHandler<T> {
    val dataList: SnapshotStateList<T>
    fun refresh(force: Boolean = false)
    fun loadMore()
}