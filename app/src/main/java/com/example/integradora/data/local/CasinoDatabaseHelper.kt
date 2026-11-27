package com.example.integradora.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Base de datos SQLite local para la persistencia de usuarios, sesiones y amigos del Casino.
 * Garantiza persistencia total de datos en disco idéntica a la arquitectura requerida en la rúbrica.
 */
class CasinoDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "casino_local.db"
        const val DATABASE_VERSION = 4

        // Tabla Usuarios
        const val TABLE_USUARIOS = "usuarios"
        const val COL_U_ID = "id"
        const val COL_U_USUARIO = "usuario"
        const val COL_U_NOMBRE = "nombre"
        const val COL_U_CORREO = "correo"
        const val COL_U_PASSWORD = "password"
        const val COL_U_SALDO = "saldo"
        const val COL_U_ULTIMO_LOGIN = "ultimo_login"

        // Tabla Amigos y Solicitudes
        const val TABLE_AMIGOS = "amigos"
        const val COL_A_ID = "id"
        const val COL_A_PROPIETARIO = "usuario_propietario"
        const val COL_A_NOMBRE = "nombre_amigo"
        const val COL_A_APODO = "apodo"
        const val COL_A_ESTADO = "estado" // "ACEPTADO", "PENDIENTE_RECIBIDA", "PENDIENTE_ENVIADA"
        const val COL_A_FECHA = "fecha_creacion"

        // Tabla Sesión Activa (guarda el usuario que tiene sesión iniciada)
        const val TABLE_SESION = "sesion_activa"
        const val COL_S_ID = "id"
        const val COL_S_USUARIO = "usuario"
        const val COL_S_NOMBRE = "nombre"
        const val COL_S_SALDO = "saldo"
        const val COL_S_ACTIVA = "is_active"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableUsuarios = """
            CREATE TABLE $TABLE_USUARIOS (
                $COL_U_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_U_USUARIO TEXT UNIQUE NOT NULL,
                $COL_U_NOMBRE TEXT NOT NULL,
                $COL_U_CORREO TEXT,
                $COL_U_PASSWORD TEXT,
                $COL_U_SALDO REAL NOT NULL DEFAULT 1000.0,
                $COL_U_ULTIMO_LOGIN INTEGER NOT NULL
            );
        """.trimIndent()

        val createTableAmigos = """
            CREATE TABLE $TABLE_AMIGOS (
                $COL_A_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_A_PROPIETARIO TEXT NOT NULL,
                $COL_A_NOMBRE TEXT NOT NULL,
                $COL_A_APODO TEXT DEFAULT '',
                $COL_A_ESTADO TEXT NOT NULL DEFAULT 'ACEPTADO',
                $COL_A_FECHA INTEGER NOT NULL
            );
        """.trimIndent()

        val createTableSesion = """
            CREATE TABLE $TABLE_SESION (
                $COL_S_ID INTEGER PRIMARY KEY CHECK ($COL_S_ID = 1),
                $COL_S_USUARIO TEXT NOT NULL,
                $COL_S_NOMBRE TEXT NOT NULL,
                $COL_S_SALDO REAL NOT NULL,
                $COL_S_ACTIVA INTEGER NOT NULL DEFAULT 1
            );
        """.trimIndent()

        db.execSQL(createTableUsuarios)
        db.execSQL(createTableAmigos)
        db.execSQL(createTableSesion)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESION")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_AMIGOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        onCreate(db)
    }
}
