package com.example.thehub

// Helper function để format thời gian - dùng chung cho toàn app
fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Vừa xong"
        diff < 3600000 -> "${diff / 60000} phút"
        diff < 86400000 -> "${diff / 3600000} giờ"
        else -> "${diff / 86400000} ngày"
    }
}
