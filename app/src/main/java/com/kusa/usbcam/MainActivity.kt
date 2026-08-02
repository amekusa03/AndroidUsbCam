package com.kusa.usbcam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var serverToggle: ToggleButton
    private lateinit var statusText: TextView
    private lateinit var cameraSwitch: ImageButton
    private lateinit var cameraRotate: ImageButton
    private lateinit var zoomBar: SeekBar
    private lateinit var cameraExecutor: ExecutorService
    private var mjpegServer: MjpegServer? = null
    private var isStreaming = false
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var camera: Camera? = null
    private var currentRotation = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewFinder = findViewById(R.id.viewFinder)
        serverToggle = findViewById(R.id.serverToggle)
        statusText = findViewById(R.id.statusText)
        cameraSwitch = findViewById(R.id.cameraSwitch)
        cameraRotate = findViewById(R.id.cameraRotate)
        zoomBar = findViewById(R.id.zoomBar)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        serverToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startServer()
            } else {
                stopServer()
            }
        }

        cameraSwitch.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        cameraRotate.setOnClickListener {
            currentRotation = (currentRotation + 90) % 360
            mjpegServer?.rotation = currentRotation
            Toast.makeText(this, "Rotation: $currentRotation°", Toast.LENGTH_SHORT).show()
        }

        zoomBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                camera?.cameraControl?.setLinearZoom(progress / 100f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (isStreaming) {
                            mjpegServer?.sendImage(image)
                        } else {
                            image.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
                // Restore zoom if needed, or reset
                camera?.cameraControl?.setLinearZoom(zoomBar.progress / 100f)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun startServer() {
        mjpegServer = MjpegServer(8080)
        mjpegServer?.rotation = currentRotation
        mjpegServer?.start()
        isStreaming = true
        statusText.text = "Server: Running on port 8080"
        Toast.makeText(this, "Server started", Toast.LENGTH_SHORT).show()
    }

    private fun stopServer() {
        isStreaming = false
        mjpegServer?.stop()
        mjpegServer = null
        statusText.text = "Server: Stopped"
        Toast.makeText(this, "Server stopped", Toast.LENGTH_SHORT).show()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        mjpegServer?.stop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "UsbCAM"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
