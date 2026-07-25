package com.pwvault.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Fixed, deterministic color per tag — no per-tag color column in the DB, no manual color picker. */
private val TAG_COLORS =
    listOf(
        Color(0xFFE0725A), // coral
        Color(0xFFD2A23F), // marigold
        Color(0xFF6B9080), // moss
        Color(0xFF4C93A6), // teal
        Color(0xFF5B6FD9), // indigo
        Color(0xFF8B5FA8), // plum
        Color(0xFFC1638A), // rose
        Color(0xFF7B8AA8), // slate
    )

fun tagColor(tagId: Long): Color = TAG_COLORS[(tagId % TAG_COLORS.size).toInt()]
