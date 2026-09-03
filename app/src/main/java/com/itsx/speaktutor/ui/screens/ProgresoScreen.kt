package com.itsx.speaktutor.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

// Estructura de datos de la sesión de práctica
data class SesionProgreso(
    val id: String,
    val fecha: String,
    val dificultad: String,
    val aciertos: Int,
    val errores: Int,
    val calificacion: Int
)

// Utilidad simple para guardar y cargar sesiones localmente con SharedPreferences
object ProgresoStorage {
    private const val PREF_NAME = "SpeakTutorProgresoPrefs"
    private const val KEY_SESIONES = "lista_sesiones"



    fun guardarSesion(context: Context, dificultad: String, aciertos: Int, errores: Int, calificacion: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val sesionesActuales = obtenerSesiones(context).toMutableList()

        val fechaActual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val nuevaSesion = SesionProgreso(
            id = UUID.randomUUID().toString(),
            fecha = fechaActual,
            dificultad = dificultad,
            aciertos = aciertos,
            errores = errores,
            calificacion = calificacion
        )

        sesionesActuales.add(0, nuevaSesion) // Añadir al inicio para ver las más recientes primero

        val sb = StringBuilder()
        for (s in sesionesActuales) {
            sb.append("${s.id}|${s.fecha}|${s.dificultad}|${s.aciertos}|${s.errores}|${s.calificacion}#")
        }
        prefs.edit().putString(KEY_SESIONES, sb.toString()).apply()
    }



    fun obtenerSesiones(context: Context): List<SesionProgreso> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY_SESIONES, "") ?: ""
        if (data.isBlank()) return emptyList()

        val lista = mutableListOf<SesionProgreso>()
        val items = data.split("#")
        for (item in items) {
            if (item.isBlank()) continue
            val parts = item.split("|")
            if (parts.size == 6) {
                lista.add(
                    SesionProgreso(
                        id = parts[0],
                        fecha = parts[1],
                        dificultad = parts[2],
                        aciertos = parts[3].toIntOrNull() ?: 0,
                        errores = parts[4].toIntOrNull() ?: 0,
                        calificacion = parts[5].toIntOrNull() ?: 0
                    )
                )
            }
        }
        return lista
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var historialSesiones by remember { mutableStateOf(ProgresoStorage.obtenerSesiones(context)) }

    val totalPracticas = historialSesiones.size
    val promedioCalificacion = if (totalPracticas > 0) {
        historialSesiones.sumOf { it.calificacion } / totalPracticas
    } else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguimiento de Progreso") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Barra de navegación rápida superior entre módulos
            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { /* Ya estás aquí */ },
                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                onNavigateProgreso = { navController.navigate(Screen.Progreso.route) }
            )
            // Tarjeta de Resumen General
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "Rendimiento General", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$totalPracticas", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Sesiones", fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$promedioCalificacion pts",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = "Promedio", fontSize = 12.sp)
                        }
                    }
                }
            }

            Text(text = "Historial de Prácticas", fontSize = 16.sp, fontWeight = FontWeight.Bold)

            if (historialSesiones.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aún no hay registros. ¡Completa una sesión de pronunciación!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Lista de Historial
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historialSesiones) { sesion ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "Nivel: ${sesion.dificultad}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "Fecha: ${sesion.fecha}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "✅ ${sesion.aciertos} aciertos | ❌ ${sesion.errores} errores", fontSize = 13.sp)
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "${sesion.calificacion}%",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}