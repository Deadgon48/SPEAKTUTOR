package com.itsx.speaktutor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itsx.speaktutor.logic.MotorAudioDSP
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ControlesAAF(motorDSP: MotorAudioDSP) {
    val dafDelayState by motorDSP.dafDelayState.collectAsState()
    val fafPitchState by motorDSP.fafPitchState.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎛️ Modulación Acústica (AAF)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF2E7D32)
                )
                TextButton(
                    onClick = {
                        motorDSP.configurarDAF(100)
                        motorDSP.configurarFAF(0f)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Restablecer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Control DAF ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Retardo (DAF)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B5E20))
                Text("$dafDelayState ms", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
            }
            Text("Efecto de eco para reducir la velocidad del habla.", fontSize = 11.sp, color = Color(0xFF558B2F))

            Slider(
                value = dafDelayState.toFloat(),
                onValueChange = {
                    motorDSP.configurarDAF(it.roundToInt())
                },
                valueRange = MotorAudioDSP.DAF_MIN_MS.toFloat()..MotorAudioDSP.DAF_MAX_MS.toFloat(),
                modifier = Modifier.height(40.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2E7D32),
                    activeTrackColor = Color(0xFF4CAF50),
                    inactiveTrackColor = Color(0xFFA5D6A7)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFC8E6C9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // --- Control FAF ---
            val pitchLimpio = if (abs(fafPitchState) < 0.05f) 0f else fafPitchState
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tono (FAF)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B5E20))
                val signo = if (pitchLimpio > 0f) "+" else ""
                Text(
                    text = if (pitchLimpio == 0f) "0.0 octavas (Off)" else "$signo${String.format(Locale.US, "%.1f", pitchLimpio)} octavas",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            }
            Text("Cambia la frecuencia para desviar la retroalimentación ósea.", fontSize = 11.sp, color = Color(0xFF558B2F))

            Slider(
                value = pitchLimpio,
                onValueChange = { raw ->
                    // Redondeo exacto a décimas para evitar que el slider quede en -0.0001 y active FAF por accidente
                    val redondeado = (raw * 10f).roundToInt() / 10f
                    motorDSP.configurarFAF(if (abs(redondeado) < 0.05f) 0f else redondeado)
                },
                valueRange = MotorAudioDSP.FAF_MIN_OCT..MotorAudioDSP.FAF_MAX_OCT,
                steps = 19,
                modifier = Modifier.height(40.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2E7D32),
                    activeTrackColor = Color(0xFF4CAF50),
                    inactiveTrackColor = Color(0xFFA5D6A7)
                )
            )
        }
    }
}