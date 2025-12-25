package com.magic.maw.website.parser

import com.magic.maw.data.PoolData
import com.magic.maw.data.PostData
import com.magic.maw.data.Rating
import com.magic.maw.data.SettingsService
import com.magic.maw.data.TagInfo
import com.magic.maw.data.UserInfo
import com.magic.maw.data.WebsiteOption
import com.magic.maw.data.konachan.KonachanData
import com.magic.maw.data.konachan.KonachanPool
import com.magic.maw.data.konachan.KonachanTag
import com.magic.maw.data.konachan.KonachanUser
import com.magic.maw.util.client
import com.magic.maw.util.get
import com.magic.maw.website.RequestOption

private const val TAG = KonachanParser.SOURCE

/**
 * Konachan的API和Yande的API大部分相同
 */
class KonachanParser : YandeParser() {
    override val baseUrl: String get() = "https://konachan.net"
    override val website: WebsiteOption = WebsiteOption.Konachan
    override val source: String get() = SOURCE
    override val supportRating: Int get() = Rating.Safe.value or Rating.Questionable.value or Rating.Explicit.value

    override suspend fun requestPostData(option: RequestOption): List<PostData> {
        // pool.post 和 popular 没有第二页
        if ((option.poolId >= 0 || option.popularOption != null) && option.page > firstPageIndex)
            return emptyList()
        val url = getPostUrl(option)
        val list = ArrayList<PostData>()
        val ratings = SettingsService.settings.websiteSettings.ratings
        if (option.poolId >= 0) {
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
            for (item in konachanList) {
                val data = item.toPostData() ?: continue
                if (ratings.contains(data.rating)) {
                    list.add(data)
                }
            }
        }
        for (data in list) {
            for ((index, tag) in data.tags.withIndex()) {
                tagManager.get(tag.name)?.let {
                    data.tags[index] = it
                }
            }
            data.tags.sort()
        }
        return list
    }

    override suspend fun requestPoolData(option: RequestOption): List<PoolData> {
        val url = getPoolUrl(option)
        val poolList: ArrayList<KonachanPool> = client.get(url)
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
                val konachanList: List<KonachanTag> = client.get(url)
                var found = false
                for (konachanTag in konachanList) {
                    val tag = konachanTag.toTagInfo() ?: continue
                    if (tag.name == name) {
                        found = true
                        targetInfo = tag
                    }
                    tagMap[tag.name] = tag
                }
                retryCount = 0
                if (konachanList.isEmpty() || found)
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
            val konachanList: List<KonachanTag> = client.get(url)
            for (konachanTag in konachanList) {
                val tag = konachanTag.toTagInfo() ?: continue
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
            val url = getUserUrl(userId)
            val userList: List<KonachanUser> = client.get(url)
            if (userList.isNotEmpty()) {
                return userList[0].toUserInfo()
            }
        } catch (_: Exception) {
        }
        return null
    }

    companion object {
        const val SOURCE = "konachan"
    }
}