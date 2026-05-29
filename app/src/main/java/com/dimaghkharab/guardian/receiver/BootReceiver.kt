package com.dimaghkharab.guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dimaghkharab.guardian.service.GuardianService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, GuardianService::class.java)
            context.startForegroundService(serviceIntent)
            Log.d(TAG, "Service started after boot")
        }
    }
}
