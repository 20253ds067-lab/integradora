package com.example.integradora.ui.screens.amigos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.integradora.ui.theme.*

@Composable
fun AmigosScreen(
    viewModel: AmigosViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dialogo agregar o editar
    if (viewModel.showDialog) {
        DialogAmigo(viewModel = viewModel)
    }

    // Dialogo confirmar borrar
    if (viewModel.showDialogBorrar) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarBorrar,
            containerColor = CasinoSurface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Eliminar amigo", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Se eliminará a \"${viewModel.amigoBorrar?.nombreReal}\" de tu lista de amigos.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmarBorrar,
                    colors = ButtonDefaults.buttonColors(containerColor = CasinoRed),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarBorrar) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CasinoBg)
    ) {
        // Barra superior limpia (sin el círculo de la derecha)
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

            Text(
                text = "Amigos y Solicitudes",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Espaciador para centrar el título
            Spacer(Modifier.width(64.dp))
        }

        // Pestañas (Tabs) estilo Memorias: Mis Amigos vs Solicitudes
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
                    Text(
                        text = "Mis Amigos (${viewModel.amigos.size})",
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.tabSeleccionada == 0) CasinoGold else TextSecondary
                    )
                }
            )
            Tab(
                selected = viewModel.tabSeleccionada == 1,
                onClick = { viewModel.onTabSelected(1) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Solicitudes",
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.tabSeleccionada == 1) CasinoGold else TextSecondary
                        )
                        if (viewModel.solicitudesPendientes.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Badge(containerColor = CasinoRed) {
                                Text("${viewModel.solicitudesPendientes.size}", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
            )
        }

        HorizontalDivider(color = CasinoBorder)

        if (viewModel.tabSeleccionada == 0) {
            // ─── TAB 0: MIS AMIGOS ───
            Column(modifier = Modifier.fillMaxSize()) {
                // Barra de acción para agregar amigo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${viewModel.amigos.size} amigo${if (viewModel.amigos.size != 1) "s" else ""} conectado${if (viewModel.amigos.size != 1) "s" else ""}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Button(
                        onClick = viewModel::abrirAgregar,
                        colors = ButtonDefaults.buttonColors(containerColor = CasinoPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = CasinoPrimaryDark, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Agregar Amigo", color = CasinoPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (viewModel.amigos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Todavía no tienes amigos",
                                color = TextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Manda una solicitud de amistad con el botón Agregar Amigo",
                                color = TextMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(viewModel.amigos, key = { it.id }) { amigo ->
                            TarjetaAmigo(
                                amigo = amigo,
                                onEditar = { viewModel.abrirEditar(amigo) },
                                onBorrar = { viewModel.pedirBorrar(amigo) }
                            )
                        }
                    }
                }
            }
        } else {
            // ─── TAB 1: SOLICITUDES DE AMISTAD (ACEPTAR / RECHAZAR) ───
            if (viewModel.solicitudesPendientes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Sin solicitudes pendientes",
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Cuando otros jugadores te manden solicitud aparecerán aquí",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.solicitudesPendientes, key = { it.id }) { solicitud ->
                        TarjetaSolicitud(
                            solicitud = solicitud,
                            onAceptar = { viewModel.aceptarSolicitud(solicitud) },
                            onRechazar = { viewModel.rechazarSolicitud(solicitud) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaAmigo(
    amigo: Amigo,
    onEditar: () -> Unit,
    onBorrar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CasinoSurface),
        border = BorderStroke(1.dp, CasinoBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar inicial
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(CasinoSurfaceElevated, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = amigo.nombreReal.take(1).uppercase(),
                    color = CasinoGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.width(12.dp))

            // Nombre y apodo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = amigo.nombreReal,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (amigo.apodo.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Apodo: ",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = amigo.apodo,
                            color = CasinoGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text("Sin apodo asignado", color = TextMuted, fontSize = 11.sp)
                }
            }

            // Acciones
            Row {
                IconButton(
                    onClick = onEditar,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = CasinoGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onBorrar,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Borrar",
                        tint = CasinoRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TarjetaSolicitud(
    solicitud: Amigo,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CasinoSurface),
        border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(CasinoSurfaceElevated, CircleShape)
                    .border(1.dp, CasinoGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = solicitud.nombreReal.take(1).uppercase(),
                    color = CasinoGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = solicitud.nombreReal,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Te envió solicitud de amistad",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAceptar,
                    colors = ButtonDefaults.buttonColors(containerColor = CasinoPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Aceptar", color = CasinoPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onRechazar,
                    border = BorderStroke(1.dp, CasinoRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Rechazar", color = CasinoRed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DialogAmigo(viewModel: AmigosViewModel) {
    val esEdicion = viewModel.amigoEditando != null

    AlertDialog(
        onDismissRequest = viewModel::cerrarDialog,
        containerColor = CasinoSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (esEdicion) "Editar Apodo" else "Mandar Solicitud de Amistad",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Campo nombre/usuario único al agregar
                OutlinedTextField(
                    value = viewModel.inputNombre,
                    onValueChange = viewModel::onNombreChanged,
                    label = { Text(if (esEdicion) "Nombre / Usuario" else "Nombre o Usuario") },
                    placeholder = { Text("Ej: carlos_99", color = TextMuted) },
                    singleLine = true,
                    isError = viewModel.errorNombre != null,
                    supportingText = {
                        viewModel.errorNombre?.let {
                            Text(it, color = CasinoRed, fontSize = 11.sp)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CasinoSurfaceElevated,
                        unfocusedContainerColor = CasinoSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CasinoPrimary,
                        unfocusedBorderColor = CasinoBorder,
                        focusedLabelColor = TextSecondary,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Sugerencias de jugadores registrados (Base de Datos SQLite + Spring Boot)
                if (!esEdicion && viewModel.jugadoresSugeridos.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Jugadores registrados para agregar:",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        viewModel.jugadoresSugeridos.take(8).forEach { nombre ->
                            SuggestionChip(
                                onClick = { viewModel.onNombreChanged(nombre) },
                                label = { Text(nombre, fontSize = 11.sp, color = TextPrimary) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = CasinoSurfaceElevated
                                ),
                                border = BorderStroke(1.dp, CasinoBorder)
                            )
                        }
                    }
                }

                // El apodo SOLO aparece en modo Edición posterior
                if (esEdicion) {
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = viewModel.inputApodo,
                        onValueChange = viewModel::onApodoChanged,
                        label = { Text("Apodo personalizado") },
                        placeholder = { Text("Ej: El Tiburón", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CasinoSurfaceElevated,
                            unfocusedContainerColor = CasinoSurfaceElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CasinoPrimary,
                            unfocusedBorderColor = CasinoBorder,
                            focusedLabelColor = TextSecondary,
                            unfocusedLabelColor = TextMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = CasinoGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Este apodo es privado y solo tú puedes verlo.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::confirmar,
                colors = ButtonDefaults.buttonColors(containerColor = CasinoPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (esEdicion) "Guardar Cambios" else "Mandar Solicitud",
                    color = CasinoPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::cerrarDialog) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}
