package com.magic.maw.website

import com.magic.maw.data.PopularType
import com.magic.maw.data.Rating
import java.time.LocalDate

data class PopularOption(
    val type: PopularType = PopularType.Day,
    val date: LocalDate = LocalDate.now()
)

data class RequestOption(
    var page: Int = 1,
    var poolId: Int? = null,
    var ratingFlag: Int = Rating.None.value,
    val ratings: List<Rating> = emptyList(),
    var popularOption: PopularOption? = null,
    val tags: Set<String> = emptySet(),
) {
    override fun toString(): String {
        return "{page:$page,poolId:$poolId,ratingFlag:$ratingFlag,ratings:$ratings,popularOption:$popularOption,tags:$tags}"
    }
}