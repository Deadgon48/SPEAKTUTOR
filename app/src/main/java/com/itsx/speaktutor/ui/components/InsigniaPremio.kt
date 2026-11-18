package com.itsx.speaktutor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TipoInsignia(val titulo: String, val fondo: Color) {
    PLATINO("Logro de Platino", Color(0xFFE2E8F0)),
    ORO("Logro de Oro", Color(0xFFF3E3AC)),
    BRONCE("Logro de Bronce", Color(0xFFF1CFA6))
}

@Composable
fun InsigniaPremio(
    tipo: TipoInsignia,
    lugar: Int? = null,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }

    val rotationX by animateFloatAsState(if (isHovered) tiltX else 0f, label = "rotX")
    val rotationY by animateFloatAsState(if (isHovered) tiltY else 0f, label = "rotY")

    // Animación infinita para el efecto holográfico
    val infiniteTransition = rememberInfiniteTransition(label = "holo")
    val rotationOverlay by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing)),
        label = "overlayRot"
    )

    Box(
        modifier = modifier
            .width(260.dp)
            .height(54.dp)
            .graphicsLayer {
                this.rotationX = rotationX
                this.rotationY = rotationY
                cameraDistance = 10f * density
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isHovered = true },
                    onDragEnd = { isHovered = false; tiltX = 0f; tiltY = 0f },
                    onDragCancel = { isHovered = false; tiltX = 0f; tiltY = 0f }
                ) { change, _ ->
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    tiltY = ((change.position.x / width) - 0.5f) * 30f
                    tiltX = -((change.position.y / height) - 0.5f) * 30f
                }
            }
            .clip(RoundedCornerShape(10.dp))
            .background(tipo.fondo)
            .border(1.dp, Color(0x33000000), RoundedCornerShape(10.dp))
    ) {
        // Textos base de la insignia
        Column(
            modifier = Modifier.padding(start = 52.dp).align(Alignment.CenterStart),
            verticalArrangement = Arrangement.Center
        ) {
            Text("SPEAKTUTOR REWARDS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF666666))
            Text("${tipo.titulo}${if (lugar != null) " #$lugar" else ""}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        }

        // Efecto holográfico animado
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.4f }) {
            val colors = listOf(
                Color(0xFFFF4D4D), Color(0xFFFF9900), Color(0xFFFFFF00),
                Color(0xFF33FF00), Color(0xFF0066FF), Color(0xFF9900FF)
            )

            colors.forEachIndexed { index, color ->
                rotate(degrees = rotationOverlay + (index * 60f)) {
                    drawRect(
                        color = color,
                        topLeft = Offset(-size.width, -size.height),
                        size = Size(size.width * 3, size.height * 3),
                        blendMode = BlendMode.Overlay
                    )
                }
            }
        }
    }
}