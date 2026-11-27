package com.example.integradora.ui.screens.login

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.integradora.data.local.CasinoRepository
import com.example.integradora.data.local.UsuarioLocal
import com.example.integradora.data.remote.RetrofitClient
import com.example.integradora.data.remote.dto.CrearJugadorRequest
import kotlinx.coroutines.launch

class AuthViewModel(application: Application? = null) : ViewModel() {

    private var repository: CasinoRepository? = application?.let { CasinoRepository.getInstance(it) }

    // Inputs de Login
    var usuarioInput by mutableStateOf("")
        private set

    var passwordInput by mutableStateOf("")
        private set

    // Inputs de Registro
    var nombreInput by mutableStateOf("")
        private set

    var correoInput by mutableStateOf("")
        private set

    var confirmPasswordInput by mutableStateOf("")
        private set

    // Mensaje de validación / error
    var errorMensaje by mutableStateOf<String?>(null)
        private set

    // Estado del usuario y sesión
    var usuarioActual by mutableStateOf("")
        private set

    var sesionActiva by mutableStateOf(false)
        private set

    // ID del jugador sincronizado con el backend de Spring Boot
    var jugadorId by mutableStateOf<Long?>(null)
        private set

    // Estado de conexión con la API REST de Spring Boot
    var serverConectado by mutableStateOf<Boolean?>(null)
        private set

    var isCargando by mutableStateOf(false)
        private set

    // Saldo inicial de habilidad: el saldo NUNCA baja de 1.0 (protección de bancarrota)
    var saldo by mutableDoubleStateOf(1000.0)
        private set

    // Lista de usuarios registrados en la base de datos SQLite local
    var usuariosRegistradosDb by mutableStateOf<List<UsuarioLocal>>(emptyList())
        private set

    init {
        verificarConexionServidor()
        if (repository != null) {
            cargarSesionPersistente()
            cargarUsuariosRegistrados()
        }
    }

    fun setContext(context: Context) {
        if (repository == null) {
            repository = CasinoRepository.getInstance(context)
            cargarSesionPersistente()
            cargarUsuariosRegistrados()
        }
    }

    private fun cargarSesionPersistente() {
        viewModelScope.launch {
            try {
                repository?.asegurarDatosIniciales(usuarioActual)
                val sesion = repository?.obtenerSesionActiva()
                if (sesion != null && sesion.usuario.isNotBlank()) {
                    usuarioActual = sesion.usuario
                    saldo = if (sesion.saldo < 1.0) 1.0 else sesion.saldo
                    sesionActiva = true
                    repository?.asegurarDatosIniciales(usuarioActual)
                } else {
                    sesionActiva = false
                }
            } catch (_: Exception) {
                sesionActiva = false
            }
        }
    }

