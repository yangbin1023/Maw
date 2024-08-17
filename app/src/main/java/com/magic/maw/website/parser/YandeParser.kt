package com.magic.maw.website.parser

import com.magic.maw.data.PostData
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
        val tags = option.tags.joinToString("+")
        return "$baseUrl/post.json?page=${option.page}&limit=40&tags=rating:s"
    }

    companion object {
        const val SOURCE = "yande"
    }
}