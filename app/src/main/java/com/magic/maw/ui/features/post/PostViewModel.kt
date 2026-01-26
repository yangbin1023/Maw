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
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.paging.PostPagingSource
import com.magic.maw.data.repository.TagHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private const val TAG = "PostViewModel"

class PostViewModel(
    savedStateHandle: SavedStateHandle,
    private val provider: ApiServiceProvider,
    private val settingsRepository: SettingsRepository,
    private val tagHistoryRepository: TagHistoryRepository
) : ViewModel() {
    private var website: WebsiteOption
    private var currentPagingSource: PostPagingSource? = null
    private val pager: Pager<String, PostData>

    val postPager: Flow<PagingData<PostData>>

    val isSubView: Boolean
        get() = currentPagingSource?.filter?.tags?.isNotEmpty() == true

    init {
        val settings = settingsRepository.settings
        website = settings.website
        val searchQuery = savedStateHandle.get<String>("searchQuery")
        val apiService = provider[website]
        val tags = apiService.parseSearchQuery(searchQuery)
        if (tags.isNotEmpty()) {
            viewModelScope.launch {
                tagHistoryRepository.updateTagHistory(website, tags)
            }
        }
        pager = Pager(
            config = PagingConfig(pageSize = 40, enablePlaceholders = false),
            pagingSourceFactory = {
                val ratings = settingsRepository.settings.danbooruSettings.ratings
                val filter = RequestFilter(ratings = ratings, tags = tags)
                PostPagingSource(apiService, filter).also {
                    currentPagingSource = it
                }
            }
        )
        postPager = pager.flow.cachedIn(CoroutineScope(Dispatchers.IO))
    }

    fun checkAndRefresh() {
        val settings = settingsRepository.settings
        val website = settings.website
        val ratings = settings.websiteSettings.ratings
        val filterRatings = currentPagingSource?.filter?.ratings
        if (website != this.website || filterRatings != null && filterRatings.toSet() != ratings.toSet()) {
            this.website = website
            clearData()
        }
    }

    fun clearData() {
        currentPagingSource?.invalidate()
    }
}