package com.magic.maw.data.api.service

import co.touchlab.kermit.Logger
import com.magic.maw.data.api.entity.PostResponse
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.entity.RequestMeta
import com.magic.maw.data.local.store.SettingsStore
import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.Rating.Companion.join
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.model.site.konachan.KonachanData
import com.magic.maw.data.model.site.konachan.KonachanPool
import com.magic.maw.util.get
import com.magic.maw.util.toMonday
import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.path

class KonachanApiService(private val client: HttpClient) : BaseApiService() {
    override val baseUrl: String = "https://konachan.net"
    override val website: WebsiteOption = WebsiteOption.Konachan
    override val supportedRatings: List<Rating> =
        listOf(Rating.Safe, Rating.Questionable, Rating.Explicit)

    override suspend fun getPostData(
        filter: RequestFilter,
        meta: RequestMeta
    ): PostResponse {
        // pool.post 和 popular 没有第二页
        if ((filter.poolId != null || filter.popularOption != null) && meta.next != null)
            return PostResponse(items = emptyList(), meta = RequestMeta(prev = "1"))
        val url = getPostUrl(filter, meta)
        val list = ArrayList<PostData>()
        val ratings = SettingsStore.settings.websiteSettings.ratings
        val noMore: Boolean
        if (filter.poolId != null) {
            noMore = true
            val konachanPool: KonachanPool = client.get(url)
            konachanPool.posts?.let { posts ->
                for (item in posts) {
                    val data = item.toPostData() ?: continue
                    if (ratings.contains(data.rating)) {
                        list.add(data)
                    }
                }
            }
        } else {
            val konachanList: List<KonachanData> = client.get(url)
            noMore = konachanList.isEmpty()
            for (item in konachanList) {
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