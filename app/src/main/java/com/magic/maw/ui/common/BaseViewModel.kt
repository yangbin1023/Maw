package com.magic.maw.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.entity.UserInfo
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.repository.TagRepository
import com.magic.maw.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private val TAG = "BaseViewModel2"

abstract class BaseViewModel2 : ViewModel() {
    private var scopeJob: Job? = null

    fun launch(
        context: CoroutineContext = Dispatchers.IO,
        block: suspend CoroutineScope.() -> Unit
    ) = synchronized(this) {
        scopeJob?.let { if (it.isActive) it.cancel() }
        scopeJob = viewModelScope.launch(context = context, block = block)
    }
}
