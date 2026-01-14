package com.magic.maw.data.model.constant

import com.magic.maw.R
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    System,
    Light,
    Dark;

    val resId: Int
        get() = when (this) {
            System -> R.string.follow_system
            Light -> R.string.always_light
            Dark -> R.string.always_dark
        }
}