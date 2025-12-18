package com.magic.maw.data

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.magic.maw.website.RequestOption
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class LoaderState {
    None,
    Refresh,
    LoadMore,
    LoadFailed;

    val isLoading: Boolean get() = this == Refresh || this == LoadMore
}

interface DataLoader {
    fun refresh(force: Boolean = false)
    fun loadMore()
    fun clearData()
}

class PostDataLoader(
    initialList: List<PostData> = emptyList(),
    noMore: Boolean = false,
    val poolId: Int = -1,
    val scope: CoroutineScope = MainScope(),
) : DataLoader {
    private var _website: WebsiteOption = SettingsService.settingsState.value.website
    private var parser = BaseParser.get(_website)
    val website get() = _website
    val dataList = SnapshotStateList<PostData>()
    val stateFlow by lazy { MutableStateFlow(LoaderState.None) }
    val noMoreFlow by lazy { MutableStateFlow(noMore) }

    init {
        dataList.addAll(initialList)
        if (dataList.isEmpty() && !noMore) {
            refresh()
        }
        scope.launch {
            SettingsService.settingsState.collect { settingsState ->
                if (settingsState.website != _website) {
                    _website = settingsState.website
                    parser = BaseParser.get(_website)
                    refresh(true)
                }
            }
        }
    }

    override fun refresh(force: Boolean) {
        synchronized(this) {
            if (stateFlow.value == LoaderState.Refresh) return
            stateFlow.value = LoaderState.Refresh
        }
        clearData()
        scope.launch {
            parser.requestPostData(RequestOption(parser.firstPageIndex, poolId))
        }
    }

    override fun loadMore() {
        synchronized(this) {
            if (stateFlow.value.isLoading) return
            stateFlow.value = LoaderState.LoadMore
        }
        scope.launch {
            parser.requestPostData(RequestOption())
        }
    }

    override fun clearData() {
        dataList.clear()
    }
}

