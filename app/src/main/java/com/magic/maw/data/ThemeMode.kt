package com.magic.maw.data

import com.magic.maw.R

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