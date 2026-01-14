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

    companion object {
        fun get(website: WebsiteOption): BaseParser {
            return when (website) {
                WebsiteOption.Yande -> YandeParser
                WebsiteOption.Konachan -> KonachanParser
                WebsiteOption.Danbooru -> DanbooruParser
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
