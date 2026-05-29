package com.dimaghkharab.guardian.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import com.dimaghkharab.guardian.data.AppDatabase
import com.dimaghkharab.guardian.data.entity.UsageLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccessibilityWatcher : AccessibilityService() {

    companion object {
        private const val TAG = "AccessibilityWatcher"
    }

    private var previousPackage: String? = null
    private var startTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val appName = getAppName(packageName)
        val now = System.currentTimeMillis()

        if (previousPackage != null && previousPackage != packageName) {
            val durationSeconds = (now - startTime) / 1000
            val usageLog = UsageLog(
                startTime = startTime,
                endTime = now,
                durationSeconds = durationSeconds,
                packageName = previousPackage!!,
                appName = getAppName(previousPackage!!),
                date = getTodayDate()
            )
            AppDatabase.getInstance(this).usageLogDao().insert(usageLog)
        }

        if (currentPackageChanged(packageName)) {
            previousPackage = packageName
            startTime = now
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun currentPackageChanged(currentPackage: String): Boolean {
        if (previousPackage != currentPackage) {
            return true
        }
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed > 5_000) {
            startTime = System.currentTimeMillis()
        }
        return previousPackage != currentPackage
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
