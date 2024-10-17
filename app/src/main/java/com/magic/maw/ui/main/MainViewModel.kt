package com.magic.maw.ui.main

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.magic.maw.MyApp
import com.magic.maw.website.parser.BaseParser

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val app: MyApp = application as MyApp
    val parser: BaseParser = app.parser

    var gesturesEnabled: Boolean by mutableStateOf(true)
}

object MainRoutes {
    const val POST = "post"
    const val POOL = "pool"
    const val POPULAR = "popular"
    const val SETTING = "setting"
    const val SEARCH = "search?text={text}"

    fun search(text: String = ""): String = SEARCH.replace("{text}", text)
}

fun NavController.onNavigate(targetRoute: String) {
    navigate(targetRoute) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}