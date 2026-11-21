package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object MediaFormatters {
    private val localeEs = Locale("es", "ES")

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format(localeEs, "%.2f %s", value, units[digitGroups])
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "0:00"
        val totalSeconds = durationMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatFullDateTime(millis: Long): String {
        val formatter = SimpleDateFormat("dd MMM yyyy · HH:mm", localeEs)
        return formatter.format(Date(millis))
    }

    fun formatMonthYear(millis: Long): String {
        val formatter = SimpleDateFormat("MMMM yyyy", localeEs)
        return formatter.format(Date(millis)).replaceFirstChar { it.uppercase(localeEs) }
    }

    fun formatDayHeader(millis: Long): String {
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(target, today) -> "Hoy"
            isSameDay(target, yesterday) -> "Ayer"
            target.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> {
                SimpleDateFormat("EEEE, d 'de' MMMM", localeEs)
                    .format(Date(millis))
                    .replaceFirstChar { it.uppercase(localeEs) }
            }
            else -> {
                SimpleDateFormat("d 'de' MMMM yyyy", localeEs)
                    .format(Date(millis))
                    .replaceFirstChar { it.uppercase(localeEs) }
            }
        }
    }

    fun formatShortScrubberLabel(millis: Long): String {
        return SimpleDateFormat("MMM yyyy", localeEs)
            .format(Date(millis))
            .replaceFirstChar { it.uppercase(localeEs) }
    }

    fun formatResolution(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return "Desconocida"
        val mp = (width.toLong() * height.toLong()) / 1_000_000.0
        return if (mp >= 0.5) {
            String.format(localeEs, "%d × %d px (%.1f MP)", width, height, mp)
        } else {
            "$width × $height px"
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}
