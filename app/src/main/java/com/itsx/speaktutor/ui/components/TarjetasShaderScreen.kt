package com.itsx.speaktutor.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder // Importante para reproducir GIFs en Android
import coil.request.ImageRequest
import com.itsx.speaktutor.R

// 👈 Corrección aquí: se quitó el paréntesis en "data class"
data class FeatureItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val colors: List<Color>,
    val onClick: () -> Unit
)

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarjetasShaderScreen(
    onNavigateMetronomo: () -> Unit,
    onNavigateHablaEstirada: () -> Unit,
    onNavigateRitmoFluidez: () -> Unit,
    onNavigateSimulacionSituaciones: () -> Unit,
    onNavigatePronunciacioninstante: () -> Unit,
    onBack: () -> Unit
) {
    val features = listOf(
        FeatureItem(
            title = "Metrónomo de Ritmo",
            description = "Mantén una velocidad constante con pulsos de audio e indicadores visuales.",
            icon = Icons.Default.PlayArrow,
            colors = listOf(Color(0xFF4A148C), Color(0xFF8E24AA), Color(0xFFAB47BC))
        ) { onNavigateMetronomo() },

        FeatureItem(
            title = "Habla Estirada",
            description = "Practica la pronunciación por modo de articulación con listas y control de velocidad.",
            icon = Icons.Default.Star,
            colors = listOf(Color(0xFF006064), Color(0xFF00ACC1), Color(0xFF26C6DA))
        ) { onNavigateHablaEstirada() },

        FeatureItem(
            title = "Ritmo y Fluidez",
            description = "Entrena en bloques, realiza lecturas guiadas y usa la grabadora de progreso.",
            icon = Icons.Default.Refresh,
            colors = listOf(Color(0xFF1B5E20), Color(0xFF43A047), Color(0xFF66BB6A))
        ) { onNavigateRitmoFluidez() },

        FeatureItem(
            title = "Simulacion de situaciones",
            description = "Simulate en situaciones pregrabadas y entrena.",
            icon = Icons.Default.SimCard,
            colors = listOf(Color(0XFF010FFD), Color(0xFF014FFA), Color(0xFF016FFD))
        ) { onNavigateSimulacionSituaciones() },

        FeatureItem(
                title = "Pronunciación al Instante",
            description = "Prueba tu fluidez por intervalos de tiempo y mide tus aciertos.",
            icon = Icons.Default.CheckCircle,
            colors = listOf(Color(0xFFE65100), Color(0xFFF57C00), Color(0xFFFFB74D))
        ) { onNavigatePronunciacioninstante() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Principal - Asistente") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(R.drawable.ascii_effect_animado) // Nombre de tu archivo gif en drawable (sin extensión)
                    .decoderFactory(ImageDecoderDecoder.Factory()) // Activa la animación del GIF
                    .build(),
                contentDescription = "Logo Animado de SpeakTutor",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp) // Ajusta la altura según prefieras
                    .padding(bottom = 8.dp)
            )

            Text(
                text = "Herramientas Principales",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Selecciona un módulo para comenzar tu práctica de articulación y fluidez.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(features) { feature ->
                    FeatureCardItem(feature = feature)
                }
            }
        }
    }
}

@Composable
fun FeatureCardItem(feature: FeatureItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(feature.colors))
            .clickable { feature.onClick() }
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Ir",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = feature.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = feature.description,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2
                )
            }
        }
    }
}