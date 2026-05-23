package com.example.game.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

object MysteryAudioPlayer {
    private const val TAG = "MysteryAudioPlayer"
    private var musicJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMusicRunning = false
    private var musicVolume = 0.5f

    // 1. Synthesize mechanical button click on the fly
    fun playClick() {
        scope.launch {
            try {
                val sampleRate = 22050
                // Short pluck of 55ms duration
                val numSamples = (sampleRate * 0.055).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    // Pitch slides down rapidly for wood block knock
                    val freq = 550.0 * (1.0 - t * 15.0).coerceAtLeast(0.15)
                    // Sine slide with exponential decay envelope
                    val envelope = sin(2.0 * java.lang.Math.PI * freq * t) * (1.0 - t / 0.055)
                    buffer[i] = (envelope * 24000.0 * musicVolume).toInt().toShort()
                }

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC
                )
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                // Wait for static buffer to play out before releasing
                delay(120)
                audioTrack.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed playing synthesized click", e)
            }
        }
    }

    // 2. Update player volume scale dynamically
    fun setVolume(volume: Float) {
        musicVolume = volume.coerceIn(0.0f, 1.0f)
    }

    // 3. Synthesize mysterious cinematic ambient loops (Suspense Low Drone & Heartbeats)
    fun startMusic() {
        if (isMusicRunning) return
        isMusicRunning = true
        musicJob = scope.launch {
            try {
                val sampleRate = 16000
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                // Stream chunks of 1.5 seconds length
                val bufferSize = (sampleRate * 1.5).toInt()
                val buffer = ShortArray(bufferSize)

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize.coerceAtLeast(bufferSize * 2),
                    AudioTrack.MODE_STREAM
                )
                
                audioTrack.play()
                
                var phase = 0.0
                while (isActive && isMusicRunning) {
                    val currentVol = musicVolume
                    for (i in 0 until buffer.size) {
                        val t = (phase + i) / sampleRate
                        
                        // Heartbeat trigger (slow thumping every 1.5s)
                        val trigger = t % 1.5
                        var accent = 0.08
                        if (trigger < 0.12) {
                            accent = 1.0 // First thump spike
                        } else if (trigger in 0.32..0.44) {
                            accent = 0.75 // Second lighter echo thump
                        }
                        
                        // Cinematic low suspense chords blend (Low A 55Hz, Low E 82.4Hz, Low C# 110Hz)
                        val fundamental = sin(2.0 * java.lang.Math.PI * 55.0 * t)
                        val fifth = 0.5 * sin(2.0 * java.lang.Math.PI * 82.4 * t)
                        val octave = 0.25 * sin(2.0 * java.lang.Math.PI * 110.0 * t)
                        
                        val wave = (fundamental + fifth + octave) * accent
                        buffer[i] = (wave * 12000.0 * currentVol).toInt().toShort()
                    }
                    phase += buffer.size
                    audioTrack.write(buffer, 0, buffer.size)
                    // Wait slightly less than buffer length to maintain fluid stream without gaps
                    delay(1250)
                }
                
                try {
                    audioTrack.stop()
                } catch (ex: Exception) {}
                audioTrack.release()
            } catch (e: Exception) {
                Log.e(TAG, "Audio drone stream failed", e)
                isMusicRunning = false
            }
        }
    }

    fun stopMusic() {
        isMusicRunning = false
        musicJob?.cancel()
        musicJob = null
    }
}
