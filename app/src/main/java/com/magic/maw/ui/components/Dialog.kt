package com.magic.maw.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magic.maw.R
import com.magic.maw.data.WebsiteOption
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedOptionIndex: Int,
    onDismissRequest: () -> Unit,
    onOptionSelected: (Int) -> Unit
) {
    AlertPaddingDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(index) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedOptionIndex,
                            onClick = { onOptionSelected(index) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = option)
                    }
                }
            }
        },
        confirmButton = {
            Text(
                modifier = Modifier
                    .clickable(onClick = throttle(func = onDismissRequest))
                    .padding(5.dp),
                text = stringResource(R.string.confirm)
            )
        },
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
fun <T> SingleChoiceDialog(
    title: String,
    options: List<T>,
    optionsToString: @Composable (T) -> String = { it.toString() },
    selectedOption: T? = null,
    selectedOptionIndex: Int? = null,
    showCancelButton: Boolean = true,
    showConfirmButton: Boolean = false,
    barrierDismissible: Boolean = true,
    shape: Shape = RoundedCornerShape(10.dp),
    onDismiss: () -> Unit,
    onOptionSelected: (T) -> Unit
) {
    var optionIndex by remember {
        val defaultIndex = selectedOptionIndex
            ?: if (selectedOption != null) {
                options.indexOf(selectedOption).coerceIn(0, options.size - 1)
            } else {
                0
            }
        mutableIntStateOf(defaultIndex)
    }
    AlertPaddingDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    val onClick: () -> Unit = {
                        if (barrierDismissible || !showConfirmButton) {
                            onOptionSelected(options[index])
                            onDismiss()
                        } else {
                            optionIndex = index
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onClick),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == optionIndex,
                            onClick = onClick
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = optionsToString(option))
                    }
                }
            }
        },
        confirmButton = {
            if (showCancelButton) {
                Text(
                    modifier = Modifier
                        .clickable(onClick = throttle(func = onDismiss))
                        .padding(5.dp),
                    text = stringResource(R.string.cancel)
                )
            }
            if (showConfirmButton) {
                Text(
                    modifier = Modifier
                        .clickable(onClick = throttle(func = {
                            onOptionSelected(options[optionIndex])
                            onDismiss()
                        }))
                        .padding(5.dp),
                    text = stringResource(R.string.confirm)
                )
            }
        },
        shape = shape
    )
}

@Composable
fun MultipleChoiceDialog(
    title: String,
    options: List<String>,
    selectedOptions: List<Int>,
    allowNoSelection: Boolean = false,
    shape: Shape = RoundedCornerShape(10.dp),
    onDismiss: () -> Unit = {},
    onConfirm: (List<Int>) -> Unit
) {
    val selectedList = SnapshotStateList<Boolean>()
    for (index in options.indices) {
        selectedList.add(selectedOptions.contains(index))
    }
    AlertPaddingDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(options.size) { index ->
                    Row(
                        modifier = Modifier
                            .clickable { selectedList[index] = !selectedList[index] }
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedList[index],
                            onCheckedChange = { selectedList[index] = !selectedList[index] }
                        )
                        Text(text = options[index])
                    }
                }
            }
        },
        confirmButton = {
            val canConfirm = if (allowNoSelection) true else selectedList.any { it }
            val color = MaterialTheme.colorScheme.let {
                if (canConfirm) it.primary else it.disableColor
            }
            Text(
                modifier = Modifier
                    .clickable(enabled = canConfirm) {
                        val result = mutableListOf<Int>()
                        for (i in selectedList.indices) {
                            if (selectedList[i]) {
                                result.add(i)
                            }
                        }
                        onConfirm(result)
                    }
                    .padding(5.dp),
                text = stringResource(id = R.string.confirm),
                color = color
            )
        },
        dismissButton = {
            Text(
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(vertical = 5.dp, horizontal = 8.dp),
                text = stringResource(id = R.string.cancel)
            )
        },
        shape = shape
    )
}

