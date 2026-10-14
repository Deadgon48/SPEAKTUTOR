package com.itsx.speaktutor.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.DialogoPerfilClinico
import com.itsx.speaktutor.ui.components.HomeIcon
import com.itsx.speaktutor.ui.navigation.Screen
import com.itsx.speaktutor.ui.screens.ProgresoStorage.obtenerSesionesClinicas
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Path as ComposePath

// --- 1. MODELOS DE DATOS CLÍNICOS ---

data class SesionClinica(
    val id: String, val fecha: String, val modulo: String,
    val aciertos: Int, val erroresTotales: Int, val calificacionGlobal: Int,
    val bloqueosTonicos: Int, val bloqueosClonicos: Int,
    val latenciaMediaMs: Int, val contactosLigerosPct: Int
)

data class PerfilClinico(
    val nombre: String, val correo: String, val fechaRegistro: String,
    val diagnosticoPrincipal: String, val objetivosSmart: String
)

data class AnaliticasClinicas(
    val rachaDias: Int, val tendenciaFluidez: String,
    val promedioTonicos: Int, val promedioClonicos: Int,
    val promedioContactosLigeros: Int, val latenciaPromedio: Int
)

// --- 2. MOTOR DE ALMACENAMIENTO ---

object ProgresoStorage {
    private const val PREF_NAME = "SpeakTutorHCE_Prefs"
    private const val KEY_SESIONES = "hce_sesiones"
    private const val KEY_PERFIL = "hce_perfil_v2"
    private const val KEY_TIEMPO_USO = "hce_tiempo_diario"

    fun guardarPerfilClinico(context: Context, nombre: String, correo: String, diagnostico: String, objetivos: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        // Codificamos saltos de línea para preservar los renglones en SharedPreferences
        val diagSeguro = diagnostico.replace("\n", "\\n")
        val objSeguro = objetivos.replace("\n", "\\n")

        val perfilString = "$nombre|$correo|$fechaActual|$diagSeguro|$objSeguro"
        prefs.edit { putString(KEY_PERFIL, perfilString) }
    }

    fun obtenerPerfilClinico(context: Context): PerfilClinico? {
        val data = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_PERFIL, "") ?: ""
        if (data.isBlank()) return null
        val p = data.split("|")
        if (p.size < 5) return PerfilClinico(p[0], p[1], p[2], "No especificado", "No especificado")

        // Restauramos los saltos de línea al leer
        val diagRestaurado = p[3].replace("\\n", "\n")
        val objRestaurado = p[4].replace("\\n", "\n")

