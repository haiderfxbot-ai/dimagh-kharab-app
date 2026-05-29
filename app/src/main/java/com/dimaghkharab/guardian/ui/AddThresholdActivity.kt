package com.dimaghkharab.guardian.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dimaghkharab.guardian.R
import com.dimaghkharab.guardian.data.AppDatabase
import com.dimaghkharab.guardian.data.entity.BatteryThreshold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AddThresholdActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    private lateinit var tvPercent: TextView
    private lateinit var btnPlus: Button
    private lateinit var btnMinus: Button
    private lateinit var seekBar: SeekBar
    private lateinit var etName: EditText
    private lateinit var btnSelectAudio: Button
    private lateinit var tvSelectedFile: TextView
    private lateinit var seekVolume: SeekBar
    private lateinit var tvVolumeLabel: TextView
    private lateinit var btnSave: Button

    private var currentPercent: Int = 50
    private var selectedFilePath: String? = null
    private var currentVolume: Int = 75

    companion object {
        private const val PICK_AUDIO_REQUEST = 300
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_threshold)

        db = AppDatabase.getInstance(this)

        tvPercent = findViewById(R.id.tvPercentage)
        btnPlus = findViewById(R.id.btnPlus)
        btnMinus = findViewById(R.id.btnMinus)
        seekBar = findViewById(R.id.seekThreshold)
        etName = findViewById(R.id.etThresholdName)
        btnSelectAudio = findViewById(R.id.btnSelectAudio)
        tvSelectedFile = findViewById(R.id.tvAudioPath)
        seekVolume = findViewById(R.id.seekVolume)
        tvVolumeLabel = findViewById(R.id.tvVolumeLabel)
        btnSave = findViewById(R.id.btnSave)

        updatePercentDisplay()

        btnPlus.setOnClickListener {
            if (currentPercent < 100) {
                currentPercent++
                updatePercentDisplay()
            }
        }

        btnMinus.setOnClickListener {
            if (currentPercent > 1) {
                currentPercent--
                updatePercentDisplay()
            }
        }

        seekBar.max = 99
        seekBar.progress = currentPercent - 1
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentPercent = progress + 1
                updatePercentDisplay()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekVolume.max = 100
        seekVolume.progress = currentVolume
        tvVolumeLabel.text = "Volume: $currentVolume%"
        seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentVolume = progress
                tvVolumeLabel.text = "Volume: $currentVolume%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSelectAudio.setOnClickListener { pickAudioFile() }

        btnSave.setOnClickListener { saveThreshold() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            copyAudioFile(uri)
        }
    }

    private fun updatePercentDisplay() {
        tvPercent.text = "$currentPercent%"
        seekBar.progress = currentPercent - 1
    }

    private fun pickAudioFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Select Audio File"), PICK_AUDIO_REQUEST)
    }

    private fun copyAudioFile(uri: Uri) {
        try {
            var fileName = "threshold_audio_${System.currentTimeMillis()}.mp3"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val name = it.getString(nameIndex)
                        if (!name.isNullOrBlank()) fileName = name
                    }
                }
            }

            val destDir = File(filesDir, "threshold_audio")
            if (!destDir.exists()) destDir.mkdirs()
            val destFile = File(destDir, fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            selectedFilePath = destFile.absolutePath
            tvSelectedFile.text = "Selected: $fileName"
            tvSelectedFile.visibility = View.VISIBLE
            Toast.makeText(this, "Audio file selected: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to copy audio file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveThreshold() {
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a threshold name", Toast.LENGTH_SHORT).show()
            return
        }

        val filePath = selectedFilePath
        if (filePath.isNullOrBlank()) {
            Toast.makeText(this, "Please select an audio file", Toast.LENGTH_SHORT).show()
            return
        }
        if (!File(filePath).exists()) {
            Toast.makeText(this, "Selected audio file no longer exists", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val threshold = BatteryThreshold(
                    percentage = currentPercent,
                    name = name,
                    filePath = filePath,
                    volume = currentVolume,
                    isActive = true,
                    triggeredToday = false
                )
                db.batteryThresholdDao().insert(threshold)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddThresholdActivity, "Threshold saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
