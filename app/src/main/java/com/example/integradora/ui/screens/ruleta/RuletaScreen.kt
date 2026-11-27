package com.example.integradora.ui.screens.ruleta

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.integradora.sensor.rememberRotationDetector
import com.example.integradora.ui.components.BandejaInferiorFichas
import com.example.integradora.ui.components.CirculoApuesta
import com.example.integradora.ui.screens.login.AuthViewModel
import com.example.integradora.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RuletaScreen(
    viewModel: RuletaViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val puedeGirar = !viewModel.isGirando && viewModel.montoApuesta > 0 && viewModel.tipoApuesta.isNotBlank()

    // Sensor de movimiento: Giroscopio con detección de giro físico
    rememberRotationDetector(enabled = puedeGirar) {
        if (!viewModel.isGirando) {
            coroutineScope.launch {
                viewModel.girarRuleta(
                    saldoActual = authViewModel.saldo,
                    onDescontarSaldo = authViewModel::descontarSaldo,
                    onSumarPremio = authViewModel::sumarPremio
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CasinoBg)
    ) {
        // --- Barra superior limpia ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
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

            // Saldo  cartera dorada
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

        HorizontalDivider(color = CasinoBorder)

        // --- Contenido central desplazable ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ruleta Clásica",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Indicador de Sensor Giroscopio
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (puedeGirar) CasinoSurfaceHover else CasinoSurface,
                border = BorderStroke(1.dp, if (puedeGirar) CasinoGold.copy(alpha = 0.6f) else CasinoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = "Sensor Giroscopio",
                        tint = if (puedeGirar) CasinoGold else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (puedeGirar)
                            "Giroscopio activo: ¡Gira tu teléfono físicamente para accionar la ruleta!"
                        else
                            "Giroscopio listo: Elige color y gira el teléfono para jugar",
                        color = if (puedeGirar) CasinoGold else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (puedeGirar) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Rueda de ruleta realista con acabado metálico/madera
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(viewModel.rotacion.value)
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2
                    val sweep = 360f / 37f

                    // Anillo exterior de madera/bronce oscuro
                    drawCircle(
                        color = Color(0xFF1E1711),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = CasinoGold.copy(alpha = 0.5f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3f)
                    )

                    // Pista de la bola (anillo de marfil oscuro)
                    val trackRadius = radius * 0.94f
                    drawCircle(
                        color = Color(0xFF2C241D),
                        radius = trackRadius,
                        center = center
                    )

                    // Casillas de la ruleta europea
                    val pocketRadius = radius * 0.86f
                    for (i in 0 until 37) {
                        val num = ROULETTE_WHEEL_NUMBERS[i]
                        val startAngle = i * sweep
                        val colorSector = when {
                            num == 0 -> Color(0xFF15803D) // Verde esmeralda
                            NUMEROS_ROJOS.contains(num) -> Color(0xFFB91C1C) // Rojo ruleta
                            else -> Color(0xFF11141A) // Negro obsidiana
                        }

                        drawArc(
                            color = colorSector,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                            size = Size(pocketRadius * 2, pocketRadius * 2),
                            topLeft = Offset(center.x - pocketRadius, center.y - pocketRadius)
                        )

                        // Separadores de metal (frets)
                        drawArc(
                            color = CasinoGold.copy(alpha = 0.45f),
                            startAngle = startAngle,
                            sweepAngle = 0.6f,
                            useCenter = true,
                            size = Size(pocketRadius * 2, pocketRadius * 2),
                            topLeft = Offset(center.x - pocketRadius, center.y - pocketRadius)
                        )
                    }

                    // Anillo interior de bronce
                    val innerRimRadius = radius * 0.48f
                    drawCircle(
                        color = Color(0xFF3E2D1E),
                        radius = innerRimRadius,
                        center = center
                    )
                    drawCircle(
                        color = CasinoGold,
                        radius = innerRimRadius,
                        center = center,
                        style = Stroke(width = 2f)
                    )

                    // Torreta/cono central de bronce torneado
                    val coneRadius = radius * 0.38f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFDE68A),
                                Color(0xFFD4AF37),
                                Color(0xFF78591A),
                                Color(0xFF2E1C0A)
                            ),
                            center = center,
                            radius = coneRadius
                        ),
                        radius = coneRadius,
                        center = center
                    )

                    // Perno central
                    drawCircle(
                        color = Color(0xFFFFFBEB),
                        radius = radius * 0.08f,
                        center = center
                    )
                }

                // Indicador / Puntero de caída en la parte superior (270°)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-6).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, CasinoGold, CircleShape)
                )

                // Insignia del número ganador al centro
                Surface(
                    shape = CircleShape,
                    color = when (viewModel.colorGanador) {
                        "ROJO" -> Color(0xFFB91C1C)
                        "NEGRO" -> Color(0xFF11141A)
                        else -> Color(0xFF15803D)
                    },
                    border = BorderStroke(2.dp, CasinoGold),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${viewModel.numeroGanador}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Mensaje de estado / resultado
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CasinoSurfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (viewModel.esGanador == true) CasinoGold else CasinoBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = viewModel.mensajeResultado,
                    color = if (viewModel.esGanador == true) CasinoGold else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Selector simplificado de color: Rojo (2x), Verde 0 (14x), Negro (2x)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CasinoSurface),
                border = BorderStroke(1.dp, CasinoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Selecciona tu color:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ROJO (2x)
                        val isRojo = viewModel.tipoApuesta == "ROJO"
                        Button(
                            onClick = { viewModel.onTipoApuestaChanged("ROJO") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRojo) Color(0xFFDC2626) else Color(0xFF451313)
                            ),
                            border = if (isRojo) BorderStroke(2.dp, CasinoGold) else BorderStroke(1.dp, Color(0xFF7F1D1D)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            enabled = !viewModel.isGirando
                        ) {
                            Text(
                                "ROJO 2x",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        // VERDE 0 (14x)
                        val isVerde = viewModel.tipoApuesta == "VERDE"
                        Button(
                            onClick = { viewModel.onTipoApuestaChanged("VERDE") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVerde) Color(0xFF16A34A) else Color(0xFF0F3B21)
                            ),
                            border = if (isVerde) BorderStroke(2.dp, CasinoGold) else BorderStroke(1.dp, Color(0xFF14532D)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            enabled = !viewModel.isGirando
                        ) {
                            Text(
                                "VERDE 14x",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        // NEGRO (2x)
                        val isNegro = viewModel.tipoApuesta == "NEGRO"
                        Button(
                            onClick = { viewModel.onTipoApuestaChanged("NEGRO") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNegro) Color(0xFF262626) else Color(0xFF171717)
                            ),
                            border = if (isNegro) BorderStroke(2.dp, CasinoGold) else BorderStroke(1.dp, Color(0xFF404040)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            enabled = !viewModel.isGirando
                        ) {
                            Text(
                                "NEGRO 2x",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Círculo de fichas apostadas sobre el tapete (tocar para retirar apuesta)
                    CirculoApuesta(
                        montoApostado = viewModel.montoApuesta,
                        onClick = {
                            if (!viewModel.isGirando) {
                                viewModel.limpiarFichas()
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
        }

        // --- Botón de acción principal ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.girarRuleta(
                            saldoActual = authViewModel.saldo,
                            onDescontarSaldo = authViewModel::descontarSaldo,
                            onSumarPremio = authViewModel::sumarPremio
                        )
                    }
                },
                enabled = !viewModel.isGirando && viewModel.montoApuesta > 0 && viewModel.tipoApuesta.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CasinoPrimary,
                    disabledContainerColor = CasinoSurfaceElevated
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (viewModel.isGirando) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CasinoPrimaryDark)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "GIRANDO...",
                        color = CasinoPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (viewModel.montoApuesta > 0 && viewModel.tipoApuesta.isNotBlank()) CasinoPrimaryDark else TextMuted)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            viewModel.tipoApuesta.isBlank() -> "SELECCIONA UN COLOR"
                            viewModel.montoApuesta <= 0 -> "COLOCA FICHAS EN LA MESA"
                            else -> "GIRAR RULETA ($${viewModel.montoApuesta})"
                        },
                        color = if (viewModel.montoApuesta > 0 && viewModel.tipoApuesta.isNotBlank()) CasinoPrimaryDark else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // --- Bandeja inferior unificada de fichas ---
        BandejaInferiorFichas(
            saldoDisponible = authViewModel.saldo,
            montoTotalApostado = viewModel.montoApuesta,
            onAgregarFicha = { valor -> viewModel.agregarFicha(valor, authViewModel.saldo) },
            onLimpiar = viewModel::limpiarFichas,
            onDoblar = { viewModel.doblarFichas(authViewModel.saldo) },
            enabled = !viewModel.isGirando
        )
    }
}
