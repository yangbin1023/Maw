package com.magic.maw.ui.post

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.PostData
import com.magic.maw.website.RequestOption
import com.magic.maw.website.parser.BaseParser
import com.magic.maw.website.parser.YandeParser
import kotlinx.coroutines.launch

private const val TAG = "PostViewModel"

class PostViewModel(
    val dataList: SnapshotStateList<PostData> = SnapshotStateList(),
    private val requestOption: RequestOption = RequestOption(),
) : ViewModel() {
    private val dataIdSet = HashSet<Int>()
    private val parser = BaseParser.getParser(YandeParser.SOURCE)

    init {
        requestOption.page = parser.firstPageIndex
    }

    var noMore: Boolean by mutableStateOf(false)
    var refreshing: Boolean by mutableStateOf(false)
    var staggered: Boolean by mutableStateOf(false)
    var loading: Boolean by mutableStateOf(false)

    fun checkRefresh() {
        if (dataList.isEmpty() && !noMore && !refreshing) {
            Log.d(TAG, "check to refresh")
            refresh()
        }
    }

    fun refresh(focus: Boolean = false) = viewModelScope.launch {
        refreshing = true
        try {
            val list = parser.requestPostData(requestOption.copy(page = parser.firstPageIndex))

            val tmpIdSet = HashSet<Int>()
            val tmpList = ArrayList<PostData>()
            if (focus) {
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
        } finally {
            refreshing = false
            loading = false
        }
    }

    fun loadMore() {
        if (refreshing || loading)
            return
        loading = true
        viewModelScope.launch {
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