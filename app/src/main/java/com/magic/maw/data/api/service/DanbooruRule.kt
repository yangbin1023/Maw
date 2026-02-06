package com.magic.maw.data.api.service

import com.magic.maw.data.local.store.BaseEntry
import com.magic.maw.data.local.store.IllegalTags
import com.magic.maw.data.local.store.JsonPathRule
import com.magic.maw.data.local.store.JsonRuleSettings
import com.magic.maw.data.local.store.PoolJsonPathRules
import com.magic.maw.data.local.store.PopularRequestMapping
import com.magic.maw.data.local.store.PostJsonPathRules
import com.magic.maw.data.local.store.RatingMapping
import com.magic.maw.data.local.store.RequestInfo
import com.magic.maw.data.local.store.RequestInfos
import com.magic.maw.data.local.store.TagJsonPathRules
import com.magic.maw.data.local.store.TypeTagsJsonPathRule
import com.magic.maw.data.local.store.UserJsonPathRules
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.TagType
import com.magic.maw.data.model.constant.WebsiteOption

private val ratingMapping = listOf(
    RatingMapping(item = "g", name = Rating.General.name),
    RatingMapping(item = "s", name = Rating.Sensitive.name),
    RatingMapping(item = "q", name = Rating.Questionable.name),
    RatingMapping(item = "e", name = Rating.Explicit.name),
)

private val postRules = PostJsonPathRules(
    dataLenRule = JsonPathRule(ruleList = listOf("$.length()")),
    idRule = JsonPathRule(ruleList = listOf("$[%d].id"), invalidValues = listOf("")),
    creatorIdRule = JsonPathRule(ruleList = listOf("$[%d].uploader_id")),
    scoreRule = JsonPathRule(ruleList = listOf("$[%d].score")),
    sourceRule = JsonPathRule(ruleList = listOf("$[%d].source")),
    uploadTimeRule = JsonPathRule(ruleList = listOf("$[%d].updated_at")),
    ratingRule = JsonPathRule(ruleList = listOf("$[%d].rating")),
    fileTypeRule = JsonPathRule(
        ruleList = listOf(
            "$[%d].file_ext",
            "$[%d].media_asset.file_ext",
        ),
        invalidValues = listOf("")
    ),
    tagsRule = JsonPathRule(ruleList = listOf("$[%d].tag_string")),
    typeTagsRules = listOf(
        TypeTagsJsonPathRule(
            type = TagType.General.value,
            rule = JsonPathRule(ruleList = listOf("$[%d].tag_string_general"))
        ),
        TypeTagsJsonPathRule(
            type = TagType.Character.value,
            rule = JsonPathRule(ruleList = listOf("$[%d].tag_string_character"))
        ),
        TypeTagsJsonPathRule(
            type = TagType.Copyright.value,
            rule = JsonPathRule(ruleList = listOf("$[%d].tag_string_copyright"))
        ),
        TypeTagsJsonPathRule(
            type = TagType.Circle.value,
            rule = JsonPathRule(ruleList = listOf("$[%d].tag_string_meta"))
        ),
    ),
    durationRule = JsonPathRule(ruleList = listOf("$[%d].duration")),
    previewUrlRule = JsonPathRule(
        ruleList = listOf(
            "$[%d].media_asset.variants[?(@.type=='360x360')].url",
            "$[%d].media_asset.variants[?(@.type=='720x720')].url",
            "$[%d].media_asset.variants[?(@.type=='180x180')].url",
            "$[%d].preview_file_url",
        ),
        invalidValues = listOf("")
    ),
    sampleUrlRule = JsonPathRule(
        ruleList = listOf(
//            "$[%d].large_file_url", // 可能和originalUrl相同
            "$[%d].media_asset.variants[?(@.type=='sample')].url",
        ),
        invalidValues = listOf("")
    ),
    sampleWidthRule = JsonPathRule(ruleList = listOf("$[%d].media_asset.variants[?(@.type=='sample')].width")),
    sampleHeightRule = JsonPathRule(ruleList = listOf("$[%d].media_asset.variants[?(@.type=='sample')].height")),
    originalUrlRule = JsonPathRule(
        ruleList = listOf(
            "$[%d].file_url",
            "$[%d].media_asset.variants[?(@.type=='original')].url",
        ),
        invalidValues = listOf("")
    ),
    originalWidthRule = JsonPathRule(
        ruleList = listOf(
            "$[%d].image_width",
            "$[%d].media_asset.variants[?(@.type=='original')].width",
        ),
    ),
    originalHeightRule = JsonPathRule(
        ruleList = listOf(
            "$[%d].image_height",
            "$[%d].media_asset.variants[?(@.type=='original')].height",
        ),
    ),
    originalSizeRule = JsonPathRule(
        ruleList = listOf(
            "$[%d].file_size",
            "$[%d].media_asset.file_size",
        ),
    ),
)

private val poolRules = PoolJsonPathRules(
    dataLenRule = JsonPathRule(ruleList = listOf("$.length()")),
    idRule = JsonPathRule(ruleList = listOf("$[%d].id"), invalidValues = listOf("")),
    nameRule = JsonPathRule(ruleList = listOf("$[%d].name"), invalidValues = listOf("")),
    descriptionRule = JsonPathRule(
        ruleList = listOf("$[%d].description"),
        invalidValues = listOf("")
    ),
    countRule = JsonPathRule(ruleList = listOf("$[%d].post_count")),
    createTimeRule = JsonPathRule(ruleList = listOf("$[%d].created_at")),
    updateTimeRule = JsonPathRule(ruleList = listOf("$[%d].updated_at")),
)

