package com.magic.maw.data.api.service

import co.touchlab.kermit.Logger
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.magic.maw.data.api.entity.PoolResponse
import com.magic.maw.data.api.entity.PopularOption
import com.magic.maw.data.api.entity.PostResponse
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.entity.RequestMeta
import com.magic.maw.data.local.store.IllegalTags
import com.magic.maw.data.local.store.JsonPathRule
import com.magic.maw.data.local.store.JsonRuleSettings
import com.magic.maw.data.local.store.PoolJsonPathRules
import com.magic.maw.data.local.store.PostJsonPathRules
import com.magic.maw.data.local.store.RatingMapping
import com.magic.maw.data.local.store.TagJsonPathRules
import com.magic.maw.data.local.store.UserJsonPathRules
import com.magic.maw.data.model.constant.FileType
import com.magic.maw.data.model.constant.FileType.Companion.toFileType
import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.model.constant.PopularType.Companion.toPopularType
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.Rating.Companion.toRating
import com.magic.maw.data.model.constant.TagType.Companion.toTagType
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.entity.UserInfo
import com.magic.maw.data.model.site.PoolData
import com.magic.maw.data.model.site.PostData
import com.magic.maw.util.get
import com.magic.maw.util.getFileTypeFromUrl
import com.magic.maw.util.toMonday
import com.magic.maw.util.toSunday
import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.path
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private const val TAG = "JsonPathApiService"

private fun getTimestamp(data: Any): Long? {
    return if (data is Long) {
        if (data > 100_000_000_000L) {
            data
        } else {
            data * 1000L
        }
    } else if (data is String) {
        Instant.parse(data).toEpochMilliseconds()
    } else {
        null
    }
}

private fun getTags(data: Any): List<String> {
    return when (data) {
        is List<*> -> {
            if (data.firstOrNull() is String) {
                data.map { it.toString() }.distinct()
            } else {
                emptyList()
            }
        }

        is String -> data.split(" ").filter { it.isNotBlank() }.distinct()
        else -> {
            println("unknown data type: ${data::class}, $data")
            emptyList()
        }
    }
}

private inline fun <reified T> readValue(
    document: DocumentContext,
    rule: JsonPathRule<T>,
    vararg args: Any?,
    noinline mapFunc: ((Any) -> T?)? = null
): T? {
    val result = rule.ruleList.asSequence()
        .map { rule -> if (args.isNotEmpty()) rule.format(*args) else rule }
        .mapNotNull { path -> document.read<Any?>(path) }
        .mapNotNull { raw ->
            val value = mapFunc?.invoke(raw) ?: coerceValue(raw)
            value?.takeIf { it !in rule.invalidValues }
        }
        .firstOrNull()

    return result ?: rule.defaultValue
}

private val config = Configuration.defaultConfiguration()
    .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
    .addOptions(Option.SUPPRESS_EXCEPTIONS)

/**
 * 智能类型强制转换
 */
private inline fun <reified T> coerceValue(data: Any): T? {
    val targetClass = T::class
    val raw = if (targetClass !is List<*> && data is List<*>) {
        data.firstOrNull()
    } else {
        data
    }
    return when {
        targetClass.isInstance(raw) -> raw as T
        targetClass == Byte::class && raw is Number -> raw.toByte() as T
        targetClass == Short::class && raw is Number -> raw.toShort() as T
        targetClass == Int::class && raw is Number -> raw.toInt() as T
        targetClass == Long::class && raw is Number -> raw.toLong() as T
        targetClass == Float::class && raw is Number -> raw.toFloat() as T
        targetClass == Double::class && raw is Number -> raw.toDouble() as T
        targetClass == String::class && raw is Number -> raw.toString() as T
        // 处理 String 转 Number 的情况
        targetClass == Byte::class && raw is String -> raw.toByteOrNull() as? T
        targetClass == Short::class && raw is String -> raw.toShortOrNull() as? T
        targetClass == Int::class && raw is String -> raw.toIntOrNull() as? T
        targetClass == Long::class && raw is String -> raw.toLongOrNull() as? T
        targetClass == Float::class && raw is String -> raw.toFloatOrNull() as? T
        targetClass == Double::class && raw is String -> raw.toDoubleOrNull() as? T
        else -> raw as? T
    }
}

