package com.magic.maw.website.parser

import com.magic.maw.data.PoolData
import com.magic.maw.data.PostData
import com.magic.maw.data.Rating
import com.magic.maw.data.TagInfo
import com.magic.maw.data.UserInfo
import com.magic.maw.data.danbooru.DanbooruData
import com.magic.maw.data.danbooru.DanbooruPool
import com.magic.maw.data.danbooru.DanbooruTag
import com.magic.maw.data.danbooru.DanbooruUser
import com.magic.maw.util.Logger
import com.magic.maw.util.TimeUtils
import com.magic.maw.util.client
import com.magic.maw.util.hasFlag
import com.magic.maw.website.RequestOption
import io.ktor.client.call.body
import io.ktor.client.request.get
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val logger = Logger(DanbooruParser.SOURCE)

class DanbooruParser : BaseParser() {
    override val baseUrl: String get() = "https://danbooru.donmai.us"
    override val source: String get() = SOURCE
    override val supportRating: Int get() = Rating.General.value or Rating.Sensitive.value or Rating.Questionable.value or Rating.Explicit.value

    override suspend fun requestPostData(option: RequestOption): List<PostData> {
        val danbooruList: List<DanbooruData> = client.get(getPostUrl(option)).body()
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
        return list
    }

    override suspend fun requestPoolData(option: RequestOption): List<PoolData> {
        val danbooruList: List<DanbooruPool> = client.get(getPoolUrl(option)).body()
        val list: ArrayList<PoolData> = ArrayList()
        for (item in danbooruList) {
            val data = item.toPoolData() ?: continue
            list.add(data)
        }
        return list
    }

    override suspend fun requestPoolPostData(option: RequestOption): List<PostData> {
        val danbooruList: List<DanbooruData> = client.get(getPoolPostUrl(option)).body()
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
        return list
    }

    override suspend fun requestTagInfo(name: String): TagInfo? {
        if (name.isEmpty())
            return null
        val tagMap = HashMap<String, TagInfo>()
        var targetInfo: TagInfo? = null
        try {
            val url = getTagUrl(name, firstPageIndex, 1)
            val danbooruList: List<DanbooruTag> = client.get(url).body()
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
            logger.severe("request tag info failed. name[$name], error: $e")
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
            val danbooruList: List<DanbooruTag> = client.get(url).body()
            for (item in danbooruList) {
                val tag = item.toTagInfo() ?: continue
                tagMap[tag.name] = tag
                tagList.add(tag)
            }
        } catch (e: Exception) {
            logger.severe("request suggest tag info failed. name[$name], error: $e")
        }
        tagManager.addAll(tagMap)
        return tagList
    }

    override suspend fun requestUserInfo(userId: Int): UserInfo? {
        try {
            val userList: List<DanbooruUser> = client.get(getUserUrl(userId)).body()
            if (userList.isNotEmpty()) {
                return userList[0].toUserInfo()
            }
        } catch (e: Exception) {
            logger.severe("request user info failed. id[$userId], error: $e")
        }
        return null
    }

    override fun RequestOption.parseSearchText(text: String): List<String> {
        if (text.isEmpty())
            return emptyList()
        val tagTexts = text.decode().split(" ")
        val tagList = ArrayList<String>()
        for (tagText in tagTexts) {
            val tmp = if (tagText.startsWith("-")) tagText.substring(1) else tagText
            if (tmp.isEmpty()
                || tmp.startsWith("tag:")
                || tmp.startsWith("filetype:")
            )
                continue
            tagList.add(tagText)
        }
        return tagList
    }

    override fun getPostUrl(option: RequestOption): String {
        val tags = ArrayList<String>()
        for (item in option.tags) {
            val tag = if (item.startsWith("-")) item.substring(1).decode() else item.decode()
            if (tag.startsWith("rating:"))
                continue
            tags.add(item.encode())
        }
        getRatingTag(option.ratings).let { if (it.isNotEmpty()) tags.add(it.encode()) }
        tags.add("filetype:png,jpg,gif".encode())
        return "$baseUrl/posts.json?page=${option.page}&limit=40&tags=${tags.joinToString("+")}"
    }

    override fun getPoolUrl(option: RequestOption): String {
        return "$baseUrl/pools.json?page=${option.page}&${"search[order]=created_at".encode()}"
    }

    override fun getPoolPostUrl(option: RequestOption): String {
        val tags = ArrayList<String>()
        for (item in option.tags) {
            val tag = if (item.startsWith("-")) item.substring(1).decode() else item.decode()
            if (tag.startsWith("rating:") or tag.startsWith("pool:"))
                continue
            tags.add(item.encode())
        }
        getRatingTag(option.ratings).let { if (it.isNotEmpty()) tags.add(it.encode()) }
        tags.add("filetype:png,jpg,gif".encode())
        tags.add("pool:${option.poolId}".encode())
        return "$baseUrl/posts.json?page=${option.page}&limit=40&tags=${tags.joinToString("+")}"
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

    private fun getRatingTag(ratings: Int): String {
        val tempRating = ratings and supportRating
        if (tempRating != supportRating) {
            val ratingList: MutableList<String> = ArrayList<String>()
            if (tempRating.hasFlag(Rating.General.value))
                ratingList.add("g")
            if (tempRating.hasFlag(Rating.Sensitive.value))
                ratingList.add("s")
            if (tempRating.hasFlag(Rating.Questionable.value))
                ratingList.add("q")
            if (tempRating.hasFlag(Rating.Explicit.value))
                ratingList.add("e")
            if (ratingList.isEmpty())
                ratingList.add("g")
            return "rating:" + ratingList.joinToString(",")
        }
        return ""
    }

    companion object {
        private val format by lazy { SimpleDateFormat(TimeUtils.FORMAT_4, Locale.getDefault()) }
        const val SOURCE = "danbooru"

        @JvmStatic
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
}