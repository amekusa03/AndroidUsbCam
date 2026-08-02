package com.kusa.usbcam

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer

class MjpegServer(private val port: Int = 8080) {
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null
    private val clients = mutableListOf<Socket>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var rotation: Int = 0

    fun start() {
        job = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d("MjpegServer", "Server started on port $port")
                while (isActive) {
                    val client = serverSocket?.accept()
                    if (client != null) {
                        Log.d("MjpegServer", "Client connected: ${client.inetAddress}")
                        handleClient(client)
                    }
                }
            } catch (e: Exception) {
                Log.e("MjpegServer", "Server error", e)
            }
        }
    }

    fun stop() {
        job?.cancel()
        serverSocket?.close()
        synchronized(clients) {
            clients.forEach { it.close() }
            clients.clear()
        }
    }

    private fun handleClient(client: Socket) {
        scope.launch {
            try {
                val outputStream = client.getOutputStream()
                outputStream.write(("HTTP/1.0 200 OK\r\n" +
                        "Server: UsbCAM\r\n" +
                        "Connection: close\r\n" +
                        "Max-Age: 0\r\n" +
                        "Expires: 0\r\n" +
                        "Cache-Control: no-cache, private\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Content-Type: multipart/x-mixed-replace; boundary=--boundary\r\n\r\n").toByteArray())
                outputStream.flush()

                synchronized(clients) {
                    clients.add(client)
                }
            } catch (e: Exception) {
                Log.e("MjpegServer", "Client handler error", e)
                client.close()
            }
        }
    }

    fun sendImage(image: ImageProxy) {
        val jpegData = imageToJpeg(image) ?: return
        image.close()

        synchronized(clients) {
            val iterator = clients.iterator()
            while (iterator.hasNext()) {
                val client = iterator.next()
                try {
                    val outputStream = client.getOutputStream()
                    outputStream.write(("--boundary\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Content-Length: ${jpegData.size}\r\n\r\n").toByteArray())
                    outputStream.write(jpegData)
                    outputStream.write("\r\n".toByteArray())
                    outputStream.flush()
                } catch (e: Exception) {
                    Log.d("MjpegServer", "Client disconnected")
                    client.close()
                    iterator.remove()
                }
            }
        }
    }

    private fun imageToJpeg(image: ImageProxy): ByteArray? {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()

        if (rotation == 0) {
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 80, out)
        } else {
            val intermediateOut = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, intermediateOut)
            val jpegData = intermediateOut.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)

            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())
            val rotatedBitmap = android.graphics.Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            rotatedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
            bitmap.recycle()
            rotatedBitmap.recycle()
        }
        return out.toByteArray()
    }
}
