package com.magic.maw.data.api.entity

import com.magic.maw.data.model.PopularOption
import com.magic.maw.data.model.constant.Rating

data class RequestFilter(
    val poolId: String? = null,
    val ratings: List<Rating> = emptyList(),
    val popularOption: PopularOption? = null,
    val tags: Set<String> = emptySet(),
)
