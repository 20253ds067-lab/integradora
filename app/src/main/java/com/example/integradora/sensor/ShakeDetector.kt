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
 * Detector de sacudidas físicas (Shake) utilizando el acelerómetro del dispositivo.
 * Idéntico a la implementación verificada de Memorias_Velez_Gael:
 * Altamente reactivo en dispositivos reales y en emuladores de Android.
 */
class ShakeDetector(
    private val context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastTime: Long = 0
    private var lastShakeTimestamp: Long = 0
    private var shakeCount = 0

    fun start() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        shakeCount = 0
        lastTime = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()
        if (lastTime == 0L) {
            lastTime = now
            lastX = event.values[0]
            lastY = event.values[1]
            lastZ = event.values[2]
            return
        }

        val timeDiff = now - lastTime
        if (timeDiff < 70) return // Muestreo cada 70ms para calcular aceleración dinámica

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Diferencia vectorial: cancela al 100% la gravedad estática (9.8 m/s²) de la Tierra
        val deltaX = x - lastX
        val deltaY = y - lastY
        val deltaZ = z - lastZ

        lastTime = now
        lastX = x
        lastY = y
        lastZ = z

        // Magnitud de la aceleración dinámica
        val deltaAccel = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()

        // Una pulsación de dedo o sostener el teléfono genera deltaAccel < 4.0 m/s².
        // Agitar vigorosamente el dispositivo físico o emulador genera picos > 12.0 m/s².
        if (deltaAccel > 12.0f) {
            shakeCount++
            // Requiere al menos 2 picos de oscilación brusca
            if (shakeCount >= 2) {
                if (now - lastShakeTimestamp > 1400L) {
                    lastShakeTimestamp = now
                    shakeCount = 0
                    emitirVibracion()
                    onShake()
                }
            }
        } else if (now - lastShakeTimestamp > 700L) {
            shakeCount = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun emitirVibracion() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100L)
                }
            }
        } catch (_: Exception) {}
    }
}

/**
 * Composable helper para registrar el detector de shake permanentemente
 * durante el ciclo de vida de la pantalla de Dados, igual a Memorias.
 */
@Composable
fun rememberShakeDetector(onShake: () -> Unit) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val detector = ShakeDetector(context, onShake)
        detector.start()

        onDispose {
            detector.stop()
        }
    }
}
