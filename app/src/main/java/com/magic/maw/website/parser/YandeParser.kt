package com.magic.maw.website.parser

import co.touchlab.kermit.Logger
import com.magic.maw.data.PoolData
import com.magic.maw.data.PopularType
import com.magic.maw.data.PostData
import com.magic.maw.data.Rating
import com.magic.maw.data.Rating.Companion.join
import com.magic.maw.data.SettingsService
import com.magic.maw.data.TagInfo
import com.magic.maw.data.UserInfo
import com.magic.maw.data.WebsiteOption
import com.magic.maw.data.yande.YandeData
import com.magic.maw.data.yande.YandePool
import com.magic.maw.data.yande.YandeTag
import com.magic.maw.data.yande.YandeUser
import com.magic.maw.util.client
import com.magic.maw.util.get
import com.magic.maw.util.toMonday
import com.magic.maw.website.LoadStatus
import com.magic.maw.website.RequestOption
import io.ktor.http.URLBuilder
import io.ktor.http.path

object YandeParser : BaseParser() {
    const val SOURCE = "yande"
    override val baseUrl: String get() = "https://yande.re"
    override val website: WebsiteOption = WebsiteOption.Yande
    override val source: String get() = SOURCE
    override val supportRating: Int get() = Rating.Safe.value or Rating.Questionable.value or Rating.Explicit.value
    override val supportedRatings: List<Rating> =
        listOf(Rating.Safe, Rating.Questionable, Rating.Explicit)

    override suspend fun requestPostData(option: RequestOption): List<PostData> {
        // pool.post 和 popular 没有第二页
        if ((option.poolId != null || option.popularOption != null) && option.page > firstPageIndex)
            return emptyList()
        val url = getPostUrl(option)
        val list: ArrayList<PostData> = ArrayList()
        val ratings = SettingsService.settings.websiteSettings.ratings
        if (option.poolId != null) {
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
            for (item in yandeList) {
                val data = item.toPostData() ?: continue
                if (ratings.contains(data.rating)) {
                    list.add(data)
                }
            }
        }
        for (data in list) {
            for ((index, tag) in data.tags.withIndex()) {
                (tagManager.getInfoStatus(tag.name).value as? LoadStatus.Success)?.let {
                    data.tags[index] = it.result
                }
            }
            data.tags.sort()
        }
        return list
    }

    override suspend fun requestPoolData(option: RequestOption): List<PoolData> {
        val poolList: ArrayList<YandePool> = client.get(getPoolUrl(option))
        val list: ArrayList<PoolData> = ArrayList()
        for (item in poolList) {
            val data = item.toPoolData() ?: continue
            data.createUid?.let { userId ->
                userManager.get(userId)?.let {
                    data.uploader = it.name
                } ?: let {
                    requestUserInfo(userId)?.let {
                        data.uploader = it.name
                        userManager.add(it)
                    }
                }
            }
            list.add(data)
        }
        return list
    }

    override suspend fun requestTagInfo(name: String): TagInfo? {
        if (name.isEmpty())
            return null
        var page = firstPageIndex
        val tagMap = HashMap<String, TagInfo>()
        var targetInfo: TagInfo? = null
        var retryCount = 0
        val limit = 20
        do {
            try {
                val url = getTagUrl(name, page, limit)
                val yandeList: List<YandeTag> = client.get(url)
                var found = false
                for (yandeTag in yandeList) {
                    val tag = yandeTag.toTagInfo() ?: continue
                    if (tag.name == name) {
                        found = true
                        targetInfo = tag
                    }
                    tagMap[tag.name] = tag
                }
                retryCount = 0
                if (yandeList.isEmpty() || found)
                    break
            } catch (_: Exception) {
                retryCount++
                if (retryCount >= 3) break else continue
            }
            page++
        } while (true)
        tagManager.addAll(tagMap)
        return targetInfo
    }

    override suspend fun requestSuggestTagInfo(name: String, limit: Int): List<TagInfo> {
        if (name.isEmpty())
            return emptyList()
        val tagMap = HashMap<String, TagInfo>()
        val tagList = ArrayList<TagInfo>()
        try {
            val url = getTagUrl(name, firstPageIndex, limit)
            val yandeList: ArrayList<YandeTag> = client.get(url)
            for (yandeTag in yandeList) {
                val tag = yandeTag.toTagInfo() ?: continue
                tagMap[tag.name] = tag
                tagList.add(tag)
            }
        } catch (_: Exception) {
        }
        tagManager.addAll(tagMap)
        return tagList
    }

    override suspend fun requestUserInfo(userId: Int): UserInfo? {
        try {
            val userList: List<YandeUser> = client.get(getUserUrl(userId))
            if (userList.isNotEmpty()) {
                return userList[0].toUserInfo()
            }
        } catch (_: Exception) {
        }
        return null
    }

    override fun parseSearchText(text: String): Set<String> {
        if (text.isEmpty())
            return emptySet()
        val tagTexts = text.decode().split(" ")
        val tagList = mutableSetOf<String>()
        for (tagText in tagTexts) {
            if (tagText.isEmpty())
                continue
            if (tagText.startsWith("tag:") || tagText.startsWith("-tag:"))
                continue
            tagList.add(tagText)
        }
        return tagList
    }

    override fun getPostUrl(option: RequestOption): String {
        option.poolId?.let {
            // 图册
            return "$baseUrl/pool/show.json?id=${it}"
        }
        val builder = URLBuilder(baseUrl)
        val tags = mutableSetOf<String>().apply { addAll(option.tags) }
        option.popularOption?.let { popularOption ->
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
        getRatingTag(option.ratings).let { if (it.isNotEmpty()) tags.add(it) }
        val tagStr = tags.joinToString("+")
        builder.encodedParameters.append("page", option.page.toString())
        builder.encodedParameters.append("limit", "40")
        builder.encodedParameters.append("tags", tagStr)
        return builder.build().toString().apply {
            Logger.d("PostDataLoader") { "url: $this" }
        }
    }

    override fun getPoolUrl(option: RequestOption): String {
        return "$baseUrl/pool.json?page=${option.page}"
    }

    override fun getTagUrl(name: String, page: Int, limit: Int): String {
        val limit2 = if (limit < 5) 5 else limit
        return "$baseUrl/tag.json?name=$name&page=$page&limit=$limit2&order=count"
    }

    override fun getUserUrl(userId: Int): String {
        return "$baseUrl/user.json?id=$userId"
    }

    private fun getRatingTag(ratings: List<Rating>): String {
        if (ratings.toSet() == supportedRatings.toSet())
            return ""
        val currentRating = ratings.join()
        println("ratings: $ratings, support rating: $supportRating, current rating: $currentRating")
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
