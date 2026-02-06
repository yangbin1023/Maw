package com.magic.maw.ui.features.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.repository.PostDataSource
import com.magic.maw.data.repository.PostRepository
import com.magic.maw.data.repository.TagHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostViewModel(
    savedStateHandle: SavedStateHandle,
    apiServiceProvider: ApiServiceProvider,
    settingsRepository: SettingsRepository,
    postRepository: PostRepository,
    tagHistoryRepository: TagHistoryRepository,
) : ViewModel() {
    private val tagNames: Set<String>

    val dataSource: PostDataSource

    val isSubView: Boolean
        get() = tagNames.isNotEmpty()

    init {
        val settings = settingsRepository.settings
        val searchQuery = savedStateHandle.get<String>("searchQuery")
        tagNames = apiServiceProvider[settings.website].parseSearchQuery(searchQuery)
        dataSource = postRepository.getPostDataSource(RequestFilter(tags = tagNames))
        if (tagNames.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                tagHistoryRepository.updateTagHistory(settings.website.name, tags = tagNames)
            }
        }
    }

    fun checkAndRefresh() {
        dataSource.checkAndRefresh()
    }
}