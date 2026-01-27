package com.magic.maw.ui.features.pool

import co.touchlab.kermit.Logger
import com.magic.maw.data.api.loader.ListUiState
import com.magic.maw.data.api.loader.LoadState
import com.magic.maw.data.api.loader.PoolDataLoader
import com.magic.maw.data.api.loader.PoolDataManager
import com.magic.maw.data.api.loader.PostDataLoader
import com.magic.maw.data.api.loader.PostDataManager
import com.magic.maw.data.api.parser.WebsiteParserProvider
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.local.store.SettingsStore
import com.magic.maw.data.model.RequestOption
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.site.PoolData
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.repository.PostRepository
import com.magic.maw.ui.common.BaseViewModel2
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "PoolViewModel"

class PoolViewModel(
    private val provider: WebsiteParserProvider,
    private val settingsRepository: SettingsRepository,
//    private val postRepository: PostRepository,
) : BaseViewModel2(), PoolDataLoader {
    private var website: WebsiteOption
    private var poolRequestOption: RequestOption
    private var postRequestOption: RequestOption
    private val poolDataManager = PoolDataManager()
    private val postDataManager = PostDataManager()

    override val uiState: StateFlow<ListUiState<PoolData>> get() = poolDataManager.uiState

    init {
        val settings = settingsRepository.settings
        val ratings = SettingsStore.settings.websiteSettings.ratings
        poolRequestOption = RequestOption(ratings = ratings)
        postRequestOption = RequestOption(ratings = ratings)
        website = settings.website
        refresh()
    }

    override fun checkAndRefresh() {
        val settings = settingsRepository.settings
        val website = settings.website
        val ratings = settings.websiteSettings.ratings
        if (website != this.website || poolRequestOption.ratings.toSet() != ratings.toSet()) {
            this.website = website
            this.poolRequestOption = poolRequestOption.copy(ratings = ratings)
            clearData()
            refresh(true)
        }
    }

    override fun refresh(force: Boolean) {
        Logger.d(TAG) { "refresh called." }
        if (uiState.value.loadState == LoadState.Refreshing) return
        poolDataManager.updateState(LoadState.Refreshing)
        launch {
            try {
                val parser = provider[website]
                val newOption = poolRequestOption.copy(page = parser.firstPageIndex)
                Logger.d(TAG) { "ratings: ${newOption.ratings}" }
                val list = parser.requestPoolData(newOption)
                if (force) {
                    poolDataManager.replaceData(list)
                } else {
                    poolDataManager.appendData(list, false)
                }
                poolRequestOption = newOption
            } catch (_: CancellationException) {
                Logger.d(TAG) { "refresh canceled." }
            } catch (e: Exception) {
                Logger.d(TAG) { "pool loader refresh failed. ${e.message}" }
                poolDataManager.updateState(LoadState.Error(e.message))
            }
        }
    }

    override fun loadMore() {
        Logger.d(TAG) { "loadMore called." }
        if (uiState.value.isLoading) return
        poolDataManager.updateState(LoadState.LoadingMore)
        launch {
            try {
                val parser = provider[website]
                val newOption = poolRequestOption.copy(page = poolRequestOption.page + 1)
                val list = parser.requestPoolData(newOption)
                if (uiState.value.loadState == LoadState.LoadingMore) {
                    poolDataManager.appendData(list)
                    poolRequestOption = newOption
                }
            } catch (e: Exception) {
                Logger.d(TAG) { "pool loader load more failed. ${e.message}" }
                poolDataManager.updateState(LoadState.Error(e.message))
            }
        }
    }

    override fun clearData() {
        poolDataManager.clearData()
    }

    fun setViewPoolPost(poolId: String) {
        postRequestOption = postRequestOption.copy(poolId = poolId)
        postLoader.clearData()
        postLoader.refresh()
    }

    val postLoader = object : PostDataLoader {
        override val uiState: StateFlow<ListUiState<PostData>>
            get() = postDataManager.uiState

        override fun checkAndRefresh() {
        }

        override fun refresh(force: Boolean) {
        }

        override fun loadMore() {
        }

        override fun clearData() {
            postDataManager.clearData()
        }
    }
}