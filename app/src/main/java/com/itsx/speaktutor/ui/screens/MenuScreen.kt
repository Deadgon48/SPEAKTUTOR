package com.itsx.speaktutor.ui.screens // Asegúrate de que coincida con tu paquete real

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

@Composable
fun MenuScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current

    // 1. Verificación de Perfil Inicial
    var perfil by remember { mutableStateOf(ProgresoStorage.obtenerPerfil(context)) }
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

    // 4. Diálogo de Registro Inicial (Nombre y Correo)
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

    // Aquí continúa el diseño normal de tu menú principal (botones hacia los módulos, etc.)
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
            // ... Tus botones de navegación existentes ...
        }
    }
}