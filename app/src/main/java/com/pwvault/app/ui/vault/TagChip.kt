package com.pwvault.app.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pwvault.app.domain.Tag
import com.pwvault.app.ui.theme.tagColor

private val TAG_CHIP_MAX_LABEL_WIDTH = 96.dp

@Composable
fun TagColorDot(
    tagId: Long,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(10.dp).clip(CircleShape).background(tagColor(tagId)))
}

/** Read-only chip used to display a tag already assigned to a Vault Item (list row, detail screen). */
@Composable
fun TagChip(
    tag: Tag,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = tag.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = TAG_CHIP_MAX_LABEL_WIDTH),
            )
        },
        leadingIcon = { TagColorDot(tag.id) },
        modifier = modifier,
    )
}
