package com.magic.maw.ui.main

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.magic.maw.MyApp
import com.magic.maw.data.PostData
import com.magic.maw.website.parser.BaseParser

sealed class MainUiState {
    object Loading : MainUiState()
    object Error : MainUiState()
    object Success : MainUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val app: MyApp = application as MyApp
    val parser: BaseParser = app.parser
    val dataList = SnapshotStateList<PostData>()

//    private var _staggered = mutableStateOf(true)
//    var staggered: Boolean
//        get() = _staggered.value
//        set(value) {
//            _staggered.value = value
//        }

    var staggered: Boolean by mutableStateOf(true)
}

object MainRoutes {
    const val POST = "post"
    const val POOL = "pool"
    const val POPULAR = "popular"
    const val SETTING = "setting"
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