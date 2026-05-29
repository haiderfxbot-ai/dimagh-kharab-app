package com.dimaghkharab.guardian.ui

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dimaghkharab.guardian.R
import com.dimaghkharab.guardian.data.AppDatabase
import com.dimaghkharab.guardian.data.entity.UsageLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UsageActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    private lateinit var btnDatePicker: Button
    private lateinit var tvTotalTime: TextView
    private lateinit var rvUsageLogs: RecyclerView
    private lateinit var emptyState: View
    private lateinit var llSortOptions: LinearLayout
    private lateinit var btnSortDuration: Button
    private lateinit var btnSortAlpha: Button
    private lateinit var btnSortTime: Button

    private var selectedDate = ""
    private var usageLogs: MutableList<UsageLog> = mutableListOf()
    private var usageAdapter: UsageLogAdapter? = null
    private var currentSortMode: SortMode = SortMode.BY_TIME

    private val dateDisplayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val dateQueryFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    enum class SortMode { BY_DURATION, ALPHABETICAL, BY_TIME }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage)

        db = AppDatabase.getInstance(this)

        btnDatePicker = findViewById(R.id.btnDatePicker)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        rvUsageLogs = findViewById(R.id.rvUsageLogs)
        emptyState = findViewById(R.id.emptyState)
        llSortOptions = findViewById(R.id.llSortOptions)
        btnSortDuration = findViewById(R.id.btnSortDuration)
        btnSortAlpha = findViewById(R.id.btnSortAlpha)
        btnSortTime = findViewById(R.id.btnSortTime)

        selectedDate = dateQueryFormat.format(Date())
        updateDateButtonText()

        usageAdapter = UsageLogAdapter(usageLogs, packageManager)
        rvUsageLogs.layoutManager = LinearLayoutManager(this)
        rvUsageLogs.adapter = usageAdapter

        btnDatePicker.setOnClickListener { showDatePicker() }

        btnSortDuration.setOnClickListener {
            currentSortMode = SortMode.BY_DURATION
            sortLogs()
            updateSortButtonHighlights()
        }

        btnSortAlpha.setOnClickListener {
            currentSortMode = SortMode.ALPHABETICAL
            sortLogs()
            updateSortButtonHighlights()
        }

        btnSortTime.setOnClickListener {
            currentSortMode = SortMode.BY_TIME
            sortLogs()
            updateSortButtonHighlights()
        }

        updateSortButtonHighlights()
        loadUsageData()
    }

    private fun updateDateButtonText() {
        try {
            val date = dateQueryFormat.parse(selectedDate) ?: Date()
            btnDatePicker.text = dateDisplayFormat.format(date)
        } catch (_: Exception) {
            btnDatePicker.text = selectedDate
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        try {
            val date = dateQueryFormat.parse(selectedDate)
            if (date != null) calendar.time = date
        } catch (_: Exception) {}

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = android.app.DatePickerDialog(
            this,
            { _, selYear, selMonth, selDay ->
                val cal = Calendar.getInstance()
                cal.set(selYear, selMonth, selDay)
                selectedDate = dateQueryFormat.format(cal.time)
                updateDateButtonText()
                loadUsageData()
            },
            year, month, day
        )
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun loadUsageData() {
        lifecycleScope.launch {
            val logs = withContext(Dispatchers.IO) {
                db.usageLogDao().getByDate(selectedDate)
            }
            usageLogs.clear()
            usageLogs.addAll(logs)
            sortLogs()
            updateTotalTime()
            updateEmptyState()
        }
    }

    private fun sortLogs() {
        when (currentSortMode) {
            SortMode.BY_DURATION -> {
                usageLogs.sortByDescending { it.durationSeconds }
            }
            SortMode.ALPHABETICAL -> {
                usageLogs.sortBy { it.appName.lowercase(Locale.getDefault()) }
            }
            SortMode.BY_TIME -> {
                usageLogs.sortByDescending { it.startTime }
            }
        }
        usageAdapter?.notifyDataSetChanged()
    }

    private fun updateSortButtonHighlights() {
        val defaultColor = 0xFF000000.toInt()
        val selectedColor = 0xFF6200EE.toInt()

        btnSortDuration.setTextColor(
            if (currentSortMode == SortMode.BY_DURATION) selectedColor else defaultColor
        )
        btnSortAlpha.setTextColor(
            if (currentSortMode == SortMode.ALPHABETICAL) selectedColor else defaultColor
        )
        btnSortTime.setTextColor(
            if (currentSortMode == SortMode.BY_TIME) selectedColor else defaultColor
        )
    }

    private fun updateTotalTime() {
        val totalSeconds = usageLogs.sumOf { it.durationSeconds }
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        tvTotalTime.text = if (hours > 0) {
            String.format("%dh %dm %ds", hours, minutes, seconds)
        } else if (minutes > 0) {
            String.format("%dm %ds", minutes, seconds)
        } else {
            String.format("%ds", seconds)
        }
    }

    private fun updateEmptyState() {
        val hasData = usageLogs.isNotEmpty()
        emptyState.visibility = if (hasData) View.GONE else View.VISIBLE
        rvUsageLogs.visibility = if (hasData) View.VISIBLE else View.GONE
        llSortOptions.visibility = if (hasData) View.VISIBLE else View.GONE
    }

    private class UsageLogAdapter(
        private val items: List<UsageLog>,
        private val packageManager: PackageManager
    ) : RecyclerView.Adapter<UsageLogAdapter.ViewHolder>() {

        private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
            val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
            val tvStartTime: TextView = itemView.findViewById(R.id.tvStartTime)
            val tvEndTime: TextView = itemView.findViewById(R.id.tvEndTime)
            val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_usage_log, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            holder.tvAppName.text = item.appName
            holder.tvStartTime.text = "Start: ${timeFormat.format(Date(item.startTime))}"
            holder.tvEndTime.text = "End: ${timeFormat.format(Date(item.endTime))}"

            val duration = item.durationSeconds
            val hours = duration / 3600
            val minutes = (duration % 3600) / 60
            val seconds = duration % 60
            holder.tvDuration.text = if (hours > 0) {
                String.format("%dh %dm %ds", hours, minutes, seconds)
            } else if (minutes > 0) {
                String.format("%dm %ds", minutes, seconds)
            } else {
                String.format("%ds", seconds)
            }

            try {
                val appIcon = packageManager.getApplicationIcon(item.packageName)
                holder.ivAppIcon.setImageDrawable(appIcon)
            } catch (e: PackageManager.NameNotFoundException) {
                holder.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
