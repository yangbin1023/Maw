package com.magic.maw.data.api.loader

import com.magic.maw.data.model.site.PoolData
import com.magic.maw.data.model.site.PostData
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.collections.forEach

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

    fun checkAndRefresh(): Any
    fun refresh(force: Boolean = false): Any
    fun loadMore(): Any
    fun clearData(): Any
}

open class UiDataManager<T>(
    private val onKey: (T) -> String,
) {
    private val dataIdSet = mutableSetOf<String>()
    private val uiStateFlow = MutableStateFlow(ListUiState<T>())

    val uiState = uiStateFlow.asStateFlow()

    fun updateState(loadState: LoadState) {
        uiStateFlow.update { it.copy(loadState = loadState) }
    }

    fun replaceData(list: List<T>) = uiStateFlow.update { currentState ->
        dataIdSet.clear()
        for (item in list) {
            dataIdSet.add(onKey(item))
        }
        currentState.copy(
            items = list.toPersistentList(),
            loadState = LoadState.Idle
        )
    }

    fun appendData(
        list: List<T>,
        appendEnd: Boolean = true
    ) = uiStateFlow.update { currentState ->
        val toInsert = mutableListOf<T>()
        var updatedList = currentState.items

        list.forEach { newData ->
            if (dataIdSet.contains(onKey(newData))) {
                val index = updatedList.indexOfFirst { onKey(it) == onKey(newData) }
                if (index != -1) {
                    updatedList = updatedList.set(index, newData)
                }
            } else {
                dataIdSet.add(onKey(newData))
                toInsert.add(newData)
            }
        }

        val finalList = if (appendEnd) {
            updatedList.addAll(toInsert)
        } else {
            updatedList.addAll(0, toInsert)
        }
        val hasNoMore = list.isEmpty() && appendEnd
        currentState.copy(
            items = finalList,
            loadState = LoadState.Idle,
            hasNoMore = hasNoMore
        )
    }

    fun clearData() {
        uiStateFlow.update { it.copy(items = persistentListOf(), loadState = LoadState.Idle) }
    }
}

class PostDataManager : UiDataManager<PostData>({ it.id })
class PoolDataManager : UiDataManager<PoolData>({ it.id })