package com.magic.maw.ui.popular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magic.maw.data.PopularType
import com.magic.maw.ui.post.PostViewModel2
import com.magic.maw.util.configFlow
import com.magic.maw.website.PopularOption
import com.magic.maw.website.RequestOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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