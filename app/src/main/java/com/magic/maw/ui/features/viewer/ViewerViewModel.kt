package com.magic.maw.ui.features.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.model.entity.TagInfo
import com.magic.maw.data.model.entity.UserInfo
import com.magic.maw.data.model.site.PostData
import com.magic.maw.data.repository.TagRepository
import com.magic.maw.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ViewerViewModel(
    private val tagRepository: TagRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _currentPostData = MutableStateFlow<PostData?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val userFlow: StateFlow<UserInfo?> = _currentPostData
        .flatMapLatest { data ->
            val userId = data?.createId
            if (userId == null) flowOf(null)
            else userRepository.getUserInfoFlow(data.website.name, userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val tagsFlow: StateFlow<List<TagInfo>> = _currentPostData
        .flatMapLatest { data ->
            if (data == null) flowOf(emptyList())
            else tagRepository.getTagsFlowForPost(data).map { list ->
                list.distinctBy { it.name }
                    .sortedWith(
                        compareBy<TagInfo> { it.type.priority }
                            .thenBy { it.name }
                    )
            }.flowOn(Dispatchers.IO)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refreshPostData(data: PostData?) {
        val oldData = _currentPostData.value
        _currentPostData.value = data
        if (data == null) return
        if (oldData != data) {
            viewModelScope.launch(Dispatchers.IO) {
                data.createId?.let { userId ->
                    userRepository.refreshUserInfo(data.website.name, userId)
                }
                tagRepository.refreshTagsForPost(data)
            }
        }
    }
}