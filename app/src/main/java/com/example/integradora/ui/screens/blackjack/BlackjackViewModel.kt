package com.example.integradora.ui.screens.blackjack

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class Carta(
    val valor: Int,      // 1=As, 2-10=valor, 11=J, 12=Q, 13=K
    val palo: String,    // "♠", "♥", "♦", "♣"
    val oculta: Boolean = false
) {
    val nombre: String get() = when (valor) {
        1 -> "A"
        11 -> "J"
        12 -> "Q"
        13 -> "K"
        else -> valor.toString()
    }
    // Valor en puntos para Blackjack
    val puntos: Int get() = when (valor) {
        1 -> 11   // As empieza como 11, se ajusta después
        in 11..13 -> 10
        else -> valor
    }
}

enum class EstadoJuego {
    ESPERANDO,   // Esperando apuesta
    JUGANDO,     // Turno del jugador
    DEALER,      // Turno del dealer (animación)
    RESULTADO    // Juego terminado, mostrando resultado
}

class BlackjackViewModel : ViewModel() {

    // --- BARAJA ---
    private val palos = listOf("♠", "♥", "♦", "♣")
    private val baraja = mutableListOf<Carta>()

    // --- ESTADO ---
    var estado by mutableStateOf(EstadoJuego.ESPERANDO)
        private set

    var cartasJugador by mutableStateOf<List<Carta>>(emptyList())
        private set

    var cartasJugadorMano2 by mutableStateOf<List<Carta>>(emptyList())
        private set

    var isSplit by mutableStateOf(false)
        private set

    var manoActivaIndex by mutableIntStateOf(0) // 0: Mano 1, 1: Mano 2
        private set

    var cartasDealer by mutableStateOf<List<Carta>>(emptyList())
        private set

    var mensajeResultado by mutableStateOf("Coloca tu apuesta y presiona REPARTIR")
        private set

    var esGanador by mutableStateOf<Boolean?>(null)
        private set

    // --- APUESTAS ---
    var montoApuesta by mutableIntStateOf(0)
        private set

    var montoApuestaMano2 by mutableIntStateOf(0)
        private set

    // --- PUNTUACIONES ---
    val puntosJugador: Int get() = calcularPuntos(cartasJugador)
    val puntosJugadorMano2: Int get() = calcularPuntos(cartasJugadorMano2)

    val puntosDealer: Int get() {
        val visibles = cartasDealer.filter { !it.oculta }
        return calcularPuntos(visibles)
    }
    val puntosDealerTotal: Int get() = calcularPuntos(cartasDealer)

    // --- FICHAS ---
    fun agregarFicha(valor: Int, saldoDisponible: Double) {
        if (estado == EstadoJuego.ESPERANDO && (montoApuesta + valor) <= saldoDisponible) {
            montoApuesta += valor
        }
    }

    fun limpiarFichas() {
        if (estado == EstadoJuego.ESPERANDO) montoApuesta = 0
    }

    fun doblarFichas(saldoDisponible: Double) {
        if (estado == EstadoJuego.ESPERANDO && montoApuesta > 0) {
            val doble = montoApuesta * 2
            if (doble <= saldoDisponible) montoApuesta = doble
        }
    }

    // --- VALIDACIONES DE ACCIÓN DINÁMICAS ---
    fun puedeDoblar(saldoDisponible: Double): Boolean {
        if (estado != EstadoJuego.JUGANDO) return false
        val manoActual = if (manoActivaIndex == 0) cartasJugador else cartasJugadorMano2
        val apuestaActual = if (manoActivaIndex == 0) montoApuesta else montoApuestaMano2
        return manoActual.size == 2 && saldoDisponible >= apuestaActual
    }

    fun puedeSplit(saldoDisponible: Double): Boolean {
        if (estado != EstadoJuego.JUGANDO || isSplit) return false
        if (cartasJugador.size != 2) return false
        val c1 = cartasJugador[0]
        val c2 = cartasJugador[1]
        // Se puede dividir si tienen el mismo valor nominal o ambas valen 10
        val mismoValor = c1.valor == c2.valor || (c1.puntos == 10 && c2.puntos == 10)
        return mismoValor && saldoDisponible >= montoApuesta
    }