        return PerfilClinico(p[0], p[1], p[2], diagRestaurado, objRestaurado)
    }

    fun guardarSesionMultidimensional(
        context: Context, modulo: String, aciertos: Int, erroresTotales: Int, calificacion: Int,
        bloqueosTonicos: Int = 0, bloqueosClonicos: Int = 0, latenciaMedia: Int = 0, contactosLigeros: Int = 0
    ) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val sesionesActuales = obtenerSesionesClinicas(context).toMutableList()
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val nueva = SesionClinica(
            UUID.randomUUID().toString(), fecha, modulo, aciertos, erroresTotales, calificacion,
            bloqueosTonicos, bloqueosClonicos, latenciaMedia, contactosLigeros
        )
        sesionesActuales.add(0, nueva)

        val sb = StringBuilder()
        for (s in sesionesActuales) {
            sb.append("${s.id}|${s.fecha}|${s.modulo}|${s.aciertos}|${s.erroresTotales}|${s.calificacionGlobal}|${s.bloqueosTonicos}|${s.bloqueosClonicos}|${s.latenciaMediaMs}|${s.contactosLigerosPct}#")
        }
        prefs.edit { putString(KEY_SESIONES, sb.toString()) }
        agregarMinutosUso(context, 3)
    }

    fun obtenerSesionesClinicas(context: Context): List<SesionClinica> {
        val data = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_SESIONES, "") ?: ""
        val lista = mutableListOf<SesionClinica>()
        for (item in data.split("#")) {
            if (item.isBlank()) continue
            val p = item.split("|")
            if (p.size >= 10) {
                lista.add(SesionClinica(p[0], p[1], p[2], p[3].toIntOrNull()?:0, p[4].toIntOrNull()?:0, p[5].toIntOrNull()?:0,
                    p[6].toIntOrNull()?:0, p[7].toIntOrNull()?:0, p[8].toIntOrNull()?:0, p[9].toIntOrNull()?:0))
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
        for ((f, m) in registros) sb.append("$f:$m#")
        prefs.edit { putString(KEY_TIEMPO_USO, sb.toString()) }
    }

    fun obtenerRegistrosTiempo(context: Context): Map<String, Int> {
        val data = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_TIEMPO_USO, "") ?: ""
        val mapa = mutableMapOf<String, Int>()
        for (item in data.split("#")) {
            if (item.isBlank()) continue
            val parts = item.split(":")
            if (parts.size == 2) mapa[parts[0]] = parts[1].toIntOrNull() ?: 0
        }
        return mapa.toSortedMap(compareByDescending { it })
    }

    fun obtenerAnaliticasClinicas(context: Context): AnaliticasClinicas {
        val sesiones = obtenerSesionesClinicas(context)
        val tiempos = obtenerRegistrosTiempo(context)

        var racha = 0
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dias = tiempos.keys.sortedDescending()
        if (dias.contains(formato.format(Date()))) {
            racha = 1
            val cal = Calendar.getInstance()
            for (_i in 1 until dias.size) {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                if (dias.contains(formato.format(cal.time))) racha++ else break
            }
        }

        val tendencia = if (sesiones.size >= 6) {
            val ultimos = sesiones.take(3).map { it.contactosLigerosPct }.average()
            val previos = sesiones.drop(3).take(3).map { it.contactosLigerosPct }.average()
            if (ultimos > previos + 5) "📈 Mejora en inicios suaves"
            else if (ultimos < previos - 5) "📉 Aumento de bloqueos iniciales"
            else "➡️ Fluidez estable"
        } else "Datos insuficientes"

        val tTonicos = if (sesiones.isNotEmpty()) sesiones.sumOf { it.bloqueosTonicos } / sesiones.size else 0
        val tClonicos = if (sesiones.isNotEmpty()) sesiones.sumOf { it.bloqueosClonicos } / sesiones.size else 0
        val tContactos = if (sesiones.isNotEmpty()) sesiones.sumOf { it.contactosLigerosPct } / sesiones.size else 0
        val latencia = if (sesiones.isNotEmpty()) sesiones.sumOf { it.latenciaMediaMs } / sesiones.size else 0

        return AnaliticasClinicas(racha, tendencia, tTonicos, tClonicos, tContactos, latencia)
    }
}

// --- 3. COMPONENTE UI: GRÁFICA DE TENDENCIA (CANVAS COMPOSE) ---
@Composable
fun GraficaFluidezInteractiva(sesiones: List<SesionClinica>) {
    if (sesiones.size < 2) {
        Text("No hay suficientes datos para generar la gráfica.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
        return
    }

    val datosGrafica = sesiones.reversed().map { it.calificacionGlobal.toFloat() }
    val maxValor = 100f

    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
        val width = size.width
        val height = size.height
        val stepX = width / (datosGrafica.size - 1).coerceAtLeast(1)

        val gridLines = 4
        for (i in 0..gridLines) {
            val y = height - (height * (i.toFloat() / gridLines))
            drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = Offset(0f, y), end = Offset(width, y), strokeWidth = 2f)
        }

        val path = ComposePath()
        val puntos = mutableListOf<Offset>()

        datosGrafica.forEachIndexed { index, valor ->
            val x = index * stepX
            val y = height - ((valor / maxValor) * height)
            puntos.add(Offset(x, y))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color(0xFF1976D2),
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        puntos.forEach { point ->
            drawCircle(color = Color(0xFF0D47A1), radius = 8f, center = point)
            drawCircle(color = Color.White, radius = 4f, center = point)
        }
    }
}

