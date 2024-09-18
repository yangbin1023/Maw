package com.magic.maw.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.magic.maw.data.Quality
import com.magic.maw.data.Rating
import com.magic.maw.ui.theme.supportDynamicColor
import com.magic.maw.website.parser.YandeParser
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val kv by lazy { MMKV.mmkvWithID("config") }

val configFlow = createStorageFlow("config") { Config() }

private inline fun <reified T : Any> createStorageFlow(
    key: String,
    crossinline init: () -> T
): MutableStateFlow<T> {
    val jsonStr = kv.decodeString(key, null)
    val valueStr = jsonStr?.let { JsonUtils.fromJson<T>(it) }

    val flow = MutableStateFlow(valueStr ?: init())
    MainScope().launch {
        flow.drop(1).collect {
            withContext(Dispatchers.IO) {
                kv.encode(key, JsonUtils.toJson(it))
            }
        }
    }
    return flow
}

fun MutableStateFlow<Config>.updateWebConfig(websiteConfig: WebsiteConfig) {
    while (true) {
        val prevValue = value
        val nextValue = when (value.source) {
            YandeParser.SOURCE -> prevValue.copy(yandeConfig = websiteConfig)
            else -> throw RuntimeException("不支持的网站")
        }
        if (compareAndSet(prevValue, nextValue)) {
            return
        }
    }
}

data class Config(
    val source: String = YandeParser.SOURCE,
    val yandeConfig: WebsiteConfig = WebsiteConfig(),
    val darkMode: Int = 0,
    val dynamicColor: Boolean = supportDynamicColor
) {
    val websiteConfig: WebsiteConfig
        get() = when (source) {
            YandeParser.SOURCE -> yandeConfig
            else -> throw RuntimeException("不支持的网站")
        }

    val darkTheme: Boolean
        @Composable
        get() = if (darkMode == 0) isSystemInDarkTheme() else darkMode == 1
}

data class WebsiteConfig(
    val rating: Int = Rating.Safe.value,
    val quality: Int = Quality.Sample.value
)