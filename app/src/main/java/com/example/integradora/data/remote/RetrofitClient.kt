package com.example.integradora.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 10.0.2.2 es el alias del host en el emulador Android; 127.0.0.1 funciona con ADB reverse
    private const val DEFAULT_HOST = "10.0.2.2"
    private const val DEFAULT_PORT = 8080
    var currentBaseUrl: String = "http://$DEFAULT_HOST:$DEFAULT_PORT/"
        private set

    private var retrofitInstance: Retrofit? = null

    // Interceptor con fallback automático de host para máxima resiliencia en pruebas
    private val fallbackInterceptor = Interceptor { chain ->
        val request = chain.request()
        try {
            chain.proceed(request)
        } catch (e: Exception) {
            val url = request.url
            val alternateHost = if (url.host == "10.0.2.2") "127.0.0.1" else "10.0.2.2"
            val fallbackUrl = url.newBuilder().host(alternateHost).build()
            chain.proceed(request.newBuilder().url(fallbackUrl).build())
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(fallbackInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val apiService: ApiService
        get() {
            if (retrofitInstance == null) {
                retrofitInstance = buildRetrofit(currentBaseUrl)
            }
            return retrofitInstance!!.create(ApiService::class.java)
        }

    fun setCustomBaseUrl(url: String) {
        val formatted = if (url.endsWith("/")) url else "$url/"
        currentBaseUrl = formatted
        retrofitInstance = buildRetrofit(formatted)
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Prueba rápida no bloqueante para verificar disponibilidad de la API Spring Boot.
     */
    suspend fun checkServerConnection(): Boolean {
        return try {
            val response = apiService.obtenerTodosJugadores()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}
