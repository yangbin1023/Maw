package com.magic.maw.website.parser

import com.magic.maw.data.PoolData
import com.magic.maw.data.PostData
import com.magic.maw.data.Rating
import com.magic.maw.data.TagInfo
import com.magic.maw.data.UserInfo
import com.magic.maw.data.yande.YandeData
import com.magic.maw.data.yande.YandePool
import com.magic.maw.data.yande.YandeTag
import com.magic.maw.data.yande.YandeUser
import com.magic.maw.util.Logger
import com.magic.maw.util.client
import com.magic.maw.website.LoadStatus
import com.magic.maw.website.RequestOption
import com.magic.maw.website.TagManager
import com.magic.maw.website.UserManager
import io.ktor.client.call.body
import io.ktor.client.request.get

private val logger = Logger("YandeParser")

class YandeParser : BaseParser() {
    override val baseUrl: String get() = "https://yande.re"
    override val source: String get() = SOURCE
    override val supportRating: Int get() = Rating.Safe.value or Rating.Questionable.value or Rating.Explicit.value
    override val tagManager: TagManager by lazy { TagManager.get(source) }
    override val userManager: UserManager by lazy { UserManager.get(source) }

    override suspend fun requestPostData(option: RequestOption): List<PostData> {
        val yandeList: ArrayList<YandeData> = client.get(getPostUrl(option)).body()
        val list: ArrayList<PostData> = ArrayList()
        for (item in yandeList) {
            val data = item.toPostData() ?: continue
            for ((index, tag) in data.tags.withIndex()) {
                (tagManager.getInfoStatus(tag.name).value as? LoadStatus.Success)?.let {
                    data.tags[index] = it.result
                }
            }
            data.tags.sort()
            list.add(data)
        }
        return list
    }

    override suspend fun requestPoolData(option: RequestOption): List<PoolData> {
        val poolList: ArrayList<YandePool> = client.get(getPoolUrl(option)).body()
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

    override suspend fun requestPoolPostData(option: RequestOption): List<PostData> {
        if (option.page > firstPageIndex || option.poolId < 0)
            return emptyList()
        logger.info("start request pool post. id: ${option.poolId}")
        val yandePool: YandePool = client.get(getPoolPostUrl(option)).body()
        val list = ArrayList<PostData>()
        yandePool.posts?.let { posts ->
            for (item in posts) {
                val data = item.toPostData() ?: continue
                for ((index, tag) in data.tags.withIndex()) {
                    (tagManager.getInfoStatus(tag.name).value as? LoadStatus.Success)?.let {
                        data.tags[index] = it.result
                    }
                }
                data.tags.sort()
                list.add(data)
            }
        }
        logger.info("finish request pool post. id: ${option.poolId}, size: ${list.size}")
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
                val yandeList: List<YandeTag> = client.get(url).body()
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
            val yandeList: ArrayList<YandeTag> = client.get(url).body()
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
            val userList: List<YandeUser> = client.get(getUserUrl(userId)).body()
            if (userList.isNotEmpty()) {
                return userList[0].toUserInfo()
            }
        } catch (_: Exception) {
        }
        return null
    }

    override fun RequestOption.parseSearchText(text: String): List<String> {
        if (text.isEmpty())
            return emptyList()
        val tagTexts = text.decode().split(" ")
        val tagList = ArrayList<String>()
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
        val tags = ArrayList<String>().apply { addAll(option.tags) }
        getRatingTag(option.ratings).let { if (it.isNotEmpty()) tags.add(it) }
        val tagStr = tags.joinToString("+")
        return "$baseUrl/post.json?page=${option.page}&limit=40&tags=$tagStr"
    }

    override fun getPoolUrl(option: RequestOption): String {
        return "$baseUrl/pool.json?page=${option.page}"
    }

    override fun getPoolPostUrl(option: RequestOption): String {
        return "$baseUrl/pool/show.json?id=${option.poolId}"
    }

    override fun getTagUrl(name: String, page: Int, limit: Int): String {
        val limit2 = if (limit < 5) 5 else limit
        return "$baseUrl/tag.json?name=$name&page=$page&limit=$limit2&order=count"
    }

    override fun getUserUrl(userId: Int): String {
        return "$baseUrl/user.json?id=$userId"
    }

    private fun getRatingTag(ratings: Int): String {
        if ((ratings and supportRating) == supportRating)
            return ""
        val currentRating = (ratings and supportRating)
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

    companion object {
        const val SOURCE = "yande"
    }
}
