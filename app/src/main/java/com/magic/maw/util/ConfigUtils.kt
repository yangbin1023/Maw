package com.magic.maw.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.magic.maw.data.Quality
import com.magic.maw.data.Rating
import com.magic.maw.util.WebsiteConfig
import com.magic.maw.ui.theme.supportDynamicColor
import com.magic.maw.website.parser.DanbooruParser
import com.magic.maw.website.parser.KonachanParser
import com.magic.maw.website.parser.YandeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

val configFlow by lazy {
    createAnyFlow(
        key = "config",
        default = { Config() }
    )
}

private fun readStoreText(name: String, private: Boolean): String? {
    return if (private) {
        privateStoreFolder
    } else {
        storeFolder
    }.resolve(name).run {
        if (exists()) {
            readText()
        } else {
            null
        }
    }
}

private fun writeStoreText(name: String, text: String, private: Boolean) {
    if (private) {
        privateStoreFolder
    } else {
        storeFolder
    }.resolve(name).writeText(text)
}

private fun <T> createTextFlow(
    key: String,
    decode: (String?) -> T,
    encode: (T) -> String,
    private: Boolean = false,
): MutableStateFlow<T> {
    val name = if (key.contains('.')) {
        key
    } else {
        "$key.txt"
    }
    val initText = readStoreText(name, private)
    val initValue = decode(initText)
    val stateFlow = MutableStateFlow(initValue)
    appScope.launch {
        stateFlow.drop(1).collect {
            withContext(Dispatchers.IO) {
                writeStoreText(name, encode(it), private)
            }
        }
    }
    return stateFlow
}

private inline fun <reified T : Any> createAnyFlow(
    key: String,
    crossinline default: () -> T,
    crossinline transform: (T) -> T = { it },
    private: Boolean = false,
): MutableStateFlow<T> {
    return createTextFlow(
        key = "$key.json",
        decode = {
            val initValue = it?.let {
                runCatching { json.decodeFromString<T>(it) }.getOrNull()
            }
            transform(initValue ?: default())
        },
        encode = {
            json.encodeToString(it)
        },
        private = private,
    )
}

fun MutableStateFlow<Config>.updateWebConfig(websiteConfig: WebsiteConfig) {
    while (true) {
        val prevValue = value
        val nextValue = when (value.source) {
            YandeParser.SOURCE -> prevValue.copy(yandeConfig = websiteConfig)
            KonachanParser.SOURCE -> prevValue.copy(konachanConfig = websiteConfig)
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
    val konachanConfig: WebsiteConfig = WebsiteConfig(),
    val danbooruConfig: WebsiteConfig = WebsiteConfig(rating = Rating.General.value),
    val autoplay: Boolean = true,
    val mute: Boolean = true,
    val videoSpeedup: Float = 3f,
    val themeMode: Int = 0,
    val dynamicColor: Boolean = supportDynamicColor
) {
    val websiteConfig: WebsiteConfig
        get() = getWebsiteConfig(source)

    val darkTheme: Boolean
        @Composable
        get() = if (themeMode == 0) isSystemInDarkTheme() else themeMode == 1

    fun getWebsiteConfig(source: String): WebsiteConfig {
        return when (source) {
            YandeParser.SOURCE -> yandeConfig
            KonachanParser.SOURCE -> konachanConfig
            DanbooruParser.SOURCE -> danbooruConfig
            else -> throw RuntimeException("不支持的网站")
        }
    }
}

@Serializable
data class WebsiteConfig(
    val rating: Int = Rating.Safe.value,
    val quality: Int = Quality.Sample.value,
    val saveQuality: Int = Quality.File.value,
    val showSaveDialog: Boolean = true
)

fun Int.hasFlag(flag: Int): Boolean = (this and flag) == flag

@Suppress("unused")
fun Int.addFlags(flags: Int): Int = this or flags

@Suppress("unused")
fun Int.removeFlags(flags: Int): Int = this and flags.inv()

@Suppress("unused")
fun Int.toFlags(): IntArray {
    val list = ArrayList<Int>()
    for (i in 0 until Int.SIZE_BITS) {
        if ((this shr i).hasFlag(1)) {
            list.add(1 shl i)
        }
    }
    return list.toIntArray()
}