private data class DateItem<T>(
    val items: List<T>,
    val noMore: Boolean = false,
)

private fun getPostData(
    rules: PostJsonPathRules,
    ratingMappings: List<RatingMapping>,
    website: WebsiteOption,
    content: String
): DateItem<PostData> {
    val document = JsonPath.using(config).parse(content)
    val dataLen = readValue(document, rules.dataLenRule) ?: run {
        Logger.e(TAG) { "get data len failed. rule: ${rules.dataLenRule.ruleList}, content: $content" }
        return DateItem(items = emptyList())
    }
    val list = mutableListOf<PostData>()

    for (index in 0 until dataLen) {
        val id = readValue(document, rules.idRule, index) ?: continue
        val createId = readValue(document, rules.creatorIdRule, index)
        val uploader = rules.creatorNameRule?.let { readValue(document, it, index) }
        val score = readValue(document, rules.scoreRule, index)
        val source = readValue(document, rules.sourceRule, index)
        val uploadTime = readValue(document, rules.uploadTimeRule, index) {
            getTimestamp(it)
        }
        val ratingItem = readValue(document, rules.ratingRule, index)
        val rating = ratingItem?.let { item ->
            ratingMappings.find { it.item == item }?.name
        }.toRating()
        val fileType = rules.fileTypeRule?.let { readValue(document, it, index)?.toFileType() }

        val tagRefs = readValue(document, rules.tagsRule, index) { getTags(it) } ?: emptyList()
        val tagsMap = mutableMapOf<String, TagInfo>()
        if (tagRefs.isNotEmpty() && rules.typeTagsRules != null) {
            for (item in rules.typeTagsRules) {
                readValue(document, item.rule, index) { getTags(it) }?.forEach {
                    if (tagRefs.contains(it)) {
                        tagsMap[it] = TagInfo(
                            website = website.name,
                            name = it,
                            type = item.type.toTagType()
                        )
                    }
                }
            }
        }
        val tags = tagsMap.values.toMutableList()
        tags.sort()

        val duration = rules.durationRule?.let { readValue(document, it, index) }?.toFloat()
        val previewUrl = readValue(document, rules.previewUrlRule, index) ?: continue
        val previewWidth = rules.previewWidthRule?.let { readValue(document, it, index) } ?: 0
        val previewHeight = rules.previewHeightRule?.let { readValue(document, it, index) } ?: 0
        val previewSize = rules.previewSizeRule?.let { readValue(document, it, index) } ?: 0
        val sampleUrl = rules.sampleUrlRule?.let { readValue(document, it, index) }
        val sampleWidth = rules.sampleWidthRule?.let { readValue(document, it, index) } ?: 0
        val sampleHeight = rules.sampleHeightRule?.let { readValue(document, it, index) } ?: 0
        val sampleSize = rules.sampleSizeRule?.let { readValue(document, it, index) } ?: 0
        val largerUrl = rules.largerUrlRule?.let { readValue(document, it, index) }
        val largerWidth = rules.largerWidthRule?.let { readValue(document, it, index) } ?: 0
        val largerHeight = rules.largerHeightRule?.let { readValue(document, it, index) } ?: 0
        val largerSize = rules.largerSizeRule?.let { readValue(document, it, index) } ?: 0
        val originalUrl = readValue(document, rules.originalUrlRule, index) ?: continue
        val originalWidth = rules.originalWidthRule?.let { readValue(document, it, index) } ?: 0
        val originalHeight = rules.originalHeightRule?.let { readValue(document, it, index) } ?: 0
        val originalSize = rules.originalSizeRule?.let { readValue(document, it, index) } ?: 0

        val data = PostData(
            website = website,
            id = id,
            createId = createId,
            uploader = uploader,
            score = score,
            srcUrl = source,
            uploadTime = uploadTime,
            rating = rating,
            fileType = fileType ?: getFileTypeFromUrl(originalUrl) ?: FileType.Jpeg,
            tags = tags,
            tagRefs = tagRefs,
            duration = duration,
            previewInfo = PostData.Info(
                url = previewUrl,
                width = previewWidth,
                height = previewHeight,
                size = previewSize,
                type = getFileTypeFromUrl(previewUrl) ?: FileType.Jpeg
            ),
            sampleInfo = sampleUrl?.let {
                PostData.Info(
                    url = it,
                    width = sampleWidth,
                    height = sampleHeight,
                    size = sampleSize,
                    type = getFileTypeFromUrl(it) ?: fileType ?: FileType.Jpeg
                )
            },
            largeInfo = largerUrl?.let {
                PostData.Info(
                    url = it,
                    width = largerWidth,
                    height = largerHeight,
                    size = largerSize,
                    type = getFileTypeFromUrl(it) ?: fileType ?: FileType.Jpeg
                )
            },
            originalInfo = PostData.Info(
                url = originalUrl,
                width = originalWidth,
                height = originalHeight,
                size = originalSize,
                type = getFileTypeFromUrl(originalUrl) ?: fileType ?: FileType.Jpeg
            )
        )

        list.add(data)
    }
    if (list.isEmpty()) {
        Logger.e(TAG) { "get empty list" }
    }
    return DateItem(items = list, noMore = dataLen <= 0)
}

