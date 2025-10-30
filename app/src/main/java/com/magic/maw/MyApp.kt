package com.magic.maw

import android.app.Application
import coil.Coil
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.GifDecoder
import com.hjq.toast.Toaster

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        _app = this
        Toaster.init(this)
        Coil.setImageLoader {
            ImageLoader.Builder(this)
                .components(
                    ComponentRegistry.Builder()
                        .add(GifDecoder.Factory())
                        .build()
                )
                .build()
        }
    }

    companion object {
        private var _app: MyApp? = null
        val app get() = _app!!
    }
}