    // --- BARAJA ---
    private fun armarBaraja() {
        baraja.clear()
        // Shoe de 4 barajas estándar de casino para mayor realismo
        repeat(4) {
            palos.forEach { palo ->
                (1..13).forEach { valor ->
                    baraja.add(Carta(valor, palo))
                }
            }
        }
        baraja.shuffle()
    }

    private fun robarCarta(oculta: Boolean = false): Carta {
        if (baraja.isEmpty()) armarBaraja()
        return baraja.removeAt(0).copy(oculta = oculta)
    }

    private fun calcularPuntos(cartas: List<Carta>): Int {
        var total = cartas.sumOf { it.puntos }
        var ases = cartas.count { it.valor == 1 }
        while (total > 21 && ases > 0) {
            total -= 10
            ases--
        }
        return total
    }

    // --- REPARTIR SECUENCIAL CON EFECTO DE SHOE ---
    suspend fun repartir(
        saldoActual: Double,
        onDescontarSaldo: (Double) -> Unit,
        onSumarPremio: (Double) -> Unit
    ) {
        if (estado != EstadoJuego.ESPERANDO) return
        if (montoApuesta <= 0) {
            mensajeResultado = "Coloca fichas en la mesa primero"
            return
        }
        if (montoApuesta > saldoActual) {
            mensajeResultado = "Saldo insuficiente para esta apuesta"
            return
        }

        onDescontarSaldo(montoApuesta.toDouble())
        armarBaraja()
        isSplit = false
        manoActivaIndex = 0
        montoApuestaMano2 = 0
        cartasJugador = emptyList()
        cartasDealer = emptyList()
        cartasJugadorMano2 = emptyList()
        esGanador = null
        estado = EstadoJuego.JUGANDO

        // Reparto secuencial pausado desde el mazo (animación fluida y cadencia natural)
        delay(320)
        cartasJugador = listOf(robarCarta())
        delay(360)
        cartasDealer = listOf(robarCarta())
        delay(360)
        cartasJugador = cartasJugador + robarCarta()
        delay(360)
        cartasDealer = cartasDealer + robarCarta(oculta = true)

        // Comprobar Blackjack natural inicial (21 en las dos primeras cartas)
        if (puntosJugador == 21) {
            mensajeResultado = "¡BLACKJACK NATURAL!"
            delay(500)
            dealerJuega(onSumarPremio)
        } else {
            mensajeResultado = "Tienes $puntosJugador pts. ¿Pedir carta o plantarte?"
        }
    }

    // --- PEDIR CARTA ---
    fun pedirCarta(onSumarPremio: (Double) -> Unit) {
        if (estado != EstadoJuego.JUGANDO) return

        if (!isSplit) {
            cartasJugador = cartasJugador + robarCarta()
            val pts = puntosJugador
            if (pts > 21) {
                val pD = puntosDealerTotal
                mensajeResultado = "Perdiste: Te pasaste ($pts pts) vs Dealer ($pD pts)"
                revelarDealer()
                esGanador = false
                estado = EstadoJuego.RESULTADO
                montoApuesta = 0
            } else if (pts == 21) {
                mensajeResultado = "¡21 Puntos! Turno del dealer..."
                viewModelScope.launch {
                    delay(400)
                    dealerJuega(onSumarPremio)
                }
            } else {
                mensajeResultado = "Tienes $pts pts. ¿Pedir carta o plantarte?"
            }
        } else {
            // Modo dos manos independientes (Split)
            if (manoActivaIndex == 0) {
                cartasJugador = cartasJugador + robarCarta()
                val pts = puntosJugador
                if (pts >= 21) {
                    manoActivaIndex = 1
                    mensajeResultado = if (pts > 21) "Mano 1 se pasó ($pts). Jugando Mano 2..." else "Mano 1 completa ($pts). Jugando Mano 2..."
                } else {
                    mensajeResultado = "Mano 1: $pts pts. ¿Pedir o plantarte?"
                }
            } else {
                cartasJugadorMano2 = cartasJugadorMano2 + robarCarta()
                val pts = puntosJugadorMano2
                if (pts >= 21) {
                    viewModelScope.launch {
                        finalizarTurnoJugadorSplit(onSumarPremio)
                    }
                } else {
                    mensajeResultado = "Mano 2: $pts pts. ¿Pedir o plantarte?"
                }
            }
        }
    }

