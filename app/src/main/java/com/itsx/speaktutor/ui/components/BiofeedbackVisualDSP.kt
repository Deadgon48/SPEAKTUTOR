package com.itsx.speaktutor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itsx.speaktutor.logic.EstadoVozDSP
import com.itsx.speaktutor.logic.ParametrosAcusticos

@Composable
fun BiofeedbackVisualAvanzadoDSP(metricas: ParametrosAcusticos) {
    val nivelNormalizado = (metricas.rmsEnergia / 10000f).coerceIn(0f, 1f)
    val suavizadoEnergia by animateFloatAsState(
        targetValue = nivelNormalizado,
        animationSpec = tween(durationMillis = 30),
        label = "energiaDSP"
    )

    // Determinar colores y textos según el estado exacto de la voz
    val (colorPrincipal, textoEstado, iconoEstado) = when (metricas.estado) {
        EstadoVozDSP.SILENCIO -> Triple(Color.Gray, "Silencio... esperando voz.", Icons.Default.MicOff)
        EstadoVozDSP.FLUIDO -> Triple(Color(0xFF00897B), "Fonación continua detectada.", Icons.Default.CheckCircle)
        EstadoVozDSP.BLOQUEO -> Triple(Color(0xFFD32F2F), "¡Bloqueo detectado! Relaja e inicia suave.", Icons.Default.Warning)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.GraphicEq, contentDescription = "DSP", tint = Color(0xFF004D40))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Biofeedback Tensión", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                }

                Badge(containerColor = colorPrincipal, contentColor = Color.White) {
                    Text("ZCR: ${metricas.zeroCrossingRate}", modifier = Modifier.padding(4.dp))
                }
            }

            Canvas(modifier = Modifier.fillMaxWidth().height(45.dp)) {
                val barWidth = size.width / 7
                val maxBarHeight = size.height

                for (i in 0 until 7) {
                    val multiplicador = when(i) {
                        0, 6 -> 0.3f
                        1, 5 -> 0.6f
                        2, 4 -> 0.8f
                        else -> 1.0f
                    }
                    // Si es silencio, la barra se mantiene muy pequeña.
                    val barHeight = if (metricas.estado == EstadoVozDSP.SILENCIO) 6f else (maxBarHeight * suavizadoEnergia * multiplicador).coerceAtLeast(6f)
                    val xPos = i * barWidth + (barWidth / 2) - 15f
                    val yPos = (maxBarHeight - barHeight) / 2

                    drawRoundRect(
                        color = colorPrincipal,
                        topLeft = Offset(xPos, yPos),
                        size = Size(30f, barHeight),
                        cornerRadius = CornerRadius(15f, 15f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = iconoEstado, contentDescription = "Estado", tint = colorPrincipal, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = textoEstado,
                    fontSize = 12.sp,
                    color = colorPrincipal,
                    fontWeight = if (metricas.estado == EstadoVozDSP.BLOQUEO) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}