package com.itsx.speaktutor

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.ui.navigation.Screen
import com.itsx.speaktutor.ui.screens.ConcienciaFonologicaScreen
import com.itsx.speaktutor.ui.screens.ContenedorBiofeedback
import com.itsx.speaktutor.ui.screens.ContenedorMetronomo
import com.itsx.speaktutor.ui.screens.EjerciciosAdaptativosScreen
import com.itsx.speaktutor.ui.screens.HablaEstiradaScreen
import com.itsx.speaktutor.ui.screens.ProgresoScreen
import com.itsx.speaktutor.ui.screens.PronunciacionInstanteScreen
import com.itsx.speaktutor.ui.screens.ReporteMultidimensionalScreen
import com.itsx.speaktutor.ui.screens.RitmoFluidezScreen
import com.itsx.speaktutor.ui.screens.SimulacionSituacionesScreen
import com.itsx.speaktutor.ui.screens.TarjetasArticulacionScreen
import com.itsx.speaktutor.ui.screens.TarjetasShaderScreen
import com.itsx.speaktutor.ui.theme.ModoTema
import com.itsx.speaktutor.ui.theme.SPEAKTUTORTheme
import com.itsx.speaktutor.ui.theme.TemaStorage

import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargar preferencia guardada (Sistema / Claro / Oscuro)
        TemaStorage.inicializar(this)

        // Desactiva el anuncio automático al abrir la app para no interrumpir la terapia
        StartAppAd.disableSplash()

        StartAppAd.disableAutoInterstitial()

        enableEdgeToEdge()
        setContent {
            val modoActual = TemaStorage.modoActual
            val esOscuro = when (modoActual) {
                ModoTema.SISTEMA -> isSystemInDarkTheme()
                ModoTema.CLARO -> false
                ModoTema.OSCURO -> true
            }

            // Ajusta el color de los iconos de hora/batería según el tema elegido
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !esOscuro
                }
            }

            SPEAKTUTORTheme(modoTema = modoActual) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    val motorDSP = remember { MotorAudioDSP() }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.TarjetasShader.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.TarjetasShader.route) {
                            TarjetasShaderScreen(
                                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                                onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                                onNavigatePronunciacioninstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                                onNavigateProgreso = { navController.navigate(Screen.Progreso.route) },
                                onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) },
                                onNavigateBio = { navController.navigate(Screen.Biofeedback.route) },
                                onNavigateTarjetasArticulacion = { navController.navigate(Screen.TarjetasArticulacion.route) },
                                onNavigateConcienciaFonologica = { navController.navigate(Screen.ConcienciaFonologica.route) },
                                onNavigateReporteMultidimensional = { navController.navigate(Screen.ReporteMultidimensional.route) },
                                onBack = { finish() }
                            )
                        }
                        composable(Screen.Metronomo.route) {
                            ContenedorMetronomo(
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Biofeedback.route) {
                            ContenedorBiofeedback(
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.HablaEstirada.route) {
                            HablaEstiradaScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.RitmoFluidez.route) {
                            RitmoFluidezScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.SimulacionSituaciones.route) {
                            SimulacionSituacionesScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.PronunciacionInstante.route) {
                            PronunciacionInstanteScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Progreso.route) {
                            ProgresoScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.EjerciciosAdaptativos.route) {
                            EjerciciosAdaptativosScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.TarjetasArticulacion.route) {
                            TarjetasArticulacionScreen(
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.ConcienciaFonologica.route) {
                            ConcienciaFonologicaScreen(
                                navController = navController,
                                motorDSP = motorDSP,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.ReporteMultidimensional.route) {
                            ReporteMultidimensionalScreen(
                                navController = navController
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