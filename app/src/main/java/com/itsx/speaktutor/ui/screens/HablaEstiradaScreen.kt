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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import com.itsx.speaktutor.logic.GestorEjercicios
import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.BiofeedbackVisualAvanzadoDSP
import com.itsx.speaktutor.ui.components.ControlVelocidadTts
import com.itsx.speaktutor.ui.components.ControlesAAF
import com.itsx.speaktutor.ui.components.HomeIcon
import com.itsx.speaktutor.ui.components.MonitorFonologicoYCAA
import com.itsx.speaktutor.ui.navigation.Screen
import java.util.*

data class CategoriaArticulacion(val nombre: String, val letras: List<String>)
enum class SeccionHabla { CATEGORIAS, LETRAS, PALABRAS, PRACTICA }

fun aplicarTartamudeoSencillo(palabra: String): Pair<String, String> {
    val regex = Regex("^([^aeiouáéíóúüAEIOUÁÉÍÓÚÜ]*)([aeiouáéíóúüAEIOUÁÉÍÓÚÜ]+)(.*)$", RegexOption.IGNORE_CASE)
    val match = regex.find(palabra)
    if (match != null) {
        val consonantes = match.groupValues[1]
        val vocales = match.groupValues[2]
        val resto = match.groupValues[3]
        if (consonantes.isEmpty()) return Pair("$vocales... $resto", "$vocales, ${vocales.lowercase()}, $resto")
        return Pair("$consonantes... $vocales... $resto", "$consonantes, ${vocales.lowercase().repeat(3)}, $resto")
    }
    return Pair(palabra, palabra)
}

