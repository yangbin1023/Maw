package com.magic.maw.ui.features.popular

import androidx.collection.MutableIntIntMap
import androidx.collection.mutableIntIntMapOf
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.magic.maw.data.api.loader.ListUiState
import com.magic.maw.data.api.loader.LoadState
import com.magic.maw.data.api.loader.PopularDataLoader
import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.data.local.store.SettingsStore
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.api.loader.PostDataManager
import com.magic.maw.data.model.PopularOption
import com.magic.maw.data.api.parser.BaseParser
import com.magic.maw.data.local.store.SettingsRepository
import com.magic.maw.data.model.RequestOption
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.repository.PostRepository
import com.magic.maw.ui.common.BaseViewModel2
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.collections.iterator

private const val TAG = "PopularViewModel"

data class PopularItemData(
    val loader: PopularDataLoader,
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

class PopularItem(
    popularOption: PopularOption,
    private val viewModel: BaseViewModel2,
    private val postRepository: PostRepository,
    private val settingsRepository: SettingsRepository
) : PopularDataLoader {
    private var website: WebsiteOption
    private var requestOption: RequestOption = RequestOption()
    private val dataManager = PostDataManager()
    override val uiState: StateFlow<ListUiState<PostData>> get() = dataManager.uiState

    init {
        val settings = settingsRepository.settings
        val ratings = settings.websiteSettings.ratings
        website = settings.website
        requestOption = RequestOption(ratings = ratings, popularOption = popularOption)
        requestOption = postRepository.getRequestOption(website, requestOption, true)
        refresh()
    }

    override fun checkAndRefresh() {
    }

    override fun refresh(force: Boolean) = viewModel.run {
        Logger.d(TAG) { "refresh called. force: $force, tags: ${requestOption.tags}" }
        if (uiState.value.loadState == LoadState.Refreshing) return@run
        dataManager.updateState(LoadState.Refreshing)
        launch {
            try {
                val newOption = postRepository.getRequestOption(website, requestOption, true)
                Logger.d(TAG) { "ratings: ${newOption.ratings}, tags: ${newOption.tags}" }
                val list = postRepository.requestPostData(website, newOption)
                if (force) {
                    dataManager.replaceData(list)
                } else {
                    dataManager.appendData(list, false)
                }
                requestOption = newOption
            } catch (_: CancellationException) {
                Logger.d(TAG) { "refresh canceled." }
            } catch (e: Exception) {
                Logger.d(TAG) { "post loader refresh failed. ${e.message}" }
                dataManager.updateState(LoadState.Error(message = e.message))
            }
        }
    }

    override fun loadMore() = viewModel.run {
        Logger.d(TAG) { "loadMore called." }
        if (uiState.value.isLoading || uiState.value.hasNoMore) return@run
        dataManager.updateState(LoadState.LoadingMore)
        launch {
            try {
                val newOption = postRepository.getRequestOption(website, requestOption)
                val list = postRepository.requestPostData(website, newOption)
                if (uiState.value.loadState == LoadState.LoadingMore) {
                    dataManager.appendData(list)
                    requestOption = newOption
                }
            } catch (e: Exception) {
                Logger.d(TAG) { "post loader load more failed. ${e.message}" }
                dataManager.updateState(LoadState.Error(message = e.message))
            }
        }
    }

    override fun clearData() = dataManager.clearData()

    override fun setPopularDate(date: LocalDate) {
        requestOption.popularOption?.let {
            requestOption = requestOption.copy(popularOption = it.copy(date = date))
            refresh(true)
        }
    }
}

class PopularViewModel(
    private val postRepository: PostRepository,
    private val settingsRepository: SettingsRepository,
) : BaseViewModel2() {
    private var website: WebsiteOption = SettingsStore.settings.website
    private var parser = BaseParser.get(website)
    private val itemDataMap: MutableMap<PopularType, PopularItemData> = mutableMapOf()
    private var _currentItemData = MutableStateFlow(value = getItemData(type = PopularType.Day))

    val currentData = _currentItemData.asStateFlow()

    init {
        viewModelScope.launch {
            SettingsStore.settingsState.collect { settingsState ->
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
                val loader = PopularItem(
                    popularOption = PopularOption(
                        type = type,
                        date = LocalDate.now().minusDays(1)
                    ),
                    viewModel = this,
                    postRepository = postRepository,
                    settingsRepository = settingsRepository,
                )
                val data = PopularItemData(loader = loader)
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