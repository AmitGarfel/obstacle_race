package com.example.obstacle_race.utilities

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorManagerHelper(
    context: Context,
    private val moveLeft: () -> Unit,
    private val moveRight: () -> Unit,
    private val adjustSpeed: (Long) -> Unit
) {

    companion object {
        private const val MOVE_DELAY_MS = 380L
        private const val SPEED_DELAY_MS = 350L

        private const val TILT_X_THRESHOLD = 3.0f
        private const val TILT_Y_DELTA_THRESHOLD = 1.2f

        private const val MIN_SPEED_MS = 150L
        private const val MAX_SPEED_MS = 1000L
        private const val SPEED_STEP = 50L

        private const val ALPHA = 0.85f
    }

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastMoveTime = 0L
    private var lastSpeedTime = 0L

    private var currentSpeedMs = 600L

    private var filteredX = 0f
    private var filteredY = 0f

    private var baseY: Float? = null

    private val listener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            val now = System.currentTimeMillis()

            val rawX = -event.values[0]
            val rawY = event.values[1]

            filteredX = ALPHA * filteredX + (1 - ALPHA) * rawX
            filteredY = ALPHA * filteredY + (1 - ALPHA) * rawY

            if (baseY == null) baseY = filteredY
            val deltaY = filteredY - (baseY ?: 0f)

            if (now - lastMoveTime > MOVE_DELAY_MS) {
                when {
                    filteredX < -TILT_X_THRESHOLD -> {
                        moveRight()
                        lastMoveTime = now
                    }
                    filteredX > TILT_X_THRESHOLD -> {
                        moveLeft()
                        lastMoveTime = now
                    }
                }
            }

            if (now - lastSpeedTime > SPEED_DELAY_MS) {
                when {
                    deltaY < -TILT_Y_DELTA_THRESHOLD -> updateSpeed(now, currentSpeedMs - SPEED_STEP)
                    deltaY > TILT_Y_DELTA_THRESHOLD -> updateSpeed(now, currentSpeedMs + SPEED_STEP)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun setInitialSpeed(speedMs: Long) {
        currentSpeedMs = speedMs.coerceIn(MIN_SPEED_MS, MAX_SPEED_MS)
    }

    // Uses accelerometer tilt: X for lane change, Y delta for speed control.
    fun start() {
        baseY = null
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }

    private fun updateSpeed(now: Long, targetSpeed: Long) {
        val newSpeed = targetSpeed.coerceIn(MIN_SPEED_MS, MAX_SPEED_MS)
        if (newSpeed != currentSpeedMs) {
            currentSpeedMs = newSpeed
            adjustSpeed(currentSpeedMs)
            lastSpeedTime = now
        }
    }
}
