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
import com.magic.maw.data.local.store.UserJsonPathRules
import com.magic.maw.data.model.constant.Rating
import com.magic.maw.data.model.constant.WebsiteOption

private val ratingMapping = listOf(
    RatingMapping(item = "s", name = Rating.Safe.name),
    RatingMapping(item = "q", name = Rating.Questionable.name),
    RatingMapping(item = "e", name = Rating.Explicit.name),
)

private val postRules = PostJsonPathRules(
    dataLenRule = JsonPathRule(ruleList = listOf("$.length()")),
    idRule = JsonPathRule(ruleList = listOf("$[%d].id"), invalidValues = listOf("")),
    creatorIdRule = JsonPathRule(ruleList = listOf("$[%d].creator_id")),
    creatorNameRule = JsonPathRule(ruleList = listOf("$[%d].author")),
    scoreRule = JsonPathRule(ruleList = listOf("$[%d].score")),
    sourceRule = JsonPathRule(ruleList = listOf("$[%d].source")),
    uploadTimeRule = JsonPathRule(ruleList = listOf("$[%d].created_at")),
    ratingRule = JsonPathRule(ruleList = listOf("$[%d].rating")),
    fileTypeRule = JsonPathRule(ruleList = listOf("$[%d].file_ext"), invalidValues = listOf("")),
    tagsRule = JsonPathRule(ruleList = listOf("$[%d].tags")),
    previewUrlRule = JsonPathRule(
        ruleList = listOf("$[%d].preview_url"),
        invalidValues = listOf("")
    ),
    previewWidthRule = JsonPathRule(ruleList = listOf("$[%d].preview_width")),
    previewHeightRule = JsonPathRule(ruleList = listOf("$[%d].preview_height")),
    sampleUrlRule = JsonPathRule(ruleList = listOf("$[%d].sample_url"), invalidValues = listOf("")),
    sampleWidthRule = JsonPathRule(ruleList = listOf("$[%d].sample_width")),
    sampleHeightRule = JsonPathRule(ruleList = listOf("$[%d].sample_height")),
    sampleSizeRule = JsonPathRule(ruleList = listOf("$[%d].sample_file_size")),
    largerUrlRule = JsonPathRule(ruleList = listOf("$[%d].jpeg_url"), invalidValues = listOf("")),
    largerWidthRule = JsonPathRule(ruleList = listOf("$[%d].jpeg_width")),
    largerHeightRule = JsonPathRule(ruleList = listOf("$[%d].jpeg_height")),
    largerSizeRule = JsonPathRule(ruleList = listOf("$[%d].jpeg_file_size")),
    originalUrlRule = JsonPathRule(ruleList = listOf("$[%d].file_url"), invalidValues = listOf("")),
    originalWidthRule = JsonPathRule(ruleList = listOf("$[%d].width")),
    originalHeightRule = JsonPathRule(ruleList = listOf("$[%d].height")),
    originalSizeRule = JsonPathRule(ruleList = listOf("$[%d].file_size")),
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

private val poolPostRules = PostJsonPathRules(
    dataLenRule = JsonPathRule(ruleList = listOf("$.posts.length()")),
    idRule = JsonPathRule(ruleList = listOf("$.posts[%d].id"), invalidValues = listOf("")),
    creatorIdRule = JsonPathRule(ruleList = listOf("$.posts[%d].creator_id")),
    creatorNameRule = JsonPathRule(ruleList = listOf("$.posts[%d].author")),
    scoreRule = JsonPathRule(ruleList = listOf("$.posts[%d].score")),
    sourceRule = JsonPathRule(ruleList = listOf("$.posts[%d].source")),
    uploadTimeRule = JsonPathRule(ruleList = listOf("$.posts[%d].created_at")),
    ratingRule = JsonPathRule(ruleList = listOf("$.posts[%d].rating")),
    tagsRule = JsonPathRule(ruleList = listOf("$.posts[%d].tags")),
    previewUrlRule = JsonPathRule(
        ruleList = listOf("$.posts[%d].preview_url"),
        invalidValues = listOf("")
    ),
    previewWidthRule = JsonPathRule(
        ruleList = listOf(
            "$.posts[%d].actual_preview_width",
            "$.posts[%d].preview_width"
        )
    ),
    previewHeightRule = JsonPathRule(
        ruleList = listOf(
            "$.posts[%d].actual_preview_height",
            "$.posts[%d].preview_height"
        )
    ),
    sampleUrlRule = JsonPathRule(
        ruleList = listOf("$.posts[%d].sample_url"),
        invalidValues = listOf("")
    ),
    sampleWidthRule = JsonPathRule(ruleList = listOf("$.posts[%d].sample_width")),
    sampleHeightRule = JsonPathRule(ruleList = listOf("$.posts[%d].sample_height")),
    sampleSizeRule = JsonPathRule(ruleList = listOf("$.posts[%d].sample_file_size")),
    largerUrlRule = JsonPathRule(
        ruleList = listOf("$.posts[%d].jpeg_url"),
        invalidValues = listOf("")
    ),
    largerWidthRule = JsonPathRule(ruleList = listOf("$.posts[%d].jpeg_width")),
    largerHeightRule = JsonPathRule(ruleList = listOf("$.posts[%d].jpeg_height")),
    largerSizeRule = JsonPathRule(ruleList = listOf("$.posts[%d].jpeg_file_size")),
    originalUrlRule = JsonPathRule(
        ruleList = listOf("$.posts[%d].file_url"),
        invalidValues = listOf("")
    ),
    originalWidthRule = JsonPathRule(ruleList = listOf("$.posts[%d].width")),
    originalHeightRule = JsonPathRule(ruleList = listOf("$.posts[%d].height")),
    originalSizeRule = JsonPathRule(ruleList = listOf("$.posts[%d].file_size")),
)

private val tagRules = TagJsonPathRules(
    dataLenRule = JsonPathRule(ruleList = listOf("$.length()")),
    idRule = JsonPathRule(ruleList = listOf("$[%d].id"), invalidValues = listOf("")),
    nameRule = JsonPathRule(ruleList = listOf("$[%d].name"), invalidValues = listOf("")),
    countRule = JsonPathRule(ruleList = listOf("$[%d].count")),
    typeRule = JsonPathRule(ruleList = listOf("$[%d].type")),
)

private val userRules = UserJsonPathRules(
    idRule = JsonPathRule(ruleList = listOf("$[0].id"), invalidValues = listOf("")),
    nameRule = JsonPathRule(ruleList = listOf("$[0].name"), invalidValues = listOf("")),
)

private val requestInfos = RequestInfos(
    baseUrl = "https://konachan.net",
    postInfo = RequestInfo(
        path = "post.json",
        params = listOf(
            BaseEntry("page", "{{next}}"),
            BaseEntry("limit", "{{limit}}"),
            BaseEntry("tags", "{{tags}}"),
        )
    ),
    poolInfo = RequestInfo(
        path = "pool.json",
        params = listOf(BaseEntry("page", "{{next}}"))
    ),
    poolPostInfo = RequestInfo(
        path = "pool/show.json",
        onlyOnePage = true,
        params = listOf(BaseEntry("id", "{{poolId}}"))
    ),
    popularMappings = listOf(
        PopularRequestMapping(
            type = "Day",
            info = RequestInfo(
                path = "post/popular_by_day.json",
                onlyOnePage = true,
                params = listOf(
                    BaseEntry("day", "{{currentDay}}"),
                    BaseEntry("month", "{{currentMonth}}"),
                    BaseEntry("year", "{{currentYear}}"),
                    BaseEntry("limit", "{{limit}}"),
                    BaseEntry("tags", "{{tags}}"),
                ),
            )
        ),
        PopularRequestMapping(
            type = "Week",
            info = RequestInfo(
                path = "post/popular_by_week.json",
                onlyOnePage = true,
                params = listOf(
                    BaseEntry("day", "{{currentDay}}"),
                    BaseEntry("month", "{{currentMonth}}"),
                    BaseEntry("year", "{{currentYear}}"),
                    BaseEntry("limit", "{{limit}}"),
                    BaseEntry("tags", "{{tags}}"),
                )
            )
        ),
        PopularRequestMapping(
            type = "Month",
            info = RequestInfo(
                path = "post/popular_by_month.json",
                onlyOnePage = true,
                params = listOf(
                    BaseEntry("day", "{{currentDay}}"),
                    BaseEntry("month", "{{currentMonth}}"),
                    BaseEntry("year", "{{currentYear}}"),
                    BaseEntry("page", "{{next}}"),
                    BaseEntry("limit", "{{limit}}"),
                    BaseEntry("tags", "{{tags}}"),
                )
            )
        ),
        PopularRequestMapping(
            type = "All",
            info = RequestInfo(
                path = "post.json",
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
        path = "tag.json",
        params = listOf(
            BaseEntry("order", "count"),
            BaseEntry("name", "{{searchTagName}}"),
            BaseEntry("limit", "{{limit}}"),
            BaseEntry("page", "{{next}}"),
        )
    ),
    searchTagInfo = RequestInfo(
        path = "tag.json",
        params = listOf(
            BaseEntry("order", "count"),
            BaseEntry("name", "{{searchTagName}}"),
            BaseEntry("limit", "{{limit}}"),
            BaseEntry("page", "{{next}}"),
        )
    ),
    searchUserInfo = RequestInfo(
        path = "user.json",
        params = listOf(
            BaseEntry("id", "{{searchUserId}}"),
        )
    ),
    commonIllegalTags = IllegalTags(startsWithTags = listOf("rating:", "-rating:"))
)

private val requestInfos2 = RequestInfos(
    baseUrl = "https://konachan.net",
    postInfo = RequestInfo(
        path = "post.json",
        params = listOf(
            BaseEntry("page", "{{next}}"),
            BaseEntry("limit", "{{limit}}"),
            BaseEntry("tags", "{{tags}}"),
        )
    ),
    poolInfo = RequestInfo(
        path = "pool.json",
        params = listOf(BaseEntry("page", "{{next}}"))
    ),
    poolPostInfo = RequestInfo(
        path = "pool/show.json",
        params = listOf(BaseEntry("id", "{{poolId}}"))
    ),
    popularMappings = listOf(
        PopularRequestMapping(
            type = "Day",
            info = RequestInfo(
                path = "post.json",
                params = listOf(
                    BaseEntry("page", "{{next}}"),
                    BaseEntry("limit", "{{limit}}"),
                    BaseEntry("tags", "{{tags}}"),
                ),
                illegalTags = IllegalTags(startsWithTags = listOf("date:", "order:")),
                defaultTags = listOf(
                    "date:{{currentDate}}",
                    "order:score"
                )
            )
        ),
        PopularRequestMapping(
            type = "Week",
            info = RequestInfo(
                path = "post.json",
                params = listOf(
                    BaseEntry("page", "{{next}}"),
                    BaseEntry("limit", "{{limit}}"),
                    BaseEntry("tags", "{{tags}}"),
                ),
                illegalTags = IllegalTags(startsWithTags = listOf("date:", "order:")),
                defaultTags = listOf(
                    "date:{{startDate}}..{{endDate}}",
                    "order:score"
                )
            )
        ),
        PopularRequestMapping(
            type = "Month",
            info = RequestInfo(
                path = "post.json",
                params = listOf(
                    BaseEntry("page", "{{next}}"),
                    BaseEntry("limit", "{{limit}}"),
                    BaseEntry("tags", "{{tags}}"),
                ),
                illegalTags = IllegalTags(startsWithTags = listOf("date:", "order:")),
                defaultTags = listOf(
                    "date:{{startDate}}..{{endDate}}",
                    "order:score"
                )
            )
        ),
        PopularRequestMapping(
            type = "Year",
            info = RequestInfo(
                path = "post.json",
                params = listOf(
                    BaseEntry("page", "{{next}}"),
                    BaseEntry("limit", "{{limit}}"),
                    BaseEntry("tags", "{{tags}}"),
                ),
                illegalTags = IllegalTags(startsWithTags = listOf("date:", "order:")),
                defaultTags = listOf(
                    "date:{{startDate}}..{{endDate}}",
                    "order:score"
                )
            )
        ),
        PopularRequestMapping(
            type = "All",
            info = RequestInfo(
                path = "post.json",
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
        path = "tag.json",
        params = listOf(
            BaseEntry("order", "count"),
            BaseEntry("name", "{{searchTagName}}"),
            BaseEntry("limit", "{{limit}}"),
            BaseEntry("page", "{{next}}"),
        )
    ),
    searchTagInfo = RequestInfo(
        path = "tag.json",
        params = listOf(
            BaseEntry("order", "count"),
            BaseEntry("name", "{{searchTagName}}"),
            BaseEntry("limit", "{{limit}}"),
            BaseEntry("page", "{{next}}"),
        )
    ),
    searchUserInfo = RequestInfo(
        path = "user.json",
        params = listOf(
            BaseEntry("id", "{{searchUserId}}"),
        )
    ),
    commonIllegalTags = IllegalTags(startsWithTags = listOf("rating:", "-rating:"))
)

val konachanJsonPathRule = JsonRuleSettings(
    website = WebsiteOption.Konachan.name,
    ratingMap = ratingMapping,
    fileTypeMap = emptyList(),
    requestInfos = requestInfos,
    postRules = postRules,
    poolRules = poolRules,
    poolPostRules = poolPostRules,
    tagRules = tagRules,
    userRules = userRules
)

val yandeJsonPathRule = JsonRuleSettings(
    website = WebsiteOption.Yande.name,
    ratingMap = ratingMapping,
    fileTypeMap = emptyList(),
    requestInfos = requestInfos2.copy(baseUrl = "https://yande.re"),
    postRules = postRules,
    poolRules = poolRules,
    poolPostRules = poolPostRules,
    tagRules = tagRules,
    userRules = userRules
)
