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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.logic.EstadoVozDSP
import com.itsx.speaktutor.logic.GestorEjercicios
import com.itsx.speaktutor.logic.LlamadaTelefonica
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.BiofeedbackVisualAvanzadoDSP
import com.itsx.speaktutor.ui.components.ControlesAAF
import com.itsx.speaktutor.ui.components.HomeIcon
import com.itsx.speaktutor.ui.components.MonitorFonologicoYCAA
import com.itsx.speaktutor.ui.navigation.Screen

enum class SimulacionSituaciones { MENU_PRINCIPAL, ESCENARIOS, GUIONES, EVOCACION_LEXICA, LLAMADA_TELEFONICA }
enum class FaseGuion { SELECCION, CHECK_IN, PRACTICA, CHECK_OUT, RESULTADOS }
enum class FaseLlamada { MENU, EN_CURSO, EVALUACION }

enum class EstadoEmocional(val icono: String, val texto: String, val color: Color) {
    TRANQUILO("😌", "Tranquilo", Color(0xFF2E7D32)),
    NERVIOSO("😬", "Nervioso", Color(0xFFF57C00)),
    ANSIOSO("😰", "Ansioso", Color(0xFFC62828)),
    NINGUNO("", "", Color.Transparent)
}

data class TurnoGuion(val rol: String, val texto: String, val esDeApp: Boolean)
data class GuionPractica(val titulo: String, val descripcion: String, val dialogos: List<TurnoGuion>)

