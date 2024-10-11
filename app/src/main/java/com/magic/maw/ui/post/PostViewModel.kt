package com.magic.maw.ui.post

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import com.magic.maw.data.PostData
import com.magic.maw.util.configFlow
import com.magic.maw.website.RequestOption
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.launch
import java.lang.reflect.Type

private const val TAG = "PostViewModel"

class PostViewModel(
    val dataList: SnapshotStateList<PostData> = SnapshotStateList(),
    private val requestOption: RequestOption = RequestOption(),
) : ViewModel() {
    private var parser: BaseParser
    private val dataIdSet = HashSet<Int>()
    internal val stateMap = HashMap<Type, Any>()

    init {
        parser = BaseParser.get(configFlow.value.source.lowercase())
        requestOption.page = parser.firstPageIndex
        requestOption.ratings = configFlow.value.websiteConfig.rating
    }

    var noMore: Boolean by mutableStateOf(false)
    var refreshing: Boolean by mutableStateOf(false)
    var staggered: Boolean by mutableStateOf(false)
    var loading: Boolean by mutableStateOf(false)
    var loadFailed: Boolean by mutableStateOf(false)
    var viewIndex: Int by mutableIntStateOf(-1)

    fun checkRefresh() {
        if (dataList.isEmpty() && !noMore && !refreshing && !loadFailed) {
            Log.d(TAG, "check to refresh")
            refresh()
        }
    }

    fun refresh() = viewModelScope.launch {
        val force = checkForceRefresh()
        if (force)
            loadFailed = false
        refreshing = true
        try {
            val list = parser.requestPostData(requestOption.copy(page = parser.firstPageIndex))

            val tmpIdSet = HashSet<Int>()
            val tmpList = ArrayList<PostData>()
            if (force) {
                for (item in list) {
                    if (item.id > 0) {
                        tmpList.add(item)
                        tmpIdSet.add(item.id)
                    }
                }
                synchronized(this@PostViewModel) {
                    dataIdSet.clear()
                    dataList.clear()
                    noMore = false
                    requestOption.page = parser.firstPageIndex
                }
            } else {
                for (item in list) {
                    if (item.id > 0 && !dataIdSet.contains(item.id)) {
                        tmpList.add(item)
                        tmpIdSet.add(item.id)
                    }
                }
            }

            if (tmpList.isNotEmpty()) {
                synchronized(this@PostViewModel) {
                    dataIdSet.addAll(tmpIdSet)
                    dataList.addAll(0, tmpList)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "request failed", e)
            loadFailed = true
        } finally {
            refreshing = false
            loading = false
        }
    }

    fun loadMore() = viewModelScope.launch {
        if (refreshing || loading)
            return@launch
        loading = true
        val list: List<PostData>
        try {
            list = parser.requestPostData(requestOption.copy(page = requestOption.page + 1))
            if (refreshing) {
                loading = false
                return@launch
            }
        } catch (e: Exception) {
            loading = false
            Log.e(TAG, "load more failed: " + e.message, e)
            return@launch
        }

        val tmpIdSet = HashSet<Int>()
        val tmpList = ArrayList<PostData>()
        for (item in list) {
            if (item.id > 0) {
                tmpList.add(item)
                tmpIdSet.add(item.id)
            }
        }

        requestOption.page++

        if (tmpList.isNotEmpty()) {
            synchronized(this@PostViewModel) {
                dataIdSet.addAll(tmpIdSet)
                dataList.addAll(tmpList)
            }
        } else {
            noMore = true
        }
        loading = false
    }

    private fun checkForceRefresh(): Boolean {
        var force = false
        if (parser.source.lowercase() != configFlow.value.source.lowercase()) {
            parser = BaseParser.get(configFlow.value.source)
            force = true
        }
        if (requestOption.ratings != configFlow.value.websiteConfig.rating) {
            force = true
            requestOption.ratings = configFlow.value.websiteConfig.rating
        }
        if (force) {
            requestOption.page = parser.firstPageIndex
            println("Force refresh. requestOption.ratings: ${requestOption.ratings}")
        }
        return force
    }

    companion object {
        fun providerFactory(
            dataList: SnapshotStateList<PostData> = SnapshotStateList(),
            requestOption: RequestOption = RequestOption(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PostViewModel(dataList, requestOption) as T
            }
        }
    }
}

@Composable
internal inline fun <reified T : Any> PostViewModel.getState(onNew: @Composable () -> T): T {
    val type = object : TypeToken<T>() {}.type
    val state = stateMap[type]
    (state as? T)?.let { return it }
    val newOne = onNew()
    stateMap[type] = newOne
    return newOne
}