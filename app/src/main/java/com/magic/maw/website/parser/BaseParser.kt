package com.magic.maw.website.parser

import com.magic.maw.data.PostData
import com.magic.maw.data.TagInfo
import com.magic.maw.website.RequestOption
import com.magic.maw.website.TagManager
import java.lang.ref.SoftReference

abstract class BaseParser {
    protected abstract val baseUrl: String
    abstract val source: String
    abstract val supportRating: Int
    abstract val tagManager: TagManager
    open val firstPageIndex: Int = 1

    abstract suspend fun requestPostData(option: RequestOption): List<PostData>
    abstract suspend fun requestTagInfo(name: String): TagInfo?
    abstract fun RequestOption.parseSearchText(text: String): Boolean

    protected abstract fun getPostUrl(option: RequestOption): String
    protected abstract fun getTagUrl(name: String, page: Int, limit: Int): String

    companion object {
        private val parserMap = HashMap<String, SoftReference<BaseParser>>()

        fun get(source: String): BaseParser {
            synchronized(parserMap) {
                parserMap[source]?.get()?.let { return it }
                val parser = when (source) {
                    YandeParser.SOURCE -> YandeParser()
                    else -> throw RuntimeException("Unknown source: $source")
                }
                parserMap[source] = SoftReference(parser)
                return parser
            }
        }
    }
}
