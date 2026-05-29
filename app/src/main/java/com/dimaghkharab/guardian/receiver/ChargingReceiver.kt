package com.dimaghkharab.guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.dimaghkharab.guardian.data.AppDatabase
import com.dimaghkharab.guardian.util.SoundPlayer
import kotlinx.coroutines.runBlocking

class ChargingReceiver : BroadcastReceiver() {

    private var player: SoundPlayer? = null

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        try {
            val db = AppDatabase.getInstance(context)
            player?.release()
            player = SoundPlayer(context)

            runBlocking {
                when (action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        val profile = db.soundProfileDao().getActiveByType("CONNECT")
                        if (profile != null) {
                            player?.play(profile.filePath, profile.volume / 100f)
                        }
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        val profile = db.soundProfileDao().getActiveByType("DISCONNECT")
                        if (profile != null) {
                            player?.play(profile.filePath, profile.volume / 100f)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChargingReceiver", "Failed to play sound", e)
        }
    }
}
