package com.example.integradora.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.integradora.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class UsuarioLocal(
    val id: Long = 0,
    val usuario: String,
    val nombre: String,
    val correo: String = "",
    val saldo: Double = 1000.0,
    val ultimoLogin: Long = System.currentTimeMillis()
)

data class AmigoRegistro(
    val id: Long,
    val usuarioPropietario: String,
    val nombreAmigo: String,
    val apodo: String = "",
    val estado: String = "ACEPTADO", // "ACEPTADO", "PENDIENTE_RECIBIDA", "PENDIENTE_ENVIADA"
    val fechaCreacion: Long = System.currentTimeMillis()
)

data class RankingUser(
    val posicion: Int = 0,
    val usuario: String,
    val nombre: String,
    val saldo: Double,
    val esUsuarioActual: Boolean = false
)

class CasinoRepository private constructor(context: Context) {

    private val dbHelper = CasinoDatabaseHelper(context)

    companion object {
        @Volatile
        private var INSTANCE: CasinoRepository? = null

        fun getInstance(context: Context): CasinoRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CasinoRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Hash de contraseñas SHA-256 idéntico al proyecto de referencia Memorias_Velez_Gael.
         */
        fun hashPassword(raw: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    // ─── USUARIOS Y LOGINS PERSISTENTES (ESTILO MEMORIAS) ──────────────────────

    /**
     * Valida credenciales estrictas para inicio de sesión contra SQLite.
     * Retorna Result.failure si el usuario no está registrado o la contraseña no coincide.
     */
    suspend fun validarCredenciales(
        usuario: String,
        password: String
    ): Result<UsuarioLocal> = withContext(Dispatchers.IO) {
        val userTrim = usuario.trim()
        val passTrim = password.trim()
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            CasinoDatabaseHelper.TABLE_USUARIOS,
            null,
            "LOWER(${CasinoDatabaseHelper.COL_U_USUARIO}) = LOWER(?)",
            arrayOf(userTrim),
            null, null, null
        )

        val existente = cursor.use {
            if (it.moveToFirst()) {
                val passAlmacenada = it.getString(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_U_PASSWORD)) ?: ""
                val u = cursorToUsuario(it)
                Pair(u, passAlmacenada)
            } else null
        }

        if (existente == null) {
            return@withContext Result.failure(Exception("Cuenta no encontrada"))
        }

        val (usuarioDb, passDb) = existente
        val hashIngresado = hashPassword(passTrim)

        // Comprobación segura de contraseña (hash SHA-256 o texto plano si venía de versión previa)
        val coincide = passDb == hashIngresado || passDb == passTrim
        if (!coincide) {
            return@withContext Result.failure(Exception("Contraseña incorrecta"))
        }

        // Credenciales correctas: actualizar último login y guardar sesión
        val writeDb = dbHelper.writableDatabase
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(CasinoDatabaseHelper.COL_U_ULTIMO_LOGIN, now)
        }
        writeDb.update(
            CasinoDatabaseHelper.TABLE_USUARIOS,
            values,
            "${CasinoDatabaseHelper.COL_U_ID} = ?",
            arrayOf(usuarioDb.id.toString())
        )
        val logueado = usuarioDb.copy(ultimoLogin = now)
        guardarSesionActiva(logueado)
        Result.success(logueado)
    }

    /**
     * Registra un nuevo usuario permanente en la base de datos local SQLite con hash SHA-256.
     * Falla si el nombre de usuario ya está en uso.
     */
    suspend fun registrarNuevoUsuario(
        usuario: String,
        nombre: String,
        correo: String = "",
        password: String = "",
        saldoInicial: Double = 1000.0
    ): Result<UsuarioLocal> = withContext(Dispatchers.IO) {
        val userTrim = usuario.trim()
        val db = dbHelper.writableDatabase

        // Verificar si ya existe en la base de datos local
        val cursor = db.query(
            CasinoDatabaseHelper.TABLE_USUARIOS,
            arrayOf(CasinoDatabaseHelper.COL_U_ID),
            "LOWER(${CasinoDatabaseHelper.COL_U_USUARIO}) = LOWER(?)",
            arrayOf(userTrim),
            null, null, null
        )
        val existe = cursor.use { it.moveToFirst() }
        if (existe) {
            return@withContext Result.failure(Exception("El usuario ya existe. Inicia sesión con tus datos."))
        }

        val now = System.currentTimeMillis()
        val passHash = hashPassword(password.trim())
        val values = ContentValues().apply {
            put(CasinoDatabaseHelper.COL_U_USUARIO, userTrim)
            put(CasinoDatabaseHelper.COL_U_NOMBRE, nombre.trim().ifBlank { userTrim })
            put(CasinoDatabaseHelper.COL_U_CORREO, correo.trim())
            put(CasinoDatabaseHelper.COL_U_PASSWORD, passHash)
            put(CasinoDatabaseHelper.COL_U_SALDO, saldoInicial)
            put(CasinoDatabaseHelper.COL_U_ULTIMO_LOGIN, now)
        }
        val id = db.insert(CasinoDatabaseHelper.TABLE_USUARIOS, null, values)
        val nuevo = UsuarioLocal(
            id = id,
            usuario = userTrim,
            nombre = nombre.trim().ifBlank { userTrim },
            correo = correo.trim(),
            saldo = saldoInicial,
            ultimoLogin = now
        )
        guardarSesionActiva(nuevo)
        Result.success(nuevo)
    }

    /**
     * Valida si una cuenta de usuario existe (en SQLite o en Spring Boot) para permitir agregar amigos.
     * Chequea SQLite al instante (<1ms) y si no existe busca en servidor con timeout ultrarrápido (500ms).
     */
    suspend fun existeUsuario(nombreUsuario: String): Boolean = withContext(Dispatchers.IO) {
        val userTrim = nombreUsuario.trim()
        val db = dbHelper.readableDatabase

        // 1. Chequeo instantáneo en SQLite local (< 1 milisegundo)
        val cursor = db.query(
            CasinoDatabaseHelper.TABLE_USUARIOS,
            arrayOf(CasinoDatabaseHelper.COL_U_ID),
            "LOWER(${CasinoDatabaseHelper.COL_U_USUARIO}) = LOWER(?) OR LOWER(${CasinoDatabaseHelper.COL_U_NOMBRE}) = LOWER(?)",
            arrayOf(userTrim, userTrim),
            null, null, null
        )
        val existeLocal = cursor.use { it.moveToFirst() }
        if (existeLocal) return@withContext true

        // 2. Chequeo en Spring Boot con límite estricto de 600ms para evitar bloqueos
        try {
            kotlinx.coroutines.withTimeoutOrNull(600L) {
                val resp = RetrofitClient.apiService.obtenerTodosJugadores()
                if (resp.isSuccessful && resp.body() != null) {
                    resp.body()!!.any { it.nombre.equals(userTrim, ignoreCase = true) }
                } else false
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun actualizarSaldo(usuario: String, nuevoSaldo: Double) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CasinoDatabaseHelper.COL_U_SALDO, nuevoSaldo)
        }
        db.update(
            CasinoDatabaseHelper.TABLE_USUARIOS,
            values,
            "${CasinoDatabaseHelper.COL_U_USUARIO} = ?",
            arrayOf(usuario.trim())
        )
        // Actualizar también en la sesión si coincide
        val sesionValues = ContentValues().apply {
            put(CasinoDatabaseHelper.COL_S_SALDO, nuevoSaldo)
        }
        db.update(
            CasinoDatabaseHelper.TABLE_SESION,
            sesionValues,
            "${CasinoDatabaseHelper.COL_S_USUARIO} = ?",
            arrayOf(usuario.trim())
        )
    }

    suspend fun obtenerTodosUsuarios(): List<UsuarioLocal> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<UsuarioLocal>()
        val cursor = db.query(
            CasinoDatabaseHelper.TABLE_USUARIOS,
            null,
            null, null, null, null,
            "${CasinoDatabaseHelper.COL_U_ULTIMO_LOGIN} DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                lista.add(cursorToUsuario(it))
            }
        }
        lista
    }

    // ─── SESIÓN ACTIVA (RECORDAR LOGIN) ──────────────────────────────────────

    private fun guardarSesionActiva(usuario: UsuarioLocal) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CasinoDatabaseHelper.COL_S_ID, 1)
            put(CasinoDatabaseHelper.COL_S_USUARIO, usuario.usuario)
            put(CasinoDatabaseHelper.COL_S_NOMBRE, usuario.nombre)
            put(CasinoDatabaseHelper.COL_S_SALDO, usuario.saldo)
            put(CasinoDatabaseHelper.COL_S_ACTIVA, 1)
        }
        db.insertWithOnConflict(
            CasinoDatabaseHelper.TABLE_SESION,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun obtenerSesionActiva(): UsuarioLocal? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            CasinoDatabaseHelper.TABLE_SESION,
            null,
            "${CasinoDatabaseHelper.COL_S_ID} = 1 AND ${CasinoDatabaseHelper.COL_S_ACTIVA} = 1",
            null, null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val usuario = it.getString(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_S_USUARIO))
                val nombre = it.getString(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_S_NOMBRE))
                val saldo = it.getDouble(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_S_SALDO))
                UsuarioLocal(usuario = usuario, nombre = nombre, saldo = saldo)
            } else null
        }
    }

    suspend fun cerrarSesion() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(CasinoDatabaseHelper.TABLE_SESION, null, null)
    }

    // ─── AMIGOS Y SOLICITUDES PERSISTENTES ───────────────────────────────────

    suspend fun obtenerAmigos(usuarioPropietario: String): List<AmigoRegistro> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<AmigoRegistro>()
        val cursor = db.query(
            CasinoDatabaseHelper.TABLE_AMIGOS,
            null,
            "${CasinoDatabaseHelper.COL_A_PROPIETARIO} = ? AND ${CasinoDatabaseHelper.COL_A_ESTADO} = 'ACEPTADO'",
            arrayOf(usuarioPropietario.trim()),
            null, null,
            "${CasinoDatabaseHelper.COL_A_ID} DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_A_ID))
                val prop = it.getString(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_A_PROPIETARIO))
                val amigo = it.getString(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_A_NOMBRE))
                val apodo = it.getString(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_A_APODO)) ?: ""
                val est = it.getString(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_A_ESTADO)) ?: "ACEPTADO"
                val fecha = it.getLong(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_A_FECHA))
                lista.add(AmigoRegistro(id, prop, amigo, apodo, est, fecha))
            }
        }
        lista
    }

    suspend fun obtenerSolicitudesPendientes(usuarioPropietario: String): List<AmigoRegistro> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<AmigoRegistro>()
        val cursor = db.query(
            CasinoDatabaseHelper.TABLE_AMIGOS,
            null,
            "${CasinoDatabaseHelper.COL_A_PROPIETARIO} = ? AND ${CasinoDatabaseHelper.COL_A_ESTADO} = 'PENDIENTE_RECIBIDA'",
            arrayOf(usuarioPropietario.trim()),
            null, null,
            "${CasinoDatabaseHelper.COL_A_ID} DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_A_ID))
                val prop = it.getString(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_A_PROPIETARIO))
                val amigo = it.getString(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_A_NOMBRE))
                val apodo = it.getString(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_A_APODO)) ?: ""
                val est = it.getString(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_A_ESTADO)) ?: "PENDIENTE_RECIBIDA"
                val fecha = it.getLong(it.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_A_FECHA))
                lista.add(AmigoRegistro(id, prop, amigo, apodo, est, fecha))
            }
        }
        lista
    }

    suspend fun enviarSolicitudAmistad(propietario: String, amigo: String, apodo: String = "") = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val now = System.currentTimeMillis()

        // 1. Registro para quien envía
        val valuesEnviada = ContentValues().apply {
            put(CasinoDatabaseHelper.COL_A_PROPIETARIO, propietario.trim())
            put(CasinoDatabaseHelper.COL_A_NOMBRE, amigo.trim())
            put(CasinoDatabaseHelper.COL_A_APODO, apodo.trim())
            put(CasinoDatabaseHelper.COL_A_ESTADO, "PENDIENTE_ENVIADA")
            put(CasinoDatabaseHelper.COL_A_FECHA, now)
        }
        db.insert(CasinoDatabaseHelper.TABLE_AMIGOS, null, valuesEnviada)

        // 2. Registro para quien recibe la solicitud
        val valuesRecibida = ContentValues().apply {
            put(CasinoDatabaseHelper.COL_A_PROPIETARIO, amigo.trim())
            put(CasinoDatabaseHelper.COL_A_NOMBRE, propietario.trim())
            put(CasinoDatabaseHelper.COL_A_APODO, "")
            put(CasinoDatabaseHelper.COL_A_ESTADO, "PENDIENTE_RECIBIDA")
            put(CasinoDatabaseHelper.COL_A_FECHA, now)
        }
        db.insert(CasinoDatabaseHelper.TABLE_AMIGOS, null, valuesRecibida)
    }

    suspend fun aceptarSolicitudAmistad(propietario: String, amigo: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CasinoDatabaseHelper.COL_A_ESTADO, "ACEPTADO")
        }
        // Actualizar en ambas direcciones
        db.update(
            CasinoDatabaseHelper.TABLE_AMIGOS,
            values,
            "(${CasinoDatabaseHelper.COL_A_PROPIETARIO} = ? AND ${CasinoDatabaseHelper.COL_A_NOMBRE} = ?) OR (${CasinoDatabaseHelper.COL_A_PROPIETARIO} = ? AND ${CasinoDatabaseHelper.COL_A_NOMBRE} = ?)",
            arrayOf(propietario.trim(), amigo.trim(), amigo.trim(), propietario.trim())
        )
    }

    suspend fun rechazarSolicitudAmistad(propietario: String, amigo: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            CasinoDatabaseHelper.TABLE_AMIGOS,
            "(${CasinoDatabaseHelper.COL_A_PROPIETARIO} = ? AND ${CasinoDatabaseHelper.COL_A_NOMBRE} = ?) OR (${CasinoDatabaseHelper.COL_A_PROPIETARIO} = ? AND ${CasinoDatabaseHelper.COL_A_NOMBRE} = ?)",
            arrayOf(propietario.trim(), amigo.trim(), amigo.trim(), propietario.trim())
        )
    }

    suspend fun actualizarAmigo(id: Long, nuevoNombre: String, nuevoApodo: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CasinoDatabaseHelper.COL_A_NOMBRE, nuevoNombre.trim())
            put(CasinoDatabaseHelper.COL_A_APODO, nuevoApodo.trim())
        }
        db.update(
            CasinoDatabaseHelper.TABLE_AMIGOS,
            values,
            "${CasinoDatabaseHelper.COL_A_ID} = ?",
            arrayOf(id.toString())
        )
    }

    suspend fun eliminarAmigo(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            CasinoDatabaseHelper.TABLE_AMIGOS,
            "${CasinoDatabaseHelper.COL_A_ID} = ?",
            arrayOf(id.toString())
        )
    }

    // ─── RANKINGS (GENERAL Y AMIGOS) ESTILO MEMORIAS ─────────────────────────

    /**
     * Asegura la existencia de jugadores de la comunidad y amigos iniciales
     * para que la pantalla de Rankings y Amigos siempre tenga información viva.
     */
    fun asegurarDatosIniciales(usuarioActual: String = "") {
        try {
            val db = dbHelper.writableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM ${CasinoDatabaseHelper.TABLE_USUARIOS}", null)
            val count = cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }

            if (count < 4) {
                val now = System.currentTimeMillis()
                val passHash = hashPassword("1234")

                val jugadoresComunidad = listOf(
                    Triple("carlos_99", "Carlos Mendoza", 15500.0),
                    Triple("valeria_m", "Valeria Morales", 11200.0),
                    Triple("diego_casino", "Diego Herrera", 8750.0),
                    Triple("sofia_pro", "Sofía Ramírez", 5400.0),
                    Triple("mateo_77", "Mateo Castro", 3200.0),
                    Triple("ana_poker", "Ana Beltrán", 2100.0)
                )

                jugadoresComunidad.forEach { (usr, nom, sal) ->
                    val values = ContentValues().apply {
                        put(CasinoDatabaseHelper.COL_U_USUARIO, usr)
                        put(CasinoDatabaseHelper.COL_U_NOMBRE, nom)
                        put(CasinoDatabaseHelper.COL_U_CORREO, "$usr@casino.com")
                        put(CasinoDatabaseHelper.COL_U_PASSWORD, passHash)
                        put(CasinoDatabaseHelper.COL_U_SALDO, sal)
                        put(CasinoDatabaseHelper.COL_U_ULTIMO_LOGIN, now)
                    }
                    db.insertWithOnConflict(
                        CasinoDatabaseHelper.TABLE_USUARIOS,
                        null,
                        values,
                        android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
                    )
                }
            }

            if (usuarioActual.isNotBlank()) {
                val cursorAmigos = db.rawQuery(
                    "SELECT COUNT(*) FROM ${CasinoDatabaseHelper.TABLE_AMIGOS} WHERE ${CasinoDatabaseHelper.COL_A_PROPIETARIO} = ?",
                    arrayOf(usuarioActual.trim())
                )
                val countAmigos = cursorAmigos.use { if (it.moveToFirst()) it.getInt(0) else 0 }

                if (countAmigos == 0) {
                    val now = System.currentTimeMillis()
                    // 1. Amigo aceptado: carlos_99 (El Tiburón)
                    val valuesAmigo = ContentValues().apply {
                        put(CasinoDatabaseHelper.COL_A_PROPIETARIO, usuarioActual.trim())
                        put(CasinoDatabaseHelper.COL_A_NOMBRE, "carlos_99")
                        put(CasinoDatabaseHelper.COL_A_APODO, "El Tiburón")
                        put(CasinoDatabaseHelper.COL_A_ESTADO, "ACEPTADO")
                        put(CasinoDatabaseHelper.COL_A_FECHA, now)
                    }
                    db.insert(CasinoDatabaseHelper.TABLE_AMIGOS, null, valuesAmigo)

                    // 2. Solicitud pendiente recibida: valeria_m (para probar Aceptar / Rechazar)
                    val valuesSolicitud = ContentValues().apply {
                        put(CasinoDatabaseHelper.COL_A_PROPIETARIO, usuarioActual.trim())
                        put(CasinoDatabaseHelper.COL_A_NOMBRE, "valeria_m")
                        put(CasinoDatabaseHelper.COL_A_APODO, "")
                        put(CasinoDatabaseHelper.COL_A_ESTADO, "PENDIENTE_RECIBIDA")
                        put(CasinoDatabaseHelper.COL_A_FECHA, now)
                    }
                    db.insert(CasinoDatabaseHelper.TABLE_AMIGOS, null, valuesSolicitud)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Carga inmediata del Ranking General desde SQLite (<2ms, cero bloqueo).
     */
    suspend fun obtenerRankingGeneralLocal(usuarioActual: String): List<RankingUser> = withContext(Dispatchers.IO) {
        asegurarDatosIniciales(usuarioActual)
        val usuariosDb = obtenerTodosUsuarios()
        val mapaUsuarios = mutableMapOf<String, RankingUser>()

        usuariosDb.forEach { u ->
            mapaUsuarios[u.usuario.lowercase()] = RankingUser(
                usuario = u.usuario,
                nombre = u.nombre,
                saldo = u.saldo,
                esUsuarioActual = u.usuario.equals(usuarioActual, ignoreCase = true)
            )
        }

        mapaUsuarios.values
            .sortedByDescending { it.saldo }
            .mapIndexed { idx, item -> item.copy(posicion = idx + 1) }
    }

    /**
     * Carga inmediata del Ranking de Amigos filtrando sobre la lista general y amigos aceptados (<1ms).
     */
    suspend fun obtenerRankingAmigosLocal(
        usuarioActual: String,
        rankingGeneral: List<RankingUser>
    ): List<RankingUser> = withContext(Dispatchers.IO) {
        val amigos = obtenerAmigos(usuarioActual)
        val nombresAmigos = amigos.map { it.nombreAmigo.lowercase() }.toSet()

        rankingGeneral
            .filter { it.usuario.lowercase() in nombresAmigos || it.esUsuarioActual }
            .sortedByDescending { it.saldo }
            .mapIndexed { idx, item -> item.copy(posicion = idx + 1) }
    }

    /**
     * Sincronización asíncrona no bloqueante con Spring Boot (estilo Memorias).
     * Timeout estricto de 1.2 segundos para garantizar que nunca congele la app.
     */
    suspend fun sincronizarJugadoresRemotos(): Boolean = withContext(Dispatchers.IO) {
        try {
            kotlinx.coroutines.withTimeoutOrNull(1200L) {
                val resp = RetrofitClient.apiService.obtenerTodosJugadores()
                if (resp.isSuccessful && resp.body() != null) {
                    val db = dbHelper.writableDatabase
                    val now = System.currentTimeMillis()
                    val hashDefault = hashPassword("1234")

                    resp.body()!!.forEach { jug ->
                        val nom = jug.nombre.trim()
                        if (nom.isNotBlank()) {
                            val values = ContentValues().apply {
                                put(CasinoDatabaseHelper.COL_U_USUARIO, nom)
                                put(CasinoDatabaseHelper.COL_U_NOMBRE, nom)
                                put(CasinoDatabaseHelper.COL_U_CORREO, "${nom.lowercase()}@casino.com")
                                put(CasinoDatabaseHelper.COL_U_PASSWORD, hashDefault)
                                put(CasinoDatabaseHelper.COL_U_SALDO, jug.saldo)
                                put(CasinoDatabaseHelper.COL_U_ULTIMO_LOGIN, now)
                            }
                            val updated = db.update(
                                CasinoDatabaseHelper.TABLE_USUARIOS,
                                values,
                                "LOWER(${CasinoDatabaseHelper.COL_U_USUARIO}) = LOWER(?)",
                                arrayOf(nom)
                            )
                            if (updated == 0) {
                                db.insert(CasinoDatabaseHelper.TABLE_USUARIOS, null, values)
                            }
                        }
                    }
                    true
                } else false
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun obtenerRankingGeneral(usuarioActual: String): List<RankingUser> =
        obtenerRankingGeneralLocal(usuarioActual)

    suspend fun obtenerRankingAmigos(usuarioActual: String): List<RankingUser> =
        obtenerRankingAmigosLocal(usuarioActual, obtenerRankingGeneralLocal(usuarioActual))

    private fun cursorToUsuario(cursor: Cursor): UsuarioLocal {
        return UsuarioLocal(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_U_ID)),
            usuario = cursor.getString(cursor.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_U_USUARIO)),
            nombre = cursor.getString(cursor.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_U_NOMBRE)),
            correo = cursor.getString(cursor.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_U_CORREO)) ?: "",
            saldo = cursor.getDouble(cursor.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_U_SALDO)),
            ultimoLogin = cursor.getLong(cursor.getColumnIndexOrThrow(CasinoDatabaseHelper.COL_U_ULTIMO_LOGIN))
        )
    }
}
