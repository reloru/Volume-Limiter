package com.reloru.volumelimiter

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        setContentView(android.widget.ScrollView(this).apply { addView(root) })
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        LimiterService.start(this)
    }

    override fun onStart() {
        super.onStart()
        if (prefs.hasPassword) showLocked() else showSetPassword(first = true)
    }

    override fun onStop() {
        super.onStop()
        root.removeAllViews()
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 24f
        setPadding(0, 0, 0, 24)
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        textSize = 16f
        setPadding(0, 8, 0, 16)
    }

    private fun passwordField(hint: String) = EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        setOnClickListener { onClick() }
    }

    private fun showSetPassword(first: Boolean) {
        root.removeAllViews()
        root.addView(title(if (first) "Set a parent password" else "Change password"))
        root.addView(body("This password is required to change the volume limit. Until it is set, the limit stays at ${prefs.capPercent}%."))
        val one = passwordField("New password (4+ characters)")
        val two = passwordField("Repeat password")
        root.addView(one)
        root.addView(two)
        root.addView(button("Save password") {
            val a = one.text.toString()
            val b = two.text.toString()
            when {
                a.length < 4 -> toast("Use at least 4 characters")
                a != b -> toast("Passwords do not match")
                else -> {
                    prefs.setPassword(a)
                    toast("Password saved")
                    showUnlocked()
                }
            }
        })
    }

    private fun showLocked() {
        root.removeAllViews()
        root.addView(title("Volume Limiter"))
        root.addView(body("Bluetooth volume is capped at ${prefs.capPercent}%. Enter the parent password to change it."))
        val field = passwordField("Password")
        root.addView(field)
        root.addView(button("Unlock") {
            val now = System.currentTimeMillis()
            val wait = (prefs.lockedUntil - now) / 1000
            if (wait > 0) {
                toast("Too many tries. Wait $wait seconds.")
            } else if (prefs.checkPassword(field.text.toString())) {
                prefs.failures = 0
                showUnlocked()
            } else {
                val failures = prefs.failures + 1
                prefs.failures = failures
                prefs.lockedUntil = now + Lockout.waitSeconds(failures) * 1000
                toast("Wrong password")
                field.text.clear()
            }
        })
    }

    private fun showUnlocked() {
        root.removeAllViews()
        root.addView(title("Volume limit"))
        val label = body("")
        val seek = SeekBar(this).apply {
            max = (VolumeMath.MAX_PERCENT - VolumeMath.MIN_PERCENT) / STEP
            progress = (prefs.capPercent - VolumeMath.MIN_PERCENT) / STEP
        }
        fun percentOf(progress: Int) = VolumeMath.MIN_PERCENT + progress * STEP
        label.text = "Maximum volume: ${percentOf(seek.progress)}%"
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                label.text = "Maximum volume: ${percentOf(p)}%"
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })
        val all = CheckBox(this).apply {
            text = "Also limit the tablet speaker and wired headphones"
            isChecked = prefs.limitAllOutputs
        }
        root.addView(label)
        root.addView(seek)
        root.addView(all)
        root.addView(button("Save limit") {
            prefs.capPercent = percentOf(seek.progress)
            prefs.limitAllOutputs = all.isChecked
            LimiterService.start(this)
            toast("Limit saved")
            showLocked()
        })
        root.addView(button("Change password") { showSetPassword(first = false) })
        root.addView(button("Lock") { showLocked() })
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    companion object {
        private const val STEP = 5
    }
}