private fun getPoolData(
    rules: PoolJsonPathRules,
    website: WebsiteOption,
    content: String
): DateItem<PoolData> {
    val document = JsonPath.using(config).parse(content)
    val dataLen = readValue(document, rules.dataLenRule) ?: run {
        Logger.e(TAG) { "get pool data len failed. rule: ${rules.dataLenRule.ruleList}, content: $content" }
        return DateItem(items = emptyList())
    }
    val list = mutableListOf<PoolData>()
    for (index in 0 until dataLen) {
        val id = readValue(document, rules.idRule, index) ?: continue
        val name = readValue(document, rules.nameRule, index) ?: ""
        val description = readValue(document, rules.nameRule, index)
        val count = readValue(document, rules.countRule, index) ?: 0
        val createTime = rules.createTimeRule?.let { rule ->
            readValue(document, rule, index) { getTimestamp(it) }
        }
        val updateTime = rules.updateTimeRule?.let { rule ->
            readValue(document, rule, index) { getTimestamp(it) }
        }
        val creatorId = rules.creatorIdRule?.let { readValue(document, it, index) }
        val creatorName = rules.creatorNameRule?.let { readValue(document, it, index) }
        val data = PoolData(
            website = website,
            id = id,
            name = name,
            description = description,
            count = count,
            createTime = createTime,
            updateTime = updateTime,
            createUid = creatorId,
            uploader = creatorName
        )
        list.add(data)
    }
    if (list.isEmpty()) {
        Logger.e(TAG) { "get empty pool list" }
    }
    return DateItem(items = list, noMore = dataLen <= 0)
}

private fun getTagData(
    rules: TagJsonPathRules,
    website: String,
    content: String
): DateItem<TagInfo> {
    val document = JsonPath.using(config).parse(content)
    val dataLen = readValue(document, rules.dataLenRule) ?: run {
        Logger.e(TAG) { "get tag data len failed. rule: ${rules.dataLenRule.ruleList}, content: $content" }
        return DateItem(items = emptyList())
    }
    val list = mutableListOf<TagInfo>()
    for (index in 0 until dataLen) {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = readValue(document, rules.idRule, index) ?: continue
        val name = readValue(document, rules.nameRule, index) ?: continue
        val count = readValue(document, rules.countRule, index) ?: 0
        val type = readValue(document, rules.typeRule, index) ?: 0
        val createTime = rules.createTimeRule?.let { rule ->
            readValue(document, rule, index) { getTimestamp(it) }
        } ?: now
        val updateTime = rules.updateTimeRule?.let { rule ->
            readValue(document, rule, index) { getTimestamp(it) }
        } ?: now
        val data = TagInfo(
            website = website,
            name = name,
            tagId = id,
            count = count,
            type = type.toTagType(),
            createTime = Instant.fromEpochMilliseconds(createTime),
            updateTime = Instant.fromEpochMilliseconds(updateTime),
        )
        list.add(data)
    }
    return DateItem(items = list, noMore = dataLen <= 0)
}

