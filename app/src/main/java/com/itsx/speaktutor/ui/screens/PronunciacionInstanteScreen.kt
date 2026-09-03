package com.itsx.speaktutor.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.navigation.Screen
import kotlinx.coroutines.delay
import java.util.*

enum class DificultadPronunciacion(val titulo: String, val segundos: Int, val color: Color) {
    FACIL("Fácil", 150, Color(0xFF2E7D32)),
    MEDIO("Medio", 90, Color(0xFFEF6C00)),
    DIFICIL("Difícil", 45, Color(0xFFC62828))
}

enum class EstadoPronunciacion {
    SELECCION_DIFICULTAD, JUGANDO, RESULTADOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PronunciacionInstanteScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var estadoActual by remember { mutableStateOf(EstadoPronunciacion.SELECCION_DIFICULTAD) }
    var dificultadSeleccionada by remember { mutableStateOf(DificultadPronunciacion.FACIL) }

    val palabrasFaciles = listOf("Casa", "Sol", "Agua", "Mesa", "Flor")
    val palabrasMedias = listOf("Caminar", "Espejo", "Ventana", "Musica", "Viaje")
    val palabrasDificiles = listOf("Ferrocarril", "Constitución", "Paralelepípedo", "Estetoscopio", "Murciélago")

    var listaPalabrasActual by remember { mutableStateOf(palabrasFaciles) }
    var indicePalabra by remember { mutableIntStateOf(0) }
    var tiempoRestante by remember { mutableIntStateOf(150) }

    // Estadísticas
    var aciertos by remember { mutableIntStateOf(0) }
    var errores by remember { mutableIntStateOf(0) }

    // Control de voz e interfaz
    var estadoVozTexto by remember { mutableStateOf("Presiona el micrófono y habla") }
    var isListening by remember { mutableStateOf(false) }

    // TTS y SpeechRecognizer
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            estadoVozTexto = "Se requiere permiso de micrófono para evaluar la voz."
        }
    }

    // Configuración y limpieza segura de recursos
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.Builder().setLanguage("es").setRegion("ES").build()
                ttsInitialized = true
            }
        }
        tts = textToSpeech

        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
            try {
                speechRecognizer.destroy()
            } catch (e: Exception) {
                // Excepción controlada de cierre
            }
        }
    }

    // Temporizador principal de cuenta regresiva
    LaunchedEffect(estadoActual, tiempoRestante) {
        if (estadoActual == EstadoPronunciacion.JUGANDO) {
            if (tiempoRestante > 0) {
                delay(1000L)
                tiempoRestante--
            } else {
                try {
                    speechRecognizer.stopListening()
                } catch (e: Exception) {
                    // Ignorar si ya estaba cerrado
                }
                estadoActual = EstadoPronunciacion.RESULTADOS
            }
        }
    }

    // Función para iniciar el reconocimiento de voz
    fun iniciarReconocimientoVoz(palabraEsperada: String) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Pronuncia: $palabraEsperada")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                estadoVozTexto = "Escuchando atentamente..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
                estadoVozTexto = "No se distinguió con claridad. Intenta de nuevo."
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val palabraDicha = matches[0].trim().lowercase()
                    val objetivo = palabraEsperada.trim().lowercase()

                    if (palabraDicha.contains(objetivo) || objetivo.contains(palabraDicha)) {
                        aciertos++
                        estadoVozTexto = "¡Excelente! Dijiste: \"${matches[0]}\""
                    } else {
                        errores++
                        estadoVozTexto = "Diste: \"${matches[0]}\" (Esperado: $palabraEsperada)"
                    }
                    indicePalabra++
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            estadoVozTexto = "Error al iniciar el micrófono. Inténtalo otra vez."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (estadoActual) {
                            EstadoPronunciacion.SELECCION_DIFICULTAD -> "Pronunciación al Instante"
                            EstadoPronunciacion.JUGANDO -> "Práctica (${dificultadSeleccionada.titulo})"
                            EstadoPronunciacion.RESULTADOS -> "Puntuación Final"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (estadoActual == EstadoPronunciacion.SELECCION_DIFICULTAD) {
                            onBack()
                        } else {
                            estadoActual = EstadoPronunciacion.SELECCION_DIFICULTAD
                        }
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Barra de navegación rápida superior entre módulos
            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { /* Ya estás aquí */ },
                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                onNavigateProgreso = { navController.navigate(Screen.Progreso.route) }
            )

            when (estadoActual) {
                EstadoPronunciacion.SELECCION_DIFICULTAD -> {
                    Text(text = "Selecciona el nivel de dificultad:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    DificultadPronunciacion.entries.forEach { dif ->
                        Button(
                            onClick = {
                                dificultadSeleccionada = dif
                                tiempoRestante = dif.segundos
                                listaPalabrasActual = when (dif) {
                                    DificultadPronunciacion.FACIL -> palabrasFaciles
                                    DificultadPronunciacion.MEDIO -> palabrasMedias
                                    DificultadPronunciacion.DIFICIL -> palabrasDificiles
                                }
                                indicePalabra = 0
                                aciertos = 0
                                errores = 0
                                estadoVozTexto = "Presiona el micrófono y habla"
                                estadoActual = EstadoPronunciacion.JUGANDO
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .padding(vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = dif.color)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = dif.titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(text = "${dif.segundos} s", fontSize = 14.sp)
                            }
                        }
                    }
                }

                EstadoPronunciacion.JUGANDO -> {
                    val palabraActual = listaPalabrasActual[indicePalabra % listaPalabrasActual.size]

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "⏱️ Tiempo Restante", fontSize = 14.sp)
                            Text(text = "$tiempoRestante s", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Pronuncia la palabra:", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = palabraActual, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (ttsInitialized) {
                                        tts?.speak(palabraActual, TextToSpeech.QUEUE_FLUSH, null, null)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f))
                            ) {
                                Text("🔊 Escuchar Modelo", color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (!isListening) {
                                iniciarReconocimientoVoz(palabraActual)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) Color(0xFFC62828) else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isListening) "Escuchando voz..." else "🎤 Hablar y Evaluar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } // <-- LLAVE CERRADA CORRECTAMENTE AQUÍ

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = estadoVozTexto,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Aciertos: $aciertos | Errores: $errores", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                EstadoPronunciacion.RESULTADOS -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "🎯 ¡Puntuación Final!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Nivel superado: ${dificultadSeleccionada.titulo}", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "Evaluación Automática de Voz", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                HorizontalDivider()
                                Text(text = "✅ Aciertos correctos: $aciertos", fontSize = 16.sp, color = Color(0xFF2E7D32))
                                Text(text = "❌ Errores detectados: $errores", fontSize = 16.sp, color = Color(0xFFC62828))

                                val totalIntentos = aciertos + errores
                                val calificacionFinal = if (totalIntentos > 0) (aciertos * 100) / totalIntentos else 0

                                // Llama a esto para registrar la sesión en el almacenamiento local:
                                ProgresoStorage.guardarSesion(
                                    context = context,
                                    dificultad = dificultadSeleccionada.titulo,
                                    aciertos = aciertos,
                                    errores = errores,
                                    calificacion = calificacionFinal
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "⭐ Calificación: $calificacionFinal / 100 pts", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { estadoActual = EstadoPronunciacion.SELECCION_DIFICULTAD },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(text = "Intentar Nuevamente", fontSize = 16.sp)
                        }

                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(text = "Volver al Menú Principal", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}