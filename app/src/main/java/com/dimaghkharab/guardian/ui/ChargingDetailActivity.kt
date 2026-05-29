package com.dimaghkharab.guardian.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dimaghkharab.guardian.R
import com.dimaghkharab.guardian.data.AppDatabase
import com.dimaghkharab.guardian.data.entity.SoundProfile
import com.dimaghkharab.guardian.util.PrefsManager
import com.dimaghkharab.guardian.util.SoundPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ChargingDetailActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager
    private lateinit var db: AppDatabase
    private lateinit var soundPlayer: SoundPlayer

    private lateinit var tvTitle: TextView
    private lateinit var etProfileName: EditText
    private lateinit var btnSelectFile: Button
    private lateinit var rvAudioFiles: RecyclerView
    private lateinit var seekVolume: SeekBar
    private lateinit var tvVolumeLabel: TextView
    private lateinit var btnTestPlay: Button
    private lateinit var btnSave: Button

    private var chargeType: String = "CONNECT"
    private var selectedFilePath: String? = null
    private var currentVolume: Int = 75
    private var existingProfile: SoundProfile? = null
    private var audioFiles: MutableList<String> = mutableListOf()

    private var audioAdapter: AudioFileAdapter? = null

    companion object {
        private const val EXTRA_TYPE = "type"
        private const val PICK_AUDIO_REQUEST = 200
        private const val PREFS_AUDIO_FILES = "audio_files_"

        fun newIntent(activity: AppCompatActivity, type: String): Intent {
            return Intent(activity, ChargingDetailActivity::class.java).apply {
                putExtra(EXTRA_TYPE, type)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charging_detail)

        prefsManager = PrefsManager.getInstance(this)
        db = AppDatabase.getInstance(this)
        soundPlayer = SoundPlayer(this)

        chargeType = intent.getStringExtra(EXTRA_TYPE) ?: "CONNECT"

        tvTitle = findViewById(R.id.tvDetailTitle)
        etProfileName = findViewById(R.id.etProfileName)
        btnSelectFile = findViewById(R.id.btnSelectFile)
        rvAudioFiles = findViewById(R.id.rvAudioFiles)
        seekVolume = findViewById(R.id.seekVolume)
        tvVolumeLabel = findViewById(R.id.tvVolumeLabel)
        btnTestPlay = findViewById(R.id.btnTestPlay)
        btnSave = findViewById(R.id.btnSave)

        tvTitle.text = if (chargeType == "CONNECT") {
            "Charge Connect Sound"
        } else {
            "Charge Disconnect Sound"
        }

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

        btnSelectFile.setOnClickListener { pickAudioFile() }

        btnTestPlay.setOnClickListener { testPlay() }

        btnSave.setOnClickListener { saveProfile() }

        setupAudioFileRecycler()
        loadAudioFiles()
        loadExistingProfile()
    }

    override fun onDestroy() {
        soundPlayer.release()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            copyAudioFile(uri)
        }
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
            var fileName = "audio_${System.currentTimeMillis()}.mp3"
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

            val destDir = File(filesDir, "audio_profiles")
            if (!destDir.exists()) destDir.mkdirs()
            val destFile = File(destDir, fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val filePath = destFile.absolutePath
            selectedFilePath = filePath
            if (!audioFiles.contains(filePath)) {
                audioFiles.add(0, selectedFilePath)
                saveAudioFilesList()
            }
            audioAdapter?.notifyDataSetChanged()
            Toast.makeText(this, "Audio file selected: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to copy audio file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAudioFiles() {
        val prefs = getSharedPreferences("charging_detail", MODE_PRIVATE)
        val files = prefs.getString(PREFS_AUDIO_FILES + chargeType, null)
        if (files != null) {
            audioFiles.clear()
            audioFiles.addAll(files.split("|||").filter { it.isNotBlank() })
            val validFiles = audioFiles.filter { File(it).exists() }
            audioFiles.clear()
            audioFiles.addAll(validFiles)
            saveAudioFilesList()
        }
    }

    private fun saveAudioFilesList() {
        val prefs = getSharedPreferences("charging_detail", MODE_PRIVATE)
        prefs.edit().putString(PREFS_AUDIO_FILES + chargeType, audioFiles.joinToString("|||")).apply()
    }

    private fun setupAudioFileRecycler() {
        audioAdapter = AudioFileAdapter(audioFiles, selectedFilePath) { path ->
            selectedFilePath = path
            audioAdapter?.notifyDataSetChanged()
        }
        rvAudioFiles.layoutManager = LinearLayoutManager(this)
        rvAudioFiles.adapter = audioAdapter
    }

    private fun testPlay() {
        val path = selectedFilePath ?: run {
            Toast.makeText(this, "No audio file selected", Toast.LENGTH_SHORT).show()
            return
        }
        if (!File(path).exists()) {
            Toast.makeText(this, "Audio file not found", Toast.LENGTH_SHORT).show()
            return
        }
        soundPlayer.play(path, currentVolume / 100f)
    }

    private fun loadExistingProfile() {
        lifecycleScope.launch {
            db.soundProfileDao().getByType(chargeType).collect { profiles ->
                if (profiles.isNotEmpty()) {
                    val profile = profiles[0]
                    existingProfile = profile
                    etProfileName.setText(profile.name)
                    selectedFilePath = profile.filePath
                    currentVolume = profile.volume
                    seekVolume.progress = currentVolume
                    tvVolumeLabel.text = "Volume: $currentVolume%"
                    audioAdapter?.selectedPath = profile.filePath
                    audioAdapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun saveProfile() {
        val name = etProfileName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a profile name", Toast.LENGTH_SHORT).show()
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
                val profile = SoundProfile(
                    id = existingProfile?.id ?: 0L,
                    type = chargeType,
                    name = name,
                    filePath = filePath,
                    volume = currentVolume,
                    isActive = true
                )
                if (existingProfile != null) {
                    db.soundProfileDao().update(profile)
                } else {
                    db.soundProfileDao().insert(profile)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ChargingDetailActivity, "Profile saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private class AudioFileAdapter(
        val files: MutableList<String>,
        var selectedPath: String?,
        private val onSelect: (String) -> Unit
    ) : RecyclerView.Adapter<AudioFileAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
            val ivCheckmark: ImageView = itemView.findViewById(R.id.ivCheckmark)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_audio_file, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val path = files[position]
            val name = File(path).name
            holder.tvFileName.text = name

            val isSelected = path == selectedPath
            holder.ivCheckmark.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.itemView.setBackgroundColor(
                if (isSelected) 0x3300FF00.toInt() else 0x00000000
            )

            holder.itemView.setOnClickListener {
                onSelect(path)
                selectedPath = path
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int = files.size
    }
}
