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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.itsx.speaktutor.logic.GestorConcienciaFonologica
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.HomeIcon
import com.itsx.speaktutor.ui.navigation.Screen

enum class FasePractica { DISCRIMINACION, SILABA, CUBOS, PALABRA, FRASE, COMPLETADO }

fun dividirPalabraEnSilabas(palabra: String): List<String> {
    val regex = "(?<=[aeiouáéíóú])(?=[b-df-hj-np-tv-z][aeiouáéíóú])|(?<=[aeiouáéíóú])(?=[b-df-hj-np-tv-z]{2}[aeiouáéíóú])".toRegex(RegexOption.IGNORE_CASE)
    return palabra.split(regex).map { it.trim() }.filter { it.isNotEmpty() }
}

// CORRECCIÓN: Frases neutrales para evitar errores de género (ej. "El flor")
fun generarFraseModelo(palabra: String): String {
    val frases = listOf(
        "La palabra $palabra es genial.",
        "Me gusta mucho decir $palabra.",
        "Hoy practicaré con $palabra."
    )
    return frases.random()
}

fun obtenerEmojiPara(palabra: String): String {
    return when (palabra.lowercase()) {
        "tractor" -> "🚜"; "tren" -> "🚆"; "brazo" -> "💪"; "fruta" -> "🍎"
        "plato" -> "🍽️"; "globo" -> "🎈"; "clavo" -> "🔨"; "flor" -> "🌸"
        else -> "💡"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TarjetasArticulacionScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var tarjetasPendientes by remember { mutableStateOf(GestorConcienciaFonologica.obtenerTarjetasPendientes(context)) }
    var indiceActual by remember { mutableIntStateOf(0) }
    var faseActual by remember { mutableStateOf(if (tarjetasPendientes.isNotEmpty()) FasePractica.DISCRIMINACION else FasePractica.COMPLETADO) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var feedbackEstado by remember { mutableStateOf("Toca el micrófono para evaluar") }

    var tensionDetectada by remember { mutableStateOf(false) }
    var nivelEnergiaAnterior by remember { mutableFloatStateOf(0f) }
    val offsetX = remember { Animatable(0f) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    DisposableEffect(context) {
        tts = TextToSpeech(context) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale("es", "MX") }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        onDispose { tts?.shutdown(); speechRecognizer?.destroy() }
    }

    val tarjetaActual = tarjetasPendientes.getOrNull(indiceActual)

    // CORRECCIÓN: Ahora la frase se regenera correctamente si la tarjetaActual cambia,
    // solucionando el bug donde se usaba la palabra de una tarjeta anterior eliminada.
    val fraseGenerada = remember(indiceActual, tarjetaActual) {
        tarjetaActual?.let { generarFraseModelo(it.second) } ?: ""
    }

    fun dispararAlertaContactoLigero() {
        if (!tensionDetectada) {
            tensionDetectada = true
            feedbackEstado = "¡Demasiada tensión! Intenta un inicio suave."
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            coroutineScope.launch {
                offsetX.animateTo(15f, tween(50))
                offsetX.animateTo(-15f, tween(50))
                offsetX.animateTo(10f, tween(50))
                offsetX.animateTo(-10f, tween(50))
                offsetX.animateTo(0f, tween(50))

                delay(2000)
                tensionDetectada = false
                if (isListening) feedbackEstado = "Escuchando... mantén la relajación."
            }
        }
    }

    fun iniciarEvaluacionSTT(objetivo: String) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) { isListening = true; feedbackEstado = "Escuchando..."; nivelEnergiaAnterior = 0f }
            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                if (nivelEnergiaAnterior > 2f) {
                    val saltoDeEnergia = rmsdB - nivelEnergiaAnterior
                    if (saltoDeEnergia > 15f && rmsdB > 10f) {
                        dispararAlertaContactoLigero()
                    }
                }
                nivelEnergiaAnterior = rmsdB
            }

            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(p0: Int) { isListening = false; feedbackEstado = "No pude escucharte bien." }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()

                // CORRECCIÓN: Filtramos comas y puntos usando Regex para que la comparación sea justa en oraciones (Paso 5)
                val textoDicho = matches.firstOrNull()?.lowercase()?.replace(Regex("[^a-záéíóúñ ]"), "")?.trim() ?: ""
                val objetivoLimpio = objetivo.lowercase().replace(Regex("[^a-záéíóúñ ]"), "").trim()

                if (textoDicho.isBlank()) {
                    feedbackEstado = "No escuché nada. Intenta hablar más cerca."
                    return
                }

                if (tensionDetectada) {
                    feedbackEstado = "Lo dijiste, pero hubo tensión. Repítelo suavemente."
                    if (faseActual == FasePractica.PALABRA || faseActual == FasePractica.FRASE) {
                        tarjetaActual?.let { (silabaError, palabraError) ->
                            ProgresoStorage.registrarFalloFonologico(context, silabaError, palabraError)
                        }
                    }
                    return
                }

                val aciertoEncontrado = matches.any { it.lowercase().replace(Regex("[^a-záéíóúñ ]"), "").contains(objetivoLimpio) }

                if (aciertoEncontrado || textoDicho.contains(objetivoLimpio)) {
                    feedbackEstado = "¡Muy bien!"
                    when (faseActual) {
                        FasePractica.SILABA -> faseActual = FasePractica.CUBOS
                        FasePractica.PALABRA -> faseActual = FasePractica.FRASE
                        FasePractica.FRASE -> {
                            tarjetaActual?.let { GestorConcienciaFonologica.marcarComoSuperado(context, it.first, it.second) }
                            if (indiceActual < tarjetasPendientes.size - 1) {
                                indiceActual++
                                faseActual = FasePractica.DISCRIMINACION
                            } else {
                                faseActual = FasePractica.COMPLETADO
                            }
                        }
                        else -> {}
                    }
                } else {
                    feedbackEstado = "Escuché '$textoDicho'. Intenta decir: '$objetivoLimpio'."

                    if (textoDicho.length > 2 && (faseActual == FasePractica.PALABRA || faseActual == FasePractica.FRASE)) {
                        tarjetaActual?.let { (silabaError, palabraError) ->
                            ProgresoStorage.registrarFalloFonologico(context, silabaError, palabraError)
                        }
                    }
                }
            }

            override fun onPartialResults(p0: Bundle?) {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tarjetas de Articulación") }, navigationIcon = { IconButton(onClick = onBack) { Icon(ArrowLeftCircleIcon, "Regresar") } }) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = HomeIcon,
                        contentDescription = "Menú Principal",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Regresar al Menú Principal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tarjeta ${indiceActual + 1} de ${tarjetasPendientes.size}", fontSize = 16.sp, color = Color.Gray)
                    IconButton(onClick = {
                        GestorConcienciaFonologica.marcarComoSuperado(context, silaba, palabra)
                        val nuevaLista = tarjetasPendientes.toMutableList().apply { removeAt(indiceActual) }
                        tarjetasPendientes = nuevaLista
                        faseActual = if (indiceActual >= tarjetasPendientes.size) FasePractica.COMPLETADO else FasePractica.DISCRIMINACION
                    }) {
                        Icon(Icons.Default.Delete, "Eliminar tarjeta", tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .offset(x = offsetX.value.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (tensionDetectada) Color(0xFFFFCDD2) else Color(0xFFE3F2FD))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = faseActual,
                        transitionSpec = { (fadeIn(tween(400)) + scaleIn(initialScale = 0.8f)) togetherWith fadeOut(tween(400)) },
                        label = "tarjeta_anim"
                    ) { estadoFase ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            when (estadoFase) {
                                FasePractica.DISCRIMINACION -> {
                                    Text("Paso 1: Escucha la sílaba objetivo", fontSize = 16.sp, color = Color.Gray)
                                    Text(silaba.uppercase(), fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color(0xFF1565C0))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { tts?.speak(silaba, TextToSpeech.QUEUE_FLUSH, null, null); faseActual = FasePractica.SILABA },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                    ) { Icon(Icons.Default.PlayArrow, null); Spacer(modifier = Modifier.width(8.dp)); Text("Avanzar") }
                                }
                                FasePractica.SILABA -> {
                                    Text("Paso 2: Pronuncia la sílaba aislada", fontSize = 16.sp, color = Color.Gray)
                                    Text(silaba.uppercase(), fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                                }
                                FasePractica.CUBOS -> {
                                    Text("Paso 3: Construcción Sensorial", fontSize = 16.sp, color = Color.Gray)
                                    Text("Toca cada cubo para escucharlo", fontSize = 12.sp, color = Color(0xFF1565C0))
                                    Spacer(modifier = Modifier.height(24.dp))

                                    val bloques = dividirPalabraEnSilabas(palabra)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                                    ) {
                                        bloques.forEach { bloque ->
                                            val esSilabaTrabada = bloque.contains(silaba, ignoreCase = true)
                                            Card(
                                                modifier = Modifier.size(70.dp).clickable { tts?.speak(bloque, TextToSpeech.QUEUE_FLUSH, null, null) },
                                                colors = CardDefaults.cardColors(containerColor = if (esSilabaTrabada) Color(0xFFFFCDD2) else Color.White),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Text(bloque.uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (esSilabaTrabada) Color(0xFFC62828) else Color(0xFF1565C0))
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    OutlinedButton(onClick = { faseActual = FasePractica.PALABRA }) { Text("Ya lo practiqué") }
                                }
                                FasePractica.PALABRA -> {
                                    Text("Paso 4: Articula la palabra completa", fontSize = 16.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(text = obtenerEmojiPara(palabra), fontSize = 60.sp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val startIndex = palabra.indexOf(silaba, ignoreCase = true)
                                    if (startIndex != -1) {
                                        val endIndex = startIndex + silaba.length
                                        val textoColoreado = buildAnnotatedString {
                                            withStyle(SpanStyle(color = Color(0xFF1565C0))) { append(palabra.substring(0, startIndex)) }
                                            withStyle(SpanStyle(color = Color(0xFFD32F2F))) { append(palabra.substring(startIndex, endIndex)) }
                                            withStyle(SpanStyle(color = Color(0xFF1565C0))) { append(palabra.substring(endIndex)) }
                                        }
                                        Text(text = textoColoreado, fontSize = 42.sp, fontWeight = FontWeight.Black)
                                    } else {
                                        Text(palabra, fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color(0xFF1565C0))
                                    }
                                }
                                FasePractica.FRASE -> {
                                    Text("Paso 5: Lectura en Frase", fontSize = 16.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = fraseGenerada,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1565C0),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Usa el inicio suave en todo el texto.", fontSize = 14.sp, color = Color(0xFF2E7D32))
                                }
                                else -> {}
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (faseActual == FasePractica.SILABA || faseActual == FasePractica.PALABRA || faseActual == FasePractica.FRASE) {
                    Text(
                        text = feedbackEstado,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (tensionDetectada) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    val objetivoActual = when (faseActual) {
                        FasePractica.SILABA -> silaba
                        FasePractica.PALABRA -> palabra
                        FasePractica.FRASE -> fraseGenerada
                        else -> ""
                    }

                    FloatingActionButton(
                        onClick = { if (isListening) speechRecognizer?.stopListening() else iniciarEvaluacionSTT(objetivoActual) },
                        containerColor = if (tensionDetectada) Color.Red else (if (isListening) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(80.dp)
                    ) { Icon(Icons.Default.Mic, "Hablar", tint = Color.White, modifier = Modifier.size(40.dp)) }
                }
            }
        }
    }
}