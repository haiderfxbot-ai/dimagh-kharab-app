package com.dimaghkharab.guardian.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.dimaghkharab.guardian.util.PrefsManager
import org.mindrot.jbcrypt.BCrypt

class LockActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager
    private lateinit var etPin: EditText
    private lateinit var tvAttempts: TextView
    private lateinit var tvForgot: TextView
    private lateinit var btnUnlock: Button
    private lateinit var keypadContainer: View

    private var attemptCount = 0
    private val maxAttempts = 5
    private var isLockedOut = false

    private var biometricPrompt: BiometricPrompt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        prefsManager = PrefsManager.getInstance(this)

        etPin = findViewById(R.id.etPin)
        tvAttempts = findViewById(R.id.tvAttempts)
        tvForgot = findViewById(R.id.tvForgot)
        btnUnlock = findViewById(R.id.btnUnlock)
        keypadContainer = findViewById(R.id.keypadContainer)

        etPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        updateAttemptsDisplay()

        setupKeypad()
        setupBiometric()

        btnUnlock.setOnClickListener { attemptUnlock() }

        tvForgot.setOnClickListener { showForgotDialog() }
    }

    private fun setupKeypad() {
        val keypadLayout = keypadContainer as? ViewGroup ?: return

        for (i in 0 until keypadLayout.childCount) {
            val child = keypadLayout.getChildAt(i)
            if (child is Button) {
                child.setOnClickListener { onKeypadClick(child) }
            }
        }
    }

    private fun onKeypadClick(button: Button) {
        if (isLockedOut) return

        when (button.text.toString()) {
            "C" -> etPin.setText("")
            "⌫" -> {
                val text = etPin.text.toString()
                if (text.isNotEmpty()) {
                    etPin.setText(text.substring(0, text.length - 1))
                    etPin.setSelection(etPin.text.length)
                }
            }
            else -> {
                if (etPin.text.length < 6) {
                    etPin.append(button.text)
                }
            }
        }
    }

    private fun attemptUnlock() {
        if (isLockedOut) return

        val inputPin = etPin.text.toString()
        if (inputPin.isEmpty()) {
            Toast.makeText(this, "Please enter your PIN", Toast.LENGTH_SHORT).show()
            return
        }

        val storedHash = prefsManager.getPasswordHash()
        if (storedHash != null && BCrypt.checkpw(inputPin, storedHash)) {
            Toast.makeText(this, "Welcome", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            attemptCount++
            updateAttemptsDisplay()
            etPin.setText("")
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()

            if (attemptCount >= maxAttempts) {
                lockOut()
            }
        }
    }

    private fun updateAttemptsDisplay() {
        val remaining = maxAttempts - attemptCount
        tvAttempts.text = "$remaining/$maxAttempts attempts remaining"
        if (remaining <= 0) {
            tvAttempts.text = "0/$maxAttempts attempts remaining"
        }
    }

    private fun lockOut() {
        isLockedOut = true
        btnUnlock.isEnabled = false
        tvAttempts.text = "Too many attempts. Try again in 30s"

        object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                tvAttempts.text = "Try again in ${seconds}s"
            }

            override fun onFinish() {
                isLockedOut = false
                attemptCount = 0
                btnUnlock.isEnabled = true
                updateAttemptsDisplay()
                Toast.makeText(this@LockActivity, "You can try again", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun showForgotDialog() {
        val puzzleType = prefsManager.getPuzzleType()
        val puzzleQuestion = prefsManager.getPuzzleQuestion()
        val puzzleAnswer = prefsManager.getPuzzleAnswer()

        if (puzzleType.isEmpty() || puzzleAnswer == null) {
            Toast.makeText(this, "No recovery method configured", Toast.LENGTH_SHORT).show()
            return
        }

        when (puzzleType) {
            "custom_question" -> showCustomQuestionDialog(puzzleQuestion, puzzleAnswer)
            "math_pin" -> showMathPinDialog(puzzleAnswer)
            "pattern" -> showPatternDialog(puzzleAnswer)
            else -> Toast.makeText(this, "Unknown puzzle type", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCustomQuestionDialog(question: String?, answer: String) {
        val input = EditText(this).apply {
            hint = "Enter your answer"
            setPadding(32, 16, 32, 16)
        }

        AlertDialog.Builder(this)
            .setTitle("Security Question")
            .setMessage(question ?: "No question set")
            .setView(input)
            .setPositiveButton("Verify") { _, _ ->
                val userAnswer = input.text.toString().trim()
                if (userAnswer.equals(answer, ignoreCase = true)) {
                    showResetPinDialog()
                } else {
                    Toast.makeText(this, "Incorrect answer", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMathPinDialog(answer: String) {
        val input = EditText(this).apply {
            hint = "Enter the result"
            inputType = InputType.TYPE_CLASS_NUMBER
            setPadding(32, 16, 32, 16)
        }

        AlertDialog.Builder(this)
            .setTitle("Math PIN Recovery")
            .setMessage("Solve: ${prefsManager.getPuzzleQuestion() ?: "5+3=?"}")
            .setView(input)
            .setPositiveButton("Verify") { _, _ ->
                val userAnswer = input.text.toString().trim()
                if (userAnswer == answer) {
                    showResetPinDialog()
                } else {
                    Toast.makeText(this, "Incorrect answer", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPatternDialog(correctAnswer: String) {
        val correctSequence = correctAnswer.split(",").mapNotNull { it.toIntOrNull() }
        if (correctSequence.size != 4) {
            Toast.makeText(this, "Invalid pattern configuration", Toast.LENGTH_SHORT).show()
            return
        }

        val userSequence = mutableListOf<Int>()
        val circleColors = listOf(
            Color.rgb(231, 76, 60),
            Color.rgb(46, 204, 113),
            Color.rgb(52, 152, 219),
            Color.rgb(241, 196, 15)
        )

        val builder = AlertDialog.Builder(this)
            .setTitle("Pattern Recovery")
            .setMessage("Tap the circles in the correct sequence")

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val statusText = TextView(this).apply {
            text = "Selected: 0/4"
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val circlesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }

        circleColors.forEachIndexed { index, color ->
            val circle = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(64, 64).apply {
                    setMargins(12, 12, 12, 12)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setSize(64, 64)
                }
                tag = index

                setOnClickListener {
                    if (!userSequence.contains(index)) {
                        userSequence.add(index)
                        alpha = 0.4f
                        statusText.text = "Selected: ${userSequence.size}/4"
                        if (userSequence.size == 4) {
                            val userAnswerStr = userSequence.joinToString(",")
                            if (userAnswerStr == correctAnswer) {
                                showResetPinDialog()
                            } else {
                                Toast.makeText(
                                    this@LockActivity,
                                    "Incorrect pattern sequence",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
            circlesRow.addView(circle)
        }

        val resetButton = Button(this).apply {
            text = "Reset"
            setOnClickListener {
                userSequence.clear()
                for (i in 0 until circlesRow.childCount) {
                    circlesRow.getChildAt(i).alpha = 1f
                }
                statusText.text = "Selected: 0/4"
            }
        }

        dialogLayout.addView(statusText)
        dialogLayout.addView(circlesRow)
        dialogLayout.addView(resetButton)

        builder.setView(dialogLayout)
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showResetPinDialog() {
        val newPinInput = EditText(this).apply {
            hint = "Enter new PIN (4-6 digits)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(32, 16, 32, 16)
        }
        val confirmPinInput = EditText(this).apply {
            hint = "Confirm new PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(32, 16, 32, 16)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(newPinInput)
            addView(confirmPinInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Reset PIN")
            .setView(container)
            .setPositiveButton("Reset") { _, _ ->
                val newPin = newPinInput.text.toString()
                val confirmPin = confirmPinInput.text.toString()

                if (newPin.length < 4) {
                    Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPin != confirmPin) {
                    Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val hashedPin = BCrypt.hashpw(newPin, BCrypt.gensalt())
                prefsManager.setPasswordHash(hashedPin)
                attemptCount = 0
                updateAttemptsDisplay()
                Toast.makeText(this, "PIN reset successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupBiometric() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(this)
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        Toast.makeText(this@LockActivity, "Biometric verified", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LockActivity, MainActivity::class.java))
                        finish()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }

                biometricPrompt = BiometricPrompt(this, executor, callback)

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Dimagh Kharab")
                    .setSubtitle("Use your fingerprint to unlock")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build()

                biometricPrompt?.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> { }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> { }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> { }
        }
    }
}
