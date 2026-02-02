package com.magic.maw.ui.features.popular

import androidx.collection.MutableIntIntMap
import androidx.collection.mutableIntIntMapOf
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.api.entity.PopularOption
import com.magic.maw.data.api.entity.RequestFilter
import com.magic.maw.data.api.service.ApiServiceProvider
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.repository.PostDataSource
import com.magic.maw.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

class PopularDataItem(
    postRepository: PostRepository,
    val popularType: PopularType,
    val lazyState: LazyStaggeredGridState = LazyStaggeredGridState(0, 0),
    val itemHeights: MutableIntIntMap = mutableIntIntMapOf(),
) {
    private val _localDateFlow = MutableStateFlow(
        Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(
            DatePeriod(days = 1)
        )
    )

    val localDateFlow: StateFlow<LocalDate> = _localDateFlow.asStateFlow()

    val dataSource: PostDataSource

    init {
        val option = PopularOption(type = popularType, date = _localDateFlow.value)
        val filter = RequestFilter(popularOption = option)
        dataSource = postRepository.getPostDataSource(filter = filter)
    }

    fun setPopularDate(date: LocalDate) = synchronized(this) {
        _localDateFlow.update { date }
        val option = PopularOption(type = popularType, date = date)
        val filter = RequestFilter(popularOption = option)
        dataSource.setFilter(filter = filter)
    }
}

class PopularViewModel(
    settingsRepository: SettingsRepository,
    private val apiServiceProvider: ApiServiceProvider,
    private val postRepository: PostRepository,
) : ViewModel() {
    private val itemDataMap: MutableMap<PopularType, PopularDataItem> = mutableMapOf()
    private var _currentItemData = MutableStateFlow(value = getItemData(type = PopularType.Day))

    val currentPopularTypes: StateFlow<List<PopularType>> = settingsRepository.appSettingsStateFlow
        .map { apiServiceProvider[it.website].supportedPopularDateTypes }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            apiServiceProvider[settingsRepository.settings.website].supportedPopularDateTypes
        )

    val currentData = _currentItemData.asStateFlow()

    fun checkAndRefresh() {
        itemDataMap.forEach { (_, source) ->
            source.dataSource.checkAndRefresh()
        }
    }

    fun setCurrentPopularType(type: PopularType) {
        _currentItemData.update {
            getItemData(type)
        }
    }

    fun getItemData(type: PopularType): PopularDataItem {
        synchronized(this) {
            return itemDataMap[type] ?: let {
                PopularDataItem(
                    postRepository = postRepository,
                    popularType = type
                ).apply {
                    itemDataMap[type] = this
                }
            }
        }
    }

    fun itemScrollToTop() {
        synchronized(this) {
            viewModelScope.launch {
                itemDataMap.forEach { (_, source) ->
                    source.lazyState.scrollToItem(0, 0)
                }
            }
        }
    }
}