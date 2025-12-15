package com.magic.maw.data

import kotlinx.serialization.Serializable

/**
 * 网站设置
 */
@Serializable
data class WebsiteSettings(
    val ratings: List<Rating> = listOf(Rating.Safe),
    val showQuality: Quality = Quality.Sample,
    val saveQuality: Quality = Quality.File,
    val showSaveDialog: Boolean = true
)

/**
 * 视频设置
 */
@Serializable
data class VideoSettings(
    val autoplay: Boolean = true,
    val mute: Boolean = true,
    val playbackSpeedMultiplier: Float = 3f
)

/**
 * 主题设置
 */
@Serializable
data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true
)

/**
 * 应用设置
 */
@Serializable
data class AppSettings(
    val website: WebsiteOption = WebsiteOption.Yande,
    val yandeSettings: WebsiteSettings = WebsiteSettings(),
    val konachanSettings: WebsiteSettings = WebsiteSettings(),
    val danbooruSettings: WebsiteSettings = WebsiteSettings(ratings = listOf(Rating.General)),
    val websiteSettings: WebsiteSettings = yandeSettings,
    val videoSettings: VideoSettings = VideoSettings(),
    val themeSettings: ThemeSettings = ThemeSettings()
)
