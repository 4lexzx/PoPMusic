package com.popmusic.app.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): File {
        val file = File(context.cacheDir, "recognition_${System.currentTimeMillis()}.m4a")

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(128_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        recorder = mediaRecorder
        outputFile = file
        return file
    }

    fun stop(): File? {
        val active = recorder ?: return null
        val file = outputFile

        try {
            active.stop()
            return file
        } catch (error: RuntimeException) {
            file?.delete()
            return null
        } finally {
            active.release()
            recorder = null
            outputFile = null
        }
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}
