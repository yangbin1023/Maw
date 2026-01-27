package com.magic.maw.data.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import coil3.request.ImageRequest
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.parser.WebsiteParserProvider
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.api.service.DanbooruApiService
import com.magic.maw.data.local.db.AppDB
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.RequestOption
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.paging.PostPagingSource
import com.magic.maw.util.client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PostRepository(
    private val provider: ApiServiceProvider,
    private val settingRepository: SettingsRepository,
    private val context: Context,//: AppDB,
) {
    fun getPostStream(filter: RequestFilter = RequestFilter()): Flow<PagingData<PostData>> {
        return Pager(
            config = PagingConfig(pageSize = 40, enablePlaceholders = false),
            pagingSourceFactory = {
                val settings = settingRepository.settings
                val apiService = provider[settings.website]
                val filter = filter.copy(ratings = settings.websiteSettings.ratings)
                PostPagingSource(apiService, filter)
            }
        ).flow
    }

    fun getRequestOption(
        website: WebsiteOption,
        option: RequestOption,
        refresh: Boolean = false
    ): RequestOption {
        return if (refresh) {
            option.copy(page = 1)
        } else {
            option.copy(page = option.page + 1)
        }
    }

    suspend fun requestPostData(website: WebsiteOption, option: RequestOption): List<PostData> {
        return emptyList()// provider[website].getPostData(option)
    }
}