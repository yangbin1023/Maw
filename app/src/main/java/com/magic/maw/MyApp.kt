package com.magic.maw

import android.app.Application
import com.hjq.toast.Toaster
import com.tencent.mmkv.MMKV

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        _app = this
        MMKV.initialize(this)
        Toaster.init(this)
    }

    companion object {
        private var _app: MyApp? = null
        val app get() = _app!!
    }
}