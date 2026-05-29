package com.dimaghkharab.guardian.util

import android.content.Context
import android.util.Log
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraCapture(context: Context) {

    private val appContext: Context = context.applicationContext
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private val lifecycleOwner: LifecycleOwner = object : LifecycleOwner {
        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    }

    init {
        lifecycleRegistry = LifecycleRegistry(lifecycleOwner)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun capturePhoto(callback: (photoPath: String?) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(appContext)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build()
                preview.surfaceProvider = { _ -> }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )

                val photoDir = File(appContext.filesDir.absolutePath + "/intruder")
                if (!photoDir.exists()) {
                    photoDir.mkdirs()
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
                val photoFile = File(photoDir, "photo_$timestamp.jpg")

                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture?.takePicture(
                    outputOptions,
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Log.d("CameraCapture", "Photo saved: ${photoFile.absolutePath}")
                            releaseCamera()
                            callback(photoFile.absolutePath)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("CameraCapture", "Photo capture failed", exception)
                            releaseCamera()
                            callback(null)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("CameraCapture", "Camera init failed", e)
                releaseCamera()
                callback(null)
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun releaseCamera() {
        try {
            cameraProvider?.unbindAll()
            cameraProvider = null
            imageCapture = null
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        } catch (e: Exception) {
            Log.e("CameraCapture", "Error releasing camera", e)
        }
        if (!executor.isShutdown) {
            executor.shutdown()
        }
    }
}
