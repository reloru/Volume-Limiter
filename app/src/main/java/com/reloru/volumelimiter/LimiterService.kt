package com.reloru.volumelimiter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings

class LimiterService : Service() {
    private lateinit var audio: AudioManager
    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())

    private val poll = object : Runnable {
        override fun run() {
            enforce()
            handler.postDelayed(this, POLL_MS)
        }
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = enforce()
    }

    private val settingsObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = enforce()
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = enforce()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = enforce()
    }

    override fun onCreate() {
        super.onCreate()
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        prefs = Prefs(this)
        val filter = IntentFilter(VOLUME_CHANGED_ACTION)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(volumeReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(volumeReceiver, filter)
        }
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, settingsObserver)
        audio.registerAudioDeviceCallback(deviceCallback, handler)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        handler.removeCallbacks(poll)
        poll.run()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        unregisterReceiver(volumeReceiver)
        contentResolver.unregisterContentObserver(settingsObserver)
        audio.unregisterAudioDeviceCallback(deviceCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun enforce() {
        val outputs = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val bluetooth = outputs.any { it.type in BLUETOOTH_TYPES }
        val sco = outputs.any { it.type == TYPE_BLUETOOTH_SCO }
        val percent = prefs.capPercent
        if (bluetooth || prefs.limitAllOutputs) clamp(AudioManager.STREAM_MUSIC, percent)
        if (sco) clamp(AudioManager.STREAM_VOICE_CALL, percent)
    }

    private fun clamp(stream: Int, percent: Int) {
        val cap = VolumeMath.capIndex(percent, audio.getStreamMaxVolume(stream))
        if (VolumeMath.needsLowering(audio.getStreamVolume(stream), cap)) {
            audio.setStreamVolume(stream, cap, 0)
        }
    }

    private fun startInForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Volume limit", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Volume limit active")
            .setContentText("Bluetooth volume is capped at ${prefs.capPercent}%")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "limit"
        private const val NOTIFICATION_ID = 1
        private const val POLL_MS = 1000L
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"

        // AudioDeviceInfo types: BLUETOOTH_SCO=7, BLUETOOTH_A2DP=8, HEARING_AID=23, BLE_HEADSET=26,
        // BLE_SPEAKER=27, BLE_BROADCAST=30 (the newer ones are not constants on API 26).
        private const val TYPE_BLUETOOTH_SCO = 7
        private val BLUETOOTH_TYPES = setOf(7, 8, 23, 26, 27, 30)

        fun start(context: Context) {
            context.startForegroundService(Intent(context, LimiterService::class.java))
        }
    }
}
