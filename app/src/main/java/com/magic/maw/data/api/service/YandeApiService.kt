package com.magic.maw.data.api.service

import co.touchlab.kermit.Logger
import com.magic.maw.data.api.entity.PoolResponse
import com.magic.maw.data.api.entity.PostResponse
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.entity.RequestMeta
import com.magic.maw.data.local.store.SettingsStore
import com.magic.maw.data.model.RequestOption
import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.Rating.Companion.join
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.site.PoolData
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.model.site.yande.YandeData
import com.magic.maw.data.model.site.yande.YandePool
import com.magic.maw.data.model.site.yande.YandeTag
import com.magic.maw.util.get
import com.magic.maw.util.toMonday
import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.path

class YandeApiService(private val client: HttpClient) : BaseApiService() {
    override val baseUrl: String = "https://yande.re"
    override val website: WebsiteOption = WebsiteOption.Yande
    override val supportedRatings: List<Rating> =
        listOf(Rating.Safe, Rating.Questionable, Rating.Explicit)

    override suspend fun getPostData(
        filter: RequestFilter,
        meta: RequestMeta
    ): PostResponse {
        // pool.post 和 popular 没有第二页
        if ((filter.poolId != null || filter.popularOption != null) && meta.next != null)
            return PostResponse(items = emptyList(), meta = RequestMeta(prev = "1", next = null))
        val url = getPostUrl(filter, meta)
        val list: ArrayList<PostData> = ArrayList()
        val ratings = SettingsStore.settings.yandeSettings.ratings
        val noMore: Boolean
        if (filter.poolId != null) {
            noMore = true
            client.get<YandePool>(url).posts?.let { posts ->
                for (item in posts) {
                    val data = item.toPostData() ?: continue
                    if (ratings.contains(data.rating)) {
                        list.add(data)
                    }
                }
            }
        } else {
            val yandeList: ArrayList<YandeData> = client.get(url)
            noMore = yandeList.isEmpty()
            for (item in yandeList) {
                val data = item.toPostData() ?: continue
                if (ratings.contains(data.rating)) {
                    list.add(data)
                }
            }
        }

        val currentPage = meta.page
        val newMeta = RequestMeta(
            prev = if (currentPage == 1) null else (currentPage - 1).toString(),
            next = if (noMore) null else (currentPage + 1).toString()
        )
        return PostResponse(items = list, meta = newMeta)
    }

    override suspend fun getPoolData(
        filter: RequestFilter,
        meta: RequestMeta
    ): PoolResponse {
        val url = getPoolUrl(filter, meta)
        val poolList: ArrayList<YandePool> = client.get(url)
        val list: ArrayList<PoolData> = ArrayList()
        for (item in poolList) {
            val data = item.toPoolData() ?: continue
            list.add(data)
        }
        val currentPage = meta.page
        val newMeta = RequestMeta(
            prev = if (currentPage == 1) null else (currentPage - 1).toString(),
            next = if (poolList.isEmpty()) null else (currentPage + 1).toString()
        )
        return PoolResponse(items = list, meta = newMeta)
    }

    override suspend fun getSuggestTagInfo(
        name: String,
        limit: Int
    ): List<TagInfo> {
        if (name.isEmpty())
            return emptyList()
        val tagMap = HashMap<String, TagInfo>()
        val tagList = ArrayList<TagInfo>()
        try {
            val url = getTagUrl(name, limit)
            val yandeList: ArrayList<YandeTag> = client.get(url)
            for (yandeTag in yandeList) {
                val tag = yandeTag.toTagInfo() ?: continue
                tagMap[tag.name] = tag
                tagList.add(tag)
            }
        } catch (_: Exception) {
        }
        return tagList
    }

    private fun getPostUrl(filter: RequestFilter, meta: RequestMeta): String {
        filter.poolId?.let { // 图册
            return "$baseUrl/pool/show.json?id=${it}"
        }
        val builder = URLBuilder(baseUrl)
        val tags = mutableSetOf<String>().apply { addAll(filter.tags) }
        filter.popularOption?.let { popularOption ->
            // 热门
            var date = popularOption.date
            when (popularOption.type) {
                PopularType.Day -> {
                    builder.path("post/popular_by_day.json")
                    builder.encodedParameters.apply {
                        append("day", date.dayOfMonth.toString())
                        append("month", date.monthValue.toString())
                        append("year", date.year.toString())
                    }
                }

                PopularType.Week -> {
                    builder.path("post/popular_by_week.json")
                    date = date.toMonday()
                    builder.encodedParameters.apply {
                        append("day", date.dayOfMonth.toString())
                        append("month", date.monthValue.toString())
                        append("year", date.year.toString())
                    }
                }

                PopularType.Month -> {
                    builder.path("post/popular_by_month.json")
                    builder.encodedParameters.apply {
                        append("month", date.monthValue.toString())
                        append("year", date.year.toString())
                    }
                }

                else -> {
                    builder.path("post.json")
                    tags.add("order:score")
                }
            }
        } ?: let {
            // 普通
            builder.path("post.json")
        }
        getRatingTag(filter.ratings).let { if (it.isNotEmpty()) tags.add(it) }
        val tagStr = tags.joinToString("+")
        builder.encodedParameters.append("page", meta.page.toString())
        builder.encodedParameters.append("limit", "40")
        builder.encodedParameters.append("tags", tagStr)
        return builder.build().toString().apply {
            Logger.d("PostDataLoader") { "url: $this" }
        }
    }

    private fun getPoolUrl(filter: RequestFilter, meta: RequestMeta): String {
        return "$baseUrl/pool.json?page=${meta.page}"
    }

    private fun getTagUrl(name: String, limit: Int): String {
        val limit = limit.coerceAtLeast(5)
        return "$baseUrl/tag.json?name=$name&page=1&limit=$limit&order=count"
    }

    private fun getRatingTag(ratings: List<Rating>): String {
        if (ratings.toSet() == supportedRatings.toSet())
            return ""
        val currentRating = ratings.join()
        return when (currentRating) {
            Rating.Safe.value -> "rating:s"
            Rating.Questionable.value -> "rating:q"
            Rating.Explicit.value -> "rating:e"
            Rating.Safe.value or Rating.Questionable.value -> "-rating:e"
            Rating.Safe.value or Rating.Explicit.value -> "-rating:q"
            Rating.Questionable.value or Rating.Explicit.value -> "-rating:s"
            else -> ""
        }
    }
}