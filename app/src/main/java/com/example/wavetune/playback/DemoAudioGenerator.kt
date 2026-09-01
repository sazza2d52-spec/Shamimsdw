package com.example.wavetune.playback

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object DemoAudioGenerator {
    private const val SAMPLE_RATE = 22050
    private const val NUM_CHANNELS = 2
    private const val BITS_PER_SAMPLE = 16
    private const val DURATION_SECONDS = 12

    suspend fun getDemoAudioFile(context: Context, trackIndex: Int): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "demo_audio").apply { if (!exists()) mkdirs() }
        val file = File(dir, "demo_$trackIndex.wav")

        if (!file.exists() || file.length() < 1000L) {
            try {
                generateWavFile(file, trackIndex)
            } catch (e: Exception) {
                Log.e("DemoAudioGenerator", "Failed to generate demo wav track $trackIndex", e)
            }
        }
        file
    }

    private fun generateWavFile(outputFile: File, trackIndex: Int) {
        val totalSamples = SAMPLE_RATE * DURATION_SECONDS
        val bytesPerSample = (BITS_PER_SAMPLE / 8) * NUM_CHANNELS
        val dataSize = totalSamples * bytesPerSample

        FileOutputStream(outputFile).use { fos ->
            // Write 44-byte WAV header
            writeWavHeader(fos, dataSize, SAMPLE_RATE, NUM_CHANNELS, BITS_PER_SAMPLE)

            // Track frequencies for musical chords
            val chords = when (trackIndex % 5) {
                0 -> listOf(220.0, 277.18, 329.63, 440.0) // A minor / synthwave
                1 -> listOf(261.63, 329.63, 392.00, 493.88) // C maj7 / lo-fi
                2 -> listOf(146.83, 220.0, 293.66, 369.99) // D maj / solar drift
                3 -> listOf(174.61, 261.63, 349.23, 440.0) // F maj / cosmic
                else -> listOf(196.00, 246.94, 293.66, 392.0) // G maj / urban
            }

            val buffer = ByteBuffer.allocate(bytesPerSample).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val beat = (t * 2.0) % 1.0
                val chordIndex = ((t / 3.0).toInt()) % chords.size
                val baseFreq = chords[chordIndex]

                // Harmonic synth voice with gentle decay and vibrato
                val vibrato = 1.0 + 0.005 * sin(2.0 * PI * 4.5 * t)
                val voice1 = sin(2.0 * PI * (baseFreq * vibrato) * t)
                val voice2 = 0.5 * sin(2.0 * PI * (baseFreq * 1.5 * vibrato) * t)
                val voice3 = 0.3 * sin(2.0 * PI * (baseFreq * 2.0 * vibrato) * t)
                val bassPulse = 0.4 * sin(2.0 * PI * (baseFreq * 0.5) * t) * (1.0 - beat * 0.7)

                val envelope = when {
                    t < 0.5 -> t / 0.5
                    t > DURATION_SECONDS - 1.0 -> (DURATION_SECONDS - t) / 1.0
                    else -> 1.0
                }

                val mixed = (voice1 + voice2 + voice3 + bassPulse) * 0.35 * envelope
                val sampleValue = (mixed.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()

                buffer.clear()
                buffer.putShort(sampleValue) // Left channel
                buffer.putShort(sampleValue) // Right channel
                fos.write(buffer.array())
            }
        }
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        dataSize: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalSize = 36 + dataSize
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size (16 for PCM)
        header.putShort(1) // AudioFormat (1 for PCM)
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray())
        header.putInt(dataSize)

        out.write(header.array())
    }
}
