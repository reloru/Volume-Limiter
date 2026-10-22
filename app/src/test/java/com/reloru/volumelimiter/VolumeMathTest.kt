package com.reloru.volumelimiter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeMathTest {
    @Test fun capRoundsDownAndNeverExceedsPercent() {
        assertEquals(7, VolumeMath.capIndex(50, 15))
        assertEquals(15, VolumeMath.capIndex(100, 15))
        assertEquals(11, VolumeMath.capIndex(75, 15))
    }

    @Test fun capNeverBelowOneOrAboveMax() {
        assertEquals(1, VolumeMath.capIndex(10, 5))
        assertEquals(1, VolumeMath.capIndex(0, 15))
        assertEquals(15, VolumeMath.capIndex(500, 15))
    }

    @Test fun lowersOnlyWhenAboveCap() {
        assertTrue(VolumeMath.needsLowering(8, 7))
        assertFalse(VolumeMath.needsLowering(7, 7))
        assertFalse(VolumeMath.needsLowering(3, 7))
    }
}
