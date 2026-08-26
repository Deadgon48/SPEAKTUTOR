package com.itsx.speaktutor.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.MetronomoReutilizableComponent
import com.itsx.speaktutor.ui.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun MetronomoScreen(navController: NavController, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Barra superior de navegación rápida entre módulos
        BarraNavegacionModulos(
            onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
            onNavigateMetronomo = { /* Ya estás aquí */ },
            onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
            onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Contenedor principal con diseño de tarjeta degradada moderna para el metrónomo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF3949AB))))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Módulo de Metrónomo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Componente funcional del metrónomo
                MetronomoReutilizableComponent()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Botón de retroceso estilizado en tarjeta/botón contenedor
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(text = "Regresar al Menú Principal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}