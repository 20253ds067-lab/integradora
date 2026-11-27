package com.example.integradora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CrearJugadorRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("saldo") val saldo: Double
)

data class RecargarSaldoRequest(
    @SerializedName("jugadorId") val jugadorId: Long,
    @SerializedName("monto") val monto: Double
)

data class JugadorResponse(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("nombre") val nombre: String = "",
    @SerializedName("saldo") val saldo: Double = 0.0,
    @SerializedName("activo") val activo: Boolean = true
)
