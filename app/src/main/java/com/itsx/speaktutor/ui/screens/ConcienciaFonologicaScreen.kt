package com.itsx.speaktutor.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.itsx.speaktutor.logic.EjercicioConciencia
import com.itsx.speaktutor.logic.GestorEjercicios
import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.ControlesAAF
import com.itsx.speaktutor.ui.components.HomeIcon
import com.itsx.speaktutor.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcienciaFonologicaScreen(navController: NavController, motorDSP: MotorAudioDSP, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val metricas by motorDSP.metricasAcusticas.collectAsState()

    var grabandoSTT by remember { mutableStateOf(false) }
    var modoDSPActivo by remember { mutableStateOf(false) }
    var evaluacion by remember { mutableStateOf("") }
    var colorEvaluacion by remember { mutableStateOf(Color.White) }

    var todosLosEjercicios by remember { mutableStateOf<List<EjercicioConciencia>>(emptyList()) }
    var ejerciciosDelNivel by remember { mutableStateOf<List<EjercicioConciencia>>(emptyList()) }
    var nivelSeleccionado by remember { mutableIntStateOf(1) }
    var indiceActual by remember { mutableIntStateOf(0) }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    LaunchedEffect(Unit) {
        todosLosEjercicios = GestorEjercicios.obtenerEjerciciosConciencia(context, "ConcienciaFonologica/Ejercicios.json")
        ejerciciosDelNivel = todosLosEjercicios.filter { it.nivel == nivelSeleccionado }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            motorDSP.detenerAnalisis(null)
        }
    }

    val ejercicioActual = ejerciciosDelNivel.getOrNull(indiceActual)
    val palabraObjetivo = ejercicioActual?.palabraVisual ?: "Cargando..."
    val fonemaFaltante = ejercicioActual?.fonemaFaltante ?: ""

    // 🚀 MEJORA: Todo se pasa directamente a MAYÚSCULAS y solo se permiten letras de la A a la Z
    fun limpiarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto.uppercase().trim(), Normalizer.Form.NFD)
        return normalizado.replace(Regex("\\p{Mn}+"), "").replace(Regex("[^A-Z]"), "")
    }

    fun finalizarValidacion(matches: List<String>?) {
        grabandoSTT = false

        val palabraLimpia = limpiarTexto(palabraObjetivo)
        val fonemaLimpio = limpiarTexto(fonemaFaltante)

        // DICCIONARIO EN MAYÚSCULAS
        val variacionesFonema = when (fonemaLimpio) {
            "A" -> listOf("A", "AH", "HA")
            "E" -> listOf("E", "EH", "HE")
            "I" -> listOf("I", "HI", "Y")
            "O" -> listOf("O", "OH", "HO")
            "U" -> listOf("U", "UH", "HU")
            "S" -> listOf("S", "ES", "CE", "ZE", "SSS", "SE")
            "M" -> listOf("M", "EME", "MMM", "ME")
            "R" -> listOf("R", "ERRE", "RRR", "RE")
            else -> listOf(fonemaLimpio)
        }

        val aciertoTexto = matches?.any { texto ->
            val t = limpiarTexto(texto)
            val coincidePalabra = t == palabraLimpia || t.contains(palabraLimpia)
            val coincideFonemaExacto = variacionesFonema.any { v -> t == v }
            val empiezaConFonema = fonemaLimpio.isNotEmpty() && t.startsWith(fonemaLimpio)

            coincidePalabra || coincideFonemaExacto || empiezaConFonema
        } == true

        val dichoPrimero = matches?.firstOrNull() ?: "nada detectado"

        // --- NUEVO: Calcular métricas y guardar ---
        val calificacion = if (aciertoTexto) 100 else 0
        val aciertos = if (aciertoTexto) 1 else 0
        val errores = if (aciertoTexto) 0 else 1

        ProgresoStorage.guardarSesionMultidimensional(
            context = context,
            modulo = "Conciencia Fonológica (/$fonemaLimpio/)",
            aciertos = aciertos,
            erroresTotales = errores,
            calificacion = calificacion
        )

        if (aciertoTexto) {
            evaluacion = "¡Excelente! Sonido correcto: \"$dichoPrimero\""
            colorEvaluacion = Color(0xFFB2FF59)
        } else {
            evaluacion = "Escuché \"$dichoPrimero\". Intenta decir: $palabraObjetivo"
            colorEvaluacion = Color.Yellow
        }
    }

    fun detenerEscuchaManual() {
        speechRecognizer?.stopListening()
        grabandoSTT = false
    }

    fun iniciarEscuchaFonologica() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (modoDSPActivo) {
            motorDSP.detenerAnalisis(null)
            modoDSPActivo = false
        }

        grabandoSTT = true
        evaluacion = "Escuchando /$fonemaFaltante/..."
        colorEvaluacion = Color.White

        var textoParcial = ""

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                scope.launch {
                    delay(200)
                    if (textoParcial.isNotBlank()) {
                        finalizarValidacion(listOf(textoParcial))
                    } else {
                        grabandoSTT = false
                        evaluacion = when(error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Error: Micrófono ocupado."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No detectado. Intenta decir la palabra completa: $palabraObjetivo"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo agotado sin hablar."
                            13 -> "Error: Paquete de idioma sin conexión no instalado."
                            else -> "Error de reconocimiento ($error)"
                        }
                        colorEvaluacion = Color.Yellow
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    textoParcial = matches[0]
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                finalizarValidacion(matches ?: if(textoParcial.isNotBlank()) listOf(textoParcial) else emptyList())
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conciencia Fonológica") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(ArrowLeftCircleIcon, "Regresar") } }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = { navController.navigate(Screen.TarjetasShader.route) { popUpTo(0) } },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(imageVector = HomeIcon, contentDescription = "Menú Principal", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regresar al Menú Principal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                onNavigateBio = { navController.navigate(Screen.Biofeedback.route) },
                onNavigateTarjetasArticulacion = { navController.navigate(Screen.TarjetasArticulacion.route) },
                onNavigateConcienciaFonologica = { /* Ya estás aquí */ },
                onNavigateReporteMultidimensional = { navController.navigate(Screen.ReporteMultidimensional.route) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val nombresNiveles = listOf("1 (Fácil)", "2 (Medio)", "3 (Difícil)")
            TabRow(
                selectedTabIndex = nivelSeleccionado - 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                nombresNiveles.forEachIndexed { index, titulo ->
                    Tab(
                        selected = (nivelSeleccionado - 1) == index,
                        onClick = {
                            if (!grabandoSTT && !modoDSPActivo) {
                                nivelSeleccionado = index + 1
                                ejerciciosDelNivel = todosLosEjercicios.filter { it.nivel == nivelSeleccionado }
                                indiceActual = 0
                                evaluacion = ""
                            }
                        },
                        text = { Text(text = titulo, fontWeight = if ((nivelSeleccionado - 1) == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFFE65100), Color(0xFFEF6C00)))).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (ejerciciosDelNivel.isEmpty()) {
                    Text("Cargando nivel...", color = Color.White)
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Completa la palabra", fontSize = 20.sp, color = Color.White.copy(alpha = 0.8f))
                            IconButton(onClick = {
                                if (!grabandoSTT && !modoDSPActivo) {
                                    ejerciciosDelNivel = ejerciciosDelNivel.shuffled()
                                    indiceActual = 0
                                    evaluacion = ""
                                }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Mezclar Palabras", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = palabraObjetivo,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 8.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Fonema clave: /$fonemaFaltante/",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = {
                                    if (!grabandoSTT) iniciarEscuchaFonologica() else detenerEscuchaManual()
                                },
                                modifier = Modifier.size(80.dp),
                                shape = RoundedCornerShape(50),
                                enabled = !modoDSPActivo,
                                colors = ButtonDefaults.buttonColors(containerColor = if (grabandoSTT) Color(0xFFFF5252) else Color(0xFF43A047))
                            ) {
                                Icon(
                                    imageVector = if (grabandoSTT) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Evaluar STT",
                                    modifier = Modifier.size(36.dp),
                                    tint = Color.White
                                )
                            }

                            Button(
                                onClick = {
                                    if (modoDSPActivo) {
                                        motorDSP.detenerAnalisis(null)
                                        modoDSPActivo = false
                                        evaluacion = ""
                                    } else {
                                        motorDSP.iniciarAnalisisDSP()
                                        modoDSPActivo = true
                                        evaluacion = "Práctica libre con DSP activada"
                                        colorEvaluacion = Color(0xFF81D4FA)
                                    }
                                },
                                modifier = Modifier.size(80.dp),
                                shape = RoundedCornerShape(50),
                                enabled = !grabandoSTT,
                                colors = ButtonDefaults.buttonColors(containerColor = if (modoDSPActivo) Color(0xFFFF5252) else Color(0xFF0288D1))
                            ) {
                                Icon(
                                    imageVector = if (modoDSPActivo) Icons.Default.Stop else Icons.Default.GraphicEq,
                                    contentDescription = "Práctica DSP",
                                    modifier = Modifier.size(36.dp),
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        AnimatedVisibility(visible = evaluacion.isNotEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = evaluacion,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorEvaluacion,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                if (!grabandoSTT && !modoDSPActivo) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            if (indiceActual < ejerciciosDelNivel.size - 1) {
                                                indiceActual++
                                            } else {
                                                indiceActual = 0
                                            }
                                            evaluacion = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFE65100))
                                    ) { Text("Siguiente Palabra ➔", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (modoDSPActivo) {
                            ControlesAAF(motorDSP = motorDSP)
                            com.itsx.speaktutor.ui.components.BiofeedbackVisualAvanzadoDSP(metricas)
                        }
                    }
                }
            }
        }
    }
}