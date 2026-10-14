package com.itsx.speaktutor.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.itsx.speaktutor.ui.components.DialogoPerfilClinico

@Composable
fun MenuScreen(navController: NavController) {
    val context = LocalContext.current

    // 1. Verificación de Perfil Clínico Inicial (Actualizado a HCE)
    var perfil by remember { mutableStateOf(ProgresoStorage.obtenerPerfilClinico(context)) }
    var mostrarDialogoRegistro by remember { mutableStateOf(perfil == null) }

    // 2. Lanzador para solicitar permisos de micrófono en caliente
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permiso de micrófono concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Se requiere el permiso de micrófono para las funciones de voz y DSP", Toast.LENGTH_LONG).show()
        }
    }

    // 3. Comprobar permisos al abrir la app por primera vez
    LaunchedEffect(Unit) {
        val checkMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (checkMic != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // 4. Diálogo de Registro Inicial usando el Componente HCE modular
    if (mostrarDialogoRegistro) {
        DialogoPerfilClinico(
            perfilActual = perfil, // Al ser null, el componente bloqueará el cierre obligando el registro
            onDismiss = { mostrarDialogoRegistro = false },
            onGuardar = { nombre, correo, diagnostico, objetivos ->
                ProgresoStorage.guardarPerfilClinico(context, nombre, correo, diagnostico, objetivos)
                perfil = ProgresoStorage.obtenerPerfilClinico(context)
                mostrarDialogoRegistro = false
                Toast.makeText(context, "¡Expediente configurado con éxito!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Aquí continúa el diseño normal de tu menú principal
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "SpeakTutor - Menú Principal", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            // ... Tus botones de navegación a los módulos (Ritmo, Pronunciación, Biofeedback, etc.) ...
        }
    }
}