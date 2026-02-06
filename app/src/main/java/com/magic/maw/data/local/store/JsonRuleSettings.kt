package com.magic.maw.data.local.store

import kotlinx.serialization.Serializable

@Serializable
data class JsonPathRule<T>(
    val ruleList: List<String>,
    val defaultValue: T? = null,
    val invalidValues: List<T> = emptyList()
)

@Serializable
data class TypeTagsJsonPathRule(
    val type: Int,
    val rule: JsonPathRule<List<String>>
)

@Serializable
data class PostJsonPathRules(
    val dataLenRule: JsonPathRule<Int>,
    val idRule: JsonPathRule<String>,
    val creatorIdRule: JsonPathRule<String>,
    val creatorNameRule: JsonPathRule<String>? = null,
    val scoreRule: JsonPathRule<Int>,
    val sourceRule: JsonPathRule<String>,
    val uploadTimeRule: JsonPathRule<Long>,
    val ratingRule: JsonPathRule<String>,
    val fileTypeRule: JsonPathRule<String>? = null,
    val tagsRule: JsonPathRule<List<String>>,
    val typeTagsRules: List<TypeTagsJsonPathRule>? = null,
    val durationRule: JsonPathRule<Double>? = null,
    val previewUrlRule: JsonPathRule<String>,
    val previewWidthRule: JsonPathRule<Int>? = null,
    val previewHeightRule: JsonPathRule<Int>? = null,
    val previewSizeRule: JsonPathRule<Long>? = null,
    val sampleUrlRule: JsonPathRule<String>? = null,
    val sampleWidthRule: JsonPathRule<Int>? = null,
    val sampleHeightRule: JsonPathRule<Int>? = null,
    val sampleSizeRule: JsonPathRule<Long>? = null,
    val largerUrlRule: JsonPathRule<String>? = null,
    val largerWidthRule: JsonPathRule<Int>? = null,
    val largerHeightRule: JsonPathRule<Int>? = null,
    val largerSizeRule: JsonPathRule<Long>? = null,
    val originalUrlRule: JsonPathRule<String>,
    val originalWidthRule: JsonPathRule<Int>? = null,
    val originalHeightRule: JsonPathRule<Int>? = null,
    val originalSizeRule: JsonPathRule<Long>? = null,
)

@Serializable
data class PoolJsonPathRules(
    val dataLenRule: JsonPathRule<Int>,
    val idRule: JsonPathRule<String>,
    val nameRule: JsonPathRule<String>,
    val descriptionRule: JsonPathRule<String>? = null,
    val countRule: JsonPathRule<Int>,
    val createTimeRule: JsonPathRule<Long>? = null,
    val updateTimeRule: JsonPathRule<Long>? = null,
    val creatorIdRule: JsonPathRule<String>? = null,
    val creatorNameRule: JsonPathRule<String>? = null,
)

@Serializable
data class TagJsonPathRules(
    val dataLenRule: JsonPathRule<Int>,
    val idRule: JsonPathRule<String>,
    val nameRule: JsonPathRule<String>,
    val countRule: JsonPathRule<Int>,
    val typeRule: JsonPathRule<Int>,
    val createTimeRule: JsonPathRule<Long>? = null,
    val updateTimeRule: JsonPathRule<Long>? = null,
)

@Serializable
data class UserJsonPathRules(
    val idRule: JsonPathRule<String>,
    val nameRule: JsonPathRule<String>,
    val createTimeRule: JsonPathRule<Long>? = null,
    val updateTimeRule: JsonPathRule<Long>? = null,
)

@Serializable
data class RatingMapping(
    val item: String,
    val name: String
)

@Serializable
data class FileTypeMapping(
    val item: String,
    val name: String
)

@Serializable
data class IllegalTags(
    val startsWithTags: List<String>? = null,
    val endsWithTags: List<String>? = null,
    val containsTags: List<String>? = null
)

@Serializable
data class BaseEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

@Serializable
data class RequestInfo(
    val path: String,
    val method: String = "GET",
    val onlyOnePage: Boolean? = null,
    val params: List<BaseEntry<String, String>>? = null,
    val headers: List<BaseEntry<String, String>>? = null,
    val illegalTags: IllegalTags? = null,
    val defaultTags: List<String>? = null,
)

@Serializable
data class PopularRequestMapping(
    val type: String, // Day, Week, Month, Year, All
    val info: RequestInfo
)

@Serializable
data class RequestInfos(
    val baseUrl: String,
    val postUsePage: Boolean = true,
    val poolUsePage: Boolean = true,
    val tagUsePage: Boolean = true,
    val postInfo: RequestInfo,
    val poolInfo: RequestInfo,
    val poolPostInfo: RequestInfo,
    val popularMappings: List<PopularRequestMapping>,
    val suggestTagInfo: RequestInfo,
    val searchTagInfo: RequestInfo? = null,
    val searchTagsInfo: RequestInfo? = null,
    val searchUserInfo: RequestInfo,
    val headers: List<BaseEntry<String, String>>? = null,
    val commonIllegalTags: IllegalTags? = null
)

@Serializable
data class JsonRuleSettings(
    val website: String,
    val ratingMap: List<RatingMapping>,
    val fileTypeMap: List<FileTypeMapping>,
    val requestInfos: RequestInfos,
    val postRules: PostJsonPathRules,
    val poolRules: PoolJsonPathRules,
    val poolPostRules: PostJsonPathRules? = null,
    val tagRules: TagJsonPathRules,
    val userRules: UserJsonPathRules,
)
