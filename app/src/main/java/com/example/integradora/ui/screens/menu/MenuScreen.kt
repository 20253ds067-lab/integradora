package com.example.integradora.ui.screens.menu

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.integradora.ui.screens.login.AuthViewModel
import com.example.integradora.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MenuScreen(
    authViewModel: AuthViewModel,
    onNavigateToRuleta: () -> Unit,
    onNavigateToDados: () -> Unit,
    onNavigateToBlackjack: () -> Unit,
    onNavigateToAmigos: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ─── DRAWER LATERAL: 56% DE ANCHO (POQUITO MÁS DE LA MITAD) CON OPCIONES OSCURAS ───
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = Color.Black.copy(alpha = 0.70f),
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                drawerContainerColor = Color(0xFF0B0D10),
                drawerTonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.56f) // Más de la mitad de la pantalla
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = authViewModel.usuarioActual.ifBlank { "Jugador" },
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$ %,.0f MXN".format(authViewModel.saldo),
                        color = CasinoGoldLight,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = CasinoBorder)
                    Spacer(Modifier.height(14.dp))

                    // Opción: Ranking de Banca (con fondo oscuro destacado)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF14171E),
                        border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                onNavigateToRanking()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = CasinoGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Ranking de Banca",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Opción: Mis Amigos (con fondo oscuro destacado)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF14171E),
                        border = BorderStroke(1.dp, CasinoBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                onNavigateToAmigos()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = CasinoGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Mis Amigos",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Indicador de conexión con API REST Spring Boot
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (authViewModel.serverConectado == true) Color(0xFF0F3B21) else Color(0xFF14171E),
                        border = BorderStroke(1.dp, if (authViewModel.serverConectado == true) Color(0xFF16A34A) else CasinoBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (authViewModel.serverConectado == true) Color(0xFF22C55E) else Color(0xFFF59E0B))
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (authViewModel.serverConectado == true) "Spring Boot OK" else "Servidor Spring Boot",
                                    color = if (authViewModel.serverConectado == true) Color(0xFF86EFAC) else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = if (authViewModel.serverConectado == true) "REST API :8080" else "10.0.2.2:8080 / Local",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    HorizontalDivider(color = CasinoBorder)
                    Spacer(Modifier.height(14.dp))

                    // Opción: Cerrar Sesión (con fondo oscuro destacado)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1C1315),
                        border = BorderStroke(1.dp, Color(0xFF451A1D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                onLogout()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                tint = CasinoRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Cerrar Sesión",
                                color = CasinoRed,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    ) {
        // ─── PANTALLA PRINCIPAL ───
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(CasinoBg)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ─── BARRA SUPERIOR PERFECTAMENTE ALINEADA (SIN CÍRCULO) ───
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CasinoSurface,
                border = BorderStroke(1.dp, CasinoBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lado izquierdo: Saludo limpio sin círculo
                    Column {
                        Text(
                            text = "Bienvenido,",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = authViewModel.usuarioActual.ifBlank { "Jugador" },
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Lado derecho: Cápsula de Saldo + Botón Menú Drawer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CasinoSurfaceElevated,
                            border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "$ %,.0f".format(authViewModel.saldo),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .size(36.dp)
                                .background(CasinoSurfaceElevated, CircleShape)
                                .border(1.dp, CasinoBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menú lateral",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Juegos de Casino",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = "Selecciona el juego que deseas jugar",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )
            }

            // TARJETAS DE JUEGOS (SIN ÍCONOS NI DESCRIPCIONES, ESTILO PROFESIONAL)

            // 1. Ruleta Clásica
            GameMenuCard(
                title = "Juego de Ruleta",
                buttonText = "Entrar a la Ruleta",
                onClick = onNavigateToRuleta
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Juego de Dados
            GameMenuCard(
                title = "Juego de Dados",
                buttonText = "Entrar a los Dados",
                onClick = onNavigateToDados
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Blackjack 21
            GameMenuCard(
                title = "Blackjack 21",
                buttonText = "Entrar al Blackjack",
                onClick = onNavigateToBlackjack
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Componente unificado y limpio para las tarjetas de juegos de la pantalla principal.
 */
@Composable
fun GameMenuCard(
    title: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CasinoSurface),
        border = BorderStroke(1.dp, CasinoBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CasinoPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = buttonText,
                    color = CasinoPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