    fun cargarUsuariosRegistrados() {
        viewModelScope.launch {
            try {
                usuariosRegistradosDb = repository?.obtenerTodosUsuarios() ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun verificarConexionServidor() {
        viewModelScope.launch {
            serverConectado = RetrofitClient.checkServerConnection()
        }
    }

    // Eventos de cambio en inputs
    fun onUsuarioChanged(nuevoValor: String) {
        usuarioInput = nuevoValor
        errorMensaje = null
    }

    fun onPasswordChanged(nuevoValor: String) {
        passwordInput = nuevoValor
        errorMensaje = null
    }

    fun onNombreChanged(nuevoValor: String) {
        nombreInput = nuevoValor
        errorMensaje = null
    }

    fun onCorreoChanged(nuevoValor: String) {
        correoInput = nuevoValor
        errorMensaje = null
    }

    fun onConfirmPasswordChanged(nuevoValor: String) {
        confirmPasswordInput = nuevoValor
        errorMensaje = null
    }

    // Lógica de inicio de sesión estricta en SQLite + Spring Boot
    fun iniciarSesion(onSuccess: () -> Unit) {
        if (usuarioInput.isBlank() || passwordInput.isBlank()) {
            errorMensaje = "Por favor ingresa tu usuario y contraseña"
            return
        }

        val username = usuarioInput.trim()
        val pass = passwordInput.trim()
        isCargando = true
        errorMensaje = null

        viewModelScope.launch {
            try {
                // 1. Validar credenciales estrictas en la Base de Datos SQLite Local
                val repo = repository
                if (repo == null) {
                    errorMensaje = "Base de datos no disponible temporalmente"
                    isCargando = false
                    return@launch
                }

                val resultadoAuth = repo.validarCredenciales(usuario = username, password = pass)
                if (resultadoAuth.isFailure) {
                    errorMensaje = resultadoAuth.exceptionOrNull()?.message ?: "Cuenta no encontrada"
                    isCargando = false
                    return@launch
                }

                val usuarioLocal = resultadoAuth.getOrThrow()
                usuarioActual = usuarioLocal.usuario
                saldo = if (usuarioLocal.saldo < 1.0) 1000.0 else usuarioLocal.saldo

                // 2. Sincronizar con la API REST de Spring Boot
                try {
                    val response = RetrofitClient.apiService.obtenerTodosJugadores()
                    if (response.isSuccessful && response.body() != null) {
                        serverConectado = true
                        val lista = response.body()!!
                        val existente = lista.firstOrNull { it.nombre.equals(username, ignoreCase = true) }

                        if (existente != null && existente.id != null) {
                            jugadorId = existente.id
                            saldo = if (existente.saldo < 1.0) 1000.0 else existente.saldo
                            repo.actualizarSaldo(username, saldo)
                        } else {
                            val crearResp = RetrofitClient.apiService.crearJugador(
                                CrearJugadorRequest(nombre = username, saldo = saldo)
                            )
                            if (crearResp.isSuccessful && crearResp.body()?.id != null) {
                                jugadorId = crearResp.body()!!.id
                            }
                        }
                    } else {
                        serverConectado = false
                    }
                } catch (_: Exception) {
                    serverConectado = false
                }

                cargarUsuariosRegistrados()
                sesionActiva = true
                isCargando = false
                usuarioInput = ""
                passwordInput = ""
                errorMensaje = null
                onSuccess()
            } catch (e: Exception) {
                isCargando = false
                errorMensaje = "Error al iniciar sesión: ${e.localizedMessage}"
            }
        }
    }

    // Lógica de registro estricta en SQLite + Spring Boot
    fun registrarse(onSuccess: () -> Unit) {
        val correo = correoInput.trim()

        when {
            nombreInput.isBlank() || usuarioInput.isBlank() || correo.isBlank() || passwordInput.isBlank() -> {
                errorMensaje = "Completa todos los campos obligatorios"
                return
            }
            !correo.contains("@") || !correo.contains(".") || correo.indexOf('@') >= correo.lastIndexOf('.') || correo.length < 5 -> {
                errorMensaje = "Ingresa un correo electrónico válido (debe contener @ y .)"
                return
            }
            passwordInput != confirmPasswordInput -> {
                errorMensaje = "Las contraseñas no coinciden"
                return
            }
            passwordInput.length < 4 -> {
                errorMensaje = "La contraseña debe tener al menos 4 caracteres"
                return
            }
        }

        val username = usuarioInput.trim()
        val nombre = nombreInput.trim()
        val pass = passwordInput.trim()
        isCargando = true
        errorMensaje = null

        viewModelScope.launch {
            try {
                val repo = repository
                if (repo == null) {
                    errorMensaje = "Base de datos no disponible temporalmente"
                    isCargando = false
                    return@launch
                }

                // 1. Guardar nuevo usuario permanente en SQLite
                val resultadoReg = repo.registrarNuevoUsuario(
                    usuario = username,
                    nombre = nombre,
                    correo = correo,
                    password = pass,
                    saldoInicial = 1000.0
                )

                if (resultadoReg.isFailure) {
                    errorMensaje = resultadoReg.exceptionOrNull()?.message ?: "Error al registrar usuario"
                    isCargando = false
                    return@launch
                }

                val usuarioLocal = resultadoReg.getOrThrow()
                usuarioActual = usuarioLocal.usuario
                saldo = 1000.0

                // 2. Registrar en la base de datos del servidor Spring Boot
                try {
                    val resp = RetrofitClient.apiService.crearJugador(
                        CrearJugadorRequest(nombre = username, saldo = 1000.0)
                    )
                    if (resp.isSuccessful && resp.body() != null) {
                        serverConectado = true
                        jugadorId = resp.body()!!.id
                    } else {
                        serverConectado = false
                    }
                } catch (_: Exception) {
                    serverConectado = false
                }

                cargarUsuariosRegistrados()
                sesionActiva = true
                isCargando = false
                usuarioInput = ""
                passwordInput = ""
                nombreInput = ""
                correoInput = ""
                confirmPasswordInput = ""
                errorMensaje = null
                onSuccess()
            } catch (e: Exception) {
                isCargando = false
                errorMensaje = "Error en el registro: ${e.localizedMessage}"
            }
        }
    }

    // REGLA ESTRICTA DE ECONOMÍA (Salvavidas contra bancarrota):
    // El saldo nunca puede quedar en $0. Siempre se garantiza como mínimo $1 MXN.
    // Además, se actualiza inmediatamente en la base de datos persistente SQLite.
    fun descontarSaldo(monto: Double) {
        val nuevo = saldo - monto
        saldo = if (nuevo < 1.0) 1.0 else nuevo
        actualizarSaldoPersistente(saldo)
    }

    fun sumarPremio(premio: Double) {
        saldo += premio
        actualizarSaldoPersistente(saldo)
    }

    private fun actualizarSaldoPersistente(nuevoSaldo: Double) {
        viewModelScope.launch {
            try {
                repository?.actualizarSaldo(usuarioActual, nuevoSaldo)
            } catch (_: Exception) {}
        }
    }

    // Cerrar sesión limpiando la sesión activa de SQLite
    fun cerrarSesion(onLogout: () -> Unit) {
        viewModelScope.launch {
            try {
                repository?.cerrarSesion()
            } catch (_: Exception) {}
            usuarioActual = ""
            jugadorId = null
            usuarioInput = ""
            passwordInput = ""
            errorMensaje = null
            sesionActiva = false
            onLogout()
        }
    }
}
