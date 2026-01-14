package com.magic.maw.ui.features.pool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.api.loader.PoolDataLoader
import com.magic.maw.data.api.loader.PostDataLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PoolViewModel : ViewModel() {
    private val _postLoader = MutableStateFlow<PostDataLoader?>(null)

    val loader = PoolDataLoader(scope = viewModelScope)

    val postLoader = _postLoader.asStateFlow()

    fun setViewPoolPost(poolId: String) {
        _postLoader.update {
            PostDataLoader(scope = viewModelScope, poolId = poolId)
        }
    }

    fun refresh(force: Boolean = false) {
        loader.refresh(force)
    }

    fun loadMore() {
        loader.loadMore()
    }
}