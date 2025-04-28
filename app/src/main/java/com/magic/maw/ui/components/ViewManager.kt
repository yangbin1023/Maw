package com.magic.maw.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.magic.maw.util.UiUtils.showSystemBars
import com.magic.maw.util.logger

private val viewInfoList by lazy { ArrayList<ViewInfo>() }

@Composable
fun RegisterView(name: String, showSystemBar: Boolean = true) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        synchronized(viewInfoList) {
            viewInfoList.add(ViewInfo(name, showSystemBar))
            context.showSystemBars(showSystemBar)
        }
        logger.info("view[$name] register")
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
            logger.info("view[$name] unregister")
        }
    }
}

fun changeSystemBarStatus(context: Context, name: String, showSystemBar: Boolean) {
    synchronized(viewInfoList) {
        var index = viewInfoList.size - 1
        while (index >= 0) {
            if (viewInfoList[index].name == name) {
                viewInfoList[index].showSystemBar = showSystemBar
                if (index == viewInfoList.size - 1) {
                    context.showSystemBars(showSystemBar)
                }
                logger.info("set system bar status, [$name][$showSystemBar][${index + 1}/${viewInfoList.size}]")
                return
            }
            index--
        }
    }
    logger.severe("can't find view. [$name][$showSystemBar]")
    return
}

private data class ViewInfo(
    val name: String,
    var showSystemBar: Boolean = true
)