package com.itsx.speaktutor.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.itsx.speaktutor.logic.AdaptadorEjercicios
import com.itsx.speaktutor.model.EjercicioPersonalizado
import com.itsx.speaktutor.model.TipoModulo
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.HomeIcon
import com.itsx.speaktutor.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import android.graphics.Color as AndroidColor

// --- ESTRUCTURAS DE DATOS ---

data class SesionProgreso(
    val id: String, val fecha: String, val dificultad: String,
    val aciertos: Int, val errores: Int, val calificacion: Int
)

data class PerfilUsuario(
    val nombre: String, val correo: String, val fechaRegistro: String
)

data class AnaliticasAvanzadas(
    val rachaDias: Int,
    val moduloFavorito: String,
    val tendenciaMejora: String
)

// --- ALMACENAMIENTO AVANZADO ---

object ProgresoStorage {
    private const val PREF_NAME = "SpeakTutorProgresoPrefs"
    private const val KEY_SESIONES = "lista_sesiones"
    private const val KEY_NOMBRE = "perfil_nombre"
    private const val KEY_CORREO = "perfil_correo"
    private const val KEY_FECHA_REGISTRO = "perfil_fecha"
    private const val KEY_TIEMPO_USO = "tiempo_uso_diario"

    fun guardarPerfil(context: Context, nombre: String, correo: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        prefs.edit().putString(KEY_NOMBRE, nombre).putString(KEY_CORREO, correo).putString(KEY_FECHA_REGISTRO, fechaActual).apply()
    }

    fun obtenerPerfil(context: Context): PerfilUsuario? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val nombre = prefs.getString(KEY_NOMBRE, "")
        if (nombre.isNullOrBlank()) return null
        return PerfilUsuario(nombre, prefs.getString(KEY_CORREO, "") ?: "", prefs.getString(KEY_FECHA_REGISTRO, "") ?: "")
    }

    fun guardarSesion(context: Context, dificultad: String, aciertos: Int, errores: Int, calificacion: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val sesionesActuales = obtenerSesiones(context).toMutableList()
        val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val nuevaSesion = SesionProgreso(UUID.randomUUID().toString(), fechaActual, dificultad, aciertos, errores, calificacion)
        sesionesActuales.add(0, nuevaSesion)

        val sb = StringBuilder()
        for (s in sesionesActuales) {
            sb.append("${s.id}|${s.fecha}|${s.dificultad}|${s.aciertos}|${s.errores}|${s.calificacion}#")
        }
        prefs.edit().putString(KEY_SESIONES, sb.toString()).apply()
        agregarMinutosUso(context, 3)
    }

    fun obtenerSesiones(context: Context): List<SesionProgreso> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY_SESIONES, "") ?: ""
        if (data.isBlank()) return emptyList()

        val lista = mutableListOf<SesionProgreso>()
        for (item in data.split("#")) {
            if (item.isBlank()) continue
            val parts = item.split("|")
            if (parts.size == 6) {
                lista.add(SesionProgreso(parts[0], parts[1], parts[2], parts[3].toIntOrNull() ?: 0, parts[4].toIntOrNull() ?: 0, parts[5].toIntOrNull() ?: 0))
            }
        }
        return lista
    }

    fun agregarMinutosUso(context: Context, minutos: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val registros = obtenerRegistrosTiempo(context).toMutableMap()
        registros[hoy] = (registros[hoy] ?: 0) + minutos

        val sb = StringBuilder()
        for ((fecha, mins) in registros) { sb.append("$fecha:$mins#") }
        prefs.edit().putString(KEY_TIEMPO_USO, sb.toString()).apply()
    }

    fun obtenerRegistrosTiempo(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY_TIEMPO_USO, "") ?: ""
        val mapa = mutableMapOf<String, Int>()
        for (item in data.split("#")) {
            if (item.isBlank()) continue
            val parts = item.split(":")
            if (parts.size == 2) mapa[parts[0]] = parts[1].toIntOrNull() ?: 0
        }
        return mapa.toSortedMap(compareByDescending { it })
    }

    fun obtenerAnaliticas(context: Context): AnaliticasAvanzadas {
        val sesiones = obtenerSesiones(context)
        val tiempos = obtenerRegistrosTiempo(context)

        var racha = 0
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hoyStr = formato.format(Date())
        val diasTrabajados = tiempos.keys.sortedDescending()

        if (diasTrabajados.contains(hoyStr)) {
            racha = 1
            val calendario = Calendar.getInstance()
            for (i in 1 until diasTrabajados.size) {
                calendario.add(Calendar.DAY_OF_YEAR, -1)
                val ayerStr = formato.format(calendario.time)
                if (diasTrabajados.contains(ayerStr)) racha++ else break
            }
        }

        val moduloFavorito = sesiones.groupBy { it.dificultad.split("(")[0].trim() }
            .maxByOrNull { it.value.size }?.key ?: "Ninguno"

        val tendencia = if (sesiones.size >= 10) {
            val ultimas5 = sesiones.take(5).map { it.calificacion }.average()
            val previas5 = sesiones.drop(5).take(5).map { it.calificacion }.average()
            if (ultimas5 > previas5 + 5) "📈 Mejorando progresivamente"
            else if (ultimas5 < previas5 - 5) "📉 Ligero descenso en fluidez"
            else "➡️ Estabilidad clínica"
        } else "Requiere más datos"

        return AnaliticasAvanzadas(racha, moduloFavorito, tendencia)
    }
}

