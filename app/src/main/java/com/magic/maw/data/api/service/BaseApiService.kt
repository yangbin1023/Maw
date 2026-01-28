package com.magic.maw.data.api.service

import com.magic.maw.data.api.entity.PoolResponse
import com.magic.maw.data.api.entity.PostResponse
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.entity.RequestMeta
import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.entity.UserInfo
import java.net.URLDecoder
import java.net.URLEncoder

abstract class BaseApiService {
    protected abstract val baseUrl: String
    abstract val website: WebsiteOption
    open val supportedRatings: List<Rating> = Rating.defaultSupportedRatings
    open val supportedPopularDateTypes: List<PopularType> = PopularType.defaultSupportedDateTypes

    abstract suspend fun getPostData(filter: RequestFilter, meta: RequestMeta): PostResponse
    abstract suspend fun getPoolData(filter: RequestFilter, meta: RequestMeta): PoolResponse
    abstract suspend fun getSuggestTagInfo(name: String, limit: Int = 10): List<TagInfo>

    open suspend fun getTagsByPostId(postId: String): List<TagInfo>? = null
    open suspend fun getTagByName(tagName: String): TagInfo? = null
    open suspend fun getUserInfo(userId: String): UserInfo? = null
    open suspend fun getThumbnailUrl(postId: String): String? = null
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

    companion object {
        fun String.encode(enc: String = "UTF-8"): String {
            return URLEncoder.encode(this, enc)
        }

        fun String.decode(enc: String = "UTF-8"): String {
            return URLDecoder.decode(this, enc)
        }
    }
}

class ApiServiceProvider(private val lists: List<BaseApiService>) {
    operator fun get(website: WebsiteOption): BaseApiService {
        return lists.find { it.website == website }
            ?: throw IllegalStateException("Unsupported website: $website")
    }
}