package com.dimaghkharab.guardian.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PrefsManager private constructor(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val PREFS_NAME = "dimagh_kharab_secure_prefs"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_PUZZLE_TYPE = "puzzle_type"
        private const val KEY_PUZZLE_ANSWER = "puzzle_answer"
        private const val KEY_PUZZLE_QUESTION = "puzzle_question"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_SERVICE_RUNNING = "service_running"

        @Volatile
        private var instance: PrefsManager? = null

        fun getInstance(context: Context): PrefsManager {
            return instance ?: synchronized(this) {
                instance ?: PrefsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun isPasswordSet(): Boolean {
        return getPasswordHash() != null
    }

    fun setPasswordHash(hash: String) {
        prefs.edit().putString(KEY_PASSWORD_HASH, hash).apply()
    }

    fun getPasswordHash(): String? {
        return prefs.getString(KEY_PASSWORD_HASH, null)
    }

    fun clearPassword() {
        prefs.edit().remove(KEY_PASSWORD_HASH).apply()
    }

    fun setPuzzleType(type: String) {
        prefs.edit().putString(KEY_PUZZLE_TYPE, type).apply()
    }

    fun getPuzzleType(): String {
        return prefs.getString(KEY_PUZZLE_TYPE, "") ?: ""
    }

    fun setPuzzleAnswer(answer: String) {
        prefs.edit().putString(KEY_PUZZLE_ANSWER, answer).apply()
    }

    fun getPuzzleAnswer(): String? {
        return prefs.getString(KEY_PUZZLE_ANSWER, null)
    }

    fun setPuzzleQuestion(question: String) {
        prefs.edit().putString(KEY_PUZZLE_QUESTION, question).apply()
    }

    fun getPuzzleQuestion(): String? {
        return prefs.getString(KEY_PUZZLE_QUESTION, null)
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun isServiceRunning(): Boolean {
        return prefs.getBoolean(KEY_SERVICE_RUNNING, false)
    }

    fun setServiceRunning(running: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_RUNNING, running).apply()
    }
}
