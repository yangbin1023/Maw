package com.magic.maw.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class CountSingleton<T>(
    private val initValue: T,
    private val initFunc: @Composable () -> State<T>
) {
    private var count: Int = 0
    private var inited: Boolean = false
    var value: State<T> = mutableStateOf(initValue)
        private set

    private fun register(): Boolean = synchronized(this) {
        count++
        if (inited)
            return false
        inited = true
        return true
    }

    private fun unregister() = synchronized(this) {
        count--
        if (count <= 0) {
            count = 0
            inited = false
            value = mutableStateOf(initValue)
        }
    }

    @Composable
    fun OnEffect() {
        var initMethod by remember { mutableStateOf(false) }
        DisposableEffect(Unit) {
            initMethod = register()
            onDispose { unregister() }
        }
        if (initMethod) {
            value = initFunc()
            initMethod = false
        }
    }
}