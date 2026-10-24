package com.itsx.speaktutor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TonoAlerta {
    INFO, EXITO, ADVERTENCIA, PELIGRO
}

@Composable
fun AlertaInPlace(
    modifier: Modifier = Modifier,
    tono: TonoAlerta = TonoAlerta.INFO,
    titulo: String? = null,
    mensaje: String? = null,
    onClose: (() -> Unit)? = null,
    accion: (@Composable () -> Unit)? = null
) {
    // Configuración de colores e iconos según el tono
    val (bgColor, contentColor, borderColor, icon) = when (tono) {
        TonoAlerta.INFO -> listOf(Color(0xFFE3F2FD), Color(0xFF0D47A1), Color(0xFF90CAF9), Icons.Default.Info)
        TonoAlerta.EXITO -> listOf(Color(0xFFE8F5E9), Color(0xFF1B5E20), Color(0xFFA5D6A7), Icons.Default.CheckCircle)
        TonoAlerta.ADVERTENCIA -> listOf(Color(0xFFFFF3E0), Color(0xFFE65100), Color(0xFFFFCC80), Icons.Default.Warning)
        TonoAlerta.PELIGRO -> listOf(Color(0xFFFFEBEE), Color(0xFFB71C1C), Color(0xFFEF9A9A), Icons.Default.Error)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor as Color)
            .border(1.dp, borderColor as Color, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Icono principal
            Icon(
                imageVector = icon as ImageVector,
                contentDescription = null,
                tint = contentColor as Color,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Contenido principal (Título, Mensaje, Acción)
            Column(modifier = Modifier.weight(1f)) {
                if (titulo != null) {
                    Text(
                        text = titulo,
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                if (mensaje != null) {
                    Text(
                        text = mensaje,
                        color = contentColor.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
                if (accion != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    accion()
                }
            }

            // Botón de cierre (Opcional)
            if (onClose != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar alerta",
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}