@Composable
fun <T> MultipleChoiceDialog(
    title: String,
    options: List<T>,
    optionsToString: @Composable (T) -> String = { it.toString() },
    selectedOptions: (List<T>)? = null,
    selectedOptionIndexList: (List<Int>)? = null,
    allowNoSelection: Boolean = false,
    showCancelButton: Boolean = true,
    shape: Shape = RoundedCornerShape(10.dp),
    onDismiss: () -> Unit = {},
    onConfirm: (List<T>) -> Unit
) {
    val selectedMap = SnapshotStateMap<Int, Boolean>()
    if (selectedOptions != null) {
        for (option in selectedOptions) {
            val index = options.indexOf(option)
            if (index >= 0) {
                selectedMap[index] = true
            }
        }
    } else if (selectedOptionIndexList != null) {
        for (index in selectedOptionIndexList) {
            if (index < 0 || index >= options.size)
                continue
            selectedMap[index] = true
        }
    }
    AlertPaddingDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(options.size) { index ->
                    val onClick: () -> Unit = {
                        selectedMap[index] = selectedMap[index] != true
                    }
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onClick)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedMap[index] == true,
                            onCheckedChange = { onClick() }
                        )
                        Text(text = optionsToString(options[index]))
                    }
                }
            }
        },
        confirmButton = {
            val canConfirm = if (allowNoSelection) true else selectedMap.any { it.value }
            val color = MaterialTheme.colorScheme.let {
                if (canConfirm) it.primary else it.disableColor
            }
            Text(
                modifier = Modifier
                    .clickable(enabled = canConfirm, onClick = throttle {
                        onConfirm(
                            selectedMap
                                .filter { (_, value) -> value }
                                .map { (key, _) -> options[key] }
                        )
                        onDismiss()
                    })
                    .padding(5.dp),
                text = stringResource(id = R.string.confirm),
                color = color
            )
        },
        dismissButton = {
            if (showCancelButton) {
                Text(
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(vertical = 5.dp, horizontal = 8.dp),
                    text = stringResource(id = R.string.cancel)
                )
            }
        },
        shape = shape
    )
}

private val ColorScheme.disableColor: Color
    get() = onSurface.copy(0.12f)

@Preview(widthDp = 360, heightDp = 640)
@Composable
fun SingleChoiceDialogPreview() {
    val options = listOf("Yande", "Konachan", "Dan")
    var selectedIndex by remember { mutableIntStateOf(0) }
    SingleChoiceDialog(
        title = "网站",
        options = options,
        selectedOptionIndex = selectedIndex,
        onDismissRequest = {}
    ) {
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
fun SingleChoiceDialogPreview2() {
    val options = WebsiteOption.entries.toList()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(true) }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Button(
                modifier = Modifier
                    .align(Alignment.Center),
                onClick = { showDialog = true }) {
                Text("测试")
            }
            if (showDialog) {
                SingleChoiceDialog(
                    title = "网站",
                    options = options,
                    selectedOption = WebsiteOption.Yande,
                    onDismiss = {
                        showDialog = false
                    },
                    showConfirmButton = true,
                    barrierDismissible = false
                ) { option ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Selected: ${option.name}")
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
fun MultipleChoiceDialogPreview() {
    val options = listOf("Yande", "Konachan", "Dan")
    val selectedOptions = emptyList<Int>()
    var showDialog by remember { mutableStateOf(true) }

    MultipleChoiceDialog(
        title = stringResource(id = R.string.website),
        options = options,
        selectedOptions = selectedOptions,
        onDismiss = { showDialog = false },
        onConfirm = {

        }
    )
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
fun MultipleChoiceDialogPreview2() {
    val options = WebsiteOption.entries.toList()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(true) }
    val selectedOptions = listOf(
        WebsiteOption.Yande,
        WebsiteOption.Konachan
    )
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Button(
                modifier = Modifier
                    .align(Alignment.Center),
                onClick = { showDialog = true }) {
                Text("测试")
            }
            if (showDialog) {
                MultipleChoiceDialog(
                    title = "网站",
                    options = options,
                    selectedOptions = selectedOptions,
                    onDismiss = {
                        showDialog = false
                    },
                ) { option ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Selected: $option")
                    }
                }
            }
        }
    }
}
