package com.dimaghkharab.guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dimaghkharab.guardian.data.AppDatabase
import com.dimaghkharab.guardian.util.SoundPlayer

class ChargingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val db = AppDatabase.getInstance(context)
        val player = SoundPlayer(context)

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val profile = db.soundProfileDao().getActiveByType("CONNECT")
                if (profile != null) {
                    player.play(profile.filePath, profile.volume / 100f)
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                val profile = db.soundProfileDao().getActiveByType("DISCONNECT")
                if (profile != null) {
                    player.play(profile.filePath, profile.volume / 100f)
                }
            }
        }
    }
}
