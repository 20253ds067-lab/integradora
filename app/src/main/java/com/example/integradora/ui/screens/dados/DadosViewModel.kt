package com.example.integradora.ui.screens.dados

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.integradora.data.remote.RetrofitClient
import com.example.integradora.data.remote.dto.IniciarPartidaRequest
import com.example.integradora.data.remote.dto.RealizarTiroRequest
import kotlinx.coroutines.delay
import kotlin.random.Random

class DadosViewModel : ViewModel() {

    var dado1 by mutableIntStateOf(3)
        private set

    var dado2 by mutableIntStateOf(4)
        private set

    // Monto acumulable con fichas — empieza en 0
    var montoApuesta by mutableIntStateOf(0)
        private set

    var tipoApuesta by mutableStateOf("MAYOR_7") // "MENOR_7", "EXACTO_7", "MAYOR_7", "PAR", "IMPAR"
        private set

    var isLanzando by mutableStateOf(false)
        private set

    var mensajeResultado by mutableStateOf("Elige tu pronóstico y lanza los dados")
        private set

    var esGanador by mutableStateOf<Boolean?>(null)
        private set

    val rotacionDados = Animatable(0f)

    fun agregarFicha(valor: Int, saldoDisponible: Double = Double.MAX_VALUE) {
        if (!isLanzando && (montoApuesta + valor) <= saldoDisponible) {
            montoApuesta += valor
        }
    }

    fun limpiarFichas() {
        if (!isLanzando) {
            montoApuesta = 0
        }
    }

    fun doblarFichas(saldoDisponible: Double = Double.MAX_VALUE) {
        if (!isLanzando && montoApuesta > 0) {
            val doble = montoApuesta * 2
            if (doble <= saldoDisponible) {
                montoApuesta = doble
            }
        }
    }

    fun onTipoApuestaChanged(nuevoTipo: String) {
        if (!isLanzando) {
            tipoApuesta = nuevoTipo
        }
    }

    fun notificarDebeApostar() {
        if (!isLanzando) {
            mensajeResultado = "Coloca tus fichas en la mesa antes de agitar"
        }
    }

    suspend fun lanzarDados(
        saldoActual: Double,
        jugadorId: Long? = null,
        onDescontarSaldo: (Double) -> Unit,
        onSumarPremio: (Double) -> Unit
    ) {
        if (isLanzando) return
        if (montoApuesta <= 0) {
            mensajeResultado = "Coloca tus fichas en la mesa antes de lanzar"
            esGanador = false
            return
        }
        if (montoApuesta > saldoActual) {
            mensajeResultado = "Saldo insuficiente para esta apuesta"
            esGanador = false
            return
        }

        isLanzando = true
        mensajeResultado = "Lanzando dados..."
        esGanador = null
        val apuestaRonda = montoApuesta.toDouble()
        onDescontarSaldo(apuestaRonda)

        // Petición asíncrona a la API REST de Spring Boot si está disponible
        var serverDados: Pair<Int, Int>? = null
        if (jugadorId != null) {
            try {
                val initResp = RetrofitClient.apiService.iniciarPartida(
                    IniciarPartidaRequest(jugadorId = jugadorId, apuesta = apuestaRonda)
                )
                if (initResp.isSuccessful && initResp.body()?.id != null) {
                    val partidaId = initResp.body()!!.id!!
                    val tiroResp = RetrofitClient.apiService.realizarTiro(
                        RealizarTiroRequest(partidaId = partidaId)
                    )
                    if (tiroResp.isSuccessful && tiroResp.body() != null) {
                        val t = tiroResp.body()!!
                        serverDados = Pair(t.valorDado1, t.valorDado2)
                    }
                }
            } catch (_: Exception) {
                // Si el backend no responde, continúa en modo local sin interrupción
            }
        }

        // Animación progresiva más lenta y realista con desaceleración natural
        val tumbleDelays = listOf(80L, 90L, 110L, 130L, 160L, 200L, 250L, 320L, 400L)
        for (pause in tumbleDelays) {
            dado1 = Random.nextInt(1, 7)
            dado2 = Random.nextInt(1, 7)
            delay(pause)
        }

        // Giro final suave que se detiene en seco
        rotacionDados.animateTo(
            targetValue = rotacionDados.value + 360f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )

        // Asentamiento en dados definitivos (desde Spring Boot o aleatorio local)
        val res1 = serverDados?.first ?: Random.nextInt(1, 7)
        val res2 = serverDados?.second ?: Random.nextInt(1, 7)
        dado1 = res1
        dado2 = res2
        val suma = res1 + res2

        var gano = false
        var ganancia = 0.0

        when (tipoApuesta) {
            "MENOR_7" -> if (suma < 7) { gano = true; ganancia = apuestaRonda * 2.0 }
            "EXACTO_7" -> if (suma == 7) { gano = true; ganancia = apuestaRonda * 4.0 }
            "MAYOR_7" -> if (suma > 7) { gano = true; ganancia = apuestaRonda * 2.0 }
            "PAR" -> if (suma % 2 == 0) { gano = true; ganancia = apuestaRonda * 2.0 }
            "IMPAR" -> if (suma % 2 != 0) { gano = true; ganancia = apuestaRonda * 2.0 }
        }

        esGanador = gano
        if (gano) {
            onSumarPremio(ganancia)
            val origen = if (serverDados != null) " [API Spring Boot]" else ""
            mensajeResultado = "Ganaste $${ganancia.toInt()} MXN — Salió $res1 y $res2 (Suma: $suma)$origen"
        } else {
            val origen = if (serverDados != null) " [API Spring Boot]" else ""
            mensajeResultado = "Salió $res1 y $res2 (Suma: $suma)$origen — Suerte en la siguiente"
        }

        // Limpiar apuesta para la siguiente ronda
        montoApuesta = 0
        isLanzando = false
    }
}
