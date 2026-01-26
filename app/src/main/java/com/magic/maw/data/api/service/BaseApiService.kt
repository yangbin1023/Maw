package com.magic.maw.data.api.service

import com.magic.maw.data.api.entity.PostResponse
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.entity.RequestMeta
import com.magic.maw.data.api.parser.BaseParser.Companion.decode
import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption

abstract class BaseApiService {
    protected abstract val baseUrl: String
    abstract val website: WebsiteOption
    abstract val supportedRatings: List<Rating>
    open val supportedPopularDateTypes: List<PopularType> = PopularType.defaultSupportedDateTypes

    abstract suspend fun getPostData(filter: RequestFilter, meta: RequestMeta): PostResponse

    open fun parseSearchQuery(searchQuery: String?): Set<String> {
        if (searchQuery.isNullOrBlank())
            return emptySet()
        val tagTexts = searchQuery.decode().split(" ")
        val tagList = mutableSetOf<String>()
        for (item in tagTexts) {
            val tagText = item.removePrefix("-")
            if (tagText.isEmpty() || tagText.startsWith("tag:"))
                continue
            tagList.add(tagText)
        }
        return tagList
    }

    protected val RequestMeta.page: Int
        get() = try {
            next?.toInt() ?: 1
        } catch (_: Exception) {
            1
        }
}

class ApiServiceProvider(private val lists: List<BaseApiService>) {
    operator fun get(website: WebsiteOption): BaseApiService {
        return lists.find { it.website == website }
            ?: throw IllegalStateException("Unsupported website: $website")
    }
}