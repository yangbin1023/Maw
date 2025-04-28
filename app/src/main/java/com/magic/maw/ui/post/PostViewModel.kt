package com.magic.maw.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.PostData
import com.magic.maw.util.Logger
import com.magic.maw.util.configFlow
import com.magic.maw.website.RequestOption
import com.magic.maw.website.parser.BaseParser
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PostViewModel"
private val logger = Logger(TAG)

enum class UiStateType {
    None,
    LoadMore,
    Refresh,
    LoadFailed;

    fun isLoading(): Boolean {
        return this == LoadMore || this == Refresh
    }
}

sealed interface PostUiState {
    val noMore: Boolean
    val type: UiStateType
    val dataList: List<PostData>

    data class Post(
        val isSearch: Boolean,
        override val noMore: Boolean,
        override val type: UiStateType,
        override val dataList: List<PostData>,
    ) : PostUiState

    data class View(
        val initIndex: Int,
        override val noMore: Boolean,
        override val type: UiStateType,
        override val dataList: List<PostData>,
    ) : PostUiState
}

private data class PostViewModelState(
    val dataList: List<PostData> = emptyList(),
    val noMore: Boolean = true,
    val type: UiStateType = UiStateType.None,
    val viewIndex: AtomicInt = atomic(-1),
    val requestOption: RequestOption
) {
    private val dataMap: HashMap<Int, PostData> = HashMap()

    init {
        for (data in dataList) {
            dataMap[data.id] = data
        }
    }

    fun toUiState(): PostUiState {
        return if (viewIndex.value < 0) {
            PostUiState.Post(
                isSearch = requestOption.tags.isNotEmpty(),
                noMore = noMore,
                type = type,
                dataList = dataList
            )
        } else {
            PostUiState.View(
                initIndex = viewIndex.value,
                noMore = noMore,
                type = type,
                dataList = dataList
            )
        }
    }

    fun append(
        dataList: List<PostData>,
        end: Boolean = true,
        type: UiStateType = UiStateType.None,
        checkReplace: Boolean = false,
    ): PostViewModelState {
        val list: MutableList<PostData> = ArrayList()
        for (data in dataList) {
            dataMap[data.id]?.updateFrom(data) ?: let {
                dataMap[data.id] = data
                list.add(data)
            }
        }
        if (checkReplace && list.size == dataList.size && !end) {
            return copy(dataList = list, noMore = false, type = UiStateType.None)
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

class PostViewModel(
    requestOption: RequestOption = RequestOption(),
    dataList: List<PostData> = emptyList(),
) : ViewModel() {
    private val viewModelState = MutableStateFlow(
        PostViewModelState(
            noMore = false,
            requestOption = requestOption,
            dataList = dataList
        )
    )

    val uiState = viewModelState
        .map(PostViewModelState::toUiState)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            viewModelState.value.toUiState()
        )

    init {
        requestOption.page = getParser().firstPageIndex
        requestOption.ratings = configFlow.value.websiteConfig.rating
        if (dataList.isEmpty()) {
            refresh(true)
        }
    }

    fun update(dataList: List<PostData>) {
        viewModelState.update {
            it.copy(
                dataList = dataList,
                noMore = false,
                type = UiStateType.None
            )
        }
    }

    fun refresh(force: Boolean = checkForceRefresh()) {
        synchronized(this) {
            if (viewModelState.value.type == UiStateType.Refresh)
                return
            viewModelState.update { it.copy(type = UiStateType.Refresh) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val option = viewModelState.value.requestOption
                val parser = getParser()
                option.ratings = configFlow.value.websiteConfig.rating
                val list = if (option.poolId > 0) {
                    parser.requestPoolPostData(option.copy(page = parser.firstPageIndex))
                } else {
                    parser.requestPostData(option.copy(page = parser.firstPageIndex))
                }
                viewModelState.update {
                    if (force) {
                        it.requestOption.page = parser.firstPageIndex
                        it.copy(dataList = list, noMore = false, type = UiStateType.None)
                    } else {
                        it.append(dataList = list, end = false, checkReplace = true)
                    }
                }
            } catch (e: Throwable) {
                logger.severe("refresh failed: ${e.message}")
                viewModelState.update { it.copy(type = UiStateType.LoadFailed) }
            }
        }
    }

    fun loadMore() {
        if (viewModelState.value.type.isLoading())
            return
        viewModelState.update { it.copy(type = UiStateType.LoadMore) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parser = getParser()
                val option = viewModelState.value.requestOption
                val list = if (option.poolId > 0) {
                    parser.requestPoolPostData(option.copy(page = option.page + 1))
                } else {
                    parser.requestPostData(option.copy(page = option.page + 1))
                }
                viewModelState.update {
                    if (it.type != UiStateType.Refresh && list.isNotEmpty()) {
                        it.requestOption.page++
                        it.append(dataList = list)
                    } else {
                        it
                    }
                }
            } catch (e: Throwable) {
                logger.severe("loadMore failed: ${e.message}")
                viewModelState.update { it.copy(type = UiStateType.LoadFailed) }
            }
        }
    }

    fun search(text: String) = with(getParser()) {
        val list = viewModelState.value.requestOption.parseSearchText(text)
        if (list.isNotEmpty() && list != viewModelState.value.requestOption.tags.toList()) {
            viewModelState.value.requestOption.clearTags()
            viewModelState.value.requestOption.addTags(list)
            tagManager.dealSearchTags(list)
            refresh(true)
        }
    }

    fun clearTags(): Boolean {
        val notEmpty = viewModelState.value.requestOption.tags.isNotEmpty()
        viewModelState.value.requestOption.clearTags()
        return notEmpty
    }

    fun clearData() {
        viewModelState.update { it.copy(dataList = emptyList()) }
    }

    fun setViewIndex(index: Int) {
        viewModelState.update {
            if (index >= 0 && index < it.dataList.size) {
                it.copy(viewIndex = atomic(index))
            } else {
                it.copy(viewIndex = atomic(-1))
            }
        }
    }

    fun exitView() {
        if (viewModelState.value.viewIndex.value < 0)
            return
        viewModelState.update { it.copy(viewIndex = atomic(-1)) }
    }

    private fun getParser(): BaseParser {
        return BaseParser.get(configFlow.value.source)
    }

    private fun checkForceRefresh(): Boolean {
        return configFlow.value.websiteConfig.rating != viewModelState.value.requestOption.ratings
    }

    companion object {
        fun providerFactory(
            requestOption: RequestOption = RequestOption(),
            dataList: List<PostData> = emptyList(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PostViewModel(requestOption, dataList) as T
            }
        }
    }
}
