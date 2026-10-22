package com.reloru.volumelimiter

object Lockout {
    private const val FREE_ATTEMPTS = 5
    private const val BASE_SECONDS = 30L
    private const val MAX_SECONDS = 3600L

    /** Seconds to wait after [failures] consecutive wrong passwords; 0 while attempts remain. */
    fun waitSeconds(failures: Int): Long {
        if (failures < FREE_ATTEMPTS) return 0
        val shift = (failures - FREE_ATTEMPTS).coerceAtMost(7)
        return (BASE_SECONDS shl shift).coerceAtMost(MAX_SECONDS)
    }
}
