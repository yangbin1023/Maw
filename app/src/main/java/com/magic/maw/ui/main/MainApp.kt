package com.magic.maw.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import com.magic.maw.ui.theme.MawTheme
import kotlinx.coroutines.launch

private val WindowAdaptiveInfo.useNavRail: Boolean
    get() = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

@Composable
fun MainApp() {
    MawTheme {
        val scope = rememberCoroutineScope()
        val navController = rememberNavController()

        if (currentWindowAdaptiveInfo().useNavRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                MainNavRail(navController = navController)
                MainNavHost(navController = navController)
            }
        } else {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed) { newValue ->
                !(newValue == DrawerValue.Open && !navController.currentAppRoute.isRootRoute)
            }
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    AppDrawer(
                        navController = navController,
                        closeDrawer = { scope.launch { drawerState.close() } }
                    )
                }
            ) {
                MainNavHost(navController = navController)

                // 如果Drawer打开，则拦截返回事件，关闭Drawer
                val isOpen by remember { derivedStateOf { drawerState.isOpen } }
                BackHandler(enabled = isOpen) {
                    scope.launch { drawerState.close() }
                }
            }
        }
    }
}