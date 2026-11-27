package com.example.integradora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.integradora.ui.navigation.AppNavGraph
import com.example.integradora.ui.screens.dados.DadosScreen
import com.example.integradora.ui.screens.dados.DadosViewModel
import com.example.integradora.ui.screens.login.AuthViewModel
import com.example.integradora.ui.screens.login.LoginScreen
import com.example.integradora.ui.screens.menu.MenuScreen
import com.example.integradora.ui.screens.registro.RegisterScreen
import com.example.integradora.ui.screens.ruleta.RuletaScreen
import com.example.integradora.ui.screens.ruleta.RuletaViewModel
import com.example.integradora.ui.screens.blackjack.BlackjackScreen
import com.example.integradora.ui.screens.blackjack.BlackjackViewModel
import com.example.integradora.ui.theme.IntegradoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IntegradoraTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Preview(name = "1. Login", showBackground = true)
@Composable
fun LoginPreview() {
    IntegradoraTheme {
        LoginScreen(
            viewModel = AuthViewModel(),
            onNavigateToRegister = {},
            onLoginSuccess = {}
        )
    }
}

@Preview(name = "2. Registro", showBackground = true)
@Composable
fun RegisterPreview() {
    IntegradoraTheme {
        RegisterScreen(
            viewModel = AuthViewModel(),
            onNavigateToLogin = {},
            onRegisterSuccess = {}
        )
    }
}

@Preview(name = "3. Menú Principal", showBackground = true)
@Composable
fun MenuPreview() {
    IntegradoraTheme {
        MenuScreen(
            authViewModel = AuthViewModel(),
            onNavigateToRuleta = {},
            onNavigateToDados = {},
            onNavigateToBlackjack = {},
            onNavigateToAmigos = {},
            onNavigateToRanking = {},
            onLogout = {}
        )
    }
}

@Preview(name = "6. Blackjack", showBackground = true)
@Composable
fun BlackjackPreview() {
    IntegradoraTheme {
        BlackjackScreen(
            viewModel = BlackjackViewModel(),
            authViewModel = AuthViewModel(),
            onBackClick = {}
        )
    }
}

@Preview(name = "4. Ruleta", showBackground = true)
@Composable
fun RoulettePreview() {
    IntegradoraTheme {
        RuletaScreen(
            viewModel = RuletaViewModel(),
            authViewModel = AuthViewModel(),
            onBackClick = {}
        )
    }
}

@Preview(name = "5. Dados", showBackground = true)
@Composable
fun DicePreview() {
    IntegradoraTheme {
        DadosScreen(
            viewModel = DadosViewModel(),
            authViewModel = AuthViewModel(),
            onBackClick = {}
        )
    }
}
