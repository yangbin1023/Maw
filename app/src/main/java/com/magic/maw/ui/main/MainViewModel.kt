package com.magic.maw.ui.main

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.NavController
import com.magic.maw.MyApp

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val app: MyApp = application as MyApp

    var gesturesEnabled: Boolean by mutableStateOf(true)
}

object MainRoutes {
    const val POST = "post"
    const val POOL = "pool"
    const val POPULAR = "popular"
    const val SETTING = "setting"
    const val SEARCH = "search?text={text}"
    const val VERIFY = "verify?url={url}&source={source}"

    fun post(tagText: String = ""): String = POST.replace("{tagText}", tagText)
    fun search(text: String = ""): String = SEARCH.replace("{text}", text)
    fun verify(url: String = "", source: String = ""): String =
        VERIFY.replace("{url}", url).replace("{source}", source)

    fun isMainView(route: String?): Boolean {
        return when (route) {
            POST, POOL, POPULAR -> true
            else -> false
        }
    }
}

fun NavController.onNavigate(route: String, vararg args: Any) {
    val targetRoute = getFormatRoute(route, *args)
    navigate(targetRoute) {
        popUpTo(route) {
            inclusive = true
            saveState = true
        }
    }
}

private fun getFormatRoute(format: String, vararg args: Any): String {
    val strList = ArrayList<String>()
    var itemStr = format
    do {
        val startIndex = itemStr.indexOf("{")
        val endIndex = itemStr.indexOf("}")
        if (startIndex < 0 || endIndex < 0 || startIndex > endIndex)
            break
        strList.add(itemStr.substring(startIndex, endIndex + 1))
        itemStr = itemStr.substring(endIndex + 1)
    } while (true)
    var str = format
    for (i in 0 until strList.size) {
        val newValue = if (args.size > i) args[i].toString() else ""
        str = str.replaceFirst(strList[i], newValue)
    }
    return str
}