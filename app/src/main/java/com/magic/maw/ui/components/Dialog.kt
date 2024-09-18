package com.magic.maw.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magic.maw.R

@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedOptionIndex: Int,
    onDismissRequest: () -> Unit,
    onOptionSelected: (Int) -> Unit
) {
    AlertDialog(
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
                modifier = Modifier.clickable(onClick = throttle(func = onDismissRequest)),
                text = stringResource(R.string.confirm)
            )
        },
        shape = RoundedCornerShape(10.dp)
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
    AlertDialog(
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
        selectedIndex = it
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