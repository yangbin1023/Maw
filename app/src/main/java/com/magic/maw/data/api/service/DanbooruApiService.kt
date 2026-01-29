package com.magic.maw.data.api.service

import co.touchlab.kermit.Logger
import com.magic.maw.data.api.entity.PoolResponse
import com.magic.maw.data.api.entity.PostResponse
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.entity.RequestMeta
import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.entity.UserInfo
import com.magic.maw.data.model.site.danbooru.DanbooruData
import com.magic.maw.data.model.site.danbooru.DanbooruPool
import com.magic.maw.data.model.site.danbooru.DanbooruTag
import com.magic.maw.data.model.site.danbooru.DanbooruUser
import com.magic.maw.util.get
import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.path
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException
import java.time.format.DateTimeFormatter

private const val TAG = "DanbooruApiService"

class DanbooruApiService(
    private val client: HttpClient
) : BaseApiService() {
    override val website: WebsiteOption = WebsiteOption.Danbooru
    override val baseUrl: String = "https://danbooru.donmai.us"
    override val supportedRatings: List<Rating> =
        listOf(Rating.General, Rating.Sensitive, Rating.Questionable, Rating.Explicit)
    override val supportedPopularDateTypes: List<PopularType> = listOf(
        PopularType.Day, PopularType.Week, PopularType.Month,
        PopularType.Year, PopularType.All
    )

    override suspend fun getPostData(filter: RequestFilter, meta: RequestMeta): PostResponse {
        val url = getPostUrl(filter, meta)
        val danbooruList: List<DanbooruData> = client.get(url)
        val list = danbooruList.mapNotNull { it.toPostData() }
        val nextMeta = getNextMeta(meta, danbooruList.isEmpty())
        return PostResponse(items = list, meta = nextMeta)
    }

    override suspend fun getPoolData(filter: RequestFilter, meta: RequestMeta): PoolResponse {
        val url = getPoolUrl(meta)
        val danbooruList: List<DanbooruPool> = client.get(url)
        val list = danbooruList.mapNotNull { it.toPoolData() }
        val nextMeta = getNextMeta(meta, danbooruList.isEmpty())
        return PoolResponse(items = list, meta = nextMeta)
    }

    override suspend fun getSuggestTagInfo(name: String, limit: Int): List<TagInfo> {
        if (name.isEmpty())
            return emptyList()
        try {
            val url = getTagUrl(name, 1, limit)
            return client.get<List<DanbooruTag>>(url).mapNotNull { it.toTagInfo() }
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            Logger.e(TAG) { "request suggest tag info failed. name[$name], error: $e" }
        }
        return emptyList()
    }

    override suspend fun getTagByName(tagName: String): List<TagInfo> {
        if (tagName.isEmpty())
            return emptyList()
        try {
            val url = getTagUrl(tagName, 1, 1)
            return client.get<List<DanbooruTag>>(url)
                .mapNotNull { it.toTagInfo() }
                .distinctBy { it.name }
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            Logger.e(TAG) { "request tag info failed. name[$tagName], error: $e" }
        }
        return emptyList()
    }

    override suspend fun getUserInfo(userId: String): UserInfo? {
        return try {
            client.get<List<DanbooruUser>>(getUserUrl(userId)).firstOrNull()?.toUserInfo()
        } catch (e: Exception) {
            Logger.e(TAG) { "request user info failed. id[$userId], error: $e" }
            null
        }
    }

    private fun getPostUrl(filter: RequestFilter, meta: RequestMeta): String {
        val builder = URLBuilder().takeFrom(baseUrl)
        val tags = ArrayList<String>()
        filter.popularOption?.let {
            val dateStr = it.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val scale = when (it.type) {
                PopularType.Day -> "day"
                PopularType.Week -> "week"
                PopularType.Month -> "month"
                PopularType.Year -> "year"
                else -> ""
            }
            if (scale.isEmpty()) {
                builder.path("posts.json")
                tags.add("order:rank")
            } else {
                builder.path("explore/posts/popular.json")
                builder.encodedParameters.apply {
                    append("date", dateStr)
                    append("scale", scale)
                }
            }
        } ?: let {
            builder.path("posts.json")
        }
        for (item in filter.tags) {
            val tag = if (item.startsWith("-")) item.substring(1).decode() else item.decode()
            if (tag.startsWith("rating:") or tag.startsWith("pool:"))
                continue
            tags.add(item.encode())
        }
        getRatingTag(filter.ratings).let { if (it.isNotEmpty()) tags.add(it.encode()) }
        filter.poolId?.let {
            tags.add("pool:${it}".encode())
        }
        builder.encodedParameters.apply {
            append("page", meta.page.toString())
            append("limit", "40")
            append("tags", tags.joinToString("+"))
        }
        return builder.build().toString()
            .apply { Logger.d(TAG) { "post url: $this, tags: $tags, option: ${filter.tags}" } }
    }

    private fun getPoolUrl(meta: RequestMeta): String {
        return "$baseUrl/pools.json?page=${meta.page}&${"search[order]=created_at".encode()}"
    }

    private fun getTagUrl(name: String, page: Int, limit: Int = 1): String {
        val searchTag = if (limit <= 1) name.encode() else "*${name.encode()}*"
        val paramMap = mapOf(
            "search[order]" to "count",
            "search[name_or_alias_matches]" to searchTag,
            "limit" to if (limit < 5) "5" else "$limit",
            "page" to page,
        )
        val builder = StringBuilder()
        for ((key, value) in paramMap) {
            builder.append("${key.encode()}=$value").append("&")
        }
        return "$baseUrl/tags.json?$builder"
    }

    private fun getUserUrl(userId: String): String {
        return "$baseUrl/users.json?${"search[id]=$userId".encode()}"
    }

    private fun getRatingTag(ratings: List<Rating>): String {
        if (ratings.toSet() == supportedRatings.toSet()) {
            return ""
        }
        val ratingList: MutableList<String> = ArrayList<String>()
        if (ratings.contains(Rating.General))
            ratingList.add("g")
        if (ratings.contains(Rating.Sensitive))
            ratingList.add("s")
        if (ratings.contains(Rating.Questionable))
            ratingList.add("q")
        if (ratings.contains(Rating.Explicit))
            ratingList.add("e")
        if (ratingList.isEmpty())
            ratingList.add("g")
        return "rating:" + ratingList.joinToString(",")
    }
}