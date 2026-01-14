package com.magic.maw.data.model.constant

import kotlinx.serialization.Serializable

@Serializable
enum class WebsiteOption {
    Yande,
    Konachan,
    Danbooru;

    val label: String get() = name.uppercase()
}