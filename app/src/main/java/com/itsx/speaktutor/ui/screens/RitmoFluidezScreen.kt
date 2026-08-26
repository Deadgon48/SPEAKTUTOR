package com.itsx.speaktutor.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.widget.Toast
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
import com.itsx.speaktutor.ui.components.ControlVelocidadTts
import com.itsx.speaktutor.ui.components.GrabadoraProgresoComponent
import com.itsx.speaktutor.ui.components.MetronomoReutilizableComponent
import com.itsx.speaktutor.ui.navigation.Screen
import java.io.File
import java.io.FileInputStream
import java.util.*

enum class SeccionRitmo {
    MENU_PRINCIPAL, BLOQUES, LECTURA, GRABADORA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RitmoFluidezScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var seccionActual by remember { mutableStateOf(SeccionRitmo.MENU_PRINCIPAL) }

    // TTS y Velocidad reutilizable para la lectura guiada
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }
    var velocidadHabla by remember { mutableFloatStateOf(0.9f) }

    // Estados de Grabación y Reproducción para Bloques y Lectura Guiada
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val audioFile = remember { File(context.filesDir, "audio_practica_ritmo.m4a") }

    // Lanzador de permisos de micrófono
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, se puede iniciar la grabación
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
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(velocidadHabla, ttsInitialized) {
        if (ttsInitialized) {
            tts?.setSpeechRate(velocidadHabla)
        }
    }

    // Función para iniciar o detener la grabación integrada
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

    // Función para reproducir el audio grabado
    fun togglePlayback() {
        if (isPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        } else {
            if (audioFile.exists()) {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(audioFile.absolutePath)
                        prepare()
                        start()
                        setOnCompletionListener {
                            isPlaying = false
                            release()
                            mediaPlayer = null
                        }
                    }
                    isPlaying = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    isPlaying = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (seccionActual) {
                            SeccionRitmo.MENU_PRINCIPAL -> "Ritmo y Fluidez"
                            SeccionRitmo.BLOQUES -> "Bloques de Ritmo"
                            SeccionRitmo.LECTURA -> "Lectura Guiada"
                            SeccionRitmo.GRABADORA -> "Grabadora de Progreso"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (seccionActual == SeccionRitmo.MENU_PRINCIPAL) {
                            onBack()
                        } else {
                            if (isRecording) {
                                mediaRecorder?.stop()
                                mediaRecorder?.release()
                                mediaRecorder = null
                                isRecording = false
                            }
                            seccionActual = SeccionRitmo.MENU_PRINCIPAL
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
            // Barra de navegación rápida superior entre módulos
            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { /* Ya estás aquí */ }
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (seccionActual) {
                // MENÚ PRINCIPAL DEL MÓDULO (Con tarjetas degradadas)
                SeccionRitmo.MENU_PRINCIPAL -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Selecciona una herramienta para entrenar tu constancia al hablar:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Opción 1: Bloques de Ritmo
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF8E24AA))))
                                .clickable { seccionActual = SeccionRitmo.BLOQUES }
                                .padding(20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    text = "🧱 Bloques de Ritmo y Metrónomo",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Separa palabras con ayuda del pulso",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Opción 2: Lectura Guiada
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF006064), Color(0xFF00ACC1))))
                                .clickable { seccionActual = SeccionRitmo.LECTURA }
                                .padding(20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    text = "📖 Lectura Guiada (Textos Cortos)",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Practica textos breves con control de velocidad",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Opción 3: Grabadora de Progreso
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF43A047))))
                                .clickable { seccionActual = SeccionRitmo.GRABADORA }
                                .padding(20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    text = "🎙️ Grabadora de Progreso",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Grábate y evalúa tus mejoras de fluidez",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(text = "Regresar al Menú Principal", fontSize = 16.sp)
                        }
                    }
                }

                // OPCIÓN 1: BLOQUES DE RITMO Y METRÓNOMO (Con grabadora integrada y botón de descarga)
                SeccionRitmo.BLOQUES -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "Habla separando las palabras por bloques constantes usando el metrónomo y grábate.", fontSize = 14.sp)

                        MetronomoReutilizableComponent()

                        // Botones de control de grabación y exportación en Bloques
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { toggleRecording() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecording) Color.Red else Color(0xFF4A148C)
                                )
                            ) {
                                Text(if (isRecording) "⏹️ Detener Grabación" else "🔴 Grabar Práctica")
                            }

                            if (audioFile.exists() && !isRecording) {
                                Button(
                                    onClick = { togglePlayback() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA))
                                ) {
                                    Text(if (isPlaying) "⏸️ Pausar" else "▶️ Escuchar")
                                }

                                Button(
                                    onClick = { exportarAudioAGaleria(context, audioFile) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                                ) {
                                    Text("📥 Descargar")
                                }
                            }
                        }

                        val bloquesEjemplo = listOf("El / pe / rro / de / San / ro / que", "Ca / mi / na / ba / sin / gen / te", "Sol / bri / llan / te / ma / ña / na")

                        Text(text = "Ejemplos de bloques:", fontWeight = FontWeight.Bold)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(bloquesEjemplo) { bloque ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF8E24AA))))
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = bloque, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                        Button(
                                            onClick = {
                                                if (ttsInitialized) tts?.speak(bloque.replace(" / ", " "), TextToSpeech.QUEUE_FLUSH, null, null)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                                        ) {
                                            Text("🔊 Escuchar", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // OPCIÓN 2: LECTURA GUIADA (Con textos cortos, mismas tarjetas, grabadora integrada y botón de descarga)
                SeccionRitmo.LECTURA -> {
                    val textosCortos = listOf(
                        "El sol brilla en el cielo azul.",
                        "La práctica constante mejora tu habla.",
                        "Respira hondo antes de cada frase.",
                        "Hablar despacio te da mayor seguridad."
                    )

                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Lee los textos cortos al ritmo que prefieras y grábate mientras practicas:", fontSize = 14.sp)

                        // Deslizante de velocidad reutilizado aquí
                        ControlVelocidadTts(
                            velocidadActual = velocidadHabla,
                            onVelocidadChange = { velocidadHabla = it }
                        )

                        // Controles de grabación y exportación para Lectura Guiada
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { toggleRecording() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecording) Color.Red else Color(0xFF006064)
                                )
                            ) {
                                Text(if (isRecording) "⏹️ Detener Grabación" else "🔴 Grabar Lectura")
                            }

                            if (audioFile.exists() && !isRecording) {
                                Button(
                                    onClick = { togglePlayback() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1))
                                ) {
                                    Text(if (isPlaying) "⏸️ Pausar" else "▶️ Escuchar")
                                }

                                Button(
                                    onClick = { exportarAudioAGaleria(context, audioFile) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                                ) {
                                    Text("📥 Descargar")
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(textosCortos) { texto ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF006064), Color(0xFF00ACC1))))
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Text(text = texto, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                if (ttsInitialized) tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
                                            },
                                            modifier = Modifier.align(Alignment.End),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                                        ) {
                                            Text("🔊 Reproducir Lectura", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // OPCIÓN 3: GRABADORA DE PROGRESO
                SeccionRitmo.GRABADORA -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "Graba tu voz practicando tus lecturas y escúchate para evaluar tus mejoras de fluidez.", fontSize = 14.sp)
                        GrabadoraProgresoComponent()
                    }
                }
            }
        }
    }
}


fun exportarAudioAGaleria(context: Context, audioFile: File) {
    if (!audioFile.exists()) {
        Toast.makeText(context, "No hay ninguna grabación disponible para exportar.", Toast.LENGTH_SHORT).show()
        return
    }

    val fileName = "SpeakTutor_Practica_${System.currentTimeMillis()}.m4a"

    val contentValues = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

    try {
        uri?.let { destinationUri ->
            resolver.openOutputStream(destinationUri)?.use { outputStream ->
                FileInputStream(audioFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Toast.makeText(context, "¡Audio guardado con éxito en la carpeta Música!", Toast.LENGTH_LONG).show()
        } ?: Toast.makeText(context, "Error al crear el archivo en el dispositivo.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}