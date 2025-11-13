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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.hjq.toast.Toaster
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
import com.magic.maw.ui.theme.supportDynamicColor
import com.magic.maw.ui.view.SaveDialog
import com.magic.maw.util.Config
import com.magic.maw.util.WebsiteConfig
import com.magic.maw.util.configFlow
import com.magic.maw.util.updateWebConfig
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.flow.update

private const val viewName = "Setting"

private var lastClickVersionItemTime = 0L
private var clickVersionItemCount = 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    isExpandedScreen: Boolean,
    onFinish: (() -> Unit)? = null,
) {
    val changeSetting = remember { mutableStateOf(false) }
    RegisterView(name = viewName)
    Scaffold(topBar = {
        SettingTopBar(
            shadowEnable = false,
            onFinish = onFinish ?: {},
        )
    }) { innerPadding ->
        SettingBody(
            modifier = Modifier.padding(innerPadding),
            changeSetting = changeSetting,
            isExpandedScreen = isExpandedScreen,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingTopBar(
    modifier: Modifier = Modifier,
    shadowEnable: Boolean = true,
    onFinish: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CenterAlignedTopAppBar(
        modifier = modifier.let { if (shadowEnable) it.shadow(3.dp) else it },
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
    val websiteConfig = config.websiteConfig
    val titleModifier = Modifier.padding(top = 7.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        /** 网站设置 */
        SettingGroupTitle(text = stringResource(R.string.website))
        // 网站设置
        WebsiteSetting(source = config.source, changeSetting = changeSetting)
        // 分级
        RatingSetting(config = config, changeSetting = changeSetting)
        // 质量
        QualitySetting(websiteConfig = websiteConfig)
        // 保存文件
        SaveSetting(websiteConfig = websiteConfig)

        /** 视频设置 */
        SettingGroupTitle(text = stringResource(R.string.video), modifier = titleModifier)
        // 自动播放
        AutoPlaySetting(autoplay = config.autoplay)
        // 静音
        MuteSetting(mute = config.mute)
        // 视频加速倍率
        VideoSpeedupSetting(speedup = config.videoSpeedup)

        /** 主题 */
        SettingGroupTitle(text = stringResource(R.string.theme), modifier = titleModifier)
        // 深色模式
        DarkModeSetting(darkMode = config.darkMode)
        // 动态颜色
        DynamicColorSetting(checked = config.dynamicColor)

        /** 其他 */
        SettingGroupTitle(text = stringResource(R.string.other), modifier = titleModifier)
        // 版本
        VersionSetting()
    }
}

@Composable
private fun SettingGroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 15.dp, vertical = 3.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * 网站设置
 */
@Composable
private fun WebsiteSetting(
    source: String,
    changeSetting: MutableState<Boolean>,
) {
    val options = LocalResources.current.getStringArray(R.array.website).toList()
    val initValue = BaseParser.getIndex(source)
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
            onOptionSelected = { index ->
                selectIndex = index
                val sourceTag = BaseParser.getTag(options[selectIndex])
                if (sourceTag.isNotEmpty() && sourceTag != source) {
                    changeSetting.value = true
                    configFlow.update { it.copy(source = sourceTag) }
                }
                onDismiss()
            }
        )
    }
}

/**
 * 分级设置
 */
@Composable
private fun RatingSetting(
    config: Config,
    changeSetting: MutableState<Boolean>,
) {
    val parser = BaseParser.get(config.source)
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
                onDismiss()
            })
        )
    }
}

/**
 * 质量设置
 */
@Composable
private fun QualitySetting(websiteConfig: WebsiteConfig) {
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
            title = stringResource(id = R.string.quality),
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
                onDismiss()
            })
        )
    }
}

/**
 * 保存设置
 */
@Composable
private fun SaveSetting(websiteConfig: WebsiteConfig) {
    val saveQuality = websiteConfig.saveQuality.toQuality()
    DialogSettingItem(
        title = stringResource(id = R.string.save),
        tips = saveQuality.toResString(LocalContext.current)
    ) { onDismiss ->
        SaveDialog(onDismiss = onDismiss)
    }
}

/**
 * 自动播放
 */
@Composable
private fun AutoPlaySetting(autoplay: Boolean) {
    SwitchSettingItem(
        title = stringResource(R.string.autoplay),
        checked = autoplay,
        onCheckedChange = {
            configFlow.update { s -> s.copy(autoplay = it) }
        }
    )
}

/**
 * 静音
 */
@Composable
private fun MuteSetting(mute: Boolean) {
    SwitchSettingItem(
        title = stringResource(R.string.mute),
        checked = mute,
        onCheckedChange = {
            configFlow.update { s -> s.copy(mute = it) }
        }
    )
}

/**
 * 视频加速倍率
 */
@Composable
private fun VideoSpeedupSetting(speedup: Float) {
    val speedupMap = mapOf("2x" to 2f, "3x" to 3f, "5x" to 5f)
    val items = speedupMap.keys.toList().sorted()
    val key = speedupMap.entries.find { it.value == speedup }?.key
    val index = key?.let { items.indexOf(it).coerceAtLeast(0) } ?: 0
    MenuSettingItem(
        title = stringResource(R.string.video_speedup),
        items = items,
        checkItem = index,
        onItemChanged = { index ->
            speedupMap[items[index]]?.let {
                configFlow.update { s -> s.copy(videoSpeedup = it) }
            }
        }
    )
}

/**
 * 深色模式
 */
@Composable
private fun DarkModeSetting(darkMode: Int) {
    MenuSettingItem(
        title = stringResource(R.string.dark_mode),
        items = listOf(
            stringResource(R.string.follow_system),
            stringResource(R.string.always_enable),
            stringResource(R.string.always_disable)
        ),
        checkItem = darkMode,
        onItemChanged = { configFlow.update { s -> s.copy(darkMode = it) } }
    )
}

/**
 * 动态颜色
 */
@Composable
private fun DynamicColorSetting(checked: Boolean) {
    if (!supportDynamicColor) {
        return
    }
    SwitchSettingItem(
        title = stringResource(id = R.string.dynamic_color),
        subTitle = stringResource(id = R.string.dynamic_color_desc),
        checked = checked,
        onCheckedChange = {
            configFlow.update { s -> s.copy(dynamicColor = it) }
        }
    )
}

/**
 * 版本
 */
@Composable
private fun VersionSetting() {
    val context = LocalContext.current
    val appInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    SettingItem(
        title = stringResource(id = R.string.version),
        tips = appInfo.versionName,
        onClickWidthThrottle = false
    ) {
        val now = System.currentTimeMillis()
        if (now - lastClickVersionItemTime < 1000) {
            clickVersionItemCount++
        } else {
            clickVersionItemCount = 1
        }
        lastClickVersionItemTime = now
        Logger.v { "click version item count: $clickVersionItemCount" }
        if (clickVersionItemCount >= 5) {
            clickVersionItemCount = 0
            CookieManager.getInstance().removeAllCookies(null)
            Toaster.show(R.string.all_cookies_cleared)
        }
    }
}
