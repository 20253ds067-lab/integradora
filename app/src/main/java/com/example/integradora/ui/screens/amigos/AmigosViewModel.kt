package com.example.integradora.ui.screens.amigos

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.integradora.data.local.CasinoRepository
import com.example.integradora.data.remote.RetrofitClient
import com.example.integradora.data.remote.dto.JugadorResponse
import kotlinx.coroutines.launch

data class Amigo(
    val id: Long = System.nanoTime(),
    val nombreReal: String,  // nombre que el amigo usa (referencia)
    val apodo: String = ""   // apodo secreto que solo tú ves
)

class AmigosViewModel(application: Application? = null) : ViewModel() {

    private var repository: CasinoRepository? = application?.let { CasinoRepository.getInstance(it) }
    private var usuarioActual: String = "Estudiante"

    var amigos by mutableStateOf<List<Amigo>>(emptyList())
        private set

    var solicitudesPendientes by mutableStateOf<List<Amigo>>(emptyList())
        private set

    // Tab seleccionada: 0 = Mis Amigos, 1 = Solicitudes
    var tabSeleccionada by androidx.compose.runtime.mutableIntStateOf(0)
        private set

    fun onTabSelected(index: Int) {
        tabSeleccionada = index
    }

    // Jugadores traídos desde Spring Boot y SQLite para sugerencias
    var jugadoresSugeridos by mutableStateOf<List<String>>(emptyList())
        private set

    // --- Dialogo agregar/editar ---
    var showDialog by mutableStateOf(false)
        private set

    var amigoEditando by mutableStateOf<Amigo?>(null)
        private set

    var inputNombre by mutableStateOf("")
        private set

    var inputApodo by mutableStateOf("")
        private set

    var errorNombre by mutableStateOf<String?>(null)
        private set

    // --- Dialogo confirmar borrar ---
    var showDialogBorrar by mutableStateOf(false)
        private set

    var amigoBorrar by mutableStateOf<Amigo?>(null)
        private set

    fun setContext(context: Context, usuario: String) {
        if (repository == null) {
            repository = CasinoRepository.getInstance(context)
        }
        usuarioActual = usuario
        cargarDatosPersistentes()
    }

    fun cargarDatosPersistentes() {
        viewModelScope.launch {
            try {
                repository?.asegurarDatosIniciales(usuarioActual)

                // 1. Cargar amigos aceptados guardados en SQLite
                val amigosDb = repository?.obtenerAmigos(usuarioActual) ?: emptyList()
                amigos = amigosDb.map {
                    Amigo(id = it.id, nombreReal = it.nombreAmigo, apodo = it.apodo)
                }

                // 2. Cargar solicitudes pendientes recibidas
                val solDb = repository?.obtenerSolicitudesPendientes(usuarioActual) ?: emptyList()
                solicitudesPendientes = solDb.map {
                    Amigo(id = it.id, nombreReal = it.nombreAmigo, apodo = it.apodo)
                }

                // 2. Cargar jugadores registrados localmente en SQLite
                val usuariosLocales = repository?.obtenerTodosUsuarios()?.map { it.usuario } ?: emptyList()

                // 3. Cargar jugadores registrados en Spring Boot
                var usuariosRemotos: List<String> = emptyList()
                try {
                    val resp = RetrofitClient.apiService.obtenerTodosJugadores()
                    if (resp.isSuccessful && resp.body() != null) {
                        usuariosRemotos = resp.body()!!.map { it.nombre }
                    }
                } catch (_: Exception) {}

                // Combinar sin duplicados ni el usuario actual
                val combinados = (usuariosLocales + usuariosRemotos)
                    .filter { it.isNotBlank() && !it.equals(usuarioActual, ignoreCase = true) }
                    .distinct()

                jugadoresSugeridos = combinados
            } catch (_: Exception) {}
        }
    }

    fun abrirAgregar() {
        amigoEditando = null
        inputNombre = ""
        inputApodo = ""
        errorNombre = null
        showDialog = true
        cargarDatosPersistentes()
    }

    fun abrirEditar(amigo: Amigo) {
        amigoEditando = amigo
        inputNombre = amigo.nombreReal
        inputApodo = amigo.apodo
        errorNombre = null
        showDialog = true
    }

    fun cerrarDialog() {
        showDialog = false
        amigoEditando = null
        errorNombre = null
    }

    fun onNombreChanged(v: String) { inputNombre = v; errorNombre = null }
    fun onApodoChanged(v: String) { inputApodo = v }

    fun confirmar() {
        val nombre = inputNombre.trim()
        if (nombre.isBlank()) {
            errorNombre = "El nombre de usuario no puede estar vacío"
            return
        }

        // 1. No puedes agregarte a ti mismo como amigo
        if (nombre.equals(usuarioActual, ignoreCase = true)) {
            errorNombre = "No te puedes agregar a ti mismo como amigo"
            return
        }

        val editando = amigoEditando

        // 2. No agregar al mismo amigo dos veces
        if (amigos.any { it.nombreReal.equals(nombre, ignoreCase = true) && it.id != editando?.id }) {
            errorNombre = "Este usuario ya está en tu lista de amigos"
            return
        }

        val apodo = inputApodo.trim()

        viewModelScope.launch {
            try {
                // 3. Validar existencia estricta de la cuenta
                val repo = repository
                val existe = repo?.existeUsuario(nombre) ?: false
                if (!existe) {
                    errorNombre = "Amigo no encontrado"
                    return@launch
                }

                if (editando != null) {
                    // Editar apodo de amigo existente
                    repo.actualizarAmigo(editando.id, nombre, apodo)
                } else {
                    // Enviar solicitud de amistad formal
                    repo.enviarSolicitudAmistad(usuarioActual, nombre, apodo)
                }
                cargarDatosPersistentes()
                cerrarDialog()
            } catch (e: Exception) {
                errorNombre = "Error al procesar solicitud: ${e.localizedMessage}"
            }
        }
    }

    fun aceptarSolicitud(amigo: Amigo) {
        viewModelScope.launch {
            try {
                repository?.aceptarSolicitudAmistad(usuarioActual, amigo.nombreReal)
                cargarDatosPersistentes()
            } catch (_: Exception) {}
        }
    }

    fun rechazarSolicitud(amigo: Amigo) {
        viewModelScope.launch {
            try {
                repository?.rechazarSolicitudAmistad(usuarioActual, amigo.nombreReal)
                cargarDatosPersistentes()
            } catch (_: Exception) {}
        }
    }

    fun pedirBorrar(amigo: Amigo) {
        amigoBorrar = amigo
        showDialogBorrar = true
    }

    fun confirmarBorrar() {
        val borrar = amigoBorrar
        if (borrar != null) {
            viewModelScope.launch {
                try {
                    repository?.eliminarAmigo(borrar.id)
                    cargarDatosPersistentes()
                } catch (_: Exception) {
                    amigos = amigos.filter { it.id != borrar.id }
                } finally {
                    amigoBorrar = null
                    showDialogBorrar = false
                }
            }
        } else {
            showDialogBorrar = false
        }
    }

    fun cancelarBorrar() {
        amigoBorrar = null
        showDialogBorrar = false
    }
}
