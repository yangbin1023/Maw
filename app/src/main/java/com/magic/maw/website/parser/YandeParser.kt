package com.magic.maw.website.parser

import com.magic.maw.data.PostData
import com.magic.maw.data.Rating
import com.magic.maw.data.yande.YandeData
import com.magic.maw.util.HttpUtils
import com.magic.maw.util.JsonUtils
import com.magic.maw.website.RequestOption
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class YandeParser : BaseParser() {
    override val baseUrl: String get() = "https://yande.re"
    override val source: String get() = SOURCE
    override val supportRating: Int get() = Rating.Safe.value or Rating.Questionable.value or Rating.Explicit.value

    override suspend fun requestPostData(option: RequestOption): List<PostData> =
        suspendCancellableCoroutine { cont ->
            HttpUtils.getHttp().async(getPostUrl(option)).setOnResponse { response ->
                try {
                    var list: List<PostData> = emptyList()
                    JsonUtils.fromJson2List<YandeData>(response.body.toString())?.let { yandeList ->
                        val postList = ArrayList<PostData>()
                        for (item in yandeList) {
                            postList.add(item.toData() ?: continue)
                        }
                        list = postList
                    }
                    if (cont.isActive) {
                        cont.resume(list)
                    }
                } catch (e: Throwable) {
                    if (cont.isActive) {
                        cont.resumeWithException(e)
                    }
                }
            }.setOnException {
                if (cont.isActive) {
                    cont.resumeWithException(it)
                }
            }.get()
        }

    override fun getPostUrl(option: RequestOption): String {
        val tags = ArrayList<String>().apply { addAll(option.tags) }
        getRatingTag(option.ratings).let { if (it.isNotEmpty()) tags.add(it) }
        val tagStr = tags.joinToString("+")
        return "$baseUrl/post.json?page=${option.page}&limit=40&tags=$tagStr".apply {
            println("post url: $this")
        }
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