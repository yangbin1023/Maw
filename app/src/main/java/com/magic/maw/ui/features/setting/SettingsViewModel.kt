package com.magic.maw.ui.features.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.local.store.AppSettings
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.local.store.ThemeSettings
import com.magic.maw.data.local.store.VideoSettings
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.local.store.WebsiteSettings
import kotlinx.coroutines.launch

/**
 * 设置界面的 ViewModel
 *
 * @property repository 设置仓库
 * @property settingsState 设置状态流
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    val provider: ApiServiceProvider
) : ViewModel() {

    /**
     * 设置状态流
     */
    val settingsState = repository.appSettingsStateFlow

    /**
     * 更新 AppSettings 对象
     *
     * @param block AppSettings 对象的更新闭包
     */
    @Suppress("unused")
    fun updateAppSettings(block: AppSettings.() -> AppSettings) {
        viewModelScope.launch {
            repository.updateAppSettings(block)
        }
    }

    fun updateWebsite(website: WebsiteOption) {
        viewModelScope.launch {
            repository.updateWebsite(website)
        }
    }

    /**
     * 更新 WebsiteSettings 对象
     *
     * @param block WebsiteSettings 对象的更新闭包
     */
    fun updateWebSettings(block: WebsiteSettings.() -> WebsiteSettings) {
        viewModelScope.launch {
            repository.updateWebsiteSettings(block)
        }
    }

    fun updateVideoSettings(block: VideoSettings.() -> VideoSettings) {
        viewModelScope.launch {
            repository.updateVideoSettings(block)
        }
    }

    fun updateThemeSettings(block: ThemeSettings.() -> ThemeSettings) {
        viewModelScope.launch {
            repository.updateThemeSettings(block)
        }
    }
}
