package com.magic.maw.ui.features.setting

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import co.touchlab.kermit.Logger
import com.magic.maw.R
import com.magic.maw.data.api.parser.BaseParser
import com.magic.maw.data.model.constant.Quality
import com.magic.maw.data.model.constant.ThemeMode
import com.magic.maw.data.model.constant.WebsiteOption
import com.magic.maw.ui.common.DialogSettingItem
import com.magic.maw.ui.common.MenuSettingItem
import com.magic.maw.ui.common.MultipleChoiceDialog
import com.magic.maw.ui.common.SettingItem
import com.magic.maw.ui.common.SingleChoiceDialog
import com.magic.maw.ui.common.SwitchSettingItem

private const val TAG = "SettingScreen"

@Composable
fun SettingScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: SettingsViewModel = settingsViewModel()
) {
    val scrollState = rememberScrollState()
    val showTopBarShadow by remember {
        derivedStateOf {
            scrollState.value > 0
        }
    }
    Logger.d(TAG) { "SettingScreen compose" }
    Scaffold(
        modifier = modifier,
        topBar = {
            SettingTopBar(
                modifier = Modifier.shadow(
                    elevation = if (showTopBarShadow) 4.dp else 0.dp
                ),
                onExit = { navController.popBackStack() }
            )
        }
    ) { innerPadding ->
        SettingsScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            scrollState = scrollState,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingTopBar(
    modifier: Modifier = Modifier,
    onExit: () -> Unit
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(R.string.setting)) },
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
            }
        }
    )
}

@Composable
private fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    viewModel: SettingsViewModel = settingsViewModel()
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .width(600.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(state = scrollState)
        ) {
            SettingsGroupTitle(text = stringResource(R.string.website))
            WebsiteSettingItem(viewModel = viewModel)
            RatingsSettingItem(viewModel = viewModel)
            ShowQualitySettingItem(viewModel = viewModel)
            SaveQualitySettingItem(viewModel = viewModel)
            UgoiraPlayerFrameRateSettingItem(viewModel = viewModel)

            SettingsGroupTitle(text = stringResource(R.string.video))
            VideoAutoPlaySettingItem(viewModel = viewModel)
            VideoMuteSettingItem(viewModel = viewModel)
            VideoSpeedMultiplierSettingItem(viewModel = viewModel)

            SettingsGroupTitle(text = stringResource(R.string.theme))
            ThemeModeSettingItem(viewModel = viewModel)
            DynamicColorSettingItem(viewModel = viewModel)

            SettingsGroupTitle(text = stringResource(R.string.others))
            VersionInfoItem()
        }
    }
}

