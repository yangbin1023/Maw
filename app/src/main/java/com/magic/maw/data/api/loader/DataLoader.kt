package com.magic.maw.data.api.loader

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

sealed class LoadState {
    object Idle : LoadState()
    object Refreshing : LoadState()
    object LoadingMore : LoadState()
    data class Error(val message: String? = null) : LoadState()

    val isLoading: Boolean get() = this == Refreshing || this == LoadingMore
}

data class ListUiState<T>(
    val items: PersistentList<T> = persistentListOf(),
    val loadState: LoadState = LoadState.Idle,
    val hasNoMore: Boolean = false
) {
    val isLoading: Boolean
        get() = loadState == LoadState.Refreshing || loadState == LoadState.LoadingMore
}

interface DataLoader<T> {
    val uiState: StateFlow<ListUiState<T>>

    fun refresh(force: Boolean = false): Any
    fun loadMore(): Any
    fun clearData(): Any
    fun search(text: String = ""): Any = {}
}
