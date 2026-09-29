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
import java.util.*

// *** INICIO IMPORTACIONES DSP ***
import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BiofeedbackVisualAvanzadoDSP
import com.itsx.speaktutor.ui.components.HomeIcon

// *** FIN IMPORTACIONES DSP ***

enum class SimulacionSituaciones {
    MENU_PRINCIPAL, ESCENARIOS, GUIONES
}

data class TurnoGuion(
    val rol: String,
    val texto: String,
    val esDeApp: Boolean
)

fun calcularPrecisionHabla(textoEsperado: String, textoReconocido: String): Int {
    if (textoReconocido.isBlank()) return 0
    val palabrasEsperadas = textoEsperado.lowercase().replace(Regex("[^a-záéíóúñ ]"), "").split("\\s+".toRegex())
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulacionSituacionesScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var seccionActual by remember { mutableStateOf(SimulacionSituaciones.MENU_PRINCIPAL) }

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

    // *** INICIO ESTADOS DSP ***
    val motorDSP = remember { MotorAudioDSP() }
    val metricasDSP by motorDSP.metricasAcusticas.collectAsState()
    var isDSPActive by remember { mutableStateOf(false) }
    // *** FIN ESTADOS DSP ***

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

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
            // *** INICIO LIMPIEZA DSP ***
            motorDSP.detenerAnalisis(null)
            // *** FIN LIMPIEZA DSP ***
        }
    }

    fun reproducirAudioApp(texto: String) {
        if (ttsInitialized) {
            tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun avanzarTurnoGuion() {
        if (indiceGuionActual < dialogosGuion.size - 1) {
            indiceGuionActual++
            val siguienteTurno = dialogosGuion[indiceGuionActual]
            if (siguienteTurno.esDeApp) {
                reproducirAudioApp(siguienteTurno.texto)
            }
        } else {
            guionTerminado = true
            val totalTurnosUsuario = dialogosGuion.count { !it.esDeApp }
            val calificacionFinal = if (totalTurnosUsuario > 0) {
                ((aciertosGuion.toFloat() / totalTurnosUsuario) * 100).toInt().coerceIn(0, 100)
            } else 100

            ProgresoStorage.guardarSesion(
                context = context,
                dificultad = "Simulación (Guión Hotel)",
                aciertos = aciertosGuion,
                errores = erroresGuion,
                calificacion = calificacionFinal
            )
        }
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
                val textoPronunciado = matches?.get(0) ?: ""
                textoEscuchado = textoPronunciado
                val calificacion = calcularPrecisionHabla(textoObjetivo, textoPronunciado)
                ultimaCalificacion = calificacion

                if (esGuion) {
                    if (calificacion >= 60) aciertosGuion++ else erroresGuion++
                    avanzarTurnoGuion()
                } else {
                    val aciertos = if (calificacion >= 60) 1 else 0
                    val errores = if (calificacion < 60) 1 else 0

                    ProgresoStorage.guardarSesion(
                        context = context,
                        dificultad = "Simulación (Escenario)",
                        aciertos = aciertos,
                        errores = errores,
                        calificacion = calificacion
                    )
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
                            SimulacionSituaciones.GUIONES -> "Práctica de Guiones e Interlocutor"
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
                            seccionActual = SimulacionSituaciones.MENU_PRINCIPAL
                        }
                    }) {
                        Icon(imageVector = ArrowLeftCircleIcon, contentDescription = "Regresar", tint = MaterialTheme.colorScheme.onSurface)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                onNavigateSimulacionSituaciones = { /* Ya estás aquí */ },
                onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                onNavigateProgreso = { navController.navigate(Screen.Progreso.route) },
                onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) }
            )

            when (seccionActual) {
                SimulacionSituaciones.MENU_PRINCIPAL -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "Entrena tu fluidez con reconocimiento automático de voz:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Text(text = "🎧 Escenarios Cotidianos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "La app evalúa tu pronunciación palabra por palabra", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFEF6C00), Color(0xFFFFA726))))
                                .clickable {
                                    indiceGuionActual = 0
                                    guionTerminado = false
                                    aciertosGuion = 0
                                    erroresGuion = 0
                                    textoEscuchado = ""
                                    seccionActual = SimulacionSituaciones.GUIONES
                                    reproducirAudioApp(dialogosGuion[0].texto)
                                }
                                .padding(20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(text = "💬 Práctica de Guiones e Interlocutor", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "Diálogo evaluado automáticamente paso a paso", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                SimulacionSituaciones.ESCENARIOS -> {
                    val (titulo, audioSimulado, respuestaSugerida) = listaEscenarios[escenarioSeleccionadoIndex]

                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(text = "🗣️ Interlocutor: \"$audioSimulado\"", fontSize = 14.sp)
                                Text(text = "🎯 Respuesta objetivo: \"$respuestaSugerida\"", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Button(
                                    onClick = { reproducirAudioApp(audioSimulado) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("🔊 Escuchar Interlocutor")
                                }
                            }
                        }

                        // *** INICIO COMPONENTE Y BOTONES DSP EN ESCENARIOS ***
                        if (isDSPActive) {
                            BiofeedbackVisualAvanzadoDSP(metricas = metricasDSP)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { if (isListening) speechRecognizer?.stopListening() else iniciarEscuchaSTT(respuestaSugerida, esGuion = false) },
                                modifier = Modifier.weight(1f),
                                enabled = !isDSPActive,
                                colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color(0xFF0277BD))
                            ) {
                                Text(if (isListening) "⏹️ Voz" else "📝 Texto")
                            }

                            Button(
                                onClick = {
                                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        return@Button
                                    }
                                    if (isDSPActive) {
                                        motorDSP.detenerAnalisis(context, "Simulación (Escenario DSP)")
                                        isDSPActive = false
                                    } else {
                                        motorDSP.detenerAnalisis(null)
                                        motorDSP.iniciarAnalisisDSP()
                                        isDSPActive = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isListening,
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDSPActive) Color(0xFFD32F2F) else Color(0xFF00897B))
                            ) {
                                Text(if (isDSPActive) "⏹️ Stop DSP" else "🎛️ DSP")
                            }
                        }
                        // *** FIN COMPONENTE Y BOTONES DSP EN ESCENARIOS ***

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
                            OutlinedButton(onClick = { if (escenarioSeleccionadoIndex > 0) escenarioSeleccionadoIndex-- }, enabled = escenarioSeleccionadoIndex > 0) {
                                Text("Anterior")
                            }
                            OutlinedButton(onClick = { if (escenarioSeleccionadoIndex < listaEscenarios.size - 1) escenarioSeleccionadoIndex++ }, enabled = escenarioSeleccionadoIndex < listaEscenarios.size - 1) {
                                Text("Siguiente Escenario")
                            }
                        }
                    }
                }

                SimulacionSituaciones.GUIONES -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (guionTerminado) {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(text = "🎉 ¡Guión Completado!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Turnos acertados: $aciertosGuion | Bloqueos detectados: $erroresGuion", fontSize = 14.sp)
                                    Button(onClick = {
                                        indiceGuionActual = 0; guionTerminado = false; aciertosGuion = 0; erroresGuion = 0; textoEscuchado = ""; reproducirAudioApp(dialogosGuion[0].texto)
                                    }) { Text("Reiniciar Práctica") }
                                }
                            }
                        } else {
                            val turnoActual = dialogosGuion[indiceGuionActual]

                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (turnoActual.esDeApp) Brush.linearGradient(listOf(Color(0xFFEF6C00), Color(0xFFFFA726))) else Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF546E7A)))).padding(20.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(text = "Turno ${indiceGuionActual + 1} de ${dialogosGuion.size}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                    Text(text = turnoActual.rol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                                    Text(text = turnoActual.texto, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)

                                    if (turnoActual.esDeApp) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { reproducirAudioApp(turnoActual.texto) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)), modifier = Modifier.weight(1f)) {
                                                Text("🔊 Escuchar", color = Color.White)
                                            }
                                            Button(onClick = { avanzarTurnoGuion() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), modifier = Modifier.weight(1f)) {
                                                Text("Siguiente ➡️", color = Color(0xFFEF6C00), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            if (!turnoActual.esDeApp) {
                                // *** INICIO COMPONENTE Y BOTONES DSP EN GUIONES ***
                                if (isDSPActive) {
                                    BiofeedbackVisualAvanzadoDSP(metricas = metricasDSP)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { if (isListening) speechRecognizer?.stopListening() else iniciarEscuchaSTT(turnoActual.texto, esGuion = true) },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isDSPActive,
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color.White)
                                    ) {
                                        Text(if (isListening) "⏹️ Voz" else "📝 Texto", color = if (isListening) Color.White else Color(0xFF37474F))
                                    }

                                    Button(
                                        onClick = {
                                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                return@Button
                                            }
                                            if (isDSPActive) {
                                                motorDSP.detenerAnalisis(context, "Simulación (Guión DSP)")
                                                isDSPActive = false
                                            } else {
                                                motorDSP.detenerAnalisis(null)
                                                motorDSP.iniciarAnalisisDSP()
                                                isDSPActive = true
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isListening,
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isDSPActive) Color(0xFFD32F2F) else Color(0xFF00897B))
                                    ) {
                                        Text(if (isDSPActive) "⏹️ DSP" else "🎛️ DSP", color = Color.White)
                                    }
                                }
                                // *** FIN COMPONENTE Y BOTONES DSP EN GUIONES ***
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