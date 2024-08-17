package com.magic.maw.website

interface RequestCallback<T> {
    fun onResult(list: List<T>)
    fun onFailed(e: Throwable)
}