// --- GENERADOR DE PDF CON DASHBOARDS Y PAGINACIÓN ---
@RequiresApi(Build.VERSION_CODES.Q)
fun exportarReportePDF(context: Context, perfil: PerfilUsuario, sesiones: List<SesionProgreso>, tiempos: Map<String, Int>, analiticas: AnaliticasAvanzadas) {
    val document = PdfDocument()
    var numeroPagina = 1
    var pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create() // Tamaño A4
    var page = document.startPage(pageInfo)
    var canvas = page.canvas

    val paintBackground = Paint().apply { color = AndroidColor.rgb(2, 119, 189) }
    val paintTextWhite = Paint().apply { color = AndroidColor.WHITE; textSize = 24f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val paintTextBlackTitle = Paint().apply { color = AndroidColor.BLACK; textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val paintTextBlackBody = Paint().apply { color = AndroidColor.DKGRAY; textSize = 14f }
    val paintBox = Paint().apply { color = AndroidColor.rgb(225, 245, 254); style = Paint.Style.FILL }
    val paintBar = Paint().apply { color = AndroidColor.rgb(46, 125, 50); style = Paint.Style.FILL }
    val paintFluidez = Paint().apply { color = AndroidColor.rgb(46, 125, 50); textSize = 12f }

    // Cabecera Página 1
    canvas.drawRect(0f, 0f, 595f, 90f, paintBackground)
    canvas.drawText("SpeakTutor - Expediente Clínico de Fluidez", 40f, 55f, paintTextWhite)

    // Datos del Usuario
    canvas.drawText("Paciente / Usuario:", 40f, 130f, paintTextBlackTitle)
    canvas.drawText("Nombre: ${perfil.nombre}", 40f, 155f, paintTextBlackBody)
    canvas.drawText("Correo: ${perfil.correo}", 40f, 175f, paintTextBlackBody)
    canvas.drawText("Registro: ${perfil.fechaRegistro}", 350f, 155f, paintTextBlackBody)
    canvas.drawText("Estado: Activo en Terapia", 350f, 175f, paintTextBlackBody)

    // Dashboards Blocks (Kpis)
    val promedio = if (sesiones.isNotEmpty()) sesiones.sumOf { it.calificacion } / sesiones.size else 0
    val totalMins = tiempos.values.sum()

    // Cajas de KPIs
    canvas.drawRoundRect(RectF(40f, 210f, 280f, 290f), 10f, 10f, paintBox)
    canvas.drawText("Precisión Promedio", 55f, 240f, paintTextBlackBody)
    canvas.drawText("$promedio%", 55f, 275f, paintTextBlackTitle.apply { textSize = 28f })

    canvas.drawRoundRect(RectF(310f, 210f, 550f, 290f), 10f, 10f, paintBox)
    canvas.drawText("Tiempo de Terapia", 325f, 240f, paintTextBlackBody)
    canvas.drawText("${totalMins / 60}h ${totalMins % 60}m", 325f, 275f, paintTextBlackTitle)

    canvas.drawRoundRect(RectF(40f, 310f, 280f, 390f), 10f, 10f, paintBox)
    canvas.drawText("Racha de Práctica", 55f, 340f, paintTextBlackBody)
    canvas.drawText("${analiticas.rachaDias} días", 55f, 375f, paintTextBlackTitle)

    canvas.drawRoundRect(RectF(310f, 310f, 550f, 390f), 10f, 10f, paintBox)
    canvas.drawText("Módulo Favorito", 325f, 340f, paintTextBlackBody)
    paintTextBlackTitle.textSize = 22f
    canvas.drawText(analiticas.moduloFavorito, 325f, 375f, paintTextBlackTitle)

    // Gráfica
    canvas.drawText("Actividad Reciente (Minutos)", 40f, 440f, paintTextBlackTitle)
    val ultimosDias = tiempos.entries.take(7).toList().reversed()
    var xPosition = 40f
    val maxMinutos = max(1, ultimosDias.maxOfOrNull { it.value } ?: 1)

    for ((fecha, mins) in ultimosDias) {
        val barHeight = (mins.toFloat() / maxMinutos) * 100f
        canvas.drawRect(xPosition, 560f - barHeight, xPosition + 40f, 560f, paintBar)
        canvas.drawText(fecha.takeLast(2), xPosition + 10f, 580f, paintTextBlackBody.apply { textSize = 10f })
        xPosition += 60f
    }

    // Comienza la Tabla Histórica con Paginación
    canvas.drawText("Historial Completo de Evaluaciones", 40f, 620f, paintTextBlackTitle.apply { textSize = 18f })
    canvas.drawLine(40f, 630f, 550f, 630f, paintTextBlackBody)

    var yPos = 650f
    paintTextBlackBody.textSize = 12f

    // Recorremos TODAS las sesiones
    for (sesion in sesiones) {
        // Si llegamos al final de la hoja A4 (margen inferior)
        if (yPos > 800f) {
            document.finishPage(page) // Cerramos la hoja actual

            numeroPagina++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
            page = document.startPage(pageInfo) // Creamos nueva hoja
            canvas = page.canvas

            yPos = 50f // Reseteamos la altura

            // Dibujamos cabecera de la tabla en la nueva hoja
            canvas.drawText("Historial de Evaluaciones (Continuación)", 40f, yPos, paintTextBlackTitle)
            yPos += 15f
            canvas.drawLine(40f, yPos, 550f, yPos, paintTextBlackBody)
            yPos += 25f
        }

        // Limitamos el texto del módulo para que no desborde si el nombre es muy largo
        val moduloCorto = if (sesion.dificultad.length > 28) sesion.dificultad.take(25) + "..." else sesion.dificultad

        canvas.drawText(sesion.fecha.take(10), 40f, yPos, paintTextBlackBody)
        canvas.drawText(moduloCorto, 130f, yPos, paintTextBlackBody)
        canvas.drawText("Fluidez: ${sesion.aciertos} | Bloqueos: ${sesion.errores}", 330f, yPos, paintTextBlackBody)
        canvas.drawText("${sesion.calificacion}%", 510f, yPos, paintFluidez)
        yPos += 20f
    }

    document.finishPage(page)

    // Guardar en Descargas
    try {
        val fileName = "SpeakTutor_Historial_${perfil.nombre.replace(" ", "_")}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            Toast.makeText(context, "✅ Expediente PDF con historial completo guardado en Descargas", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
    } finally {
        document.close()
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var perfil by remember { mutableStateOf(ProgresoStorage.obtenerPerfil(context)) }
    var mostrarDialogoRegistro by remember { mutableStateOf(perfil == null) }
    var mostrarDialogoEditar by remember { mutableStateOf(false) }

    val historialSesiones = remember { ProgresoStorage.obtenerSesiones(context) }
    val registrosTiempo = remember { ProgresoStorage.obtenerRegistrosTiempo(context) }
    val analiticas = remember { ProgresoStorage.obtenerAnaliticas(context) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val totalMinutosUso = registrosTiempo.values.sum()
    val totalPracticas = historialSesiones.size
    val promedioCalificacion = if (totalPracticas > 0) historialSesiones.sumOf { it.calificacion } / totalPracticas else 0

    if (mostrarDialogoRegistro) {
        var nombreInput by remember { mutableStateOf("") }
        var correoInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { },
            title = { Text("Expediente SpeakTutor", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresa tus datos para los reportes clínicos en PDF:")
                    OutlinedTextField(value = nombreInput, onValueChange = { nombreInput = it }, label = { Text("Nombre completo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = correoInput, onValueChange = { correoInput = it }, label = { Text("Correo electrónico") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nombreInput.isNotBlank() && correoInput.isNotBlank()) {
                        ProgresoStorage.guardarPerfil(context, nombreInput.trim(), correoInput.trim())
                        perfil = ProgresoStorage.obtenerPerfil(context)
                        mostrarDialogoRegistro = false
                    } else Toast.makeText(context, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                }) { Text("Guardar Expediente") }
            }
        )
    }

    if (mostrarDialogoEditar) {
        var nombreInput by remember { mutableStateOf(perfil?.nombre ?: "") }
        var correoInput by remember { mutableStateOf(perfil?.correo ?: "") }

        AlertDialog(
            onDismissRequest = { mostrarDialogoEditar = false },
            title = { Text("Editar Perfil de Usuario", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nombreInput, onValueChange = { nombreInput = it }, label = { Text("Nombre completo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = correoInput, onValueChange = { correoInput = it }, label = { Text("Correo electrónico") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nombreInput.isNotBlank() && correoInput.isNotBlank()) {
                        ProgresoStorage.guardarPerfil(context, nombreInput.trim(), correoInput.trim())
                        perfil = ProgresoStorage.obtenerPerfil(context)
                        mostrarDialogoEditar = false
                    }
                }) { Text("Actualizar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogoEditar = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard de Progreso") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(ArrowLeftCircleIcon, "Regresar", tint = MaterialTheme.colorScheme.onSurface) } }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = onBack, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(HomeIcon, "Menú", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regresar al Menú Principal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(modifier = Modifier.height(4.dp))
            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                onNavigateProgreso = { /* Ya estás aquí */ },
                onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) },
                onNavigateBio = { navController.navigate(Screen.Biofeedback.route) }
            )

            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 80.dp)) {

                // PERFIL
                item {
                    if (perfil != null) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp))
                                    Column {
                                        Text(perfil!!.nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Text("Expediente iniciado: ${perfil!!.fechaRegistro}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                                    }
                                }
                                IconButton(onClick = { mostrarDialogoEditar = true }) { Icon(Icons.Default.Edit, "Editar Perfil") }
                            }
                        }
                    }
                }

                // DASHBOARD DE MÉTRICAS AVANZADAS
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Assessment, null, tint = MaterialTheme.colorScheme.primary)
                                    Text("$promedioCalificacion%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("Precisión Global", fontSize = 12.sp)
                                }
                            }
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.secondary)
                                    Text("${totalMinutosUso / 60}h ${totalMinutosUso % 60}m", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Text("Terapia Total", fontSize = 12.sp)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFE65100))
                                    Text("${analiticas.rachaDias}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                    Text("Días Seguidos", fontSize = 12.sp, color = Color.Black)
                                }
                            }
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFF2E7D32))
                                    Text(analiticas.moduloFavorito.take(12), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), maxLines = 1)
                                    Text("Módulo Favorito", fontSize = 12.sp, color = Color.Black)
                                }
                            }
                        }
                    }
                }

                // EXPORTACIÓN PDF
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Análisis: ${analiticas.tendenciaMejora}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Button(
                                onClick = { if (perfil != null) exportarReportePDF(context, perfil!!, historialSesiones, registrosTiempo, analiticas) },
                                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD))
                            ) {
                                Icon(Icons.Default.Download, null); Spacer(modifier = Modifier.width(8.dp))
                                Text("Descargar Expediente Clínico PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item { Text("Historial de Evaluación", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

                if (historialSesiones.isEmpty()) {
                    item { Text("Aún no hay evaluaciones registradas.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
                } else {
                    items(historialSesiones) { sesion ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                                    Text("Módulo: ${sesion.dificultad}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(sesion.fecha, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("✅ Fluidas: ${sesion.aciertos} | ❌ Bloqueos: ${sesion.errores}", fontSize = 13.sp)
                                }
                                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary) {
                                    Text("${sesion.calificacion}%", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}