package com.magic.maw.data.api.service

import co.touchlab.kermit.Logger
import com.hjq.toast.Toaster
import com.magic.maw.MyApp
import com.magic.maw.data.api.entity.PoolResponse
import com.magic.maw.data.api.entity.PostResponse
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.entity.RequestMeta
import com.magic.maw.data.api.parser.BaseParser.Companion.decode
import com.magic.maw.data.api.parser.BaseParser.Companion.encode
import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.site.PoolData
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.model.site.danbooru.DanbooruData
import com.magic.maw.data.model.site.danbooru.DanbooruPool
import com.magic.maw.data.model.site.danbooru.DanbooruTag
import com.magic.maw.util.TimeUtils
import com.magic.maw.util.get
import com.magic.maw.util.isHtml
import com.magic.maw.util.json
import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.path
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
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

    override suspend fun getPostData(
        filter: RequestFilter,
        meta: RequestMeta
    ): PostResponse {
        val url = getPostUrl(filter, meta)
        val resultStr: String = client.get(url)
        val danbooruList: List<DanbooruData> = try {
            json.decodeFromString(resultStr)
        } catch (e: Exception) {
            Logger.e(TAG) { "request failed, url: $url, resultStr: \n$resultStr" }
            if (resultStr.isHtml()) {
                try {
                    val logDir = MyApp.app.getExternalFilesDir("log")
                    val fileName = TimeUtils.getCurrentTimeStr(TimeUtils.FORMAT_3) + ".log"
                    withContext(Dispatchers.IO) {
                        val fWriter = FileWriter(File(logDir, fileName))
                        fWriter.write(resultStr)
                        fWriter.close()
                    }
                    Toaster.show("写入日志：$fileName")
                } catch (e: Exception) {
                    Logger.e(TAG) { """write log failed: ${e.message}""" }
                }
            }
            throw e
        }
        val list = ArrayList<PostData>()
        for (item in danbooruList) {
            val data = item.toPostData() ?: continue
            data.tags.sort()
            list.add(data)
        }
        val nextMeta = getNextMeta(meta, danbooruList.isEmpty())
        if (list.isEmpty() && danbooruList.isNotEmpty()) {
            Logger.e(TAG) { "Warning: list is empty, but danbooruList is not empty!" }
            return getPostData(filter, nextMeta)
        }
        return PostResponse(items = list, meta = nextMeta)
    }

    override suspend fun getPoolData(
        filter: RequestFilter,
        meta: RequestMeta
    ): PoolResponse {
        val url = getPoolUrl(filter, meta)
        val danbooruList: List<DanbooruPool> = client.get(url)
        val list: ArrayList<PoolData> = ArrayList()
        for (item in danbooruList) {
            val data = item.toPoolData() ?: continue
            list.add(data)
        }
        val nextMeta = getNextMeta(meta, danbooruList.isEmpty())
        return PoolResponse(items = list, meta = nextMeta)
    }

    override suspend fun getSuggestTagInfo(name: String, limit: Int): List<TagInfo> {
        if (name.isEmpty())
            return emptyList()
        val tagMap = HashMap<String, TagInfo>()
        val tagList = ArrayList<TagInfo>()
        try {
            val url = getTagUrl(name, limit)
            val danbooruList: List<DanbooruTag> = client.get(url)
            for (item in danbooruList) {
                val tag = item.toTagInfo() ?: continue
                tagMap[tag.name] = tag
                tagList.add(tag)
            }
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            Logger.e(TAG) { "request suggest tag info failed. name[$name], error: $e" }
        }
        return tagList
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

    private fun getPoolUrl(filter: RequestFilter, meta: RequestMeta): String {
        return "$baseUrl/pools.json?page=${meta.page}&${"search[order]=created_at".encode()}"
    }

    private fun getTagUrl(name: String, limit: Int): String {
        val searchTag = if (limit <= 1) name.encode() else "*${name.encode()}*"
        val paramMap = mapOf(
            "search[order]" to "count",
            "search[name_or_alias_matches]" to searchTag,
            "limit" to if (limit < 5) "5" else "$limit",
            "page" to "1",
        )
        val builder = StringBuilder()
        for ((key, value) in paramMap) {
            builder.append("${key.encode()}=$value").append("&")
        }
        return "$baseUrl/tags.json?$builder"
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

    private fun getNextMeta(meta: RequestMeta, noMore: Boolean = false): RequestMeta {
        val currentPage = meta.page
        return RequestMeta(
            prev = if (currentPage == 1) null else (currentPage - 1).toString(),
            next = if (noMore) null else (currentPage + 1).toString()
        )
    }
}