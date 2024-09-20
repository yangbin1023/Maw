package com.magic.maw.ui.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.magic.maw.R
import com.magic.maw.data.Rating.Companion.getRatings
import com.magic.maw.data.Rating.Companion.hasRating
import com.magic.maw.ui.components.DialogSettingItem
import com.magic.maw.ui.components.MenuSettingItem
import com.magic.maw.ui.components.MultipleChoiceDialog
import com.magic.maw.ui.components.ShowSystemBars
import com.magic.maw.ui.components.SingleChoiceDialog
import com.magic.maw.ui.components.SwitchSettingItem
import com.magic.maw.ui.components.throttle
import com.magic.maw.ui.theme.MawTheme
import com.magic.maw.ui.theme.supportDynamicColor
import com.magic.maw.util.configFlow
import com.magic.maw.util.updateWebConfig
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.flow.update

private const val TAG = "SettingScreen"

@Composable
fun SettingScreen(
    windowSizeClass: WindowSizeClass,
    onFinish: () -> Unit,
    systemBarShown: Boolean = true
) {
    MawTheme {
        val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
        SettingContent(isExpandedScreen = isExpandedScreen, onFinish = onFinish)

        if (!systemBarShown) {
            ShowSystemBars()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingContent(
    isExpandedScreen: Boolean,
    onFinish: () -> Unit,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = topAppBarState)
    Scaffold(topBar = {
        SettingTopBar(
            enableShadow = false,
            onFinish = onFinish,
            scrollBehavior = scrollBehavior
        )
    }) { innerPadding ->
        SettingBody(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            isExpandedScreen = isExpandedScreen,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingTopBar(
    modifier: Modifier = Modifier,
    enableShadow: Boolean = true,
    onFinish: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CenterAlignedTopAppBar(
        modifier = modifier.let { if (enableShadow) it.shadow(5.dp) else it },
        title = { Text(stringResource(R.string.setting)) },
        navigationIcon = {
            IconButton(onClick = onFinish) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "",
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun SettingBody(
    modifier: Modifier = Modifier,
    isExpandedScreen: Boolean,
) {
    val config by configFlow.collectAsState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // 网站设置
        val options = LocalContext.current.resources.getStringArray(R.array.website).toList()
        var selectIndex by remember { mutableIntStateOf(0) }
        DialogSettingItem(
            title = stringResource(id = R.string.website),
            tips = options[selectIndex]
        ) { onDismiss ->
            SingleChoiceDialog(
                title = stringResource(R.string.website),
                options = options,
                selectedOptionIndex = selectIndex,
                onDismissRequest = onDismiss,
                onOptionSelected = {
                    selectIndex = it
                    onDismiss.invoke()
                }
            )
        }

        // 分级
        val parser = BaseParser.getParser(config.source.lowercase())
        val websiteConfig = config.websiteConfig
        val supportRatings = parser.supportRating.getRatings()
        val selectedRatings = websiteConfig.rating.getRatings()
        val ratingNames = ArrayList<String>()
        val selectedRatingList = ArrayList<Int>()
        for ((index, rating) in supportRatings.withIndex()) {
            ratingNames.add(rating.name)
            if (websiteConfig.rating.hasRating(rating))
                selectedRatingList.add(index)
        }
        DialogSettingItem(
            title = stringResource(id = R.string.rating),
            tips = if (websiteConfig.rating == parser.supportRating)
                stringResource(R.string.all)
            else
                selectedRatings.joinToString(", ")
        ) { onDismiss ->
            MultipleChoiceDialog(
                title = stringResource(id = R.string.rating),
                options = ratingNames,
                selectedOptions = selectedRatingList,
                onDismiss = onDismiss,
                onConfirm = throttle(func = { indexes: List<Int> ->
                    var newRating = 0
                    for (index in indexes) {
                        newRating += supportRatings[index].value
                    }
                    configFlow.updateWebConfig(websiteConfig.copy(rating = newRating))
                    onDismiss.invoke()
                })
            )
        }

        // 质量

        // 深色模式
        val darkSettingItems = arrayOf(
            stringResource(R.string.follow_system),
            stringResource(R.string.always_enable),
            stringResource(R.string.always_disable)
        )
        MenuSettingItem(
            title = stringResource(R.string.dark_mode),
            items = darkSettingItems,
            checkItem = config.darkMode,
            onItemChanged = { configFlow.update { s -> s.copy(darkMode = it) } }
        )

        // 动态颜色
        if (supportDynamicColor) {
            SwitchSettingItem(
                title = stringResource(id = R.string.dynamic_color),
                subTitle = stringResource(id = R.string.dynamic_color_desc),
                checked = config.dynamicColor,
                onCheckedChange = { configFlow.update { s -> s.copy(dynamicColor = it) } })
        }
    }
}