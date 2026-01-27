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

abstract class BaseDataViewModel(
    protected val tagRepository: TagRepository,
    protected val userRepository: UserRepository,
) : ViewModel() {
    private val _currentUserId = MutableStateFlow<UserInfo?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val userFlow: StateFlow<UserInfo?> = _currentUserId
        .flatMapLatest { info ->
            if (info == null) flowOf(null)
            else userRepository.getUserInfoFlow(info.website, info.userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun refreshUserInfo(website: WebsiteOption, userId: String?) {
        if (userId == null) {
            _currentUserId.value = null
        } else {
            _currentUserId.value = UserInfo(website = website, userId = userId)
            viewModelScope.launch {
                userRepository.refreshUserInfo(website, userId)
            }
        }
    }

    private val _currentPostData = MutableStateFlow<PostData?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val tagsFlow: StateFlow<List<TagInfo>> = _currentPostData
        .flatMapLatest { data ->
            if (data == null) flowOf(emptyList())
            else tagRepository.getTagsFlowForPost(data).map { list ->
                list.sortedWith(
                    compareBy<TagInfo> { it.type.priority }
                        .thenBy { it.name }
                )
            }.flowOn(Dispatchers.Default)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refreshTagsForPost(data: PostData?) {
        _currentPostData.value = data
        if (data == null) return
        viewModelScope.launch {
            tagRepository.refreshTagsForPost(data)
        }
    }

    suspend fun getTagsForPost(data: PostData): List<TagInfo> {
        return tagRepository.getTagsForPost(data)
    }

    suspend fun getUserInfo(data: PostData): UserInfo? {
        data.createId?.let { userId ->
            return userRepository.getUserInfo(data.website, userId)
        }
        return null
    }

    abstract fun checkAndRefresh()
}

val LocalDataViewModel = staticCompositionLocalOf<BaseDataViewModel> {
    error("not LocalDataViewModel provided")
}