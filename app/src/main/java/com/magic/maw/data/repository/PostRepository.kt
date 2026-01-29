package com.magic.maw.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.local.db.dao.TagDao
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.paging.PostPagingSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class PostRepository(
    private val apiServiceProvider: ApiServiceProvider,
    private val settingsRepository: SettingsRepository,
    private val tagDao: TagDao,
) {
    fun getPostDataSource(filter: RequestFilter = RequestFilter()): PostDataSource {
        return PostDataSource(
            initFilter = filter,
            apiServiceProvider = apiServiceProvider,
            settingsRepository = settingsRepository,
            tagDao = tagDao,
        )
    }
}

class PostDataSource(
    initFilter: RequestFilter,
    private val apiServiceProvider: ApiServiceProvider,
    private val settingsRepository: SettingsRepository,
    private val tagDao: TagDao,
) {
    private var postPagingSource: PostPagingSource? = null
    private var website: WebsiteOption = settingsRepository.settings.website
    private var ratings: List<Rating> = settingsRepository.settings.websiteSettings.ratings
    var filter: RequestFilter = initFilter
        private set

    val dataFlow = Pager(
        config = PagingConfig(pageSize = 40, enablePlaceholders = false),
        pagingSourceFactory = {
            val settings = settingsRepository.settings
            val apiService = apiServiceProvider[settings.website]
            val filter = this@PostDataSource.filter.copy(ratings = settings.websiteSettings.ratings)
            website = settings.website
            ratings = settings.websiteSettings.ratings
            PostPagingSource(apiService, tagDao, filter).apply {
                postPagingSource = this
            }
        }
    ).flow.cachedIn(CoroutineScope(Dispatchers.IO))

    fun checkAndRefresh() {
        val settings = settingsRepository.settings
        if (settings.website != website || settings.websiteSettings.ratings != ratings) {
            website = settings.website
            ratings = settings.websiteSettings.ratings
            clearData()
        }
    }

    fun clearData() {
        postPagingSource?.invalidate()
    }

    fun setFilter(filter: RequestFilter) {
        if (this.filter != filter) {
            this.filter = filter
            postPagingSource?.invalidate()
        }
    }
}