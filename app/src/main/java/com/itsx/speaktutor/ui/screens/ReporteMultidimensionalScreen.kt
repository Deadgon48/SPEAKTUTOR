package com.itsx.speaktutor.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.HomeIcon
import com.itsx.speaktutor.ui.navigation.Screen
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteMultidimensionalScreen(navController: NavController) {
    val context = LocalContext.current
    val sesiones = remember { ProgresoStorage.obtenerSesionesClinicas(context) }

    // EXTRACCIÓN Y CÁLCULO DE MÉTRICAS DINÁMICAS DESDE ROOM/SHARED PREFERENCES
    val avancePraxias = sesiones.filter { it.modulo.contains("Praxias", ignoreCase = true) }.map { it.calificacionGlobal }.average().takeIf { !it.isNaN() } ?: 0.0
    val avanceFonologia = sesiones.filter { it.modulo.contains("Fonológica", ignoreCase = true) }.map { it.calificacionGlobal }.average().takeIf { !it.isNaN() } ?: 0.0
    val avanceFluidez = sesiones.filter { it.modulo.contains("Habla Estirada", ignoreCase = true) || it.modulo.contains("Fluidez") }.map { it.calificacionGlobal }.average().takeIf { !it.isNaN() } ?: 0.0
    val avanceSimulacion = sesiones.filter { it.modulo.contains("Simulación") || it.modulo.contains("Llamada") }.map { it.calificacionGlobal }.average().takeIf { !it.isNaN() } ?: 0.0

    // Extracción del OASES (Impacto psicosocial clínico)
    val sesionesConPsicosocial = sesiones.filter { it.impactoPsicosocial > 0 }
    val avanceOases = if (sesionesConPsicosocial.isNotEmpty()) {
        sesionesConPsicosocial.map { it.impactoPsicosocial }.average()
    } else 0.0

    val totalTonicos = sesiones.sumOf { it.bloqueosTonicos }
    val totalClonicos = sesiones.sumOf { it.bloqueosClonicos }
    val latenciaPromedio = sesiones.map { it.latenciaMediaMs }.average().takeIf { !it.isNaN() } ?: 0.0

    // CONVERSIÓN PARA EL GRÁFICO RADAR (Escala 0.0 a 1.0)
    val ritmoCalculado = (1f - (latenciaPromedio / 2500f).toFloat()).coerceIn(0.1f, 1f)

    val valFluidez = (avanceFluidez / 100f).toFloat().coerceIn(0.1f, 1f)
    val valPronunciacion = (avanceFonologia / 100f).toFloat().coerceIn(0.1f, 1f)
    val valTono = (avancePraxias / 100f).toFloat().coerceIn(0.1f, 1f)
    val valSimulacion = (avanceSimulacion / 100f).toFloat().coerceIn(0.1f, 1f)
    val valVolumen = (avanceOases / 100f).toFloat().coerceIn(0.1f, 1f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Métricas Multidimensionales") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(ArrowLeftCircleIcon, "Regresar") }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Screen.TarjetasShader.route) { popUpTo(0) } },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(imageVector = HomeIcon, contentDescription = "Menú Principal", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regresar al Menú Principal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                onNavigateProgreso = { navController.navigate(Screen.Progreso.route) },
                onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) },
                onNavigateBio = { navController.navigate(Screen.Biofeedback.route) },
                onNavigateTarjetasArticulacion = { navController.navigate(Screen.TarjetasArticulacion.route) },
                onNavigateConcienciaFonologica = { navController.navigate(Screen.ConcienciaFonologica.route) },
                onNavigateReporteMultidimensional = { /* Ya estás aquí */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    ReporteMultidimensionalCanvas(
                        fluidez = valFluidez,
                        pronunciacion = valPronunciacion,
                        ritmo = ritmoCalculado,
                        tono = valTono,
                        volumen = valVolumen
                    )
                }

                item {
                    Text(
                        text = "Rendimiento por Categoría",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    TarjetaProgresoCategoria(
                        titulo = "Praxias Faciales (Tono)",
                        porcentaje = avancePraxias.toInt(),
                        color = Color(0xFF43A047),
                        descripcion = "Ejercicios de articulación y gesticulación"
                    )
                }

                item {
                    TarjetaProgresoCategoria(
                        titulo = "Conciencia Fonológica (Pronunciación)",
                        porcentaje = avanceFonologia.toInt(),
                        color = Color(0xFF1E88E5),
                        descripcion = "Segmentación e identificación de sonidos"
                    )
                }

                item {
                    TarjetaProgresoCategoria(
                        titulo = "Ejercicios de Fluidez (DSP)",
                        porcentaje = avanceFluidez.toInt(),
                        color = Color(0xFF8E24AA),
                        descripcion = "Habla estirada, inicios suaves, lectura"
                    )
                }

                item {
                    TarjetaProgresoCategoria(
                        titulo = "Impacto Clínico OASES (Volumen/Confianza)",
                        porcentaje = avanceOases.toInt(),
                        color = Color(0xFFF57C00),
                        descripcion = "Evaluación mHealth de experiencia y confort al hablar"
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Estadísticas Acústicas Globales",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Latencia Fonatoria Promedio:", fontWeight = FontWeight.Medium)
                                Text("${latenciaPromedio.toInt()} ms", fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Bloqueos Tónicos Detectados:", fontWeight = FontWeight.Medium)
                                Text("$totalTonicos", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Bloqueos Clónicos Detectados:", fontWeight = FontWeight.Medium)
                                Text("$totalClonicos", fontWeight = FontWeight.Bold, color = Color(0xD6D60DDD))
                            }
                        }
                    }
                }

                item {
                    GraficaHistorialIncidencias(sesiones = sesiones)
                }
            }
        }
    }
}

