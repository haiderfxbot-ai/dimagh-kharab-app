package com.dimaghkharab.guardian.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dimaghkharab.guardian.R
import com.dimaghkharab.guardian.util.PrefsManager
import org.mindrot.jbcrypt.BCrypt

class SetupActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager
    private lateinit var etPin: EditText
    private lateinit var etConfirmPin: EditText
    private lateinit var radioGroup: RadioGroup
    private lateinit var rbCustomQuestion: RadioButton
    private lateinit var rbMathPin: RadioButton
    private lateinit var rbPattern: RadioButton
    private lateinit var puzzleContentContainer: LinearLayout
    private lateinit var btnSave: Button

    private var selectedPuzzleType: String = PUZZLE_CUSTOM_QUESTION
    private val patternSequence = mutableListOf<Int>()
    private val correctPattern = listOf(0, 2, 3, 1)

    companion object {
        private const val PUZZLE_CUSTOM_QUESTION = "custom_question"
        private const val PUZZLE_MATH_PIN = "math_pin"
        private const val PUZZLE_PATTERN = "pattern"
        private const val MIN_PIN_LENGTH = 4
        private const val MATH_EQUATION = "5+3=?"
        private const val MATH_ANSWER = "8"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        prefsManager = PrefsManager.getInstance(this)

        etPin = findViewById(R.id.etPin)
        etConfirmPin = findViewById(R.id.etConfirmPin)
        radioGroup = findViewById(R.id.radioGroup)
        rbCustomQuestion = findViewById(R.id.rbCustomQuestion)
        rbMathPin = findViewById(R.id.rbMathPin)
        rbPattern = findViewById(R.id.rbPattern)
        puzzleContentContainer = findViewById(R.id.puzzleContentContainer)
        btnSave = findViewById(R.id.btnSave)

        etPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        etConfirmPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            puzzleContentContainer.removeAllViews()
            when (checkedId) {
                R.id.rbCustomQuestion -> {
                    selectedPuzzleType = PUZZLE_CUSTOM_QUESTION
                    showCustomQuestionFields()
                }
                R.id.rbMathPin -> {
                    selectedPuzzleType = PUZZLE_MATH_PIN
                    showMathPinField()
                }
                R.id.rbPattern -> {
                    selectedPuzzleType = PUZZLE_PATTERN
                    showPatternCircles()
                }
            }
        }

        rbCustomQuestion.isChecked = true
        showCustomQuestionFields()

        btnSave.setOnClickListener { saveSetup() }
    }

    private fun showCustomQuestionFields() {
        val questionInput = EditText(this).apply {
            hint = "Enter your security question"
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 16, 0, 16)
            id = R.id.etPuzzleQuestion
        }
        val answerInput = EditText(this).apply {
            hint = "Enter the answer"
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 16, 0, 16)
            id = R.id.etPuzzleAnswer
        }
        puzzleContentContainer.addView(questionInput)
        puzzleContentContainer.addView(answerInput)
    }

    private fun showMathPinField() {
        val equationText = TextView(this).apply {
            text = "Solve: $MATH_EQUATION"
            textSize = 18f
            setPadding(0, 16, 0, 16)
        }
        val answerInput = EditText(this).apply {
            hint = "Enter the result"
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 16, 0, 16)
            id = R.id.etPuzzleAnswer
        }
        puzzleContentContainer.addView(equationText)
        puzzleContentContainer.addView(answerInput)
    }

    private fun showPatternCircles() {
        val instruction = TextView(this).apply {
            text = "Tap the circles in the correct sequence"
            textSize = 16f
            setPadding(0, 16, 0, 16)
        }
        puzzleContentContainer.addView(instruction)

        val circleColors = listOf(
            Color.rgb(231, 76, 60),
            Color.rgb(46, 204, 113),
            Color.rgb(52, 152, 219),
            Color.rgb(241, 196, 15)
        )

        patternSequence.clear()

        val circlesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = android.view.Gravity.CENTER
        }

        val statusText = TextView(this).apply {
            text = "Selected: 0/4"
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 16, 0, 16)
            id = R.id.tvPatternStatus
        }

        circleColors.forEachIndexed { index, color ->
            val circle = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(16, 16, 16, 16)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setSize(80, 80)
                }
                tag = index

                setOnClickListener {
                    if (!patternSequence.contains(index)) {
                        patternSequence.add(index)
                        alpha = 0.4f
                        statusText.text = "Selected: ${patternSequence.size}/4"
                        if (patternSequence.size == 4) {
                            statusText.text = "Pattern set! Tap Save & Continue"
                        }
                    }
                }
            }
            circlesLayout.addView(circle)
        }

        val resetButton = Button(this).apply {
            text = "Reset Pattern"
            setOnClickListener {
                patternSequence.clear()
                for (i in 0 until circlesLayout.childCount) {
                    circlesLayout.getChildAt(i).alpha = 1f
                }
                statusText.text = "Selected: 0/4"
            }
        }

        puzzleContentContainer.addView(circlesLayout)
        puzzleContentContainer.addView(statusText)
        puzzleContentContainer.addView(resetButton)
    }

    private fun saveSetup() {
        val pin = etPin.text.toString()
        val confirmPin = etConfirmPin.text.toString()

        if (pin.length < MIN_PIN_LENGTH) {
            Toast.makeText(this, "PIN must be at least $MIN_PIN_LENGTH digits", Toast.LENGTH_SHORT).show()
            return
        }
        if (pin != confirmPin) {
            Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val puzzleAnswer: String
        val puzzleQuestion: String

        when (selectedPuzzleType) {
            PUZZLE_CUSTOM_QUESTION -> {
                val questionView = puzzleContentContainer.findViewById<EditText>(R.id.etPuzzleQuestion)
                val answerView = puzzleContentContainer.findViewById<EditText>(R.id.etPuzzleAnswer)
                if (questionView == null || answerView == null) {
                    Toast.makeText(this, "Please fill in question and answer", Toast.LENGTH_SHORT).show()
                    return
                }
                puzzleQuestion = questionView.text.toString().trim()
                puzzleAnswer = answerView.text.toString().trim()
                if (puzzleQuestion.isEmpty() || puzzleAnswer.isEmpty()) {
                    Toast.makeText(this, "Please fill in question and answer", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            PUZZLE_MATH_PIN -> {
                val answerView = puzzleContentContainer.findViewById<EditText>(R.id.etPuzzleAnswer)
                if (answerView == null) {
                    Toast.makeText(this, "Please enter the math answer", Toast.LENGTH_SHORT).show()
                    return
                }
                puzzleQuestion = MATH_EQUATION
                puzzleAnswer = answerView.text.toString().trim()
                if (puzzleAnswer.isEmpty()) {
                    Toast.makeText(this, "Please enter the math answer", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            PUZZLE_PATTERN -> {
                if (patternSequence.size != 4) {
                    Toast.makeText(this, "Please tap all 4 circles in the correct order", Toast.LENGTH_SHORT).show()
                    return
                }
                puzzleQuestion = "pattern"
                puzzleAnswer = patternSequence.joinToString(",")
            }
            else -> {
                Toast.makeText(this, "Please select a puzzle type", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val hashedPin = BCrypt.hashpw(pin, BCrypt.gensalt())

        prefsManager.setPasswordHash(hashedPin)
        prefsManager.setPuzzleType(selectedPuzzleType)
        prefsManager.setPuzzleAnswer(puzzleAnswer)
        prefsManager.setPuzzleQuestion(puzzleQuestion)

        Toast.makeText(this, "Setup Complete", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
