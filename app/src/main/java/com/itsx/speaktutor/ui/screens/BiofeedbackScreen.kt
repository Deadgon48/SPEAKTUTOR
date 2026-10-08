package com.itsx.speaktutor.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.logic.ParametrosAcusticos
import com.itsx.speaktutor.logic.EstadoVozDSP
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos

@Composable
fun ContenedorBiofeedback(navController: NavController, onBack: () -> Unit) {
    val motorDSP = remember { MotorAudioDSP() }
    DisposableEffect(Unit) {
        motorDSP.iniciarAnalisisDSP()
        onDispose { motorDSP.detenerAnalisis(null) }
    }
    BiofeedbackScreen(navController = navController, onBack = onBack, motorDSP = motorDSP)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiofeedbackScreen(navController: NavController, onBack: () -> Unit, motorDSP: MotorAudioDSP) {
    val metricasDSP by motorDSP.metricasAcusticas.collectAsState()
    var tabSeleccionada by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biofeedback y Relajación") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(ArrowLeftCircleIcon, "Regresar") } }
            )
        }
    ) { innerPadding ->
        // Contenedor principal con centrado estricto vertical y horizontal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            ScrollableTabRow(
                selectedTabIndex = tabSeleccionada,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = tabSeleccionada == 0,
                    onClick = { tabSeleccionada = 0 },
                    text = { Text("Koeppen", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = tabSeleccionada == 1,
                    onClick = { tabSeleccionada = 1 },
                    text = { Text("Soplido", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = tabSeleccionada == 2,
                    onClick = { tabSeleccionada = 2 },
                    text = { Text("Pluma", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            val ctx = LocalContext.current

            // Caja flexible centrada para que el contenido NUNCA se desplace hacia arriba o los lados
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (tabSeleccionada) {
                    0 -> EjercicioKoeppen(metricasDSP, ctx)
                    1 -> EjercicioSoplidoSostenido(metricasDSP, ctx)
                    2 -> EjercicioToqueDePluma(metricasDSP, ctx)
                }
            }
        }
    }
}

@Composable
fun EjercicioKoeppen(metricasDSP: ParametrosAcusticos, context: Context) {
    var fase by remember { mutableStateOf("INHALAR") }
    var ciclosCompletos by remember { mutableStateOf(0) }
    var erroresTension by remember { mutableStateOf(0) }

    val transition = rememberInfiniteTransition(label = "koppen")
    val escala by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "escala"
    )

    LaunchedEffect(escala) {
        val nuevaFase = if (escala < 1.0f) "INHALAR" else "EXHALAR"
        if (fase == "EXHALAR" && nuevaFase == "INHALAR") ciclosCompletos++
        fase = nuevaFase
    }

    // Koeppen: detecta si emites sonido/tensión indebida en fase de exhalación/relajación
    val rmsSensible = metricasDSP.rmsEnergia * 3.0f
    LaunchedEffect(rmsSensible) {
        if (fase == "EXHALAR" && rmsSensible > 500f) {
            erroresTension++
        }
    }

    val colorCirculo by animateColorAsState(
        targetValue = when (fase) {
            "INHALAR" -> Color(0xFF0277BD)
            else -> Color(0xFF388E3C)
        },
        label = "color"
    )

    // Columna estricta centrada
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (fase == "INHALAR") "Inhala profundamente..." else "Exhala con total relajación...",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorCirculo,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Ciclos limpios: $ciclosCompletos | Tensiones: $erroresTension", fontSize = 14.sp, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(30.dp))
        Box(
            modifier = Modifier
                .size((160 * escala).dp)
                .clip(CircleShape)
                .background(colorCirculo),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fase,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = {
                val total = ciclosCompletos + erroresTension
                ProgresoStorage.guardarSesion(context, "Koeppen", ciclosCompletos, erroresTension, if (total > 0) (ciclosCompletos * 100) / total else 0)
            },
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Text("Guardar Progreso")
        }
    }
}

@Composable
fun EjercicioSoplidoSostenido(metricasDSP: ParametrosAcusticos, context: Context) {
    var tiempoSostenido by remember { mutableStateOf(0f) }
    var exitosTotales by remember { mutableStateOf(0) }
    var fallosFuerza by remember { mutableStateOf(0) }

    // Soplido: Detecta el flujo constante de aire por proximidad al micrófono físico
    val rmsSoplido = metricasDSP.rmsEnergia * 4.5f
    val detectandoSoplido = rmsSoplido in 250f..35000f

    LaunchedEffect(rmsSoplido) {
        if (detectandoSoplido) {
            tiempoSostenido += 0.1f
            if (tiempoSostenido >= 50f) {
                exitosTotales++
                tiempoSostenido = 0f
            }
        } else if (rmsSoplido > 35000f) {
            fallosFuerza++
            tiempoSostenido = 0f
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💨 Sopla de manera continua hacia el micrófono", fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Logros: $exitosTotales | Cortes: $fallosFuerza", fontSize = 14.sp, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(30.dp))
        LinearProgressIndicator(
            progress = { (tiempoSostenido / 50f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(0.75f).height(25.dp),
            color = Color(0xFF0277BD)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text("${(tiempoSostenido / 10f).toInt()} s / 5.0 s", fontWeight = FontWeight.Bold, color = Color(0xFF0277BD), textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = {
                val total = exitosTotales + fallosFuerza
                ProgresoStorage.guardarSesion(context, "Soplido", exitosTotales, fallosFuerza, if (total > 0) (exitosTotales * 100) / total else 0)
            },
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Text("Guardar Progreso")
        }
    }
}

@Composable
fun EjercicioToqueDePluma(metricasDSP: ParametrosAcusticos, context: Context) {
    val listaPalabras = listOf("Perro", "Barco", "Camino", "Tigre", "Puerta")
    var indice by remember { mutableStateOf(0) }
    var aciertosLimpios by remember { mutableStateOf(0) }
    var bloqueosDuros by remember { mutableStateOf(0) }

    // Pluma: Responde a la emisión de voz y ataques bruscos al pronunciar
    val vozActiva = metricasDSP.rmsEnergia * 3.0f > 400f
    val esTension = metricasDSP.ataqueBrusco > 0.15f || metricasDSP.estado == EstadoVozDSP.BLOQUEO

    LaunchedEffect(esTension) {
        if (esTension) {
            bloqueosDuros++
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
            delay(1500)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Pronuncia la palabra suavemente", fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.size(240.dp, 150.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (esTension) Color(0xFFD32F2F) else if (vozActiva) Color(0xFF81C784) else Color(0xFFE0F2F1))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = listaPalabras[indice],
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (esTension || vozActiva) Color.White else Color(0xFF004D40),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (esTension) "⚠️ ¡Tensión detectada!" else if (vozActiva) "🗣️ ¡Voz detectada!" else "✨ Esperando fonación suave...",
            color = if (esTension) Color.Red else if (vozActiva) Color(0xFF2E7D32) else Color.Gray,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))
        Button(
            onClick = {
                aciertosLimpios++
                indice = (indice + 1) % listaPalabras.size
            },
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Text("Siguiente Palabra")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                val total = aciertosLimpios + bloqueosDuros
                ProgresoStorage.guardarSesion(context, "Toque Pluma", aciertosLimpios, bloqueosDuros, if (total > 0) (aciertosLimpios * 100) / total else 0)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Text("Guardar Progreso")
        }
    }
}