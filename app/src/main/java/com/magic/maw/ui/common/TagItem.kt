package com.magic.maw.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magic.maw.R
import com.magic.maw.data.model.constant.TagType

@Composable
fun TagItem(
    modifier: Modifier = Modifier,
    type: TagType = TagType.None,
    text: String,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    deleteEnable: Boolean = false,
    onDeleteClick: (() -> Unit)? = null
) {
    Box(modifier = modifier.padding(vertical = 2.dp, horizontal = 5.dp)) {
        val boxStartPadding = TagItemDefaults.itemHeight / 3f
        val boxEndPadding = if (deleteEnable) {
            (TagItemDefaults.itemHeight - TagItemDefaults.iconSize) / 2
        } else {
            boxStartPadding
        }
        val textEndPadding = if (deleteEnable) 5.dp else 0.dp
        val tagModifier = if (onClick != null || onLongClick != null) {
            Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick ?: {}
            )
        } else {
            Modifier
        }
        Row(
            modifier = tagModifier
                .background(type.tagColor, shape = CircleShape)
                .padding(start = boxStartPadding, end = boxEndPadding)
                .height(TagItemDefaults.itemHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(end = textEndPadding, bottom = 1.dp),
                color = Color.White,
                text = text.replace("_", " "),
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
            if (deleteEnable) {
                Icon(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDeleteClick ?: {}
                        )
                        .size(TagItemDefaults.iconSize),
                    painter = painterResource(id = R.drawable.ic_tag_delete),
                    tint = Color.White,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
@Preview
private fun TagItemPreview() {
    Column {
        TagItem(text = "genshin_impact")
        TagItem(text = "genshin_impact", type = TagType.General)
        TagItem(text = "genshin_impact", type = TagType.Artist)
        TagItem(text = "genshin_impact", type = TagType.Copyright)
        TagItem(text = "genshin_impact", type = TagType.Character)
        TagItem(text = "genshin_impact", type = TagType.Circle)
        TagItem(text = "genshin_impact", type = TagType.Faults)
        TagItem(text = "genshin_impact", type = TagType.Faults, deleteEnable = true)
    }
}

object TagItemDefaults {
    val itemHeight: Dp = 26.dp
    val iconSize: Dp = 20.dp
}