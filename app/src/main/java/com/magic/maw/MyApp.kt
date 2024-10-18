package com.magic.maw

import android.app.Activity
import android.app.Application
import com.hjq.toast.Toaster
import com.magic.maw.website.parser.BaseParser
import com.magic.maw.website.parser.YandeParser
import com.tencent.mmkv.MMKV
import java.util.Stack

class MyApp : Application() {
    private val activityStack = Stack<Activity>()
    private var _parser: BaseParser? = null
    val parser: BaseParser get() = _parser!!

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        _app = this
        _parser = BaseParser.get(YandeParser.SOURCE)
        MMKV.initialize(this)
        Toaster.init(this)
    }

    fun closeAllActivity() {
        for (activity in activityStack) {
            activity.finish()
        }
    }

    fun pushActivity(activity: Activity) {
        activityStack.push(activity)
    }

    fun removeActivity(activity: Activity) {
        activityStack.remove(activity)
    }

    fun peekActivity(): Activity? {
        return if (activityStack.empty()) null else activityStack.peek()
    }

    companion object {
        private var _app: MyApp? = null
        val app get() = _app!!
    }
}