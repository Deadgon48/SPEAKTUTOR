package com.itsx.speaktutor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.itsx.speaktutor.ui.theme.SPEAKTUTORTheme

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.itsx.speaktutor.ui.navigation.Screen
import com.itsx.speaktutor.ui.screens.MetronomoScreen
import com.itsx.speaktutor.ui.screens.HablaEstiradaScreen
import com.itsx.speaktutor.ui.screens.PronunciacionInstanteScreen
import com.itsx.speaktutor.ui.screens.RitmoFluidezScreen
import com.itsx.speaktutor.ui.screens.TarjetasShaderScreen
import com.itsx.speaktutor.ui.screens.SimulacionSituacionesScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SPEAKTUTORTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 1. Declarar el navController primero
                    val navController = rememberNavController()

                    // 2. Un solo NavHost limpio que maneja todas las rutas
                    NavHost(
                        navController = navController,
                        startDestination = Screen.TarjetasShader.route, // Inicia con el menú moderno de tarjetas
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.TarjetasShader.route) {
                            TarjetasShaderScreen(
                                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                                onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                                onNavigatePronunciacioninstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                                onBack = { finish() } // Cierra la app al salir del menú principal
                            )
                        }
                        composable(Screen.Metronomo.route) {
                            MetronomoScreen(navController = navController,
                                onBack = { navController.popBackStack() })
                        }
                        composable(Screen.HablaEstirada.route) {
                            HablaEstiradaScreen(navController = navController,
                                onBack = { navController.popBackStack() })
                        }
                        composable(Screen.RitmoFluidez.route) {
                            RitmoFluidezScreen(navController = navController,
                                onBack = { navController.popBackStack() })
                        }
                        composable(Screen.SimulacionSituaciones.route) {
                            SimulacionSituacionesScreen(navController = navController,
                                onBack = { navController.popBackStack() })
                        }
                        composable(Screen.PronunciacionInstante.route) {
                            PronunciacionInstanteScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        SPEAKTUTORTheme {
            Greeting("Android")
        }
    }
}