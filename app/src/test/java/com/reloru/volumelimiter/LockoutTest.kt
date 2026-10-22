package com.reloru.volumelimiter

import org.junit.Assert.assertEquals
import org.junit.Test

class LockoutTest {
    @Test fun freeAttemptsThenBackoffCappedAtOneHour() {
        assertEquals(0L, Lockout.waitSeconds(0))
        assertEquals(0L, Lockout.waitSeconds(4))
        assertEquals(30L, Lockout.waitSeconds(5))
        assertEquals(60L, Lockout.waitSeconds(6))
        assertEquals(3600L, Lockout.waitSeconds(50))
    }
}
