package id.gustyx.signalsonar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import id.gustyx.signalsonar.R
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Low-latency audio playback engine utilizing Android [SoundPool].
 * Provides crisp repeated acoustic sonar pulses without memory leaks or garbage collection stalls.
 */
class SonarAudioEngine(private val context: Context) {

    private val tag = "SonarAudioEngine"

    private var soundPool: SoundPool? = null
    private var pingSoundId: Int = 0
    private var flatSoundId: Int = 0

    private val isPingLoaded = AtomicBoolean(false)
    private val isFlatLoaded = AtomicBoolean(false)
    private val isMuted = AtomicBoolean(false)

    init {
        initializeSoundPool()
    }

    @Synchronized
    private fun initializeSoundPool() {
        if (soundPool != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build().apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        if (sampleId == pingSoundId) isPingLoaded.set(true)
                        if (sampleId == flatSoundId) isFlatLoaded.set(true)
                        Log.d(tag, "Sample $sampleId loaded successfully")
                    } else {
                        Log.e(tag, "Failed to load sample $sampleId, status: $status")
                    }
                }
            }

        try {
            soundPool?.let { pool ->
                pingSoundId = pool.load(context, R.raw.sonar_beep, 1)
                flatSoundId = pool.load(context, R.raw.sonar_flat, 1)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading sound samples", e)
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted.set(muted)
    }

    /**
     * Plays the signature sonar radar ping with optional dynamic pitch variation.
     */
    fun playPing(ratePitch: Float = 1.0f, volume: Float = 0.9f) {
        if (isMuted.get()) return
        val pool = soundPool ?: return
        if (!isPingLoaded.get() || pingSoundId == 0) return

        try {
            // Pitch clamped between 0.8f and 1.25f for expressive acoustics
            val clampedPitch = ratePitch.coerceIn(0.8f, 1.3f)
            pool.play(pingSoundId, volume, volume, 1, 0, clampedPitch)
        } catch (e: Exception) {
            Log.e(tag, "Error playing ping sound", e)
        }
    }

    /**
     * Plays the flat warning tone when cellular signal is critically low or lost.
     */
    fun playFlat(volume: Float = 0.7f) {
        if (isMuted.get()) return
        val pool = soundPool ?: return
        if (!isFlatLoaded.get() || flatSoundId == 0) return

        try {
            pool.play(flatSoundId, volume, volume, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.e(tag, "Error playing flat sound", e)
        }
    }

    /**
     * Halts all active streams immediately to prevent lingering audio when app leaves foreground.
     */
    fun pauseAll() {
        soundPool?.autoPause()
    }

    /**
     * Resumes streams if applicable.
     */
    fun resumeAll() {
        soundPool?.autoResume()
    }

    /**
     * Fully releases SoundPool native resources upon ViewModel or Activity destruction.
     */
    @Synchronized
    fun release() {
        try {
            soundPool?.autoPause()
            soundPool?.release()
            soundPool = null
            isPingLoaded.set(false)
            isFlatLoaded.set(false)
            Log.d(tag, "SoundPool released successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error releasing SoundPool", e)
        }
    }
}
