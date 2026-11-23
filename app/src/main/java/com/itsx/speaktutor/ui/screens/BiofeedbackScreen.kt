package com.itsx.speaktutor.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.itsx.speaktutor.ui.components.HomeIcon
import com.itsx.speaktutor.ui.components.MonitorFonologicoYCAA
import com.itsx.speaktutor.ui.navigation.Screen

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
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(imageVector = HomeIcon, contentDescription = "Menú Principal", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Regresar al Menú Principal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                onNavigateProgreso = { navController.navigate(Screen.Progreso.route) },
                onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) },
                onNavigateBio = { /* Ya estás aquí */ },
                onNavigateTarjetasArticulacion = { navController.navigate(Screen.TarjetasArticulacion.route) },
                onNavigateConcienciaFonologica = { navController.navigate(Screen.ConcienciaFonologica.route) },
                onNavigateReporteMultidimensional = { navController.navigate(Screen.ReporteMultidimensional.route) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            ScrollableTabRow(selectedTabIndex = tabSeleccionada, edgePadding = 0.dp, modifier = Modifier.fillMaxWidth()) {
                Tab(selected = tabSeleccionada == 0, onClick = { tabSeleccionada = 0 }, text = { Text("Koeppen", fontWeight = FontWeight.Bold) })
                Tab(selected = tabSeleccionada == 1, onClick = { tabSeleccionada = 1 }, text = { Text("Soplido", fontWeight = FontWeight.Bold) })
                Tab(selected = tabSeleccionada == 2, onClick = { tabSeleccionada = 2 }, text = { Text("Pluma", fontWeight = FontWeight.Bold) })
            }

            Spacer(modifier = Modifier.height(16.dp))
            val ctx = LocalContext.current

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (tabSeleccionada) {
                    0 -> EjercicioKoeppen(metricasDSP, ctx)
                    1 -> EjercicioSoplidoSostenido(metricasDSP, ctx)
                    2 -> EjercicioToqueDePluma(motorDSP, ctx)
                }
            }
        }
    }
}

@Composable
fun EjercicioKoeppen(metricasDSP: ParametrosAcusticos, context: Context) {
    var fase by remember { mutableStateOf("INHALAR") }
    var ciclosCompletos by remember { mutableIntStateOf(0) }
    var erroresTension by remember { mutableIntStateOf(0) }
    var enCooldownTension by remember { mutableStateOf(false) }

    val transition = rememberInfiniteTransition(label = "koppen")
    val escala by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "escala"
    )

    LaunchedEffect(escala) {
        val nuevaFase = if (escala < 1.0f) "INHALAR" else "EXHALAR"
        if (fase == "EXHALAR" && nuevaFase == "INHALAR") {
            ciclosCompletos++
        }
        fase = nuevaFase
    }

    val rmsSensible = metricasDSP.rmsEnergia * 3.0f
    LaunchedEffect(rmsSensible, fase) {
        if (fase == "EXHALAR" && rmsSensible > 650f && !enCooldownTension) {
            enCooldownTension = true
            erroresTension++
            delay(1200L)
            enCooldownTension = false
        }
    }

    val colorCirculo by animateColorAsState(
        targetValue = if (fase == "INHALAR") Color(0xFF0277BD) else Color(0xFF388E3C),
        label = "color"
    )

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
            Text(text = fase, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = {
                val total = ciclosCompletos + erroresTension
                if (total == 0) {
                    Toast.makeText(context, "Completa al menos un ciclo antes de guardar", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val calificacion = (ciclosCompletos * 100) / total
                ProgresoStorage.guardarSesionMultidimensional(context, "Koeppen", ciclosCompletos, erroresTension, calificacion)
                Toast.makeText(context, "Progreso de Koeppen guardado ($calificacion%)", Toast.LENGTH_SHORT).show()
                ciclosCompletos = 0
                erroresTension = 0
            },
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Text("Guardar Progreso")
        }
    }
}

