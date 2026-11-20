package com.itsx.speaktutor.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.navigation.NavController
import com.itsx.speaktutor.ui.components.AlertaInPlace
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.DialogoPerfilClinico
import com.itsx.speaktutor.ui.components.HomeIcon
import com.itsx.speaktutor.ui.components.TonoAlerta
import com.itsx.speaktutor.ui.navigation.Screen
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
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
    val latenciaMediaMs: Int, val contactosLigerosPct: Int,
    val impactoPsicosocial: Int = 0,
    val dafDelayMs: Int = 50,
    val fafPitchOctavas: Float = 0f
)

data class PerfilClinico(
    val nombre: String, val correo: String, val fechaRegistro: String,
    val diagnosticoPrincipal: String, val objetivosSmart: String,
)

data class AnaliticasClinicas(
    val rachaDias: Int, val tendenciaFluidez: String,
    val promedioTonicos: Int, val promedioClonicos: Int,
    val promedioContactosLigeros: Int, val latenciaPromedio: Int
)

// Helper seguro para extraer MM-dd de fechas yyyy-MM-dd HH:mm sin riesgo de IndexOutOfBounds
private fun formatearFechaCorta(fecha: String): String {
    return if (fecha.length >= 10) fecha.substring(5, 10) else fecha.take(5)
}

// --- 2. MOTOR DE ALMACENAMIENTO Y RESPALDOS (Base de datos local) ---
object ProgresoStorage {
    private const val PREF_NAME = "SpeakTutorHCE_Prefs"
    private const val KEY_SESIONES = "hce_sesiones"
    private const val KEY_PERFIL = "hce_perfil_v2"
    private const val KEY_TIEMPO_USO = "hce_tiempo_diario"
    private const val KEY_ARCHIVO_FONOLOGICO = "hce_archivo_fonologico"
    private const val KEY_RECOMPENSAS = "hce_recompensas_desbloqueadas"

    class WebAppInterface(private val onClose: () -> Unit) {
        @JavascriptInterface
        fun cerrarWebView() {
            onClose()
        }
    }

    data class Recompensa3D(
        val titulo: String,
        val descripcion: String,
        val htmlFile: String,
        val desbloqueada: Boolean,
        val icono: androidx.compose.ui.graphics.vector.ImageVector
    )

    fun obtenerRecompensasDesbloqueadas(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_RECOMPENSAS, emptySet()) ?: emptySet()
    }

    fun registrarRecompensaDesbloqueada(context: Context, idRecompensa: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val actuales = (prefs.getStringSet(KEY_RECOMPENSAS, emptySet()) ?: emptySet()).toMutableSet()
        if (actuales.add(idRecompensa)) {
            prefs.edit { putStringSet(KEY_RECOMPENSAS, actuales) }
        }
    }

    fun guardarPerfilClinico(context: Context, nombre: String, correo: String, diagnostico: String, objetivos: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val diagSeguro = diagnostico.replace("\n", "\\n")
        val objSeguro = objetivos.replace("\n", "\\n")
        val perfilString = "$nombre|$correo|$fechaActual|$diagSeguro|$objSeguro"
        prefs.edit { putString(KEY_PERFIL, perfilString) }
    }

    fun obtenerPerfilClinico(context: Context): PerfilClinico? {
        val data = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_PERFIL, "") ?: ""
        if (data.isBlank()) return null
        val p = data.split("|")
        val diagRestaurado = if (p.size > 3) p[3].replace("\\n", "\n") else "No especificado"
        val objRestaurado = if (p.size > 4) p[4].replace("\\n", "\n") else "No especificado"

        return PerfilClinico(p[0], p[1], p[2], diagRestaurado, objRestaurado)
    }

    fun guardarSesionMultidimensional(
        context: Context, modulo: String, aciertos: Int, erroresTotales: Int, calificacion: Int,
        bloqueosTonicos: Int = 0, bloqueosClonicos: Int = 0, latenciaMedia: Int = 0,
        contactosLigeros: Int = 0, impactoPsicosocial: Int = 0, dafDelayMs: Int = 50, fafPitchOctavas: Float = 0f
    ) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val sesionesActuales = obtenerSesionesClinicas(context).toMutableList()
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val nueva = SesionClinica(
            UUID.randomUUID().toString(), fecha, modulo, aciertos, erroresTotales, calificacion,
            bloqueosTonicos, bloqueosClonicos, latenciaMedia, contactosLigeros, impactoPsicosocial,
            dafDelayMs, fafPitchOctavas
        )
        sesionesActuales.add(0, nueva)
        val sb = StringBuilder()
        for (s in sesionesActuales) {
            sb.append("${s.id}|${s.fecha}|${s.modulo}|${s.aciertos}|${s.erroresTotales}|${s.calificacionGlobal}|${s.bloqueosTonicos}|${s.bloqueosClonicos}|${s.latenciaMediaMs}|${s.contactosLigerosPct}|${s.impactoPsicosocial}|${s.dafDelayMs}|${s.fafPitchOctavas}#")
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
                lista.add(SesionClinica(
                    p[0], p[1], p[2], p[3].toIntOrNull() ?: 0, p[4].toIntOrNull() ?: 0, p[5].toIntOrNull() ?: 0,
                    p[6].toIntOrNull() ?: 0, p[7].toIntOrNull() ?: 0, p[8].toIntOrNull() ?: 0, p[9].toIntOrNull() ?: 0,
                    if (p.size > 10) p[10].toIntOrNull() ?: 0 else 0,
                    if (p.size > 11) p[11].toIntOrNull() ?: 50 else 50,
                    if (p.size > 12) p[12].toFloatOrNull() ?: 0f else 0f
                ))
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
        } catch (e: Exception) { e.printStackTrace() }
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
        } catch (e: Exception) { e.printStackTrace() }
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
        val recompensas = obtenerRecompensasDesbloqueadas(context).joinToString(";")

        val prefsFonologia = context.getSharedPreferences("SpeakTutorFonologia", Context.MODE_PRIVATE)
        val grafemas = prefsFonologia.getStringSet("grafemas_pendientes", emptySet())?.joinToString(";") ?: ""

        val backupData = "PERFIL=$perfil\nSESIONES=$sesiones\nTIEMPO=$tiempo\nGRAFEMAS=$grafemas\nARCHIVO_FONOLOGICO=$fonologia\nRECOMPENSAS=$recompensas"
        val fileName = "SpeakTutor_Backup_${System.currentTimeMillis()}.stb"

        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out -> out.write(backupData.toByteArray()) }
                true
            } else false
        } catch (e: Exception) { false }
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
                    line.startsWith("RECOMPENSAS=") -> {
                        val r = line.substringAfter("RECOMPENSAS=").split(";").filter { it.isNotBlank() }.toSet()
                        prefs.edit().putStringSet(KEY_RECOMPENSAS, r).apply()
                    }
                }
            }
            true
        } catch (e: Exception) { false }
    }
}

