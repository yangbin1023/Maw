package com.magic.maw.ui.search

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magic.maw.R
import com.magic.maw.ui.components.ShowSystemBars
import com.magic.maw.ui.theme.PreviewTheme
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(onFinish: () -> Unit = {}) {
    ShowSystemBars()
    Scaffold(
        topBar = { SearchTopBar(onFinish = onFinish) }
    ) {
        SearchContent(modifier = Modifier.padding(it))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    modifier: Modifier = Modifier,
    onFinish: () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier.shadow(3.dp),
        navigationIcon = {
            IconButton(onClick = onFinish) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "",
                )
            }
        },
        title = {
            Surface {
                SearchTopBar()
            }
        }
    )
}

@Composable
private fun SearchTopBar() {
    var hasFocus by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textStyle = MaterialTheme.typography.bodyMedium
    val textColor = MaterialTheme.colorScheme.onSurface
    val colors = TextFieldDefaults.colors().copy(
        focusedContainerColor = backgroundColor,
        unfocusedContainerColor = backgroundColor,
        errorContainerColor = backgroundColor,
        disabledContainerColor = backgroundColor,
        cursorColor = textColor,
        errorCursorColor = textColor,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    LaunchedEffect(Unit) {
        delay(50)
        focusRequester.requestFocus()
    }
    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { hasFocus = it.isFocused },
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions {
                println("text: $text")
            }
        ) { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1.0f)) {
                    if (text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_tags),
                            color = MaterialTheme.colorScheme.onSurface.copy(0.45f),
                            style = mergedTextStyle
                        )
                    }
                    innerTextField()
                }
                if (hasFocus && text.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null,
                        modifier = Modifier.clickable { text = "" },
                        tint = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {

    }
}

@Composable
@Preview(name = "Search Preview", widthDp = 360, heightDp = 180, uiMode = UI_MODE_NIGHT_NO)
private fun SearchPreview() {
    PreviewTheme {
        SearchScreen()
    }
}

@Composable
@Preview(name = "Drawer contents (dark)", widthDp = 360, heightDp = 180, uiMode = UI_MODE_NIGHT_YES)
private fun SearchPreviewDark() {
    PreviewTheme {
        SearchScreen()
    }
}