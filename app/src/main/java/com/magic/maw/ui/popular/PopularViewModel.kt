package com.magic.maw.ui.popular

import androidx.collection.MutableIntIntMap
import androidx.collection.mutableIntIntMapOf
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.PopularType
import com.magic.maw.data.SettingsService
import com.magic.maw.data.WebsiteOption
import com.magic.maw.data.loader.PostDataLoader
import com.magic.maw.website.PopularOption
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PopularItemData(
    val loader: PostDataLoader,
    private val _localDateFlow: MutableStateFlow<LocalDate> = MutableStateFlow(
        LocalDate.now().minusDays(1)
    ),
    val localDateFlow: StateFlow<LocalDate> = _localDateFlow.asStateFlow(),
    val lazyState: LazyStaggeredGridState = LazyStaggeredGridState(0, 0),
    val itemHeights: MutableIntIntMap = mutableIntIntMapOf(),
) {
    fun setPopularDate(date: LocalDate) = synchronized(this) {
        _localDateFlow.update { date }
        loader.setPopularDate(date)
    }
}

class PopularViewModel : ViewModel() {
    private var website: WebsiteOption = SettingsService.settings.website
    private var parser = BaseParser.get(website)
    private val itemDataMap: MutableMap<PopularType, PopularItemData> = mutableMapOf()
    private var _currentItemData = MutableStateFlow(value = getItemData(type = PopularType.Day))

    val currentData = _currentItemData.asStateFlow()

    init {
        viewModelScope.launch {
            SettingsService.settingsState.collect { settingsState ->
                if (settingsState.website != website) {
                    website = settingsState.website
                    parser = BaseParser.get(website)
                    itemDataMap.clear()
                    _currentItemData.update {
                        getItemData(type = PopularType.Day)
                    }
                }
            }
        }
    }

    fun setCurrentPopularType(type: PopularType) {
        _currentItemData.update {
            getItemData(type)
        }
    }

    fun getItemData(type: PopularType): PopularItemData {
        synchronized(this) {
            return itemDataMap[type] ?: let {
                val data = PopularItemData(
                    loader = PostDataLoader(
                        scope = viewModelScope,
                        popularOption = PopularOption(
                            type = type,
                            date = LocalDate.now().minusDays(1)
                        )
                    )
                )
                itemDataMap[type] = data
                data
            }
        }
    }

    fun itemScrollToTop() {
        synchronized(this) {
            for ((_, v) in itemDataMap) {
                viewModelScope.launch {
                    v.lazyState.scrollToItem(0, 0)
                }
            }
        }
    }
}