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

    // 1. EJE TONO / RELAJACIÓN: Biofeedback (Koeppen, Soplido, Pluma) y Praxias
    val avanceTono = sesiones.filter {
        it.modulo.contains("Praxias", ignoreCase = true) ||
                it.modulo.contains("Koeppen", ignoreCase = true) ||
                it.modulo.contains("Soplido", ignoreCase = true) ||
                it.modulo.contains("Pluma", ignoreCase = true) ||
                it.modulo.contains("Biofeedback", ignoreCase = true)
    }.map { it.calificacionGlobal }.average().takeIf { !it.isNaN() } ?: 0.0

    // 2. EJE PRONUNCIACIÓN / ARTICULACIÓN: Conciencia Fonológica, Tarjetas Articulación y Pronunciación al Instante
    val avanceArticulacion = sesiones.filter {
        it.modulo.contains("Fonológica", ignoreCase = true) ||
                it.modulo.contains("Fonologica", ignoreCase = true) ||
                it.modulo.contains("Articulación", ignoreCase = true) ||
                it.modulo.contains("Articulacion", ignoreCase = true) ||
                it.modulo.contains("Pronunciación", ignoreCase = true) ||
                it.modulo.contains("Pronunciacion", ignoreCase = true) ||
                it.modulo.contains("Instante", ignoreCase = true)
    }.map { it.calificacionGlobal }.average().takeIf { !it.isNaN() } ?: 0.0

    // 3. EJE FLUIDEZ: Habla Estirada, Ritmo y Fluidez, Metrónomo y Ejercicios Adaptativos
    val avanceFluidez = sesiones.filter {
        it.modulo.contains("Habla Estirada", ignoreCase = true) ||
                it.modulo.contains("Estirada", ignoreCase = true) ||
                it.modulo.contains("Fluidez", ignoreCase = true) ||
                it.modulo.contains("Metrónomo", ignoreCase = true) ||
                it.modulo.contains("Metronomo", ignoreCase = true) ||
                it.modulo.contains("Adaptativo", ignoreCase = true)
    }.map { it.calificacionGlobal }.average().takeIf { !it.isNaN() } ?: 0.0

    // 4. EJE DESENSIBILIZACIÓN / SIMULACIÓN: Simulación de Situaciones, Llamadas e Impacto OASES
    val sesionesSimulacion = sesiones.filter {
        it.modulo.contains("Simulación", ignoreCase = true) ||
                it.modulo.contains("Simulacion", ignoreCase = true) ||
                it.modulo.contains("Situaciones", ignoreCase = true) ||
                it.modulo.contains("Llamada", ignoreCase = true)
    }
    val avanceSimulacion = sesionesSimulacion.map { it.calificacionGlobal }.average().takeIf { !it.isNaN() } ?: 0.0

    val sesionesConPsicosocial = sesiones.filter { it.impactoPsicosocial > 0 }
    val avanceOases = if (sesionesConPsicosocial.isNotEmpty()) {
        sesionesConPsicosocial.map { it.impactoPsicosocial }.average()
    } else {
        avanceSimulacion // Si aún no hay encuesta OASES directa, refleja el avance en Simulación de Situaciones
    }

    // 5. EJE RITMO GLOBAL Y MÉTRICAS ACÚSTICAS DE TODOS LOS MÓDULOS
    val totalTonicos = sesiones.sumOf { it.bloqueosTonicos }
    val totalClonicos = sesiones.sumOf { it.bloqueosClonicos }
    val sesionesConLatencia = sesiones.filter { it.latenciaMediaMs > 0 }
    val latenciaPromedio = if (sesionesConLatencia.isNotEmpty()) {
        sesionesConLatencia.map { it.latenciaMediaMs }.average()
    } else 0.0

    val ritmoPorcentaje = if (sesionesConLatencia.isNotEmpty()) {
        ((1f - (latenciaPromedio / 2500f).toFloat()) * 100f).coerceIn(10f, 100f).toInt()
    } else if (sesiones.isNotEmpty()) {
        // Respaldo si las sesiones guardadas no registraron latencia en ms
        avanceFluidez.toInt().coerceAtLeast(10)
    } else {
        0
    }

    // CONVERSIÓN PARA EL GRÁFICO RADAR (Escala 0.1f a 1.0f)
    val valFluidez = (avanceFluidez / 100f).toFloat().coerceIn(0.1f, 1f)
    val valPronunciacion = (avanceArticulacion / 100f).toFloat().coerceIn(0.1f, 1f)
    val valRitmo = (ritmoPorcentaje / 100f).toFloat().coerceIn(0.1f, 1f)
    val valTono = (avanceTono / 100f).toFloat().coerceIn(0.1f, 1f)
    val valConfianza = (avanceOases / 100f).toFloat().coerceIn(0.1f, 1f)

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
                        ritmo = valRitmo,
                        tono = valTono,
                        volumen = valConfianza
                    )
                }

                item {
                    Column {
                        Text(
                            text = "Rendimiento por Categoría",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cada indicador se calcula promediando los módulos clínicos vinculados:",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                item {
                    TarjetaProgresoCategoria(
                        titulo = "Control Motor y Relajación (Tono)",
                        porcentaje = avanceTono.toInt(),
                        color = Color(0xFF43A047),
                        descripcion = "Sube con: Biofeedback (Koeppen, Soplido, Toque de Pluma) y Praxias"
                    )
                }

                item {
                    TarjetaProgresoCategoria(
                        titulo = "Articulación y Fonología (Pronunciación)",
                        porcentaje = avanceArticulacion.toInt(),
                        color = Color(0xFF1E88E5),
                        descripcion = "Sube con: Conciencia Fonológica, Tarjetas de Articulación y Pronunciación al Instante"
                    )
                }

                item {
                    TarjetaProgresoCategoria(
                        titulo = "Fluidez y Control Temporal (DSP)",
                        porcentaje = avanceFluidez.toInt(),
                        color = Color(0xFF8E24AA),
                        descripcion = "Sube con: Habla Estirada, Ritmo y Fluidez, Metrónomo y Ejercicios Adaptativos"
                    )
                }

                item {
                    TarjetaProgresoCategoria(
                        titulo = "Desensibilización y Confianza (Simulación / OASES)",
                        porcentaje = avanceOases.toInt(),
                        color = Color(0xFFF57C00),
                        descripcion = "Sube con: Simulación de Situaciones, Llamadas y Escala Psicosocial OASES"
                    )
                }

                item {
                    TarjetaProgresoCategoria(
                        titulo = "Agilidad de Inicio Vocal (Ritmo)",
                        porcentaje = ritmoPorcentaje,
                        color = Color(0xFF00897B),
                        descripcion = "Sube al reducir la latencia fonatoria en todos los módulos con micrófono activo"
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
                                Text("Total de Sesiones Evaluadas:", fontWeight = FontWeight.Medium)
                                Text("${sesiones.size}", fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider()
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
                Text(
                    text = titulo,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text("$porcentaje%", fontWeight = FontWeight.ExtraBold, color = color, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(descripcion, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))
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
        Text(
            text = "Mapa Estructural del Habla",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Fluidez · Pronunciación · Ritmo · Tono · Confianza",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

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