// --- 3. COMPONENTES UI Y GRÁFICAS ---
@Composable
fun GraficaUsoDiario(registrosTiempo: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("⏱️ Registro de Uso Diario (Minutos por Día)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Control de tiempo de actividad en la app guardado localmente.", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            if (registrosTiempo.isEmpty()) {
                Text("Aún no hay registros de tiempo de uso.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp)
            } else {
                val ultimosDias = registrosTiempo.entries.take(7).reversed()
                val maxMinutos = (ultimosDias.maxOfOrNull { it.value } ?: 30).coerceAtLeast(15).toFloat()

                Row(modifier = Modifier.fillMaxWidth().height(140.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Eje Y (Minutos)
                    Column(
                        modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("${maxMinutos.toInt()}m", fontSize = 9.sp, color = Color.Gray)
                        Text("${(maxMinutos / 2).toInt()}m", fontSize = 9.sp, color = Color.Gray)
                        Text("0m", fontSize = 9.sp, color = Color.Gray)
                    }

                    Canvas(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 4.dp)) {
                        val width = size.width
                        val height = size.height
                        val stepX = width / ultimosDias.size.coerceAtLeast(1)

                        for (i in 0..2) {
                            val y = height - (height * (i.toFloat() / 2))
                            drawLine(color = Color.LightGray.copy(alpha = 0.4f), start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
                        }

                        ultimosDias.forEachIndexed { index, entry ->
                            val x = (index * stepX) + (stepX / 2f)
                            val barHeight = (entry.value / maxMinutos) * (height - 10f)
                            val y = height - barHeight

                            drawRect(
                                color = Color(0xFF1976D2),
                                topLeft = Offset(x - 16f, y),
                                size = Size(32f, barHeight)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(start = 28.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    ultimosDias.forEach { (fecha, minutos) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${minutos}m", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(fecha.takeLast(5), fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GraficaFluidezInteractiva(sesiones: List<SesionClinica>) {
    if (sesiones.isEmpty()) {
        Text("Aún no hay sesiones registradas para graficar.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
        return
    }

    val datosOrdenados = sesiones.reversed()
    val datosGrafica = datosOrdenados.map { it.calificacionGlobal.toFloat() }
    val maxValor = 100f

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(180.dp), verticalAlignment = Alignment.CenterVertically) {
            // Eje Y (Porcentajes)
            Column(
                modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("100%", fontSize = 10.sp, color = Color.Gray)
                Text("75%", fontSize = 10.sp, color = Color.Gray)
                Text("50%", fontSize = 10.sp, color = Color.Gray)
                Text("25%", fontSize = 10.sp, color = Color.Gray)
                Text("0%", fontSize = 10.sp, color = Color.Gray)
            }

            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val width = size.width
                val height = size.height
                val stepX = if (datosGrafica.size > 1) width / (datosGrafica.size - 1) else width / 2f

                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = height - (height * (i.toFloat() / gridLines))
                    drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = Offset(0f, y), end = Offset(width, y), strokeWidth = 2f)
                }

                val path = ComposePath()
                val puntos = mutableListOf<Offset>()

                datosGrafica.forEachIndexed { index, valor ->
                    val x = if (datosGrafica.size > 1) index * stepX else width / 2f
                    val y = height - ((valor / maxValor) * height)
                    puntos.add(Offset(x, y))
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                if (datosGrafica.size > 1) {
                    drawPath(
                        path = path,
                        color = Color(0xFF1976D2),
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                puntos.forEach { point ->
                    drawCircle(color = Color(0xFF0D47A1), radius = 8f, center = point)
                    drawCircle(color = Color.White, radius = 4f, center = point)
                }
            }
        }

        // Eje X (Fechas y Calificación)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val muestrasX = if (datosOrdenados.size <= 5) datosOrdenados else listOf(datosOrdenados.first(), datosOrdenados[datosOrdenados.size / 2], datosOrdenados.last())
            muestrasX.forEach { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${s.calificacionGlobal}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    Text(formatearFechaCorta(s.fecha), fontSize = 9.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun GraficaEvolucionLatencia(sesiones: List<SesionClinica>) {
    if (sesiones.isEmpty()) return

    val datosOrdenados = sesiones.reversed()
    val datosLatencia = datosOrdenados.map { it.latenciaMediaMs.toFloat() }
    val maxValor = (datosLatencia.maxOrNull() ?: 2000f).coerceAtLeast(1000f)

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Evolución de Latencia (Tiempo de reacción en ms)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().height(140.dp), verticalAlignment = Alignment.CenterVertically) {
                // Eje Y (Milisegundos)
                Column(
                    modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("${maxValor.toInt()}ms", fontSize = 9.sp, color = Color.Gray)
                    Text("${(maxValor / 2).toInt()}ms", fontSize = 9.sp, color = Color.Gray)
                    Text("0ms", fontSize = 9.sp, color = Color.Gray)
                }

                Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val width = size.width
                    val height = size.height
                    val stepX = if (datosLatencia.size > 1) width / (datosLatencia.size - 1) else width / 2f

                    for (i in 0..2) {
                        val y = height - (height * (i.toFloat() / 2))
                        drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
                    }

                    val path = ComposePath()
                    datosLatencia.forEachIndexed { index, valor ->
                        val x = if (datosLatencia.size > 1) index * stepX else width / 2f
                        val y = height - ((valor / maxValor) * height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(color = Color(0xFFF57C00), radius = 6f, center = Offset(x, y))
                    }

                    if (datosLatencia.size > 1) {
                        drawPath(
                            path = path,
                            color = Color(0xFFFFB74D),
                            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }

            // Eje X (Fechas y valores en ms)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 42.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val muestrasX = if (datosOrdenados.size <= 4) datosOrdenados else listOf(datosOrdenados.first(), datosOrdenados[datosOrdenados.size / 2], datosOrdenados.last())
                muestrasX.forEach { s ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${s.latenciaMediaMs}ms", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
                        Text(formatearFechaCorta(s.fecha), fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Nota: Valores menores indican un inicio de fonación más rápido y sin bloqueos iniciales.", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun RendimientoPorModulo(sesiones: List<SesionClinica>) {
    if (sesiones.isEmpty()) return

    val rendimiento = sesiones
        .groupBy { it.modulo }
        .mapValues { entry -> entry.value.map { it.calificacionGlobal }.average().toInt() }
        .entries.sortedByDescending { it.value }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Rendimiento Promedio por Módulo Clínico", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            rendimiento.forEach { (modulo, calificacion) ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(modulo, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("$calificacion%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.LightGray)) {
                        Box(modifier = Modifier.fillMaxWidth(calificacion / 100f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color(0xFF1976D2)))
                    }
                }
            }
        }
    }
}

@Composable
fun GraficaHistorialBloqueos(sesiones: List<SesionClinica>) {
    if (sesiones.isEmpty()) return
    val ultimas = sesiones.take(10).reversed()
    val maxBloqueos = ultimas.maxOfOrNull { it.bloqueosTonicos + it.bloqueosClonicos }?.coerceAtLeast(4)?.toFloat() ?: 4f

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Historial de Tensión (Últimas 10 sesiones)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().height(150.dp), verticalAlignment = Alignment.CenterVertically) {
                // Eje Y (Cantidad de bloqueos)
                Column(
                    modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("${maxBloqueos.toInt()}", fontSize = 10.sp, color = Color.Gray)
                    Text("${(maxBloqueos / 2).toInt()}", fontSize = 10.sp, color = Color.Gray)
                    Text("0", fontSize = 10.sp, color = Color.Gray)
                }

                Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val w = size.width
                    val h = size.height
                    val stepX = w / ultimas.size
                    val barW = (stepX * 0.5f).coerceAtMost(48f)

                    for (i in 0..2) {
                        val y = h - (h * (i / 2f))
                        drawLine(color = Color.LightGray.copy(0.5f), start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
                    }

                    ultimas.forEachIndexed { index, sesion ->
                        val x = (index * stepX) + (stepX - barW) / 2f
                        val hTonico = (sesion.bloqueosTonicos / maxBloqueos) * h
                        val hClonico = (sesion.bloqueosClonicos / maxBloqueos) * h

                        drawRect(color = Color(0xFFD32F2F), topLeft = Offset(x, h - hTonico), size = Size(barW, hTonico))
                        drawRect(color = Color(0xD6D60DDD), topLeft = Offset(x, h - hTonico - hClonico), size = Size(barW, hClonico))
                    }
                }
            }

            // Eje X (Total de bloqueos y día por barra)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp), horizontalArrangement = Arrangement.SpaceAround) {
                ultimas.forEach { sesion ->
                    val totalB = sesion.bloqueosTonicos + sesion.bloqueosClonicos
                    val diaMes = if (sesion.fecha.length >= 10) sesion.fecha.substring(8, 10) else sesion.fecha.take(2)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$totalB", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text(diaMes, fontSize = 9.sp, color = Color.Gray)
                    }
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
                        Text(if (totalFallos == 1) "1 bloqueo registrado" else "$totalFallos bloqueos registrados", fontSize = 12.sp, color = Color.Gray)
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
                            Text(palabra, fontSize = 14.sp)
                            Text(if (fallos == 1) "1 fallo" else "$fallos fallos", fontSize = 14.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GraficaDistribucionBloqueos(tonicos: Int, clonicos: Int) {
    val total = (tonicos + clonicos).coerceAtLeast(1).toFloat()
    val sweepTonicos = (tonicos / total) * 360f
    val sweepClonicos = (clonicos / total) * 360f

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(90.dp)) {
                drawArc(color = Color(0xFFD32F2F), startAngle = -90f, sweepAngle = sweepTonicos, useCenter = false, style = Stroke(width = 30f))
                drawArc(color = Color(0xD6D60DDD), startAngle = -90f + sweepTonicos, sweepAngle = sweepClonicos, useCenter = false, style = Stroke(width = 30f))
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text("Distribución de Bloqueos", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFFD32F2F)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tónicos: $tonicos", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xD6D60DDD)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clónicos: $clonicos", fontSize = 13.sp)
                }
            }
        }
    }
}

// --- 4. GENERADOR DE HISTORIA CLÍNICA ELECTRÓNICA Y PDF CON PAGINACIÓN DINÁMICA ---
@RequiresApi(Build.VERSION_CODES.Q)
fun exportarHcePdf(context: Context, perfil: PerfilClinico, sesiones: List<SesionClinica>, registrosTiempo: Map<String, Int>): File? {
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
    val pGrid = Paint().apply { color = AndroidColor.LTGRAY; strokeWidth = 1f; style = Paint.Style.STROKE }
    val pAxisText = Paint().apply { color = AndroidColor.DKGRAY; textSize = 8.5f }

    // PÁGINA 1
    canvas.drawRect(0f, 0f, 595f, 60f, pHeader)
    canvas.drawText("Historia Clínica Electrónica - Logopedia (SpeakTutor)", 40f, 38f, pTextWhite)

    canvas.drawText("1. Datos Demográficos y Diagnóstico", 40f, 95f, pTitle)
    canvas.drawRoundRect(RectF(40f, 105f, 555f, 175f), 8f, 8f, pBox)
    canvas.drawText("Paciente: ${perfil.nombre}", 55f, 125f, pBody)
    canvas.drawText("Correo: ${perfil.correo}", 55f, 145f, pBody)
    canvas.drawText("Registro: ${perfil.fechaRegistro}", 55f, 165f, pBody)
    canvas.drawText("Diagnóstico: ${perfil.diagnosticoPrincipal}", 300f, 125f, pBody.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
    pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    fun dibujarBloqueTexto(titulo: String, texto: String, startY: Float): Float {
        if (titulo.isNotEmpty()) canvas.drawText(titulo, 40f, startY, pTitle)
        val boxY = startY + (if (titulo.isNotEmpty()) 10f else 0f)
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

    dibujarBloqueTexto("2. Objetivos Terapéuticos SMART", perfil.objetivosSmart, 210f)
    document.finishPage(page)

    // PÁGINA 2
    numeroPagina++
    pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
    page = document.startPage(pageInfo)
    canvas = page.canvas

    canvas.drawText("5. Gráfica de Evolución de Fluidez (Calificación Global %)", 40f, 45f, pTitle)
    canvas.drawRect(RectF(40f, 55f, 555f, 205f), pBox)

    val chartLeft = 75f
    val chartTop = 70f
    val chartRight = 530f
    val chartBottom = 175f
    val chartWidth = chartRight - chartLeft
    val chartHeight = chartBottom - chartTop

    val pDataLabel = Paint().apply { color = AndroidColor.rgb(13, 71, 161); textSize = 8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

    canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, pGrid)
    canvas.drawLine(chartLeft, (chartTop + chartBottom) / 2f, chartRight, (chartTop + chartBottom) / 2f, pGrid)
    canvas.drawLine(chartLeft, chartTop, chartRight, chartTop, pGrid)
    canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, pGrid)

    canvas.drawText("100%", chartLeft - 30f, chartTop + 4f, pAxisText)
    canvas.drawText("50%", chartLeft - 25f, (chartTop + chartBottom) / 2f + 4f, pAxisText)
    canvas.drawText("0%", chartLeft - 20f, chartBottom + 4f, pAxisText)

    val sesionesRev = sesiones.reversed()
    if (sesionesRev.isNotEmpty()) {
        val stepXPdf = if (sesionesRev.size > 1) chartWidth / (sesionesRev.size - 1) else chartWidth / 2f
        val pLineChart = Paint().apply { color = AndroidColor.rgb(25, 118, 210); strokeWidth = 3f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        val pPoint = Paint().apply { color = AndroidColor.rgb(13, 71, 161); style = Paint.Style.FILL }

        var prevX = -1f
        var prevY = -1f
        sesionesRev.forEachIndexed { index, sesion ->
            val valor = sesion.calificacionGlobal.toFloat()
            val x = if (sesionesRev.size > 1) chartLeft + (index * stepXPdf) else chartLeft + chartWidth / 2f
            val y = chartBottom - ((valor / 100f) * chartHeight)

            if (prevX != -1f) {
                canvas.drawLine(prevX, prevY, x, y, pLineChart)
            }
            canvas.drawCircle(x, y, 4f, pPoint)

            if (sesionesRev.size <= 12 || index % 2 == 0 || index == sesionesRev.lastIndex) {
                canvas.drawText("${sesion.calificacionGlobal}%", x - 8f, (y - 7f).coerceAtLeast(chartTop + 8f), pDataLabel)
                canvas.drawText(formatearFechaCorta(sesion.fecha), x - 11f, chartBottom + 13f, pAxisText)
            }
            prevX = x
            prevY = y
        }
    } else {
        canvas.drawText("No hay sesiones registradas para mostrar en la gráfica.", chartLeft + 15f, chartTop + 50f, pBody)
    }

    // BLOQUE PDF: DATOS MULTIDIMENSIONALES Y RADAR NATIVO
    val yM = 225f
    canvas.drawText("7. Análisis Multidimensional de Habilidades", 40f, yM, pTitle)
    canvas.drawLine(40f, yM + 6f, 555f, yM + 6f, pLine)

    val ptAvanceFonologia = sesiones.filter { it.modulo.contains("Fonológica") }.map { it.calificacionGlobal }.average().takeIf { !it.isNaN() } ?: 0.0
    val ptAvanceEstirada = sesiones.filter { it.modulo.contains("Habla Estirada") }.map { it.calificacionGlobal }.average().takeIf { !it.isNaN() } ?: 0.0
    val ptAvanceLlamadas = sesiones.filter { it.modulo.contains("Llamada") }.map { it.calificacionGlobal }.average().takeIf { !it.isNaN() } ?: 0.0
    val ptAvancePraxias = sesiones.filter { it.modulo.contains("Praxias") }.map { it.calificacionGlobal }.average().takeIf { !it.isNaN() } ?: 0.0

    val latenciaPdf = sesiones.map { it.latenciaMediaMs }.average().takeIf { !it.isNaN() } ?: 0.0
    val valFluidez = (ptAvanceEstirada / 100f).toFloat().coerceIn(0.1f, 1f)
    val valPronunciacion = (ptAvanceFonologia / 100f).toFloat().coerceIn(0.1f, 1f)
    val valTono = (ptAvancePraxias / 100f).toFloat().coerceIn(0.1f, 1f)
    val valSimulacion = (ptAvanceLlamadas / 100f).toFloat().coerceIn(0.1f, 1f)
    val valRitmo = (1f - (latenciaPdf / 2500f).toFloat()).coerceIn(0.1f, 1f)
    val radarValues = listOf(valFluidez, valPronunciacion, valRitmo, valTono, valSimulacion)

    canvas.drawText("• Conciencia Fonológica (Articulación): ${ptAvanceFonologia.toInt()}%", 50f, yM + 26f, pBody)
    canvas.drawText("• Habla Estirada (Control de Velocidad): ${ptAvanceEstirada.toInt()}%", 50f, yM + 42f, pBody)
    canvas.drawText("• Praxias Faciales (Tono/Relajación): ${ptAvancePraxias.toInt()}%", 50f, yM + 58f, pBody)
    canvas.drawText("• Simulador (Desensibilización): ${ptAvanceLlamadas.toInt()}%", 50f, yM + 74f, pBody)

    val cx = 450f
    val cy = yM + 60f
    val rMax = 42f
    val pRadarGrid = Paint().apply { color = AndroidColor.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 1f }
    val pRadarFill = Paint().apply { color = AndroidColor.argb(80, 25, 118, 210); style = Paint.Style.FILL }
    val pRadarLine = Paint().apply { color = AndroidColor.rgb(25, 118, 210); style = Paint.Style.STROKE; strokeWidth = 2f }
    val pRadarText = Paint().apply { color = AndroidColor.DKGRAY; textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val radarLabels = listOf("Fluidez", "Articulación", "Ritmo", "Tono", "Simulador")

    for (step in 1..4) {
        val stepR = rMax * (step / 4f)
        val path = android.graphics.Path()
        for (i in 0 until 5) {
            val angle = i * (2 * Math.PI / 5) - (Math.PI / 2)
            val x = cx + stepR * Math.cos(angle).toFloat()
            val y = cy + stepR * Math.sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, pRadarGrid)
    }

    val pathVal = android.graphics.Path()
    for (i in 0 until 5) {
        val angle = i * (2 * Math.PI / 5) - (Math.PI / 2)
        val vR = rMax * radarValues[i]
        val x = cx + vR * Math.cos(angle).toFloat()
        val y = cy + vR * Math.sin(angle).toFloat()
        if (i == 0) pathVal.moveTo(x, y) else pathVal.lineTo(x, y)

        val endX = cx + rMax * Math.cos(angle).toFloat()
        val endY = cy + rMax * Math.sin(angle).toFloat()
        canvas.drawLine(cx, cy, endX, endY, pRadarGrid)

        val labelX = cx + (rMax + 15f) * Math.cos(angle).toFloat() - 14f
        val labelY = cy + (rMax + 11f) * Math.sin(angle).toFloat() + 3f
        canvas.drawText(radarLabels[i], labelX, labelY, pRadarText)
    }
    pathVal.close()
    canvas.drawPath(pathVal, pRadarFill)
    canvas.drawPath(pathVal, pRadarLine)

    val tonicosTotales = sesiones.sumOf { it.bloqueosTonicos }
    val clonicosTotales = sesiones.sumOf { it.bloqueosClonicos }
    val tiempoTotal = registrosTiempo.values.sum()
    canvas.drawText("Incidencias Acústicas Históricas:", 50f, yM + 104f, pTitle)
    canvas.drawText("Bloqueos Tónicos: $tonicosTotales   |   Bloqueos Clónicos: $clonicosTotales   |   Tiempo en app: $tiempoTotal min", 50f, yM + 122f, pBody.apply { color = AndroidColor.RED })
    pBody.color = AndroidColor.BLACK

    document.finishPage(page)

    // PÁGINA 3 - REGISTRO CLÍNICO DE SESIONES
    numeroPagina++
    pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
    page = document.startPage(pageInfo)
    canvas = page.canvas

    canvas.drawText("8. Registro Clínico Completo (${sesiones.size} Sesiones Registradas)", 40f, 45f, pTitle)
    canvas.drawLine(40f, 55f, 555f, 55f, pLine)
    var ySesiones = 75f

    pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Fecha", 40f, ySesiones, pBody)
    canvas.drawText("Módulo", 130f, ySesiones, pBody)
    canvas.drawText("C.Ligeros", 300f, ySesiones, pBody)
    canvas.drawText("Tón/Clón", 380f, ySesiones, pBody)
    canvas.drawText("Éxito", 500f, ySesiones, pBody)
    pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    ySesiones += 20f

    for (sesion in sesiones) {
        if (ySesiones > 780f) {
            document.finishPage(page)
            numeroPagina++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas

            canvas.drawText("8. Registro Clínico Completo (Continuación)", 40f, 45f, pTitle)
            canvas.drawLine(40f, 55f, 555f, 55f, pLine)
            ySesiones = 75f

            pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Fecha", 40f, ySesiones, pBody)
            canvas.drawText("Módulo", 130f, ySesiones, pBody)
            canvas.drawText("C.Ligeros", 300f, ySesiones, pBody)
            canvas.drawText("Tón/Clón", 380f, ySesiones, pBody)
            canvas.drawText("Éxito", 500f, ySesiones, pBody)
            pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            ySesiones += 20f
        }

        canvas.drawText(sesion.fecha.take(16), 40f, ySesiones, pBody)
        canvas.drawText(sesion.modulo.take(20), 130f, ySesiones, pBody)
        canvas.drawText("${sesion.contactosLigerosPct}%", 310f, ySesiones, pBody)
        canvas.drawText("${sesion.bloqueosTonicos}/${sesion.bloqueosClonicos}", 390f, ySesiones, pBody)
        canvas.drawText("${sesion.calificacionGlobal}%", 500f, ySesiones, pBody)
        ySesiones += 18f
    }
    document.finishPage(page)

    // PÁGINA 4: MAPEO FONÉTICO
    numeroPagina++
    pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
    page = document.startPage(pageInfo)
    canvas = page.canvas

    canvas.drawText("9. Mapeo Fonético Estandarizado", 40f, 45f, pTitle)
    canvas.drawText("Evaluación de grafemas y sílabas trabadas según el registro del motor DSP y STT.", 40f, 65f, pBody)

    var yFonetica = 95f
    canvas.drawRect(40f, yFonetica, 555f, yFonetica + 20f, pBox)
    canvas.drawText("Grafema", 60f, yFonetica + 14f, pTitle); canvas.drawText("Estado", 140f, yFonetica + 14f, pTitle)
    canvas.drawText("Grafema", 320f, yFonetica + 14f, pTitle); canvas.drawText("Estado", 400f, yFonetica + 14f, pTitle)
    yFonetica += 32f

    val grafemasEstandar = listOf("pr", "br", "tr", "cr", "dr", "fr", "gr", "bl", "cl", "fl", "gl", "pl", "s", "r", "l", "rr")
    val prefsFonologia = context.getSharedPreferences("SpeakTutorFonologia", Context.MODE_PRIVATE)
    val pendientesStr = prefsFonologia.getStringSet("grafemas_pendientes", emptySet()) ?: emptySet()
    val silabasConError = pendientesStr.map { it.split("|")[0].lowercase() }

    val pCheck = Paint().apply { color = AndroidColor.rgb(46, 125, 50); textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val pCross = Paint().apply { color = AndroidColor.rgb(198, 40, 40); textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

    var index = 0
    for (grafema in grafemasEstandar) {
        val isCol2 = index % 2 != 0
        val xG = if (isCol2) 320f else 60f
        val xS = if (isCol2) 400f else 140f
        val tieneError = silabasConError.any { it.contains(grafema) }

        canvas.drawText(grafema.uppercase(Locale.getDefault()), xG, yFonetica, pBody.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        if (tieneError) canvas.drawText("✗ Tensión detectada", xS, yFonetica, pCross) else canvas.drawText("✓ Fluido", xS, yFonetica, pCheck)

        if (isCol2) { yFonetica += 24f }
        index++
    }
    document.finishPage(page)

    // PÁGINA 5: DAF/FAF Y GRÁFICAS DE TENSIÓN
    numeroPagina++
    pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
    page = document.startPage(pageInfo)
    canvas = page.canvas

    canvas.drawText("10. Modulación Acústica y Análisis de Tensión", 40f, 45f, pTitle)
    canvas.drawLine(40f, 55f, 555f, 55f, pLine)

    canvas.drawText("Retroalimentación Auditiva (DAF) y Modulación de Frecuencia (FAF)", 40f, 75f, pTitle)
    val fafText = "Modulación de Frecuencia (FAF): Además del retardo, la literatura señala los beneficios de modificar el tono de la voz en tiempo real (FAF), con desplazamientos que varían desde -1.2 hasta +0.8 octavas."
    dibujarBloqueTexto("", fafText, 85f)

    canvas.drawText("Distribución Histórica de Bloqueos (Tónicos vs Clónicos)", 40f, 220f, pTitle)
    canvas.drawRect(RectF(40f, 235f, 555f, 430f), pBox)

    val pBarTonico = Paint().apply { color = AndroidColor.rgb(211, 47, 47); style = Paint.Style.FILL }
    val pBarClonico = Paint().apply { color = AndroidColor.rgb(194, 24, 91); style = Paint.Style.FILL }

    val chartLeft5 = 75f
    val chartTop5 = 255f
    val chartRight5 = 530f
    val chartBottom5 = 395f
    val chartHeight5 = chartBottom5 - chartTop5
    val chartWidth5 = chartRight5 - chartLeft5

    val maxBloqueos = sesiones.maxOfOrNull { it.bloqueosTonicos + it.bloqueosClonicos }?.coerceAtLeast(4)?.toFloat() ?: 4f

    canvas.drawLine(chartLeft5, chartBottom5, chartRight5, chartBottom5, pGrid)
    canvas.drawLine(chartLeft5, (chartTop5 + chartBottom5) / 2f, chartRight5, (chartTop5 + chartBottom5) / 2f, pGrid)
    canvas.drawLine(chartLeft5, chartTop5, chartRight5, chartTop5, pGrid)
    canvas.drawLine(chartLeft5, chartTop5, chartLeft5, chartBottom5, pGrid)

    canvas.drawText("${maxBloqueos.toInt()}", chartLeft5 - 22f, chartTop5 + 4f, pAxisText)
    canvas.drawText("${(maxBloqueos / 2).toInt()}", chartLeft5 - 22f, (chartTop5 + chartBottom5) / 2f + 4f, pAxisText)
    canvas.drawText("0", chartLeft5 - 18f, chartBottom5 + 4f, pAxisText)

    val sesionesBarras = sesiones.take(15).reversed()
    if (sesionesBarras.isNotEmpty()) {
        val stepX5 = chartWidth5 / sesionesBarras.size
        val barWidth5 = (stepX5 * 0.55f).coerceAtMost(32f)

        sesionesBarras.forEachIndexed { i, sesion ->
            val x = chartLeft5 + (i * stepX5) + (stepX5 - barWidth5) / 2f
            val hTonico = (sesion.bloqueosTonicos / maxBloqueos) * chartHeight5
            val hClonico = (sesion.bloqueosClonicos / maxBloqueos) * chartHeight5

            val topTonico = chartBottom5 - hTonico
            val topTotal = topTonico - hClonico
            canvas.drawRect(x, topTonico, x + barWidth5, chartBottom5, pBarTonico)
            canvas.drawRect(x, topTotal, x + barWidth5, topTonico, pBarClonico)

            val totalB = sesion.bloqueosTonicos + sesion.bloqueosClonicos
            canvas.drawText("$totalB", x + (barWidth5 / 2f) - 4f, (topTotal - 4f).coerceAtLeast(chartTop5 + 8f), pAxisText)
            canvas.drawText(formatearFechaCorta(sesion.fecha), x + (barWidth5 / 2f) - 11f, chartBottom5 + 12f, pAxisText)
        }
    } else {
        canvas.drawText("Sin datos de bloqueos registrados.", chartLeft5 + 15f, chartTop5 + 60f, pBody)
    }

    canvas.drawRect(75f, 445f, 90f, 460f, pBarTonico)
    canvas.drawText("Bloqueos Tónicos", 98f, 457f, pBody)
    canvas.drawRect(225f, 445f, 240f, 460f, pBarClonico)
    canvas.drawText("Bloqueos Clónicos", 248f, 457f, pBody)

    document.finishPage(page)

    // PÁGINA 6: ANÁLISIS AVANZADO (LATENCIA Y RENDIMIENTO POR MÓDULO)
    numeroPagina++
    pageInfo = PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create()
    page = document.startPage(pageInfo)
    canvas = page.canvas

    canvas.drawText("11. Análisis Avanzado de Latencia y Módulos", 40f, 45f, pTitle)
    canvas.drawLine(40f, 55f, 555f, 55f, pLine)

    // Gráfica de Latencia Media
    canvas.drawText("Evolución de Latencia de Fonación (Milisegundos)", 40f, 75f, pTitle)
    canvas.drawRect(RectF(40f, 85f, 555f, 235f), pBox)

    val chartLeft6 = 75f
    val chartTop6 = 100f
    val chartRight6 = 530f
    val chartBottom6 = 205f
    val chartHeight6 = chartBottom6 - chartTop6
    val chartWidth6 = chartRight6 - chartLeft6

    canvas.drawLine(chartLeft6, chartBottom6, chartRight6, chartBottom6, pGrid)
    canvas.drawLine(chartLeft6, (chartTop6 + chartBottom6) / 2f, chartRight6, (chartTop6 + chartBottom6) / 2f, pGrid)
    canvas.drawLine(chartLeft6, chartTop6, chartRight6, chartTop6, pGrid)
    canvas.drawLine(chartLeft6, chartTop6, chartLeft6, chartBottom6, pGrid)

    val sesionesLatRev = sesiones.reversed()
    val maxLatencia = (sesionesLatRev.maxOfOrNull { it.latenciaMediaMs.toFloat() } ?: 2000f).coerceAtLeast(1000f)

    canvas.drawText("${maxLatencia.toInt()}ms", chartLeft6 - 38f, chartTop6 + 4f, pAxisText)
    canvas.drawText("${(maxLatencia / 2).toInt()}ms", chartLeft6 - 38f, (chartTop6 + chartBottom6) / 2f + 4f, pAxisText)
    canvas.drawText("0ms", chartLeft6 - 25f, chartBottom6 + 4f, pAxisText)

    if (sesionesLatRev.isNotEmpty()) {
        val stepX6 = if (sesionesLatRev.size > 1) chartWidth6 / (sesionesLatRev.size - 1) else chartWidth6 / 2f
        val pLineLatencia = Paint().apply { color = AndroidColor.rgb(245, 124, 0); strokeWidth = 3f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        val pPointLatencia = Paint().apply { color = AndroidColor.rgb(230, 81, 0); style = Paint.Style.FILL }
        val pLatLabel = Paint().apply { color = AndroidColor.rgb(230, 81, 0); textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

        var prevX = -1f
        var prevY = -1f
        sesionesLatRev.forEachIndexed { index, sesion ->
            val valor = sesion.latenciaMediaMs.toFloat()
            val x = if (sesionesLatRev.size > 1) chartLeft6 + (index * stepX6) else chartLeft6 + chartWidth6 / 2f
            val y = chartBottom6 - ((valor / maxLatencia) * chartHeight6)

            if (prevX != -1f) canvas.drawLine(prevX, prevY, x, y, pLineLatencia)
            canvas.drawCircle(x, y, 4f, pPointLatencia)

            if (sesionesLatRev.size <= 10 || index % 2 == 0 || index == sesionesLatRev.lastIndex) {
                canvas.drawText("${sesion.latenciaMediaMs}", x - 10f, (y - 6f).coerceAtLeast(chartTop6 + 8f), pLatLabel)
                canvas.drawText(formatearFechaCorta(sesion.fecha), x - 11f, chartBottom6 + 13f, pAxisText)
            }
            prevX = x
            prevY = y
        }
    } else {
        canvas.drawText("No hay registros de latencia disponibles.", chartLeft6 + 15f, chartTop6 + 50f, pBody)
    }
    canvas.drawText("* Valores menores indican mayor rapidez para iniciar la fonación (menos bloqueos iniciales).", 40f, 250f, pBody.apply { color = AndroidColor.GRAY; textSize = 9f })

    // Gráfica Horizontal de Rendimiento por Módulo
    pBody.color = AndroidColor.BLACK; pBody.textSize = 11f
    canvas.drawText("Rendimiento Promedio por Módulo Clínico", 40f, 290f, pTitle)

    val rendimientoModulos = sesiones.groupBy { it.modulo }
        .mapValues { entry -> entry.value.map { it.calificacionGlobal }.average().toInt() }
        .entries.sortedByDescending { it.value }

    var yModulo = 320f
    val pBarModBG = Paint().apply { color = AndroidColor.LTGRAY; style = Paint.Style.FILL }
    val pBarModFG = Paint().apply { color = AndroidColor.rgb(25, 118, 210); style = Paint.Style.FILL }

    for ((modulo, calificacion) in rendimientoModulos) {
        canvas.drawText(modulo.take(30), 40f, yModulo + 10f, pBody)
        canvas.drawText("$calificacion%", 210f, yModulo + 10f, pBody.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        pBody.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        canvas.drawRoundRect(RectF(250f, yModulo, 555f, yModulo + 12f), 4f, 4f, pBarModBG)
        val anchoBarra = 250f + (305f * (calificacion / 100f))
        canvas.drawRoundRect(RectF(250f, yModulo, anchoBarra, yModulo + 12f), 4f, 4f, pBarModFG)

        yModulo += 24f
    }

    document.finishPage(page)

    val fileCache = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "HCE_${perfil.nombre.replace(" ", "_")}.pdf")
    try {
        FileOutputStream(fileCache).use { outputStream -> document.writeTo(outputStream) }
    } catch (e: Exception) { e.printStackTrace() }

    document.close()
    return fileCache
}

@RequiresApi(Build.VERSION_CODES.Q)
fun descargarHcePdfEnDescargas(context: Context, perfil: PerfilClinico, sesiones: List<SesionClinica>, registrosTiempo: Map<String, Int>): Boolean {
    val pdfFile = exportarHcePdf(context, perfil, sesiones, registrosTiempo)
    if (pdfFile == null || !pdfFile.exists()) return false
    return try {
        val fileName = "HCE_${perfil.nombre.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdfFile.inputStream().use { input -> input.copyTo(out) }
            }
            true
        } else false
    } catch (e: Exception) { false }
}

fun compartirArchivoPdf(context: Context, pdfFile: File, viaWhatsApp: Boolean) {
    try {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Historia Clínica Electrónica - SpeakTutor")
            putExtra(Intent.EXTRA_TEXT, "Hola, adjunto mi reporte clínico de evolución y registro de uso de SpeakTutor.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (viaWhatsApp) intent.setPackage("com.whatsapp")
        val chooser = Intent.createChooser(intent, "Enviar reporte PDF vía...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Error al abrir la aplicación para compartir.", Toast.LENGTH_SHORT).show()
    }
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
    } catch (_: Exception) { false }
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
    val analiticas = remember(sesiones) { ProgresoStorage.obtenerAnaliticasClinicas(context) }
    val registrosTiempo = remember(sesiones) { ProgresoStorage.obtenerRegistrosTiempo(context) }
    val archivoFonologico = remember(sesiones) { ProgresoStorage.obtenerArchivoFonologico(context) }

    var desbloqueadasGuardadas by remember { mutableStateOf(ProgresoStorage.obtenerRecompensasDesbloqueadas(context)) }
    var recompensaUrlWeb by remember { mutableStateOf<String?>(null) }
    var alertaSistema by remember { mutableStateOf<Pair<TonoAlerta, String>?>(null) }

    val promedioGlobal = if (sesiones.isNotEmpty()) sesiones.map { it.calificacionGlobal }.average() else 0.0

    fun verificarYDesbloquear(id: String, condicionActual: Boolean): Boolean {
        if (desbloqueadasGuardadas.contains(id)) return true
        if (condicionActual) {
            ProgresoStorage.registrarRecompensaDesbloqueada(context, id)
            desbloqueadasGuardadas = desbloqueadasGuardadas + id
            return true
        }
        return false
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val exito = ProgresoStorage.restaurarRespaldo(context, it)
            if (exito) {
                alertaSistema = Pair(TonoAlerta.EXITO, "Progreso restaurado correctamente. Datos actualizados.")
                perfil = ProgresoStorage.obtenerPerfilClinico(context)
                sesiones = ProgresoStorage.obtenerSesionesClinicas(context)
                desbloqueadasGuardadas = ProgresoStorage.obtenerRecompensasDesbloqueadas(context)
            } else {
                alertaSistema = Pair(TonoAlerta.PELIGRO, "El archivo es inválido o está dañado.")
            }
        }
    }

    if (mostrarDialogoPerfil) {
        DialogoPerfilClinico(
            perfilActual = perfil,
            onDismiss = { if (perfil != null) mostrarDialogoPerfil = false },
            onGuardar = { nombre, correo, diagnostico, objetivos ->
                ProgresoStorage.guardarPerfilClinico(context, nombre, correo, diagnostico, objetivos)
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
                onNavigateProgreso = { /* Ya estás aquí */ },
                onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) },
                onNavigateBio = { navController.navigate(Screen.Biofeedback.route) },
                onNavigateTarjetasArticulacion = { navController.navigate(Screen.TarjetasArticulacion.route) },
                onNavigateConcienciaFonologica = { navController.navigate(Screen.ConcienciaFonologica.route) },
                onNavigateReporteMultidimensional = { navController.navigate(Screen.ReporteMultidimensional.route) }
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
                            }
                        }
                    }
                }

                item { GraficaUsoDiario(registrosTiempo = registrosTiempo) }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF1976D2))
                                Text("${if (sesiones.isNotEmpty()) sesiones.map { it.calificacionGlobal }.average().toInt() else 0}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
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
                    val tonicos = sesiones.sumOf { it.bloqueosTonicos }
                    val clonicos = sesiones.sumOf { it.bloqueosClonicos }
                    GraficaDistribucionBloqueos(tonicos = tonicos, clonicos = clonicos)
                }

                item {
                    GraficaHistorialBloqueos(sesiones = sesiones)
                }

                item {
                    GraficaEvolucionLatencia(sesiones = sesiones)
                }

                item {
                    RendimientoPorModulo(sesiones = sesiones)
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Hearing, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Modulación Acústica (DAF/FAF)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Modulación de Frecuencia (FAF): Además del retardo, la literatura señala los beneficios de modificar el tono de la voz en tiempo real (FAF), con desplazamientos que varían desde -1.2 hasta +0.8 octavas.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                val nombrePaciente = perfil?.nombre ?: "PACIENTE ESTRELLA"
                val nombreUrl = java.net.URLEncoder.encode(nombrePaciente, "UTF-8")
                val tituloOroUrl = java.net.URLEncoder.encode("Insignia de Oro", "UTF-8")
                val tituloPlatinoUrl = java.net.URLEncoder.encode("Insignia Platino", "UTF-8")
                val rachaActual = analiticas.rachaDias.toString()

                val listaRecompensas = listOf(
                    // 1. TICKETS DE RACHA
                    ProgresoStorage.Recompensa3D(
                        "Racha: Primera vez",
                        "Mantén 1 día de práctica.",
                        "file:///android_asset/ticket_racha.html?nombre=$nombreUrl&racha=$rachaActual&tema=fuego",
                        verificarYDesbloquear("racha_1", analiticas.rachaDias >= 1),
                        Icons.Default.LocalFireDepartment
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Racha: Principiante",
                        "Mantén 3 días de práctica continua.",
                        "file:///android_asset/ticket_racha.html?nombre=$nombreUrl&racha=$rachaActual&tema=retro",
                        verificarYDesbloquear("racha_3", analiticas.rachaDias >= 3),
                        Icons.Default.LocalFireDepartment
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Racha: Constante",
                        "Mantén 7 días de práctica continua.",
                        "file:///android_asset/ticket_racha.html?nombre=$nombreUrl&racha=$rachaActual&tema=esmeralda",
                        verificarYDesbloquear("racha_7", analiticas.rachaDias >= 7),
                        Icons.Default.WaterDrop
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Racha: Imparable",
                        "Mantén 14 días de práctica continua.",
                        "file:///android_asset/ticket_racha.html?nombre=$nombreUrl&racha=$rachaActual&tema=oro",
                        verificarYDesbloquear("racha_14", analiticas.rachaDias >= 14),
                        Icons.Default.Whatshot
                    ),

                    // 2. INSIGNIAS DE PRECISIÓN
                    ProgresoStorage.Recompensa3D(
                        "Insignia Oro",
                        "Alcanza 60% de precisión global.",
                        "file:///android_asset/insignia_oro.html?titulo=$tituloOroUrl&tema=oro",
                        verificarYDesbloquear("insignia_oro", promedioGlobal >= 60),
                        Icons.Default.WorkspacePremium
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Insignia Platino",
                        "Alcanza 70% de precisión global.",
                        "file:///android_asset/insignia_oro.html?titulo=$tituloPlatinoUrl&tema=diamante",
                        verificarYDesbloquear("insignia_platino", promedioGlobal >= 70),
                        Icons.Default.Diamond
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Rango Épico",
                        "Alcanza 80% de precisión (Diseño Pixel Art).",
                        "file:///android_asset/insignia_oro.html?titulo=Rango%20Epico&tema=retro",
                        verificarYDesbloquear("rango_epico", promedioGlobal >= 80),
                        Icons.Default.VideogameAsset
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Rango Leyenda",
                        "Precisión del 90% (Diseño Gremio RPG).",
                        "file:///android_asset/insignia_oro.html?titulo=Rango%20Leyenda&tema=rpg",
                        verificarYDesbloquear("rango_leyenda", promedioGlobal >= 90),
                        Icons.Default.Shield
                    ),

                    // 3. GAFETES DE EVENTO
                    ProgresoStorage.Recompensa3D(
                        "Gafete Clasico",
                        "Completa tu primera sesión clínica.",
                        "file:///android_asset/lanyard-badge.html?nombre=$nombreUrl&tema=clasico",
                        verificarYDesbloquear("gafete_1", sesiones.isNotEmpty()),
                        Icons.Default.Badge
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Gafete Oro",
                        "Completa 5 sesiones clínicas.",
                        "file:///android_asset/lanyard-badge.html?nombre=$nombreUrl&tema=oro",
                        verificarYDesbloquear("gafete_5", sesiones.size >= 5),
                        Icons.Default.Badge
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Gafete Zafiro",
                        "Completa 10 sesiones clínicas.",
                        "file:///android_asset/lanyard-badge.html?nombre=$nombreUrl&tema=diamante",
                        verificarYDesbloquear("gafete_10", sesiones.size >= 10),
                        Icons.Default.Badge
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Gafete Esmeralda",
                        "Completa 15 sesiones clínicas.",
                        "file:///android_asset/lanyard-badge.html?nombre=$nombreUrl&tema=esmeralda",
                        verificarYDesbloquear("gafete_15", sesiones.size >= 15),
                        Icons.Default.Badge
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Gafete Rubí",
                        "Completa 20 sesiones clínicas.",
                        "file:///android_asset/lanyard-badge.html?nombre=$nombreUrl&tema=rpg",
                        verificarYDesbloquear("gafete_20", sesiones.size >= 20),
                        Icons.Default.Badge
                    ),

                    // 4. TICKETS VIP WEBGL
                    ProgresoStorage.Recompensa3D(
                        "VIP: Neón Clásico",
                        "Logra 0 bloqueos tónicos en tu última sesión.",
                        "file:///android_asset/ticket/index.html?nombre=$nombreUrl&evento=SESION%20PERFECTA&tema=vip",
                        verificarYDesbloquear("vip_neon", sesiones.isNotEmpty() && analiticas.promedioTonicos == 0),
                        Icons.Default.ConfirmationNumber
                    ),
                    ProgresoStorage.Recompensa3D(
                        "VIP: Llamas",
                        "Supera el 70% de contactos ligeros.",
                        "file:///android_asset/ticket/index.html?nombre=$nombreUrl&evento=FLUIDEZ%20TOTAL&tema=fuego",
                        verificarYDesbloquear("vip_llamas", analiticas.promedioContactosLigeros >= 70),
                        Icons.Default.LocalActivity
                    ),
                    ProgresoStorage.Recompensa3D(
                        "VIP: Ciberpunk",
                        "Mantén un 80% de precisión general.",
                        "file:///android_asset/ticket/index.html?nombre=$nombreUrl&evento=CIBER%20TERAPIA&tema=diamante",
                        verificarYDesbloquear("vip_ciberpunk", promedioGlobal >= 80),
                        Icons.Default.Memory
                    ),
                    ProgresoStorage.Recompensa3D(
                        "VIP: Retro 8-bit",
                        "Consigue una racha de 10 días seguidos.",
                        "file:///android_asset/ticket/index.html?nombre=$nombreUrl&evento=ARCADE%20MASTER&tema=retro",
                        verificarYDesbloquear("vip_retro", analiticas.rachaDias >= 10),
                        Icons.Default.Gamepad
                    ),
                    ProgresoStorage.Recompensa3D(
                        "VIP: Oro Imperial",
                        "Logra al menos 70% en contactos ligeros.",
                        "file:///android_asset/ticket/index.html?nombre=$nombreUrl&evento=LOGRO%20IMPERIAL&tema=oro",
                        verificarYDesbloquear("vip_oro", analiticas.promedioContactosLigeros >= 70),
                        Icons.Default.Stars
                    ),

                    // 5. PLACAS GRABADAS 3D
                    ProgresoStorage.Recompensa3D(
                        "Placa: Papel Vintage",
                        "Tómate un respiro. (Recompensa de bienvenida).",
                        "file:///android_asset/card_3d.html?nombre=$nombreUrl&palabra=INICIO&tema=paper",
                        verificarYDesbloquear("placa_paper", sesiones.isNotEmpty()),
                        Icons.Default.MenuBook
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Placa: Carmesí Oscuro",
                        "Mantén un promedio menor a 2 bloqueos tónicos.",
                        "file:///android_asset/card_3d.html?nombre=$nombreUrl&palabra=ENFOQUE&tema=crimson",
                        verificarYDesbloquear("placa_crimson", sesiones.isNotEmpty() && analiticas.promedioTonicos <= 2),
                        Icons.Default.FilterVintage
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Placa: Oro Puro",
                        "Alcanza 5 días de racha.",
                        "file:///android_asset/card_3d.html?nombre=$nombreUrl&palabra=EXCELENTE&tema=oro",
                        verificarYDesbloquear("placa_oro", analiticas.rachaDias >= 5),
                        Icons.Default.CardMembership
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Placa: Diamante Azul",
                        "Completa 20 sesiones clínicas.",
                        "file:///android_asset/card_3d.html?nombre=$nombreUrl&palabra=CONSTANCIA&tema=diamante",
                        verificarYDesbloquear("placa_diamante", sesiones.size >= 20),
                        Icons.Default.Spa
                    ),
                    ProgresoStorage.Recompensa3D(
                        "Placa: Matriz Retro",
                        "Desbloqueo final: Precisión del 85% global.",
                        "file:///android_asset/card_3d.html?nombre=$nombreUrl&palabra=MAESTRO&tema=retro",
                        verificarYDesbloquear("placa_retro", promedioGlobal >= 85),
                        Icons.Default.Code
                    )
                )

                item {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Tus Logros y Recompensas 3D", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Toca un premio desbloqueado para interactuar en 3D.", fontSize = 13.sp, color = Color.Gray)

                        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(listaRecompensas) { premio ->
                                Card(
                                    modifier = Modifier.width(160.dp).height(120.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = premio.desbloqueada) { recompensaUrlWeb = premio.htmlFile },
                                    colors = CardDefaults.cardColors(containerColor = if (premio.desbloqueada) Color(0xFFFFF3E0) else Color(0xFFF5F5F5))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(imageVector = if (premio.desbloqueada) premio.icono else Icons.Default.Lock, contentDescription = null, tint = if (premio.desbloqueada) Color(0xFFE65100) else Color.LightGray, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(premio.titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (premio.desbloqueada) Color.Black else Color.Gray)
                                        Text(premio.descripcion, fontSize = 10.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }

                if (recompensaUrlWeb != null) {
                    item {
                        Dialog(onDismissRequest = { recompensaUrlWeb = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.allowFileAccess = true
                                            settings.allowFileAccessFromFileURLs = true
                                            settings.allowUniversalAccessFromFileURLs = true
                                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                            addJavascriptInterface(ProgresoStorage.WebAppInterface { post { recompensaUrlWeb = null } }, "Android")
                                            webViewClient = WebViewClient()
                                            loadUrl(recompensaUrlWeb!!)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        alertaSistema?.let { (tono, mensaje) ->
                            AlertaInPlace(tono = tono, titulo = if (tono == TonoAlerta.EXITO) "Operación Exitosa" else "Atención", mensaje = mensaje, onClose = { alertaSistema = null })
                        }
                        if (analiticas.rachaDias >= 3) {
                            AlertaInPlace(tono = TonoAlerta.EXITO, titulo = "¡Racha de ${analiticas.rachaDias} días consecutivos!", mensaje = "Tu constancia está fortaleciendo tus rutas neuronales y memoria muscular fonatoria.")
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
                            Text(text = "Gestión de Datos y Respaldo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                            Button(
                                onClick = {
                                    perfil?.let { p ->
                                        if (descargarHcePdfEnDescargas(context, p, sesiones, registrosTiempo)) alertaSistema = Pair(TonoAlerta.EXITO, "Historia Clínica guardada en Descargas.")
                                        else alertaSistema = Pair(TonoAlerta.PELIGRO, "Error al guardar el PDF.")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Default.Download, null); Spacer(modifier = Modifier.width(8.dp))
                                Text("Descargar Historia Clínica (PDF)")
                            }

                            Button(
                                onClick = {
                                    perfil?.let { p ->
                                        exportarHcePdf(context, p, sesiones, registrosTiempo)?.let { compartirArchivoPdf(context, it, false) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                            ) {
                                Icon(Icons.Default.Share, null); Spacer(modifier = Modifier.width(8.dp))
                                Text("Enviar por Gmail / Menú General")
                            }

                            Button(
                                onClick = {
                                    perfil?.let { p ->
                                        exportarHcePdf(context, p, sesiones, registrosTiempo)?.let { compartirArchivoPdf(context, it, true) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Icon(Icons.Default.Chat, null); Spacer(modifier = Modifier.width(8.dp))
                                Text("Enviar por WhatsApp")
                            }

                            Button(
                                onClick = {
                                    if (ProgresoStorage.crearRespaldoDatos(context)) alertaSistema = Pair(TonoAlerta.EXITO, "Respaldo (.stb) guardado en Descargas.")
                                    else alertaSistema = Pair(TonoAlerta.PELIGRO, "Error al crear respaldo.")
                                },
                                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                            ) {
                                Icon(Icons.Default.Backup, null); Spacer(modifier = Modifier.width(8.dp))
                                Text("Crear Archivo de Respaldo (.stb)")
                            }

                            OutlinedButton(onClick = { restoreLauncher.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Restore, null); Spacer(modifier = Modifier.width(8.dp))
                                Text("Restaurar Progreso desde Archivo")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (exportarDatosParaML(context)) alertaSistema = Pair(TonoAlerta.EXITO, "Dataset CSV exportado con éxito.")
                                    else alertaSistema = Pair(TonoAlerta.PELIGRO, "No hay suficientes datos para exportar.")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Storage, null); Spacer(modifier = Modifier.width(8.dp))
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