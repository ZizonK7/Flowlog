package com.example.flowlog.notification

object FlowlogVibrationPatterns {
    fun alert(): LongArray = longArrayOf(0L, 2_400L)

    fun alertAmplitudes(): IntArray = intArrayOf(0, 255)
}