// --- 4. GENERADOR DE HISTORIA CLÍNICA ELECTRÓNICA (PDF CON GRÁFICAS) ---
@RequiresApi(Build.VERSION_CODES.Q)
fun exportarHcePdf(context: Context, perfil: PerfilClinico, sesiones: List<SesionClinica>) {
    val document = PdfDocument()
    var numeroPagina = 1
    var pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas

    val pHeader = Paint().apply { color = AndroidColor.rgb(27, 94, 32); style = Paint.Style.FILL }
    val pTextWhite = Paint().apply { color = AndroidColor.WHITE; textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val pTitle = Paint().apply { color = AndroidColor.DKGRAY; textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val pBody = Paint().apply { color = AndroidColor.BLACK; textSize = 11f }
    val pBox = Paint().apply { color = AndroidColor.rgb(232, 245, 233); style = Paint.Style.FILL }
    val pLine = Paint().apply { color = AndroidColor.LTGRAY; strokeWidth = 1f }

    canvas.drawRect(0f, 0f, 595f, 60f, pHeader)
    canvas.drawText("Historia Clínica Electrónica - Logopedia (SpeakTutor)", 40f, 38f, pTextWhite)

    canvas.drawText("1. Datos Demográficos y Diagnóstico", 40f, 95f, pTitle)
    canvas.drawRoundRect(RectF(40f, 105f, 555f, 175f), 8f, 8f, pBox)
    canvas.drawText("Paciente: ${perfil.nombre}", 55f, 125f, pBody)
    canvas.drawText("Correo: ${perfil.correo}", 55f, 145f, pBody)
    canvas.drawText("Registro: ${perfil.fechaRegistro}", 55f, 165f, pBody)
    canvas.drawText("Diagnóstico: ${perfil.diagnosticoPrincipal}", 300f, 125f, pBody.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
    pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    canvas.drawText("2. Objetivos Terapéuticos SMART", 40f, 210f, pTitle)
    canvas.drawRoundRect(RectF(40f, 220f, 555f, 280f), 8f, 8f, pBox)
    val palabras = perfil.objetivosSmart.split(" ")
    var linea = ""
    var yTemp = 240f
    for (palabra in palabras) {
        if (pBody.measureText("$linea $palabra") > 480f) {
            canvas.drawText(linea, 55f, yTemp, pBody)
            linea = palabra; yTemp += 16f
        } else { linea = if (linea.isEmpty()) palabra else "$linea $palabra" }
    }
    canvas.drawText(linea, 55f, yTemp, pBody)

    canvas.drawText("3. Gráfica de Evolución de Fluidez (%)", 40f, 320f, pTitle)
    canvas.drawRect(40f, 335f, 555f, 455f, Paint().apply { color = AndroidColor.rgb(250, 250, 250); style = Paint.Style.FILL })
    canvas.drawRect(40f, 335f, 555f, 455f, Paint().apply { color = AndroidColor.LTGRAY; style = Paint.Style.STROKE })

    if (sesiones.size >= 2) {
        val puntosGrafica = sesiones.reversed().map { it.calificacionGlobal.toFloat() }
        val pGrid = Paint().apply { color = AndroidColor.LTGRAY; strokeWidth = 0.5f }
        val pPath = Paint().apply { color = AndroidColor.rgb(25, 118, 210); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }

        for (i in 0..4) {
            val yLine = 455f - (i * 30f)
            canvas.drawLine(40f, yLine, 555f, yLine, pGrid)
        }

        val stepX = 515f / (puntosGrafica.size - 1).coerceAtLeast(1)
        val pathPDF = Path()

        puntosGrafica.forEachIndexed { i, valor ->
            val x = 40f + (i * stepX)
            val y = 455f - ((valor / 100f) * 120f)
            if (i == 0) pathPDF.moveTo(x, y) else pathPDF.lineTo(x, y)
            canvas.drawCircle(x, y, 3f, Paint().apply { color = AndroidColor.rgb(13, 71, 161) })
        }
        canvas.drawPath(pathPDF, pPath)
    } else {
        canvas.drawText("Datos insuficientes para graficar", 200f, 400f, pBody.apply { color = AndroidColor.GRAY })
    }

    canvas.drawText("4. Observaciones Clínicas y Recomendaciones", 40f, 490f, pTitle)
    canvas.drawRoundRect(RectF(40f, 505f, 555f, 605f), 8f, 8f, Paint().apply { color = AndroidColor.WHITE; style = Paint.Style.FILL })
    canvas.drawRoundRect(RectF(40f, 505f, 555f, 605f), 8f, 8f, Paint().apply { color = AndroidColor.GRAY; style = Paint.Style.STROKE })
    for(i in 0..3) canvas.drawLine(50f, 530f + (i*20f), 545f, 530f + (i*20f), pLine)

    canvas.drawText("5. Registro de Sesiones Clínicas Detallado (Historial Completo)", 40f, 640f, pTitle)
    canvas.drawLine(40f, 650f, 555f, 650f, pLine)

    pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Fecha", 40f, 665f, pBody)
    canvas.drawText("Módulo", 120f, 665f, pBody)
    canvas.drawText("C. Ligeros", 260f, 665f, pBody)
    canvas.drawText("Tónicos", 330f, 665f, pBody)
    canvas.drawText("Clónicos", 390f, 665f, pBody)
    canvas.drawText("Latencia", 460f, 665f, pBody)
    canvas.drawText("Éxito (%)", 515f, 665f, pBody)
    pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawLine(40f, 672f, 555f, 672f, pLine)

    var yPos = 690f
    for (sesion in sesiones) {
        if (yPos > 800f) {
            document.finishPage(page)
            numeroPagina++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            yPos = 50f
            canvas.drawText("5. Registro de Sesiones Clínicas (Continuación)", 40f, yPos, pTitle)
            yPos += 30f
        }

        val mod = if (sesion.modulo.length > 18) sesion.modulo.take(15) + "..." else sesion.modulo
        canvas.drawText(sesion.fecha.take(10), 40f, yPos, pBody)
        canvas.drawText(mod, 120f, yPos, pBody)
        canvas.drawText("${sesion.contactosLigerosPct}%", 270f, yPos, pBody)
        canvas.drawText("${sesion.bloqueosTonicos}", 340f, yPos, pBody)
        canvas.drawText("${sesion.bloqueosClonicos}", 400f, yPos, pBody)
        canvas.drawText("${sesion.latenciaMediaMs}ms", 460f, yPos, pBody)
        canvas.drawText("${sesion.calificacionGlobal}%", 520f, yPos, pBody.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        yPos += 20f
        canvas.drawLine(40f, yPos-5f, 555f, yPos-5f, pLine)
    }

    document.finishPage(page)

    try {
        val fileName = "HCE_${perfil.nombre.replace(" ", "_")}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out -> document.writeTo(out) }
            Toast.makeText(context, "HCE Clínica guardada en Descargas", Toast.LENGTH_LONG).show()
        }
    } catch (_: Exception) {
        Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
    } finally { document.close() }
}

// --- 5. INTERFAZ DE USUARIO (Dashboard Clínico) ---

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var perfil by remember { mutableStateOf(ProgresoStorage.obtenerPerfilClinico(context)) }
    var mostrarDialogoPerfil by remember { mutableStateOf(perfil == null) }

    // Estado reactivo para refrescar la lista completa de sesiones
    var sesiones by remember { mutableStateOf(ProgresoStorage.obtenerSesionesClinicas(context)) }
    val analiticas = remember { ProgresoStorage.obtenerAnaliticasClinicas(context) }

    if (mostrarDialogoPerfil) {
        DialogoPerfilClinico(
            perfilActual = perfil,
            onDismiss = { mostrarDialogoPerfil = false },
            onGuardar = { nombre, correo, diagnostico, objetivos ->
                ProgresoStorage.guardarPerfilClinico(context, nombre, correo, diagnostico, objetivos)
                perfil = ProgresoStorage.obtenerPerfilClinico(context)
                mostrarDialogoPerfil = false
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Expediente Clínico") }, navigationIcon = { IconButton(onClick = onBack) { Icon(ArrowLeftCircleIcon, "Regresar") } }) },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                    Icon(HomeIcon, "Menú"); Spacer(modifier = Modifier.width(8.dp)); Text("Menú Principal", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                onNavigateProgreso = { /* Aquí */ },
                onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) },
                onNavigateBio = { navController.navigate(Screen.Biofeedback.route) },
                onNavigateTarjetasArticulacion = { navController.navigate(Screen.TarjetasArticulacion.route) }
            )

            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 80.dp)) {

                // CABECERA PACIENTE CON ICONO DE LÁPIZ PARA EDITAR
                item {
                    perfil?.let { p ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(p.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    // Botón de lápiz integrado al lado de la info del usuario
                                    IconButton(onClick = { mostrarDialogoPerfil = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar perfil", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Diagnóstico: ${p.diagnosticoPrincipal}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Meta SMART:\n${p.objetivosSmart}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF1976D2))
                                Text("${if(sesiones.isNotEmpty()) sesiones.map{it.calificacionGlobal}.average().toInt() else 0}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                                Text("Éxito Global", fontSize = 11.sp, color = Color.Black)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MonitorHeart, null, tint = Color(0xFF2E7D32))
                                Text("${analiticas.promedioContactosLigeros}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Text("C. Ligeros", fontSize = 11.sp, color = Color.Black)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.TrackChanges, null, tint = Color(0xFFC62828))
                                Text("${analiticas.promedioTonicos}/${analiticas.promedioClonicos}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                Text("Tónic/Clónic", fontSize = 11.sp, color = Color.Black)
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text("Evolución de Fluidez (Historial Completo)", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp))
                            GraficaFluidezInteractiva(sesiones)
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Racha de Terapia: ${analiticas.rachaDias} días", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(text = "Análisis IA: ${analiticas.tendenciaFluidez}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Button(
                                onClick = { perfil?.let { exportarHcePdf(context, it, sesiones) } },
                                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Default.Download, null); Spacer(modifier = Modifier.width(8.dp))
                                Text("Exportar Historia Clínica (PDF)")
                            }

                            OutlinedButton(
                                onClick = {
                                    exportarDatosParaML(context)
                                    Toast.makeText(context, "Dataset CSV guardado en Descargas", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = "Base de datos"); Spacer(modifier = Modifier.width(8.dp))
                                Text("Exportar Dataset para IA (.csv)")
                            }
                        }
                    }
                }

                item { Text("Registro Clínico Completo (${sesiones.size} sesiones)", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

                items(sesiones) { sesion ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sesion.modulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text("${sesion.calificacionGlobal}%", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(sesion.fecha, fontSize = 12.sp, color = Color.Gray)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tónicos: ${sesion.bloqueosTonicos} | Clónicos: ${sesion.bloqueosClonicos}", fontSize = 13.sp)
                                Text("Latencia: ${sesion.latenciaMediaMs}ms", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun exportarDatosParaML(context: Context) {
    val sesiones = obtenerSesionesClinicas(context)
    if (sesiones.isEmpty()) return

    val fileName = "SpeakTutor_Dataset.csv"
    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(path, fileName)

    try {
        val writer = FileWriter(file)
        writer.append("calificacion,tonicos,clonicos,latencia,contactos_ligeros,categoria_ideal\n")

        for (s in sesiones) {
            val categoriaIdeal = when {
                s.bloqueosTonicos > 4 -> 2
                s.bloqueosClonicos > 4 -> 0
                s.latenciaMediaMs > 2500 -> 3
                else -> 1
            }

            writer.append("${s.calificacionGlobal},${s.bloqueosTonicos},${s.bloqueosClonicos},${s.latenciaMediaMs},${s.contactosLigerosPct},$categoriaIdeal\n")
        }
        writer.flush()
        writer.close()
    } catch (_: Exception) {
        // Excepción controlada
    }
}