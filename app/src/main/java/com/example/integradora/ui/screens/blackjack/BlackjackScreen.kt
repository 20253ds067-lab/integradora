package com.example.integradora.ui.screens.blackjack

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.integradora.sensor.rememberProximityDetector
import com.example.integradora.ui.components.BandejaInferiorFichas
import com.example.integradora.ui.components.CirculoApuesta
import com.example.integradora.ui.screens.login.AuthViewModel
import com.example.integradora.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BlackjackScreen(
    viewModel: BlackjackViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    val puedeDoblar = viewModel.puedeDoblar(authViewModel.saldo)
    val puedeSplit = viewModel.puedeSplit(authViewModel.saldo)

    // ─── SENSOR DE PROXIMIDAD: pasar la mano = PLANTARSE (solo en estado JUGANDO) ───
    val proximityEnabled = viewModel.estado == EstadoJuego.JUGANDO && !viewModel.isSplit
    rememberProximityDetector(enabled = proximityEnabled) {
        viewModel.plantarse(onSumarPremio = authViewModel::sumarPremio)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CasinoBg,
        bottomBar = {
            // ─── RIEL INFERIOR CON ACCIONES Y BANDEJA DE FICHAS ───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CasinoSurface)
                    .navigationBarsPadding()
            ) {
                // Botones de acción contextual
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    when (viewModel.estado) {
                        EstadoJuego.ESPERANDO -> {
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.repartir(
                                            saldoActual = authViewModel.saldo,
                                            onDescontarSaldo = authViewModel::descontarSaldo,
                                            onSumarPremio = authViewModel::sumarPremio
                                        )
                                    }
                                },
                                enabled = viewModel.montoApuesta > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CasinoPrimary,
                                    disabledContainerColor = CasinoSurfaceElevated
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = if (viewModel.montoApuesta > 0)
                                        "REPARTIR  ($ %,d MXN)".format(viewModel.montoApuesta)
                                    else
                                        "COLOCA FICHAS EN LA MESA",
                                    color = if (viewModel.montoApuesta > 0) CasinoPrimaryDark else TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        EstadoJuego.JUGANDO -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Fila principal: PEDIR CARTA y PLANTARME
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.pedirCarta(onSumarPremio = authViewModel::sumarPremio)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CasinoPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    ) {
                                        Text(
                                            "PEDIR CARTA",
                                            color = CasinoPrimaryDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.plantarse(onSumarPremio = authViewModel::sumarPremio)
                                        },
                                        border = BorderStroke(1.5.dp, CasinoGold),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    ) {
                                        Text(
                                            "PLANTARME",
                                            color = CasinoGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Fila secundaria dinámica: DOBLAR y SPLIT
                                if (puedeDoblar || puedeSplit) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (puedeDoblar) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        viewModel.doblar(
                                                            saldoActual = authViewModel.saldo,
                                                            onDescontarSaldo = authViewModel::descontarSaldo,
                                                            onSumarPremio = authViewModel::sumarPremio
                                                        )
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CasinoSurfaceElevated),
                                                border = BorderStroke(1.dp, CasinoGold),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                            ) {
                                                Text(
                                                    "DOBLAR",
                                                    color = CasinoGold,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        if (puedeSplit) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        viewModel.split(
                                                            saldoActual = authViewModel.saldo,
                                                            onDescontarSaldo = authViewModel::descontarSaldo
                                                        )
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                                border = BorderStroke(1.dp, Color(0xFF60A5FA)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                            ) {
                                                Text(
                                                    "SPLIT (DIVIDIR)",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        EstadoJuego.RESULTADO -> {
                            Button(
                                onClick = { viewModel.nuevaRonda() },
                                colors = ButtonDefaults.buttonColors(containerColor = CasinoPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    "NUEVA RONDA",
                                    color = CasinoPrimaryDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        EstadoJuego.DEALER -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CasinoSurfaceElevated,
                                border = BorderStroke(1.dp, CasinoBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "Turno de la banca...",
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Bandeja de fichas estándar y dinámica
                BandejaInferiorFichas(
                    saldoDisponible = authViewModel.saldo,
                    montoTotalApostado = viewModel.montoApuesta,
                    onAgregarFicha = { v -> viewModel.agregarFicha(v, authViewModel.saldo) },
                    onLimpiar = viewModel::limpiarFichas,
                    onDoblar = { viewModel.doblarFichas(authViewModel.saldo) },
                    enabled = viewModel.estado == EstadoJuego.ESPERANDO
                )
            }
        }
    ) { innerPadding ->
        // ─── PAÑO DE BLACKJACK ───
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barra superior: Volver y Saldo limpio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextPrimary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Menú", color = TextPrimary, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CasinoSurfaceElevated,
                    border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$ %,.0f MXN".format(authViewModel.saldo),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ─── BANNER SENSOR DE PROXIMIDAD ───
            if (viewModel.estado == EstadoJuego.JUGANDO && !viewModel.isSplit) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CasinoSurfaceElevated,
                    border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Sensor activo",
                            color = CasinoGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Pasa la mano sobre el teléfono para PLANTARTE",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // ─── MESA SUPERIOR CON SHOE (MAZO) Y DEALER ───
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Mazo visible (Shoe de casino) en la esquina superior derecha
                MazoShoe(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp)
                )

                // Cartas del dealer al centro
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DEALER",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ManoCartas(cartas = viewModel.cartasDealer)

                        if (viewModel.cartasDealer.isNotEmpty()) {
                            val pts = if (viewModel.estado == EstadoJuego.JUGANDO)
                                viewModel.puntosDealer
                            else
                                viewModel.puntosDealerTotal
                            Spacer(Modifier.width(8.dp))
                            BadgePuntuacion(pts = "$pts")
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ─── INSCRIPCIÓN CLÁSICA DE MESA DE CASINO EN ESPAÑOL ───
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, CasinoBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BLACKJACK PAGA 3 A 2",
                        color = CasinoGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "El dealer pide carta con 16 y se planta con 17",
                        color = TextMuted,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ─── MENSAJE DE RESULTADO O ESTADO ───
            if (viewModel.esGanador != null || viewModel.estado == EstadoJuego.RESULTADO) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CasinoSurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        when (viewModel.esGanador) {
                            true  -> CasinoGold
                            false -> CasinoRed
                            null  -> CasinoBorder
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.mensajeResultado,
                        color = when (viewModel.esGanador) {
                            true  -> CasinoGold
                            false -> Color(0xFFFCA5A5)
                            null  -> TextPrimary
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // ─── MANO DEL JUGADOR (O DOS MANOS SI HUBO SPLIT) ───
            if (!viewModel.isSplit) {
                // Modo estándar (1 mano)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TU MANO",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ManoCartas(cartas = viewModel.cartasJugador)

                        if (viewModel.cartasJugador.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            BadgePuntuacion(pts = "${viewModel.puntosJugador}")
                        }
                    }
                }
            } else {
                // Modo Split (2 manos independientes lado a lado como en la referencia)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mano 1
                    val activo1 = viewModel.manoActivaIndex == 0 && viewModel.estado == EstadoJuego.JUGANDO
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (activo1) CasinoSurfaceHover else CasinoSurface,
                        border = BorderStroke(
                            if (activo1) 2.dp else 1.dp,
                            if (activo1) CasinoGold else CasinoBorder
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "MANO 1",
                                color = if (activo1) CasinoGold else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                ManoCartas(cartas = viewModel.cartasJugador)
                                if (viewModel.cartasJugador.isNotEmpty()) {
                                    Spacer(Modifier.width(4.dp))
                                    BadgePuntuacion(pts = "${viewModel.puntosJugador}")
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "$ ${viewModel.montoApuesta} MXN",
                                color = CasinoGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Mano 2
                    val activo2 = viewModel.manoActivaIndex == 1 && viewModel.estado == EstadoJuego.JUGANDO
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (activo2) CasinoSurfaceHover else CasinoSurface,
                        border = BorderStroke(
                            if (activo2) 2.dp else 1.dp,
                            if (activo2) CasinoGold else CasinoBorder
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "MANO 2",
                                color = if (activo2) CasinoGold else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                ManoCartas(cartas = viewModel.cartasJugadorMano2)
                                if (viewModel.cartasJugadorMano2.isNotEmpty()) {
                                    Spacer(Modifier.width(4.dp))
                                    BadgePuntuacion(pts = "${viewModel.puntosJugadorMano2}")
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "$ ${viewModel.montoApuestaMano2} MXN",
                                color = CasinoGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ─── CÍRCULO DE FICHAS EN EL TAPETE (TOCAR PARA RETIRAR APUESTA) ───
            CirculoApuesta(
                montoApostado = viewModel.montoApuesta + viewModel.montoApuestaMano2,
                etiqueta = if (viewModel.isSplit) "Apuesta Total" else "Apuesta en Mesa",
                onClick = {
                    if (viewModel.estado == EstadoJuego.ESPERANDO) {
                        viewModel.limpiarFichas()
                    }
                }
            )

            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * Visual de Mazo / Shoe de casino con cartas inclinadas en ángulo.
 */
@Composable
fun MazoShoe(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .rotate(-18f)
            .width(52.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
            .border(1.5.dp, CasinoGold.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(3.dp)
    ) {
        // Trama diagonal de naipe casino
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF172554))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
        )
    }
}

/**
 * Badge circular negro con puntuación.
 */
@Composable
fun BadgePuntuacion(pts: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(0xFF0F172A))
            .border(1.5.dp, CasinoGold, CircleShape)
    ) {
        Text(
            text = pts,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

// ─── VISUALIZACIÓN DE CARTAS ─────────────────────────────────────────────────
@Composable
fun ManoCartas(cartas: List<Carta>) {
    if (cartas.isEmpty()) {
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(76.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("— Sin cartas —", color = TextMuted, fontSize = 11.sp)
        }
        return
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy((-24).dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cartas.forEach { carta ->
            CartaItem(carta = carta)
        }
    }
}

@Composable
fun CartaItem(carta: Carta) {
    val esPaloRojo = carta.palo == "♥" || carta.palo == "♦"
    val colorPalo = if (esPaloRojo) Color(0xFFDC2626) else Color(0xFF18181B)
    val bgColor = if (carta.oculta) Color(0xFF1E293B) else Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .width(52.dp)
            .height(76.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, if (carta.oculta) CasinoGold.copy(alpha = 0.5f) else Color(0xFFCBD5E1), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (carta.oculta) {
            // Reverso con trama de naipe azul marino
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF172554))
                        )
                    )
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Esquina superior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = carta.nombre,
                        color = colorPalo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = carta.palo,
                        color = colorPalo,
                        fontSize = 10.sp
                    )
                }
                // Palo grande al centro
                Spacer(Modifier.weight(1f))
                Text(
                    text = carta.palo,
                    color = colorPalo,
                    fontSize = 22.sp
                )
                Spacer(Modifier.weight(1f))
                // Esquina inferior
                Text(
                    text = carta.nombre,
                    color = colorPalo,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
