package com.dimaghkharab.guardian.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import java.io.File

class SoundPlayer(context: Context) {

    private val mediaPlayer: MediaPlayer = MediaPlayer()

    init {
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
    }

    fun play(filePath: String, volume: Float = 1.0f) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, Uri.fromFile(File(filePath)))
            mediaPlayer.setVolume(volume.coerceIn(0.0f, 1.0f), volume.coerceIn(0.0f, 1.0f))
            mediaPlayer.setOnPreparedListener { mp -> mp.start() }
            mediaPlayer.setOnErrorListener { _, what, extra ->
                android.util.Log.e("SoundPlayer", "Error playing: what=$what extra=$extra")
                true
            }
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            android.util.Log.e("SoundPlayer", "Failed to play audio", e)
        }
    }

    fun stop() {
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.reset()
        } catch (e: Exception) {
            android.util.Log.e("SoundPlayer", "Failed to stop", e)
        }
    }

    fun release() {
        try {
            mediaPlayer.release()
        } catch (e: Exception) {
            android.util.Log.e("SoundPlayer", "Failed to release", e)
        }
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer.isPlaying
        } catch (e: Exception) {
            false
        }
    }
}
