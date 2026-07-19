package com.pwvault.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Fixed, deterministic color per tag — no per-tag color column in the DB, no manual color picker. */
private val TAG_COLORS =
    listOf(
        Color(0xFFE57373), // red
        Color(0xFFFFB74D), // orange
        Color(0xFFFFF176), // yellow
        Color(0xFF81C784), // green
        Color(0xFF4DB6AC), // teal
        Color(0xFF64B5F6), // blue
        Color(0xFF9575CD), // purple
        Color(0xFFF06292), // pink
    )

fun tagColor(tagId: Long): Color = TAG_COLORS[(tagId % TAG_COLORS.size).toInt()]
