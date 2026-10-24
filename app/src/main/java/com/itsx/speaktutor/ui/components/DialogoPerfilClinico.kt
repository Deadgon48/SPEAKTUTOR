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
    onGuardar: (String, String, String, String, String, String) -> Unit
) {
    var nombre by remember { mutableStateOf(perfilActual?.nombre ?: "") }
    var correo by remember { mutableStateOf(perfilActual?.correo ?: "") }
    var diagnostico by remember { mutableStateOf(perfilActual?.diagnosticoPrincipal ?: "") }
    var objetivos by remember { mutableStateOf(perfilActual?.objetivosSmart ?: "") }
    var observaciones by remember { mutableStateOf(perfilActual?.observaciones ?: "") }
    var recomendaciones by remember { mutableStateOf(perfilActual?.recomendaciones ?: "") }

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
                    label = { Text("Diagnóstico Principal (Ej. TDAH, Disfemia)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = objetivos,
                    onValueChange = { objetivos = it },
                    label = { Text("Metas y Objetivos SMART") },
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text("1. Reducir latencia...\n2. Mejorar contacto...") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    label = { Text("Observaciones Cualitativas") },
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text("Motivación, fatiga, colaboración...") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = recomendaciones,
                    onValueChange = { recomendaciones = it },
                    label = { Text("Recomendaciones Casa/Escuela") },
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text("Tareas complementarias, uso de temporizadores...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(nombre, correo, diagnostico, objetivos, observaciones, recomendaciones) },
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