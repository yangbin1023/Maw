package com.magic.maw.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class BaseViewModel : ViewModel() {
    private var scopeJob: Job? = null

    fun launch(
        context: CoroutineContext = Dispatchers.IO,
        block: suspend CoroutineScope.() -> Unit
    ) = synchronized(this) {
        scopeJob?.let { if (it.isActive) it.cancel() }
        scopeJob = viewModelScope.launch(context = context, block = block)
    }
}