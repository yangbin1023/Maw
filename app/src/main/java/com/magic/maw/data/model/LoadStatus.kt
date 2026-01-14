package com.magic.maw.data.model

sealed class LoadStatus<out T> {
    data object Waiting : LoadStatus<Nothing>()
    data class Loading(val progress: Float) : LoadStatus<Nothing>()
    data class Error(val exception: Exception) : LoadStatus<Nothing>()
    data class Success<T>(val result: T) : LoadStatus<T>()
}

enum class LoadType {
    Waiting,
    Loading,
    Error,
    Success;
}

fun LoadStatus<*>.toLoadType(): LoadType {
    return if (this == LoadStatus.Waiting) {
        LoadType.Waiting
    } else if (this is LoadStatus.Loading) {
        LoadType.Loading
    } else if (this is LoadStatus.Error) {
        LoadType.Error
    } else {
        LoadType.Success
    }
}