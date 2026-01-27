package com.magic.maw.ui.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.repository.TagHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val provider: ApiServiceProvider,
    private val tagHistoryRepository: TagHistoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val tagHistoryFlow: StateFlow<List<TagInfo>> = tagHistoryRepository.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteHistory(name: String) = viewModelScope.launch {
        val website = settingsRepository.settings.website
        tagHistoryRepository.deleteHistory(website, name)
    }

    fun deleteAllHistory() = viewModelScope.launch {
        val website = settingsRepository.settings.website
        tagHistoryRepository.deleteAllHistory(website)
    }

    suspend fun requestSuggestTagInfo(name: String): List<TagInfo> {
        val website = settingsRepository.settings.website
        val apiService = provider[website]
        val tags = apiService.getSuggestTagInfo(name)
        return tags
    }
}