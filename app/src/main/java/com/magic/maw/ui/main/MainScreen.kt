package com.magic.maw.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.magic.maw.ui.theme.MawTheme
import com.magic.maw.util.UiUtils.currentRoute
import kotlinx.coroutines.launch

private const val TAG = "MainScreen"

@Composable
fun MainScreen() {
    MawTheme {
        val navController = rememberNavController()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        // drawerState.currentOffset 侧边栏滑动的偏移量，全部显示时为0

        val coroutineScope = rememberCoroutineScope()
        val inSubView = remember { mutableStateOf(false) }
        val screenWidth = LocalConfiguration.current.screenWidthDp * LocalDensity.current.density
        val dragThreshold = screenWidth * 0.3f
        val isExpandedScreen = false//windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
        val gesturesEnabled = remember { mutableStateOf(true) }

        CheckGestureEnable(
            navController = navController,
            isExpandedScreen = isExpandedScreen,
            inSubView = inSubView,
            enable = gesturesEnabled
        )

        ModalNavigationDrawer(
            drawerContent = {
                MainDrawerContent(
                    navController = navController,
                    drawerState = drawerState
                )
            },
            drawerState = drawerState,
            gesturesEnabled = gesturesEnabled.value
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
                    MainNavRail(navController = navController)
                }
                MainNavGraph(
                    inSubView = inSubView,
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

@Composable
private fun MainDrawerContent(navController: NavController, drawerState: DrawerState) {
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentRoute ?: MainRoutes.POST
    val lastRoute = remember { mutableStateOf(currentRoute) }
    if (MainRoutes.isMainView(currentRoute) || currentRoute == MainRoutes.SETTING) {
        lastRoute.value = currentRoute
    }
    MainDrawer(
        currentRoute = lastRoute.value,
        navController = navController,
        closeDrawer = { scope.launch { drawerState.close() } }
    )
}

@Composable
private fun CheckGestureEnable(
    navController: NavController,
    isExpandedScreen: Boolean,
    inSubView: MutableState<Boolean>,
    enable: MutableState<Boolean>
) {
    val currentRoute = navController.currentRoute ?: MainRoutes.POST
    enable.value = !isExpandedScreen
                && !inSubView.value
                && MainRoutes.isMainView(currentRoute)
}