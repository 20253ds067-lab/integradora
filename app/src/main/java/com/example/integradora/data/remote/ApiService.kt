package com.example.integradora.data.remote

import com.example.integradora.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Endpoints Jugador ---
    @GET("jugador")
    suspend fun obtenerTodosJugadores(): Response<List<JugadorResponse>>

    @GET("jugador/{id}")
    suspend fun obtenerJugadorPorId(@Path("id") id: Long): Response<JugadorResponse>

    @POST("jugador")
    suspend fun crearJugador(@Body request: CrearJugadorRequest): Response<JugadorResponse>

    @POST("jugador/recargarSaldo")
    suspend fun recargarSaldo(@Body request: RecargarSaldoRequest): Response<JugadorResponse>

    // --- Endpoints Partida (Dados / Casino) ---
    @POST("partida/iniciarPartida")
    suspend fun iniciarPartida(@Body request: IniciarPartidaRequest): Response<PartidaResponse>

    @POST("partida/realizarTiro")
    suspend fun realizarTiro(@Body request: RealizarTiroRequest): Response<TiroResponse>

    @POST("partida/finalizarPartidaManual")
    suspend fun finalizarPartidaManual(@Body request: FinalizarPartidaRequest): Response<PartidaResponse>

    @GET("partida/obtenerHistorialPartida/{jugadorId}")
    suspend fun obtenerHistorialPartida(@Path("jugadorId") jugadorId: Long): Response<List<PartidaResponse>>
}
