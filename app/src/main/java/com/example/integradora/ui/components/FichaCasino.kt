package com.example.integradora.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.integradora.ui.theme.*

data class FichaInfo(
    val valor: Int,
    val colorFicha: Color,
    val colorBorde: Color = Color.White
)

// Catálogo completo de 13 fichas con denominaciones de casino profesionales:
// 1, 5, 25, 100, 500, 1K, 2K, 5K, 10K, 25K, 50K, 100K, 1M
val TODAS_LAS_FICHAS = listOf(
    FichaInfo(1,       Color(0xFF64748B), Color(0xFF94A3B8)), // 1 Gris Platino
    FichaInfo(5,       Color(0xFF2563EB), Color(0xFF93C5FD)), // 5 Azul Real
    FichaInfo(25,      Color(0xFF16A34A), Color(0xFF86EFAC)), // 25 Verde Esmeralda
    FichaInfo(100,     Color(0xFFEAB308), Color(0xFFFDE047)), // 100 Amarillo Dorado
    FichaInfo(500,     Color(0xFFEA580C), Color(0xFFFDBA74)), // 500 Naranja
    FichaInfo(1000,    Color(0xFFDC2626), Color(0xFFFCA5A5)), // 1K Rojo Rubí
    FichaInfo(2000,    Color(0xFF9333EA), Color(0xFFD8B4FE)), // 2K Amatista
    FichaInfo(5000,    Color(0xFF0D9488), Color(0xFF5EEAD4)), // 5K Turquesa
    FichaInfo(10000,   Color(0xFF334155), Color(0xFFCBD5E1)), // 10K Pizarra
    FichaInfo(25000,   Color(0xFF065F46), Color(0xFF6EE7B7)), // 25K Esmeralda Oscura
    FichaInfo(50000,   Color(0xFF1E3A8A), Color(0xFF93C5FD)), // 50K Zafiro Profundo
    FichaInfo(100000,  Color(0xFF78350F), Color(0xFFFDE68A)), // 100K Bronce Imperial
    FichaInfo(1000000, Color(0xFF991B1B), Color(0xFFFCA5A5))  // 1M Rubí de Alto Rodillo
)

fun formatFichaValor(valor: Int): String = when {
    valor >= 1_000_000 -> "${valor / 1_000_000}M"
    valor >= 1_000     -> "${valor / 1_000}K"
    else               -> "$valor"
}

/**
 * Ficha de casino profesional con 8 muescas perimetrales, ranura interior y centro grabado.
 */
