package dev.jordanempire.youflow.ui.util

import java.util.Locale

fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0) return ""
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

fun formatCount(count: Long, unit: String): String {
    val text = when {
        count >= 1_000_000_000 -> compact(count / 1_000_000_000.0, "B")
        count >= 1_000_000 -> compact(count / 1_000_000.0, "M")
        count >= 1_000 -> compact(count / 1_000.0, "K")
        else -> count.toString()
    }
    return "$text $unit"
}

private fun compact(value: Double, suffix: String): String {
    val rounded = if (value >= 100) String.format(Locale.US, "%.0f", value)
    else String.format(Locale.US, "%.1f", value).removeSuffix(".0")
    return rounded + suffix
}
