package com.magic.maw.website

data class RequestOption(
    var page: Int = 1,
    internal val tags: HashSet<String> = HashSet(),
) {
    fun addTag(tag: String) = apply { if (tag.isNotEmpty()) tags.add(tag) }

    fun addTags(tags: List<String>) = apply { for (item in tags) addTag(item) }

    fun clearTags() = apply { tags.clear() }
}