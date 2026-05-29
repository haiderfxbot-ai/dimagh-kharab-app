package com.dimaghkharab.guardian.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dimaghkharab.guardian.R
import com.dimaghkharab.guardian.util.PrefsManager

class SplashActivity : AppCompatActivity() {

    private var prefsManager: PrefsManager? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            supportActionBar?.hide()
            setContentView(R.layout.activity_splash)

            prefsManager = PrefsManager.getInstance(this)

            val splashText = findViewById<View>(R.id.tvSplashTitle)
            val fadeIn = AlphaAnimation(0f, 1f).apply {
                duration = 1500
                fillAfter = true
            }
            splashText.startAnimation(fadeIn)
        } catch (e: Exception) {
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        handler.postDelayed({
            try {
                if (prefsManager?.isPasswordSet() == true) {
                    startActivity(Intent(this, LockActivity::class.java))
                } else {
                    startActivity(Intent(this, SetupActivity::class.java))
                }
            } catch (e: Exception) {
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