@Composable
fun EjercicioSoplidoSostenido(metricasDSP: ParametrosAcusticos, context: Context) {
    var ticksSostenidos by remember { mutableIntStateOf(0) } // 50 ticks * 100ms = 5.0 segundos exactos
    var exitosTotales by remember { mutableIntStateOf(0) }
    var fallosFuerza by remember { mutableIntStateOf(0) }
    var enCooldownFallo by remember { mutableStateOf(false) }

    // Escala calibrada de 0 a 1000 para que la barra visual (20% azul / 60% verde / 20% rojo) coincida exactamente
    val nivelSoplido = (metricasDSP.rmsEnergia * 3.5f).coerceIn(0f, 1000f)
    val LIMITE_INFERIOR = 200f // 20% inferior de la barra (Azul: muy débil)
    val LIMITE_SUPERIOR = 800f // 80% superior de la barra (Rojo: exceso de fuerza)

    val enZonaVerde = nivelSoplido in LIMITE_INFERIOR..LIMITE_SUPERIOR
    val demasiadaFuerza = nivelSoplido > LIMITE_SUPERIOR

    val enZonaVerdeActual by rememberUpdatedState(enZonaVerde)
    val demasiadaFuerzaActual by rememberUpdatedState(demasiadaFuerza)

    // Bucle cronometrado estable cada 100ms independiente de la frecuencia de actualización del audio
    LaunchedEffect(Unit) {
        while (true) {
            delay(100L)
            if (demasiadaFuerzaActual) {
                if (!enCooldownFallo) {
                    enCooldownFallo = true
                    fallosFuerza++
                }
                ticksSostenidos = 0
            } else {
                enCooldownFallo = false
                if (enZonaVerdeActual) {
                    ticksSostenidos++
                    if (ticksSostenidos >= 50) {
                        exitosTotales++
                        ticksSostenidos = 0
                        vibrarDispositivo(context, 120L)
                    }
                } else {
                    // Si baja el aire sin pasarse de fuerza, descuenta gradualmente sin castigar con fallo
                    ticksSostenidos = (ticksSostenidos - 1).coerceAtLeast(0)
                }
            }
        }
    }

    val alturaMedidor by animateFloatAsState(
        targetValue = (nivelSoplido / 1000f).coerceIn(0f, 1f),
        animationSpec = tween(100),
        label = "altura_voz"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💨 Control Respiratorio", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
        Text("Mantén el aire en la ZONA VERDE por 5 segundos", fontSize = 14.sp, textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Zonas de referencia (20% roja arriba, 60% verde centro, 20% azul abajo)
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(0.2f).fillMaxWidth().background(Color.Red.copy(alpha = 0.22f)))
                    Box(modifier = Modifier.weight(0.6f).fillMaxWidth().background(Color.Green.copy(alpha = 0.25f)))
                    Box(modifier = Modifier.weight(0.2f).fillMaxWidth().background(Color.Blue.copy(alpha = 0.20f)))
                }
                // Indicador dinámico del soplido
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(alturaMedidor)
                        .background(
                            when {
                                demasiadaFuerza -> Color(0xFFD32F2F)
                                enZonaVerde -> Color(0xFF388E3C)
                                else -> Color(0xFF0277BD)
                            }
                        )
                )
            }

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (ticksSostenidos / 50f).coerceIn(0f, 1f) },
                    modifier = Modifier.size(120.dp),
                    color = Color(0xFF388E3C),
                    strokeWidth = 12.dp,
                    trackColor = Color(0xFFE0E0E0)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.1f", ticksSostenidos / 10f),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF388E3C)
                    )
                    Text("de 5.0 s", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when {
                demasiadaFuerza -> "⚠️ ¡Demasiada fuerza! Suaviza el soplido"
                enZonaVerde -> "✅ ¡Flujo estable! Mantén así..."
                nivelSoplido > 40f -> "⬆️ Sopla un poco más constante para entrar al verde"
                else -> "🌬️ Acerca el micrófono y sopla suavemente"
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                demasiadaFuerza -> Color(0xFFD32F2F)
                enZonaVerde -> Color(0xFF2E7D32)
                else -> Color.Gray
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Logros: $exitosTotales | Cortes por tensión: $fallosFuerza", fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                val total = exitosTotales + fallosFuerza
                if (total == 0) {
                    Toast.makeText(context, "Realiza al menos un intento antes de guardar", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val calificacion = (exitosTotales * 100) / total
                ProgresoStorage.guardarSesionMultidimensional(context, "Soplido", exitosTotales, fallosFuerza, calificacion)
                Toast.makeText(context, "Progreso de Soplido guardado ($calificacion%)", Toast.LENGTH_SHORT).show()
                exitosTotales = 0
                fallosFuerza = 0
                ticksSostenidos = 0
            },
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Text("Guardar Progreso")
        }
    }
}

