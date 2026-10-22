package com.reloru.volumelimiter

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test fun acceptsCorrectAndRejectsWrongPassword() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("hunter2", salt)
        assertTrue(PinHasher.verify("hunter2", salt, hash))
        assertFalse(PinHasher.verify("hunter3", salt, hash))
    }

    @Test fun saltChangesHash() {
        val a = PinHasher.hash("same", PinHasher.newSalt())
        val b = PinHasher.hash("same", PinHasher.newSalt())
        assertNotEquals(PinHasher.toHex(a), PinHasher.toHex(b))
    }

    @Test fun hexRoundTrips() {
        val bytes = byteArrayOf(0, 1, 127, -1, -128)
        assertArrayEquals(bytes, PinHasher.fromHex(PinHasher.toHex(bytes)))
        assertEquals("0001 7f ff 80".replace(" ", ""), PinHasher.toHex(bytes))
    }
}
