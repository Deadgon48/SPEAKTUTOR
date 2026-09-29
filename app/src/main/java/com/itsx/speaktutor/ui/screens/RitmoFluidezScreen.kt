package com.itsx.speaktutor.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
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
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.ControlVelocidadTts
import com.itsx.speaktutor.ui.components.MetronomoReutilizableComponent
import com.itsx.speaktutor.ui.navigation.Screen
import java.io.File
import java.io.FileInputStream
import java.util.*

// *** INICIO IMPORTACIONES DSP ***
import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BiofeedbackVisualAvanzadoDSP
import com.itsx.speaktutor.ui.components.HomeIcon

// *** FIN IMPORTACIONES DSP ***

enum class SeccionRitmo {
    MENU_PRINCIPAL, BLOQUES, LECTURA, GRABADORA
}

enum class NivelBloques {
    PRINCIPIANTE, INTERMEDIO, AVANZADO
}

fun calcularPrecisionRitmo(textoEsperado: String, textoReconocido: String): Int {
    if (textoReconocido.isBlank()) return 0
    val textoLimpioEsperado = textoEsperado.replace(" / ", " ").lowercase().replace(Regex("[^a-záéíóúñ ]"), "")
    val palabrasEsperadas = textoLimpioEsperado.split("\\s+".toRegex())
    val palabrasReconocidas = textoReconocido.lowercase().replace(Regex("[^a-záéíóúñ ]"), "").split("\\s+".toRegex())

    var coincidencias = 0
    for (palabra in palabrasReconocidas) {
        if (palabrasEsperadas.contains(palabra)) {
            coincidencias++
        }
    }
    val porcentaje = (coincidencias.toFloat() / palabrasEsperadas.size.coerceAtLeast(1)) * 100
    return porcentaje.toInt().coerceIn(0, 100)
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RitmoFluidezScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var seccionActual by remember { mutableStateOf(SeccionRitmo.MENU_PRINCIPAL) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }
    var velocidadHabla by remember { mutableFloatStateOf(0.9f) }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var textoEscuchado by remember { mutableStateOf("") }
    var ultimaCalificacion by remember { mutableStateOf(0) }

    var nivelBloqueSeleccionado by remember { mutableStateOf(NivelBloques.PRINCIPIANTE) }

    var isRecordingLibre by remember { mutableStateOf(false) }
    var isPlayingLibre by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val audioFile = remember { File(context.filesDir, "audio_practica_ritmo.m4a") }

    // *** INICIO ESTADOS DSP ***
    val motorDSP = remember { MotorAudioDSP() }
    val metricasDSP by motorDSP.metricasAcusticas.collectAsState()
    var isDSPActiveId by remember { mutableStateOf<String?>(null) }
    // *** FIN ESTADOS DSP ***

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val bloquesPrincipiante = listOf(
        "El / pe / rro / de / San / ro / que",
        "Ca / mi / na / ba / sin / gen / te",
        "Sol / bri / llan / te / ma / ña / na",
        "Pau / sa / y / res / pi / ra / bien",
        "Un / pa / so / a / la / vez"
    )
    val bloquesIntermedio = listOf(
        "La / cons / tan / cia / me / jo / ra / tu / ha / bla",
        "El / a / i / re / fluye / con / na / tu / ra / li / dad",
        "Hab / lar / des / pa / cio / te / da / con / fianza",
        "Ca / da / pa / la / bra / tie / ne / su / tiem / po"
    )
    val bloquesAvanzado = listOf(
        "La / ar / ti / cu / la / ción / cla / ra / fa / ci / li / ta / la / co / mu / ni / ca / ción",
        "E / je / cu / tar / pau / sas / es / trug / tu / ras / re / du / ce / los / blo / que / os",
        "Mantén / el / con / trol / de / tu / res / pi / ra / ción / en / ca / da / ora / ción"
    )

    val lecturasExtensas = listOf(
        Pair(
            "🌊 El curso del río (Reflexión)",
            "Un río nunca se detiene ante las rocas; simplemente encuentra la forma de rodearlas y continuar su camino. De la misma manera, al hablar no debemos apresurar las palabras ni temer a las pausas. El secreto de una fluidez serena reside en permitir que el aire fluya sin prisa."
        ),
        Pair(
            "🌳 La fuerza del árbol (Cuento breve)",
            "En medio del bosque, un viejo roble enseñaba a los árboles jóvenes el arte de resistir las tormentas. Les decía que el árbol que intenta ser rígido frente al viento termina por romperse, mientras que aquel que se mece suavemente al ritmo de la brisa permanece siempre firme."
        ),
        Pair(
            "💡 El valor de la pausa (Técnica de habla)",
            "Hacer una pausa antes de hablar no es un signo de duda, sino una muestra de dominio y serenidad. Cuando hacemos un alto para inhalar aire de forma diafragmática, preparamos las cuerdas vocales para articular cada sílaba con precisión."
        )
    )

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
            textToSpeech.stop()
            textToSpeech.shutdown()
            recognizer.destroy()
            mediaRecorder?.release()
            mediaPlayer?.release()
            // *** INICIO LIMPIEZA DSP ***
            motorDSP.detenerAnalisis(null)
            // *** FIN LIMPIEZA DSP ***
        }
    }

    LaunchedEffect(velocidadHabla, ttsInitialized) {
        if (ttsInitialized) tts?.setSpeechRate(velocidadHabla)
    }

    fun iniciarEscuchaSTTRitmo(textoObjetivo: String, nombreModulo: String) {
        val permissionCheck =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val textoPronunciado = matches?.get(0) ?: ""
                textoEscuchado = textoPronunciado

                val calificacion = calcularPrecisionRitmo(textoObjetivo, textoPronunciado)
                ultimaCalificacion = calificacion

                ProgresoStorage.guardarSesion(
                    context = context,
                    dificultad = nombreModulo,
                    aciertos = if (calificacion >= 60) 1 else 0,
                    errores = if (calificacion < 60) 1 else 0,
                    calificacion = calificacion
                )
                Toast.makeText(context, "Evaluación completada: $calificacion%", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (seccionActual) {
                            SeccionRitmo.MENU_PRINCIPAL -> "Ritmo y Fluidez"; SeccionRitmo.BLOQUES -> "Bloques de Ritmo"; SeccionRitmo.LECTURA -> "Lectura Guiada"; SeccionRitmo.GRABADORA -> "Grabadora de Progreso"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (seccionActual == SeccionRitmo.MENU_PRINCIPAL) onBack() else {
                            if (isListening) speechRecognizer?.stopListening()
                            motorDSP.detenerAnalisis(null)
                            seccionActual = SeccionRitmo.MENU_PRINCIPAL
                        }
                    }) {
                        Icon(
                            imageVector = ArrowLeftCircleIcon,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { /* Ya estás aquí */ },
                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                onNavigateProgreso = { navController.navigate(Screen.Progreso.route) },
                onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (seccionActual) {
                SeccionRitmo.MENU_PRINCIPAL -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(85.dp)
                                .clip(RoundedCornerShape(16.dp)).background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF4A148C),
                                            Color(0xFF8E24AA)
                                        )
                                    )
                                ).clickable {
                                    textoEscuchado = ""; seccionActual = SeccionRitmo.BLOQUES
                                }.padding(20.dp), contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    "🧱 Bloques de Ritmo y Metrónomo",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ); Text(
                                "Separa sílabas y evalúa tu precisión paso a paso",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(85.dp)
                                .clip(RoundedCornerShape(16.dp)).background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF006064),
                                            Color(0xFF00ACC1)
                                        )
                                    )
                                ).clickable {
                                    textoEscuchado = ""; seccionActual = SeccionRitmo.LECTURA
                                }.padding(20.dp), contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    "📖 Lectura Guiada",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ); Text(
                                "Lee párrafos completos con calificación instantánea",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(85.dp)
                                .clip(RoundedCornerShape(16.dp)).background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF1B5E20),
                                            Color(0xFF43A047)
                                        )
                                    )
                                ).clickable { seccionActual = SeccionRitmo.GRABADORA }
                                .padding(20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    "🎙️ Grabadora de Progreso",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ); Text(
                                "Grábate libremente y descarga tus archivos MP4/M4A",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            }
                        }
                    }
                }

                SeccionRitmo.BLOQUES -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Habla separando las palabras por bloques usando el metrónomo:",
                            fontSize = 14.sp
                        )
                        MetronomoReutilizableComponent()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = nivelBloqueSeleccionado == NivelBloques.PRINCIPIANTE,
                                onClick = { nivelBloqueSeleccionado = NivelBloques.PRINCIPIANTE },
                                label = { Text("Principiante") })
                            FilterChip(
                                selected = nivelBloqueSeleccionado == NivelBloques.INTERMEDIO,
                                onClick = { nivelBloqueSeleccionado = NivelBloques.INTERMEDIO },
                                label = { Text("Intermedio") })
                            FilterChip(
                                selected = nivelBloqueSeleccionado == NivelBloques.AVANZADO,
                                onClick = { nivelBloqueSeleccionado = NivelBloques.AVANZADO },
                                label = { Text("Avanzado") })
                        }

                        if (textoEscuchado.isNotEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Reconocido: \"$textoEscuchado\"",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Precisión: $ultimaCalificacion%",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        val listaMostrada = when (nivelBloqueSeleccionado) {
                            NivelBloques.PRINCIPIANTE -> bloquesPrincipiante; NivelBloques.INTERMEDIO -> bloquesIntermedio; NivelBloques.AVANZADO -> bloquesAvanzado
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listaMostrada) { bloque ->
                                val isThisDSPActive = isDSPActiveId == bloque

                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp)).background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF4A148C),
                                                    Color(0xFF8E24AA)
                                                )
                                            )
                                        ).padding(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = bloque,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )

                                        if (isThisDSPActive) {
                                            BiofeedbackVisualAvanzadoDSP(metricas = metricasDSP)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    iniciarEscuchaSTTRitmo(
                                                        bloque,
                                                        "Ritmo (Bloques)"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color.White),
                                                modifier = Modifier.weight(1f),
                                                enabled = isDSPActiveId == null
                                            ) {
                                                Text(
                                                    if (isListening) "⏹️ Voz" else "📝 Texto",
                                                    color = if (isListening) Color.White else Color(
                                                        0xFF4A148C
                                                    ),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    val permissionCheck =
                                                        ContextCompat.checkSelfPermission(
                                                            context,
                                                            Manifest.permission.RECORD_AUDIO
                                                        )
                                                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                        return@Button
                                                    }
                                                    if (isThisDSPActive) {
                                                        motorDSP.detenerAnalisis(
                                                            context,
                                                            "Ritmo DSP (Bloques)"
                                                        )
                                                        isDSPActiveId = null
                                                    } else {
                                                        motorDSP.detenerAnalisis(null)
                                                        motorDSP.iniciarAnalisisDSP()
                                                        isDSPActiveId = bloque
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                enabled = !isListening,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isThisDSPActive) Color(
                                                        0xFFD32F2F
                                                    ) else Color(0xFF00897B)
                                                )
                                            ) {
                                                Text(
                                                    if (isThisDSPActive) "⏹️ Stop DSP" else "🎛️ DSP",
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SeccionRitmo.LECTURA -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Lee los párrafos guiados para ejercitar tu fluidez con evaluación nativa:",
                            fontSize = 14.sp
                        )
                        ControlVelocidadTts(
                            velocidadActual = velocidadHabla,
                            onVelocidadChange = { velocidadHabla = it })

                        if (textoEscuchado.isNotEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Lo que la app escuchó:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "\"$textoEscuchado\"",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Precisión de Lectura: $ultimaCalificacion%",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(lecturasExtensas) { (titulo, contenido) ->
                                val isThisDSPActive = isDSPActiveId == titulo

                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp)).background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF006064),
                                                    Color(0xFF00ACC1)
                                                )
                                            )
                                        ).padding(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = titulo,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = contenido,
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.95f),
                                            lineHeight = 20.sp
                                        )

                                        if (isThisDSPActive) {
                                            BiofeedbackVisualAvanzadoDSP(metricas = metricasDSP)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    iniciarEscuchaSTTRitmo(
                                                        contenido,
                                                        "Ritmo (Lectura Guiada)"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color.White),
                                                modifier = Modifier.weight(1f),
                                                enabled = isDSPActiveId == null
                                            ) {
                                                Text(
                                                    if (isListening) "⏹️ Voz" else "📝 Texto",
                                                    color = if (isListening) Color.White else Color(
                                                        0xFF006064
                                                    ),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    val permissionCheck =
                                                        ContextCompat.checkSelfPermission(
                                                            context,
                                                            Manifest.permission.RECORD_AUDIO
                                                        )
                                                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                        return@Button
                                                    }
                                                    if (isThisDSPActive) {
                                                        motorDSP.detenerAnalisis(
                                                            context,
                                                            "Ritmo DSP (Lectura)"
                                                        )
                                                        isDSPActiveId = null
                                                    } else {
                                                        motorDSP.detenerAnalisis(null)
                                                        motorDSP.iniciarAnalisisDSP()
                                                        isDSPActiveId = titulo
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                enabled = !isListening,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isThisDSPActive) Color(
                                                        0xFFD32F2F
                                                    ) else Color(0xFF00897B)
                                                )
                                            ) {
                                                Text(
                                                    if (isThisDSPActive) "⏹️ Stop DSP" else "🎛️ DSP",
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SeccionRitmo.GRABADORA -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Grabadora de Progreso",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Grábate leyendo un texto o hablando libremente para escuchar tu propia evolución y detectar tus bloqueos.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Indicador visual de estado
                                val estadoTexto = when {
                                    isRecordingLibre -> "🔴 Grabando audio..."
                                    isPlayingLibre -> "🔊 Reproduciendo..."
                                    else -> "Lista para grabar"
                                }

                                Text(
                                    text = estadoTexto,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRecordingLibre) Color.Red else MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // BOTÓN GRABAR / DETENER GRABACIÓN
                                    FloatingActionButton(
                                        onClick = {
                                            if (isPlayingLibre) {
                                                Toast.makeText(context, "Detén la reproducción primero", Toast.LENGTH_SHORT).show()
                                                return@FloatingActionButton
                                            }

                                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                return@FloatingActionButton
                                            }

                                            if (isRecordingLibre) {
                                                // Detener
                                                try {
                                                    mediaRecorder?.stop()
                                                    mediaRecorder?.release()
                                                    mediaRecorder = null
                                                    isRecordingLibre = false
                                                    Toast.makeText(context, "Grabación guardada con éxito", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            } else {
                                                // Iniciar
                                                try {
                                                    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                        MediaRecorder(context)
                                                    } else {
                                                        MediaRecorder()
                                                    }
                                                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                                                    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                                    recorder.setOutputFile(audioFile.absolutePath)
                                                    recorder.prepare()
                                                    recorder.start()
                                                    mediaRecorder = recorder
                                                    isRecordingLibre = true
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Error al iniciar grabación", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        containerColor = if (isRecordingLibre) Color.Red else MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ) {
                                        Icon(
                                            imageVector = if (isRecordingLibre) Icons.Default.Stop else Icons.Default.Mic,
                                            contentDescription = "Grabar",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    // BOTÓN REPRODUCIR / DETENER REPRODUCCIÓN
                                    FloatingActionButton(
                                        onClick = {
                                            if (isRecordingLibre) {
                                                Toast.makeText(context, "Detén la grabación primero", Toast.LENGTH_SHORT).show()
                                                return@FloatingActionButton
                                            }

                                            if (isPlayingLibre) {
                                                // Detener reproducción
                                                mediaPlayer?.stop()
                                                mediaPlayer?.release()
                                                mediaPlayer = null
                                                isPlayingLibre = false
                                            } else {
                                                // Iniciar reproducción
                                                if (audioFile.exists()) {
                                                    try {
                                                        val player = MediaPlayer()
                                                        player.setDataSource(audioFile.absolutePath)
                                                        player.prepare()
                                                        player.start()
                                                        player.setOnCompletionListener {
                                                            isPlayingLibre = false
                                                        }
                                                        mediaPlayer = player
                                                        isPlayingLibre = true
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "No hay ninguna grabación guardada", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        containerColor = if (isPlayingLibre) Color(0xFFF57C00) else MaterialTheme.colorScheme.secondary,
                                        contentColor = Color.White
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingLibre) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "Reproducir",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                // --- NUEVO: BOTÓN DE DESCARGAR AUDIO ---
                                Button(
                                    onClick = {
                                        if (isRecordingLibre || isPlayingLibre) {
                                            Toast.makeText(context, "Detén la grabación/reproducción primero", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        if (audioFile.exists()) {
                                            try {
                                                // Crear metadatos para el archivo en la carpeta pública
                                                val valores = ContentValues().apply {
                                                    put(MediaStore.MediaColumns.DISPLAY_NAME, "SpeakTutor_${System.currentTimeMillis()}.m4a")
                                                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                                    }
                                                }

                                                // Insertar y copiar el archivo
                                                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
                                                if (uri != null) {
                                                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                                        FileInputStream(audioFile).use { inputStream ->
                                                            inputStream.copyTo(outputStream)
                                                        }
                                                    }
                                                    Toast.makeText(context, "✅ Audio descargado en la carpeta 'Descargas'", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "Error al crear el archivo", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(context, "Error al descargar audio", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "No hay ninguna grabación para descargar", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    enabled = !isRecordingLibre,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD))
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = "Descargar")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Descargar Grabación", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }