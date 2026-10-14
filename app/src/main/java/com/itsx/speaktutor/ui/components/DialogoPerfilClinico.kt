package com.itsx.speaktutor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itsx.speaktutor.ui.screens.PerfilClinico

@Composable
fun DialogoPerfilClinico(
    perfilActual: PerfilClinico?,
    onDismiss: () -> Unit,
    onGuardar: (String, String, String, String) -> Unit
) {
    // Al inicializar, cargamos los datos del perfil actual si existen.
    // Si es nuevo (null), los dejamos en blanco.
    var nombre by remember { mutableStateOf(perfilActual?.nombre ?: "") }
    var correo by remember { mutableStateOf(perfilActual?.correo ?: "") }
    var diagnostico by remember { mutableStateOf(perfilActual?.diagnosticoPrincipal ?: "") }
    var objetivos by remember { mutableStateOf(perfilActual?.objetivosSmart ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (perfilActual == null) "Crear Perfil Clínico" else "Editar Perfil Clínico")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del Paciente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo Electrónico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = diagnostico,
                    onValueChange = { diagnostico = it },
                    label = { Text("Diagnóstico Principal") },
                    minLines = 2, // Le da espacio de 2 renglones por defecto
                    maxLines = 4, // Crece hasta 4 renglones si escribes mucho
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = objetivos,
                    onValueChange = { objetivos = it },
                    label = { Text("Metas y Objetivos SMART") },
                    minLines = 3, // Inicia con 3 renglones de alto
                    maxLines = 6, // Permite explayarse hasta 6 renglones
                    placeholder = { Text("1. Reducir latencia...\n2. Mejorar contacto...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(nombre, correo, diagnostico, objetivos) },
                enabled = nombre.isNotBlank() && correo.isNotBlank()
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}