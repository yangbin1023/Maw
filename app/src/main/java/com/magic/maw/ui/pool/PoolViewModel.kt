package com.magic.maw.ui.pool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.magic.maw.data.PoolData
import com.magic.maw.data.loader.PoolDataLoader
import com.magic.maw.data.loader.PostDataLoader
import com.magic.maw.ui.post.UiStateType
import com.magic.maw.util.configFlow
import com.magic.maw.website.RequestOption
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PoolViewModel"

data class PoolUiState(
    val dataList: List<PoolData> = emptyList(),
    val noMore: Boolean = true,
    val type: UiStateType = UiStateType.None,
    val viewIndex: Int = -1,
    val requestOption: RequestOption
) {
    private val dataMap: HashMap<Int, PoolData> = HashMap()

    val isPoolView: Boolean get() = viewIndex < 0
    val isPostView: Boolean get() = viewIndex >= 0

    init {
        for (data in dataList) {
            dataMap[data.id] = data
        }
    }

    fun append(
        dataList: List<PoolData>,
        end: Boolean = true,
        type: UiStateType = UiStateType.None
    ): PoolUiState {
        val list: MutableList<PoolData> = ArrayList()
        for (data in dataList) {
            dataMap[data.id]?.updateFrom(data) ?: let {
                dataMap[data.id] = data
                list.add(data)
            }
        }
        val newList = if (list.isEmpty()) {
            this.dataList
        } else {
            this.dataList.toMutableList().apply {
                if (end) addAll(list) else addAll(0, list)
            }
        }
        val noMore = dataList.isEmpty() && end
        return copy(dataList = newList, noMore = noMore, type = type)
    }
}

class PoolViewModel : ViewModel() {
    private val viewModelState: MutableStateFlow<PoolUiState>

    init {
        val parser = getParser()
        val websiteConfig = configFlow.value.websiteConfig
        val option = RequestOption(page = parser.firstPageIndex, ratingFlag = websiteConfig.rating)
        viewModelState = MutableStateFlow(PoolUiState(noMore = false, requestOption = option))
    }

    val uiState = viewModelState
        .stateIn(viewModelScope, SharingStarted.Eagerly, viewModelState.value)

    fun refresh(force: Boolean = checkForceRefresh()) {
        synchronized(this) {
            if (viewModelState.value.type == UiStateType.Refresh)
                return
            viewModelState.update { it.copy(type = UiStateType.Refresh) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Logger.d(TAG) { "pool refresh." }
                val parser = getParser()
                val option = viewModelState.value.requestOption
                option.ratingFlag = configFlow.value.websiteConfig.rating
                val list = parser.requestPoolData(option.copy(page = parser.firstPageIndex))
                viewModelState.update {
                    if (force) {
                        it.requestOption.page = parser.firstPageIndex
                        it.copy(dataList = list, noMore = false, type = UiStateType.None)
                    } else {
                        it.append(dataList = list, end = false, type = UiStateType.None)
                    }
                }
            } catch (e: Throwable) {
                Logger.e { "pool refresh failed: ${e.message}" }
                viewModelState.update { it.copy(type = UiStateType.LoadFailed) }
            }
        }
    }

    fun loadMore() {
        synchronized(this) {
            if (viewModelState.value.type.isLoading())
                return
            viewModelState.update { it.copy(type = UiStateType.LoadMore) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parser = getParser()
                val option = viewModelState.value.requestOption
                val list = parser.requestPoolData(option.copy(page = option.page + 1))
                viewModelState.update {
                    if (it.type != UiStateType.Refresh && list.isNotEmpty()) {
                        it.requestOption.page++
                        it.append(dataList = list, type = UiStateType.None)
                    } else {
                        it
                    }
                }
            } catch (e: Throwable) {
                Logger.e { "pool loadMore failed: ${e.message}" }
                viewModelState.update { it.copy(type = UiStateType.LoadFailed) }
            }
        }
    }

    fun clearData() {
        viewModelState.update { it.copy(dataList = emptyList()) }
    }

    fun setViewIndex(viewIndex: Int) {
        viewModelState.update { it.copy(viewIndex = viewIndex) }
    }

    fun exitPostView() {
        if (viewModelState.value.isPoolView)
            return
        viewModelState.update { it.copy(viewIndex = -1) }
    }

    private fun checkForceRefresh(): Boolean {
        val websiteConfig = configFlow.value.websiteConfig
        val state = viewModelState.value
        return state.dataList.isEmpty()
                || websiteConfig.rating != state.requestOption.ratingFlag
    }

    private fun getParser(): BaseParser {
        return BaseParser.get(configFlow.value.source)
    }
}

class PoolViewModel2 : ViewModel() {
    private val _postLoader = MutableStateFlow<PostDataLoader?>(null)

    val loader = PoolDataLoader(scope = viewModelScope)

    val postLoader = _postLoader.asStateFlow()

    fun setViewPoolPost(poolId: Int) {
        _postLoader.update {
            PostDataLoader(scope = viewModelScope, poolId = poolId)
        }
    }

    fun refresh(force: Boolean = false) {
        loader.refresh(force)
    }

    fun loadMore() {
        loader.loadMore()
    }

    fun search(text: String = "") {
        loader.search(text)
    }
}