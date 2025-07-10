package com.example.thehub

import java.text.SimpleDateFormat
import java.util.*

fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "vừa xong"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} phút trước"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} giờ trước"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} ngày trước"
        else -> {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

fun formatNumber(number: Int): String {
    return when {
        number < 1000 -> number.toString()
        number < 1000000 -> String.format("%.1fK", number / 1000.0)
        else -> String.format("%.1fM", number / 1000000.0)
    }
}
