package `in`.gov.nag.mobile.security

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import android.webkit.JavascriptInterface
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * NagProctoringCompanionModule - On-Device Sensor Proctoring Companion.
 * Continuously monitors ambient microphone dB (RMS amplitude calculation) and device attitude
 * (accelerometer + magnetometer pitch/roll/azimuth via SensorManager) to detect candidate posture
 * anomalies, suspicious movement, or unauthorized background conversations during CBT tests.
 */
class ProctoringCompanionModule(private val context: Context) : SensorEventListener {

    companion object {
        const val MODULE_NAME = "NagProctoringCompanionModule"
        private const val TAG = "NagProctoringModule"

        // Default strict examination thresholds
        const val DEFAULT_AUDIO_THRESHOLD_DB = 65.0
        const val DEFAULT_ORIENTATION_THRESHOLD_DEG = 45.0

        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val sensorManager: SensorManager? by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    private var audioRecord: AudioRecord? = null
    private var audioRecordingThread: Thread? = null
    private val isMonitoring = AtomicBoolean(false)

    // Dynamic thresholds
    private var audioThresholdDb: Double = DEFAULT_AUDIO_THRESHOLD_DB
    private var orientationThresholdDeg: Double = DEFAULT_ORIENTATION_THRESHOLD_DEG

    // Sensor state
    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    @Volatile
    private var currentNoiseDb: Double = 35.0

    @Volatile
    private var currentPitchDeg: Double = 0.0

    @Volatile
    private var currentRollDeg: Double = 0.0

    @Volatile
    private var currentAzimuthDeg: Double = 0.0

    /**
     * Start background audio and orientation monitoring.
     */
    @JavascriptInterface
    fun startSensorMonitoring(
        audioThresholdDbArg: Double,
        orientationThresholdDegArg: Double
    ): Boolean {
        if (isMonitoring.get()) {
            return true
        }

        audioThresholdDb = if (audioThresholdDbArg > 0) audioThresholdDbArg else DEFAULT_AUDIO_THRESHOLD_DB
        orientationThresholdDeg = if (orientationThresholdDegArg > 0) orientationThresholdDegArg else DEFAULT_ORIENTATION_THRESHOLD_DEG

        return try {
            registerSensors()
            startAudioRecording()
            isMonitoring.set(true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start sensor monitoring: ${e.message}", e)
            stopSensorsInternal()
            false
        }
    }

    /**
     * Stop background sensor monitoring and release hardware resources.
     */
    @JavascriptInterface
    fun stopSensorMonitoring(): Boolean {
        return try {
            stopSensorsInternal()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop sensor monitoring: ${e.message}", e)
            false
        }
    }

    /**
     * Evaluate current sensor sample against active thresholds.
     */
    @JavascriptInterface
    fun evaluateSensorSample(): String {
        val noise = currentNoiseDb
        val pitch = currentPitchDeg
        val roll = currentRollDeg
        val azimuth = currentAzimuthDeg

        val noiseViolation = noise > audioThresholdDb
        val postureViolation = abs(pitch) > orientationThresholdDeg || abs(roll) > orientationThresholdDeg

        return JSONObject().apply {
            put("ambientNoiseDb", Math.round(noise * 10.0) / 10.0)
            put("pitchDeg", Math.round(pitch * 10.0) / 10.0)
            put("rollDeg", Math.round(roll * 10.0) / 10.0)
            put("azimuthDeg", Math.round(azimuth * 10.0) / 10.0)
            put("noiseViolation", noiseViolation)
            put("postureViolation", postureViolation)
            put("timestamp", System.currentTimeMillis().toDouble())
        }.toString()
    }

    private fun registerSensors() {
        val sm = sensorManager ?: return
        val accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer != null) {
            sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
        if (magnetometer != null) {
            sm.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun startAudioRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize <= 0) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "AudioRecord could not be initialized")
                return
            }

            audioRecord?.startRecording()

            audioRecordingThread = Thread({
                val buffer = ShortArray(bufferSize / 2)
                while (isMonitoring.get() && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            val sample = buffer[i]
                            sum += (sample * sample)
                        }
                        val rms = sqrt(sum / read)
                        // SPL decibel approximation with reference amplitude
                        val db = if (rms > 0) 20 * log10(rms / 1.0) else 0.0
                        // Map PCM-16 RMS to typical ambient dBSPL range (approx 30 dB to 100 dB)
                        val scaledDb = (db * 0.75 + 15.0).coerceIn(20.0, 110.0)
                        currentNoiseDb = scaledDb
                    }
                    try {
                        Thread.sleep(100)
                    } catch (ignored: InterruptedException) {
                        break
                    }
                }
            }, "NagProctoringAudioThread")
            audioRecordingThread?.start()
        } catch (e: SecurityException) {
            Log.w(TAG, "RECORD_AUDIO permission not granted: ${e.message}")
        }
    }

    private fun stopSensorsInternal() {
        isMonitoring.set(false)
        sensorManager?.unregisterListener(this)

        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
            audioRecord = null
            audioRecordingThread?.interrupt()
            audioRecordingThread = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audio record: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravityValues, 0, 3)
                hasGravity = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
                hasGeomagnetic = true
            }
        }

        if (hasGravity && hasGeomagnetic) {
            val rotationMatrix = FloatArray(9)
            val inclinationMatrix = FloatArray(9)
            val success = SensorManager.getRotationMatrix(
                rotationMatrix,
                inclinationMatrix,
                gravityValues,
                geomagneticValues
            )

            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                // Convert radians to degrees
                currentAzimuthDeg = Math.toDegrees(orientation[0].toDouble())
                currentPitchDeg = Math.toDegrees(orientation[1].toDouble())
                currentRollDeg = Math.toDegrees(orientation[2].toDouble())
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