private fun getUserData(
    rules: UserJsonPathRules,
    website: String,
    content: String
): UserInfo? {
    val document = JsonPath.using(config).parse(content)
    val id = readValue(document, rules.idRule) ?: return null
    val name = readValue(document, rules.nameRule) ?: return null
    val now = Clock.System.now().toEpochMilliseconds()
    val createTime = rules.createTimeRule?.let { rule ->
        readValue(document, rule) { getTimestamp(it) }
    } ?: now
    val updateTime = rules.updateTimeRule?.let { rule ->
        readValue(document, rule) { getTimestamp(it) }
    } ?: now

    return UserInfo(
        website = website,
        userId = id,
        name = name,
        createTime = Instant.fromEpochMilliseconds(createTime),
        updateTime = Instant.fromEpochMilliseconds(updateTime),
    )
}

class JsonPathApiService(
    override val website: WebsiteOption,
    private val client: HttpClient,
    private val jsonRuleSettings: JsonRuleSettings
) : BaseApiService() {
    override val baseUrl: String = jsonRuleSettings.requestInfos.baseUrl
    override val supportedRatings: List<Rating> =
        jsonRuleSettings.ratingMap.map {
            it.name.toRating()
        }.filter { it != Rating.None }.distinct().apply {
            Logger.d(TAG) { "support ratings: $this" }
        }

    override val supportedPopularDateTypes: List<PopularType> =
        jsonRuleSettings.requestInfos.popularMappings.mapNotNull {
            it.type.toPopularType()
        }.distinct().apply {
            Logger.d(TAG) { "support popular types: $this" }
        }

    override suspend fun getPostData(filter: RequestFilter, meta: RequestMeta): PostResponse {
        val url = getPostUrl(filter, meta)
        if (url.isEmpty()) {
            return PostResponse(items = emptyList(), meta = RequestMeta(prev = meta.next))
        }
        val content: String = client.get<String>(url)
        val rules = if (filter.poolId.isNullOrBlank()) {
            jsonRuleSettings.postRules
        } else {
            jsonRuleSettings.poolPostRules ?: jsonRuleSettings.postRules
        }
        val value = getPostData(rules, jsonRuleSettings.ratingMap, website, content)
        if (value.items.isEmpty()) {
            Logger.e(TAG) { "get data empty" }
        }
        val nextMeta = getNextMeta(meta, value.noMore)
        return PostResponse(items = value.items, meta = nextMeta)
    }

    override suspend fun getPoolData(filter: RequestFilter, meta: RequestMeta): PoolResponse {
        val url = getPoolUrl(meta)
        val content: String = client.get(url)
        val value = getPoolData(jsonRuleSettings.poolRules, website, content)
        val nextMeta = getNextMeta(meta, value.noMore)
        return PoolResponse(items = value.items, meta = nextMeta)
    }

    override suspend fun getSuggestTagInfo(name: String, limit: Int): List<TagInfo> {
        if (name.isEmpty())
            return emptyList()
        try {
            val content: String = client.get(getSuggestTagUrl(name))
            return getTagData(jsonRuleSettings.tagRules, website.name, content).items
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            Logger.e(TAG) { "request suggest tag info failed. name[$name], error: $e" }
        }
        return emptyList()
    }

    override suspend fun getTagByName(tagName: String): List<TagInfo> {
        if (tagName.isEmpty())
            return emptyList()
        if (jsonRuleSettings.requestInfos.tagUsePage) {
            val tagMap = mutableMapOf<String, TagInfo>()
            var retryCount = 0
            var page = 1
            do {
                try {
                    val meta = RequestMeta(next = page.toString())
                    val url = getTagUrl(tagName, meta)
                    if (url.isBlank()) {
                        return emptyList()
                    }
                    val content: String = client.get(url)
                    val value = getTagData(jsonRuleSettings.tagRules, website.name, content)
                    val list = value.items
                    val found = list.find { it.name == tagName } != null
                    tagMap += list.associateBy { it.name }
                    if (value.noMore || found)
                        break
                } catch (_: Exception) {
                    retryCount++
                    if (retryCount >= 3) break else continue
                }
                page++
            } while (true)
            return tagMap.values.toList()
        } else {
            try {
                val url = getTagUrl(tagName, RequestMeta())
                if (url.isBlank()) {
                    return emptyList()
                }
                val content: String = client.get(url)
                return getTagData(jsonRuleSettings.tagRules, website.name, content).items
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                Logger.e(TAG) { "request tag info failed. name[$tagName], error: $e" }
            }
        }
        return emptyList()
    }

    override suspend fun getUserInfo(userId: String): UserInfo? {
        return try {
            val url = getUserUrl(userId)
            val content: String = client.get(url)
            return getUserData(jsonRuleSettings.userRules, website.name, content)
        } catch (e: Exception) {
            Logger.e(TAG) { "request user info failed. id[$userId], error: $e" }
            null
        }
    }

    private fun getPostUrl(filter: RequestFilter, meta: RequestMeta): String {
        val requestInfos = jsonRuleSettings.requestInfos
        val builder = URLBuilder().takeFrom(requestInfos.baseUrl)
        val params = mutableMapOf<String, String>()
        val requestInfo = if (filter.poolId != null) {
            params["poolId"] = filter.poolId
            requestInfos.poolPostInfo
        } else if (filter.popularOption != null) {
            params["currentDate"] = filter.popularOption.date.toString()
            params["currentDay"] = filter.popularOption.date.dayOfMonth.toString()
            params["currentMonth"] = filter.popularOption.date.monthNumber.toString()
            params["currentYear"] = filter.popularOption.date.year.toString()
            getStartAndEndDate(filter.popularOption)?.let { (startDate, endDate) ->
                params["startDate"] = startDate.toString()
                params["endDate"] = endDate.toString()
            }
            requestInfos.popularMappings.find {
                it.type == filter.popularOption.type.name
            }?.info ?: requestInfos.postInfo
        } else {
            requestInfos.postInfo
        }
        if (requestInfo.onlyOnePage == true) {
            meta.next?.let { return "" }
        }
        params["limit"] = 80.toString()
        meta.prev?.let { params["prev"] = it }
        meta.next?.let { params["next"] = it }
        if (requestInfos.postUsePage) {
            params["next"] = meta.page.toString()
        }
        val tags = filter.tags
            .dealTags(requestInfos.commonIllegalTags)
            .dealTags(requestInfo.illegalTags, requestInfo.defaultTags, params)
        params["tags"] = tags.joinToString("+")

        builder.path(requestInfo.path)
        builder.parameters.apply {
            requestInfo.params?.forEach { param ->
                append(param.key, param.value.fillParams(params))
            }
        }
        return builder.build().toString().apply {
            Logger.d(TAG) { "post url: $this, tags: $tags, option: ${filter.tags}" }
        }
    }

    private fun Collection<String>.dealTags(
        illegalTags: IllegalTags? = null,
        defaultTags: List<String>? = null,
        params: Map<String, String>? = null,
    ): List<String> {
        val tags = this.toMutableList()
        illegalTags?.let { (startsWithTags, endsWithTags, containsTags) ->
            startsWithTags?.forEach { tag ->
                tags.removeIf { it.startsWith(tag) }
            }
            endsWithTags?.forEach { tag ->
                tags.removeIf { it.endsWith(tag) }
            }
            containsTags?.forEach { tag ->
                tags.removeIf { it.contains(tag) }
            }
        }
        params?.let {
            defaultTags?.forEach {
                tags.add(it.fillParams(params, false))
            }
        } ?: run {
            defaultTags?.let {
                tags.addAll(it)
            }
        }
        return tags
    }

    private fun String.fillParams(params: Map<String, String>, retain: Boolean = true): String {
        val regex = Regex("\\{\\{(\\w+)\\}\\}") // 匹配 {{any_name}}
        return regex.replace(this) { matchResult ->
            val key = matchResult.groupValues[1]
            params[key] ?: if (retain) matchResult.value else ""
        }
    }

    private fun getPoolUrl(meta: RequestMeta): String {
        val requestInfos = jsonRuleSettings.requestInfos
        val builder = URLBuilder().takeFrom(requestInfos.baseUrl)
        val params = mutableMapOf<String, String>()
        params["limit"] = 80.toString()
        meta.prev?.let { params["prev"] = it }
        meta.next?.let { params["next"] = it }
        if (requestInfos.poolUsePage) {
            params["next"] = meta.page.toString()
        }
        val requestInfo = requestInfos.poolInfo
        builder.path(requestInfo.path)
        builder.parameters.apply {
            requestInfo.params?.forEach { param ->
                append(param.key, param.value.fillParams(params))
            }
        }
        return builder.build().toString().apply {
            Logger.d(TAG) { "pool url: $this" }
        }
    }

    private fun getStartAndEndDate(popularOption: PopularOption): Pair<LocalDate, LocalDate>? {
        val date = popularOption.date
        return when (popularOption.type) {
            PopularType.Week -> {
                Pair(date.toMonday(), date.toSunday())
            }

            PopularType.Month -> {
                val start = date.minus(DatePeriod(days = date.dayOfMonth))
                val end = start.plus(DatePeriod(months = 1))
                Pair(start, end)
            }

            PopularType.Year -> {
                val start = date.minus(DatePeriod(days = date.dayOfYear))
                val end = start.plus(DatePeriod(years = 1))
                Pair(start, end)
            }

            else -> null
        }
    }

    private fun getSuggestTagUrl(name: String): String {
        val requestInfos = jsonRuleSettings.requestInfos
        val builder = URLBuilder().takeFrom(requestInfos.baseUrl)
        val params = mutableMapOf<String, String>()
        params["searchTagName"] = name
        params["limit"] = 10.toString()
        if (requestInfos.tagUsePage) {
            params["next"] = 1.toString()
        }
        val requestInfo = requestInfos.suggestTagInfo
        builder.path(requestInfo.path)
        builder.parameters.apply {
            requestInfo.params?.forEach { param ->
                append(param.key, param.value.fillParams(params, false))
            }
        }
        return builder.build().toString().apply {
            Logger.d(TAG) { "suggest tag: $name, url: $this" }
        }
    }

    private fun getTagUrl(name: String, meta: RequestMeta, limit: Int = 20): String {
        val requestInfos = jsonRuleSettings.requestInfos
        val builder = URLBuilder().takeFrom(requestInfos.baseUrl)
        val params = mutableMapOf<String, String>()
        params["searchTagName"] = name
        params["limit"] = limit.toString()
        meta.prev?.let { params["prev"] = it }
        meta.next?.let { params["next"] = it }
        if (requestInfos.tagUsePage) {
            params["next"] = meta.page.toString()
        }
        val requestInfo = requestInfos.searchTagInfo ?: requestInfos.suggestTagInfo
        builder.path(requestInfo.path)
        builder.parameters.apply {
            requestInfo.params?.forEach { param ->
                append(param.key, param.value.fillParams(params, false))
            }
        }
        return builder.build().toString().apply {
            Logger.d(TAG) { "search tag: $name, url: $this" }
        }
    }

    private fun getUserUrl(userId: String): String {
        val requestInfos = jsonRuleSettings.requestInfos
        val builder = URLBuilder().takeFrom(requestInfos.baseUrl)
        val params = mutableMapOf<String, String>()
        params["searchUserId"] = userId
        val requestInfo = requestInfos.searchUserInfo
        builder.path(requestInfo.path)
        builder.parameters.apply {
            requestInfo.params?.forEach { param ->
                append(param.key, param.value.fillParams(params, false))
            }
        }
        return builder.build().toString().apply {
            Logger.d(TAG) { "search user id: $userId, url: $this" }
        }
    }

    private fun getRatingTag(ratings: List<Rating>): String {
        if (ratings.toSet() == supportedRatings.toSet()) {
            return ""
        }
        val ratingList: MutableList<String> = ArrayList<String>()
        if (ratings.contains(Rating.General))
            ratingList.add("g")
        if (ratings.contains(Rating.Sensitive))
            ratingList.add("s")
        if (ratings.contains(Rating.Questionable))
            ratingList.add("q")
        if (ratings.contains(Rating.Explicit))
            ratingList.add("e")
        if (ratingList.isEmpty())
            ratingList.add("g")
        return "rating:" + ratingList.joinToString(",")
    }
}