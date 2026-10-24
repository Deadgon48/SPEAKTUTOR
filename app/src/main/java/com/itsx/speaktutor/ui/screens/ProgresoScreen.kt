package com.itsx.speaktutor.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.platform.LocalLocale
import com.itsx.speaktutor.ui.components.AlertaInPlace
import com.itsx.speaktutor.ui.components.TonoAlerta

// --- 1. MODELOS DE DATOS CLÍNICOS ---

data class SesionClinica(
    val id: String, val fecha: String, val modulo: String,
    val aciertos: Int, val erroresTotales: Int, val calificacionGlobal: Int,
    val bloqueosTonicos: Int, val bloqueosClonicos: Int,
    val latenciaMediaMs: Int, val contactosLigerosPct: Int
)

data class PerfilClinico(
    val nombre: String, val correo: String, val fechaRegistro: String,
    val diagnosticoPrincipal: String, val objetivosSmart: String,
    val observaciones: String = "", val recomendaciones: String = "" // NUEVOS CAMPOS CLÍNICOS
)

data class AnaliticasClinicas(
    val rachaDias: Int, val tendenciaFluidez: String,
    val promedioTonicos: Int, val promedioClonicos: Int,
    val promedioContactosLigeros: Int, val latenciaPromedio: Int
)

// --- 2. MOTOR DE ALMACENAMIENTO Y RESPALDOS ---

object ProgresoStorage {
    private const val PREF_NAME = "SpeakTutorHCE_Prefs"
    private const val KEY_SESIONES = "hce_sesiones"
    private const val KEY_PERFIL = "hce_perfil_v2"
    private const val KEY_TIEMPO_USO = "hce_tiempo_diario"
    private const val KEY_ARCHIVO_FONOLOGICO = "hce_archivo_fonologico"

    fun guardarPerfilClinico(context: Context, nombre: String, correo: String, diagnostico: String, objetivos: String, observaciones: String, recomendaciones: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val diagSeguro = diagnostico.replace("\n", "\\n")
        val objSeguro = objetivos.replace("\n", "\\n")
        val obsSeguro = observaciones.replace("\n", "\\n")
        val recSeguro = recomendaciones.replace("\n", "\\n")
        val perfilString = "$nombre|$correo|$fechaActual|$diagSeguro|$objSeguro|$obsSeguro|$recSeguro"
        prefs.edit { putString(KEY_PERFIL, perfilString) }
    }

    fun obtenerPerfilClinico(context: Context): PerfilClinico? {
        val data = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_PERFIL, "") ?: ""
        if (data.isBlank()) return null
        val p = data.split("|")
        val diagRestaurado = if (p.size > 3) p[3].replace("\\n", "\n") else "No especificado"
        val objRestaurado = if (p.size > 4) p[4].replace("\\n", "\n") else "No especificado"
        val obsRestauradas = if (p.size > 5) p[5].replace("\\n", "\n") else "Sin observaciones."
        val recRestauradas = if (p.size > 6) p[6].replace("\\n", "\n") else "Sin recomendaciones."
        return PerfilClinico(p[0], p[1], p[2], diagRestaurado, objRestaurado, obsRestauradas, recRestauradas)
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

