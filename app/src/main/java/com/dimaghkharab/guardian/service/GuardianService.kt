package com.dimaghkharab.guardian.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dimaghkharab.guardian.data.AppDatabase
import com.dimaghkharab.guardian.receiver.ChargingReceiver
import com.dimaghkharab.guardian.util.SoundPlayer
import java.util.Calendar

class GuardianService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.dimaghkharab.guardian.START_SERVICE"
        const val ACTION_STOP = "com.dimaghkharab.guardian.STOP_SERVICE"
        private const val TAG = "GuardianService"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private var chargingReceiver: ChargingReceiver? = null

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkBatteryThresholds()
            handler.postDelayed(this, 60_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GuardianService:WakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L)

        Log.d(TAG, "Service created, foreground with notification")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (chargingReceiver == null) {
                    chargingReceiver = ChargingReceiver()
                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_POWER_CONNECTED)
                        addAction(Intent.ACTION_POWER_DISCONNECTED)
                    }
                    registerReceiver(chargingReceiver, filter)
                }
                handler.post(checkRunnable)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        chargingReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        SoundPlayer(this).stop()
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "guardian_channel",
            "Guardian Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "guardian_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Dimagh Kharab Active")
            .setContentText("Monitoring battery & charging")
            .setOngoing(true)
            .build()
    }

    private fun checkBatteryThresholds() {
        try {
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra("level", 0) ?: return
            val db = AppDatabase.getDatabase(this)
            val thresholds = db.batteryThresholdDao().getActiveThresholds()
            val now = System.currentTimeMillis()
            val todayStart = getTodayStartMillis()

            for (threshold in thresholds) {
                if (threshold.percentage == level && !threshold.triggeredToday) {
                    SoundPlayer(this).play(threshold.filePath, threshold.volume / 100f)
                    threshold.triggeredToday = true
                    db.batteryThresholdDao().update(threshold)
                    scheduleMidnightReset(threshold.id)
                } else if (threshold.percentage != level && threshold.triggeredToday) {
                    if (threshold.lastTriggeredAt < todayStart) {
                        threshold.triggeredToday = false
                        db.batteryThresholdDao().update(threshold)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery thresholds", e)
        }
    }

    private fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun scheduleMidnightReset(thresholdId: Long) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val intent = Intent(this, GuardianService::class.java).apply {
            action = "com.dimaghkharab.guardian.RESET_THRESHOLD"
            putExtra("threshold_id", thresholdId)
        }
        val pendingIntent = PendingIntent.getService(
            this,
            thresholdId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
    }
}
