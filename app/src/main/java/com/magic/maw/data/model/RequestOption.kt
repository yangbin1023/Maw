package com.magic.maw.data.model

import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.model.constant.Rating
import java.time.LocalDate

data class PopularOption(
    val type: PopularType = PopularType.Day,
    val date: LocalDate = LocalDate.now()
)

data class RequestOption(
    var page: Int = 1,
    var poolId: String? = null,
    var ratingFlag: Int = Rating.None.value,
    val ratings: List<Rating> = emptyList(),
    var popularOption: PopularOption? = null,
    val tags: Set<String> = emptySet(),
) {
    override fun toString(): String {
        return "{page:$page,poolId:$poolId,ratingFlag:$ratingFlag,ratings:$ratings,popularOption:$popularOption,tags:$tags}"
    }
}