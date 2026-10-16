package com.itsx.speaktutor.ui.components

import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

import com.itsx.speaktutor.logic.GestorConcienciaFonologica
import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.logic.EstadoVozDSP

@Composable
fun MonitorFonologicoYCAA(
    motorDSP: MotorAudioDSP,
    palabraObjetivoActual: String,
    textoEscuchado: String = "", // <-- NUEVO PARÁMETRO
    speechRecognizer: SpeechRecognizer? = null
) {
    val context = LocalContext.current
    val metricasDSP by motorDSP.metricasAcusticas.collectAsState()

    var mostrarOverlayCAA by remember { mutableStateOf(false) }
    var mostrarNotificacionRoja by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // Almacenaremos la única palabra en la que se atascó
    var palabraAisladaCAA by remember { mutableStateOf("") }

    DisposableEffect(context) {
        tts = TextToSpeech(context) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = Locale("es", "MX") }
        onDispose { tts?.shutdown() }
    }

    LaunchedEffect(metricasDSP.estado, palabraObjetivoActual) {
        if (metricasDSP.estado == EstadoVozDSP.BLOQUEO_TONICO || metricasDSP.estado == EstadoVozDSP.BLOQUEO_CLONICO) {

            // Mandamos guardar la palabra
            GestorConcienciaFonologica.guardarGrafemaProblematico(context, palabraObjetivoActual, textoEscuchado)

            // Extraemos la palabra para mostrarla en los cuadros visuales (aislamos la frase)
            val regex = Regex("[^a-zA-ZáéíóúÁÉÍÓÚñÑ ]")
            val palabrasObj = palabraObjetivoActual.replace(regex, "").split("\\s+".toRegex()).filter { it.isNotBlank() }
            val palabrasEsc = textoEscuchado.replace(regex, "").split("\\s+".toRegex()).filter { it.isNotBlank() }

            palabraAisladaCAA = if (palabrasEsc.isNotEmpty() && palabrasEsc.size < palabrasObj.size) {
                palabrasObj[palabrasEsc.size]
            } else {
                palabrasObj.maxByOrNull { it.length } ?: palabrasObj.firstOrNull() ?: ""
            }

            // Disparar Notificación Roja
            mostrarNotificacionRoja = true

            delay(4000)

            if (metricasDSP.estado == EstadoVozDSP.BLOQUEO_TONICO ||
                metricasDSP.estado == EstadoVozDSP.BLOQUEO_CLONICO ||
                metricasDSP.rmsEnergia > 500f) {
                mostrarNotificacionRoja = false
                mostrarOverlayCAA = true
                motorDSP.pausarAnalisisTemporal()

                // El modelo dirá SOLO la palabra aislada, no toda la frase
                tts?.speak(palabraAisladaCAA, TextToSpeech.QUEUE_FLUSH, null, null)

                delay(3000)
                mostrarOverlayCAA = false
                motorDSP.reanudarAnalisis()
            } else {
                delay(2000)
                mostrarNotificacionRoja = false
            }
        }
    }

    // NOTIFICACIÓN ROJA
    AnimatedVisibility(
        visible = mostrarNotificacionRoja && !mostrarOverlayCAA,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828), contentColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, contentDescription = "Error")
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Dificultad en '$palabraAisladaCAA'. Te sugiero repasarla en Tarjetas de Articulación.",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp
                )
            }
        }
    }

    // OVERLAY VISUAL CAA
    if (mostrarOverlayCAA) {
        Dialog(onDismissRequest = { }) {
            Box(
                modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFFFF3E0)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Soporte Visual", tint = Color(0xFFF57C00), modifier = Modifier.size(72.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Respira y relaja la garganta", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE65100))
                    Spacer(modifier = Modifier.height(12.dp))
                    // Mostrará UNA sola palabra (Ej. "HOTEL") en lugar de todo el diálogo
                    Text(text = palabraAisladaCAA.uppercase(), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E2723))
                }
            }
        }
    }
}