package com.magic.maw.data.model.constant

import kotlinx.serialization.Serializable

@Serializable
enum class DLState(val value: Int) {
    None(0),
    Waiting(1),
    Downloading(2),
    Pause(3),
    Failed(4),
    Finished(5);

    companion object {
        fun Int?.toDLState(): DLState {
            for (item in entries) {
                if (item.value == this)
                    return item
            }
            return None
        }
    }
}