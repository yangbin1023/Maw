package com.magic.maw.data.repository

import androidx.lifecycle.viewModelScope
import com.magic.maw.data.api.parser.WebsiteParserProvider
import com.magic.maw.data.local.db.AppDB
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.RequestOption
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagHistory
import com.magic.maw.data.model.site.PostData
import kotlinx.coroutines.launch

class PostRepository(
    private val provider: WebsiteParserProvider,
    private val settingRepository: SettingsRepository,
    private val db: AppDB,
) {
    fun parseSearchText(website: WebsiteOption, searchQuery: String?): Set<String> {
        val parser = provider[website]
        return if (!searchQuery.isNullOrEmpty()) {
            parser.parseSearchText(searchQuery)
        } else {
            emptySet()
        }
    }

    fun getRequestOption(website: WebsiteOption, option: RequestOption, refresh: Boolean = false): RequestOption {
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