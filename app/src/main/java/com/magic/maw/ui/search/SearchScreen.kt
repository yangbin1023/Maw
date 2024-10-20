package com.magic.maw.ui.search

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteForever
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
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
import com.magic.maw.data.TagType
import com.magic.maw.ui.components.ShowSystemBars
import com.magic.maw.ui.components.TagItem
import com.magic.maw.ui.theme.PreviewTheme
import com.magic.maw.util.Logger
import com.magic.maw.util.configFlow
import com.magic.maw.website.TagManager
import com.magic.maw.website.parser.BaseParser
import kotlinx.coroutines.delay

private val logger = Logger("SearchScreen")

@Composable
fun SearchScreen(
    initText: String = "",
    onFinish: () -> Unit = {},
    onSearch: (String) -> Unit = {}
) {
    ShowSystemBars()
    val source = configFlow.collectAsState().value.source
    val tagManager = TagManager.get(source)
    val (inputText, setInputText) = remember {
        val text = initText.toSearchTagText()
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
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
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            SearchBody(
                modifier = Modifier.fillMaxSize(),
                onGetHistory = { tagManager.getHistoryList() },
                onTagClick = { tag ->
                    val text = "${inputText.text.trim()} ${tag.name.trim()}".toSearchTagText()
                    setInputText(TextFieldValue(text = text, selection = TextRange(text.length)))
                },
                onTagDelete = { tagManager.deleteHistory(it.name) },
                onAllTagDelete = { tagManager.deleteAllHistory() }
            )
            SearchSuggestion(
                modifier = Modifier.fillMaxWidth(),
                text = inputText.text,
                onGetHistory = { BaseParser.get(source).requestSuggestTagInfo(it) },
                onItemClick = {
                    val old = inputText.text.substring(0, inputText.text.lastIndexOf(" ") + 1)
                    val new = "${old.trim()} ${it.trim()}".toSearchTagText()
                    setInputText(TextFieldValue(text = new, selection = TextRange(new.length)))
                }
            )
        }
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
    val textStyle = MaterialTheme.typography.bodyLarge
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
    onGetHistory: () -> List<TagInfo>,
    onTagClick: (TagInfo) -> Unit,
    onTagDelete: (TagInfo) -> Unit,
    onAllTagDelete: () -> Unit,
) {
    var deleteEnable by remember { mutableStateOf(false) }
    val tagList = remember { onGetHistory().toMutableStateList() }
    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.padding(horizontal = 10.dp),
                text = stringResource(R.string.search_history),
                color = MaterialTheme.colorScheme.onBackground.copy(0.85f)
            )
            Spacer(modifier = Modifier.weight(1.0f))
            IconButton(
                modifier = Modifier.fillMaxHeight(),
                onClick = {
                    tagList.clear()
                    onAllTagDelete()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            for (tag in tagList) {
                TagItem(
                    text = tag.name,
                    type = tag.type,
                    onClick = { onTagClick(tag) },
                    onLongClick = { deleteEnable = !deleteEnable },
                    deleteEnable = deleteEnable,
                    onDeleteClick = {
                        tagList.remove(tag)
                        onTagDelete(tag)
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchSuggestion(
    modifier: Modifier = Modifier,
    text: String,
    onGetHistory: suspend (String) -> List<TagInfo>,
    onItemClick: (String) -> Unit
) {
    var suggestionList: List<TagInfo> by remember { mutableStateOf(emptyList()) }
    LaunchedEffect(text) {
        if (text.isNotEmpty() && !text.endsWith(" ")) {
            val current = text.substring(text.lastIndexOf(" ") + 1).trim()
            suggestionList = onGetHistory(current)
        } else {
            suggestionList = emptyList()
        }
        logger.info("get suggest, text: $text, list: $suggestionList")
    }
    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colorScheme.onPrimary)
    ) {
        items(suggestionList) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 15.dp, vertical = 5.dp)
                    .clickable { onItemClick(it.name) },
                text = it.name,
                color = it.type.tagColor
            )
        }
    }
}

private fun String.toSearchTagText(): String = trim().run { if (isNotEmpty()) "$this " else "" }

private fun getPreviewTagList(): List<TagInfo> {
    val baseTag = TagInfo(source = "yande", name = "none")
    return listOf(
        baseTag.copy(name = "genshin_impact", type = TagType.Copyright),
        baseTag.copy(name = "ganyu", type = TagType.Character),
        baseTag.copy(name = "keqing", type = TagType.Character),
        baseTag.copy(name = "alpha", type = TagType.Artist),
        baseTag.copy(name = "girl", type = TagType.General),
        baseTag.copy(name = "2girl", type = TagType.General),
        baseTag.copy(name = "other", type = TagType.Circle),
    )
}

@Composable
@Preview(name = "SearchBody", widthDp = 360, heightDp = 180, uiMode = UI_MODE_NIGHT_NO)
private fun SearchPreview() {
    PreviewTheme {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                SearchBody(
                    modifier = Modifier.fillMaxWidth(),
                    onGetHistory = { getPreviewTagList() },
                    onTagDelete = {},
                    onAllTagDelete = {},
                    onTagClick = {})
                SearchSuggestion(
                    modifier = Modifier.fillMaxWidth(),
                    text = "test",
                    onGetHistory = { getPreviewTagList().subList(0, 3) },
                    onItemClick = {})
            }
        }
    }
}

@Composable
@Preview(name = "SearchBody(dark)", widthDp = 360, heightDp = 180, uiMode = UI_MODE_NIGHT_YES)
private fun SearchPreviewDark() {
    PreviewTheme {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                SearchBody(
                    modifier = Modifier.fillMaxWidth(),
                    onGetHistory = { getPreviewTagList() },
                    onTagDelete = {},
                    onAllTagDelete = {},
                    onTagClick = {})
                SearchSuggestion(
                    modifier = Modifier.fillMaxWidth(),
                    text = "test",
                    onGetHistory = { getPreviewTagList().subList(0, 3) },
                    onItemClick = {})
            }
        }
    }
}