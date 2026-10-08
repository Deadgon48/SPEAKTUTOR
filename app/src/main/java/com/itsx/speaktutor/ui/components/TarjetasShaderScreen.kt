package com.itsx.speaktutor.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.itsx.speaktutor.R
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon

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
    onNavigateProgreso: () -> Unit,
    onNavigateEjerciciosAdaptativos: () -> Unit,
    onNavigateBio: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // --- ESTADOS DE PERFIL Y REGISTRO INICIAL ---
    var perfil by remember { mutableStateOf(ProgresoStorage.obtenerPerfil(context)) }
    var mostrarDialogoRegistro by remember { mutableStateOf(perfil == null) }

    // Lanzador para solicitar permisos de micrófono al abrir la app
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permiso de micrófono concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Se requiere permiso de micrófono para las funciones de voz", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val checkMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (checkMic != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // DIÁLOGO DE PRIMERA VEZ (ONBOARDING)
    if (mostrarDialogoRegistro) {
        var nombreInput by remember { mutableStateOf("") }
        var correoInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { /* Bloquear cierre sin registrar */ },
            title = { Text("¡Bienvenido a SpeakTutor!", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Por favor, ingresa tus datos para configurar tu perfil y generar tus reportes de progreso:")
                    OutlinedTextField(
                        value = nombreInput,
                        onValueChange = { nombreInput = it },
                        label = { Text("Nombre completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = correoInput,
                        onValueChange = { correoInput = it },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nombreInput.isNotBlank() && correoInput.isNotBlank()) {
                            ProgresoStorage.guardarPerfil(context, nombreInput.trim(), correoInput.trim())
                            perfil = ProgresoStorage.obtenerPerfil(context)
                            mostrarDialogoRegistro = false
                            Toast.makeText(context, "¡Perfil configurado con éxito!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Por favor completa ambos campos", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Comenzar a Practicar") }
            }
        )
    }

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
        ) { onNavigatePronunciacioninstante() },

        // NUEVA TARJETA: Biofeedback y Autorregulación
        FeatureItem(
            title = "Biofeedback y Relajación",
            description = "Ejercicios clínicos de control respiratorio, tensión-distensión y toque de pluma.",
            icon = Icons.Default.Favorite,
            colors = listOf(Color(0xFFC2185B), Color(0xFFE91E63), Color(0xFFF06292))
        ) { onNavigateBio() },

        FeatureItem(
            title = "Mi Progreso y Estadísticas",
            description = "Consulta tu historial de práctica, aciertos y promedio general.",
            icon = Icons.Default.Assessment,
            colors = listOf(Color(0xFF00695C), Color(0xFF00897B), Color(0xFF4DB6AC))
        ) { onNavigateProgreso() },

        FeatureItem(
            title = "Ejercicios Adaptativos",
            description = "Acceda a ejecricios basados en su historial de progreso y promedio general.",
            icon = Icons.Default.Assessment,
            colors = listOf(Color(0xFF00356B), Color(0xFF00897B), Color(0xFF4DB6AC))
        ) { onNavigateEjerciciosAdaptativos() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Principal - Asistente") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = ArrowLeftCircleIcon, contentDescription = "Regresar", tint = MaterialTheme.colorScheme.onSurface
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
                    .data(R.drawable.ascii_effect_animado)
                    .decoderFactory(ImageDecoderDecoder.Factory())
                    .build(),
                contentDescription = "Logo Animado de SpeakTutor",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
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