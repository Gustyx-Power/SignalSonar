package id.gustyx.signalsonar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import id.gustyx.signalsonar.R
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Low-latency audio playback engine utilizing Android [SoundPool].
 * Configured specifically to output on the MEDIA stream (STREAM_MUSIC / USAGE_MEDIA)
 * so users can adjust beep volume directly via normal device volume buttons.
 */
class SonarAudioEngine(private val context: Context) {

    private val tag = "SonarAudioEngine"

    private var soundPool: SoundPool? = null
    private var pingSoundId: Int = 0
    private var flatSoundId: Int = 0
    private var btsBeaconSoundId: Int = 0

    private val isPingLoaded = AtomicBoolean(false)
    private val isFlatLoaded = AtomicBoolean(false)
    private val isBtsBeaconLoaded = AtomicBoolean(false)
    private val isMuted = AtomicBoolean(false)

    @Volatile
    private var inAppVolume: Float = 0.9f

    init {
        initializeSoundPool()
    }

    @Synchronized
    private fun initializeSoundPool() {
        if (soundPool != null) return

        // USAGE_MEDIA & CONTENT_TYPE_MUSIC routes audio directly to Media Volume (STREAM_MUSIC)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build().apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        if (sampleId == pingSoundId) isPingLoaded.set(true)
                        if (sampleId == flatSoundId) isFlatLoaded.set(true)
                        if (sampleId == btsBeaconSoundId) isBtsBeaconLoaded.set(true)
                        Log.d(tag, "Sample $sampleId loaded successfully into MEDIA stream")
                    } else {
                        Log.e(tag, "Failed to load sample $sampleId, status: $status")
                    }
                }
            }

        try {
            soundPool?.let { pool ->
                pingSoundId = pool.load(context, R.raw.sonar_beep, 1)
                flatSoundId = pool.load(context, R.raw.sonar_flat, 1)
                btsBeaconSoundId = pool.load(context, R.raw.bts_beacon, 1)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading sound samples", e)
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted.set(muted)
    }

    fun setInAppVolume(volume: Float) {
        inAppVolume = volume.coerceIn(0.0f, 1.0f)
    }

    fun getInAppVolume(): Float = inAppVolume

    /**
     * Plays the signature sonar radar ping with optional dynamic pitch variation.
     */
    fun playPing(ratePitch: Float = 1.0f, baseVolume: Float = 1.0f) {
        if (isMuted.get()) return
        val pool = soundPool ?: return
        if (!isPingLoaded.get() || pingSoundId == 0) return

        try {
            val finalVolume = (baseVolume * inAppVolume).coerceIn(0.0f, 1.0f)
            // Pitch clamped between 0.8f and 1.3f for expressive acoustics
            val clampedPitch = ratePitch.coerceIn(0.8f, 1.3f)
            pool.play(pingSoundId, finalVolume, finalVolume, 1, 0, clampedPitch)
        } catch (e: Exception) {
            Log.e(tag, "Error playing ping sound", e)
        }
    }

    /**
     * Plays the distinct BTS cell tower telemetry beacon sound.
     * Uses a resonant microwave telemetry chirp, distinct from the medical ECG pulse.
     */
    fun playBtsBeacon(ratePitch: Float = 1.0f, baseVolume: Float = 1.0f) {
        if (isMuted.get()) return
        val pool = soundPool ?: return
        if (!isBtsBeaconLoaded.get() || btsBeaconSoundId == 0) return

        try {
            val finalVolume = (baseVolume * inAppVolume).coerceIn(0.0f, 1.0f)
            val clampedPitch = ratePitch.coerceIn(0.75f, 1.4f)
            pool.play(btsBeaconSoundId, finalVolume, finalVolume, 1, 0, clampedPitch)
        } catch (e: Exception) {
            Log.e(tag, "Error playing BTS beacon sound", e)
        }
    }

    /**
     * Plays the flat warning tone when cellular signal is critically low or lost.
     */
    fun playFlat(baseVolume: Float = 0.8f) {
        if (isMuted.get()) return
        val pool = soundPool ?: return
        if (!isFlatLoaded.get() || flatSoundId == 0) return

        try {
            val finalVolume = (baseVolume * inAppVolume).coerceIn(0.0f, 1.0f)
            pool.play(flatSoundId, finalVolume, finalVolume, 1, 0, 1.0f)
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
            isBtsBeaconLoaded.set(false)
            Log.d(tag, "SoundPool released successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error releasing SoundPool", e)
        }
    }
}
