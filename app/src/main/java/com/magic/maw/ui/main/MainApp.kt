package com.magic.maw.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import com.magic.maw.ui.theme.MawTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "MainApp"

@Composable
fun MainApp() {
    MawTheme {
        val scope = rememberCoroutineScope()
        val navController = rememberNavController()

        Logger.d(TAG) { "MainApp recompose" }
        if (useNavRail()) {
            Row(modifier = Modifier.fillMaxSize()) {
                MainNavRail(navController = navController)
                MainNavHost(navController = navController)
            }
        } else {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val gesturesEnabled = isDrawerGesturesEnabled(navController)
            Logger.d(TAG) { "MainApp ModalNavigationDrawer recompose: $gesturesEnabled" }
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    AppDrawer(
                        navController = navController,
                        closeDrawer = { scope.launch { drawerState.close() } }
                    )
                },
                gesturesEnabled = gesturesEnabled
            ) {
                Logger.d(TAG) { "MainApp ModalNavigationDrawer content recompose" }

                MainNavHost(
                    navController = navController,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )

                DrawerOpenBackHandler(drawerState = drawerState, scope = scope)
            }
        }
    }
}

@Composable
private fun DrawerOpenBackHandler(
    drawerState: DrawerState,
    scope: CoroutineScope = rememberCoroutineScope()
) {
    Logger.d(TAG) { "MainApp ModalNavigationDrawer DrawerOpenBackHandler recompose" }
    // 如果Drawer打开，则拦截返回事件，关闭Drawer
    val isOpen by remember { derivedStateOf { drawerState.isOpen } }
    BackHandler(enabled = isOpen) {
        scope.launch { drawerState.close() }
    }
}

@Composable
fun useNavRail(): Boolean {
    val width = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp()
    }
    return width > 600.dp
}

@Composable
fun isDrawerGesturesEnabled(navController: NavController): Boolean {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val enabled by remember {
        derivedStateOf {
            (backStackEntry?.currentRoute ?: AppRoute.Post).isRootRoute
        }
    }
    return enabled
}