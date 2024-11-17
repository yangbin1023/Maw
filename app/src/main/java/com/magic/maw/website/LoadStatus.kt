package com.magic.maw.website

import java.lang.Exception

sealed class LoadStatus<out T> {
    data object Waiting : LoadStatus<Nothing>()
    data class Loading(val progress: Float = 0f) : LoadStatus<Nothing>()
    data class Error(val exception: Exception) : LoadStatus<Nothing>()
    data class Success<T>(val result: T) : LoadStatus<T>()
}

enum class LoadType {
    Waiting,
    Loading,
    Error,
    Success;
}
