package com.reloru.volumelimiter

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("limiter", Context.MODE_PRIVATE)

    var capPercent: Int
        get() = sp.getInt("cap_percent", VolumeMath.DEFAULT_PERCENT)
            .coerceIn(VolumeMath.MIN_PERCENT, VolumeMath.MAX_PERCENT)
        set(v) = sp.edit().putInt("cap_percent", v).apply()

    /** When true the cap also applies to the speaker and wired headphones, not only Bluetooth. */
    var limitAllOutputs: Boolean
        get() = sp.getBoolean("limit_all", false)
        set(v) = sp.edit().putBoolean("limit_all", v).apply()

    val hasPassword: Boolean get() = sp.contains("pw_hash")

    fun setPassword(password: String) {
        val salt = PinHasher.newSalt()
        sp.edit()
            .putString("pw_salt", PinHasher.toHex(salt))
            .putString("pw_hash", PinHasher.toHex(PinHasher.hash(password, salt)))
            .putInt("failures", 0)
            .apply()
    }

    fun checkPassword(password: String): Boolean {
        val salt = sp.getString("pw_salt", null) ?: return false
        val hash = sp.getString("pw_hash", null) ?: return false
        return PinHasher.verify(password, PinHasher.fromHex(salt), PinHasher.fromHex(hash))
    }

    var failures: Int
        get() = sp.getInt("failures", 0)
        set(v) = sp.edit().putInt("failures", v).apply()

    var lockedUntil: Long
        get() = sp.getLong("locked_until", 0L)
        set(v) = sp.edit().putLong("locked_until", v).apply()
}
