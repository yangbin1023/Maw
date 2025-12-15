package com.magic.maw.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream


/**
 * AppSettings 的数据存储器
 */
object AppSettingsSerializer : Serializer<AppSettings> {
    override val defaultValue: AppSettings = AppSettings()

    /**
     * 从输入流中读取数据
     *
     * @param input 输入流
     * @return AppSettings 对象
     */
    override suspend fun readFrom(input: InputStream): AppSettings {
        return try {
            // 使用 Kotlinx Json 解析输入流中的数据
            Json.decodeFromString(
                deserializer = AppSettings.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (exception: Exception) {
            // 如果解析失败，返回默认值
            exception.printStackTrace()
            defaultValue
        }
    }

    /**
     * 将对象写入输出流中
     *
     * @param t AppSettings 对象
     * @param output 输出流
     */
    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        // 使用 Kotlinx Json 将对象序列化为 JSON 字符串并写入输出流
        output.write(
            Json.encodeToString(
                serializer = AppSettings.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }
}

private val Context.appSettingsDataStore: DataStore<AppSettings> by dataStore(
    fileName = "app_settings.json",
    serializer = AppSettingsSerializer
)

/**
 * 设置仓库类，用于获取和更新 AppSettings 对象
 */
class SettingsRepository(context: Context) {
    private val dataStore: DataStore<AppSettings> = context.appSettingsDataStore

    /**
     * 获取 AppSettings 对象的流
     */
    val appSettingsFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(AppSettingsSerializer.defaultValue)
            } else {
                throw exception
            }
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Eagerly,
            initialValue = AppSettingsSerializer.defaultValue
        )

    /**
     * 更新 AppSettings 对象
     *
     * @param block AppSettings 对象的更新闭包
     */
    suspend fun updateAppSettings(block: AppSettings.() -> AppSettings) {
        dataStore.updateData(block)
    }

    /**
     * 更新 WebsiteSettings 对象
     *
     * @param block WebsiteSettings 对象的更新闭包
     */
    suspend fun updateWebsiteSettings(block: WebsiteSettings.() -> WebsiteSettings) {
        dataStore.updateData {
            val websiteSettings = block(it.websiteSettings)
            when (it.website) {
                WebsiteOption.Yande -> it.copy(
                    websiteSettings = websiteSettings,
                    yandeSettings = websiteSettings
                )

                WebsiteOption.Konachan -> it.copy(
                    websiteSettings = websiteSettings,
                    konachanSettings = websiteSettings
                )

                WebsiteOption.Danbooru -> it.copy(
                    websiteSettings = websiteSettings,
                    danbooruSettings = websiteSettings
                )
            }
        }
    }

    /**
     * 更新指定 Website 的 WebsiteSettings 对象
     *
     * @param website 网站选项
     * @param block WebsiteSettings 对象的更新闭包
     */
    suspend fun updateWebsiteSettings(
        website: WebsiteOption,
        block: WebsiteSettings.() -> WebsiteSettings
    ) {
        dataStore.updateData {
            when (website) {
                WebsiteOption.Yande -> it.copy(yandeSettings = block(it.websiteSettings))
                WebsiteOption.Konachan -> it.copy(konachanSettings = block(it.websiteSettings))
                WebsiteOption.Danbooru -> it.copy(danbooruSettings = block(it.websiteSettings))
            }
        }
    }

    /**
     * 更新网站选项
     *
     * @param website 网站选项
     */
    suspend fun updateWebsite(website: WebsiteOption) {
        dataStore.updateData {
            when (website) {
                WebsiteOption.Yande -> it.copy(
                    website = website,
                    websiteSettings = it.yandeSettings
                )

                WebsiteOption.Konachan -> it.copy(
                    website = website,
                    websiteSettings = it.konachanSettings
                )

                WebsiteOption.Danbooru -> it.copy(
                    website = website,
                    websiteSettings = it.danbooruSettings
                )
            }
        }
    }

    /**
     * 更新 VideoSettings 对象
     *
     * @param block VideoSettings 对象的更新闭包
     */
    suspend fun updateVideoSettings(block: VideoSettings.() -> VideoSettings) {
        dataStore.updateData {
            it.copy(videoSettings = block(it.videoSettings))
        }
    }

    /**
     * 更新 ThemeSettings 对象
     *
     * @param block ThemeSettings 对象的更新闭包
     */
    suspend fun updateThemeSettings(block: ThemeSettings.() -> ThemeSettings) {
        dataStore.updateData {
            it.copy(themeSettings = block(it.themeSettings))
        }
    }
}

/**
 * 设置服务类，用于获取全局的 AppSettings 对象流
 */
object SettingsService {
    private lateinit var repository: SettingsRepository

    private lateinit var appScope: CoroutineScope

    private lateinit var _settingsState: StateFlow<AppSettings>

    /**
     * 获取 AppSettings 对象流
     */
    val settingsState: StateFlow<AppSettings> get() = _settingsState

    /**
     * 初始化 SettingsService
     *
     * @param context 上下文对象
     * @param appScope 协程作用域（默认为 MainScope）
     */
    fun init(context: Context, appScope: CoroutineScope = MainScope()) {
        if (::repository.isInitialized) return
        repository = SettingsRepository(context.applicationContext)
        this.appScope = appScope
        _settingsState = repository.appSettingsFlow
    }
}