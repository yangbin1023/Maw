package com.magic.maw.website.parser

import com.magic.maw.data.PoolData
import com.magic.maw.data.PostData
import com.magic.maw.data.TagInfo
import com.magic.maw.data.UserInfo
import com.magic.maw.website.RequestOption
import com.magic.maw.website.TagManager
import com.magic.maw.website.UserManager
import java.io.File
import java.lang.ref.SoftReference
import java.net.URLDecoder
import java.net.URLEncoder

typealias OnVerifyCallback = (String, String) -> Unit

data class VerifyResult(
    val result: Boolean = false,
    val url: String = "",
    val text: String = "",
)

abstract class BaseParser {
    protected abstract val baseUrl: String
    abstract val source: String
    abstract val supportRating: Int
    abstract val tagManager: TagManager
    abstract val userManager: UserManager
    open val firstPageIndex: Int = 1
    protected var onVerifyCallback: OnVerifyCallback? = null

    abstract suspend fun requestPostData(option: RequestOption): List<PostData>
    abstract suspend fun requestPoolData(option: RequestOption): List<PoolData>
    abstract suspend fun requestPoolPostData(option: RequestOption): List<PostData>
    abstract suspend fun requestTagInfo(name: String): TagInfo?
    abstract suspend fun requestSuggestTagInfo(name: String, limit: Int = 10): List<TagInfo>
    abstract suspend fun requestUserInfo(userId: Int): UserInfo?
    abstract fun RequestOption.parseSearchText(text: String): List<String>

    protected abstract fun getPostUrl(option: RequestOption): String
    protected abstract fun getPoolUrl(option: RequestOption): String
    protected abstract fun getPoolPostUrl(option: RequestOption): String
    protected abstract fun getTagUrl(name: String, page: Int, limit: Int): String
    protected abstract fun getUserUrl(userId: Int): String

    fun setOnVerifyCallback(callback: OnVerifyCallback?) = apply { onVerifyCallback = callback }
    open fun checkVerifyResult(url: String, text: String): Boolean = false
    open fun cancelVerify() {}

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

        fun String.encode(enc: String = "UTF-8"): String {
            return URLEncoder.encode(this, enc)
        }

        fun String.decode(enc: String = "UTF-8"): String {
            return URLDecoder.decode(this, enc)
        }
    }
}