@Composable
fun EjercicioToqueDePluma(motorDSP: MotorAudioDSP, context: Context) {
    val metricasDSP by motorDSP.metricasAcusticas.collectAsState()

    val listaPalabras = remember {
        listOf(
            "Perro", "Barco", "Camino", "Tigre", "Puerta",
            "Gato", "Fuego", "Cielo", "Tren", "Sol",
            "Agua", "Viento", "Nube", "Pájaro", "Tierra",
            "Mar", "Luna", "Flor", "Zapato", "Café",
            "Piedra", "Diente", "Bosque", "Sombra", "Pluma",
            "Beso", "Casa", "Dado", "Gota", "Juego"
        ).shuffled()
    }

    var indice by remember { mutableIntStateOf(0) }
    var aciertosLimpios by remember { mutableIntStateOf(0) }
    var bloqueosDuros by remember { mutableIntStateOf(0) }
    var alertaTensionActiva by remember { mutableStateOf(false) }

    val energiaVoz = metricasDSP.rmsEnergia * 3.0f
    val vozActiva = energiaVoz > 260f

    // Evita falsos positivos en consonantes suaves: exige volumen alto (>950) + ataque brusco (>0.92) o bloqueo real
    val tensionInstantanea = vozActiva && (
            (energiaVoz > 950f && metricasDSP.ataqueBrusco > 0.92f) ||
                    (energiaVoz > 700f && metricasDSP.estado == EstadoVozDSP.BLOQUEO_TONICO) ||
                    metricasDSP.estado == EstadoVozDSP.BLOQUEO_CLONICO
            )

    // Cooldown estable: cuando detecta tensión real, marca 1 solo fallo, vibra una vez y espera 1.5s antes de volver a evaluar
    LaunchedEffect(tensionInstantanea) {
        if (tensionInstantanea && !alertaTensionActiva) {
            alertaTensionActiva = true
            bloqueosDuros++
            vibrarDispositivo(context, 180L)
            delay(1500L)
            alertaTensionActiva = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Pronuncia la palabra con inicio suave", fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Limpios: $aciertosLimpios | Tensiones: $bloqueosDuros", fontSize = 13.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.size(240.dp, 100.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    alertaTensionActiva -> Color(0xFFD32F2F)
                    vozActiva -> Color(0xFF43A047)
                    else -> Color(0xFFE0F2F1)
                }
            )
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = listaPalabras[indice],
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (alertaTensionActiva || vozActiva) Color.White else Color(0xFF004D40),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        MonitorFonologicoYCAA(motorDSP = motorDSP, palabraObjetivoActual = listaPalabras[indice])

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                alertaTensionActiva -> "⚠️ ¡Inicio brusco detectado! Relaja y repite suave"
                vozActiva -> "🗣️ ¡Inicio suave correcto!"
                else -> "✨ Esperando fonación suave..."
            },
            color = when {
                alertaTensionActiva -> Color(0xFFD32F2F)
                vozActiva -> Color(0xFF2E7D32)
                else -> Color.Gray
            },
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (!alertaTensionActiva) {
                    aciertosLimpios++
                }
                alertaTensionActiva = false
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
                if (total == 0) {
                    Toast.makeText(context, "Practica al menos una palabra antes de guardar", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val calificacion = (aciertosLimpios * 100) / total
                ProgresoStorage.guardarSesionMultidimensional(context, "Toque Pluma", aciertosLimpios, bloqueosDuros, calificacion)
                Toast.makeText(context, "Progreso de Toque de Pluma guardado ($calificacion%)", Toast.LENGTH_SHORT).show()
                aciertosLimpios = 0
                bloqueosDuros = 0
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Text("Guardar Progreso")
        }
    }
}

private fun vibrarDispositivo(context: Context, duracionMs: Long) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(duracionMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(duracionMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duracionMs)
            }
        }
    } catch (_: Exception) {
        // Ignorar si el dispositivo no tiene motor de vibración
    }
}