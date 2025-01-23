package com.magic.maw.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.magic.maw.R
import com.magic.maw.data.Quality
import com.magic.maw.ui.components.AlertPaddingDialog
import com.magic.maw.ui.components.SourceChangeChecker
import com.magic.maw.ui.components.throttle
import com.magic.maw.util.configFlow
import com.magic.maw.util.updateWebConfig

@Composable
fun SaveDialog(
    showDialog: MutableState<Boolean>,
    qualityList: List<Quality> = Quality.SaveList,
    showSaveTips: Boolean = false,
    onConfirm: (Quality) -> Unit = {}
) {
    if (!showDialog.value) {
        return
    }
    SaveDialog(
        onDismiss = { showDialog.value = false },
        qualityList = qualityList,
        showSaveTips = showSaveTips,
        onConfirm = onConfirm
    )
}

@Composable
fun SaveDialog(
    onDismiss: () -> Unit,
    qualityList: List<Quality> = Quality.SaveList,
    showSaveTips: Boolean = false,
    onConfirm: (Quality) -> Unit = {}
) {
    SourceChangeChecker { onDismiss() }
    val config = configFlow.collectAsStateWithLifecycle().value
    val websiteConfig = config.websiteConfig
    var initialIndex = 0
    for ((index, quality) in qualityList.withIndex()) {
        if (websiteConfig.saveQuality == quality.value) {
            initialIndex = index
            break
        }
    }
    val selectedIndex = remember { mutableIntStateOf(initialIndex) }
    val showAgain = remember { mutableStateOf(websiteConfig.showSaveDialog) }
    val onSelected: (Int) -> Unit = { selectedIndex.intValue = it }
    AlertPaddingDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.save)) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(qualityList) { index, quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable { onSelected(index) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedIndex.intValue,
                            onClick = { onSelected(index) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = quality.toResString(LocalContext.current))
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable { showAgain.value = !showAgain.value },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = !showAgain.value,
                            onCheckedChange = { showAgain.value = !showAgain.value }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = stringResource(R.string.not_show_again))
                    }
                }
                item {
                    if (showSaveTips) {
                        Text(
                            text = stringResource(R.string.save_tips),
                            modifier = Modifier.padding(horizontal = 15.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Text(
                modifier = Modifier
                    .clickable(onClick = throttle(func = {
                        val quality = qualityList[selectedIndex.intValue]
                        val newConfig = websiteConfig.copy(
                            saveQuality = quality.value,
                            showSaveDialog = showAgain.value
                        )
                        configFlow.updateWebConfig(newConfig)
                        onDismiss()
                        onConfirm(quality)
                    }))
                    .padding(5.dp),
                text = stringResource(id = R.string.confirm)
            )
        },
        dismissButton = {
            Text(
                modifier = Modifier
                    .clickable(onClick = throttle(func = onDismiss))
                    .padding(vertical = 5.dp, horizontal = 8.dp),
                text = stringResource(id = R.string.cancel)
            )
        },
        shape = RoundedCornerShape(10.dp),
    )
}