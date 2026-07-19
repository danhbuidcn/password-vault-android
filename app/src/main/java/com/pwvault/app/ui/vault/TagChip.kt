package com.pwvault.app.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pwvault.app.domain.Tag
import com.pwvault.app.ui.theme.tagColor

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
        label = { Text(tag.name) },
        leadingIcon = { TagColorDot(tag.id) },
        modifier = modifier,
    )
}
