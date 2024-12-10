package com.magic.maw.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.magic.maw.util.UiUtils.showSystemBars

private val viewInfoList by lazy { ArrayList<ViewInfo>() }

@Composable
fun RegisterView(name: String, showSystemBar: Boolean = true) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        synchronized(viewInfoList) {
            viewInfoList.add(ViewInfo(name, showSystemBar))
            context.showSystemBars(showSystemBar)
        }
        onDispose {
            synchronized(viewInfoList) {
                var index = viewInfoList.size - 1
                while (index >= 0) {
                    if (viewInfoList[index].name == name) {
                        viewInfoList.removeAt(index)
                        break
                    }
                    index--
                }
                if (viewInfoList.isNotEmpty()) {
                    context.showSystemBars(viewInfoList.last().showSystemBar)
                }
            }
        }
    }
}

fun changeSystemBarStatus(context: Context, name: String, showSystemBar: Boolean): Boolean {
    synchronized(viewInfoList) {
        var index = viewInfoList.size - 1
        while (index >= 0) {
            if (viewInfoList[index].name == name) {
                viewInfoList[index].showSystemBar = showSystemBar
                if (index == viewInfoList.size - 1) {
                    context.showSystemBars(showSystemBar)
                }
                return true
            }
            index--
        }
    }
    return false
}

private data class ViewInfo(
    val name: String,
    var showSystemBar: Boolean = true
)