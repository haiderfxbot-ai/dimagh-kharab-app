package com.dimaghkharab.guardian.ui

import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.dimaghkharab.guardian.data.AppDatabase
import com.dimaghkharab.guardian.R
import com.dimaghkharab.guardian.data.entity.BatteryThreshold
import com.dimaghkharab.guardian.service.GuardianService
import com.dimaghkharab.guardian.util.PermissionHelper
import com.dimaghkharab.guardian.util.PrefsManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager
    private lateinit var db: AppDatabase

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var contentContainer: FrameLayout
    private lateinit var btnStartStop: Button

    private var tvBatteryPercent: TextView? = null
    private var vServiceStatus: View? = null
    private var cardChargingSound: Button? = null
    private var cardDisconnectSound: Button? = null
    private var rvThresholds: RecyclerView? = null
    private var fabAddThreshold: FloatingActionButton? = null

    private val handler = Handler(Looper.getMainLooper())
    private val batteryUpdateRunnable = object : Runnable {
        override fun run() {
            updateBatteryLevel()
            handler.postDelayed(this, 30000)
        }
    }

    private var thresholdAdapter: ThresholdAdapter? = null
    private var thresholds: MutableList<BatteryThreshold> = mutableListOf()
    private var isServiceRunning = false
    private var thresholdsJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefsManager = PrefsManager.getInstance(this)
        db = AppDatabase.getInstance(this)

        bottomNav = findViewById(R.id.bottomNav)
        contentContainer = findViewById(R.id.contentContainer)
        btnStartStop = findViewById(R.id.btnStartStop)

        checkPermissions()

        isServiceRunning = prefsManager.isServiceRunning()

        setupBottomNav()
        showChargingTab()

        btnStartStop.setOnClickListener { toggleService() }

        batteryUpdateRunnable.run()

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceRunning = prefsManager.isServiceRunning()
                updateServiceStatus()
                updateBatteryLevel()
                loadThresholds()
            }
        })
    }

    override fun onDestroy() {
        handler.removeCallbacks(batteryUpdateRunnable)
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                showPermissionDialog()
            }
        }
    }

    private fun checkPermissions() {
        if (!PermissionHelper.hasAllPermissions(this)) {
            showPermissionDialog()
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs camera, storage, and notification permissions to function properly.")
            .setPositiveButton("Grant") { _, _ ->
                val perms = PermissionHelper.getMissingPermissions(this)
                if (perms.isNotEmpty()) {
                    PermissionHelper.requestPermissions(this, perms.toTypedArray(), PERMISSION_REQUEST_CODE)
                }
            }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun setupBottomNav() {
        bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_charging -> {
                    showChargingTab()
                    true
                }
                R.id.nav_intruder -> {
                    startActivity(Intent(this, IntruderActivity::class.java))
                    true
                }
                R.id.nav_usage -> {
                    startActivity(Intent(this, UsageActivity::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_charging
    }

    private fun showChargingTab() {
        contentContainer.removeAllViews()
        val view = layoutInflater.inflate(R.layout.fragment_charging, contentContainer)

        tvBatteryPercent = view.findViewById(R.id.tvBatteryPercent)
        vServiceStatus = view.findViewById(R.id.vServiceStatus)
        cardChargingSound = view.findViewById(R.id.cardChargingSound)
        cardDisconnectSound = view.findViewById(R.id.cardDisconnectSound)
        rvThresholds = view.findViewById(R.id.rvThresholds)
        fabAddThreshold = view.findViewById(R.id.fabAddThreshold)

        isServiceRunning = prefsManager.isServiceRunning()
        updateServiceStatus()
        updateBatteryLevel()

        cardChargingSound?.setOnClickListener {
            startActivity(ChargingDetailActivity.newIntent(this, "CONNECT"))
        }
        cardDisconnectSound?.setOnClickListener {
            startActivity(ChargingDetailActivity.newIntent(this, "DISCONNECT"))
        }

        fabAddThreshold?.setOnClickListener {
            startActivity(Intent(this, AddThresholdActivity::class.java))
        }

        setupThresholdRecycler()
        loadThresholds()
    }

    private fun updateBatteryLevel() {
        try {
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra("level", 0) ?: 0
            val scale = batteryIntent?.getIntExtra("scale", 100) ?: 100
            val percent = (level.toFloat() / scale.toFloat() * 100).toInt()
            tvBatteryPercent?.text = "$percent%"
        } catch (e: Exception) {
            tvBatteryPercent?.text = "N/A"
        }
    }

    private fun updateServiceStatus() {
        val color = if (isServiceRunning) Color.GREEN else Color.RED
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setSize(24, 24)
        }
        vServiceStatus?.background = shape
        btnStartStop.text = if (isServiceRunning) "Stop Service" else "Start Service"
    }

    private fun toggleService() {
        val intent = Intent(this, GuardianService::class.java)
        if (isServiceRunning) {
            intent.action = GuardianService.ACTION_STOP
            ContextCompat.startForegroundService(this, intent)
            prefsManager.setServiceRunning(false)
            isServiceRunning = false
            Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show()
        } else {
            intent.action = GuardianService.ACTION_START
            ContextCompat.startForegroundService(this, intent)
            prefsManager.setServiceRunning(true)
            isServiceRunning = true
            Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show()
        }
        updateServiceStatus()
    }

    private fun setupThresholdRecycler() {
        thresholdAdapter = ThresholdAdapter(thresholds) { threshold ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    db.batteryThresholdDao().delete(threshold)
                }
                loadThresholds()
            }
        }
        rvThresholds?.adapter = thresholdAdapter
        rvThresholds?.itemAnimator = DefaultItemAnimator()

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position >= 0 && position < thresholds.size) {
                    val threshold = thresholds[position]
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            db.batteryThresholdDao().delete(threshold)
                        }
                        loadThresholds()
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Threshold deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        rvThresholds?.let { ItemTouchHelper(swipeCallback).attachToRecyclerView(it) }
    }

    private fun loadThresholds() {
        thresholdsJob?.cancel()
        thresholdsJob = lifecycleScope.launch {
            db.batteryThresholdDao().getAll().collect { list ->
                thresholds.clear()
                thresholds.addAll(list)
                thresholdAdapter?.notifyDataSetChanged()
            }
        }
    }

    private class ThresholdAdapter(
        private val items: List<BatteryThreshold>,
        private val onDelete: (BatteryThreshold) -> Unit
    ) : RecyclerView.Adapter<ThresholdAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvPercent: TextView = itemView.findViewById(R.id.tvThresholdPercent)
            val tvName: TextView = itemView.findViewById(R.id.tvThresholdName)
            val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteThreshold)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_battery_threshold, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvPercent.text = "${item.percentage}%"
            holder.tvName.text = item.name
            holder.btnDelete.setOnClickListener { onDelete(item) }
        }

        override fun getItemCount(): Int = items.size
    }
}
