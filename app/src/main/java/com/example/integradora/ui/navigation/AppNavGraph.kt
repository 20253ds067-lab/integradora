package com.example.integradora.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.integradora.ui.screens.amigos.AmigosScreen
import com.example.integradora.ui.screens.amigos.AmigosViewModel
import com.example.integradora.ui.screens.blackjack.BlackjackScreen
import com.example.integradora.ui.screens.blackjack.BlackjackViewModel
import com.example.integradora.ui.screens.dados.DadosScreen
import com.example.integradora.ui.screens.dados.DadosViewModel
import com.example.integradora.ui.screens.login.AuthViewModel
import com.example.integradora.ui.screens.login.LoginScreen
import com.example.integradora.ui.screens.menu.MenuScreen
import com.example.integradora.ui.screens.ranking.RankingScreen
import com.example.integradora.ui.screens.ranking.RankingViewModel
import com.example.integradora.ui.screens.registro.RegisterScreen
import com.example.integradora.ui.screens.ruleta.RuletaScreen
import com.example.integradora.ui.screens.ruleta.RuletaViewModel

// Definición de las rutas de navegación estilo UTEZ Aplicaciones Móviles
sealed class Route(val path: String) {
    object Login      : Route("login")
    object Registro   : Route("registro")
    object Menu       : Route("menu")
    object Ruleta     : Route("ruleta")
    object Dados      : Route("dados")
    object Blackjack  : Route("blackjack")
    object Amigos     : Route("amigos")
    object Ranking    : Route("ranking")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    ruletaViewModel: RuletaViewModel = viewModel(),
    dadosViewModel: DadosViewModel = viewModel(),
    blackjackViewModel: BlackjackViewModel = viewModel(),
    amigosViewModel: AmigosViewModel = viewModel(),
    rankingViewModel: RankingViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.LaunchedEffect(authViewModel.usuarioActual) {
        authViewModel.setContext(context)
        amigosViewModel.setContext(context, authViewModel.usuarioActual)
    }

    NavHost(
        navController = navController,
        startDestination = Route.Login.path,
        modifier = modifier
    ) {
        // 1. Login
        composable(Route.Login.path) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(Route.Registro.path) },
                onLoginSuccess = {
                    navController.navigate(Route.Menu.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                }
            )
        }

        // 2. Registro
        composable(Route.Registro.path) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Route.Menu.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                }
            )
        }

        // 3. Menú Principal
        composable(Route.Menu.path) {
            MenuScreen(
                authViewModel = authViewModel,
                onNavigateToRuleta    = { navController.navigate(Route.Ruleta.path) },
                onNavigateToDados     = { navController.navigate(Route.Dados.path) },
                onNavigateToBlackjack = { navController.navigate(Route.Blackjack.path) },
                onNavigateToAmigos    = { navController.navigate(Route.Amigos.path) },
                onNavigateToRanking   = { navController.navigate(Route.Ranking.path) },
                onLogout = {
                    authViewModel.cerrarSesion {
                        navController.navigate(Route.Login.path) {
                            popUpTo(Route.Menu.path) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 4. Ruleta
        composable(Route.Ruleta.path) {
            RuletaScreen(
                viewModel = ruletaViewModel,
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 5. Dados
        composable(Route.Dados.path) {
            DadosScreen(
                viewModel = dadosViewModel,
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 6. Blackjack
        composable(Route.Blackjack.path) {
            BlackjackScreen(
                viewModel = blackjackViewModel,
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 7. Amigos
        composable(Route.Amigos.path) {
            androidx.compose.runtime.LaunchedEffect(authViewModel.usuarioActual) {
                amigosViewModel.setContext(context, authViewModel.usuarioActual)
            }
            AmigosScreen(
                viewModel = amigosViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 8. Ranking
        composable(Route.Ranking.path) {
            androidx.compose.runtime.LaunchedEffect(authViewModel.usuarioActual) {
                rankingViewModel.setContext(context, authViewModel.usuarioActual)
            }
            RankingScreen(
                viewModel = rankingViewModel,
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

