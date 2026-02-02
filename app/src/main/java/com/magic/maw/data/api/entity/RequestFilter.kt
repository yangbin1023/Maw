package com.magic.maw.data.api.entity

import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.model.constant.Rating
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class PopularOption(
    val type: PopularType = PopularType.Day,
    val date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
)

data class RequestFilter(
    val poolId: String? = null,
    val ratings: List<Rating> = emptyList(),
    val popularOption: PopularOption? = null,
    val tags: Set<String> = emptySet(),
)
