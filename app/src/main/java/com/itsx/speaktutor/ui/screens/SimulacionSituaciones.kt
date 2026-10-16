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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.navigation.Screen
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.logic.EstadoVozDSP
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BiofeedbackVisualAvanzadoDSP
import com.itsx.speaktutor.ui.components.HomeIcon
import com.itsx.speaktutor.ui.components.MonitorFonologicoYCAA

enum class SimulacionSituaciones {
    MENU_PRINCIPAL, ESCENARIOS, GUIONES, EVOCACION_LEXICA
}

data class TurnoGuion(val rol: String, val texto: String, val esDeApp: Boolean)

// DICCIONARIOS SEMÁNTICOS AMPLIADOS
val diccionariosEvocacion = mapOf(
    "ANIMALES" to listOf("perro", "gato", "león", "tigre", "elefante", "pájaro", "oso", "caballo", "vaca", "cerdo", "lobo", "zorro", "conejo", "serpiente", "mono", "rana", "pez", "jirafa", "rinoceronte", "hipopótamo", "cocodrilo", "águila", "ballena", "delfín", "tiburón", "tortuga", "pingüino"),
    "COLORES" to listOf("rojo", "azul", "verde", "amarillo", "blanco", "negro", "naranja", "morado", "rosa", "café", "marrón", "gris", "celeste", "violeta", "turquesa", "beige", "dorado", "plateado"),
    "FRUTAS" to listOf("manzana", "plátano", "naranja", "uva", "pera", "fresa", "sandía", "melón", "mango", "piña", "kiwi", "limón", "papaya", "cereza", "ciruela", "durazno", "frambuesa", "arándano", "coco", "guayaba"),
    "PAÍSES" to listOf("méxico", "españa", "argentina", "colombia", "chile", "perú", "francia", "italia", "alemania", "japón", "china", "brasil", "canadá", "ecuador", "venezuela", "bolivia", "uruguay", "paraguay", "rusia", "india", "australia"),
    "PROFESIONES" to listOf("doctor", "médico", "ingeniero", "abogado", "profesor", "maestro", "arquitecto", "enfermera", "policía", "bombero", "carpintero", "piloto", "dentista", "cocinero", "chef", "mecánico", "electricista", "periodista", "psicólogo", "veterinario"),
    "DEPORTES" to listOf("fútbol", "basquetbol", "béisbol", "tenis", "natación", "atletismo", "boxeo", "voleibol", "golf", "ciclismo", "gimnasia", "rugby", "surf", "esquí", "taekwondo", "karate"),
    "VERDURAS" to listOf("tomate", "cebolla", "ajo", "zanahoria", "papa", "lechuga", "brócoli", "espinaca", "calabaza", "pepino", "chile", "pimiento", "apio", "aguacate", "chícharo"),
    "HERRAMIENTAS" to listOf("martillo", "desarmador", "destornillador", "pinzas", "alicate", "llave", "taladro", "sierra", "clavo", "tornillo", "tuerca", "metro", "cincel"),
    "VEHÍCULOS" to listOf("coche", "auto", "automóvil", "autobús", "camión", "motocicleta", "moto", "bicicleta", "tren", "avión", "barco", "helicóptero", "submarino", "yate"),
    "INSTRUMENTOS" to listOf("guitarra", "piano", "violín", "batería", "flauta", "trompeta", "saxofón", "bajo", "teclado", "violonchelo", "arpa", "acordeón", "clarinete"),
    "ROPA" to listOf("camisa", "camiseta", "pantalón", "jeans", "zapato", "tenis", "chamarra", "chaqueta", "abrigo", "sombrero", "gorra", "calcetín", "vestido", "falda", "suéter", "bufanda", "guante"),
    "MUEBLES" to listOf("silla", "mesa", "sillón", "sofá", "cama", "ropero", "armario", "escritorio", "estante", "librero", "buró", "espejo"),
    "ACCIONES" to listOf("correr", "saltar", "nadar", "leer", "caminar", "dormir", "comer", "escribir", "bailar", "cantar", "jugar", "estudiar", "viajar"),
    "EMOCIONES" to listOf("alegría", "tristeza", "enojo", "sorpresa", "miedo", "amor", "calma", "ansiedad", "paz", "angustia", "felicidad")
)

