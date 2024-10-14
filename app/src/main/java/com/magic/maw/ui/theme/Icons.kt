package com.magic.maw.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val TableLayout: ImageVector
    get() {
        if (_tableLayout != null) {
            return _tableLayout!!
        }
        val lineWidth = 2f
        _tableLayout = materialIcon(name = "Filled.TableLayout") {
            materialPath {
                moveTo(3.0f, 3.0f + lineWidth)
                curveTo(3.0f, 3.0f + lineWidth, 3.0f, 3.0f, 3.0f + lineWidth, 3.0f)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                curveTo(11.0f - lineWidth, 3.0f, 11f, 3f, 11.0f, 3.0f + lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth)
                curveTo(11.0f, 11.0f - lineWidth, 11f, 11f, 11.0f - lineWidth, 11.0f)
                horizontalLineToRelative(-(8.0f - 2 * lineWidth))
                curveTo(3.0f + lineWidth, 11.0f, 3.0f, 11f, 3.0f, 11.0f - lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth))
                horizontalLineToRelative(lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth))
                close()
                moveTo(13.0f, 3.0f + lineWidth)
                curveTo(13.0f, 3.0f + lineWidth, 13.0f, 3.0f, 13.0f + lineWidth, 3.0f)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                curveTo(21.0f - lineWidth, 3.0f, 21f, 3f, 21.0f, 3.0f + lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth)
                curveTo(21.0f, 11.0f - lineWidth, 21f, 11f, 21.0f - lineWidth, 11.0f)
                horizontalLineToRelative(-(8.0f - 2 * lineWidth))
                curveTo(13.0f + lineWidth, 11.0f, 13.0f, 11f, 13.0f, 11.0f - lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth))
                horizontalLineToRelative(lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth))
                close()
                moveTo(3.0f, 13.0f + lineWidth)
                curveTo(3.0f, 13.0f + lineWidth, 3.0f, 13.0f, 3.0f + lineWidth, 13.0f)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                curveTo(11.0f - lineWidth, 13.0f, 11f, 13f, 11.0f, 13.0f + lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth)
                curveTo(11.0f, 21.0f - lineWidth, 11f, 21f, 11.0f - lineWidth, 21.0f)
                horizontalLineToRelative(-(8.0f - 2 * lineWidth))
                curveTo(3.0f + lineWidth, 21.0f, 3.0f, 21f, 3.0f, 21.0f - lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth))
                horizontalLineToRelative(lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth))
                close()
                moveTo(13.0f, 13.0f + lineWidth)
                curveTo(13.0f, 13.0f + lineWidth, 13.0f, 13.0f, 13.0f + lineWidth, 13.0f)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                curveTo(21.0f - lineWidth, 13.0f, 21f, 13f, 21.0f, 13.0f + lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth)
                curveTo(21.0f, 21.0f - lineWidth, 21f, 21f, 21.0f - lineWidth, 21.0f)
                horizontalLineToRelative(-(8.0f - 2 * lineWidth))
                curveTo(13.0f + lineWidth, 21.0f, 13.0f, 21f, 13.0f, 21.0f - lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth))
                horizontalLineToRelative(lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth))
                close()
            }
        }
        return _tableLayout!!
    }

private var _tableLayout: ImageVector? = null

@Preview(widthDp = 120, heightDp = 120)
@Composable
fun TableLayoutPreview() {
    Icon(
        imageVector = TableLayout,
        contentDescription = "",
        modifier = Modifier.fillMaxSize(),
    )
}

