package com.example.integradora.ui.screens.dados

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.integradora.sensor.rememberShakeDetector
import com.example.integradora.ui.components.BandejaInferiorFichas
import com.example.integradora.ui.components.CirculoApuesta
import com.example.integradora.ui.screens.login.AuthViewModel
import com.example.integradora.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DadosScreen(
    viewModel: DadosViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var tiempoUltimaInteraccion by remember { mutableLongStateOf(0L) }

    // Sensor de movimiento: Acelerómetro con detección de sacudida (Shake estilo Memorias)
    // Se comprueba primero que tenga una apuesta colocada (montoApuesta > 0) para tirar.
    // Incluye ventana de enfriamiento de 800ms para evitar falsos positivos al tocar fichas.
    rememberShakeDetector {
        val now = System.currentTimeMillis()
        if (now - tiempoUltimaInteraccion < 800L) return@rememberShakeDetector

        if (!viewModel.isLanzando) {
            if (viewModel.montoApuesta > 0) {
                coroutineScope.launch {
                    viewModel.lanzarDados(
                        saldoActual = authViewModel.saldo,
                        jugadorId = authViewModel.jugadorId,
                        onDescontarSaldo = authViewModel::descontarSaldo,
                        onSumarPremio = authViewModel::sumarPremio
                    )
                }
            } else {
                viewModel.notificarDebeApostar()
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

            // Saldo con cartera dorada
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
                text = "Juego de Dados",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Indicador de Sensor Háptico / Acelerómetro
            val tieneApuesta = viewModel.montoApuesta > 0
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (tieneApuesta) CasinoSurfaceHover else CasinoSurface,
                border = BorderStroke(1.dp, if (tieneApuesta) CasinoGold.copy(alpha = 0.6f) else CasinoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Sensor Acelerómetro",
                        tint = if (tieneApuesta) CasinoGold else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (tieneApuesta)
                            "Sensor activo: ¡Agita tu teléfono para lanzar los dados!"
                        else
                            "Coloca fichas en la mesa y luego agita el teléfono",
                        color = if (tieneApuesta) CasinoGold else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (tieneApuesta) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Tapete visual de dados
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CasinoSurface),
                border = BorderStroke(1.dp, CasinoBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DadoItem(valor = viewModel.dado1, rotacion = viewModel.rotacionDados.value)
                        DadoItem(valor = viewModel.dado2, rotacion = -viewModel.rotacionDados.value)
                    }

                    Spacer(Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CasinoSurfaceElevated,
                        border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Suma Total: ${viewModel.dado1 + viewModel.dado2}",
                            color = CasinoGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Mensaje de resultado
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

            // Opciones de pronóstico
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
                        text = "1. Pronóstico de la suma:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    // Fila Menor 7 / Exacto 7 / Mayor 7
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BotonApuestaDado(
                            titulo = "Menor a 7",
                            subtitulo = "Paga 2x",
                            seleccionado = viewModel.tipoApuesta == "MENOR_7",
                            modifier = Modifier.weight(1f),
                            enabled = !viewModel.isLanzando,
                            onClick = {
                                tiempoUltimaInteraccion = System.currentTimeMillis()
                                viewModel.onTipoApuestaChanged("MENOR_7")
                            }
                        )

                        BotonApuestaDado(
                            titulo = "Exacto 7",
                            subtitulo = "Paga 4x",
                            seleccionado = viewModel.tipoApuesta == "EXACTO_7",
                            modifier = Modifier.weight(1f),
                            enabled = !viewModel.isLanzando,
                            onClick = {
                                tiempoUltimaInteraccion = System.currentTimeMillis()
                                viewModel.onTipoApuestaChanged("EXACTO_7")
                            }
                        )

                        BotonApuestaDado(
                            titulo = "Mayor a 7",
                            subtitulo = "Paga 2x",
                            seleccionado = viewModel.tipoApuesta == "MAYOR_7",
                            modifier = Modifier.weight(1f),
                            enabled = !viewModel.isLanzando,
                            onClick = {
                                tiempoUltimaInteraccion = System.currentTimeMillis()
                                viewModel.onTipoApuestaChanged("MAYOR_7")
                            }
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Círculo de fichas apostadas en el tapete (tocar para retirar apuesta)
                    CirculoApuesta(
                        montoApostado = viewModel.montoApuesta,
                        onClick = {
                            if (!viewModel.isLanzando) {
                                tiempoUltimaInteraccion = System.currentTimeMillis()
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
                        viewModel.lanzarDados(
                            saldoActual = authViewModel.saldo,
                            jugadorId = authViewModel.jugadorId,
                            onDescontarSaldo = authViewModel::descontarSaldo,
                            onSumarPremio = authViewModel::sumarPremio
                        )
                    }
                },
                enabled = !viewModel.isLanzando && viewModel.montoApuesta > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CasinoPrimary,
                    disabledContainerColor = CasinoSurfaceElevated
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    Icons.Default.Casino,
                    contentDescription = null,
                    tint = if (viewModel.montoApuesta > 0) CasinoPrimaryDark else TextMuted
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        viewModel.isLanzando -> "LANZANDO..."
                        viewModel.montoApuesta > 0 -> "LANZAR DADOS ($${viewModel.montoApuesta})"
                        else -> "COLOCA FICHAS EN LA MESA"
                    },
                    color = if (viewModel.montoApuesta > 0) CasinoPrimaryDark else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // --- Bandeja inferior unificada de fichas ---
        BandejaInferiorFichas(
            saldoDisponible = authViewModel.saldo,
            montoTotalApostado = viewModel.montoApuesta,
            onAgregarFicha = { valor ->
                tiempoUltimaInteraccion = System.currentTimeMillis()
                viewModel.agregarFicha(valor, authViewModel.saldo)
            },
            onLimpiar = {
                tiempoUltimaInteraccion = System.currentTimeMillis()
                viewModel.limpiarFichas()
            },
            onDoblar = {
                tiempoUltimaInteraccion = System.currentTimeMillis()
                viewModel.doblarFichas(authViewModel.saldo)
            },
            enabled = !viewModel.isLanzando
        )
    }
}

@Composable
fun BotonApuestaDado(
    titulo: String,
    subtitulo: String,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (seleccionado) CasinoSurfaceHover else CasinoSurfaceElevated,
        border = BorderStroke(
            if (seleccionado) 1.5.dp else 1.dp,
            if (seleccionado) CasinoGold else CasinoBorder
        ),
        modifier = modifier
            .height(52.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = titulo,
                color = if (seleccionado) CasinoGold else TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitulo,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Dado visual realista marfil pulido con pips obsidiana.
 */
@Composable
fun DadoItem(valor: Int, rotacion: Float) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .rotate(rotacion)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF4F4F5))
            .border(2.dp, Color(0xFFFFFFFF), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PuntoDado(visible = valor in listOf(2, 3, 4, 5, 6))
                PuntoDado(visible = false)
                PuntoDado(visible = valor in listOf(4, 5, 6))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PuntoDado(visible = valor == 6)
                PuntoDado(visible = valor in listOf(1, 3, 5))
                PuntoDado(visible = valor == 6)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PuntoDado(visible = valor in listOf(4, 5, 6))
                PuntoDado(visible = false)
                PuntoDado(visible = valor in listOf(2, 3, 4, 5, 6))
            }
        }
    }
}

@Composable
fun PuntoDado(visible: Boolean) {
    Box(
        modifier = Modifier
            .size(11.dp)
            .clip(CircleShape)
            .background(if (visible) Color(0xFF18181B) else Color.Transparent)
    )
}
