package com.saferoute.ai.util

import java.util.Locale

// ============================================================

fun formatDistance(
    meters: Double
): String {

    return if (meters < 1000) {
        "${meters.toInt()} m"
    } else {
        String.format(
            Locale.US,
            "%.1f km",
            meters / 1000.0
        )
    }
}

// ============================================================
// ============================================================

fun formatDuration(
    seconds: Double
): String {

    val minutes =
        (seconds / 60.0).toInt()

    return if (minutes < 60) {

        "$minutes min"

    } else {

        val hours =
            minutes / 60

        val remaining =
            minutes % 60

        if (remaining == 0) {
            "$hours hr"
        } else {
            "$hours hr $remaining min"
        }
    }
}
