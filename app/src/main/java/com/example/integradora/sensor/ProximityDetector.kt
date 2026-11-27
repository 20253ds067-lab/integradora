package com.example.integradora.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Sensor de Proximidad — Blackjack
 *
 * El jugador pasa la mano sobre la parte superior del teléfono (como en un
 * casino real cuando indica "STAND" / PLANTARSE sin tocar la mesa).
 *
 * Lógica:
 *  - Se suscribe al Sensor.TYPE_PROXIMITY
 *  - Cuando el valor baja a < maxRange * 0.5 (objeto cerca) → dispara el callback
 *  - Luego cuando el valor vuelve a subir (mano alejada) resetea el lock
 *  - Debounce de 1500ms para evitar disparo múltiple
 *  - Fallback: si el sensor reporta maxRange = 0 no hace nada (hardware no disponible)
 */
class ProximityDetector(
    private val context: Context,
    private val onProximity: () -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val proximitySensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    @Volatile
    private var hasTriggered = false

    @Volatile
    private var lastTriggerTime = 0L

    private var maxRange = 5f   // valor por defecto; se actualiza en primer evento

    fun start() {
        proximitySensor?.let {
            maxRange = it.maximumRange
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        hasTriggered = false
        lastTriggerTime = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return

        val distance = event.values[0]
        val threshold = if (maxRange > 0f) maxRange * 0.5f else 3f
        val now = System.currentTimeMillis()

        if (distance < threshold) {
            // Mano cerca → trigger PLANTARSE
            if (!hasTriggered && (now - lastTriggerTime) > DEBOUNCE_MS) {
                hasTriggered = true
                lastTriggerTime = now
                vibrateHaptic()
                onProximity()
            }
        } else {
            // Mano alejada → resetear lock para permitir el próximo gesto
            hasTriggered = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun vibrateHaptic() {
        try {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(
                VibrationEffect.createOneShot(90L, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } catch (_: Exception) { /* Ignorar si no hay vibrador */ }
    }

    companion object {
        private const val DEBOUNCE_MS = 1500L
    }
}

/**
 * Composable helper que gestiona el ciclo de vida del ProximityDetector.
 *
 * @param enabled   Solo activo cuando el juego está en estado JUGANDO
 * @param onProximity Callback ejecutado cuando se detecta la mano encima
 */
@Composable
fun rememberProximityDetector(
    enabled: Boolean,
    onProximity: () -> Unit
) {
    val context = LocalContext.current
    val detector = remember { ProximityDetector(context, onProximity) }

    DisposableEffect(enabled) {
        if (enabled) {
            detector.start()
        } else {
            detector.stop()
        }
        onDispose {
            detector.stop()
        }
    }
}