// DICCIONARIO DE ANTÓNIMOS
val diccionarioAntonimos = mapOf(
    "Día" to listOf("noche"),
    "Frío" to listOf("calor", "caliente"),
    "Rápido" to listOf("lento", "despacio"),
    "Feliz" to listOf("triste", "infeliz"),
    "Arriba" to listOf("abajo"),
    "Blanco" to listOf("negro"),
    "Grande" to listOf("chico", "pequeño"),
    "Lleno" to listOf("vacío"),
    "Largo" to listOf("corto"),
    "Caro" to listOf("barato")
)

fun calcularPrecisionHabla(textoEsperado: String, textoReconocido: String): Int {
    if (textoReconocido.isBlank()) return 0
    val palabrasEsperadas = textoEsperado.lowercase().replace(Regex("[^a-záéíóúñ ]"), "").split("\\s+".toRegex())
    val palabrasReconocidas = textoReconocido.lowercase().replace(Regex("[^a-záéíóúñ ]"), "").split("\\s+".toRegex())

    var coincidencias = 0
    for (palabra in palabrasReconocidas) {
        if (palabrasEsperadas.contains(palabra)) coincidencias++
    }
    val porcentaje = (coincidencias.toFloat() / palabrasEsperadas.size.coerceAtLeast(1)) * 100
    return porcentaje.toInt().coerceIn(0, 100)
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

    var indiceGuionActual by remember { mutableStateOf(0) }
    var guionTerminado by remember { mutableStateOf(false) }
    var aciertosGuion by remember { mutableStateOf(0) }
    var erroresGuion by remember { mutableStateOf(0) }

    var escenarioSeleccionadoIndex by remember { mutableStateOf(0) }

    val motorDSP = remember { MotorAudioDSP() }
    val metricasDSP by motorDSP.metricasAcusticas.collectAsState()
    var isDSPActive by remember { mutableStateOf(false) }

    var tiempoRestante by remember { mutableStateOf(30) }
    var estadoEvocacion by remember { mutableStateOf("INICIO") }
    var modoEvocacion by remember { mutableStateOf("CATEGORIAS") }
    val categoriasEvocacion = diccionariosEvocacion.keys.toList()
    var categoriaActual by remember { mutableStateOf(categoriasEvocacion.random()) }
    var antonimoActual by remember { mutableStateOf(diccionarioAntonimos.keys.random()) }
    var aciertosEvocacion by remember { mutableIntStateOf(0) }
    var erroresEvocacion by remember { mutableIntStateOf(0) }

    var pistaActual by remember { mutableStateOf("") }
    var pausarTimer by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(estadoEvocacion, tiempoRestante, pausarTimer) {
        if (estadoEvocacion == "JUGANDO" && tiempoRestante > 0 && !pausarTimer) {
            delay(1000)
            tiempoRestante--

            if (tiempoRestante == 15) {
                if (modoEvocacion == "CATEGORIAS") {
                    val palabraAzar = diccionariosEvocacion[categoriaActual]?.random() ?: ""
                    if (palabraAzar.isNotEmpty()) pistaActual = "Pista fonológica: Empieza con '${palabraAzar.first().uppercaseChar()}'"
                } else {
                    val opuestos = diccionarioAntonimos[antonimoActual] ?: emptyList()
                    if (opuestos.isNotEmpty()) pistaActual = "Empieza con '${opuestos.first().first().uppercaseChar()}'"
                }
            }
        } else if (tiempoRestante == 0 && estadoEvocacion == "JUGANDO") {
            estadoEvocacion = "FIN"
            motorDSP.detenerAnalisis(null)
            isDSPActive = false
            if(isListening) speechRecognizer?.stopListening()
            isListening = false
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

    val listaEscenarios = listOf(
        Triple("☕ Pedir un café en la cafetería", "Hola, buenos días. ¿Qué le gustaría ordenar hoy?", "Hola, quisiera un café americano bien caliente por favor."),
        Triple("🛒 Comprar en el supermercado", "Hola, ¿encontró todo lo que buscaba o le ayudo en algo?", "Sí, encontré todo lo que buscaba, muchas gracias."),
        Triple("🚌 Preguntar dirección en la calle", "Disculpe, ¿sabe por dónde queda la estación del metro más cercana?", "Siga derecho dos cuadras y dé vuelta a la izquierda."),
        Triple("💊 Comprar medicamentos en la farmacia", "Buenas tardes, ¿en qué le puedo servir el día de hoy?", "Buenas tardes, busco una caja de analgésicos por favor."),
        Triple("🍽️ Reservar una mesa en restaurante", "Bienvenido, ¿busca mesa para cuántas personas?", "Buenas tardes, necesitamos una mesa para cuatro personas."),
        Triple("🚕 Tomar un taxi o transporte", "Hola, ¿a qué dirección lo llevamos en este viaje?", "Por favor lléveme a la estación central de autobuses.")
    )

    val dialogosGuion = listOf(
        TurnoGuion("App (Recepcionista):", "Bienvenido al hotel. ¿Tiene una reserva a su nombre?", true),
        TurnoGuion("Tú (Huésped):", "Sí, reservé una habitación individual para dos noches.", false),
        TurnoGuion("App (Recepcionista):", "Perfecto, ¿me podría proporcionar una identificación por favor?", true),
        TurnoGuion("Tú (Huésped):", "Claro que sí, aquí tiene mi credencial de elector.", false),
        TurnoGuion("App (Recepcionista):", "Excelente. Su habitación es la 304. ¿Necesita ayuda con su equipaje?", true),
        TurnoGuion("Tú (Huésped):", "No, gracias, puedo llevarlo yo mismo. Muy amable.", false)
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
            motorDSP.detenerAnalisis(null)
        }
    }

    fun reproducirAudioApp(texto: String) {
        if (ttsInitialized) tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun avanzarTurnoGuion() {
        if (indiceGuionActual < dialogosGuion.size - 1) {
            indiceGuionActual++
            val siguienteTurno = dialogosGuion[indiceGuionActual]
            if (siguienteTurno.esDeApp) reproducirAudioApp(siguienteTurno.texto)
        } else {
            guionTerminado = true
            val totalTurnosUsuario = dialogosGuion.count { !it.esDeApp }
            val calificacionFinal = if (totalTurnosUsuario > 0) ((aciertosGuion.toFloat() / totalTurnosUsuario) * 100).toInt().coerceIn(0, 100) else 100
            ProgresoStorage.guardarSesionMultidimensional(context = context, modulo = "Simulación (Guión Hotel)", aciertos = aciertosGuion, erroresTotales = erroresGuion, calificacion = calificacionFinal)
        }
    }

    fun iniciarEscuchaTextoSemantico() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return }

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
                        if (modoEvocacion == "ANTONIMOS") antonimoActual = diccionarioAntonimos.keys.random()
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
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return }
        if (isListening) { speechRecognizer?.stopListening(); isListening = false; return }

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
                val textoPronunciado = matches?.firstOrNull()?.trim() ?: ""

                if (textoPronunciado.isBlank()) {
                    textoEscuchado = "No escuché nada. Intenta hablar más cerca."
                    return
                }

                textoEscuchado = textoPronunciado
                val calificacion = calcularPrecisionHabla(textoObjetivo, textoPronunciado)
                ultimaCalificacion = calificacion

                if (esGuion) {
                    if (calificacion >= 60) aciertosGuion++ else erroresGuion++
                    avanzarTurnoGuion()
                } else {
                    val aciertos = if (calificacion >= 60) 1 else 0
                    val errores = if (calificacion < 60) 1 else 0
                    ProgresoStorage.guardarSesionMultidimensional(context = context, modulo = "Simulación (Escenario)", aciertos = aciertos, erroresTotales = errores, calificacion = calificacion)
                }
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
                        text = when (seccionActual) {
                            SimulacionSituaciones.MENU_PRINCIPAL -> "Simulación de Situaciones"
                            SimulacionSituaciones.ESCENARIOS -> "Escenarios Cotidianos"
                            SimulacionSituaciones.GUIONES -> "Práctica de Guiones"
                            SimulacionSituaciones.EVOCACION_LEXICA -> "Evocación Léxica"
                        }
                    )
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
                        }
                    }) { Icon(imageVector = ArrowLeftCircleIcon, contentDescription = "Regresar", tint = MaterialTheme.colorScheme.onSurface) }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                    Icon(imageVector = HomeIcon, contentDescription = "Menú Principal", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Regresar al Menú Principal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    onNavigateTarjetasArticulacion = { navController.navigate(Screen.TarjetasArticulacion.route) }
                )
            }

            when (seccionActual) {
                SimulacionSituaciones.MENU_PRINCIPAL -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "Entrena tu fluidez en distintas situaciones:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Box(modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF0277BD), Color(0xFF00ACC1)))).clickable { seccionActual = SimulacionSituaciones.ESCENARIOS }.padding(20.dp), contentAlignment = Alignment.CenterStart) {
                            Column { Text(text = "🎧 Escenarios Cotidianos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White); Text(text = "La app evalúa tu pronunciación", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f)) }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFFEF6C00), Color(0xFFFFA726)))).clickable {
                            indiceGuionActual = 0; guionTerminado = false; aciertosGuion = 0; erroresGuion = 0; textoEscuchado = ""; seccionActual = SimulacionSituaciones.GUIONES; reproducirAudioApp(dialogosGuion[0].texto)
                        }.padding(20.dp), contentAlignment = Alignment.CenterStart) {
                            Column { Text(text = "💬 Práctica de Guiones", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White); Text(text = "Diálogo evaluado paso a paso", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f)) }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF66BB6A)))).clickable {
                            tiempoRestante = 30; estadoEvocacion = "INICIO"; categoriaActual = categoriasEvocacion.random(); seccionActual = SimulacionSituaciones.EVOCACION_LEXICA
                        }.padding(20.dp), contentAlignment = Alignment.CenterStart) {
                            Column { Text(text = "⏱️ Evocación bajo Presión", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White); Text(text = "Nombra palabras sin bloquearte", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f)) }
                        }
                    }
                }

                SimulacionSituaciones.EVOCACION_LEXICA -> {
                    val colorContainer = if (tensionDetectada) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.primaryContainer
                    Column(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp)).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = colorContainer), modifier = Modifier.fillMaxWidth().offset(x = offsetX.value.dp)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (estadoEvocacion == "INICIO") {
                                    Text("Selecciona el modo:", fontSize = 16.sp)
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        FilterChip(selected = modoEvocacion == "CATEGORIAS", onClick = { modoEvocacion = "CATEGORIAS" }, label = { Text("Categorías") })
                                        FilterChip(selected = modoEvocacion == "ANTONIMOS", onClick = { modoEvocacion = "ANTONIMOS" }, label = { Text("Antónimos") })
                                    }
                                } else {
                                    Text(if (modoEvocacion == "CATEGORIAS") "Nombra elementos de la categoría:" else "Di el antónimo de:", fontSize = 16.sp)
                                    Text(if (modoEvocacion == "CATEGORIAS") categoriaActual else antonimoActual, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Text(if (pausarTimer) "RESCATE RESPIRATORIO" else "Tiempo Restante", fontSize = 18.sp, color = if (pausarTimer) Color.Red else Color.Gray)
                        Text(
                            text = "00:${tiempoRestante.toString().padStart(2, '0')}",
                            fontSize = 60.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (tiempoRestante <= 10) Color.Red else MaterialTheme.colorScheme.onBackground
                        )

                        if (pistaActual.isNotEmpty() && estadoEvocacion == "JUGANDO") {
                            Card(modifier = Modifier.padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))) {
                                Text(pistaActual, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        when (estadoEvocacion) {
                            "INICIO" -> {
                                Button(onClick = {
                                    tiempoRestante = 30; aciertosEvocacion = 0; erroresEvocacion = 0; textoEscuchado = ""; pistaActual = ""; estadoEvocacion = "JUGANDO"
                                }, modifier = Modifier.fillMaxWidth(0.7f).height(50.dp)) { Text("Iniciar Desafío", fontSize = 18.sp) }
                            }
                            "FIN" -> {
                                Text("¡Tiempo terminado!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Text("Aciertos: $aciertosEvocacion | Errores/Bloqueos: $erroresEvocacion", fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))

                                Button(onClick = {
                                    val total = aciertosEvocacion + erroresEvocacion
                                    val calificacion = if (total > 0) ((aciertosEvocacion.toFloat() / total) * 100).toInt() else 0
                                    ProgresoStorage.guardarSesionMultidimensional(context, "Evocación ($modoEvocacion)", aciertosEvocacion, erroresEvocacion, calificacion)

                                    tiempoRestante = 30; categoriaActual = categoriasEvocacion.random(); estadoEvocacion = "INICIO"
                                }, modifier = Modifier.fillMaxWidth(0.8f)) { Text("Guardar Progreso") }
                            }
                            "JUGANDO" -> {
                                if (textoEscuchado.isNotEmpty()) {
                                    Text(textoEscuchado, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (isDSPActive) {
                                    BiofeedbackVisualAvanzadoDSP(metricas = metricasDSP)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { if (isListening) speechRecognizer?.stopListening() else iniciarEscuchaTextoSemantico() },
                                        modifier = Modifier.weight(1f).height(60.dp), enabled = !isDSPActive,
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color(0xFF1976D2))
                                    ) { Text(if (isListening) "⏹️ Escuchando..." else "🎤 Hablar") }

                                    Button(
                                        onClick = {
                                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                            if (permissionCheck != PackageManager.PERMISSION_GRANTED) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return@Button }
                                            if (isDSPActive) { motorDSP.detenerAnalisis(null); isDSPActive = false } else { motorDSP.detenerAnalisis(null); motorDSP.iniciarAnalisisDSP(); isDSPActive = true }
                                        },
                                        modifier = Modifier.weight(1f).height(60.dp), enabled = !isListening,
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isDSPActive) Color.Red else Color(0xFF00897B))
                                    ) { Text(if (isDSPActive) "⏹️ Stop DSP" else "🎛️ DSP (Fluidez)") }
                                }
                            }
                        }
                    }
                }

                SimulacionSituaciones.ESCENARIOS -> {
                    val (titulo, audioSimulado, respuestaSugerida) = listaEscenarios[escenarioSeleccionadoIndex]
                    val colorContainer = if (tensionDetectada) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.secondaryContainer

                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(modifier = Modifier.fillMaxWidth().offset(x = offsetX.value.dp), colors = CardDefaults.cardColors(containerColor = colorContainer)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(text = "🗣️ Interlocutor: \"$audioSimulado\"", fontSize = 14.sp)
                                Text(text = "🎯 Respuesta objetivo: \"$respuestaSugerida\"", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Button(onClick = { reproducirAudioApp(audioSimulado) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                                    Text("🔊 Escuchar Interlocutor")
                                }
                            }
                        }

                        if (isDSPActive) {
                            BiofeedbackVisualAvanzadoDSP(metricas = metricasDSP)
                            MonitorFonologicoYCAA(motorDSP = motorDSP, palabraObjetivoActual = respuestaSugerida, textoEscuchado = textoEscuchado)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { if (isListening) speechRecognizer?.stopListening() else iniciarEscuchaSTT(respuestaSugerida, esGuion = false) },
                                modifier = Modifier.weight(1f), enabled = !isDSPActive, colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color(0xFF0277BD))
                            ) { Text(if (isListening) "⏹️ Voz" else "📝 Texto") }

                            Button(
                                onClick = {
                                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return@Button }
                                    if (isDSPActive) { motorDSP.detenerAnalisis(context, "Simulación (Escenario DSP)"); isDSPActive = false }
                                    else { motorDSP.detenerAnalisis(null); motorDSP.iniciarAnalisisDSP(); isDSPActive = true }
                                },
                                modifier = Modifier.weight(1f), enabled = !isListening, colors = ButtonDefaults.buttonColors(containerColor = if (isDSPActive) Color(0xFFD32F2F) else Color(0xFF00897B))
                            ) { Text(if (isDSPActive) "⏹️ Stop DSP" else "🎛️ DSP") }
                        }

                        if (textoEscuchado.isNotEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = "Lo que la app escuchó:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "\"$textoEscuchado\"", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Precisión / Fluidez: $ultimaCalificacion%", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(onClick = { if (escenarioSeleccionadoIndex > 0) escenarioSeleccionadoIndex-- }, enabled = escenarioSeleccionadoIndex > 0) { Text("Anterior") }
                            OutlinedButton(onClick = { if (escenarioSeleccionadoIndex < listaEscenarios.size - 1) escenarioSeleccionadoIndex++ }, enabled = escenarioSeleccionadoIndex < listaEscenarios.size - 1) { Text("Siguiente Escenario") }
                        }
                    }
                }

                SimulacionSituaciones.GUIONES -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (guionTerminado) {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(text = "🎉 ¡Guión Completado!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Turnos acertados: $aciertosGuion | Errores: $erroresGuion", fontSize = 14.sp)
                                    Button(onClick = {
                                        indiceGuionActual = 0; guionTerminado = false; aciertosGuion = 0; erroresGuion = 0; textoEscuchado = ""; reproducirAudioApp(dialogosGuion[0].texto)
                                    }) { Text("Reiniciar Práctica") }
                                }
                            }
                        } else {
                            val turnoActual = dialogosGuion[indiceGuionActual]

                            val bgBrush = if (tensionDetectada && !turnoActual.esDeApp) {
                                Brush.linearGradient(listOf(Color(0xFFC62828), Color(0xFFD32F2F)))
                            } else if (turnoActual.esDeApp) {
                                Brush.linearGradient(listOf(Color(0xFFEF6C00), Color(0xFFFFA726)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF546E7A)))
                            }

                            Box(modifier = Modifier.fillMaxWidth().offset(x = if (!turnoActual.esDeApp) offsetX.value.dp else 0.dp).clip(RoundedCornerShape(16.dp)).background(bgBrush).padding(20.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(text = "Turno ${indiceGuionActual + 1} de ${dialogosGuion.size}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text(text = turnoActual.rol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                                    Text(text = turnoActual.texto, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)

                                    if (turnoActual.esDeApp) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { reproducirAudioApp(turnoActual.texto) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)), modifier = Modifier.weight(1f)) { Text("🔊 Escuchar", color = Color.White) }
                                            Button(onClick = { avanzarTurnoGuion() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), modifier = Modifier.weight(1f)) { Text("Siguiente ➡️", color = Color(0xFFEF6C00), fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            }

                            if (!turnoActual.esDeApp) {
                                if (isDSPActive) {
                                    BiofeedbackVisualAvanzadoDSP(metricas = metricasDSP)
                                    MonitorFonologicoYCAA(motorDSP = motorDSP, palabraObjetivoActual = turnoActual.texto, textoEscuchado = textoEscuchado)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { if (isListening) speechRecognizer?.stopListening() else iniciarEscuchaSTT(turnoActual.texto, esGuion = true) },
                                        modifier = Modifier.weight(1f), enabled = !isDSPActive, colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color.White)
                                    ) { Text(if (isListening) "⏹️ Voz" else "📝 Texto", color = if (isListening) Color.White else Color(0xFF37474F)) }

                                    Button(
                                        onClick = {
                                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                            if (permissionCheck != PackageManager.PERMISSION_GRANTED) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return@Button }
                                            if (isDSPActive) { motorDSP.detenerAnalisis(context, "Simulación (Guión DSP)"); isDSPActive = false }
                                            else { motorDSP.detenerAnalisis(null); motorDSP.iniciarAnalisisDSP(); isDSPActive = true }
                                        },
                                        modifier = Modifier.weight(1f), enabled = !isListening, colors = ButtonDefaults.buttonColors(containerColor = if (isDSPActive) Color(0xFFD32F2F) else Color(0xFF00897B))
                                    ) { Text(if (isDSPActive) "⏹️ DSP" else "🎛️ DSP", color = Color.White) }
                                }
                            }

                            if (textoEscuchado.isNotEmpty() && !turnoActual.esDeApp) {
                                Text(text = "Reconocido: \"$textoEscuchado\" ($ultimaCalificacion%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}