package com.magic.maw.util

import com.magic.maw.common.EventMessage
import org.greenrobot.eventbus.EventBus

object BusUtils {
    fun register(obj: Any) {
        if (!EventBus.getDefault().isRegistered(obj)) {
            EventBus.getDefault().register(obj)
        }
    }

    fun unregister(obj: Any) {
        if (EventBus.getDefault().isRegistered(obj)) {
            EventBus.getDefault().unregister(obj)
        }
    }

    fun post(type: EventMessage.Type, arg: Any? = null) {
        EventBus.getDefault().post(EventMessage(type, arg))
    }
}