fun calcularPrecisionHabla(textoEsperado: String, textoReconocido: String): Int {
    if (textoReconocido.isBlank()) return 0
    val palabrasEsperadas = textoEsperado.lowercase().replace(Regex("[^a-záéíóúñ ]"), "").split("\\s+".toRegex())
    val palabrasReconocidas = textoReconocido.lowercase().replace(Regex("[^a-záéíóúñ ]"), "").split("\\s+".toRegex())
    var coincidencias = 0
    for (palabra in palabrasReconocidas) {
        if (palabrasEsperadas.contains(palabra)) coincidencias++
    }
    return ((coincidencias.toFloat() / palabrasEsperadas.size.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulacionSituacionesScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var seccionActual by remember { mutableStateOf(SimulacionSituaciones.MENU_PRINCIPAL) }

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    var tensionDetectada by remember { mutableStateOf(false) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var textoEscuchado by remember { mutableStateOf("") }
    var ultimaCalificacion by remember { mutableStateOf(0) }

    var escenariosCargados by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var guionesCargados by remember { mutableStateOf<List<GuionPractica>>(emptyList()) }


    var diccionariosEvocacion by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var diccionarioAntonimos by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var llamadasCargadas by remember { mutableStateOf<List<LlamadaTelefonica>>(emptyList()) }

    var nivelSeleccionadoIndex by remember { mutableStateOf(0) }
    var escenarioSeleccionadoIndex by remember { mutableStateOf(0) }
    val cantidadCargaAleatoria = 5

    var faseGuion by remember { mutableStateOf(FaseGuion.SELECCION) }
    var guionSeleccionado by remember { mutableStateOf<GuionPractica?>(null) }
    var emocionInicial by remember { mutableStateOf(EstadoEmocional.NINGUNO) }
    var emocionFinal by remember { mutableStateOf(EstadoEmocional.NINGUNO) }
    var indiceGuionActual by remember { mutableStateOf(0) }
    var aciertosGuion by remember { mutableStateOf(0) }
    var erroresGuion by remember { mutableStateOf(0) }

    var faseLlamada by remember { mutableStateOf(FaseLlamada.MENU) }
    var llamadaSeleccionada by remember { mutableStateOf<LlamadaTelefonica?>(null) }
    var segundosLlamada by remember { mutableStateOf(0) }
    var llamadaTimerActivo by remember { mutableStateOf(false) }

    var mostrarPausaRespiracion by remember { mutableStateOf(false) }

    val motorDSP = remember { MotorAudioDSP() }
    val metricasDSP by motorDSP.metricasAcusticas.collectAsState()
    var isDSPActive by remember { mutableStateOf(false) }

    var dafDelay by remember { mutableStateOf(50f) }
    var fafPitch by remember { mutableStateOf(-0.5f) }

    val TIEMPO_MAXIMO_EVOCACION = 30
    var tiempoRestante by remember { mutableStateOf(TIEMPO_MAXIMO_EVOCACION) }
    var estadoEvocacion by remember { mutableStateOf("INICIO") }
    var modoEvocacion by remember { mutableStateOf("CATEGORIAS") }

    var categoriaActual by remember { mutableStateOf("") }
    var antonimoActual by remember { mutableStateOf("") }

    var aciertosEvocacion by remember { mutableStateOf(0) }
    var erroresEvocacion by remember { mutableStateOf(0) }
    var pistaActual by remember { mutableStateOf("") }
    var pausarTimer by remember { mutableStateOf(false) }

    val empeoramiento by motorDSP.alertaEmpeoramiento.collectAsState()

    fun encenderDSP() {
        motorDSP.detenerAnalisis(null)
        motorDSP.configurarDAF(dafDelay.toInt())
        motorDSP.configurarFAF(fafPitch)
        motorDSP.iniciarAnalisisDSP()
        isDSPActive = true
    }

    LaunchedEffect(empeoramiento) {
        if (empeoramiento) {
            mostrarPausaRespiracion = true
            motorDSP._alertaEmpeoramiento.value = false
        }
    }

    LaunchedEffect(llamadaTimerActivo) {
        while(llamadaTimerActivo) {
            delay(1000)
            segundosLlamada++
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    LaunchedEffect(seccionActual) {
        when (seccionActual) {
            SimulacionSituaciones.ESCENARIOS -> {
                escenariosCargados = GestorEjercicios.obtenerEscenarios(context, "SimulacionSituaciones/escenarios.json", cantidadCargaAleatoria)
                escenarioSeleccionadoIndex = 0
            }
            SimulacionSituaciones.EVOCACION_LEXICA -> {
                diccionariosEvocacion = GestorEjercicios.obtenerDiccionarioEvocacion(context, "SimulacionSituaciones/evocacion_categorias.json")
                diccionarioAntonimos = GestorEjercicios.obtenerDiccionarioEvocacion(context, "SimulacionSituaciones/evocacion_antonimos.json")
                if (diccionariosEvocacion.isNotEmpty()) categoriaActual = diccionariosEvocacion.keys.random()
                if (diccionarioAntonimos.isNotEmpty()) antonimoActual = diccionarioAntonimos.keys.random()
            }
            SimulacionSituaciones.LLAMADA_TELEFONICA -> {
                llamadasCargadas = GestorEjercicios.obtenerLlamadas(context, "SimulacionSituaciones/llamadas.json", 10)
            }
            else -> {}
        }
    }

    LaunchedEffect(faseGuion, nivelSeleccionadoIndex) {
        if (faseGuion == FaseGuion.SELECCION && seccionActual == SimulacionSituaciones.GUIONES) {
            val archivo = "SimulacionSituaciones/guiones_nivel${nivelSeleccionadoIndex + 1}.json"
            guionesCargados = GestorEjercicios.obtenerGuiones(context, archivo, cantidadCargaAleatoria)
        }
    }

    LaunchedEffect(estadoEvocacion, tiempoRestante, pausarTimer) {
        if (estadoEvocacion == "JUGANDO" && tiempoRestante > 0 && !pausarTimer && !mostrarPausaRespiracion) {
            delay(1000)
            tiempoRestante--
            if (tiempoRestante == 15) {
                pistaActual = if (modoEvocacion == "CATEGORIAS" && diccionariosEvocacion.isNotEmpty()) {
                    "Empieza con '${diccionariosEvocacion[categoriaActual]?.random()?.first()?.uppercaseChar() ?: ""}'"
                } else if (modoEvocacion == "ANTONIMOS" && diccionarioAntonimos.isNotEmpty()) {
                    "Empieza con '${diccionarioAntonimos[antonimoActual]?.random()?.first()?.uppercaseChar() ?: ""}'"
                } else ""
            }
        } else if (tiempoRestante == 0 && estadoEvocacion == "JUGANDO") {
            estadoEvocacion = "FIN"
            motorDSP.detenerAnalisis(null)
            isDSPActive = false
            if (isListening) {
                speechRecognizer?.stopListening()
                isListening = false
            }
        }
    }

    var procesandoAudio by remember { mutableStateOf(false) }
    LaunchedEffect(metricasDSP.estado) {
        if (isDSPActive && !procesandoAudio) {
            if (metricasDSP.estado == EstadoVozDSP.BLOQUEO_TONICO || metricasDSP.estado == EstadoVozDSP.BLOQUEO_CLONICO) {
                procesandoAudio = true
                tensionDetectada = true

                if (estadoEvocacion == "JUGANDO") {
                    erroresEvocacion++
                    pausarTimer = true
                }

                textoEscuchado = "⚠️ Bloqueo detectado. Inhala... Exhala..."
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                coroutineScope.launch {
                    offsetX.animateTo(15f, tween(50))
                    offsetX.animateTo(-15f, tween(50))
                    offsetX.animateTo(10f, tween(50))
                    offsetX.animateTo(-10f, tween(50))
                    offsetX.animateTo(0f, tween(50))
                }
                delay(3000)
                tensionDetectada = false
                procesandoAudio = false
                pausarTimer = false
            } else if (metricasDSP.estado == EstadoVozDSP.FLUIDO) {
                procesandoAudio = true
                textoEscuchado = "✅ Fonación fluida"
                delay(1200)
                procesandoAudio = false
            }
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
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
            recognizer.destroy()
            motorDSP.detenerAnalisis(null)
        }
    }

    fun reproducirAudioApp(texto: String) {
        if (ttsInitialized) tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun avanzarTurnoGuion() {
        if (guionSeleccionado != null && indiceGuionActual < guionSeleccionado!!.dialogos.size - 1) {
            indiceGuionActual++
            val turno = guionSeleccionado!!.dialogos[indiceGuionActual]
            if (turno.esDeApp) reproducirAudioApp(turno.texto)
        } else {
            faseGuion = FaseGuion.CHECK_OUT
        }
    }

    fun iniciarEscuchaTextoSemantico() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true; textoEscuchado = "Escuchando..." }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false; textoEscuchado = "Error al escuchar" }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val textoDicho = matches?.firstOrNull()?.trim() ?: ""

                if (textoDicho.isBlank()) {
                    textoEscuchado = "No escuché nada. Intenta hablar más cerca."
                    return
                }

                val palabraDicha = textoDicho.lowercase()
                val esValido = if (modoEvocacion == "CATEGORIAS") {
                    diccionariosEvocacion[categoriaActual]?.any { palabraDicha.contains(it) } == true
                } else {
                    diccionarioAntonimos[antonimoActual]?.any { palabraDicha.contains(it) } == true
                }

                if (esValido) {
                    aciertosEvocacion++
                    textoEscuchado = "✅ Correcto: $palabraDicha"
                    pistaActual = ""
                    if (modoEvocacion == "ANTONIMOS" && diccionarioAntonimos.isNotEmpty()) {
                        antonimoActual = diccionarioAntonimos.keys.random()
                    }
                } else {
                    erroresEvocacion++
                    textoEscuchado = "❌ Incorrecto/No es: $palabraDicha"
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    fun iniciarEscuchaSTT(textoObjetivo: String, esGuion: Boolean) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val textoDicho = matches?.firstOrNull()?.trim() ?: ""

                if (textoDicho.isBlank()) {
                    textoEscuchado = "No escuché nada. Intenta hablar más cerca."
                    return
                }

                textoEscuchado = textoDicho
                ultimaCalificacion = calcularPrecisionHabla(textoObjetivo, textoDicho)

                if (esGuion) {
                    if (ultimaCalificacion >= 60) aciertosGuion++ else erroresGuion++
                    avanzarTurnoGuion()
                } else {
                    val aciertos = if (ultimaCalificacion >= 60) 1 else 0
                    val errores = if (ultimaCalificacion < 60) 1 else 0
                    ProgresoStorage.guardarSesionMultidimensional(context, "Simulación (Escenario)", aciertos, errores, ultimaCalificacion)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    fun calcularScorePsicosocial(inicial: EstadoEmocional, final: EstadoEmocional): Int {
        val valores = mapOf(EstadoEmocional.TRANQUILO to 100, EstadoEmocional.NERVIOSO to 50, EstadoEmocional.ANSIOSO to 10)
        val vInicial = valores[inicial] ?: 50
        val vFinal = valores[final] ?: 50
        return ((vInicial + (vFinal * 2)) / 3).coerceIn(0, 100)
    }

    if (mostrarPausaRespiracion) {
        Dialog(onDismissRequest = { mostrarPausaRespiracion = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().height(350.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    var faseRespiracion by remember { mutableStateOf("INHALA (4s)") }
                    val transition = rememberInfiniteTransition(label = "respiracion")
                    val scale by transition.animateFloat(
                        initialValue = 0.6f, targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                        label = "escala"
                    )

                    LaunchedEffect(scale) {
                        faseRespiracion = when {
                            scale > 1.35f -> "SOSTÉN (4s)"
                            scale < 0.65f -> "PAUSA"
                            scale > 1.0f -> "INHALA (4s)"
                            else -> "EXHALA (4s)"
                        }
                    }

                    Text("Tiempo Fuera", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006064))
                    Spacer(modifier = Modifier.height(30.dp))
                    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size((100f * scale).dp).clip(CircleShape).background(Color(0xFF00ACC1).copy(alpha = 0.5f)))
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF00ACC1)))
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    Text(faseRespiracion, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00838F))
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { mostrarPausaRespiracion = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006064))) {
                        Text("Reanudar Práctica")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (seccionActual) {
                        SimulacionSituaciones.MENU_PRINCIPAL -> "Simulación"
                        SimulacionSituaciones.ESCENARIOS -> "Escenarios Cotidianos"
                        SimulacionSituaciones.GUIONES -> "Guiones Estresantes"
                        SimulacionSituaciones.EVOCACION_LEXICA -> "Evocación Léxica"
                        SimulacionSituaciones.LLAMADA_TELEFONICA -> "Simulador de Llamadas"
                    })
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (seccionActual == SimulacionSituaciones.MENU_PRINCIPAL) {
                            onBack()
                        } else {
                            if (isListening) speechRecognizer?.stopListening()
                            motorDSP.detenerAnalisis(null)
                            isDSPActive = false
                            estadoEvocacion = "INICIO"
                            seccionActual = SimulacionSituaciones.MENU_PRINCIPAL
                            faseGuion = FaseGuion.SELECCION
                            faseLlamada = FaseLlamada.MENU
                            llamadaTimerActivo = false
                        }
                    }) { Icon(ArrowLeftCircleIcon, contentDescription = "Regresar") }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(HomeIcon, contentDescription = "Menú Principal", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regresar al Menú Principal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (seccionActual == SimulacionSituaciones.MENU_PRINCIPAL) {
                BarraNavegacionModulos(
                    onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                    onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                    onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                    onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                    onNavigateSimulacionSituaciones = { /* Ya estás aquí */ },
                    onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                    onNavigateProgreso = { navController.navigate(Screen.Progreso.route) },
                    onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) },
                    onNavigateBio = { navController.navigate(Screen.Biofeedback.route) },
                    onNavigateTarjetasArticulacion = { navController.navigate(Screen.TarjetasArticulacion.route) },
                    onNavigateConcienciaFonologica = { navController.navigate(Screen.ConcienciaFonologica.route) },
                    onNavigateReporteMultidimensional = { navController.navigate(Screen.ReporteMultidimensional.route) }
                )
            }

            when (seccionActual) {
                SimulacionSituaciones.MENU_PRINCIPAL -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                            .clip(RoundedCornerShape(16.dp)).background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0277BD),
                                    Color(0xFF00ACC1)
                                )
                            )
                        ).clickable { seccionActual = SimulacionSituaciones.ESCENARIOS }
                            .padding(20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                "🎧 Escenarios Cotidianos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ); Text(
                            "Interacciones cortas de 1 turno",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                            .clip(RoundedCornerShape(16.dp)).background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFEF6C00),
                                    Color(0xFFFFA726)
                                )
                            )
                        ).clickable {
                            seccionActual = SimulacionSituaciones.GUIONES; faseGuion =
                            FaseGuion.SELECCION
                        }.padding(20.dp), contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                "💬 Guiones Estresantes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ); Text(
                            "Jerarquía de desensibilización social",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                            .clip(RoundedCornerShape(16.dp)).background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF2E7D32),
                                    Color(0xFF66BB6A)
                                )
                            )
                        ).clickable {
                            tiempoRestante = TIEMPO_MAXIMO_EVOCACION; estadoEvocacion =
                            "INICIO"; seccionActual = SimulacionSituaciones.EVOCACION_LEXICA
                        }.padding(20.dp), contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                "⏱️ Evocación bajo Presión",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ); Text(
                            "Piensa rápido sin bloquearte",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                            .clip(RoundedCornerShape(16.dp)).background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4527A0),
                                    Color(0xFF673AB7)
                                )
                            )
                        ).clickable {
                            seccionActual = SimulacionSituaciones.LLAMADA_TELEFONICA; faseLlamada =
                            FaseLlamada.MENU
                        }.padding(20.dp), contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                "📞 Simulador de Llamadas",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ); Text(
                            "Desensibilización al teléfono",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        }
                    }
                }

                SimulacionSituaciones.LLAMADA_TELEFONICA -> {
                    when (faseLlamada) {
                        FaseLlamada.MENU -> {
                            if (llamadasCargadas.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text("Cargando números...") }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "Agenda Telefónica",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    llamadasCargadas.forEach { llamada ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                llamadaSeleccionada = llamada; faseLlamada =
                                                FaseLlamada.EN_CURSO
                                            },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Phone,
                                                    contentDescription = "Llamar",
                                                    modifier = Modifier.size(30.dp),
                                                    tint = Color(0xFF4CAF50)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text(
                                                        llamada.contexto,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                    Text(
                                                        "Presiona para iniciar llamada",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                                            alpha = 0.7f
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(40.dp))
                                }
                            }
                        }

                        FaseLlamada.EN_CURSO -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (!llamadaTimerActivo) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = "Llamar",
                                        modifier = Modifier.size(100.dp),
                                        tint = Color(0xFF4CAF50)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        llamadaSeleccionada?.contexto ?: "Llamando...",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(40.dp))
                                    Button(
                                        onClick = {
                                            llamadaTimerActivo = true
                                            segundosLlamada = 0
                                            encenderDSP()
                                            llamadaSeleccionada?.operadora?.let {
                                                reproducirAudioApp(
                                                    it
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(
                                                0xFF4CAF50
                                            )
                                        ),
                                        modifier = Modifier.size(80.dp), shape = CircleShape
                                    ) { Icon(Icons.Default.Call, contentDescription = "Contestar") }
                                } else {
                                    val minutos = segundosLlamada / 60
                                    val segs = segundosLlamada % 60
                                    Text(
                                        String.format(Locale.US, "%02d:%02d", minutos, segs),
                                        fontSize = 18.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        "En llamada...",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF388E3C)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFFF3E0)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                "Sugerencia de respuesta:",
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE65100)
                                            )
                                            Text(
                                                llamadaSeleccionada?.respuestaSugerida ?: "",
                                                fontSize = 16.sp,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                    }

                                    ControlesAAF(motorDSP = motorDSP)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    BiofeedbackVisualAvanzadoDSP(metricasDSP)
                                    Spacer(modifier = Modifier.height(30.dp))

                                    Button(
                                        onClick = {
                                            llamadaTimerActivo = false
                                            motorDSP.detenerAnalisis(context, "Llamada Telefónica")
                                            isDSPActive = false
                                            faseLlamada = FaseLlamada.EVALUACION
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(
                                                0xFFD32F2F
                                            )
                                        ),
                                        modifier = Modifier.size(80.dp), shape = CircleShape
                                    ) { Icon(Icons.Default.CallEnd, contentDescription = "Colgar") }
                                }
                            }
                        }

                        FaseLlamada.EVALUACION -> {
                            var guardadoExitoso by remember { mutableStateOf(false) }
                            val calificacionFinal =
                                100 // Basado en tu código donde mandas 100 directo

                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Llamada Finalizada",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "¿Cómo sentiste tu fluidez y confianza durante la llamada?",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    EstadoEmocional.entries.filter { it != EstadoEmocional.NINGUNO }
                                        .forEach { estado ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .clickable {
                                                        // 1. BLOQUEO SÍNCRONO: Si ya entró un clic, ignoramos cualquier otro instantáneamente
                                                        if (guardadoExitoso) return@clickable
                                                        guardadoExitoso = true

                                                        val scoreOases = calcularScorePsicosocial(EstadoEmocional.ANSIOSO, estado)

                                                        ProgresoStorage.guardarSesionMultidimensional(
                                                            context = context,
                                                            modulo = "Llamada: ${llamadaSeleccionada?.contexto?.take(15)}...",
                                                            aciertos = 1,
                                                            erroresTotales = 0,
                                                            calificacion = calificacionFinal,
                                                            impactoPsicosocial = scoreOases
                                                        )

                                                        faseLlamada = FaseLlamada.MENU
                                                        llamadasCargadas = GestorEjercicios.obtenerLlamadas(
                                                            context,
                                                            "SimulacionSituaciones/llamadas.json",
                                                            10
                                                        )
                                                    }
                                                    .padding(8.dp)

                                            ) {
                                                Text(estado.icono, fontSize = 50.sp)
                                                Text(
                                                    estado.texto,
                                                    fontWeight = FontWeight.Bold,
                                                    color = estado.color
                                                )
                                            }
                                        }
                                }
                                    }
                                }
                            }
                        }


                SimulacionSituaciones.GUIONES -> {
                    when (faseGuion) {
                        FaseGuion.SELECCION -> {
                            val nombresNiveles = listOf("Nivel 1 (Baja)", "Nivel 2 (Media)", "Nivel 3 (Alta)", "Nivel 4 (Crítica)")
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Selecciona tu nivel de exposición:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))

                                ScrollableTabRow(selectedTabIndex = nivelSeleccionadoIndex, edgePadding = 0.dp, containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clip(RoundedCornerShape(12.dp))) {
                                    nombresNiveles.forEachIndexed { index, nivel ->
                                        Tab(selected = nivelSeleccionadoIndex == index, onClick = { nivelSeleccionadoIndex = index }, text = { Text(text = nivel, fontWeight = if (nivelSeleccionadoIndex == index) FontWeight.Bold else FontWeight.Normal) })
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { val archivo = "SimulacionSituaciones/guiones_nivel${nivelSeleccionadoIndex + 1}.json"; guionesCargados = GestorEjercicios.obtenerGuiones(context, archivo, cantidadCargaAleatoria) }) { Text("🔄 Cargar otros $cantidadCargaAleatoria") }
                                }

                                if (guionesCargados.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Text("Cargando guiones...") }
                                } else {
                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        guionesCargados.forEach { guion ->
                                            Card(modifier = Modifier.fillMaxWidth().clickable { guionSeleccionado = guion; faseGuion = FaseGuion.CHECK_IN }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text(guion.titulo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = guion.descripcion, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(40.dp))
                                    }
                                }
                            }
                        }
                        FaseGuion.CHECK_IN, FaseGuion.CHECK_OUT -> {
                            val esInicio = faseGuion == FaseGuion.CHECK_IN
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(if (esInicio) "¿Cómo te sientes antes de comenzar?" else "¿Cómo te sientes ahora que terminaste?", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(40.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    EstadoEmocional.entries.filter { it != EstadoEmocional.NINGUNO }.forEach { estado ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                            if (esInicio) {
                                                emocionInicial = estado; indiceGuionActual = 0; aciertosGuion = 0; erroresGuion = 0; faseGuion = FaseGuion.PRACTICA
                                                if (guionSeleccionado?.dialogos?.get(0)?.esDeApp == true) reproducirAudioApp(guionSeleccionado!!.dialogos[0].texto)
                                            } else {
                                                emocionFinal = estado; faseGuion = FaseGuion.RESULTADOS
                                            }
                                        }.padding(8.dp)) {
                                            Text(estado.icono, fontSize = 50.sp); Text(estado.texto, fontWeight = FontWeight.Bold, color = estado.color)
                                        }
                                    }
                                }
                            }
                        }
                        FaseGuion.PRACTICA -> {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                val turnoActual = guionSeleccionado!!.dialogos[indiceGuionActual]
                                val bgBrush = if (tensionDetectada && !turnoActual.esDeApp) Brush.linearGradient(listOf(Color(0xFFC62828), Color(0xFFD32F2F))) else if (turnoActual.esDeApp) Brush.linearGradient(listOf(Color(0xFFEF6C00), Color(0xFFFFA726))) else Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF546E7A)))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Práctica de Guion", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    OutlinedButton(onClick = { if (isListening) speechRecognizer?.stopListening(); motorDSP.detenerAnalisis(null); isDSPActive = false; tts?.stop(); mostrarPausaRespiracion = true }) { Text("🌬️ Respirar", color = Color(0xFF00838F)) }
                                }
                                LinearProgressIndicator(progress = { (indiceGuionActual + 1).toFloat() / guionSeleccionado!!.dialogos.size }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)

                                Box(modifier = Modifier.fillMaxWidth().offset(x = if (!turnoActual.esDeApp) offsetX.value.dp else 0.dp).clip(RoundedCornerShape(16.dp)).background(bgBrush).padding(20.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Turno ${indiceGuionActual + 1} de ${guionSeleccionado!!.dialogos.size}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                        Text(turnoActual.rol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                                        Text(turnoActual.texto, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                        if (turnoActual.esDeApp) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = { reproducirAudioApp(turnoActual.texto) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)), modifier = Modifier.weight(1f)) { Text("🔊 Escuchar", color = Color.White) }
                                                Button(onClick = { avanzarTurnoGuion() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), modifier = Modifier.weight(1f)) { Text("Siguiente ➡️", color = Color(0xFFEF6C00)) }
                                            }
                                        }
                                    }
                                }

                                if (!turnoActual.esDeApp) {
                                    if (isDSPActive) {
                                        ControlesAAF(motorDSP = motorDSP)
                                        BiofeedbackVisualAvanzadoDSP(metricasDSP)
                                        MonitorFonologicoYCAA(motorDSP = motorDSP, palabraObjetivoActual = turnoActual.texto, textoEscuchado = textoEscuchado)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { if (isListening) speechRecognizer?.stopListening() else iniciarEscuchaSTT(turnoActual.texto, true) }, modifier = Modifier.weight(1f), enabled = !isDSPActive, colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color.White)) { Text(if (isListening) "⏹️ Voz" else "📝 Texto STT", color = if (isListening) Color.White else Color(0xFF37474F)) }

                                        Button(
                                            onClick = {
                                                val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                    return@Button
                                                }
                                                if (isDSPActive) {
                                                    motorDSP.detenerAnalisis(context, "Simulación DSP")
                                                    isDSPActive = false
                                                } else {
                                                    encenderDSP()
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            enabled = !isListening,
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isDSPActive) Color(0xFFD32F2F) else Color(0xFF00897B))
                                        ) { Text(if (isDSPActive) "⏹️ Stop DSP" else "🎛️ DSP Activo") }
                                    }
                                    if (textoEscuchado.isNotEmpty()) Text("Reconocido: \"$textoEscuchado\" ($ultimaCalificacion%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        FaseGuion.RESULTADOS -> {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("🎉 ¡Guion Completado!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Validación Emocional", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(modifier = Modifier.height(12.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Antes", fontSize = 12.sp); Text(emocionInicial.icono, fontSize = 30.sp); Text(emocionInicial.texto, fontSize = 12.sp) }
                                            Text("➡️", fontSize = 24.sp)
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Después", fontSize = 12.sp); Text(emocionFinal.icono, fontSize = 30.sp); Text(emocionFinal.texto, fontSize = 12.sp) }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        if (emocionInicial == EstadoEmocional.ANSIOSO && emocionFinal != EstadoEmocional.ANSIOSO) Text("¡Excelente! Notaste cómo la ansiedad bajó al enfrentar la situación.", textAlign = TextAlign.Center, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                        else Text("Recuerda que cada práctica fortalece tu confianza y tus redes neuronales.", textAlign = TextAlign.Center)
                                    }
                                }
                                val totalTurnos = guionSeleccionado!!.dialogos.count { !it.esDeApp }
                                val calificacionFinal = if (totalTurnos > 0) ((aciertosGuion.toFloat() / totalTurnos) * 100).toInt() else 100
                                Text("Precisión de habla: $calificacionFinal%", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Button(onClick = {
                                    val scoreOases = calcularScorePsicosocial(emocionInicial, emocionFinal)
                                    ProgresoStorage.guardarSesionMultidimensional(
                                        context = context,
                                        "Simulación (${guionSeleccionado!!.titulo})",
                                        aciertos = aciertosGuion,
                                        erroresGuion,
                                        calificacion = calificacionFinal,
                                        impactoPsicosocial = scoreOases
                                    )
                                    faseGuion = FaseGuion.SELECCION }) { Text("Guardar y Volver") }
                            }
                        }
                    }
                }

                SimulacionSituaciones.ESCENARIOS -> {
                    if (escenariosCargados.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Text("Cargando escenarios...") }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            val (titulo, audioSimulado, respuestaSugerida) = escenariosCargados[escenarioSeleccionadoIndex]

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Escenarios Rápidos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                OutlinedButton(onClick = { if (isListening) speechRecognizer?.stopListening(); motorDSP.detenerAnalisis(null); isDSPActive = false; tts?.stop(); mostrarPausaRespiracion = true }) { Text("🌬️ Respirar", color = Color(0xFF00838F)) }
                            }

                            Card(modifier = Modifier.fillMaxWidth().offset(x = offsetX.value.dp), colors = CardDefaults.cardColors(containerColor = if (tensionDetectada) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.secondaryContainer)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("🗣️ Interlocutor: \"$audioSimulado\"", fontSize = 14.sp)
                                    Text("🎯 Respuesta objetivo: \"$respuestaSugerida\"", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Button(onClick = { reproducirAudioApp(audioSimulado) }) { Text("🔊 Escuchar Interlocutor") }
                                }
                            }

                            if (isDSPActive) {
                                ControlesAAF(motorDSP = motorDSP)
                                BiofeedbackVisualAvanzadoDSP(metricasDSP)
                                MonitorFonologicoYCAA(motorDSP = motorDSP, palabraObjetivoActual = respuestaSugerida, textoEscuchado = textoEscuchado)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { if (isListening) speechRecognizer?.stopListening() else iniciarEscuchaSTT(respuestaSugerida, false) }, modifier = Modifier.weight(1f), enabled = !isDSPActive, colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color(0xFF0277BD))) { Text(if (isListening) "⏹️ Voz" else "📝 Texto") }
                                Button(
                                    onClick = {
                                        if (isDSPActive) {
                                            motorDSP.detenerAnalisis(context, "Escenario DSP")
                                            isDSPActive = false
                                        } else {
                                            encenderDSP()
                                        }
                                    },
                                    modifier = Modifier.weight(1f), enabled = !isListening, colors = ButtonDefaults.buttonColors(containerColor = if (isDSPActive) Color(0xFFD32F2F) else Color(0xFF00897B))) { Text(if (isDSPActive) "⏹️ Stop DSP" else "🎛️ DSP") }
                            }

                            if (textoEscuchado.isNotEmpty()) Text("\"$textoEscuchado\" ($ultimaCalificacion%)", fontWeight = FontWeight.Bold)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                OutlinedButton(onClick = { if (escenarioSeleccionadoIndex > 0) escenarioSeleccionadoIndex-- }, enabled = escenarioSeleccionadoIndex > 0) { Text("Anterior") }
                                OutlinedButton(onClick = { if (escenarioSeleccionadoIndex < escenariosCargados.size - 1) escenarioSeleccionadoIndex++ else { escenariosCargados = GestorEjercicios.obtenerEscenarios(context, "SimulacionSituaciones/escenarios.json", cantidadCargaAleatoria); escenarioSeleccionadoIndex = 0 } }) { Text("Siguiente") }
                            }
                        }
                    }
                }

                SimulacionSituaciones.EVOCACION_LEXICA -> {
                    if (diccionariosEvocacion.isEmpty() || diccionarioAntonimos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { Text("Cargando diccionarios...") }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Card(modifier = Modifier.fillMaxWidth().offset(x = offsetX.value.dp), colors = CardDefaults.cardColors(containerColor = if (tensionDetectada) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.primaryContainer)) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (estadoEvocacion == "INICIO") {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                            FilterChip(selected = modoEvocacion == "CATEGORIAS", onClick = { modoEvocacion = "CATEGORIAS"; categoriaActual = diccionariosEvocacion.keys.random() }, label = { Text("Categorías") })
                                            FilterChip(selected = modoEvocacion == "ANTONIMOS", onClick = { modoEvocacion = "ANTONIMOS"; antonimoActual = diccionarioAntonimos.keys.random() }, label = { Text("Antónimos") })
                                        }
                                    } else {
                                        Text(if (modoEvocacion == "CATEGORIAS") categoriaActual else antonimoActual, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(if (pausarTimer) "RESCATE RESPIRATORIO" else "Tiempo Restante", color = if (pausarTimer) Color.Red else Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(progress = { tiempoRestante.toFloat() / TIEMPO_MAXIMO_EVOCACION }, modifier = Modifier.size(140.dp), color = when { tiempoRestante > 15 -> Color(0xFF4CAF50); tiempoRestante > 5 -> Color(0xFFFFA000); else -> Color(0xFFD32F2F) }, strokeWidth = 12.dp, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                                    Text("00:${tiempoRestante.toString().padStart(2, '0')}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = if (tiempoRestante <= 10) Color.Red else MaterialTheme.colorScheme.onBackground)
                                }
                                if (pistaActual.isNotEmpty() && estadoEvocacion == "JUGANDO") Text(pistaActual, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17), modifier = Modifier.padding(top = 16.dp))
                                Spacer(modifier = Modifier.height(24.dp))
                                when (estadoEvocacion) {
                                    "INICIO" -> Button(onClick = { tiempoRestante = TIEMPO_MAXIMO_EVOCACION; aciertosEvocacion = 0; erroresEvocacion = 0; estadoEvocacion = "JUGANDO" }, modifier = Modifier.fillMaxWidth(0.7f).height(50.dp)) { Text("Iniciar Desafío", fontSize = 18.sp) }
                                    "FIN" -> { Text("Aciertos: $aciertosEvocacion | Errores: $erroresEvocacion", fontSize = 16.sp); Button(onClick = { ProgresoStorage.guardarSesionMultidimensional(context, "Evocación ($modoEvocacion)", aciertosEvocacion, erroresEvocacion, 100); estadoEvocacion = "INICIO" }, modifier = Modifier.padding(top = 8.dp)) { Text("Guardar Progreso") } }
                                    "JUGANDO" -> {
                                        if (textoEscuchado.isNotEmpty()) Text(textoEscuchado, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))
                                        if (isDSPActive) {
                                            ControlesAAF(motorDSP = motorDSP)
                                            BiofeedbackVisualAvanzadoDSP(metricasDSP); Spacer(modifier = Modifier.height(12.dp))
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { if (isListening) speechRecognizer?.stopListening() else iniciarEscuchaTextoSemantico() }, modifier = Modifier.weight(1f).height(56.dp), enabled = !isDSPActive) { Text(if (isListening) "⏹️ Escuchando..." else "🎤 Hablar") }
                                            Button(
                                                onClick = {
                                                    if (isDSPActive) {
                                                        motorDSP.detenerAnalisis(null)
                                                        isDSPActive = false
                                                    } else {
                                                        encenderDSP()
                                                    }
                                                }, modifier = Modifier.weight(1f).height(56.dp), enabled = !isListening) { Text(if (isDSPActive) "⏹️ Stop DSP" else "🎛️ DSP (Fluidez)") }
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