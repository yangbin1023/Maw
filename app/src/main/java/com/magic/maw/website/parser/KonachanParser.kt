package com.magic.maw.website.parser

import co.touchlab.kermit.Logger
import com.magic.maw.data.PoolData
import com.magic.maw.data.PostData
import com.magic.maw.data.Rating
import com.magic.maw.data.TagInfo
import com.magic.maw.data.UserInfo
import com.magic.maw.data.konachan.KonachanData
import com.magic.maw.data.konachan.KonachanPool
import com.magic.maw.data.konachan.KonachanTag
import com.magic.maw.data.konachan.KonachanUser
import com.magic.maw.util.client
import com.magic.maw.util.configFlow
import com.magic.maw.util.cookie
import com.magic.maw.util.hasFlag
import com.magic.maw.util.isTextFile
import com.magic.maw.util.json
import com.magic.maw.util.readString
import com.magic.maw.website.DLTask
import com.magic.maw.website.RequestOption
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import java.io.File

private const val TAG = KonachanParser.SOURCE

/**
 * Konachan的API和Yande的API大部分相同
 */
class KonachanParser : YandeParser(), VerifyContainer {
    override val baseUrl: String get() = "https://konachan.net"
    override val source: String get() = SOURCE
    override val supportRating: Int get() = Rating.Safe.value or Rating.Questionable.value or Rating.Explicit.value
    private val verifying = atomic(false)
    private val verifySuccess = atomic(false)
    private val verifyChannel = Channel<VerifyResult>()

    override suspend fun requestPostData(option: RequestOption): List<PostData> {
        // pool.post 和 popular 没有第二页
        if ((option.poolId >= 0 || option.popularOption != null) && option.page > firstPageIndex)
            return emptyList()
        val url = getPostUrl(option)
        val (ret, msg) = securityRequest(url)
        if (!ret) {
            return emptyList()
        }
        Logger.d(TAG) { "url: $url, msg: $msg" }
        val list = ArrayList<PostData>()
        val ratings = configFlow.value.websiteConfig.rating
        if (option.poolId >= 0) {
            json.decodeFromString<KonachanPool>(msg).posts?.let { posts ->
                for (item in posts) {
                    val data = item.toPostData() ?: continue
                    if (ratings.hasFlag(data.rating.value)) {
                        list.add(data)
                    }
                }
            }
        } else {
            val konachanList: List<KonachanData> = json.decodeFromString(msg)
            for (item in konachanList) {
                val data = item.toPostData() ?: continue
                if (ratings.hasFlag(data.rating.value)) {
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
        val (ret, msg) = securityRequest(url)
        if (!ret) {
            return emptyList()
        }
        val poolList: ArrayList<KonachanPool> = json.decodeFromString(msg)
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
                val (ret, msg) = securityRequest(url)
                if (!ret) {
                    return null
                }
                val konachanList: List<KonachanTag> = json.decodeFromString(msg)
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

    override suspend fun requestSuggestTagInfo(
        name: String,
        limit: Int
    ): List<TagInfo> {
        if (name.isEmpty())
            return emptyList()
        val tagMap = HashMap<String, TagInfo>()
        val tagList = ArrayList<TagInfo>()
        try {
            val url = getTagUrl(name, firstPageIndex, limit)
            val (ret, msg) = securityRequest(url)
            if (!ret) {
                return emptyList()
            }
            val konachanList: List<KonachanTag> = json.decodeFromString(msg)
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
            val (ret, msg) = securityRequest(url)
            if (!ret) {
                return null
            }
            val userList: List<KonachanUser> = json.decodeFromString(msg)
            if (userList.isNotEmpty()) {
                return userList[0].toUserInfo()
            }
        } catch (_: Exception) {
        }
        return null
    }

    override fun getVerifyContainer(): VerifyContainer? {
        return if (onVerifyCallback == null) null else this
    }

    override suspend fun checkDlFile(file: File, task: DLTask): Boolean {
        if (file.isTextFile() && !task.baseData.fileType.isText()) {
            val text = file.readString()
            if (isVerifyHtml(text)) {
                if (!verifying.getAndSet(true)) {
                    Logger.d(TAG) { "konachan download failed. invoke onVerifyCallback, url: ${task.url}" }
                    onVerifyCallback?.invoke(task.url)
                }
                verifyChannel.receive()
            }
            return false
        }
        return true
    }

    override fun verifySuccess(url: String, text: String) {
        val tmpUrl = if (isJsonStr(text)) url else ""
        if (isVerifyHtml(text)) {
            Logger.w(TAG) { "verify result error. it's still verify html" }
            verifySuccess.value = false
        } else {
            verifySuccess.value = true
        }
        verifying.value = false
        verifyChannel.trySend(VerifyResult(result = verifySuccess.value, url = tmpUrl, text = text))
    }

    override fun cancelVerify() {
        verifySuccess.value = false
        verifying.value = false
        verifyChannel.trySend(VerifyResult(result = false))
    }

    private fun isVerifyHtml(msg: String): Boolean {
        return msg.startsWith("<!DOCTYPE") && msg.contains("<title>Just a moment...</title>")
    }

    private fun isErrorHtml(msg: String): Boolean {
        return msg.startsWith("<!DOCTYPE") && msg.contains("<title>Error")
    }

    private fun isJsonStr(msg: String): Boolean {
        try {
            json.parseToJsonElement(msg)
            return true
        } catch (_: Exception) {
        }
        return false
    }

    private suspend fun securityRequest(url: String): Pair<Boolean, String> {
        if (!verifySuccess.value) {
            val msg: String = client.get(url) { cookie() }.body()
            if (!isVerifyHtml(msg)) {
                return Pair(true, msg)
            }
            if (!verifying.getAndSet(true)) {
                Logger.d(TAG) { "konachan invoke onVerifyCallback, url: $url" }
                onVerifyCallback?.invoke(url)
            }
            Logger.d(TAG) { "konachan waiting for verify, url: $url" }
            val result = verifyChannel.receive()
            if (!result.result) {
                return Pair(false, "")
            }
            if (result.url == url) {
                return Pair(true, result.text)
            }
            delay(50 + (0L..150L).random())
            return securityRequest(url)
        } else {
            val msg: String = client.get(url) { cookie() }.body()
            if (isVerifyHtml(msg)) {
                this.verifySuccess.value = false
                return securityRequest(url)
            } else {
                return Pair(true, msg)
            }
        }
    }

    companion object {
        const val SOURCE = "konachan"
    }
}