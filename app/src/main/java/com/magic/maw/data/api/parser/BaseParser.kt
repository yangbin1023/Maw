package com.magic.maw.data.api.parser

import com.magic.maw.data.api.manager.TagManager
import com.magic.maw.data.api.manager.UserManager
import com.magic.maw.data.model.RequestOption
import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.entity.UserInfo
import com.magic.maw.data.model.site.PoolData
import com.magic.maw.data.model.site.PostData
import io.ktor.client.HttpClient
import java.net.URLDecoder
import java.net.URLEncoder

abstract class BaseParser {
    protected abstract val baseUrl: String
    abstract val website: WebsiteOption
    abstract val supportedRatings: List<Rating>
    open val supportedPopularDateTypes: List<PopularType> = PopularType.defaultSupportedDateTypes
    open val tagManager: TagManager by lazy { TagManager.get(website) }
    open val userManager: UserManager by lazy { UserManager.get(website) }
    open val firstPageIndex: Int = 1

    abstract suspend fun requestPostData(option: RequestOption): List<PostData>
    abstract suspend fun requestPoolData(option: RequestOption): List<PoolData>
    abstract suspend fun requestTagInfo(name: String): TagInfo?
    abstract suspend fun requestSuggestTagInfo(name: String, limit: Int = 10): List<TagInfo>
    abstract suspend fun requestUserInfo(userId: String): UserInfo?
    abstract fun parseSearchText(text: String): Set<String>

    protected abstract fun getPostUrl(option: RequestOption): String
    protected abstract fun getPoolUrl(option: RequestOption): String
    protected abstract fun getTagUrl(name: String, page: Int, limit: Int): String
    protected abstract fun getUserUrl(userId: String): String

    open suspend fun requestTagsByPostId(postId: String): List<TagInfo>? = null

    companion object {
        fun get(
            website: WebsiteOption,
            client: HttpClient = com.magic.maw.util.client
        ): BaseParser {
            return when (website) {
                WebsiteOption.Yande -> YandeParser(client)
                WebsiteOption.Konachan -> KonachanParser(client)
                WebsiteOption.Danbooru -> DanbooruParser(client)
            }
        }

        fun String.encode(enc: String = "UTF-8"): String {
            return URLEncoder.encode(this, enc)
        }

        fun String.decode(enc: String = "UTF-8"): String {
            return URLDecoder.decode(this, enc)
        }
    }
}

class WebsiteParserProvider(private val lists: List<BaseParser>) {
    operator fun get(website: WebsiteOption): BaseParser {
        return lists.find { it.website == website }
            ?: throw IllegalStateException("Unsupported website: $website")
    }
}