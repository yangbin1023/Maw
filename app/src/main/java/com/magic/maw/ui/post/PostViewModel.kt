package com.magic.maw.ui.post

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import com.magic.maw.util.configFlow
import com.magic.maw.website.LoadStatus
import com.magic.maw.website.RequestOption
import com.magic.maw.website.loadDLFile
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "PostViewModel"

class PostViewModel(
    val dataList: SnapshotStateList<PostData> = SnapshotStateList(),
    private val requestOption: RequestOption = RequestOption(),
) : ViewModel() {
    private var parser: BaseParser
    private val dataIdSet = HashSet<Int>()
    private val modelMap = HashMap<Int, Any?>()
    internal val stateMap = HashMap<String, Any>()
    private var needSearch: Boolean = false
    var showView = MutableStateFlow(false)

    init {
        parser = BaseParser.get(configFlow.value.source.lowercase())
        requestOption.page = parser.firstPageIndex
        requestOption.ratings = configFlow.value.websiteConfig.rating
        showView.onEach { if (!it) viewIndex = -1 }
    }

    var noMore: Boolean by mutableStateOf(false)
    var refreshing: Boolean by mutableStateOf(false)
    var staggered: Boolean by mutableStateOf(false)
    var loading: Boolean by mutableStateOf(false)
    var loadFailed: Boolean by mutableStateOf(false)
    var viewIndex: Int = -1
        set(value) {
            if (value >= 0)
                showView.update { true }
            field = value
        }

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
            needSearch = false
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

    fun search(text: String) {
        needSearch = with(parser) { requestOption.parseSearchText(text) }
        if (needSearch) {
            refresh()
        }
    }

    fun getPreviewModel(context: Context, postData: PostData): MutableStateFlow<LoadStatus<Any>> {
        synchronized(modelMap) {
            modelMap[postData.id]?.let { return MutableStateFlow(LoadStatus.Success(it)) }
        }
        val resultFlow = MutableStateFlow<LoadStatus<Any>>(LoadStatus.Waiting)
        val checkFunc: suspend (LoadStatus<File>) -> Unit = func@{
            if (it is LoadStatus.Success<File> && resultFlow.value !is LoadStatus.Success) {
                val model = ImageRequest.Builder(context).data(it.result).build()
                synchronized(modelMap) {
                    modelMap[postData.id] = model
                }
                resultFlow.value = LoadStatus.Success(model)
            } else if (it is LoadStatus.Error && resultFlow.value !is LoadStatus.Error) {
                resultFlow.value = it
            }
        }
        val statusFlow = loadDLFile(postData, Quality.Preview, viewModelScope)
        viewModelScope.launch {
            checkFunc(statusFlow.value)
            statusFlow.collect {
                checkFunc(it)
            }
        }
        return resultFlow
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
        if (needSearch) {
            force = true
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
    val type = T::class.java.name
    val state = stateMap[type]
    (state as? T)?.let { return it }
    val newOne = onNew()
    stateMap[type] = newOne
    return newOne
}