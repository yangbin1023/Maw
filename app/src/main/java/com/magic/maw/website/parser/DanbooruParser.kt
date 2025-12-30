package com.magic.maw.website.parser

import co.touchlab.kermit.Logger
import com.magic.maw.data.PoolData
import com.magic.maw.data.PopularType
import com.magic.maw.data.PostData
import com.magic.maw.data.Rating
import com.magic.maw.data.TagInfo
import com.magic.maw.data.UserInfo
import com.magic.maw.data.WebsiteOption
import com.magic.maw.data.danbooru.DanbooruData
import com.magic.maw.data.danbooru.DanbooruPool
import com.magic.maw.data.danbooru.DanbooruTag
import com.magic.maw.data.danbooru.DanbooruUser
import com.magic.maw.util.TimeUtils
import com.magic.maw.util.client
import com.magic.maw.util.get
import com.magic.maw.util.json
import com.magic.maw.website.RequestOption
import io.ktor.http.URLBuilder
import io.ktor.http.path
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

private const val TAG = "danbooru"

object DanbooruParser : BaseParser() {
    const val SOURCE = "danbooru"
    override val baseUrl: String get() = "https://danbooru.donmai.us"
    override val website: WebsiteOption = WebsiteOption.Danbooru
    override val source: String get() = SOURCE
    override val supportRating: Int get() = Rating.General.value or Rating.Sensitive.value or Rating.Questionable.value or Rating.Explicit.value
    override val supportedRatings: List<Rating> =
        listOf(Rating.General, Rating.Sensitive, Rating.Questionable, Rating.Explicit)
    override val supportPopular: Int get() = PopularType.defaultSupport or PopularType.Year.value
    override val supportedPopularDateTypes: List<PopularType> = listOf(
        PopularType.Day, PopularType.Week, PopularType.Month,
        PopularType.Year, PopularType.All
    )

    override suspend fun requestPostData(option: RequestOption): List<PostData> {
        val url = getPostUrl(option)
        val resultStr:String = client.get(url)
        val danbooruList: List<DanbooruData> = try {
            json.decodeFromString(resultStr)
        } catch (e: Exception) {
            Logger.e(TAG) { "request failed, url: $url, resultStr: \n$resultStr" }
            throw e
        }
        val list = ArrayList<PostData>()
        for (item in danbooruList) {
            val data = item.toPostData() ?: continue
            data.createId?.let { createId ->
                userManager.get(createId)?.let {
                    data.uploader = it.name
                }
            }
            for ((index, tag) in data.tags.withIndex()) {
                tagManager.get(tag.name)?.let {
                    data.tags[index] = it
                }
            }
            data.tags.sort()
            list.add(data)
        }
        if (list.isEmpty() && danbooruList.isNotEmpty()) {
            Logger.e(TAG) { "Warning: list is empty, but danbooruList is not empty!" }
            return requestPostData(option.apply { page++ })
        }
        return list
    }

    override suspend fun requestPoolData(option: RequestOption): List<PoolData> {
        val url = getPoolUrl(option)
        val danbooruList: List<DanbooruPool> = client.get(url)
        val list: ArrayList<PoolData> = ArrayList()
        for (item in danbooruList) {
            val data = item.toPoolData() ?: continue
            list.add(data)
        }
        return list
    }

    override suspend fun requestTagInfo(name: String): TagInfo? {
        if (name.isEmpty())
            return null
        val tagMap = HashMap<String, TagInfo>()
        var targetInfo: TagInfo? = null
        try {
            val url = getTagUrl(name, firstPageIndex, 1)
            val danbooruList: List<DanbooruTag> = client.get(url)
            for (item in danbooruList) {
                val tag = item.toTagInfo() ?: continue
                if (tag.name == name) {
                    targetInfo = tag
                }
                tagMap[tag.name] = tag
            }
            if (tagMap.isNotEmpty()) {
                tagManager.addAll(tagMap)
            }
        } catch (e: Exception) {
            Logger.e(TAG) { "request tag info failed. name[$name], error: $e" }
        }
        return targetInfo
    }

