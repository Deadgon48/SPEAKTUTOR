package com.itsx.speaktutor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection

class TicketShape(
    private val cornerRadius: Float = 25f,
    private val notchRadius: Float = 21f,
    private val perforationRatio: Float = 0.75f
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: androidx.compose.ui.unit.Density): Outline {
        val path = Path().apply {
            val perfX = size.width * perforationRatio

            moveTo(cornerRadius, 0f)
            lineTo(perfX - notchRadius, 0f)
            arcTo(Rect(perfX - notchRadius, -notchRadius, perfX + notchRadius, notchRadius), 180f, -180f, false)
            lineTo(size.width - cornerRadius, 0f)
            arcTo(Rect(size.width - cornerRadius * 2, 0f, size.width, cornerRadius * 2), 270f, 90f, false)
            lineTo(size.width, size.height - cornerRadius)
            arcTo(Rect(size.width - cornerRadius * 2, size.height - cornerRadius * 2, size.width, size.height), 0f, 90f, false)
            lineTo(perfX + notchRadius, size.height)
            arcTo(Rect(perfX - notchRadius, size.height - notchRadius, perfX + notchRadius, size.height + notchRadius), 0f, -180f, false)
            lineTo(cornerRadius, size.height)
            arcTo(Rect(0f, size.height - cornerRadius * 2, cornerRadius * 2, size.height), 90f, 90f, false)
            lineTo(0f, cornerRadius)
            arcTo(Rect(0f, 0f, cornerRadius * 2, cornerRadius * 2), 180f, 90f, false)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun TicketRecompensa(
    nombre: String,
    evento: String,
    descripcion: String,
    fecha: String,
    codigo: String,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }

    val rotationX by animateFloatAsState(targetValue = if (isPressed) tiltX else 0f, animationSpec = tween(300), label = "rotX")
    val rotationY by animateFloatAsState(targetValue = if (isPressed) tiltY else 0f, animationSpec = tween(300), label = "rotY")
    val scale by animateFloatAsState(targetValue = if (isPressed) 1.02f else 1f, animationSpec = tween(200), label = "scale")

    val colorLight = Color(0xFFFFC691)
    val colorMid = Color(0xFFFE9046)
    val colorDark = Color(0xFFEF671C)

    Box(
        modifier = modifier
            .width(320.dp)
            .height(180.dp)
            .graphicsLayer {
                this.rotationX = rotationX
                this.rotationY = rotationY
                this.scaleX = scale
                this.scaleY = scale
                cameraDistance = 12f * density
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isPressed = true },
                    onDragEnd = { isPressed = false; tiltX = 0f; tiltY = 0f },
                    onDragCancel = { isPressed = false; tiltX = 0f; tiltY = 0f }
                ) { change, _ ->
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    val dx = (change.position.x / width) - 0.5f
                    val dy = (change.position.y / height) - 0.5f
                    tiltY = dx * 20f
                    tiltX = -(dy * 20f)
                }
            }
            .clip(TicketShape())
            .background(Brush.radialGradient(listOf(colorLight, colorMid, colorDark)))
    ) {
        // Línea perforada punteada
        Canvas(modifier = Modifier.fillMaxSize()) {
            val perfX = size.width * 0.75f
            drawLine(
                color = Color(0x555A3520),
                start = Offset(perfX, 0f),
                end = Offset(perfX, size.height),
                strokeWidth = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // Contenido Principal
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.70f)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(evento.uppercase(), color = Color(0xFF5A3520), fontSize = 10.sp, letterSpacing = 2.sp)
            Text(nombre.uppercase(), color = Color(0xFF5A3520), fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
            Text("$descripcion • $fecha".uppercase(), color = Color(0xFF5A3520), fontSize = 10.sp, letterSpacing = 1.sp)
        }

        // Contenido del Talón (Stub)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.25f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = codigo.uppercase(),
                color = Color(0xFF5A3520),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.graphicsLayer { rotationZ = -90f }
            )
        }
    }
}