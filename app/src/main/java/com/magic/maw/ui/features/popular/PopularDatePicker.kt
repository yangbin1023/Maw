package com.magic.maw.ui.features.popular

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magic.maw.data.model.constant.PopularType
import com.magic.maw.ui.common.throttle
import com.magic.maw.ui.theme.PreviewTheme
import com.magic.maw.util.format
import com.magic.maw.util.toMonday
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun PopularDatePicker(
    modifier: Modifier = Modifier,
    popularType: PopularType = PopularType.Day,
    focusDate: LocalDate,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(0.85f),
    backgroundShape: Shape = CircleShape,
    onDateChanged: (LocalDate) -> Unit
) {
    val onDateDown = {
        val newDate = when (popularType) {
            PopularType.Day -> focusDate.minusDays(1)
            PopularType.Week -> focusDate.minusWeeks(1)
            PopularType.Month -> focusDate.minusMonths(1)
            PopularType.Year -> focusDate.minusYears(1)
            else -> focusDate
        }
        onDateChanged(newDate)
    }
    val onDateUp = {
        val newDate = when (popularType) {
            PopularType.Day -> focusDate.plusDays(1)
            PopularType.Week -> focusDate.plusWeeks(1)
            PopularType.Month -> focusDate.plusMonths(1)
            PopularType.Year -> focusDate.plusYears(1)
            else -> focusDate
        }
        onDateChanged(newDate)
    }
    val text = getFormatStr(focusDate, popularType)
    if (text.isEmpty()) {
        return
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = Defaults.minHeight)
            .padding(bottom = Defaults.bottomMargin)
            .background(color = backgroundColor, shape = backgroundShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tintColor = MaterialTheme.colorScheme.onSurface
        IconButton(onClick = throttle(func = onDateDown), enabled = enabled) {
            Icon(
                imageVector = Icons.Default.KeyboardDoubleArrowLeft,
                contentDescription = null,
                modifier = Modifier.defaultMinSize(Defaults.iconSize, Defaults.iconSize),
                tint = tintColor
            )
        }

        Text(
            text = text,
            color = tintColor,
            style = MaterialTheme.typography.bodyLarge
        )

        IconButton(onClick = throttle(func = onDateUp), enabled = enabled) {
            Icon(
                imageVector = Icons.Default.KeyboardDoubleArrowRight,
                contentDescription = null,
                modifier = Modifier.defaultMinSize(Defaults.iconSize, Defaults.iconSize),
                tint = tintColor
            )
        }
    }
}

private fun getFormatStr(focusDate: LocalDate, popularType: PopularType): String {
    return when (popularType) {
        PopularType.Day -> focusDate.format()
        PopularType.Week -> {
            val monday = focusDate.toMonday().format()
            val sunDay = focusDate.plusWeeks(1).toMonday().format()
            "$monday - $sunDay"
        }

        PopularType.Month -> focusDate.format(DateTimeFormatter.ofPattern("yyyy/M"))
        PopularType.Year -> focusDate.format(DateTimeFormatter.ofPattern("yyyy"))
        else -> ""
    }
}

private object Defaults {
    val minHeight = 42.dp
    val bottomMargin = 42.dp
    val iconSize = 30.dp
}

@Composable
@Preview(widthDp = 360, heightDp = 320)
private fun DatePickerPreview() {
    val focusDate = remember { mutableStateOf(LocalDate.now()) }
    PreviewTheme {
        Surface {
            Box(modifier = Modifier.fillMaxSize()) {
                PopularDatePicker(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    popularType = PopularType.Day,
                    focusDate = focusDate.value,
                    backgroundShape = RectangleShape,
                    onDateChanged = { focusDate.value = it }
                )
            }
        }
    }
}

@Composable
@Preview(widthDp = 360, heightDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun DatePickerPreviewDark() {
    val focusDate = remember { mutableStateOf(LocalDate.now()) }
    PreviewTheme {
        Surface {
            Box(modifier = Modifier.fillMaxSize()) {
                PopularDatePicker(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    popularType = PopularType.Month,
                    focusDate = focusDate.value,
                    onDateChanged = { focusDate.value = it }
                )
            }
        }
    }
}