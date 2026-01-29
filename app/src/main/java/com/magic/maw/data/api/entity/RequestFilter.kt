package com.magic.maw.data.api.entity

import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.model.constant.Rating
import java.time.LocalDate

data class PopularOption(
    val type: PopularType = PopularType.Day,
    val date: LocalDate = LocalDate.now()
)

data class RequestFilter(
    val poolId: String? = null,
    val ratings: List<Rating> = emptyList(),
    val popularOption: PopularOption? = null,
    val tags: Set<String> = emptySet(),
)
