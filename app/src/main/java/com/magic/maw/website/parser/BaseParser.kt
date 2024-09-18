package com.magic.maw.website.parser

import com.magic.maw.data.PostData
import com.magic.maw.website.RequestOption
import java.lang.ref.SoftReference

abstract class BaseParser {
    protected abstract val baseUrl: String
    abstract val source: String
    abstract val supportRating: Int
    open val firstPageIndex: Int = 1

    abstract suspend fun requestPostData(option: RequestOption): List<PostData>

    protected abstract fun getPostUrl(option: RequestOption): String

    companion object {
        private val parserMap = HashMap<String, SoftReference<BaseParser>>()

        fun getParser(source: String): BaseParser {
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

data class PostResult(
    val list: List<PostData> = emptyList(),
    val throwable: Throwable? = null,
)