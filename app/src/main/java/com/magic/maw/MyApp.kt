package com.magic.maw

import android.app.Application
import com.hjq.toast.Toaster
import com.magic.maw.data.di.appModule
import com.magic.maw.data.local.store.SettingsStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        _app = this
        SettingsStore.init(this)
        Toaster.init(this)
        startKoin {
            androidContext(this@MyApp)
            modules(appModule)
        }
    }

    companion object {
        private var _app: MyApp? = null
        val app get() = _app!!
    }
}