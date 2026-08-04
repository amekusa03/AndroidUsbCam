package com.kusa.usbcam

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.net.ServerSocket
import java.net.Socket

class AudioServer(private val port: Int = 8081) {
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null
    private val clients = mutableListOf<Socket>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    fun start() {
        job = scope.launch {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("AudioServer", "AudioRecord initialization failed")
                    return@launch
                }

                serverSocket = ServerSocket(port)
                Log.d("AudioServer", "Audio Server started on port $port")

                audioRecord?.startRecording()
                isRecording = true

                launch {
                    val buffer = ByteArray(bufferSize)
                    while (isRecording && isActive) {
                        val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                        if (read > 0) {
                            broadcastAudio(buffer, read)
                        }
                    }
                }

                while (isActive) {
                    val client = serverSocket?.accept()
                    if (client != null) {
                        Log.d("AudioServer", "Audio client connected: ${client.inetAddress}")
                        synchronized(clients) {
                            clients.add(client)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioServer", "Server error", e)
            }
        }
    }

    private fun broadcastAudio(data: ByteArray, length: Int) {
        synchronized(clients) {
            val iterator = clients.iterator()
            while (iterator.hasNext()) {
                val client = iterator.next()
                try {
                    val outputStream = client.getOutputStream()
                    outputStream.write(data, 0, length)
                } catch (e: Exception) {
                    Log.d("AudioServer", "Audio client disconnected")
                    client.close()
                    iterator.remove()
                }
            }
        }
    }

    fun stop() {
        isRecording = false
        job?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        serverSocket?.close()
        synchronized(clients) {
            clients.forEach { it.close() }
            clients.clear()
        }
    }
}
