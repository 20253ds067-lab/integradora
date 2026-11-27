package com.example.integradora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class IniciarPartidaRequest(
    @SerializedName("jugadorId") val jugadorId: Long,
    @SerializedName("apuesta") val apuesta: Double
)

data class RealizarTiroRequest(
    @SerializedName("partidaId") val partidaId: Long
)

data class FinalizarPartidaRequest(
    @SerializedName("partidaId") val partidaId: Long,
    @SerializedName("motivo") val motivo: String = "FINALIZADA_DESDE_MOVIL"
)

data class PartidaResponse(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("fecha") val fecha: String? = null,
    @SerializedName("estado") val estado: String = "EN_JUEGO",
    @SerializedName("apuesta") val apuesta: Double = 0.0,
    @SerializedName("jugador") val jugador: JugadorResponse? = null
)

data class TiroResponse(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("valorDado1") val valorDado1: Int = 1,
    @SerializedName("valorDado2") val valorDado2: Int = 1,
    @SerializedName("suma") val suma: Int = 2,
    @SerializedName("esGanador") val esGanador: Boolean = false,
    @SerializedName("partida") val partida: PartidaResponse? = null
)