    // --- PLANTARSE ---
    fun plantarse(onSumarPremio: (Double) -> Unit) {
        if (estado != EstadoJuego.JUGANDO) return

        if (!isSplit) {
            viewModelScope.launch {
                dealerJuega(onSumarPremio)
            }
        } else {
            if (manoActivaIndex == 0) {
                manoActivaIndex = 1
                mensajeResultado = "Mano 1 plantada ($puntosJugador pts). Jugando Mano 2..."
            } else {
                viewModelScope.launch {
                    finalizarTurnoJugadorSplit(onSumarPremio)
                }
            }
        }
    }

    // --- DOBLAR (DOUBLE DOWN) ---
    suspend fun doblar(
        saldoActual: Double,
        onDescontarSaldo: (Double) -> Unit,
        onSumarPremio: (Double) -> Unit
    ) {
        if (!puedeDoblar(saldoActual)) return

        if (!isSplit) {
            onDescontarSaldo(montoApuesta.toDouble())
            montoApuesta *= 2
            delay(250)
            cartasJugador = cartasJugador + robarCarta()
            dealerJuega(onSumarPremio)
        } else {
            if (manoActivaIndex == 0) {
                onDescontarSaldo(montoApuesta.toDouble())
                montoApuesta *= 2
                delay(250)
                cartasJugador = cartasJugador + robarCarta()
                manoActivaIndex = 1
                mensajeResultado = "Mano 1 doblada ($puntosJugador pts). Jugando Mano 2..."
            } else {
                onDescontarSaldo(montoApuestaMano2.toDouble())
                montoApuestaMano2 *= 2
                delay(250)
                cartasJugadorMano2 = cartasJugadorMano2 + robarCarta()
                finalizarTurnoJugadorSplit(onSumarPremio)
            }
        }
    }

    // --- SPLIT (DIVIDIR EN DOS MANOS) ---
    suspend fun split(
        saldoActual: Double,
        onDescontarSaldo: (Double) -> Unit
    ) {
        if (!puedeSplit(saldoActual)) return

        onDescontarSaldo(montoApuesta.toDouble())
        montoApuestaMano2 = montoApuesta
        isSplit = true
        manoActivaIndex = 0

        val c1 = cartasJugador[0]
        val c2 = cartasJugador[1]

        cartasJugador = listOf(c1)
        cartasJugadorMano2 = listOf(c2)

        delay(320)
        cartasJugador = listOf(c1, robarCarta())
        delay(320)
        cartasJugadorMano2 = listOf(c2, robarCarta())

        mensajeResultado = "Mano dividida: Mano 1 activa ($puntosJugador pts)"
    }

    private suspend fun dealerJuega(onSumarPremio: (Double) -> Unit) {
        estado = EstadoJuego.DEALER
        mensajeResultado = "Turno del dealer..."
        delay(600)
        revelarDealer()
        delay(850)
        while (puntosDealerTotal < 17) {
            cartasDealer = cartasDealer + robarCarta()
            delay(950) // Pausa realista para ver cada carta que roba el dealer
        }
        delay(450)
        resolverMano(onSumarPremio)
    }

    private suspend fun finalizarTurnoJugadorSplit(onSumarPremio: (Double) -> Unit) {
        estado = EstadoJuego.DEALER
        mensajeResultado = "Turno del dealer..."
        delay(600)
        revelarDealer()
        delay(850)
        while (puntosDealerTotal < 17) {
            cartasDealer = cartasDealer + robarCarta()
            delay(950)
        }
        delay(450)
        resolverSplit(onSumarPremio)
    }

