package com.example.integradora.ui.screens.ranking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.integradora.data.local.RankingUser
import com.example.integradora.ui.screens.login.AuthViewModel
import com.example.integradora.ui.theme.*

@Composable
fun RankingScreen(
    viewModel: RankingViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(authViewModel.usuarioActual) {
        viewModel.setContext(context, authViewModel.usuarioActual)
    }

    val listaActual = if (viewModel.tabSeleccionada == 0) viewModel.rankingGeneral else viewModel.rankingAmigos

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CasinoBg)
    ) {
        // ─── BARRA SUPERIOR ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                Spacer(Modifier.width(4.dp))
                Text("Menú", color = TextPrimary, fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = CasinoGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Ranking de Banca",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(60.dp))
        }

        // ─── PESTAÑAS (TABS): GENERAL VS AMIGOS ───
        TabRow(
            selectedTabIndex = viewModel.tabSeleccionada,
            containerColor = CasinoSurface,
            contentColor = CasinoGold,
            divider = { HorizontalDivider(color = CasinoBorder) }
        ) {
            Tab(
                selected = viewModel.tabSeleccionada == 0,
                onClick = { viewModel.onTabSelected(0) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "General",
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.tabSeleccionada == 0) CasinoGold else TextSecondary
                        )
                    }
                }
            )
            Tab(
                selected = viewModel.tabSeleccionada == 1,
                onClick = { viewModel.onTabSelected(1) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Amigos (${viewModel.rankingAmigos.size})",
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.tabSeleccionada == 1) CasinoGold else TextSecondary
                        )
                    }
                }
            )
        }

        HorizontalDivider(color = CasinoBorder)

        if (viewModel.isCargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CasinoGold, strokeWidth = 2.dp)
            }
        } else if (listaActual.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TextMuted, modifier = Modifier.size(60.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (viewModel.tabSeleccionada == 0) "No hay jugadores registrados" else "Aún no tienes amigos en el ranking",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (viewModel.tabSeleccionada == 0) "Los jugadores aparecerán conforme jueguen" else "Agrega amigos en la sección Mis Amigos para competir contra ellos",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Podio Top 3 si hay al menos 2 jugadores
                if (listaActual.size >= 2) {
                    item {
                        PodioTop3(jugadores = listaActual.take(3))
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = CasinoBorder)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                item {
                    Text(
                        text = if (viewModel.tabSeleccionada == 0) "Clasificación General por Saldo" else "Clasificación entre Amigos",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                items(listaActual, key = { it.usuario }) { jugador ->
                    TarjetaRankingJugador(jugador = jugador)
                }
            }
        }
    }
}

/**
 * Podio estilo torneo de casino para los 3 mejores jugadores.
 */
@Composable
fun PodioTop3(jugadores: List<RankingUser>) {
    val top1 = jugadores.getOrNull(0)
    val top2 = jugadores.getOrNull(1)
    val top3 = jugadores.getOrNull(2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // #2 Plata
        if (top2 != null) {
            ColumnaPodio(
                posicion = 2,
                jugador = top2,
                colorBorde = Color(0xFFCBD5E1),
                altura = 100.dp,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        // #1 Oro (Más alto al centro)
        if (top1 != null) {
            ColumnaPodio(
                posicion = 1,
                jugador = top1,
                colorBorde = CasinoGold,
                altura = 130.dp,
                modifier = Modifier.weight(1.15f)
            )
        }

        // #3 Bronce
        if (top3 != null) {
            ColumnaPodio(
                posicion = 3,
                jugador = top3,
                colorBorde = Color(0xFFD97706),
                altura = 85.dp,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun ColumnaPodio(
    posicion: Int,
    jugador: RankingUser,
    colorBorde: Color,
    altura: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Insignia de medalla
        Box(
            modifier = Modifier
                .size(if (posicion == 1) 48.dp else 40.dp)
                .clip(CircleShape)
                .background(CasinoSurfaceElevated)
                .border(2.dp, colorBorde, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (posicion) {
                    1 -> "🥇"
                    2 -> "🥈"
                    else -> "🥉"
                },
                fontSize = if (posicion == 1) 22.sp else 18.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = jugador.usuario,
            color = if (jugador.esUsuarioActual) CasinoGold else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Text(
            text = "$ %,.0f".format(jugador.saldo),
            color = colorBorde,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(6.dp))

        // Bloque del podio
        Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            color = CasinoSurfaceElevated,
            border = BorderStroke(1.dp, colorBorde.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(altura)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#$posicion",
                    color = colorBorde,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * Fila individual del ranking de jugadores.
 */
@Composable
fun TarjetaRankingJugador(jugador: RankingUser) {
    val esTop3 = jugador.posicion in 1..3
    val colorBorde = when (jugador.posicion) {
        1 -> CasinoGold
        2 -> Color(0xFFCBD5E1)
        3 -> Color(0xFFD97706)
        else -> if (jugador.esUsuarioActual) CasinoGold.copy(alpha = 0.7f) else CasinoBorder
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (jugador.esUsuarioActual) Color(0xFF191F28) else CasinoSurface
        ),
        border = BorderStroke(
            if (jugador.esUsuarioActual || esTop3) 1.5.dp else 1.dp,
            colorBorde
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Posición
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (esTop3) colorBorde.copy(alpha = 0.2f) else CasinoSurfaceElevated,
                border = BorderStroke(1.dp, if (esTop3) colorBorde else CasinoBorder),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#${jugador.posicion}",
                        color = if (esTop3) colorBorde else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Nombre y badge si es tú
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = jugador.usuario,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (jugador.esUsuarioActual) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CasinoGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CasinoGold)
                        ) {
                            Text(
                                text = "TÚ",
                                color = CasinoGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = jugador.nombre,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            // Saldo en cuenta
            Text(
                text = "$ %,.0f MXN".format(jugador.saldo),
                color = if (jugador.esUsuarioActual) CasinoGoldLight else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
