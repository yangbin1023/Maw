package com.magic.maw.ui.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.magic.maw.data.SettingsService
import com.magic.maw.data.loader.PostDataLoader
import com.magic.maw.website.parser.BaseParser

private const val TAG = "PostViewModel"

class PostViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val searchQuery = savedStateHandle.get<String>("searchQuery")
    val loader: PostDataLoader

    init {
        if (!searchQuery.isNullOrEmpty()) {
            val parser = BaseParser.get(SettingsService.settings.website)
            val tags = parser.parseSearchText(searchQuery)
            Logger.d(TAG) { "PostScreen searchQuery: $searchQuery, tags: $tags" }
            loader = PostDataLoader(scope = viewModelScope, tags = tags)
        } else {
            loader = PostDataLoader(scope = viewModelScope)
        }
    }
}