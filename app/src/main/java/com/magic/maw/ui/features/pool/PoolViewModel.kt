package com.magic.maw.ui.features.pool

import androidx.lifecycle.ViewModel
import androidx.paging.cachedIn
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.repository.PoolRepository
import com.magic.maw.data.repository.PostDataSource
import com.magic.maw.data.repository.PostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest

class PoolViewModel(
    private val settingsRepository: SettingsRepository,
    private val poolRepository: PoolRepository,
    private val postRepository: PostRepository,
) : ViewModel() {
    private var website: WebsiteOption
    private var ratings: List<Rating>
    private val refreshSignal = MutableSharedFlow<Unit>(replay = 1).apply {
        tryEmit(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val poolFlow = refreshSignal
        .flatMapLatest { poolRepository.getPoolStream() }
        .cachedIn(CoroutineScope(Dispatchers.IO))

    var postSource: PostDataSource? = null
        private set

    init {
        val settings = settingsRepository.settings
        ratings = settings.websiteSettings.ratings
        website = settings.website
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
        postSource = postRepository.getPostDataSource(filter = RequestFilter(poolId = poolId))
    }
}