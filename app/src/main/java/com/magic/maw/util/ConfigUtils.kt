package com.magic.maw.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.magic.maw.data.Quality
import com.magic.maw.data.Rating
import com.magic.maw.ui.theme.supportDynamicColor
import com.magic.maw.website.parser.DanbooruParser
import com.magic.maw.website.parser.YandeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

val configFlow = createStorageFlow("config") { Config() }

private inline fun <reified T : Any> createStorageFlow(
    key: String,
    crossinline init: () -> T
): MutableStateFlow<T> {
    val jsonStr = kv.decodeString(key, null)
    val valueStr = jsonStr?.let { json.decodeFromString<T>(jsonStr) }

    val flow = MutableStateFlow(valueStr ?: init())
    MainScope().launch {
        flow.drop(1).collect {
            withContext(Dispatchers.IO) {
                kv.encode(key, json.encodeToString(it))
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
            DanbooruParser.SOURCE -> prevValue.copy(danbooruConfig = websiteConfig)
            else -> throw RuntimeException("不支持的网站")
        }
        if (compareAndSet(prevValue, nextValue)) {
            return
        }
    }
}

@Serializable
data class Config(
    val source: String = YandeParser.SOURCE,
    val yandeConfig: WebsiteConfig = WebsiteConfig(),
    val danbooruConfig: WebsiteConfig = WebsiteConfig(rating = Rating.General.value),
    val darkMode: Int = 0,
    val dynamicColor: Boolean = supportDynamicColor
) {
    val websiteConfig: WebsiteConfig
        get() = getWebsiteConfig(source)

    val darkTheme: Boolean
        @Composable
        get() = if (darkMode == 0) isSystemInDarkTheme() else darkMode == 1

    fun getWebsiteConfig(source: String): WebsiteConfig {
        return when (source) {
            YandeParser.SOURCE -> yandeConfig
            DanbooruParser.SOURCE -> danbooruConfig
            else -> throw RuntimeException("不支持的网站")
        }
    }
}

@Serializable
data class WebsiteConfig(
    val rating: Int = Rating.Safe.value,
    val quality: Int = Quality.Sample.value
)

fun Int.hasFlag(flag: Int): Boolean = (this and flag) == flag
fun Int.addFlags(flags: Int): Int = this or flags
fun Int.removeFlags(flags: Int): Int = this and flags.inv()
fun Int.toFlags(): IntArray {
    val list = ArrayList<Int>()
    for (i in 0 until Int.SIZE_BITS) {
        if ((this shr i).hasFlag(1)) {
            list.add(1 shl i)
        }
    }
    return list.toIntArray()
}