package com.itsx.speaktutor.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.logic.EstadoVozDSP
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.navigation.Screen

@Composable
fun ContenedorMetronomo(navController: NavController, onBack: () -> Unit) {
    val motorDSP = remember { MotorAudioDSP() }

    DisposableEffect(Unit) {
        motorDSP.iniciarAnalisisDSP()
        onDispose { motorDSP.detenerAnalisis(null) }
    }

    MetronomoScreen(navController = navController, onBack = onBack, motorDSP = motorDSP)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomoScreen(navController: NavController, onBack: () -> Unit, motorDSP: MotorAudioDSP) {
    val context = LocalContext.current
    val metricasDSP by motorDSP.metricasAcusticas.collectAsState()

    var bpm by remember { mutableFloatStateOf(40f) }
    var enPulso by remember { mutableStateOf(false) }
    var aciertos by remember { mutableStateOf(0) }
    var bloqueos by remember { mutableStateOf(0) }
    var omisiones by remember { mutableStateOf(0) }
    var mensajeFeedback by remember { mutableStateOf("Espera el pulso para hablar...") }
    var habloEnEsteCiclo by remember { mutableStateOf(false) }

    LaunchedEffect(bpm) {
        val intervaloMs = (60000 / bpm.toInt()).toLong()
        while (true) {
            enPulso = true
            habloEnEsteCiclo = false
            delay(500)

            enPulso = false
            if (!habloEnEsteCiclo) {
                omisiones++
                mensajeFeedback = "¡Se te pasó! Omisión detectada."
            }
            delay(intervaloMs - 500)
        }
    }

    LaunchedEffect(metricasDSP.estado) {
        if (metricasDSP.estado != EstadoVozDSP.SILENCIO && !habloEnEsteCiclo) {
            habloEnEsteCiclo = true
            if (enPulso) {
                if (metricasDSP.estado == EstadoVozDSP.BLOQUEO) {
                    bloqueos++
                    mensajeFeedback = "⚠️ Bloqueo detectado."
                } else {
                    aciertos++
                    mensajeFeedback = "¡Perfecto! Sincronizado."
                }
            } else {
                bloqueos++
                mensajeFeedback = "Fuera de tiempo (Impulsividad)."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metrónomo") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(ArrowLeftCircleIcon, "Regresar") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { /* Ya estás aquí */ },
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                onNavigateProgreso = { navController.navigate(Screen.Progreso.route) },
                onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) },
                onNavigateBio = { navController.navigate(Screen.Biofeedback.route) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF3949AB)))).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Control de Impulsividad", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Habla SOLO cuando el círculo esté verde", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    val escalaPulso by animateFloatAsState(targetValue = if (enPulso) 1.5f else 1.0f)
                    val colorPulso by animateColorAsState(targetValue = if (enPulso) Color(0xFF69F0AE) else Color(0xFF9E9E9E))

                    Box(modifier = Modifier.size(100.dp).scale(escalaPulso).clip(CircleShape).background(colorPulso))

                    Spacer(modifier = Modifier.height(30.dp))
                    Text(mensajeFeedback, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (mensajeFeedback.contains("Perfecto")) Color(0xFF69F0AE) else if (mensajeFeedback.contains("Bloqueo") || mensajeFeedback.contains("Omisión")) Color(0xFFFF5252) else Color.White)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aciertos", color = Color.White); Text("$aciertos", fontSize = 28.sp, color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bloqueos", color = Color.White); Text("$bloqueos", fontSize = 28.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Omisiones", color = Color.White); Text("$omisiones", fontSize = 28.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // BOTÓN PRINCIPAL DE GUARDADO REPARADO
                    Button(
                        onClick = {
                            val total = aciertos + bloqueos + omisiones
                            val calif = if (total > 0) (aciertos * 100) / total else 0
                            ProgresoStorage.guardarSesion(context, "Metrónomo DSP", aciertos, bloqueos + omisiones, calif)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Text("💾 FINALIZAR Y GUARDAR PROGRESO", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Velocidad: ${bpm.toInt()} BPM", color = Color.White, fontWeight = FontWeight.Bold)
                    Slider(value = bpm, onValueChange = { bpm = it }, valueRange = 30f..80f, modifier = Modifier.fillMaxWidth(0.9f))
                }
            }
        }
    }
}