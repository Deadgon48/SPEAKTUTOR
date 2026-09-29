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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
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
import com.itsx.speaktutor.logic.CategoriaAdaptativa
import com.itsx.speaktutor.logic.EjercicioGenerado
import com.itsx.speaktutor.logic.MotorAdaptativo
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.MetronomoReutilizableComponent
import com.itsx.speaktutor.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.util.Locale

// *** INICIO IMPORTACIONES DSP ***
import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BiofeedbackVisualAvanzadoDSP
import com.itsx.speaktutor.ui.components.HomeIcon

// *** FIN IMPORTACIONES DSP ***

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjerciciosAdaptativosScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val consejoContextual = remember { MotorAdaptativo.generarConsejoContextual(context) }
    val historialEjercicios = remember { mutableStateListOf(MotorAdaptativo.generarEjercicioNuevo(context)) }

    // TTS para las Simulaciones Interactivas
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListeningId by remember { mutableStateOf<String?>(null) }
    var textoEscuchado by remember { mutableStateOf("") }
    var calificacionObtenida by remember { mutableStateOf(-1) }

    // *** INICIO ESTADOS DSP ***
    val motorDSP = remember { MotorAudioDSP() }
    val metricasDSP by motorDSP.metricasAcusticas.collectAsState()
    var isDSPActiveId by remember { mutableStateOf<String?>(null) }
    // *** FIN ESTADOS DSP ***

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                ttsInitialized = true
            }
        }
        tts = textToSpeech

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        onDispose {
            recognizer.destroy()
            textToSpeech.stop()
            textToSpeech.shutdown()

            // *** INICIO LIMPIEZA DSP ***
            motorDSP.detenerAnalisis(null)
            // *** FIN LIMPIEZA DSP ***
        }
    }

    fun iniciarEvaluacion(ejercicio: EjercicioGenerado) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (isListeningId == ejercicio.id) {
            speechRecognizer?.stopListening()
            isListeningId = null
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListeningId = ejercicio.id; textoEscuchado = "" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListeningId = null }
            override fun onError(error: Int) { isListeningId = null }

            override fun onResults(results: Bundle?) {
                isListeningId = null
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val textoPronunciado = matches?.get(0) ?: ""
                textoEscuchado = textoPronunciado

                calificacionObtenida = if (textoPronunciado.isNotBlank()) 85 else 0

                ProgresoStorage.guardarSesion(
                    context = context,
                    dificultad = "IA (${ejercicio.categoria.name})",
                    aciertos = if (calificacionObtenida >= 60) 1 else 0,
                    errores = if (calificacionObtenida < 60) 1 else 0,
                    calificacion = calificacionObtenida
                )
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laboratorio de Adaptación IA") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isListeningId != null) speechRecognizer?.stopListening()
                        motorDSP.detenerAnalisis(null)
                        onBack()
                    }) {
                        Icon(imageVector = ArrowLeftCircleIcon, contentDescription = "Regresar", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    historialEjercicios.add(MotorAdaptativo.generarEjercicioNuevo(context))
                    calificacionObtenida = -1
                    coroutineScope.launch {
                        listState.animateScrollToItem(historialEjercicios.size)
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Nuevo") },
                text = { Text("Generar Nuevo Ejercicio") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        },
        bottomBar = {
            // Este contenedor anclará tu botón perfectamente a la parte inferior
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                onNavigateProgreso = { navController.navigate(Screen.Progreso.route) },
                onNavigateEjerciciosAdaptativos = { /* Ya estás aquí */ }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Psychology, contentDescription = "IA", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Análisis Clínico Longitudinal", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider()
                            Text(text = consejoContextual, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                items(historialEjercicios) { ejercicio ->
                    val isThisDSPActive = isDSPActiveId == ejercicio.id
                    val isThisSTTActive = isListeningId == ejercicio.id

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF004D40), Color(0xFF00897B))))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = ejercicio.titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "IA", tint = Color(0xFFFFD54F))
                            }

                            Badge(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White) {
                                Text(text = ejercicio.categoria.name.replace("_", " "), modifier = Modifier.padding(4.dp))
                            }

                            Text(text = ejercicio.instruccionEspecial, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.White.copy(alpha = 0.9f))

                            if (ejercicio.categoria == CategoriaAdaptativa.SIMULACION && ejercicio.audioInterlocutor != null) {
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1))) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🗣️ \"${ejercicio.audioInterlocutor}\"",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF004D40),
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = {
                                            if (ttsInitialized) tts?.speak(ejercicio.audioInterlocutor, TextToSpeech.QUEUE_FLUSH, null, null)
                                        }) {
                                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Reproducir", tint = Color(0xFF004D40))
                                        }
                                    }
                                }
                            }

                            if (ejercicio.bpmSugerido > 0) {
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(8.dp)) {
                                    MetronomoReutilizableComponent()
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    if (ejercicio.categoria == CategoriaAdaptativa.SIMULACION) {
                                        Text(text = "🎯 Tu objetivo de respuesta es:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    Text(
                                        text = ejercicio.textoPractica,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }

                            if (calificacionObtenida >= 0 && textoEscuchado.isNotEmpty() && isListeningId == null && historialEjercicios.last() == ejercicio) {
                                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Escuchado: \"$textoEscuchado\"", fontSize = 14.sp, color = Color.Black)
                                        Text(
                                            text = "Precisión: $calificacionObtenida%",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (calificacionObtenida >= 80) Color(0xFF2E7D32) else Color.Red
                                        )
                                    }
                                }
                            }

                            // *** CORREGIDO: Se quitó el detenerAnalisis automático que rompía la vista ***
                            if (isThisDSPActive) {
                                BiofeedbackVisualAvanzadoDSP(metricas = metricasDSP)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { iniciarEvaluacion(ejercicio) },
                                    modifier = Modifier.weight(1f),
                                    enabled = isDSPActiveId == null,
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isThisSTTActive) Color.Red else Color.White)
                                ) {
                                    Text(
                                        if (isThisSTTActive) "⏹️ Voz" else "📝 Texto",
                                        color = if (isThisSTTActive) Color.White else Color(0xFF004D40),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            return@Button
                                        }

                                        if (isThisDSPActive) {
                                            // *** CORREGIDO: Solo se detiene y guarda al dar clic aquí ***
                                            motorDSP.detenerAnalisis(context, "DSP (${ejercicio.categoria.name})")
                                            isDSPActiveId = null
                                        } else {
                                            motorDSP.detenerAnalisis(null)
                                            motorDSP.iniciarAnalisisDSP()
                                            isDSPActiveId = ejercicio.id
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = isListeningId == null,
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isThisDSPActive) Color(0xFFD32F2F) else Color(0xFF00BFA5))
                                ) {
                                    Text(
                                        if (isThisDSPActive) "⏹️ Stop DSP" else "🎛️ Tensión",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}