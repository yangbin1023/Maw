package com.magic.maw.ui.popular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.PopularType
import com.magic.maw.data.SettingsService
import com.magic.maw.data.WebsiteOption
import com.magic.maw.data.loader.PostDataLoader
import com.magic.maw.ui.post.PostViewModel2
import com.magic.maw.util.configFlow
import com.magic.maw.website.PopularOption
import com.magic.maw.website.RequestOption
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class PopularViewModel : ViewModel() {
    private val viewModelState = MutableStateFlow(
        PopularOption(date = LocalDate.now().minusDays(1))
    )
    val postViewModel2: PostViewModel2 by lazy {
        val requestOption = RequestOption(
            ratings = configFlow.value.websiteConfig.rating,
            popularOption = viewModelState.value
        )
        PostViewModel2(requestOption)
    }

    val uiState = viewModelState.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        viewModelState.value
    )

    fun update(date: LocalDate) {
        viewModelState.update { it.copy(date = date) }
    }

    fun update(popularType: PopularType) {
        viewModelState.update { it.copy(type = popularType) }
    }

    fun clearData() {
        postViewModel2.clearData()
    }
}

class PopularViewModel2 : ViewModel() {
    private var _website: WebsiteOption = SettingsService.settingsState.value.website
    private var parser = BaseParser.get(_website)
    private var scopeJob: Job? = null
    private var supportedPopularDateTypes = parser.supportedPopularDateTypes
    private val loaderMap: MutableMap<PopularType, PostDataLoader> = mutableMapOf()
    private var currentPopularType: PopularType = PopularType.Day
    private var _currentLoader = MutableStateFlow(value = getLoader(type = PopularType.Day))
    val website get() = _website

    val currentLoader = _currentLoader.asStateFlow()

    init {
        viewModelScope.launch {
            SettingsService.settingsState.collect { settingsState ->
                if (settingsState.website != _website) {
                    _website = settingsState.website
                    parser = BaseParser.get(_website)
                    supportedPopularDateTypes = parser.supportedPopularDateTypes
                }
            }
        }
    }

    fun setCurrentPopularType(type: PopularType) {
        _currentLoader.update {
            getLoader(type)
        }
    }

    fun getLoader(type: PopularType): PostDataLoader {
        synchronized(this) {
            return loaderMap[type] ?: let {
                val loader = PostDataLoader(
                    scope = viewModelScope,
                    popularOption = PopularOption(
                        type = type,
                        date = getDefaultPopularDate()
                    )
                )
                loaderMap[type] = loader
                loader
            }
        }
    }

    fun getDefaultPopularDate(): LocalDate {
        return if (_website == WebsiteOption.Konachan) {
            LocalDate.now().minusDays(1)
        } else {
            LocalDate.now()
        }
    }
}