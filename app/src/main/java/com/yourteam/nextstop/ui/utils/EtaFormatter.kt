package com.yourteam.nextstop.ui.utils

fun formatEta(minutes: Int): String = when {
    minutes == -2  -> "Arrived/Passed"
    minutes < 0    -> "Calculating..."
    minutes < 60   -> "$minutes min"
    minutes < 120  -> "1 hr ${minutes - 60} min"
    else           -> "Bus is far away"
}