private val tagRules = TagJsonPathRules(
    dataLenRule = JsonPathRule(ruleList = listOf("$.length()")),
    idRule = JsonPathRule(ruleList = listOf("$[%d].id"), invalidValues = listOf("")),
    nameRule = JsonPathRule(ruleList = listOf("$[%d].name"), invalidValues = listOf("")),
    countRule = JsonPathRule(ruleList = listOf("$[%d].post_count")),
    typeRule = JsonPathRule(ruleList = listOf("$[%d].category")),
    createTimeRule = JsonPathRule(ruleList = listOf("$[%d].created_at")),
    updateTimeRule = JsonPathRule(ruleList = listOf("$[%d].updated_at")),
)

private val userRules = UserJsonPathRules(
    idRule = JsonPathRule(ruleList = listOf("$[0].id"), invalidValues = listOf("")),
    nameRule = JsonPathRule(ruleList = listOf("$[0].name"), invalidValues = listOf("")),
    createTimeRule = JsonPathRule(ruleList = listOf("$[0].created_at")),
)

private val requestInfos = RequestInfos(
    baseUrl = "https://danbooru.donmai.us",
    postInfo = RequestInfo(
        path = "posts.json",
        params = listOf(
            BaseEntry("page", "{{next}}"),
            BaseEntry("limit", "{{limit}}"),
            BaseEntry("tags", "{{tags}}"),
        )
    ),
    poolInfo = RequestInfo(
        path = "pools.json",
        params = listOf(
            BaseEntry("page", "{{next}}"),
            BaseEntry("search[order]", "created_at"),
        )
    ),
    poolPostInfo = RequestInfo(
        path = "posts.json",
        params = listOf(
            BaseEntry("page", "{{next}}"),
            BaseEntry("limit", "{{limit}}"),
            BaseEntry("tags", "{{tags}}"),
        ),
        defaultTags = listOf("pool:{{poolId}}")
    ),
    popularMappings = listOf(
        PopularRequestMapping(
            type = "Day",
            info = RequestInfo(
                path = "explore/posts/popular.json",
                params = listOf(
                    BaseEntry("date", "{{currentDate}}"),
                    BaseEntry("scale", "day"),
                    BaseEntry("page", "{{next}}"),
                    BaseEntry("limit", "{{limit}}"),
                    BaseEntry("tags", "{{tags}}"),
                )
            )
        ),
        PopularRequestMapping(
            type = "Week",
            info = RequestInfo(
                path = "explore/posts/popular.json",
                params = listOf(
                    BaseEntry("date", "{{currentDate}}"),
                    BaseEntry("scale", "week"),
                    BaseEntry("page", "{{next}}"),
                    BaseEntry("limit", "{{limit}}"),
                    BaseEntry("tags", "{{tags}}"),
                )
            )
        ),
        PopularRequestMapping(
            type = "Month",
            info = RequestInfo(
                path = "explore/posts/popular.json",
                params = listOf(
                    BaseEntry("date", "{{currentDate}}"),
                    BaseEntry("scale", "month"),
                    BaseEntry("page", "{{next}}"),
                    BaseEntry("limit", "{{limit}}"),
                    BaseEntry("tags", "{{tags}}"),
                )
            )
        ),
        PopularRequestMapping(
            type = "Year",
            info = RequestInfo(
                path = "explore/posts/popular.json",
                params = listOf(
                    BaseEntry("date", "{{currentDate}}"),
                    BaseEntry("scale", "year"),
                    BaseEntry("page", "{{next}}"),
                    BaseEntry("limit", "{{limit}}"),
                    BaseEntry("tags", "{{tags}}"),
                )
            )
        ),
        PopularRequestMapping(
            type = "All",
            info = RequestInfo(
                path = "posts.json",
                params = listOf(
                    BaseEntry("page", "{{next}}"),
                    BaseEntry("limit", "{{limit}}"),
                    BaseEntry("tags", "{{tags}}"),
                ),
                illegalTags = IllegalTags(
                    startsWithTags = listOf("order:")
                ),
                defaultTags = listOf("order:score")
            )
        ),
    ),
    suggestTagInfo = RequestInfo(
        path = "tags.json",
        params = listOf(
            BaseEntry("search[order]", "count"),
            BaseEntry("search[name_or_alias_matches]", "*{{searchTagName}}*"),
            BaseEntry("limit", "{{limit}}"),
            BaseEntry("page", "{{next}}"),
        )
    ),
    searchTagInfo = RequestInfo(
        path = "tags.json",
        params = listOf(
            BaseEntry("search[order]", "count"),
            BaseEntry("search[name_or_alias_matches]", "{{searchTagName}}"),
            BaseEntry("limit", "{{limit}}"),
            BaseEntry("page", "{{next}}"),
        )
    ),
    searchUserInfo = RequestInfo(
        path = "users.json",
        params = listOf(
            BaseEntry("search[id]", "{{searchUserId}}"),
        )
    ),
    commonIllegalTags = IllegalTags(startsWithTags = listOf("rating:", "-rating:"))
)

val danbooruJsonPathRule = JsonRuleSettings(
    website = WebsiteOption.Danbooru.name,
    ratingMap = ratingMapping,
    fileTypeMap = emptyList(),
    requestInfos = requestInfos,
    postRules = postRules,
    poolRules = poolRules,
    tagRules = tagRules,
    userRules = userRules
)
