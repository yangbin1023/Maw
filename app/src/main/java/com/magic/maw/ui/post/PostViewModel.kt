package com.magic.maw.ui.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.PostData
import com.magic.maw.util.configFlow
import com.magic.maw.website.RequestOption
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PostViewModel"

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
    val viewIndex: Int = -1,
    val requestOption: RequestOption
) {
    private val dataIdSet: HashSet<Int> = HashSet()

    init {
        for (data in dataList) {
            dataIdSet.add(data.id)
        }
    }

    fun toUiState(): PostUiState {
        return if (viewIndex < 0) {
            PostUiState.Post(
                isSearch = requestOption.tags.isNotEmpty(),
                noMore = noMore,
                type = type,
                dataList = dataList
            )
        } else {
            PostUiState.View(
                initIndex = viewIndex,
                noMore = noMore,
                type = type,
                dataList = dataList
            )
        }
    }

    fun append(
        dataList: List<PostData>,
        end: Boolean = true,
        type: UiStateType = UiStateType.None
    ): PostViewModelState {
        val list: MutableList<PostData> = ArrayList()
        for (data in dataList) {
            if (!dataIdSet.contains(data.id)) {
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

class PostViewModel(
    private val parser: BaseParser,
    requestOption: RequestOption = RequestOption(),
) : ViewModel() {
    private val viewModelState = MutableStateFlow(
        PostViewModelState(
            noMore = false,
            requestOption = requestOption,
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
        requestOption.page = parser.firstPageIndex
        requestOption.ratings = configFlow.value.websiteConfig.rating
        refresh(true)
    }

    fun refresh(force: Boolean = checkForceRefresh()) {
        if (viewModelState.value.type == UiStateType.Refresh)
            return
        viewModelState.update { it.copy(type = UiStateType.Refresh) }
        viewModelScope.launch {
            try {
                val option = viewModelState.value.requestOption
                val list = parser.requestPostData(option.copy(page = parser.firstPageIndex))
                viewModelState.update {
                    if (force) {
                        it.copy(dataList = list, noMore = false, type = UiStateType.None)
                    } else {
                        it.append(dataList = list, end = false, type = UiStateType.None)
                    }
                }
            } catch (e: Throwable) {
                Log.d(TAG, "refresh failed: ${e.message}")
                viewModelState.update { it.copy(type = UiStateType.LoadFailed) }
            }
        }
    }

    fun loadMore() {
        if (viewModelState.value.type.isLoading())
            return
        viewModelState.update { it.copy(type = UiStateType.LoadMore) }
        viewModelScope.launch {
            try {
                val option = viewModelState.value.requestOption
                val list = parser.requestPostData(option.copy(page = option.page + 1))
                viewModelState.update {
                    if (it.type != UiStateType.Refresh) {
                        it.append(dataList = list, type = UiStateType.None)
                    } else {
                        it
                    }
                }
            } catch (e: Throwable) {
                Log.d(TAG, "loadMore failed: ${e.message}")
                viewModelState.update { it.copy(type = UiStateType.LoadFailed) }
            }
        }
    }

    fun search(text: String) {
        with(parser) {
            if (viewModelState.value.requestOption.parseSearchText(text)) {
                refresh(true)
            }
        }
    }

    fun clearTags() {
        viewModelState.value.requestOption.clearTags()
    }

    fun setViewIndex(index: Int) {
        viewModelState.update {
            if (index >= 0 && index < it.dataList.size) {
                it.copy(viewIndex = index)
            } else {
                it
            }
        }
    }

    fun exitView() {
        viewModelState.update { it.copy(viewIndex = -1) }
    }

    private fun checkForceRefresh(): Boolean {
        return configFlow.value.websiteConfig.rating != viewModelState.value.requestOption.ratings
    }

    companion object {
        fun providerFactory(
            parser: BaseParser = BaseParser.get(configFlow.value.source),
            requestOption: RequestOption = RequestOption(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PostViewModel(parser, requestOption) as T
            }
        }
    }
}

//@Composable
//internal inline fun <reified T : Any> PostViewModel.getState(onNew: @Composable () -> T): T {
//    val type = T::class.java.name
//    val state = stateMap[type]
//    (state as? T)?.let { return it }
//    val newOne = onNew()
//    stateMap[type] = newOne
//    return newOne
//}