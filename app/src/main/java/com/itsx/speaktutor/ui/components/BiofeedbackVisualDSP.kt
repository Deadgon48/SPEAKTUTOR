package com.itsx.speaktutor.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itsx.speaktutor.logic.EstadoVozDSP
import com.itsx.speaktutor.logic.ParametrosAcusticos
import java.util.Locale
import kotlin.math.max

@Composable
fun BiofeedbackVisualAvanzadoDSP(
    metricas: ParametrosAcusticos
) {
    val colorEstado = when (metricas.estado) {
        EstadoVozDSP.SILENCIO -> Color(0xFF78909C)
        EstadoVozDSP.FLUIDO -> Color(0xFF2EAD70)
        EstadoVozDSP.BLOQUEO_TONICO -> Color(0xFFE05252)
        EstadoVozDSP.BLOQUEO_CLONICO -> Color(0xFFE875A5)
    }

    val fondoEstado = when (metricas.estado) {
        EstadoVozDSP.SILENCIO -> Color(0xFFECEFF1)
        EstadoVozDSP.FLUIDO -> Color(0xFFE4F5EB)
        EstadoVozDSP.BLOQUEO_TONICO -> Color(0xFFFFE9E7)
        EstadoVozDSP.BLOQUEO_CLONICO -> Color(0xFFFFEAF2)
    }

    val colorAnimado by animateColorAsState(
        targetValue = colorEstado,
        animationSpec = tween(durationMillis = 180),
        label = "ColorEstadoDSP"
    )

    val energiaNormalizada = (metricas.rmsEnergia / 20_000f).coerceIn(0f, 1f)

    val energiaAnimada by animateFloatAsState(
        targetValue = energiaNormalizada,
        animationSpec = tween(durationMillis = 120),
        label = "EnergiaDSP"
    )

    val textoEstado = when (metricas.estado) {
        EstadoVozDSP.SILENCIO -> "Sin voz detectada"
        EstadoVozDSP.FLUIDO -> "Habla fluida"
        EstadoVozDSP.BLOQUEO_TONICO -> "Posible bloqueo tónico (Tensión alta)"
        EstadoVozDSP.BLOQUEO_CLONICO -> "Posible bloqueo clónico (Repetición)"
    }

    val iconoEstado = when (metricas.estado) {
        EstadoVozDSP.SILENCIO -> Icons.Default.MicOff
        EstadoVozDSP.FLUIDO -> Icons.Default.CheckCircle
        EstadoVozDSP.BLOQUEO_TONICO,
        EstadoVozDSP.BLOQUEO_CLONICO -> Icons.Default.Warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Biofeedback acústico y espectral (FFT)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = fondoEstado, shape = RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = iconoEstado, contentDescription = null, tint = colorAnimado)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = textoEstado,
                    color = colorAnimado,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Nivel de energía",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                val cantidadBarras = 7
                val espacio = 8.dp.toPx()
                val anchoBarra = (size.width - espacio * (cantidadBarras - 1)) / cantidadBarras
                val alturaMaxima = size.height * 0.90f
                val alturaMinima = size.height * 0.12f
                val factores = listOf(0.45f, 0.70f, 0.90f, 1f, 0.82f, 0.65f, 0.42f)

                for (i in 0 until cantidadBarras) {
                    val factor = factores[i] * energiaAnimada
                    val altura = max(alturaMinima, alturaMaxima * factor)
                    val x = i * (anchoBarra + espacio)
                    val inicioY = size.height - altura

                    drawLine(
                        color = colorAnimado.copy(alpha = 0.25f),
                        start = Offset(x + anchoBarra / 2, 0f),
                        end = Offset(x + anchoBarra / 2, size.height),
                        strokeWidth = anchoBarra,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = colorAnimado,
                        start = Offset(x + anchoBarra / 2, inicioY),
                        end = Offset(x + anchoBarra / 2, size.height),
                        strokeWidth = anchoBarra,
                        cap = StrokeCap.Round
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RMS: ${String.format(Locale.US, "%.1f", metricas.rmsEnergia)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Tensión HF: ${(metricas.hfTensionRatio * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (metricas.hfTensionRatio > 0.28f) Color(0xFFE05252) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Flujo FFT: ${String.format(Locale.US, "%.2f", metricas.spectralFlux)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Badge(
                    containerColor = Color(0xFF2EAD70),
                    contentColor = Color.White
                ) {
                    Text(
                        text = "Voz: ${metricas.tramasVocales}",
                        modifier = Modifier.padding(5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Tónicos", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(
                        text = metricas.tonicosActuales.toString(),
                        color = Color(0xFFE05252),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Column {
                    Text("Clónicos", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(
                        text = metricas.clonicosActuales.toString(),
                        color = Color(0xFFE875A5),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Column {
                    Text("Ataque", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(
                        text = String.format(Locale.US, "%.1f", metricas.ataqueBrusco),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            if (metricas.esClonico) {
                Text(
                    text = "Patrón clónico detectado mediante flujo espectral repetitivo.",
                    color = Color(0xFFE875A5),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}