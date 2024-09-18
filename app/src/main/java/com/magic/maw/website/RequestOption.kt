package com.magic.maw.website

import com.magic.maw.data.Rating

data class RequestOption(
    var page: Int = 1,
    var ratings: Int = Rating.None.value,
    internal val tags: HashSet<String> = HashSet(),
) {
    fun addTag(tag: String) = apply { if (tag.isNotEmpty()) tags.add(tag) }

    fun addTags(tags: List<String>) = apply { for (item in tags) addTag(item) }

    fun clearTags() = apply { tags.clear() }
}