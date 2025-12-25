package com.magic.maw.data.loader

import co.touchlab.kermit.Logger
import com.magic.maw.data.PoolData
import com.magic.maw.data.SettingsService
import com.magic.maw.data.WebsiteOption
import com.magic.maw.website.RequestOption
import com.magic.maw.website.parser.BaseParser
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val TAG = "PoolDataLoader"

typealias PoolDataUiState = ListUiState<PoolData>

class PoolDataLoader(val scope: CoroutineScope) : DataLoader<PoolData> {
    private var _website: WebsiteOption = SettingsService.settings.website
    private var parser = BaseParser.get(_website)
    private var scopeJob: Job? = null
    private var requestOption: RequestOption
    private val dataIdSet = mutableSetOf<Int>()

    val website get() = _website

    private val _uiState = MutableStateFlow(PoolDataUiState())

    override val uiState: StateFlow<PoolDataUiState> = _uiState.asStateFlow()

    init {
        requestOption = RequestOption(
            page = parser.firstPageIndex,
            ratingSet = SettingsService.settings.websiteSettings.ratings
        )
        refresh()
        scope.launch {
            SettingsService.settingsState.collect { settingsState ->
                if (settingsState.website != _website) {
                    _website = settingsState.website
                    parser = BaseParser.get(_website)
                    requestOption = RequestOption(
                        page = parser.firstPageIndex,
                        ratingSet = settingsState.websiteSettings.ratings
                    )
                    refresh(true)
                } else if (requestOption.ratingSet.toSet() != settingsState.websiteSettings.ratings.toSet()) {
                    requestOption = RequestOption(ratingSet = settingsState.websiteSettings.ratings)
                    refresh(true)
                }
            }
        }
    }

    override fun refresh(force: Boolean) {
        Logger.d(TAG) { "refresh called." }
        if (uiState.value.loadState == LoadState.Refreshing) return
        _uiState.update { it.copy(loadState = LoadState.Refreshing) }
        launch {
            try {
                val newOption = requestOption.copy(page = parser.firstPageIndex)
                Logger.d(TAG) { "ratings: ${newOption.ratingSet}" }
                val list = parser.requestPoolData(newOption)
                if (force) {
                    replaceAllData(list)
                } else {
                    appendData(list, false)
                }
                requestOption = newOption
            } catch (_: CancellationException) {
                Logger.d(TAG) { "refresh canceled." }
            } catch (e: Exception) {
                Logger.d(TAG) { "pool loader refresh failed. ${e.message}" }
                _uiState.update { it.copy(loadState = LoadState.Error(message = e.message)) }
            }
        }
    }

    override fun loadMore() {
        Logger.d(TAG) { "loadMore called." }
        if (uiState.value.isLoading) return
        _uiState.update { it.copy(loadState = LoadState.LoadingMore) }
        launch {
            try {
                val newOption = requestOption.copy(page = requestOption.page + 1)
                val list = parser.requestPoolData(newOption)
                if (uiState.value.loadState == LoadState.LoadingMore) {
                    appendData(list)
                    requestOption = newOption
                }
            } catch (e: Exception) {
                Logger.d(TAG) { "pool loader load more failed. ${e.message}" }
                _uiState.update { it.copy(loadState = LoadState.Error(message = e.message)) }
            }
        }
    }

    override fun clearData() {
        _uiState.update { it.copy(items = persistentListOf(), loadState = LoadState.Idle) }
    }

    private fun replaceAllData(list: List<PoolData>) {
        _uiState.update { currentState ->
            dataIdSet.clear()
            for (item in list) {
                dataIdSet.add(item.id)
            }
            currentState.copy(
                items = list.toPersistentList(),
                loadState = LoadState.Idle
            )
        }
    }

    private fun appendData(list: List<PoolData>, appendEnd: Boolean = true) {
        _uiState.update { currentState ->
            val toInsert = mutableListOf<PoolData>()
            var updatedList = currentState.items

            list.forEach { newData ->
                if (dataIdSet.contains(newData.id)) {
                    val index = updatedList.indexOfFirst { it.id == newData.id }
                    if (index != -1) {
                        updatedList = updatedList.set(index, newData)
                    }
                } else {
                    dataIdSet.add(newData.id)
                    toInsert.add(newData)
                }
            }

            val finalList = if (appendEnd) {
                updatedList.addAll(toInsert)
            } else {
                updatedList.addAll(0, toInsert)
            }

            currentState.copy(
                items = finalList,
                loadState = LoadState.Idle
            )
        }
    }

    private fun launch(
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ) {
        synchronized(this) {
            scopeJob?.let { if (it.isActive) it.cancel() }
            scopeJob = scope.launch(context = context, start = start, block = block)
        }
    }
}