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
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import java.util.Locale

import com.itsx.speaktutor.logic.GestorConcienciaFonologica
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.navigation.Screen

enum class FasePractica { DISCRIMINACION, SILABA, PALABRA, COMPLETADO }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TarjetasArticulacionScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var tarjetasPendientes by remember { mutableStateOf(GestorConcienciaFonologica.obtenerTarjetasPendientes(context)) }
    var indiceActual by remember { mutableIntStateOf(0) }
    var faseActual by remember { mutableStateOf(if (tarjetasPendientes.isNotEmpty()) FasePractica.DISCRIMINACION else FasePractica.COMPLETADO) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var feedbackEstado by remember { mutableStateOf("Toca el micrófono para evaluar") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    DisposableEffect(context) {
        tts = TextToSpeech(context) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale("es", "MX") }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        onDispose { tts?.shutdown(); speechRecognizer?.destroy() }
    }

    val tarjetaActual = tarjetasPendientes.getOrNull(indiceActual)

    fun iniciarEvaluacionSTT(objetivo: String) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) { isListening = true; feedbackEstado = "Escuchando..." }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(p0: Int) { isListening = false; feedbackEstado = "No pude escucharte bien." }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val textoDicho = matches?.firstOrNull()?.lowercase()?.trim() ?: ""
                val objetivoLimpio = objetivo.lowercase().trim()

                // Verificamos si alguna de las opciones devueltas contiene la palabra objetivo
                val aciertoEncontrado = matches?.any { it.lowercase().contains(objetivoLimpio) }

                if (aciertoEncontrado == true || textoDicho.contains(objetivoLimpio)) {
                    feedbackEstado = "¡Muy bien!"
                    if (faseActual == FasePractica.SILABA) {
                        faseActual = FasePractica.PALABRA
                    } else if (faseActual == FasePractica.PALABRA) {
                        tarjetaActual?.let { GestorConcienciaFonologica.marcarComoSuperado(context, it.first, it.second) }
                        if (indiceActual < tarjetasPendientes.size - 1) {
                            indiceActual++
                            faseActual = FasePractica.DISCRIMINACION
                        } else {
                            faseActual = FasePractica.COMPLETADO
                        }
                    }
                } else {
                    feedbackEstado = "Escuché '$textoDicho'. Intenta decir: '$objetivo'."
                }
            }

            override fun onPartialResults(p0: Bundle?) {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tarjetas de Articulación") }, navigationIcon = { IconButton(onClick = onBack) { Icon(ArrowLeftCircleIcon, "Regresar") } }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route)},
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                onNavigateProgreso = { navController.navigate(Screen.Progreso.route) },
                onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) },
                onNavigateBio = { navController.navigate(Screen.Biofeedback.route) },
                onNavigateTarjetasArticulacion = { /* Ya estás aquí */ }
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (faseActual == FasePractica.COMPLETADO) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(100.dp), tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.height(24.dp))
                Text("¡Todo Superado!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Text("No tienes grafemas problemáticos pendientes.", textAlign = TextAlign.Center, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)) { Text("Volver al Menú") }
                return@Scaffold
            }

            tarjetaActual?.let { (silaba, palabra) ->
                // ENCABEZADO CON BOTÓN DE ELIMINAR
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tarjeta ${indiceActual + 1} de ${tarjetasPendientes.size}", fontSize = 16.sp, color = Color.Gray)

                    IconButton(onClick = {
                        // 1. Lo borramos de la memoria interna
                        GestorConcienciaFonologica.marcarComoSuperado(context, silaba, palabra)

                        // 2. Lo quitamos de la lista actual en pantalla
                        val nuevaLista = tarjetasPendientes.toMutableList()
                        nuevaLista.removeAt(indiceActual)
                        tarjetasPendientes = nuevaLista

                        // 3. Revisamos si ya terminamos o pasamos a la siguiente
                        if (indiceActual >= tarjetasPendientes.size) {
                            faseActual = FasePractica.COMPLETADO
                        } else {
                            faseActual = FasePractica.DISCRIMINACION
                        }
                    }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Delete, "Eliminar tarjeta", tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // TARJETA ANIMADA
                Box(
                    modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFE3F2FD)).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = faseActual,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.8f)) togetherWith fadeOut(animationSpec = tween(400))
                        }, label = "tarjeta_anim"
                    ) { estadoFase ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            when (estadoFase) {
                                FasePractica.DISCRIMINACION -> {
                                    Text("Paso 1: Escucha la sílaba", fontSize = 18.sp, color = Color.Gray)
                                    Text(silaba, fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color(0xFF1565C0))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { tts?.speak(silaba, TextToSpeech.QUEUE_FLUSH, null, null); faseActual = FasePractica.SILABA },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                    ) { Icon(Icons.Default.PlayArrow, "Escuchar"); Spacer(modifier = Modifier.width(8.dp)); Text("Reproducir y Avanzar") }
                                }
                                FasePractica.SILABA -> {
                                    Text("Paso 2: Pronuncia la sílaba", fontSize = 18.sp, color = Color.Gray)
                                    Text(silaba, fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                                }
                                FasePractica.PALABRA -> {
                                    Text("Paso 3: Pronuncia la palabra", fontSize = 18.sp, color = Color.Gray)
                                    Text(palabra, fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFFC62828))
                                }
                                else -> {}
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (faseActual == FasePractica.SILABA || faseActual == FasePractica.PALABRA) {
                    Text(feedbackEstado, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(24.dp))
                    FloatingActionButton(
                        onClick = { if (isListening) speechRecognizer?.stopListening() else iniciarEvaluacionSTT(if (faseActual == FasePractica.SILABA) silaba else palabra) },
                        containerColor = if (isListening) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    ) { Icon(Icons.Default.Mic, "Hablar", tint = Color.White, modifier = Modifier.size(40.dp)) }
                }
            }
        }
    }
}