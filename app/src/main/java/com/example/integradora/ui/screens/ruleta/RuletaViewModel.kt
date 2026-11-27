package com.example.integradora.ui.screens.ruleta

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.random.Random

// Números rojos en la ruleta europea
val NUMEROS_ROJOS = setOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)

// Orden exacto de la rueda de ruleta europea (37 casillas)
val ROULETTE_WHEEL_NUMBERS = listOf(
    0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26
)

data class RuletaRonda(
    val numero: Int,
    val color: String,
    val apuesta: String,
    val gano: Boolean,
    val ganancia: Int
)

class RuletaViewModel : ViewModel() {

    // Empieza en 0: el usuario acumula fichas desde la bandeja inferior
    var montoApuesta by mutableIntStateOf(0)
        private set

    // Sin preselección inicial: debe ser explícita cada ronda ("ROJO", "VERDE", "NEGRO")
    var tipoApuesta by mutableStateOf("")
        private set

    var numeroGanador by mutableIntStateOf(0)
        private set

    var colorGanador by mutableStateOf("VERDE")
        private set

    var isGirando by mutableStateOf(false)
        private set

    var mensajeResultado by mutableStateOf("Elige tu color, coloca fichas y gira")
        private set

    var esGanador by mutableStateOf<Boolean?>(null)
        private set

    // Historial de las últimas 5 rondas
    var historial by mutableStateOf<List<RuletaRonda>>(emptyList())
        private set

    // Animación de rotación de la rueda
    val rotacion = Animatable(0f)

    // Acumular fichas sin superar el saldo disponible
    fun agregarFicha(valor: Int, saldoDisponible: Double = Double.MAX_VALUE) {
        if (!isGirando && (montoApuesta + valor) <= saldoDisponible) {
            montoApuesta += valor
        }
    }

    fun limpiarFichas() {
        if (!isGirando) {
            montoApuesta = 0
        }
    }

    fun doblarFichas(saldoDisponible: Double = Double.MAX_VALUE) {
        if (!isGirando && montoApuesta > 0) {
            val doble = montoApuesta * 2
            if (doble <= saldoDisponible) {
                montoApuesta = doble
            }
        }
    }

    fun onTipoApuestaChanged(nuevoTipo: String) {
        if (!isGirando) {
            tipoApuesta = nuevoTipo
        }
    }

    suspend fun girarRuleta(
        saldoActual: Double,
        onDescontarSaldo: (Double) -> Unit,
        onSumarPremio: (Double) -> Unit
    ) {
        if (isGirando) return

        if (tipoApuesta.isBlank()) {
            mensajeResultado = "Selecciona un color: Rojo, Verde 0 o Negro"
            esGanador = false
            return
        }

        if (montoApuesta <= 0) {
            mensajeResultado = "Toca las fichas para apostar"
            esGanador = false
            return
        }

        if (montoApuesta > saldoActual) {
            mensajeResultado = "Saldo insuficiente para esta apuesta"
            esGanador = false
            return
        }

        isGirando = true
        mensajeResultado = "Girando ruleta..."
        esGanador = null
        onDescontarSaldo(montoApuesta.toDouble())

        try {
            // Elegir número ganador de los 37 posibles (0 al 36)
            val resultado = Random.nextInt(0, 37)
            val colorRes = when {
                resultado == 0 -> "VERDE"
                NUMEROS_ROJOS.contains(resultado) -> "ROJO"
                else -> "NEGRO"
            }

            // CÁLCULO PRECISO DEL ÁNGULO DE ROTACIÓN
            val targetIndex = ROULETTE_WHEEL_NUMBERS.indexOf(resultado)
            val sweep = 360f / 37f
            val centerAngle = targetIndex * sweep + sweep / 2f

            var desiredAngle = (270f - centerAngle) % 360f
            if (desiredAngle < 0f) desiredAngle += 360f

            val currentNorm = ((rotacion.value % 360f) + 360f) % 360f
            var diff = desiredAngle - currentNorm
            if (diff <= 0f) diff += 360f

            val targetRotation = rotacion.value + 360f * 5 + diff

            // Desaceleración suave física de ruleta
            val rouletteEasing = CubicBezierEasing(0.12f, 0.82f, 0.22f, 1.0f)
            rotacion.animateTo(
                targetValue = targetRotation,
                animationSpec = tween(durationMillis = 3200, easing = rouletteEasing)
            )

            // Normalizar ángulo tras detenerse para evitar desbordamiento numérico
            rotacion.snapTo(targetRotation % 360f)

            numeroGanador = resultado
            colorGanador = colorRes

            // Evaluar resultado: solo Rojo (2x), Verde (14x), Negro (2x)
            var gano = false
            var ganancia = 0.0

            when (tipoApuesta) {
                "ROJO" -> if (colorRes == "ROJO") { gano = true; ganancia = montoApuesta * 2.0 }
                "VERDE" -> if (colorRes == "VERDE") { gano = true; ganancia = montoApuesta * 14.0 }
                "NEGRO" -> if (colorRes == "NEGRO") { gano = true; ganancia = montoApuesta * 2.0 }
            }

            esGanador = gano
            if (gano) {
                onSumarPremio(ganancia)
                mensajeResultado = "Ganaste $${ganancia.toInt()} MXN — Cayó el $resultado ($colorRes)"
            } else {
                mensajeResultado = "Cayó el $resultado ($colorRes) — Suerte en la siguiente"
            }

            // Registrar en historial (máx 5 rondas)
            val ronda = RuletaRonda(
                numero = resultado,
                color = colorRes,
                apuesta = tipoApuesta,
                gano = gano,
                ganancia = if (gano) ganancia.toInt() else -montoApuesta
            )
            historial = (listOf(ronda) + historial).take(5)

            // Limpiar apuesta y selección
            montoApuesta = 0
            tipoApuesta = ""
        } catch (_: Exception) {
            // Protección contra cancelaciones para nunca dejar el juego trabado
        } finally {
            isGirando = false
        }
    }
}