    private fun revelarDealer() {
        cartasDealer = cartasDealer.map { it.copy(oculta = false) }
    }

    // --- RESOLUCIÓN MANO SIMPLE (MENSAJES CLAROS Y DIRECTOS) ---
    private fun resolverMano(onSumarPremio: (Double) -> Unit) {
        val pJ = puntosJugador
        val pD = puntosDealerTotal
        val apuesta = montoApuesta.toDouble()

        when {
            pJ > 21 -> {
                esGanador = false
                mensajeResultado = "Perdiste: Te pasaste ($pJ) vs Dealer ($pD)"
            }
            pD > 21 -> {
                esGanador = true
                onSumarPremio(apuesta * 2)
                mensajeResultado = "¡Ganaste! El Dealer se pasó ($pD) | Tú: $pJ"
            }
            pJ == 21 && cartasJugador.size == 2 -> {
                esGanador = true
                val premio = apuesta * 2.5
                onSumarPremio(premio)
                mensajeResultado = "¡Ganaste! ¡Blackjack Natural! (Pago 3:2)"
            }
            pJ > pD -> {
                esGanador = true
                onSumarPremio(apuesta * 2)
                mensajeResultado = "¡Ganaste! Tú: $pJ vs Dealer: $pD"
            }
            pJ == pD -> {
                esGanador = null
                onSumarPremio(apuesta) // Empate: se devuelve la apuesta
                mensajeResultado = "Empate: Tú: $pJ vs Dealer: $pD"
            }
            else -> {
                esGanador = false
                mensajeResultado = "Perdiste: Dealer: $pD vs Tú: $pJ"
            }
        }

        estado = EstadoJuego.RESULTADO
        montoApuesta = 0
    }

    // --- RESOLUCIÓN MANO SPLIT (DOS MANOS) ---
    private fun resolverSplit(onSumarPremio: (Double) -> Unit) {
        val pD = puntosDealerTotal
        var gananciaTotal = 0.0
        var alMenosUnGanador = false

        // Mano 1
        val p1 = puntosJugador
        val ap1 = montoApuesta.toDouble()
        val m1Res = when {
            p1 > 21 -> "Perdió ($p1)"
            pD > 21 || p1 > pD -> {
                gananciaTotal += ap1 * 2
                alMenosUnGanador = true
                "Ganó ($p1)"
            }
            p1 == pD -> {
                gananciaTotal += ap1
                "Empató ($p1)"
            }
            else -> "Perdió ($p1)"
        }

        // Mano 2
        val p2 = puntosJugadorMano2
        val ap2 = montoApuestaMano2.toDouble()
        val m2Res = when {
            p2 > 21 -> "Perdió ($p2)"
            pD > 21 || p2 > pD -> {
                gananciaTotal += ap2 * 2
                alMenosUnGanador = true
                "Ganó ($p2)"
            }
            p2 == pD -> {
                gananciaTotal += ap2
                "Empató ($p2)"
            }
            else -> "Perdió ($p2)"
        }

        if (gananciaTotal > 0) {
            onSumarPremio(gananciaTotal)
        }

        esGanador = when {
            alMenosUnGanador -> true
            gananciaTotal > 0 -> null
            else -> false
        }

        mensajeResultado = "Mano 1: $m1Res | Mano 2: $m2Res vs Dealer ($pD)"
        estado = EstadoJuego.RESULTADO
        montoApuesta = 0
        montoApuestaMano2 = 0
    }

    // --- NUEVA RONDA ---
    fun nuevaRonda() {
        estado = EstadoJuego.ESPERANDO
        cartasJugador = emptyList()
        cartasJugadorMano2 = emptyList()
        cartasDealer = emptyList()
        isSplit = false
        manoActivaIndex = 0
        montoApuestaMano2 = 0
        esGanador = null
        mensajeResultado = "Coloca tu apuesta y presiona REPARTIR"
    }
}
