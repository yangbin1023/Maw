package com.magic.maw.ui.features.setting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

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

/**
 * SettingsViewModel 的工厂类
 *
 * @property repository 设置仓库
 */
class SettingsViewModelFactory(
    private val repository: SettingsRepository
) : ViewModelProvider.Factory {

    /**
     * 创建 ViewModel 实例
     *
     * @param modelClass ViewModel 类
     * @return 创建的 ViewModel 实例
     * @throws IllegalArgumentException 如果不支持创建指定类型的 ViewModel
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * 创建 SettingsViewModel 实例
 *
 * @return SettingsViewModel 实例
 */
@Composable
fun settingsViewModel(): SettingsViewModel {
    // 获取上下文对象
    val context = LocalContext.current
    // 创建设置仓库
    val repository = remember { SettingsRepository(context.applicationContext) }
    // 创建 SettingsViewModel 工厂
    val factory = remember { SettingsViewModelFactory(repository) }
    // 创建 SettingsViewModel 实例
    return viewModel(factory = factory)
}