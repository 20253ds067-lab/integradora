package com.example.integradora.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detector de giro rotacional físico calibrado para activar la Ruleta sin congelamientos.
 * Cuenta con un seguro booleano atómico (hasTriggered) y debounce para garantizar
 * que solo se lance una animación por giro y no se solapen eventos.
 */
class RotationDetector(
    private val context: Context,
    private val onRotation: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    @Volatile
    private var hasTriggered = false

    private var lastRotationTimestamp: Long = 0
    private val debounceTimeMs: Long = 1200

    // Umbrales calibrados para respuesta ágil pero estable
    private val angularSpeedThreshold = 2.2f

    // Fallback con acelerómetro (para emuladores o dispositivos sin giroscopio)
    private var lastAccX: Float = 0f
    private var lastAccY: Float = 0f

    fun start() {
        hasTriggered = false
        lastRotationTimestamp = System.currentTimeMillis()
        if (gyroscope != null) {
            sensorManager?.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        } else if (accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        hasTriggered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || hasTriggered) return

        val now = System.currentTimeMillis()
        if (now - lastRotationTimestamp < debounceTimeMs) return

        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val rotX = event.values[0]
            val rotY = event.values[1]
            val rotZ = event.values[2]

            val angularSpeed = sqrt((rotX * rotX + rotY * rotY + rotZ * rotZ).toDouble()).toFloat()

            // Giro angular perceptible en cualquier eje o giro plano en Z
            if (angularSpeed > angularSpeedThreshold || abs(rotZ) > 1.8f) {
                hasTriggered = true
                lastRotationTimestamp = now
                emitirVibracion()
                onRotation()
            }
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Fallback con acelerómetro si no hay giroscopio físico
            val x = event.values[0]
            val y = event.values[1]
            val deltaX = abs(x - lastAccX)
            val deltaY = abs(y - lastAccY)

            lastAccX = x
            lastAccY = y

            if (deltaX > 5.5f || deltaY > 5.5f) {
                hasTriggered = true
                lastRotationTimestamp = now
                emitirVibracion()
                onRotation()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun emitirVibracion() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(70)
                }
            }
        } catch (_: Exception) {}
    }
}

/**
 * Composable helper para registrar el detector de rotación/giro en Jetpack Compose.
 */
@Composable
fun rememberRotationDetector(
    enabled: Boolean = true,
    onRotation: () -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        val detector = RotationDetector(context, onRotation)
        detector.start()

        onDispose {
            detector.stop()
        }
    }
}