fun calcularPrecisionPalabra(esperada: String, reconocida: String): Int {
    if (reconocida.isBlank()) return 0
    val palabraObj = esperada.lowercase().replace(Regex("[^a-záéíóúñ]"), "")
    val reconocidas = reconocida.lowercase().replace(Regex("[^a-záéíóúñ ]"), "").split("\\s+".toRegex())
    return if (reconocidas.contains(palabraObj)) 100 else 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HablaEstiradaScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var velocidadHabla by remember { mutableFloatStateOf(0.9f) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var textoEscuchado by remember { mutableStateOf("") }
    var ultimaCalificacion by remember { mutableStateOf(-1) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
    val motorDSP = remember { MotorAudioDSP() }
    val metricasDSP by motorDSP.metricasAcusticas.collectAsState()
    var isDSPActive by remember { mutableStateOf(false) }

    val diccionarioPalabras by remember { mutableStateOf(GestorEjercicios.obtenerDiccionarioHablaEstirada(context, "HablaEstirada/palabras_habla_estirada.json")) }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) { tts?.language = Locale("es", "ES"); ttsInitialized = true }
        }
        tts = textToSpeech
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        onDispose { textToSpeech.stop(); textToSpeech.shutdown(); speechRecognizer?.destroy(); motorDSP.detenerAnalisis(null) }
    }

    val categorias = listOf(
        CategoriaArticulacion("Oclusivas", listOf("B", "D", "G", "K", "P", "T")),
        CategoriaArticulacion("Nasales", listOf("M", "N", "Ñ")),
        CategoriaArticulacion("Semivocales", listOf("Y", "W")),
        CategoriaArticulacion("Vocales", listOf("A", "E", "I", "O", "U")),
        CategoriaArticulacion("Consonantes Restantes", listOf("C", "F", "H", "J", "L", "Q", "R", "S", "V", "X", "Z", "CH"))
    )

    var seccionActual by remember { mutableStateOf(SeccionHabla.CATEGORIAS) }
    var categoriaSeleccionada by remember { mutableStateOf<CategoriaArticulacion?>(null) }
    var letraSeleccionada by remember { mutableStateOf<String?>(null) }
    var palabraSeleccionada by remember { mutableStateOf<String?>(null) }

    fun iniciarEscuchaSTT(palabraObjetivo: String) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return }
        if (isListening) { speechRecognizer?.stopListening(); isListening = false; return }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES") }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true; textoEscuchado = "Escuchando..." }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false; textoEscuchado = "Error de escucha" }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val textoDicho = matches?.firstOrNull()?.trim() ?: ""
                if (textoDicho.isBlank()) { textoEscuchado = "No se detectó audio."; return }
                textoEscuchado = textoDicho
                ultimaCalificacion = calcularPrecisionPalabra(palabraObjetivo, textoDicho)

                // --- NUEVO: Guardar en ProgresoStorage ---
                val aciertos = if (ultimaCalificacion >= 80) 1 else 0
                val errores = if (ultimaCalificacion < 80) 1 else 0
                ProgresoStorage.guardarSesionMultidimensional(
                    context = context,
                    modulo = "Habla Estirada",
                    aciertos = aciertos,
                    erroresTotales = errores,
                    calificacion = ultimaCalificacion
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
                title = { Text("Habla Estirada") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isDSPActive) { motorDSP.detenerAnalisis(null); isDSPActive = false }
                        if (isListening) { speechRecognizer?.stopListening(); isListening = false }
                        onBack()
                    }) { Icon(ArrowLeftCircleIcon, contentDescription = "Regresar", tint = MaterialTheme.colorScheme.onSurface) }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = {
                        if (isDSPActive) { motorDSP.detenerAnalisis(null); isDSPActive = false }
                        if (isListening) { speechRecognizer?.stopListening(); isListening = false }
                        onBack()
                    }, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(HomeIcon, contentDescription = "Menú Principal", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regresar al Menú Principal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            if (seccionActual == SeccionHabla.CATEGORIAS) {
                BarraNavegacionModulos(
                    onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                    onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                    onNavigateHablaEstirada = { /* Ya estás aquí */ },
                    onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                    onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                    onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                    onNavigateProgreso = { navController.navigate(Screen.Progreso.route) },
                    onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) },
                    onNavigateBio = { navController.navigate(Screen.Biofeedback.route) },
                    onNavigateTarjetasArticulacion = { navController.navigate(Screen.TarjetasArticulacion.route) },
                    onNavigateConcienciaFonologica = { navController.navigate(Screen.ConcienciaFonologica.route) },
                    onNavigateReporteMultidimensional = { navController.navigate(Screen.ReporteMultidimensional.route) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text("Habla Estirada", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Practica la pronunciación por modo de articulación y prolongación de fonemas.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            when (seccionActual) {
                SeccionHabla.PRACTICA -> {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Button(
                            onClick = {
                                if (isDSPActive) { motorDSP.detenerAnalisis(null); isDSPActive = false }
                                if (isListening) { speechRecognizer?.stopListening(); isListening = false }
                                textoEscuchado = ""
                                ultimaCalificacion = -1
                                seccionActual = SeccionHabla.PALABRAS
                            },
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) { Text("← Volver a Palabras") }

                        val palabraNormal = palabraSeleccionada ?: ""
                        val (textoVisual, textoTts) = aplicarTartamudeoSencillo(palabraNormal)

                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Palabra Objetivo", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                Text(textoVisual, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                ControlVelocidadTts(velocidadActual = velocidadHabla, onVelocidadChange = { velocidadHabla = it; tts?.setSpeechRate(it) })
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { if (ttsInitialized) tts?.speak(textoTts, TextToSpeech.QUEUE_FLUSH, null, null) }) { Text("🔊 Escuchar Ejemplo") }
                            }
                        }

                        if (isDSPActive) {
                            ControlesAAF(motorDSP = motorDSP)
                            BiofeedbackVisualAvanzadoDSP(metricasDSP)
                            MonitorFonologicoYCAA(motorDSP = motorDSP, palabraObjetivoActual = palabraNormal, textoEscuchado = textoEscuchado)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { if (isListening) speechRecognizer?.stopListening() else iniciarEscuchaSTT(palabraNormal) },
                                modifier = Modifier.weight(1f).height(50.dp), enabled = !isDSPActive, colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else MaterialTheme.colorScheme.primary)
                            ) { Text(if (isListening) "⏹️ Escuchando..." else "🎤 Leer (STT)") }

                            Button(
                                onClick = {
                                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return@Button }
                                    if (isDSPActive) { motorDSP.detenerAnalisis(null); isDSPActive = false } else { motorDSP.iniciarAnalisisDSP(); isDSPActive = true }
                                },
                                modifier = Modifier.weight(1f).height(50.dp), enabled = !isListening, colors = ButtonDefaults.buttonColors(containerColor = if (isDSPActive) Color(0xFFD32F2F) else Color(0xFF00897B))
                            ) { Text(if (isDSPActive) "⏹️ Detener DSP" else "🎛️ Activar DSP") }
                        }

                        if (textoEscuchado.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(if (ultimaCalificacion == 100) "✅ Correcto: $textoEscuchado" else "❌ Escuchado: $textoEscuchado", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (ultimaCalificacion == 100) Color(0xFF2E7D32) else Color(0xFFC62828))
                        }
                    }
                }

                SeccionHabla.PALABRAS -> {
                    Button(onClick = { seccionActual = SeccionHabla.LETRAS }, modifier = Modifier.padding(bottom = 8.dp)) { Text("← Volver a Letras") }

                    val colorPalabrasFondo = when (categoriaSeleccionada?.nombre) {
                        "Oclusivas" -> listOf(Color(0xFF4A148C), Color(0xFF8E24AA))
                        "Nasales" -> listOf(Color(0xFF006064), Color(0xFF00ACC1))
                        "Semivocales" -> listOf(Color(0xFF1B5E20), Color(0xFF43A047))
                        "Vocales" -> listOf(Color(0xFFE65100), Color(0xFFF57C00))
                        else -> listOf(Color(0xFF37474F), Color(0xFF546E7A))
                    }

                    Text("Letra: $letraSeleccionada", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorPalabrasFondo[1])
                    Spacer(modifier = Modifier.height(12.dp))

                    val palabras = diccionarioPalabras[letraSeleccionada] ?: emptyList()

                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(palabras) { palabra ->
                            Box(
                                modifier = Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(colorPalabrasFondo))
                                    .clickable { palabraSeleccionada = palabra; textoEscuchado = ""; ultimaCalificacion = -1; seccionActual = SeccionHabla.PRACTICA }
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(palabra, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Practicar", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                SeccionHabla.LETRAS -> {
                    Button(onClick = { seccionActual = SeccionHabla.CATEGORIAS }, modifier = Modifier.padding(bottom = 8.dp)) { Text("← Volver a Categorías") }

                    val colorLetraFondoGrid = when (categoriaSeleccionada?.nombre) {
                        "Oclusivas" -> listOf(Color(0xFF4A148C), Color(0xFF8E24AA))
                        "Nasales" -> listOf(Color(0xFF006064), Color(0xFF00ACC1))
                        "Semivocales" -> listOf(Color(0xFF1B5E20), Color(0xFF43A047))
                        "Vocales" -> listOf(Color(0xFFE65100), Color(0xFFF57C00))
                        else -> listOf(Color(0xFF37474F), Color(0xFF546E7A))
                    }

                    Text(categoriaSeleccionada?.nombre ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorLetraFondoGrid[1])
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(categoriaSeleccionada?.letras ?: emptyList()) { letra ->
                            Box(
                                modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(colorLetraFondoGrid))
                                    .clickable { letraSeleccionada = letra; seccionActual = SeccionHabla.PALABRAS },
                                contentAlignment = Alignment.Center
                            ) { Text(letra, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                    }
                }

                SeccionHabla.CATEGORIAS -> {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(categorias) { cat ->
                            val colorLetraFondo = when (cat.nombre) {
                                "Oclusivas" -> listOf(Color(0xFF4A148C), Color(0xFF8E24AA))
                                "Nasales" -> listOf(Color(0xFF006064), Color(0xFF00ACC1))
                                "Semivocales" -> listOf(Color(0xFF1B5E20), Color(0xFF43A047))
                                "Vocales" -> listOf(Color(0xFFE65100), Color(0xFFF57C00))
                                else -> listOf(Color(0xFF37474F), Color(0xFF546E7A))
                            }
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(cat.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorLetraFondo[1])
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(colorLetraFondo))
                                        .clickable { categoriaSeleccionada = cat; seccionActual = SeccionHabla.LETRAS }.padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(cat.nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("${cat.letras.size} fonemas", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                                        }
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
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