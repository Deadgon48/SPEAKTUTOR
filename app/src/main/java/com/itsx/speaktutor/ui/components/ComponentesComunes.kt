package com.itsx.speaktutor.ui.components

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

// 1. Control deslizante de velocidad
@Composable
fun ControlVelocidadTts(
    velocidadActual: Float,
    onVelocidadChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Velocidad de reproducción",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", velocidadActual)}x",
                    fontSize = 14.sp
                )
            }
            Slider(
                value = velocidadActual,
                onValueChange = onVelocidadChange,
                valueRange = 0.5f..1.5f,
                steps = 9
            )
        }
    }
}


// 2. Componente Reutilizable de Grabadora de Progreso
@Composable
fun GrabadoraProgresoComponent() {
    val context = LocalContext.current
    val archivoAudio = remember {
        File(context.externalCacheDir, "audio_progreso_fluidez.3gp").absolutePath
    }

    var estaGrabando by remember { mutableStateOf(false) }
    var estaReproduciendo by remember { mutableStateOf(false) }
    var tieneGrabacion by remember { mutableStateOf(false) }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Grabadora de Progreso", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (estaGrabando) "Grabando tu voz..." else if (estaReproduciendo) "Reproduciendo..." else "Grábate para escuchar tu fluidez",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Grabar / Detener Grabación
                Button(
                    onClick = {
                        if (!estaGrabando) {
                            try {
                                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    MediaRecorder(context)
                                } else {
                                    @Suppress("DEPRECATION")
                                    MediaRecorder()
                                }.apply {
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                    setOutputFile(archivoAudio)
                                    prepare()
                                    start()
                                }
                                mediaRecorder = recorder
                                estaGrabando = true
                                tieneGrabacion = false
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            try {
                                mediaRecorder?.stop()
                                mediaRecorder?.release()
                                mediaRecorder = null
                                estaGrabando = false
                                tieneGrabacion = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (estaGrabando) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = if (estaGrabando) "🛑 Detener" else "🎙️ Grabar")
                }

                // Botón Reproducir Grabación Propia
                Button(
                    onClick = {
                        if (!estaReproduciendo && tieneGrabacion) {
                            try {
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(archivoAudio)
                                    prepare()
                                    start()
                                    estaReproduciendo = true
                                    setOnCompletionListener {
                                        estaReproduciendo = false
                                        release()
                                        mediaPlayer = null
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else if (estaReproduciendo) {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                            estaReproduciendo = false
                        }
                    },
                    enabled = tieneGrabacion && !estaGrabando,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text(text = if (estaReproduciendo) "⏹️ Parar" else "▶️ Escuchar")
                }
            }
        }
    }
}

// 3. Componente Metrónomo Visual y Constante
@Composable
fun MetronomoReutilizableComponent() {
    var bpm by remember { mutableStateOf(60f) }
    var activo by remember { mutableStateOf(false) }
    var parpadeo by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator?.release()
        }
    }

    val currentBpm by rememberUpdatedState(bpm)

    LaunchedEffect(activo, bpm) {
        if (activo) {
            while (activo) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                parpadeo = true
                delay(100)
                parpadeo = false

                val intervaloMs = (60000 / currentBpm).toLong()
                val tiempoRestante = intervaloMs - 100
                if (tiempoRestante > 0) {
                    delay(tiempoRestante)
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Metrónomo de Ritmo",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Indicador Visual (Círculo parpadeante)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        if (parpadeo) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (parpadeo) "♪" else "•",
                    fontSize = 36.sp,
                    color = if (parpadeo) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Velocidad: ${bpm.toInt()} BPM",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = bpm,
                onValueChange = { bpm = it },
                valueRange = 40f..140f,
                steps = 20,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { activo = !activo },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (activo) "Detener Metrónomo" else "Iniciar Metrónomo",
                    fontSize = 16.sp
                )
            }
        }
    }
}



@Composable
fun BarraNavegacionModulos(
    onNavigateTarjetas: () -> Unit,
    onNavigateMetronomo: () -> Unit,
    onNavigateHablaEstirada: () -> Unit,
    onNavigateRitmoFluidez: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MiniTarjetaBoton(
            titulo = "Panel Principal",
            colorFondo = listOf(Color(0xFF37474F), Color(0xFF546E7A)),
            onClick = onNavigateTarjetas
        )
        MiniTarjetaBoton(
            titulo = "Metrónomo",
            colorFondo = listOf(Color(0xFF4A148C), Color(0xFF8E24AA)),
            onClick = onNavigateMetronomo
        )
        MiniTarjetaBoton(
            titulo = "Habla Estirada",
            colorFondo = listOf(Color(0xFF006064), Color(0xFF00ACC1)),
            onClick = onNavigateHablaEstirada
        )
        MiniTarjetaBoton(
            titulo = "Ritmo y Fluidez",
            colorFondo = listOf(Color(0xFF1B5E20), Color(0xFF43A047)),
            onClick = onNavigateRitmoFluidez
        )
    }
}

@Composable
fun MiniTarjetaBoton(
    titulo: String,
    colorFondo: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colorFondo))
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = titulo,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}