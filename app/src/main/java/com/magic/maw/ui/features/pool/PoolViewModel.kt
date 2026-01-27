package com.magic.maw.ui.features.pool

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.cachedIn
import co.touchlab.kermit.Logger
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.loader.ListUiState
import com.magic.maw.data.api.loader.LoadState
import com.magic.maw.data.api.loader.PoolDataLoader
import com.magic.maw.data.api.loader.PoolDataManager
import com.magic.maw.data.api.loader.PostDataLoader
import com.magic.maw.data.api.loader.PostDataManager
import com.magic.maw.data.api.parser.WebsiteParserProvider
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.local.store.SettingsStore
import com.magic.maw.data.model.RequestOption
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.site.PoolData
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.repository.PoolRepository
import com.magic.maw.data.repository.PostRepository
import com.magic.maw.ui.common.BaseViewModel2
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

private const val TAG = "PoolViewModel"

@OptIn(ExperimentalCoroutinesApi::class)
class PoolViewModel(
    private val provider: ApiServiceProvider,
    private val settingsRepository: SettingsRepository,
    private val poolRepository: PoolRepository,
    private val postRepository: PostRepository,
) : ViewModel() {
    private var website: WebsiteOption
    private var ratings: List<Rating>
    private val refreshSignal = MutableSharedFlow<Unit>(replay = 1).apply {
        tryEmit(Unit)
    }

    val poolFlow: Flow<PagingData<PoolData>>

    var postFlow: Flow<PagingData<PostData>>? = null
        private set

    init {
        val settings = settingsRepository.settings
        ratings = settings.websiteSettings.ratings
        website = settings.website
        poolFlow = refreshSignal
            .flatMapLatest {
                poolRepository.getPoolStream()
            }
            .cachedIn(CoroutineScope(Dispatchers.IO))
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

    fun setViewPoolPost(poolId: String) {
        postFlow = refreshSignal
            .flatMapLatest {
                postRepository.getPostStream(RequestFilter(poolId = poolId))
            }
            .cachedIn(CoroutineScope(Dispatchers.IO))
    }
}