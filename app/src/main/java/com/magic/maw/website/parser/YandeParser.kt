package com.magic.maw.website.parser

import com.magic.maw.data.PostData
import com.magic.maw.data.Rating
import com.magic.maw.data.TagInfo
import com.magic.maw.data.yande.YandeData
import com.magic.maw.data.yande.YandeTag
import com.magic.maw.util.client
import com.magic.maw.website.LoadStatus
import com.magic.maw.website.RequestOption
import com.magic.maw.website.TagManager
import io.ktor.client.call.body
import io.ktor.client.request.get
import java.net.URLDecoder

class YandeParser : BaseParser() {
    override val baseUrl: String get() = "https://yande.re"
    override val source: String get() = SOURCE
    override val supportRating: Int get() = Rating.Safe.value or Rating.Questionable.value or Rating.Explicit.value
    override val tagManager: TagManager = TagManager.get(source)

    override suspend fun requestPostData(option: RequestOption): List<PostData> {
        val yandeList: ArrayList<YandeData> = client.get(getPostUrl(option)).body()
        val list: ArrayList<PostData> = ArrayList()
        for (item in yandeList) {
            val data = item.toData() ?: continue
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
                val yandeList: ArrayList<YandeTag> = client.get(url).body()
                var found = false
                for (yandeTag in yandeList) {
                    val tag = yandeTag.toData() ?: continue
                    if (tag.name == name) {
                        found = true
                        targetInfo = tag
                    }
                    tagMap[tag.name] = tag
                }
                retryCount = 0
                if (yandeList.isEmpty() || found)
                    break
            } catch (e: Exception) {
                retryCount++
                if (retryCount >= 3) break else continue
            }
            page++
        } while (true)
        tagManager.addAll(tagMap)
        return targetInfo
    }

    override fun RequestOption.parseSearchText(text: String): Boolean {
        if (text.isEmpty())
            return false
        val tagTexts = URLDecoder.decode(text, "UTF-8").split(" ")
        val tagList = ArrayList<String>()
        for (tagText in tagTexts) {
            if (tagText.isEmpty())
                continue
            if (tagText.startsWith("tag:") || tagText.startsWith("-tag:"))
                continue
            tagList.add(tagText)
        }
        if (tagList.isNotEmpty()) {
            addTags(tagList)
        }
        return tagList.isNotEmpty()
    }

    override fun getPostUrl(option: RequestOption): String {
        val tags = ArrayList<String>().apply { addAll(option.tags) }
        getRatingTag(option.ratings).let { if (it.isNotEmpty()) tags.add(it) }
        val tagStr = tags.joinToString("+")
        return "$baseUrl/post.json?page=${option.page}&limit=40&tags=$tagStr".apply {
            println("post url: $this")
        }
    }

    override fun getTagUrl(name: String, page: Int, limit: Int): String {
        val limit2 = if (limit < 5) 5 else limit
        return "$baseUrl/tag.json?name=$name&page=$page&limit=$limit2&order=count"
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