val WaterLayout: ImageVector
    get() {
        if (_waterLayout != null) {
            return _waterLayout!!
        }
        val lineWidth = 2f
        val offset = 1.5f
        _waterLayout = materialIcon(name = "Filled.TableLayout") {
            materialPath {
                moveTo(3.0f, 3.0f + lineWidth)
                curveTo(3.0f, 3.0f + lineWidth, 3.0f, 3.0f, 3.0f + lineWidth, 3.0f)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                curveTo(11.0f - lineWidth, 3.0f, 11f, 3f, 11.0f, 3.0f + lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth + offset)
                curveTo(11.0f, 11.0f - lineWidth + offset, 11f, 11f + offset, 11.0f - lineWidth, 11.0f + offset)
                horizontalLineToRelative(-(8.0f - 2 * lineWidth))
                curveTo(3.0f + lineWidth, 11.0f + offset, 3.0f, 11f + offset, 3.0f, 11.0f - lineWidth + offset)
                verticalLineToRelative(-(8.0f - 2 * lineWidth + offset))
                horizontalLineToRelative(lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth + offset)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth + offset))
                close()
                moveTo(13.0f, 3.0f + lineWidth)
                curveTo(13.0f, 3.0f + lineWidth, 13.0f, 3.0f, 13.0f + lineWidth, 3.0f)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                curveTo(21.0f - lineWidth, 3.0f, 21f, 3f, 21.0f, 3.0f + lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth - offset)
                curveTo(21.0f, 11.0f - lineWidth - offset, 21f, 11f - offset, 21.0f - lineWidth, 11.0f - offset)
                horizontalLineToRelative(-(8.0f - 2 * lineWidth))
                curveTo(13.0f + lineWidth, 11.0f - offset, 13.0f, 11f - offset, 13.0f, 11.0f - lineWidth - offset)
                verticalLineToRelative(-(8.0f - 2 * lineWidth - offset))
                horizontalLineToRelative(lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth - offset)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth - offset))
                close()
                moveTo(3.0f, 13.0f + lineWidth + offset)
                curveTo(3.0f, 13.0f + lineWidth + offset, 3.0f, 13.0f + offset, 3.0f + lineWidth, 13.0f + offset)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                curveTo(11.0f - lineWidth, 13.0f + offset, 11f, 13f + offset, 11.0f, 13.0f + lineWidth + offset)
                verticalLineToRelative(8.0f - 2 * lineWidth - offset)
                curveTo(11.0f, 21.0f - lineWidth, 11f, 21f, 11.0f - lineWidth, 21.0f)
                horizontalLineToRelative(-(8.0f - 2 * lineWidth))
                curveTo(3.0f + lineWidth, 21.0f, 3.0f, 21f, 3.0f, 21.0f - lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth - offset))
                horizontalLineToRelative(lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth - offset)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth - offset))
                close()
                moveTo(13.0f, 13.0f + lineWidth - offset)
                curveTo(13.0f, 13.0f + lineWidth - offset, 13.0f, 13.0f - offset, 13.0f + lineWidth, 13.0f - offset)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                curveTo(21.0f - lineWidth, 13.0f - offset, 21f, 13f - offset, 21.0f, 13.0f + lineWidth - offset)
                verticalLineToRelative(8.0f - 2 * lineWidth + offset)
                curveTo(21.0f, 21.0f - lineWidth, 21f, 21f, 21.0f - lineWidth, 21.0f)
                horizontalLineToRelative(-(8.0f - 2 * lineWidth))
                curveTo(13.0f + lineWidth, 21.0f, 13.0f, 21f, 13.0f, 21.0f - lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth + offset))
                horizontalLineToRelative(lineWidth)
                verticalLineToRelative(8.0f - 2 * lineWidth + offset)
                horizontalLineToRelative(8.0f - 2 * lineWidth)
                verticalLineToRelative(-(8.0f - 2 * lineWidth + offset))
                close()
            }
        }
        return _waterLayout!!
    }

private var _waterLayout: ImageVector? = null

@Preview(widthDp = 120, heightDp = 120)
@Composable
fun WaterLayoutPreview() {
    Icon(
        imageVector = WaterLayout,
        contentDescription = "",
        modifier = Modifier.fillMaxSize(),
    )
}