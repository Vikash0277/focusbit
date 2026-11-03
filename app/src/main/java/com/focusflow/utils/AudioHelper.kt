package com.focusflow.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

object AudioRecorderHelper {
    private const val TAG = "AudioRecorderHelper"
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    var isRecording: Boolean = false
        private set

    fun startRecording(context: Context): File? {
        if (isRecording) stopRecording()

        val audioDir = File(context.filesDir, "audio_notes").apply { if (!exists()) mkdirs() }
        val file = File(audioDir, "rec_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file

        mediaRecorder = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording audio", e)
            mediaRecorder?.release()
            mediaRecorder = null
            currentOutputFile = null
            return null
        }

        isRecording = true
        Log.d(TAG, "Audio recording started at: ${file.absolutePath}")
        return file
    }

    fun stopRecording(): String? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            val path = currentOutputFile?.absolutePath
            Log.d(TAG, "Audio recording stopped. Path: $path")
            path
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media recorder", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            null
        }
    }

    fun cancelRecording() {
        if (!isRecording) return
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            currentOutputFile?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling recording", e)
        } finally {
            mediaRecorder = null
            currentOutputFile = null
            isRecording = false
        }
    }
}

class AudioPlayerHelper {
    private var mediaPlayer: MediaPlayer? = null
    var isPlaying: Boolean = false
        private set

    fun play(filePath: String, onFinished: () -> Unit, onError: () -> Unit = {}) {
        stop()
        val file = File(filePath)
        if (!file.exists()) {
            onError()
            return
        }

        val player = MediaPlayer()
        try {
            player.setDataSource(filePath)
            player.prepare()
            player.setOnCompletionListener {
                isPlaying = false
                onFinished()
            }
            player.setOnErrorListener { _, _, _ ->
                isPlaying = false
                onError()
                true
            }
            player.start()
            isPlaying = true
            mediaPlayer = player
        } catch (e: IOException) {
            Log.e("AudioPlayerHelper", "Failed to play audio file: $filePath", e)
            onError()
            player.release()
            mediaPlayer = null
            isPlaying = false
        }
    }

    fun pause() {
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
        }
    }

    fun resume() {
        mediaPlayer?.start()
        isPlaying = true
    }

    fun stop() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Error releasing player", e)
        } finally {
            mediaPlayer = null
            isPlaying = false
        }
    }

    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }
}