@Composable
fun FichaCasino(
    valor: Int,
    colorFicha: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 56.dp
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 1.15f else 1.0f,
        label = "chipPressScale"
    )

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }

    val alpha = if (enabled) 1.0f else 0.32f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .size(size)
            .clickable(enabled = enabled) {
                pressed = true
                onClick()
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val d = size.toPx()
            val radius = d / 2f
            val center = Offset(radius, radius)

            // 1. Círculo base con color primario de la ficha
            drawCircle(
                color = colorFicha.copy(alpha = alpha),
                radius = radius,
                center = center
            )

            // 2. 8 muescas blancas uniformes en el perímetro exterior (patrón auténtico)
            val stripeSweep = 15f
            for (i in 0 until 8) {
                val startAngle = i * 45f - (stripeSweep / 2f)
                drawArc(
                    color = Color.White.copy(alpha = 0.94f * alpha),
                    startAngle = startAngle,
                    sweepAngle = stripeSweep,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = Size(d, d)
                )
            }

            // 3. Máscara circular para restringir las muescas al borde
            drawCircle(
                color = colorFicha.copy(alpha = alpha),
                radius = radius * 0.82f,
                center = center
            )

            // 4. Anillo ranurado interior
            drawCircle(
                color = Color.White.copy(alpha = 0.75f * alpha),
                radius = radius * 0.79f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 5. Inlay central
            drawCircle(
                color = colorFicha.copy(alpha = 0.88f * alpha),
                radius = radius * 0.60f,
                center = center
            )

            // 6. Borde del inlay
            drawCircle(
                color = Color.White.copy(alpha = 0.45f * alpha),
                radius = radius * 0.60f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 7. Sombra exterior
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f * alpha),
                radius = radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Valor de la ficha centrado
        Text(
            text = formatFichaValor(valor),
            color = Color.White.copy(alpha = alpha),
            fontSize = if (valor >= 1_000_000) 10.sp else if (valor >= 1000) 11.sp else 13.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Ficha miniatura para apilado dentro del círculo de apuestas.
 */
@Composable
fun FichaMini(
    valor: Int,
    size: Dp = 44.dp
) {
    val info = TODAS_LAS_FICHAS.find { it.valor == valor } ?: TODAS_LAS_FICHAS.firstOrNull { it.valor <= valor } ?: TODAS_LAS_FICHAS[1]
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val d = size.toPx()
            val radius = d / 2f
            val center = Offset(radius, radius)

            drawCircle(color = info.colorFicha, radius = radius, center = center)

            val stripeSweep = 15f
            for (i in 0 until 8) {
                val startAngle = i * 45f - (stripeSweep / 2f)
                drawArc(
                    color = Color.White,
                    startAngle = startAngle,
                    sweepAngle = stripeSweep,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = Size(d, d)
                )
            }

            drawCircle(color = info.colorFicha, radius = radius * 0.82f, center = center)
            drawCircle(color = Color.White.copy(alpha = 0.7f), radius = radius * 0.78f, center = center, style = Stroke(1.dp.toPx()))
            drawCircle(color = info.colorFicha.copy(alpha = 0.9f), radius = radius * 0.60f, center = center)
            drawCircle(color = Color.White.copy(alpha = 0.5f), radius = radius * 0.60f, center = center, style = Stroke(0.8.dp.toPx()))
        }

        Text(
            text = formatFichaValor(valor),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Círculo de apuestas en la mesa con fichas apiladas en perspectiva 3D.
 * Al tocar el círculo/fichas se retira la apuesta.
 */
@Composable
fun CirculoApuesta(
    montoApostado: Int,
    modifier: Modifier = Modifier,
    etiqueta: String = "APUESTA",
    onClick: () -> Unit = {}
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(86.dp)
            .clip(CircleShape)
            .background(Color(0xFF0F172A).copy(alpha = 0.60f))
            .border(
                width = 2.dp,
                color = if (montoApostado > 0) CasinoGold.copy(alpha = 0.85f) else CasinoBorder.copy(alpha = 0.55f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    ) {
        if (montoApostado > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.offset(x = (-4).dp, y = (-2).dp)) {
                    FichaMini(valor = montoApostado, size = 42.dp)
                }
                Box(modifier = Modifier.offset(x = 4.dp, y = 2.dp)) {
                    FichaMini(valor = montoApostado, size = 42.dp)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$ %,d".format(montoApostado),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = etiqueta,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "$0",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Bandeja inferior estandarizada de fichas:
 * - Se ancla en la parte inferior de todos los juegos (Blackjack, Ruleta, Dados).
 * - Desbloqueo progresivo por capital del jugador (las fichas mayores aparecen conforme crece la banca).
 * - Desplazamiento horizontal fluido (smooth scroll con inercia nativa).
 * - Las fichas en la mesa se retiran directamente tocándolas en el tapete.
 */
@Composable
fun BandejaInferiorFichas(
    saldoDisponible: Double,
    montoTotalApostado: Int,
    onAgregarFicha: (Int) -> Unit,
    onLimpiar: () -> Unit = {},
    onDoblar: () -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Desbloqueo progresivo según la banca actual del usuario
    val fichasVisibles = remember(saldoDisponible) {
        val accesibles = TODAS_LAS_FICHAS.filter { it.valor <= saldoDisponible }
        if (accesibles.isEmpty()) {
            TODAS_LAS_FICHAS.take(3)
        } else {
            val siguiente = TODAS_LAS_FICHAS.firstOrNull { it.valor > saldoDisponible }
            if (siguiente != null) accesibles + siguiente else accesibles
        }
    }

    Surface(
        color = CasinoSurface,
        border = BorderStroke(1.dp, CasinoBorder),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Fila de información de mesa limpia (sin botones 2X ni Limpiar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (montoTotalApostado > 0) "Mesa: $ %,d MXN".format(montoTotalApostado) else "Selecciona fichas para apostar",
                    color = if (montoTotalApostado > 0) TextPrimary else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                if (montoTotalApostado > 0) {
                    Text(
                        text = "Toca la ficha en el tapete para retirar",
                        color = CasinoGoldLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Riel de fichas con scroll táctil suave y continuo
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(fichasVisibles, key = { it.valor }) { ficha ->
                    val puedePoner = enabled && (montoTotalApostado + ficha.valor) <= saldoDisponible
                    FichaCasino(
                        valor = ficha.valor,
                        colorFicha = ficha.colorFicha,
                        enabled = puedePoner,
                        onClick = { onAgregarFicha(ficha.valor) },
                        size = 56.dp
                    )
                }
            }
        }
    }
}
