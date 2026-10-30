package com.learntogether.app.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects a phone movement that starts a voice command, so a learner can begin without finding
 * the voice button. Must be used from the main thread.
 *
 *  - Shake: three quick hard movements within a second.
 *  - Twist: a quick wrist twist about the phone's long axis, then back.
 *
 * Sensors are only read while the app is on screen (resume/pause), which also saves battery.
 */
class MotionTrigger(context: Context, private val onTrigger: () -> Unit) : SensorEventListener {

    enum class Mode(val key: String, val label: String) {
        Off("off", "Off"),
        Shake("shake", "Shake"),
        Twist("twist", "Twist");

        companion object {
            fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: Off
        }
    }

    private val sensors = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var mode = Mode.Off
    private var sensitivity = 1 // 0 = gentle, 1 = normal, 2 = firm
    private var resumed = false

    // Shake state
    private var spikes = 0
    private var firstSpikeMs = 0L
    private var lastSpikeMs = 0L

    // Twist state
    private var lastSign = 0
    private var lastPeakMs = 0L

    private var lastFireMs = 0L

    fun isSupported(mode: Mode): Boolean = when (mode) {
        Mode.Off -> true
        Mode.Shake -> accelerometer != null
        Mode.Twist -> gyroscope != null
    }

    fun configure(mode: Mode, sensitivity: Int) {
        this.mode = mode
        this.sensitivity = sensitivity.coerceIn(0, 2)
        register()
    }

    fun resume() {
        resumed = true
        register()
    }

    fun pause() {
        resumed = false
        sensors.unregisterListener(this)
    }

    private fun register() {
        sensors.unregisterListener(this)
        resetState()
        if (!resumed) return
        val sensor = when (mode) {
            Mode.Off -> null
            Mode.Shake -> accelerometer
            Mode.Twist -> gyroscope
        } ?: return
        sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun resetState() {
        spikes = 0
        lastSign = 0
    }

    override fun onSensorChanged(event: SensorEvent) {
        val nowMs = event.timestamp / 1_000_000L
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> if (mode == Mode.Shake) onAcceleration(event.values, nowMs)
            Sensor.TYPE_GYROSCOPE -> if (mode == Mode.Twist) onRotation(event.values[1], nowMs)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun onAcceleration(v: FloatArray, nowMs: Long) {
        val g = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]) / SensorManager.GRAVITY_EARTH
        if (g < SHAKE_G[sensitivity]) return
        if (nowMs - lastSpikeMs < SPIKE_GAP_MS) return
        lastSpikeMs = nowMs
        if (spikes == 0 || nowMs - firstSpikeMs > SHAKE_WINDOW_MS) {
            firstSpikeMs = nowMs
            spikes = 0
        }
        spikes++
        if (spikes >= SPIKES_NEEDED) fire(nowMs)
    }

    /** [rate] is the rotation speed in rad/s around the phone's long (Y) axis. */
    private fun onRotation(rate: Float, nowMs: Long) {
        if (abs(rate) < TWIST_RATE[sensitivity]) return
        val sign = if (rate > 0) 1 else -1
        when {
            sign == lastSign -> lastPeakMs = nowMs // still turning the same way
            lastSign != 0 && nowMs - lastPeakMs <= TWIST_WINDOW_MS -> {
                lastSign = 0
                fire(nowMs) // turned, then turned back
            }
            else -> {
                lastSign = sign
                lastPeakMs = nowMs
            }
        }
    }

    private fun fire(nowMs: Long) {
        resetState()
        if (nowMs - lastFireMs < COOLDOWN_MS) return
        lastFireMs = nowMs
        onTrigger()
    }

    private companion object {
        /** Needed strength in g (resting phone = 1 g), for gentle / normal / firm. */
        val SHAKE_G = floatArrayOf(1.8f, 2.4f, 3.0f)
        /** Needed rotation speed in rad/s, for gentle / normal / firm. */
        val TWIST_RATE = floatArrayOf(3.0f, 4.5f, 6.0f)

        const val SPIKES_NEEDED = 3
        const val SPIKE_GAP_MS = 120L
        const val SHAKE_WINDOW_MS = 1000L
        const val TWIST_WINDOW_MS = 700L
        const val COOLDOWN_MS = 2000L
    }
}