    override suspend fun requestSuggestTagInfo(name: String, limit: Int): List<TagInfo> {
        if (name.isEmpty())
            return emptyList()
        val tagMap = HashMap<String, TagInfo>()
        val tagList = ArrayList<TagInfo>()
        try {
            val url = getTagUrl(name, firstPageIndex, limit)
            val danbooruList: List<DanbooruTag> = client.get(url)
            for (item in danbooruList) {
                val tag = item.toTagInfo() ?: continue
                tagMap[tag.name] = tag
                tagList.add(tag)
            }
        } catch (e: Exception) {
            Logger.e(TAG) { "request suggest tag info failed. name[$name], error: $e" }
        }
        tagManager.addAll(tagMap)
        return tagList
    }

    override suspend fun requestUserInfo(userId: Int): UserInfo? {
        try {
            val userList: List<DanbooruUser> = client.get(getUserUrl(userId))
            if (userList.isNotEmpty()) {
                return userList[0].toUserInfo()
            }
        } catch (e: Exception) {
            Logger.e(TAG) { "request user info failed. id[$userId], error: $e" }
        }
        return null
    }

    override fun parseSearchText(text: String): Set<String> {
        if (text.isEmpty())
            return emptySet()
        val tagTexts = text.decode().split(" ")
        val tagList = mutableSetOf<String>()
        for (tagText in tagTexts) {
            val tmp = if (tagText.startsWith("-")) tagText.substring(1) else tagText
            if (tmp.isEmpty() || tmp.startsWith("tag:"))
                continue
            tagList.add(tagText)
        }
        return tagList
    }

    override fun getPostUrl(option: RequestOption): String {
        val builder = URLBuilder(baseUrl)
        option.popularOption?.let {
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
                option.tags.add("order:rank")
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
        val tags = ArrayList<String>()
        for (item in option.tags) {
            val tag = if (item.startsWith("-")) item.substring(1).decode() else item.decode()
            if (tag.startsWith("rating:") or tag.startsWith("pool:"))
                continue
            tags.add(item.encode())
        }
        getRatingTag(option.ratingSet).let { if (it.isNotEmpty()) tags.add(it.encode()) }
        if (option.poolId >= 0) {
            tags.add("pool:${option.poolId}".encode())
        }
        builder.encodedParameters.apply {
            append("page", option.page.toString())
            append("limit", "40")
            append("tags", tags.joinToString("+"))
        }
        return builder.build().toString().apply { Logger.d(TAG) { "post url: $this" } }
    }

    override fun getPoolUrl(option: RequestOption): String {
        return "$baseUrl/pools.json?page=${option.page}&${"search[order]=created_at".encode()}"
    }

    override fun getTagUrl(name: String, page: Int, limit: Int): String {
        val searchTag = if (limit <= 1) name.encode() else "*${name.encode()}*"
        val paramMap = mapOf(
            "search[order]" to "count",
            "search[name_or_alias_matches]" to searchTag,
            "limit" to if (limit < 5) "5" else "$limit",
            "page" to "$page",
        )
        val builder = StringBuilder()
        for ((key, value) in paramMap) {
            builder.append("${key.encode()}=$value").append("&")
        }
        return "$baseUrl/tags.json?$builder"
    }

    override fun getUserUrl(userId: Int): String {
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

    private val format by lazy { SimpleDateFormat(TimeUtils.FORMAT_4, Locale.getDefault()) }

    fun getUnixTime(timeStr: String?): Long? {
        timeStr ?: return null
        try {
            val zoneStr = timeStr.substring(23)
            format.timeZone = TimeZone.getTimeZone("GMT$zoneStr")
            return format.parse(timeStr.substring(0, 23))?.time
        } catch (_: Exception) {
        }
        return null
    }
}