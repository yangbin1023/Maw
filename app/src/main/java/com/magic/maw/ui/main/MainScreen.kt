package com.magic.maw.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.compose.rememberNavController
import com.magic.maw.ui.theme.MawTheme
import com.magic.maw.util.UiUtils.currentRoute
import kotlinx.coroutines.launch

private const val TAG = "MainScreen"

@Composable
fun MainScreen(mainViewModel: MainViewModel, windowSizeClass: WindowSizeClass) {
    MawTheme {
        val navController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        // drawerState.currentOffset 侧边栏滑动的偏移量，全部显示时为0

        val coroutineScope = rememberCoroutineScope()
        val currentRoute = navController.currentRoute ?: MainRoutes.POST

        val screenWidth = LocalConfiguration.current.screenWidthDp * LocalDensity.current.density
        val dragThreshold = screenWidth * 0.3f
        val isExpandedScreen = false//windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
        val gesturesEnabled = !isExpandedScreen
                && mainViewModel.gesturesEnabled
                && currentRoute != MainRoutes.SETTING
                && currentRoute != MainRoutes.SEARCH

        ModalNavigationDrawer(
            drawerContent = {
                MainDrawer(
                    currentRoute = currentRoute,
                    navController = navController,
                    closeDrawer = { coroutineScope.launch { drawerState.close() } }
                )
            },
            drawerState = drawerState,
            gesturesEnabled = gesturesEnabled
        ) {
            Row(modifier = Modifier.apply {
                if (!isExpandedScreen) {
                    pointerInput(Unit) {
                        // 限制侧边栏的滑动打开的范围，仅左侧十分之三范围滑动可打开Drawer
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (change.position.x < dragThreshold && dragAmount > 0) {
                                drawerState.currentOffset
                                coroutineScope.launch { drawerState.open() }
                            }
                        }
                    }
                }
            }) {
                if (isExpandedScreen) {
                    MainNavRail(
                        currentRoute = currentRoute,
                        onNavigate = { navController.onNavigate(it) }
                    )
                }
                MainNavGraph(
                    mainViewModel = mainViewModel,
                    isExpandedScreen = isExpandedScreen,
                    navController = navController,
                    openDrawer = { coroutineScope.launch { drawerState.open() } }
                )
            }

            // 如果Drawer打开，则拦截返回事件，关闭Drawer
            BackHandler(enabled = drawerState.isOpen) {
                coroutineScope.launch { drawerState.close() }
            }
        }
    }
}
