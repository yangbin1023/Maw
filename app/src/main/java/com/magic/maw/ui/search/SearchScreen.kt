package com.magic.maw.ui.search

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magic.maw.R
import com.magic.maw.data.TagInfo
import com.magic.maw.ui.components.ShowSystemBars
import com.magic.maw.ui.components.TagItem
import com.magic.maw.ui.theme.PreviewTheme
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(onFinish: () -> Unit = {}, onSearch: (String) -> Unit = {}) {
    ShowSystemBars()
    val (inputText, setInputText) = remember {
        mutableStateOf(TextFieldValue(text = ""))
    }
    Scaffold(
        topBar = {
            SearchTopBar(
                value = inputText,
                onValueChange = setInputText,
                onFinish = onFinish,
                onSearch = {
                    if (inputText.text.isNotEmpty()) {
                        onSearch(inputText.text)
                    }
                }
            )
        }
    ) { innerPadding ->
        SearchBody(
            modifier = Modifier.padding(innerPadding),
            onTagClick = { tag ->
                val oldText = inputText.text.trimEnd().run { if (isEmpty()) "" else "$this " }
                val text = oldText + "${tag.name} "
                setInputText(TextFieldValue(text = text, selection = TextRange(text.length)))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onFinish: () -> Unit,
    onSearch: () -> Unit,
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
            SearchTopBarTitle(
                value = value,
                onValueChange = onValueChange,
                onSearch = onSearch
            )
        }
    )
}

@Composable
private fun SearchTopBarTitle(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSearch: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
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
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions { onSearch() }
        ) { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1.0f)) {
                    if (value.text.isEmpty()) {
                        Text(
                            modifier = Modifier.padding(horizontal = 3.dp),
                            text = stringResource(R.string.search_tags),
                            color = MaterialTheme.colorScheme.onSurface.copy(0.45f),
                            style = mergedTextStyle
                        )
                    }
                    innerTextField()
                }
                if (hasFocus && value.text.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .clickable { onValueChange(TextFieldValue(text = "")) },
                        tint = textColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchBody(
    modifier: Modifier = Modifier,
    onTagClick: (TagInfo) -> Unit
) {
    var deleteEnable by remember { mutableStateOf(false) }
//    val baseTag = TagInfo(source = "yande", name = "none")
//    val tagList = arrayListOf(
//        baseTag.copy(name = "genshin_impact", type = TagType.Copyright),
//        baseTag.copy(name = "ganyu", type = TagType.Character),
//        baseTag.copy(name = "keqing", type = TagType.Character),
//        baseTag.copy(name = "alpha", type = TagType.Artist),
//        baseTag.copy(name = "girl", type = TagType.General),
//        baseTag.copy(name = "2girl", type = TagType.General),
//        baseTag.copy(name = "other", type = TagType.Circle),
//    ).toMutableStateList()
    val tagList = ArrayList<TagInfo>()
    Box(modifier = modifier.fillMaxSize()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp)
        ) {
            for (tag in tagList) {
                TagItem(
                    text = tag.name,
                    type = tag.type,
                    onClick = { onTagClick(tag) },
                    onLongClick = { deleteEnable = !deleteEnable },
                    deleteEnable = deleteEnable,
                    onDeleteClick = { tagList.remove(tag) }
                )
            }
        }
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