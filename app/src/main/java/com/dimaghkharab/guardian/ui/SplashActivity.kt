package com.dimaghkharab.guardian.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import androidx.appcompat.app.AppCompatActivity
import com.dimaghkharab.guardian.R
import com.dimaghkharab.guardian.util.PrefsManager

class SplashActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)

        prefsManager = PrefsManager.getInstance(this)

        val splashText = findViewById<View>(R.id.tvSplashTitle)
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1500
            fillAfter = true
        }
        splashText.startAnimation(fadeIn)

        handler.postDelayed({
            if (prefsManager.isPasswordSet()) {
                startActivity(Intent(this, LockActivity::class.java))
            } else {
                startActivity(Intent(this, SetupActivity::class.java))
            }
            finish()
        }, 3000)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