    fun registrarFalloFonologico(context: Context, silaba: String, palabra: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonActual = prefs.getString(KEY_ARCHIVO_FONOLOGICO, "{}") ?: "{}"

        try {
            val root = JSONObject(jsonActual)
            val silabaLimpia = silaba.lowercase().trim()
            val palabraLimpia = palabra.lowercase().trim()

            val objSilaba = if (root.has(silabaLimpia)) root.getJSONObject(silabaLimpia) else JSONObject()
            val fallos = if (objSilaba.has(palabraLimpia)) objSilaba.getInt(palabraLimpia) else 0

            objSilaba.put(palabraLimpia, fallos + 1)
            root.put(silabaLimpia, objSilaba)

            prefs.edit().putString(KEY_ARCHIVO_FONOLOGICO, root.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun obtenerArchivoFonologico(context: Context): Map<String, Map<String, Int>> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ARCHIVO_FONOLOGICO, "{}") ?: "{}"
        val mapa = mutableMapOf<String, Map<String, Int>>()

        try {
            val root = JSONObject(json)
            val keysSilabas = root.keys()
            while (keysSilabas.hasNext()) {
                val silaba = keysSilabas.next()
                val objSilaba = root.getJSONObject(silaba)
                val mapaPalabras = mutableMapOf<String, Int>()
                val keysPalabras = objSilaba.keys()
                while (keysPalabras.hasNext()) {
                    val palabra = keysPalabras.next()
                    mapaPalabras[palabra] = objSilaba.getInt(palabra)
                }
                mapa[silaba] = mapaPalabras
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mapa
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

    @RequiresApi(Build.VERSION_CODES.Q)
    fun crearRespaldoDatos(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val perfil = prefs.getString(KEY_PERFIL, "") ?: ""
        val sesiones = prefs.getString(KEY_SESIONES, "") ?: ""
        val tiempo = prefs.getString(KEY_TIEMPO_USO, "") ?: ""
        val fonologia = prefs.getString(KEY_ARCHIVO_FONOLOGICO, "") ?: ""

        val prefsFonologia = context.getSharedPreferences("SpeakTutorFonologia", Context.MODE_PRIVATE)
        val grafemas = prefsFonologia.getStringSet("grafemas_pendientes", emptySet())?.joinToString(";") ?: ""

        val backupData = "PERFIL=$perfil\nSESIONES=$sesiones\nTIEMPO=$tiempo\nGRAFEMAS=$grafemas\nARCHIVO_FONOLOGICO=$fonologia"
        val fileName = "SpeakTutor_Backup_${System.currentTimeMillis()}.stb"

        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(backupData.toByteArray())
                }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun restaurarRespaldo(context: Context, uri: Uri): Boolean {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return false
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val prefsFonologia = context.getSharedPreferences("SpeakTutorFonologia", Context.MODE_PRIVATE)

            content.lines().forEach { line ->
                when {
                    line.startsWith("PERFIL=") -> prefs.edit().putString(KEY_PERFIL, line.substringAfter("PERFIL=")).apply()
                    line.startsWith("SESIONES=") -> prefs.edit().putString(KEY_SESIONES, line.substringAfter("SESIONES=")).apply()
                    line.startsWith("TIEMPO=") -> prefs.edit().putString(KEY_TIEMPO_USO, line.substringAfter("TIEMPO=")).apply()
                    line.startsWith("GRAFEMAS=") -> {
                        val g = line.substringAfter("GRAFEMAS=").split(";").filter { it.isNotBlank() }.toSet()
                        prefsFonologia.edit().putStringSet("grafemas_pendientes", g).apply()
                    }
                    line.startsWith("ARCHIVO_FONOLOGICO=") -> prefs.edit().putString(KEY_ARCHIVO_FONOLOGICO, line.substringAfter("ARCHIVO_FONOLOGICO=")).apply()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

// --- 3. COMPONENTES UI ---

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

@Composable
fun MiArchivoFonologico(archivoDatos: Map<String, Map<String, Int>>) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🗂️ Mi Archivo Fonológico", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Tus sonidos con mayor índice de bloqueo y las palabras clave.", fontSize = 13.sp, color = Color.Gray)

        if (archivoDatos.isEmpty()) {
            Text("Aún no hay registros de fallos fonológicos.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 14.sp)
        } else {
            val sonidosOrdenados = archivoDatos.entries.sortedByDescending { it.value.values.sum() }

            sonidosOrdenados.forEach { (silaba, palabras) ->
                CajonFonologico(silaba = silaba, palabrasErrores = palabras)
            }
        }
    }
}

@Composable
fun CajonFonologico(silaba: String, palabrasErrores: Map<String, Int>) {
    var expandido by remember { mutableStateOf(false) }
    val totalFallos = palabrasErrores.values.sum()

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { expandido = !expandido },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFD32F2F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(silaba.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Sonido /$silaba/", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("$totalFallos bloqueos registrados", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Icon(if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "Expandir")
            }

            AnimatedVisibility(visible = expandido) {
                Column(modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
                    HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Palabras críticas:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

                    val palabrasOrdenadas = palabrasErrores.entries.sortedByDescending { it.value }
                    palabrasOrdenadas.forEach { (palabra, fallos) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase(LocalLocale.current.platformLocale) else it.toString() }, fontSize = 14.sp)
                            Text("$fallos fallos", fontSize = 14.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// --- 4. GENERADOR DE HISTORIA CLÍNICA ELECTRÓNICA (PDF MULTIPÁGINA) ---
@RequiresApi(Build.VERSION_CODES.Q)
fun exportarHcePdf(context: Context, perfil: PerfilClinico, sesiones: List<SesionClinica>): Boolean {
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

    // ==========================================
    // PÁGINA 1: PERFIL, OBJETIVOS Y OBSERVACIONES
    // ==========================================
    canvas.drawRect(0f, 0f, 595f, 60f, pHeader)
    canvas.drawText("Historia Clínica Electrónica - Logopedia (SpeakTutor)", 40f, 38f, pTextWhite)

    // 1. Datos Demográficos
    canvas.drawText("1. Datos Demográficos y Diagnóstico", 40f, 95f, pTitle)
    canvas.drawRoundRect(RectF(40f, 105f, 555f, 175f), 8f, 8f, pBox)
    canvas.drawText("Paciente: ${perfil.nombre}", 55f, 125f, pBody)
    canvas.drawText("Correo: ${perfil.correo}", 55f, 145f, pBody)
    canvas.drawText("Registro: ${perfil.fechaRegistro}", 55f, 165f, pBody)
    canvas.drawText("Diagnóstico: ${perfil.diagnosticoPrincipal}", 300f, 125f, pBody.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
    pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    fun dibujarBloqueTexto(titulo: String, texto: String, startY: Float): Float {
        canvas.drawText(titulo, 40f, startY, pTitle)
        val boxY = startY + 10f
        canvas.drawRoundRect(RectF(40f, boxY, 555f, boxY + 100f), 8f, 8f, pBox)
        val palabras = texto.split(" ")
        var linea = ""
        var yTemp = boxY + 20f
        for (palabra in palabras) {
            if (pBody.measureText("$linea $palabra") > 480f) {
                canvas.drawText(linea, 55f, yTemp, pBody)
                linea = palabra; yTemp += 16f
            } else { linea = if (linea.isEmpty()) palabra else "$linea $palabra" }
        }
        canvas.drawText(linea, 55f, yTemp, pBody)
        return boxY + 120f
    }

    var nextY = dibujarBloqueTexto("2. Objetivos Terapéuticos SMART", perfil.objetivosSmart, 210f)
    nextY = dibujarBloqueTexto("3. Observaciones Cualitativas (Sesión)", perfil.observaciones, nextY)
    dibujarBloqueTexto("4. Comunicación con Familia / Educadores", perfil.recomendaciones, nextY)
    document.finishPage(page)

    // ==========================================
    // PÁGINA 2: GRÁFICA Y REGISTRO DE SESIONES
    // ==========================================
    numeroPagina++
    pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
    page = document.startPage(pageInfo)
    canvas = page.canvas

    canvas.drawText("5. Gráfica de Evolución de Fluidez (%)", 40f, 50f, pTitle)
    canvas.drawRect(40f, 65f, 555f, 215f, Paint().apply { color = AndroidColor.rgb(250, 250, 250); style = Paint.Style.FILL })
    canvas.drawRect(40f, 65f, 555f, 215f, Paint().apply { color = AndroidColor.LTGRAY; style = Paint.Style.STROKE })

    if (sesiones.size >= 2) {
        val puntosGrafica = sesiones.reversed().map { it.calificacionGlobal.toFloat() }
        val pGrid = Paint().apply { color = AndroidColor.LTGRAY; strokeWidth = 0.5f }
        val pPath = Paint().apply { color = AndroidColor.rgb(25, 118, 210); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }
        for (i in 0..4) { val yLine = 215f - (i * 37.5f); canvas.drawLine(40f, yLine, 555f, yLine, pGrid) }
        val stepX = 515f / (puntosGrafica.size - 1).coerceAtLeast(1)
        val pathPDF = Path()
        puntosGrafica.forEachIndexed { i, valor ->
            val x = 40f + (i * stepX); val y = 215f - ((valor / 100f) * 150f)
            if (i == 0) pathPDF.moveTo(x, y) else pathPDF.lineTo(x, y)
            canvas.drawCircle(x, y, 3f, Paint().apply { color = AndroidColor.rgb(13, 71, 161) })
        }
        canvas.drawPath(pathPDF, pPath)
    } else {
        canvas.drawText("Datos insuficientes para graficar", 200f, 140f, pBody.apply { color = AndroidColor.GRAY })
    }

    canvas.drawText("6. Registro de Sesiones Clínicas Detallado", 40f, 260f, pTitle)
    canvas.drawLine(40f, 270f, 555f, 270f, pLine)
    pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Fecha", 40f, 285f, pBody); canvas.drawText("Módulo", 120f, 285f, pBody)
    canvas.drawText("C. Ligeros", 260f, 285f, pBody); canvas.drawText("Tónicos", 330f, 285f, pBody)
    canvas.drawText("Clónicos", 390f, 285f, pBody); canvas.drawText("Latencia", 460f, 285f, pBody)
    canvas.drawText("Éxito (%)", 515f, 285f, pBody)
    pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawLine(40f, 292f, 555f, 292f, pLine)

    var yPos = 310f
    for (sesion in sesiones) {
        if (yPos > 800f) {
            document.finishPage(page)
            numeroPagina++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            yPos = 50f
            canvas.drawText("6. Registro de Sesiones Clínicas (Continuación)", 40f, yPos, pTitle)
            yPos += 30f
        }
        val mod = if (sesion.modulo.length > 18) sesion.modulo.take(15) + "..." else sesion.modulo
        canvas.drawText(sesion.fecha.take(10), 40f, yPos, pBody); canvas.drawText(mod, 120f, yPos, pBody)
        canvas.drawText("${sesion.contactosLigerosPct}%", 270f, yPos, pBody); canvas.drawText("${sesion.bloqueosTonicos}", 340f, yPos, pBody)
        canvas.drawText("${sesion.bloqueosClonicos}", 400f, yPos, pBody); canvas.drawText("${sesion.latenciaMediaMs}ms", 460f, yPos, pBody)
        canvas.drawText("${sesion.calificacionGlobal}%", 520f, yPos, pBody.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        yPos += 20f
        canvas.drawLine(40f, yPos-5f, 555f, yPos-5f, pLine)
    }

    document.finishPage(page)

    // ==========================================
    // PÁGINA 3: MAPEO FONÉTICO
    // ==========================================
    numeroPagina++
    pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
    page = document.startPage(pageInfo)
    canvas = page.canvas

    canvas.drawText("7. Mapeo Fonético Estandarizado", 40f, 50f, pTitle)
    canvas.drawText("Evaluación de grafemas y sílabas trabadas según el registro del motor DSP y STT.", 40f, 70f, pBody)

    val pCheck = Paint().apply { color = AndroidColor.rgb(46, 125, 50); textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val pCross = Paint().apply { color = AndroidColor.rgb(198, 40, 40); textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

    canvas.drawRect(40f, 95f, 555f, 115f, pBox)
    canvas.drawText("Grafema", 60f, 110f, pTitle); canvas.drawText("Estado / Observación", 140f, 110f, pTitle)
    canvas.drawText("Grafema", 320f, 110f, pTitle); canvas.drawText("Estado / Observación", 400f, 110f, pTitle)

    val grafemasEstandar = listOf("pr", "br", "tr", "cr", "dr", "fr", "gr", "bl", "cl", "fl", "gl", "pl", "s", "r", "l", "rr")
    val prefsFonologia = context.getSharedPreferences("SpeakTutorFonologia", Context.MODE_PRIVATE)
    val pendientesStr = prefsFonologia.getStringSet("grafemas_pendientes", emptySet()) ?: emptySet()
    val silabasConError = pendientesStr.map { it.split("|")[0].lowercase() }

    var startYGrid = 135f
    var index = 0
    for (grafema in grafemasEstandar) {
        val isCol2 = index % 2 != 0
        val xG = if (isCol2) 320f else 60f
        val xS = if (isCol2) 400f else 140f
        val tieneError = silabasConError.any { it.contains(grafema) }

        canvas.drawText(grafema.uppercase(Locale.getDefault()), xG, startYGrid, pBody.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        if (tieneError) canvas.drawText("✗ Tensión detectada", xS, startYGrid, pCross) else canvas.drawText("✓ Fluido", xS, startYGrid, pCheck)

        if (isCol2) {
            canvas.drawLine(40f, startYGrid + 8f, 555f, startYGrid + 8f, pLine)
            startYGrid += 25f
        }
        index++
    }
    document.finishPage(page)

    // Exportación Final
    return try {
        val fileName = "HCE_${perfil.nombre.replace(" ", "_")}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out -> document.writeTo(out) }
            true
        } else false
    } catch (_: Exception) {
        false
    } finally { document.close() }
}

fun exportarDatosParaML(context: Context): Boolean {
    val sesiones = ProgresoStorage.obtenerSesionesClinicas(context)
    if (sesiones.isEmpty()) return false
    val fileName = "SpeakTutor_Dataset.csv"
    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(path, fileName)
    return try {
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
        true
    } catch (_: Exception) {
        false
    }
}

// --- 5. INTERFAZ DE USUARIO ---

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var perfil by remember { mutableStateOf(ProgresoStorage.obtenerPerfilClinico(context)) }
    var mostrarDialogoPerfil by remember { mutableStateOf(perfil == null) }
    var sesiones by remember { mutableStateOf(ProgresoStorage.obtenerSesionesClinicas(context)) }
    val analiticas = remember { ProgresoStorage.obtenerAnaliticasClinicas(context) }
    val archivoFonologico = remember { ProgresoStorage.obtenerArchivoFonologico(context) }

    // Estado para notificaciones transitorias y dinámicas
    var alertaSistema by remember { mutableStateOf<Pair<TonoAlerta, String>?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val exito = ProgresoStorage.restaurarRespaldo(context, it)
            if (exito) {
                alertaSistema = Pair(TonoAlerta.EXITO, "Progreso restaurado correctamente. Los datos han sido actualizados.")
                perfil = ProgresoStorage.obtenerPerfilClinico(context)
                sesiones = ProgresoStorage.obtenerSesionesClinicas(context)
            } else {
                alertaSistema = Pair(TonoAlerta.PELIGRO, "El archivo es inválido o está dañado. No se pudo restaurar.")
            }
        }
    }

    if (mostrarDialogoPerfil) {
        DialogoPerfilClinico(
            perfilActual = perfil,
            onDismiss = { if (perfil != null) mostrarDialogoPerfil = false },
            onGuardar = { nombre, correo, diagnostico, objetivos, observaciones, recomendaciones ->
                ProgresoStorage.guardarPerfilClinico(context, nombre, correo, diagnostico, objetivos, observaciones, recomendaciones)
                perfil = ProgresoStorage.obtenerPerfilClinico(context)
                mostrarDialogoPerfil = false
                alertaSistema = Pair(TonoAlerta.EXITO, "¡Expediente configurado con éxito!")
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
                                    IconButton(onClick = { mostrarDialogoPerfil = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar perfil", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Diagnóstico: ${p.diagnosticoPrincipal}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Meta SMART:\n${p.objetivosSmart}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Recomendaciones para casa:\n${p.recomendaciones}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
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

                // ==========================================
                // SECCIÓN DE ALERTAS Y NOTIFICACIONES DINÁMICAS
                // ==========================================
                item {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        // 1. Notificaciones Transitorias del Sistema (Ej: Restaurar datos)
                        alertaSistema?.let { (tono, mensaje) ->
                            AlertaInPlace(
                                tono = tono,
                                titulo = if (tono == TonoAlerta.EXITO) "Operación Exitosa" else "Atención",
                                mensaje = mensaje,
                                onClose = { alertaSistema = null }
                            )
                        }

                        // 2. Notificación Clínica de Racha Activa (Motivación)
                        if (analiticas.rachaDias >= 3) {
                            AlertaInPlace(
                                tono = TonoAlerta.EXITO,
                                titulo = "¡Racha de ${analiticas.rachaDias} días consecutivos!",
                                mensaje = "Tu constancia está fortaleciendo tus rutas neuronales y mejorando la memoria muscular fonatoria. Sigue así."
                            )
                        }

                        // 3. Notificación de Tendencias Clínicas
                        if (sesiones.size < 5) {
                            AlertaInPlace(
                                tono = TonoAlerta.INFO,
                                titulo = "Perfil en calibración",
                                mensaje = "Completa al menos ${5 - sesiones.size} sesiones más para que la IA genere tu gráfica de evolución y tendencias precisas."
                            )
                        } else if (analiticas.tendenciaFluidez.contains("Aumento") || analiticas.tendenciaFluidez.contains("baja", ignoreCase = true)) {
                            AlertaInPlace(
                                tono = TonoAlerta.ADVERTENCIA,
                                titulo = "Atención a la Fluidez",
                                mensaje = "Hemos detectado un aumento en la tensión vocal recientemente. Te recomendamos priorizar la rutina de 'Inicios Suaves'."
                            )
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

                // TARJETA DE ACCIONES LIMPIA (Sin notificaciones apiladas y duras)
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Gestión de Datos y Respaldo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                            Button(
                                onClick = {
                                    perfil?.let {
                                        val exito = exportarHcePdf(context, it, sesiones)
                                        if (exito) alertaSistema = Pair(TonoAlerta.EXITO, "Historia Clínica Electrónica guardada en Descargas.")
                                        else alertaSistema = Pair(TonoAlerta.PELIGRO, "Hubo un error al generar el PDF.")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null); Spacer(modifier = Modifier.width(8.dp))
                                Text("Exportar Historia y Mapeo Fonético (PDF)")
                            }

                            Button(
                                onClick = {
                                    val exito = ProgresoStorage.crearRespaldoDatos(context)
                                    if (exito) alertaSistema = Pair(TonoAlerta.EXITO, "Respaldo (.stb) guardado correctamente en Descargas.")
                                    else alertaSistema = Pair(TonoAlerta.PELIGRO, "Error al crear el archivo de respaldo.")
                                },
                                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                            ) {
                                Icon(Icons.Default.Backup, null); Spacer(modifier = Modifier.width(8.dp))
                                Text("Crear Archivo de Respaldo (.stb)")
                            }

                            OutlinedButton(
                                onClick = { restoreLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = "Restaurar"); Spacer(modifier = Modifier.width(8.dp))
                                Text("Restaurar Progreso desde Archivo")
                            }

                            OutlinedButton(
                                onClick = {
                                    val exito = exportarDatosParaML(context)
                                    if (exito) alertaSistema = Pair(TonoAlerta.EXITO, "Dataset CSV para Machine Learning exportado con éxito.")
                                    else alertaSistema = Pair(TonoAlerta.PELIGRO, "No se pudo exportar el Dataset o no hay datos suficientes.")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = "Base de datos"); Spacer(modifier = Modifier.width(8.dp))
                                Text("Exportar Dataset de IA (.csv)")
                            }
                        }
                    }
                }

                item { MiArchivoFonologico(archivoDatos = archivoFonologico) }
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