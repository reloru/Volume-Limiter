package com.reloru.volumelimiter

object VolumeMath {
    const val MIN_PERCENT = 10
    const val MAX_PERCENT = 100
    const val DEFAULT_PERCENT = 50

    /** Highest stream index allowed for [percent] of [maxIndex]; rounds down, never below 1. */
    fun capIndex(percent: Int, maxIndex: Int): Int {
        val p = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)
        return (p * maxIndex / 100).coerceIn(1, maxIndex.coerceAtLeast(1))
    }

    fun needsLowering(current: Int, cap: Int): Boolean = current > cap
}
