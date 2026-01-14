package com.magic.maw.ui.features.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.magic.maw.data.local.store.SettingsStore
import com.magic.maw.data.api.loader.PostDataLoader
import com.magic.maw.data.api.parser.BaseParser

private const val TAG = "PostViewModel"

class PostViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val searchQuery = savedStateHandle.get<String>("searchQuery")
    val loader: PostDataLoader

    init {
        if (!searchQuery.isNullOrEmpty()) {
            val parser = BaseParser.get(SettingsStore.settings.website)
            val tags = parser.parseSearchText(searchQuery)
            Logger.d(TAG) { "PostScreen searchQuery: $searchQuery, tags: $tags" }
            loader = PostDataLoader(scope = viewModelScope, tags = tags)
        } else {
            loader = PostDataLoader(scope = viewModelScope)
        }
    }
}