package com.magic.maw.ui.setting

import android.webkit.CookieManager
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.magic.maw.R
import com.magic.maw.data.Quality
import com.magic.maw.data.Quality.Companion.toQuality
import com.magic.maw.data.Rating.Companion.getRatings
import com.magic.maw.data.Rating.Companion.hasRating
import com.magic.maw.ui.components.DialogSettingItem
import com.magic.maw.ui.components.MenuSettingItem
import com.magic.maw.ui.components.MultipleChoiceDialog
import com.magic.maw.ui.components.RegisterView
import com.magic.maw.ui.components.SettingItem
import com.magic.maw.ui.components.SingleChoiceDialog
import com.magic.maw.ui.components.SwitchSettingItem
import com.magic.maw.ui.components.throttle
import com.magic.maw.ui.main.MainRoutes
import com.magic.maw.ui.theme.supportDynamicColor
import com.magic.maw.util.Logger
import com.magic.maw.util.configFlow
import com.magic.maw.util.updateWebConfig
import com.magic.maw.website.parser.BaseParser
import com.magic.maw.website.parser.DanbooruParser
import com.magic.maw.website.parser.KonachanParser
import com.magic.maw.website.parser.YandeParser
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val viewName = "Setting"
private val logger = Logger(viewName)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    isExpandedScreen: Boolean,
    onFinish: (() -> Unit)? = null,
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(state = topAppBarState)
    val changeSetting = remember { mutableStateOf(false) }
    RegisterView(name = viewName)
    Scaffold(topBar = {
        SettingTopBar(
            enableShadow = false,
            onFinish = onFinish ?: {},
            scrollBehavior = scrollBehavior
        )
    }) { innerPadding ->
        SettingBody(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            changeSetting = changeSetting,
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
    changeSetting: MutableState<Boolean>,
) {
    val config by configFlow.collectAsState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // 网站设置
        val options = LocalContext.current.resources.getStringArray(R.array.website).toList()
        val initValue = when (config.source) {
            YandeParser.SOURCE -> 0
            KonachanParser.SOURCE -> 1
            DanbooruParser.SOURCE -> 2
            else -> 0
        }
        var selectIndex by remember { mutableIntStateOf(initValue) }
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
                    val source = when (options[selectIndex]) {
                        "Yande" -> YandeParser.SOURCE
                        "Konachan" -> KonachanParser.SOURCE
                        "Danbooru" -> DanbooruParser.SOURCE
                        else -> ""
                    }
                    if (source.isNotEmpty() && source != config.source) {
                        changeSetting.value = true
                        configFlow.update { it.copy(source = source) }
                    }
                    onDismiss.invoke()
                }
            )
        }

        // 分级
        val parser = BaseParser.get(config.source.lowercase())
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
                    if (websiteConfig.rating != newRating) {
                        changeSetting.value = true
                        configFlow.updateWebConfig(websiteConfig.copy(rating = newRating))
                    }
                    onDismiss.invoke()
                })
            )
        }

        // 质量
        val currentQuality = websiteConfig.quality.toQuality()
        val qualitySettingItems = listOf(
            stringResource(R.string.quality_sample),
            stringResource(R.string.quality_large),
            stringResource(R.string.quality_origin)
        )
        val qualitySelectIndex = when (currentQuality) {
            Quality.Large -> 1
            Quality.File -> 2
            else -> 0
        }
        DialogSettingItem(
            title = stringResource(id = R.string.quality),
            tips = currentQuality.toResString(LocalContext.current)
        ) { onDismiss ->
            SingleChoiceDialog(
                title = stringResource(id = R.string.rating),
                options = qualitySettingItems,
                selectedOptionIndex = qualitySelectIndex,
                onDismissRequest = onDismiss,
                onOptionSelected = throttle(func = { index: Int ->
                    val quality = when (index) {
                        1 -> Quality.Large
                        2 -> Quality.File
                        else -> Quality.Sample
                    }
                    configFlow.updateWebConfig(websiteConfig.copy(quality = quality.value))
                    onDismiss.invoke()
                })
            )
        }

        // 深色模式
        val darkSettingItems = listOf(
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

        // 版本
        val context = LocalContext.current
        val appInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        SettingItem(title = stringResource(id = R.string.version), tips = appInfo.versionName)

        SettingItem(title = "清除Cookie", onClick = {
            CookieManager.getInstance().removeAllCookies(null)
        })

        SettingItem(title = "Test", onClick = {
            val ret = BaseParser.callGotoVerifyUrlCallback("https://konachan.net/post.json",
                KonachanParser.SOURCE)
            logger.info("call goto verify ret: $ret")
        })
    }
}
