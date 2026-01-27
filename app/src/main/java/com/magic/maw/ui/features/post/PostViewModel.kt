package com.magic.maw.ui.features.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.paging.PostPagingSource
import com.magic.maw.data.repository.PostRepository
import com.magic.maw.data.repository.TagHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

private const val TAG = "PostViewModel"

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModel(
    savedStateHandle: SavedStateHandle,
    private val provider: ApiServiceProvider,
    private val settingsRepository: SettingsRepository,
    private val postRepository: PostRepository,
    private val tagHistoryRepository: TagHistoryRepository,
) : ViewModel() {
    private var website: WebsiteOption
    private var ratings: List<Rating>
    private val tagNames: Set<String>
    private val refreshSignal = MutableSharedFlow<Unit>(replay = 1).apply {
        tryEmit(Unit)
    }

    val postFlow: Flow<PagingData<PostData>>

    val isSubView: Boolean

    init {
        val settings = settingsRepository.settings
        val searchQuery = savedStateHandle.get<String>("searchQuery")
        website = settings.website
        ratings = settings.websiteSettings.ratings
        tagNames = provider[website].parseSearchQuery(searchQuery)
        isSubView = tagNames.isNotEmpty()
        postFlow = refreshSignal
            .flatMapLatest {
                postRepository.getPostStream(RequestFilter(tags = tagNames))
            }
            .cachedIn(CoroutineScope(Dispatchers.IO))
        if (tagNames.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                tagHistoryRepository.updateTagHistory(website, tags = tagNames)
            }
        }
    }

    fun checkAndRefresh() {
        val settings = settingsRepository.settings
        val website = settings.website
        val ratings = settings.websiteSettings.ratings
        if (website != this.website || ratings.toSet() != this.ratings.toSet()) {
            this.website = website
            this.ratings = ratings
            clearData()
        }
    }

    fun clearData() {
        refreshSignal.tryEmit(Unit)
    }
}