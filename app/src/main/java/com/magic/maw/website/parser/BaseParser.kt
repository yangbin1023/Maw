package com.magic.maw.website.parser

import com.magic.maw.data.PoolData
import com.magic.maw.data.PopularType
import com.magic.maw.data.PostData
import com.magic.maw.data.Rating
import com.magic.maw.data.TagInfo
import com.magic.maw.data.UserInfo
import com.magic.maw.data.WebsiteOption
import com.magic.maw.website.RequestOption
import com.magic.maw.website.TagManager
import com.magic.maw.website.UserManager
import java.lang.ref.SoftReference
import java.net.URLDecoder
import java.net.URLEncoder

abstract class BaseParser {
    protected abstract val baseUrl: String
    abstract val website: WebsiteOption
    abstract val source: String
    abstract val supportRating: Int
    abstract val supportRatings: List<Rating>
    open val supportPopular: Int = PopularType.defaultSupport
    open val tagManager: TagManager by lazy { TagManager.get(source) }
    open val userManager: UserManager by lazy { UserManager.get(source) }
    open val firstPageIndex: Int = 1

    abstract suspend fun requestPostData(option: RequestOption): List<PostData>
    abstract suspend fun requestPoolData(option: RequestOption): List<PoolData>
    abstract suspend fun requestTagInfo(name: String): TagInfo?
    abstract suspend fun requestSuggestTagInfo(name: String, limit: Int = 10): List<TagInfo>
    abstract suspend fun requestUserInfo(userId: Int): UserInfo?
    abstract fun RequestOption.parseSearchText(text: String): List<String>

    protected abstract fun getPostUrl(option: RequestOption): String
    protected abstract fun getPoolUrl(option: RequestOption): String
    protected abstract fun getTagUrl(name: String, page: Int, limit: Int): String
    protected abstract fun getUserUrl(userId: Int): String

    companion object {
        private val parserMap = HashMap<String, SoftReference<BaseParser>>()

        fun get(source: String): BaseParser {
            synchronized(parserMap) {
                parserMap[source]?.get()?.let { return it }
                val parser = when (source) {
                    YandeParser.SOURCE -> YandeParser()
                    KonachanParser.SOURCE -> KonachanParser()
                    DanbooruParser.SOURCE -> DanbooruParser()
                    else -> throw RuntimeException("Unknown source: $source")
                }
                parserMap[source] = SoftReference(parser)
                return parser
            }
        }

        fun get(website: WebsiteOption): BaseParser {
            return when(website) {
                WebsiteOption.Yande -> YandeParser()
                WebsiteOption.Konachan -> KonachanParser()
                WebsiteOption.Danbooru -> DanbooruParser()
            }
        }

        fun getIndex(source: String): Int {
            return when (source) {
                YandeParser.SOURCE -> 0
                KonachanParser.SOURCE -> 1
                DanbooruParser.SOURCE -> 2
                else -> 0
            }
        }

        fun getTag(source: String): String {
            return when (source) {
                "Yande" -> YandeParser.SOURCE
                "Konachan" -> KonachanParser.SOURCE
                "Danbooru" -> DanbooruParser.SOURCE
                else -> ""
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
