package com.magic.maw.common

data class EventMessage(val type: Type, val obj: Any? = null) {
    enum class Type {
        UpdateViewPosition,
    }
}