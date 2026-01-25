package com.magic.maw.ui.features.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.magic.maw.data.api.loader.ListUiState
import com.magic.maw.data.api.loader.LoadState
import com.magic.maw.data.api.loader.PostDataLoader
import com.magic.maw.data.api.loader.PostDataManager
import com.magic.maw.data.local.db.AppDB
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.RequestOption
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagHistory
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.repository.PostRepository
import com.magic.maw.ui.common.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "PostViewModel"

class PostViewModel(
    savedStateHandle: SavedStateHandle,
    private val postRepository: PostRepository,
    private val settingsRepository: SettingsRepository,
    private val db: AppDB,
) : BaseViewModel(), PostDataLoader {
    private var website: WebsiteOption
    private var requestOption: RequestOption = RequestOption()
    private val dataManager: PostDataManager = PostDataManager()
    override val uiState: StateFlow<ListUiState<PostData>> get() = dataManager.uiState

    init {
        val settings = settingsRepository.settings
        website = settings.website
        val searchQuery = savedStateHandle.get<String>("searchQuery")
        val tags = postRepository.parseSearchText(website, searchQuery)
        if (tags.isNotEmpty()) {
            viewModelScope.launch {
                tags.forEach { tag ->
                    if (tag.isNotBlank()) {
                        db.tagHistoryDao().upsert(TagHistory(website = website, name = tag))
                    }
                }
            }
        }
        val ratings = settings.websiteSettings.ratings
        requestOption = RequestOption(ratings = ratings, tags = tags)
        requestOption = postRepository.getRequestOption(website, requestOption, true)
        refresh()
    }

    override fun checkAndRefresh() {
        val settings = settingsRepository.settings
        val website = settings.website
        val ratings = settings.websiteSettings.ratings
        if (website != this.website || requestOption.ratings.toSet() != ratings.toSet()) {
            this.website = website
            this.requestOption = requestOption.copy(ratings = ratings)
            clearData()
            refresh(true)
        }
    }

    override fun refresh(force: Boolean) {
        Logger.d(TAG) { "refresh called. force: $force, tags: ${requestOption.tags}" }
        if (uiState.value.loadState == LoadState.Refreshing) return
        dataManager.updateState(LoadState.Refreshing)
        launch {
            try {
                val newOption = postRepository.getRequestOption(website, requestOption, true)
                Logger.d(TAG) { "ratings: ${newOption.ratings}, tags: ${newOption.tags}" }
                val list = postRepository.requestPostData(website, newOption)
                if (force) {
                    dataManager.replaceData(list)
                } else {
                    dataManager.appendData(list, false)
                }
                requestOption = newOption
            } catch (_: CancellationException) {
                Logger.d(TAG) { "refresh canceled." }
            } catch (e: Exception) {
                Logger.d(TAG) { "post loader refresh failed. ${e.message}" }
                dataManager.updateState(LoadState.Error(message = e.message))
            }
        }
    }

    override fun loadMore() {
        Logger.d(TAG) { "loadMore called." }
        if (uiState.value.isLoading || uiState.value.hasNoMore) return
        dataManager.updateState(LoadState.LoadingMore)
        launch {
            try {
                val newOption = postRepository.getRequestOption(website, requestOption)
                val list = postRepository.requestPostData(website, newOption)
                if (uiState.value.loadState == LoadState.LoadingMore) {
                    dataManager.appendData(list)
                    requestOption = newOption
                }
            } catch (e: Exception) {
                Logger.d(TAG) { "post loader load more failed. ${e.message}" }
                dataManager.updateState(LoadState.Error(message = e.message))
            }
        }
    }

    override fun clearData() = dataManager.clearData()

    fun isSearchScreen(): Boolean {
        return requestOption.tags.isNotEmpty()
    }
}