@Composable
private fun SettingsGroupTitle(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        modifier = modifier.padding(horizontal = 15.dp, vertical = 3.dp),
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun WebsiteSettingItem(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = settingsViewModel()
) {
    val options = WebsiteOption.entries.toList()
    val settingsState by viewModel.settingsState.collectAsState()
    val option by remember { derivedStateOf { settingsState.website } }
    Logger.d(TAG) { "WebsiteSettingItem compose website: $option" }
    DialogSettingItem(
        modifier = modifier,
        title = stringResource(id = R.string.website),
        tips = option.toString()
    ) { onDismiss ->
        SingleChoiceDialog(
            title = stringResource(R.string.website),
            options = options,
            selectedOption = option,
            onDismiss = onDismiss,
            onOptionSelected = { option ->
                viewModel.updateWebsite(option)
            }
        )
    }
}

@Composable
private fun RatingsSettingItem(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = settingsViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val website by remember { derivedStateOf { settingsState.website } }
    val ratings by remember { derivedStateOf { settingsState.websiteSettings.ratings } }
    val supportRatings = BaseParser.get(website).supportedRatings
    val tips = if (ratings.toSet() == supportRatings.toSet()) {
        stringResource(R.string.all)
    } else {
        ratings.joinToString(", ")
    }
    Logger.d(TAG) { "RatingsSettingItem compose website: $website, ratings: $ratings, supportRatings: $supportRatings" }
    DialogSettingItem(
        modifier = modifier,
        title = stringResource(id = R.string.rating),
        tips = tips
    ) { onDismiss ->
        MultipleChoiceDialog(
            title = stringResource(R.string.rating),
            options = supportRatings,
            selectedOptions = ratings,
            onDismiss = onDismiss,
            onConfirm = { ratings ->
                viewModel.updateWebSettings { copy(ratings = ratings) }
            }
        )
    }
}

@Composable
private fun ShowQualitySettingItem(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = settingsViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val showQuality by remember { derivedStateOf { settingsState.websiteSettings.showQuality } }
    val options = Quality.SaveList
    Logger.d(TAG) { "ShowQualitySettingItem compose showQuality: $showQuality" }
    DialogSettingItem(
        modifier = modifier,
        title = stringResource(id = R.string.quality),
        tips = showQuality.resId?.let { stringResource(it) } ?: ""
    ) { onDismiss ->
        SingleChoiceDialog(
            title = stringResource(R.string.quality),
            options = options,
            optionsToString = { quality -> quality.resId?.let { stringResource(it) } ?: "" },
            selectedOption = showQuality,
            onDismiss = onDismiss,
            onOptionSelected = { option ->
                viewModel.updateWebSettings { copy(showQuality = option) }
            }
        )
    }
}

@Composable
private fun SaveQualitySettingItem(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = settingsViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val saveQuality by remember { derivedStateOf { settingsState.websiteSettings.saveQuality } }
    val options = Quality.SaveList
    Logger.d(TAG) { "ShowQualitySettingItem compose showQuality: $saveQuality" }
    DialogSettingItem(
        modifier = modifier,
        title = stringResource(id = R.string.save),
        tips = saveQuality.resId?.let { stringResource(it) } ?: ""
    ) { onDismiss ->
        SingleChoiceDialog(
            title = stringResource(R.string.save),
            options = options,
            optionsToString = { quality -> quality.resId?.let { stringResource(it) } ?: "" },
            selectedOption = saveQuality,
            onDismiss = onDismiss,
            onOptionSelected = { option ->
                viewModel.updateWebSettings { copy(showQuality = option) }
            }
        )
    }
}

@Composable
private fun UgoiraPlayerFrameRateSettingItem(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = settingsViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsState()
    if (settingsState.website != WebsiteOption.Danbooru)
        return

    val ugoiraFrameRate by remember {
        derivedStateOf {
            settingsState.websiteSettings.ugoiraFrameRate
        }
    }
    val ugoiraFrameRateList = listOf(5, 10, 15, 20, 25, 30)
    MenuSettingItem(
        modifier = modifier,
        title = stringResource(R.string.ugoira_default_frame_rate),
        options = ugoiraFrameRateList,
        selectOption = ugoiraFrameRate,
        onOptionSelected = { frameRate ->
            viewModel.updateWebSettings {
                copy(ugoiraFrameRate = frameRate)
            }
        }
    )
}

@Composable
private fun VideoAutoPlaySettingItem(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = settingsViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val autoplay by remember { derivedStateOf { settingsState.videoSettings.autoplay } }
    Logger.d(TAG) { "VideoAutoPlaySettingItem compose autoplay: $autoplay" }
    SwitchSettingItem(
        modifier = modifier,
        title = stringResource(R.string.autoplay),
        checked = autoplay,
        onCheckedChange = {
            viewModel.updateVideoSettings { copy(autoplay = it) }
        }
    )
}

@Composable
private fun VideoMuteSettingItem(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = settingsViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val mute by remember { derivedStateOf { settingsState.videoSettings.mute } }
    Logger.d(TAG) { "VideoMuteSettingItem compose mute: $mute" }
    SwitchSettingItem(
        modifier = modifier,
        title = stringResource(R.string.mute),
        checked = mute,
        onCheckedChange = {
            viewModel.updateVideoSettings { copy(mute = it) }
        }
    )
}

@Composable
private fun VideoSpeedMultiplierSettingItem(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = settingsViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val multiplier by remember { derivedStateOf { settingsState.videoSettings.playbackSpeedMultiplier } }
    val multiplierMap = mapOf("2x" to 2f, "3x" to 3f, "5x" to 5f)
    val items = multiplierMap.keys.toList().sorted()
    val key = multiplierMap.entries.find { it.value == multiplier }?.key
    val index = key?.let { items.indexOf(it).coerceAtLeast(0) } ?: 0
    Logger.d(TAG) { "VideoSpeedMultiplierSettingItem compose items: $items, index: $index, multiplier: $multiplier" }
    MenuSettingItem(
        modifier = modifier,
        title = stringResource(R.string.video_speedup),
        items = items,
        checkItem = index,
        onItemChanged = { index ->
            multiplierMap[items[index]]?.let {
                viewModel.updateVideoSettings { copy(playbackSpeedMultiplier = it) }
            }
        }
    )
}

@Composable
private fun ThemeModeSettingItem(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = settingsViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val themeMode by remember { derivedStateOf { settingsState.themeSettings.themeMode } }
    Logger.d(TAG) { "ThemeModeSettingItem compose themeMode: $themeMode" }
    MenuSettingItem(
        modifier = modifier,
        title = stringResource(R.string.theme_mode),
        options = ThemeMode.entries,
        optionToString = { stringResource(it.resId) },
        selectOption = themeMode,
        onOptionSelected = { viewModel.updateThemeSettings { copy(themeMode = it) } }
    )
}

@Composable
private fun DynamicColorSettingItem(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = settingsViewModel()
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val dynamicColor by remember { derivedStateOf { settingsState.themeSettings.dynamicColor } }
    Logger.d(TAG) { "DynamicColorSettingItem compose dynamicColor: $dynamicColor" }
    SwitchSettingItem(
        modifier = modifier,
        title = stringResource(R.string.dynamic_color),
        subTitle = stringResource(R.string.dynamic_color_desc),
        checked = dynamicColor,
        onCheckedChange = {
            viewModel.updateThemeSettings { copy(dynamicColor = it) }
        }
    )
}

@Composable
private fun VersionInfoItem(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    SettingItem(
        modifier = modifier,
        title = stringResource(id = R.string.version),
        tips = appInfo.versionName,
    )
}