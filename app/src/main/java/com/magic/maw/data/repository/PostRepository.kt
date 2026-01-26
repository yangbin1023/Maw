package com.magic.maw.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.magic.maw.data.api.parser.WebsiteParserProvider
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

class PostRepository(
    private val provider: WebsiteParserProvider,
    private val settingRepository: SettingsRepository,
    private val db: AppDB,
) {
    private var currentPagingSource: PostPagingSource? = null

    val postPager = Pager(
        config = PagingConfig(pageSize = 40, enablePlaceholders = false),
        pagingSourceFactory = {
            PostPagingSource(DanbooruApiService(client = client)).also {
                currentPagingSource = it
            }
        }
    ).flow.cachedIn(CoroutineScope(Dispatchers.IO))

    fun parseSearchText(website: WebsiteOption, searchQuery: String?): Set<String> {
        val parser = provider[website]
        return if (!searchQuery.isNullOrEmpty()) {
            parser.parseSearchText(searchQuery)
        } else {
            emptySet()
        }
    }

    fun getRequestOption(
        website: WebsiteOption,
        option: RequestOption,
        refresh: Boolean = false
    ): RequestOption {
        val parser = provider[website]
        return if (refresh) {
            option.copy(page = parser.firstPageIndex)
        } else {
            option.copy(page = option.page + 1)
        }
    }

    suspend fun requestPostData(website: WebsiteOption, option: RequestOption): List<PostData> {
        return provider[website].requestPostData(option)
    }
}