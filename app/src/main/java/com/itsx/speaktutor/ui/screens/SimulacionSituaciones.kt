package com.itsx.speaktutor.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.io.File
import java.util.*

enum class SimulacionSituaciones {
    MENU_PRINCIPAL, ESCENARIOS, GUIONES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulacionSituacionesScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var seccionActual by remember { mutableStateOf(SimulacionSituaciones.MENU_PRINCIPAL) }

    // TTS para interactuar en los guiones y leer las situaciones
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }

    // Control de grabación para responder a los escenarios
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    val audioFile = remember { File(context.filesDir, "audio_simulacion.m4a") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido
        }
    }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                ttsInitialized = true
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
            mediaRecorder?.release()
        }
    }

    fun toggleRecording() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (isRecording) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isRecording = false
        } else {
            try {
                if (audioFile.exists()) audioFile.delete()
                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFile.absolutePath)
                    prepare()
                    start()
                }
                mediaRecorder = recorder
                isRecording = true
            } catch (e: Exception) {
                e.printStackTrace()
                isRecording = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (seccionActual) {
                            SimulacionSituaciones.MENU_PRINCIPAL -> "Simulación de Situaciones"
                            SimulacionSituaciones.ESCENARIOS -> "Escenarios Cotidianos"
                            SimulacionSituaciones.GUIONES -> "Práctica de Guiones e Interlocutor"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (seccionActual == SimulacionSituaciones.MENU_PRINCIPAL) {
                            onBack()
                        } else {
                            if (isRecording) {
                                mediaRecorder?.stop()
                                mediaRecorder?.release()
                                mediaRecorder = null
                                isRecording = false
                            }
                            seccionActual = SimulacionSituaciones.MENU_PRINCIPAL
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (seccionActual) {
                SimulacionSituaciones.MENU_PRINCIPAL -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Entrena tu fluidez enfrentando situaciones reales de la vida cotidiana:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Tarjeta 1: Escenarios Cotidianos
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF0277BD), Color(0xFF00ACC1))))
                                .clickable { seccionActual = SimulacionSituaciones.ESCENARIOS }
                                .padding(20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    text = "🎧 Escenarios Cotidianos",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Escucha la situación y responde grabando tu voz",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Tarjeta 2: Práctica de Guiones (Interlocutor)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFEF6C00), Color(0xFFFFA726))))
                                .clickable { seccionActual = SimulacionSituaciones.GUIONES }
                                .padding(20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    text = "💬 Práctica de Guiones e Interlocutor",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "La app actúa como interlocutor en diálogos guiados",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Regresar al Menú Principal", fontSize = 16.sp)
                        }
                    }
                }

                SimulacionSituaciones.ESCENARIOS -> {
                    val escenarios = listOf(
                        Triple("☕ Pedir un café en la cafetería", "Hola, buenos días. ¿Qué le gustaría ordenar hoy?", "Simula que pides tu bebida favorita con calma."),
                        Triple("🛒 Comprar en el supermercado", "Hola, ¿encontró todo lo que buscaba o le ayudo en algo?", "Responde al cajero o dependiente de tienda."),
                        Triple("🚌 Preguntar dirección en la calle", "Disculpe, ¿sabe por dónde queda la estación del metro más cercana?", "Indica una dirección de forma fluida y pausada.")
                    )

                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "Escucha el planteamiento del interlocutor y graba tu respuesta de forma natural:", fontSize = 14.sp)

                        // Controles globales de grabación para escenarios
                        Button(
                            onClick = { toggleRecording() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) Color.Red else Color(0xFF0277BD)
                            )
                        ) {
                            Text(if (isRecording) "⏹️ Detener Respuesta" else "🔴 Grabar mi Respuesta al Escenario")
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(escenarios) { (titulo, audioSimulado, instruccion) ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF0277BD), Color(0xFF00ACC1))))
                                        .padding(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(text = titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(text = "🗣️ Interlocutor: \"$audioSimulado\"", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                                        Text(text = instruccion, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))

                                        Button(
                                            onClick = {
                                                if (ttsInitialized) {
                                                    tts?.speak(audioSimulado, TextToSpeech.QUEUE_FLUSH, null, null)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                                        ) {
                                            Text("🔊 Reproducir Situación", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SimulacionSituaciones.GUIONES -> {
                    val dialogos = listOf(
                        Pair("App (Recepcionista):", "Bienvenido al hotel. ¿Tiene una reserva a su nombre?"),
                        Pair("Tú (Huésped):", "Sí, reservé una habitación individual para dos noches."),
                        Pair("App (Recepcionista):", "Perfecto, ¿me podría proporcionar una identificación por favor?"),
                        Pair("Tú (Huésped):", "Claro que sí, aquí tiene mi credencial de elector.")
                    )

                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "Practica un diálogo guiado donde la app toma un rol y tú respondes paso a paso:", fontSize = 14.sp)

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(dialogos) { (rol, linea) ->
                                val esApp = rol.contains("App")
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (esApp) Brush.linearGradient(listOf(Color(0xFFEF6C00), Color(0xFFFFA726)))
                                            else Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF546E7A)))
                                        )
                                        .padding(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(text = rol, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                                        Text(text = linea, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                        if (esApp) {
                                            Button(
                                                onClick = {
                                                    if (ttsInitialized) {
                                                        tts?.speak(linea, TextToSpeech.QUEUE_FLUSH, null, null)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                                            ) {
                                                Text("🔊 Escuchar Interlocutor", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}