@Composable
fun TarjetaProgresoCategoria(titulo: String, porcentaje: Int, color: Color, descripcion: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(titulo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("$porcentaje%", fontWeight = FontWeight.ExtraBold, color = color, fontSize = 18.sp)
            }
            Text(descripcion, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (porcentaje / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun ReporteMultidimensionalCanvas(
    fluidez: Float,
    pronunciacion: Float,
    ritmo: Float,
    tono: Float,
    volumen: Float,
    modifier: Modifier = Modifier
) {
    val values = listOf(fluidez, pronunciacion, ritmo, tono, volumen)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Mapa Estructural del Habla", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))

        Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val gridColor = Color.Gray.copy(alpha = 0.3f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2.2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val angleStep = (2 * Math.PI / 5).toFloat()

                for (step in 1..5) {
                    val stepRadius = radius * (step / 5f)
                    val path = Path()
                    for (i in 0 until 5) {
                        val angle = i * angleStep - (Math.PI / 2).toFloat()
                        val x = center.x + stepRadius * cos(angle)
                        val y = center.y + stepRadius * sin(angle)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(path = path, color = gridColor, style = Stroke(width = 1.dp.toPx()))
                }

                for (i in 0 until 5) {
                    val angle = i * angleStep - (Math.PI / 2).toFloat()
                    val x = center.x + radius * cos(angle)
                    val y = center.y + radius * sin(angle)
                    drawLine(color = gridColor, start = center, end = Offset(x, y), strokeWidth = 1.dp.toPx())
                }

                val valuePath = Path()
                for (i in 0 until 5) {
                    val angle = i * angleStep - (Math.PI / 2).toFloat()
                    val valueRadius = radius * values[i].coerceIn(0f, 1f)
                    val x = center.x + valueRadius * cos(angle)
                    val y = center.y + valueRadius * sin(angle)
                    if (i == 0) valuePath.moveTo(x, y) else valuePath.lineTo(x, y)
                }
                valuePath.close()

                drawPath(path = valuePath, color = primaryColor.copy(alpha = 0.4f))
                drawPath(path = valuePath, color = primaryColor, style = Stroke(width = 3.dp.toPx()))
            }
        }
    }
}

@Composable
fun GraficaHistorialIncidencias(sesiones: List<SesionClinica>) {
    if (sesiones.isEmpty()) return
    val ultimas = sesiones.take(5).reversed()
    val maxBloqueos = ultimas.maxOfOrNull { it.bloqueosTonicos + it.bloqueosClonicos }?.coerceAtLeast(5)?.toFloat() ?: 5f

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Historial de Incidencias (Últimas 5 sesiones)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 16.dp)) {
                val w = size.width
                val h = size.height
                val stepX = w / ultimas.size

                for (i in 0..3) {
                    val y = h - (h * (i / 3f))
                    drawLine(color = Color.LightGray.copy(0.5f), start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
                }

                ultimas.forEachIndexed { index, sesion ->
                    val x = (index * stepX) + (stepX / 2f)
                    val total = sesion.bloqueosTonicos + sesion.bloqueosClonicos
                    val hTotal = (total / maxBloqueos) * h
                    val hTonicos = (sesion.bloqueosTonicos / maxBloqueos) * h

                    // Dibujar barra Clónicos (Base a Top)
                    drawRect(color = Color(0xD6D60DDD), topLeft = Offset(x - 20f, h - hTotal), size = androidx.compose.ui.geometry.Size(40f, hTotal - hTonicos))
                    // Dibujar barra Tónicos (Base)
                    drawRect(color = Color(0xFFD32F2F), topLeft = Offset(x - 20f, h - hTonicos), size = androidx.compose.ui.geometry.Size(40f, hTonicos))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFFD32F2F)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tónicos", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xD6D60DDD)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clónicos", fontSize = 12.sp)
                }
            }
        }
    }
}