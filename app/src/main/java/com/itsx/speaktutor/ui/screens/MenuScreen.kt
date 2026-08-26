package com.itsx.speaktutor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import com.itsx.speaktutor.ui.navigation.Screen

import androidx.compose.ui.tooling.preview.Preview
import com.itsx.speaktutor.ui.theme.SPEAKTUTORTheme

@Composable
fun MenuScreen(navController: NavController) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Asistente de Fluidez",
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            BotonPrincipal("Módulo de Metrónomo") {
                navController.navigate(Screen.Metronomo.route)
            }
            Spacer(modifier = Modifier.height(16.dp))

            BotonPrincipal("Habla Estirada (Fonemas)") {
                navController.navigate(Screen.HablaEstirada.route)
            }
            Spacer(modifier = Modifier.height(16.dp))

            BotonPrincipal("Entrenamiento de Ritmo") {
                navController.navigate(Screen.RitmoFluidez.route)
            }
            Spacer(modifier = Modifier.height(16.dp))

            BotonPrincipal("Simulación de situaciones") {
                navController.navigate(Screen.SimulacionSituaciones.route)
            }
        }
    }
}

@Composable
fun BotonPrincipal(texto: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp), // Botón grande para mayor accesibilidad
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text = texto, fontSize = 18.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun MenuPreview() {
    SPEAKTUTORTheme {
        // Puedes pasarle un NavController simulado o crear un objeto temporal para ver el diseño
        // MenuScreen(navController = rememberNavController())
    }
}