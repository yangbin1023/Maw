package com.magic.maw.website.parser

import co.touchlab.kermit.Logger
import com.magic.maw.data.PoolData
import com.magic.maw.data.PopularType
import com.magic.maw.data.PostData
import com.magic.maw.data.Rating
import com.magic.maw.data.TagInfo
import com.magic.maw.data.UserInfo
import com.magic.maw.data.danbooru.DanbooruData
import com.magic.maw.data.danbooru.DanbooruPool
import com.magic.maw.data.danbooru.DanbooruTag
import com.magic.maw.data.danbooru.DanbooruUser
import com.magic.maw.util.TimeUtils
import com.magic.maw.util.client
import com.magic.maw.util.configFlow
import com.magic.maw.util.cookie
import com.magic.maw.util.hasFlag
import com.magic.maw.util.isJsonStr
import com.magic.maw.util.isTextFile
import com.magic.maw.util.isVerifyHtml
import com.magic.maw.util.json
import com.magic.maw.util.readString
import com.magic.maw.website.DLTask
import com.magic.maw.website.RequestOption
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.path
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

private const val TAG = DanbooruParser.SOURCE

class DanbooruParser : BaseParser(), VerifyContainer {
    override val baseUrl: String get() = "https://danbooru.donmai.us"
    override val source: String get() = SOURCE
    override val supportRating: Int get() = Rating.General.value or Rating.Sensitive.value or Rating.Questionable.value or Rating.Explicit.value
    override val supportPopular: Int get() = PopularType.defaultSupport or PopularType.Year.value
    private val verifying = atomic(false)
    private val verifySuccess = atomic(false)
    private val verifyChannel = Channel<VerifyResult>()

    override suspend fun requestPostData(option: RequestOption): List<PostData> {
        val url = getPostUrl(option)
        val (ret, msg) = securityRequest(url)
        if (!ret) {
            return emptyList()
        }
        val danbooruList: List<DanbooruData> = json.decodeFromString(msg)
        val list = ArrayList<PostData>()
        val ratings = configFlow.value.websiteConfig.rating
        for (item in danbooruList) {
            val data = item.toPostData() ?: continue
            if (!ratings.hasFlag(data.rating.value)) {
                continue
            }
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
            return requestPostData(option.apply { page++ })
        }
        return list
    }

    override suspend fun requestPoolData(option: RequestOption): List<PoolData> {
        val url = getPoolUrl(option)
        val (ret, msg) = securityRequest(url)
        if (!ret) {
            return emptyList()
        }
        val danbooruList: List<DanbooruPool> = json.decodeFromString(msg)
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
            val (ret, msg) = securityRequest(url)
            if (!ret) {
                return null
            }
            val danbooruList: List<DanbooruTag> = json.decodeFromString(msg)
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
            val (ret, msg) = securityRequest(url)
            if (!ret) {
                return emptyList()
            }
            val danbooruList: List<DanbooruTag> = json.decodeFromString(msg)
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
            val (ret, msg) = securityRequest(getUserUrl(userId))
            if (!ret) {
                return null
            }
            val userList: List<DanbooruUser> = json.decodeFromString(msg)
            if (userList.isNotEmpty()) {
                return userList[0].toUserInfo()
            }
        } catch (e: Exception) {
            Logger.e(TAG) { "request user info failed. id[$userId], error: $e" }
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
            )
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
        getRatingTag(option.ratings).let { if (it.isNotEmpty()) tags.add(it.encode()) }
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

    override fun getVerifyContainer(): VerifyContainer? {
        return if (onVerifyCallback == null) null else this
    }

    override suspend fun checkDlFile(file: File, task: DLTask): Boolean {
        if (file.isTextFile() && !task.baseData.fileType.isText) {
            if (file.readString().isVerifyHtml()) {
                if (!verifying.getAndSet(true)) {
                    Logger.d(TAG) { "konachan download failed. invoke onVerifyCallback, url: ${task.url}" }
                    onVerifyCallback?.invoke(task.url)
                }
                verifyChannel.receive()
            }
            return false
        }
        return true
    }

    override fun verifySuccess(url: String, text: String) {
        val tmpUrl = if (text.isJsonStr()) url else ""
        if (text.isVerifyHtml()) {
            Logger.w(TAG) { "verify result error. it's still verify html" }
            verifySuccess.value = false
        } else {
            verifySuccess.value = true
        }
        verifying.value = false
        verifyChannel.trySend(VerifyResult(result = verifySuccess.value, url = tmpUrl, text = text))
    }

    override fun cancelVerify() {
        verifySuccess.value = false
        verifying.value = false
        verifyChannel.trySend(VerifyResult(result = false))
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

    private suspend fun securityRequest(url: String): Pair<Boolean, String> {
        if (!verifySuccess.value) {
            val msg: String = client.get(url) {
                cookie()
                header(HttpHeaders.Referrer, baseUrl)
            }.body()
            if (!msg.isVerifyHtml()) {
                return Pair(true, msg)
            }
            if (!verifying.getAndSet(true)) {
                Logger.d(TAG) { "invoke onVerifyCallback, url: $url. msg: $msg" }
                onVerifyCallback?.invoke(url)
            }
            Logger.d(TAG) { "waiting for verify, url: $url" }
            val result = verifyChannel.receive()
            Logger.d(TAG) { "receive for verify, $result" }
            if (!result.result) {
                return Pair(false, "")
            }
            if (result.url == url) {
                return Pair(true, result.text)
            }
            delay(50 + (0L..150L).random())
            return securityRequest(url)
        } else {
            val msg: String = client.get(url) { cookie() }.body()
            if (msg.isVerifyHtml()) {
                this.verifySuccess.value = false
                return securityRequest(url)
            } else {
                return Pair(true, msg)
            }
        }
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