package com.example.integradora.ui.screens.ranking

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.integradora.data.local.CasinoRepository
import com.example.integradora.data.local.RankingUser
import kotlinx.coroutines.launch

class RankingViewModel(application: Application? = null) : ViewModel() {

    private var repository: CasinoRepository? = application?.let { CasinoRepository.getInstance(it) }
    private var usuarioActual: String = ""

    var tabSeleccionada by mutableIntStateOf(0) // 0: General, 1: Amigos
        private set

    var rankingGeneral by mutableStateOf<List<RankingUser>>(emptyList())
        private set

    var rankingAmigos by mutableStateOf<List<RankingUser>>(emptyList())
        private set

    var isCargando by mutableStateOf(false)
        private set

    fun setContext(context: Context, usuario: String) {
        if (repository == null) {
            repository = CasinoRepository.getInstance(context)
        }
        usuarioActual = usuario
        cargarRankings()
    }

    fun onTabSelected(index: Int) {
        tabSeleccionada = index
    }

    fun cargarRankings() {
        viewModelScope.launch {
            try {
                val repo = repository ?: return@launch

                // 1. Carga INMEDIATA desde SQLite local (cero latencia, respuesta instantánea en pantalla)
                val generalLocal = repo.obtenerRankingGeneralLocal(usuarioActual)
                val amigosLocal = repo.obtenerRankingAmigosLocal(usuarioActual, generalLocal)
                rankingGeneral = generalLocal
                rankingAmigos = amigosLocal
                isCargando = false

                // 2. Sincronización en segundo plano con Spring Boot (estilo Memorias, sin bloquear UI)
                val sincronizado = repo.sincronizarJugadoresRemotos()
                if (sincronizado) {
                    val generalActualizado = repo.obtenerRankingGeneralLocal(usuarioActual)
                    rankingGeneral = generalActualizado
                    rankingAmigos = repo.obtenerRankingAmigosLocal(usuarioActual, generalActualizado)
                }
            } catch (_: Exception) {
            } finally {
                isCargando = false
            }
        }
    }
}
