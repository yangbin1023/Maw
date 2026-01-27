package com.magic.maw.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.site.PoolData
import com.magic.maw.data.paging.PoolPagingSource
import kotlinx.coroutines.flow.Flow

class PoolRepository(
    private val provider: ApiServiceProvider,
    private val settingRepository: SettingsRepository,
) {
    fun getPoolStream(filter: RequestFilter = RequestFilter()): Flow<PagingData<PoolData>> {
        return Pager(
            config = PagingConfig(pageSize = 40, enablePlaceholders = false),
            pagingSourceFactory = {
                val settings = settingRepository.settings
                val apiService = provider[settings.website]
                val filter = filter.copy(ratings = settings.websiteSettings.ratings)
                PoolPagingSource(apiService, filter)
            }
        ).flow
    }
}