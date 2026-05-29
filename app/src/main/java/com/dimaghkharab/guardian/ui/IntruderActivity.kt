package com.dimaghkharab.guardian.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dimaghkharab.guardian.data.AppDatabase
import com.dimaghkharab.guardian.data.entity.IntruderPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IntruderActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    private lateinit var tvPhotoCount: TextView
    private lateinit var btnDeleteAll: TextView
    private lateinit var rvPhotos: RecyclerView
    private lateinit var emptyState: View

    private var photos: MutableList<IntruderPhoto> = mutableListOf()
    private var photoAdapter: IntruderPhotoAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intruder)

        db = AppDatabase.getInstance(this)

        tvPhotoCount = findViewById(R.id.tvPhotoCount)
        btnDeleteAll = findViewById(R.id.btnDeleteAll)
        rvPhotos = findViewById(R.id.rvPhotos)
        emptyState = findViewById(R.id.emptyState)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        photoAdapter = IntruderPhotoAdapter(photos) { photo ->
            showDeleteDialog(photo)
        }
        rvPhotos.layoutManager = GridLayoutManager(this, 2)
        rvPhotos.adapter = photoAdapter

        btnDeleteAll.setOnClickListener { showDeleteAllDialog() }

        loadPhotos()
    }

    private fun loadPhotos() {
        lifecycleScope.launch {
            db.intruderPhotoDao().getAll().collect { list ->
                photos.clear()
                photos.addAll(list)
                photoAdapter?.notifyDataSetChanged()
                updateCount()
                updateEmptyState()
            }
        }
    }

    private fun updateCount() {
        tvPhotoCount.text = "${photos.size} photo${if (photos.size != 1) "s" else ""}"
    }

    private fun updateEmptyState() {
        emptyState.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
        rvPhotos.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showDeleteDialog(photo: IntruderPhoto) {
        AlertDialog.Builder(this)
            .setTitle("Delete Photo")
            .setMessage("Delete this intruder photo?")
            .setPositiveButton("Delete") { _, _ -> deletePhoto(photo) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePhoto(photo: IntruderPhoto) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(photo.filePath)
                    if (file.exists()) file.delete()
                } catch (_: Exception) {}
                db.intruderPhotoDao().delete(photo)
            }
            Toast.makeText(this@IntruderActivity, "Photo deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteAllDialog() {
        if (photos.isEmpty()) {
            Toast.makeText(this, "No photos to delete", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete All Photos")
            .setMessage("Are you sure you want to delete all ${photos.size} intruder photos?")
            .setPositiveButton("Delete All") { _, _ -> deleteAllPhotos() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAllPhotos() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                for (photo in photos) {
                    try {
                        val file = File(photo.filePath)
                        if (file.exists()) file.delete()
                    } catch (_: Exception) {}
                }
                db.intruderPhotoDao().deleteAll()
            }
            Toast.makeText(this@IntruderActivity, "All photos deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private class IntruderPhotoAdapter(
        private val items: List<IntruderPhoto>,
        private val onLongClick: (IntruderPhoto) -> Unit
    ) : RecyclerView.Adapter<IntruderPhotoAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivPhoto: ImageView = itemView.findViewById(R.id.ivIntruderPhoto)
            val tvTimestamp: TextView = itemView.findViewById(R.id.tvIntruderTimestamp)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_intruder_photo, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val file = File(item.filePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(item.filePath)
                if (bitmap != null) {
                    holder.ivPhoto.setImageBitmap(bitmap)
                } else {
                    holder.ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                holder.ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            holder.tvTimestamp.text = dateFormat.format(Date(item.timestamp))

            holder.itemView.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
