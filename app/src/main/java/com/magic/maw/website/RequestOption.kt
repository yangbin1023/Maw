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
    var poolId: Int = -1,
    var ratings: Int = Rating.None.value,
    var popularOption: PopularOption? = null,
    internal val tags: HashSet<String> = HashSet(),
) {
    fun addTag(tag: String) = apply { if (tag.isNotEmpty()) tags.add(tag) }

    fun addTags(tags: List<String>) = apply { for (item in tags) addTag(item) }

    fun clearTags() = apply { tags.clear() }
}