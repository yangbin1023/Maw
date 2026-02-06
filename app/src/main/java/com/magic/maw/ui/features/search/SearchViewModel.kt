package com.magic.maw.ui.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.repository.TagHistoryRepository
import com.magic.maw.data.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val provider: ApiServiceProvider,
    private val tagHistoryRepository: TagHistoryRepository,
    private val tagRepository: TagRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    @Suppress("UnusedFlow")
    @OptIn(ExperimentalCoroutinesApi::class)
    val tagHistoryFlow: StateFlow<List<TagInfo>> = tagHistoryRepository.getAllHistory()
        .flatMapLatest { histories ->
            if (histories.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val names = histories.map { it.name }
            val website = histories.first().website

            tagRepository.getTagsByNamesFlow(website, names).map { infoList ->
                val infoMap = infoList.associateBy { it.name }

                histories.map { history ->
                    infoMap[history.name] ?: TagInfo(name = history.name, website = website)
                }
            }
        }
        .flowOn(Dispatchers.Default) // 确保复杂的 Map 转换在计算线程执行
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteHistory(name: String) = viewModelScope.launch {
        val website = settingsRepository.settings.website
        tagHistoryRepository.deleteHistory(website.name, name)
    }

    fun deleteAllHistory() = viewModelScope.launch {
        val website = settingsRepository.settings.website
        tagHistoryRepository.deleteAllHistory(website.name)
    }

    suspend fun requestSuggestTagInfo(name: String): List<TagInfo> {
        val website = settingsRepository.settings.website
        val apiService = provider[website]
        val tags = apiService.getSuggestTagInfo(name)
        return tags
    }
}