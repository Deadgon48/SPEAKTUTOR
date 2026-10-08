package com.itsx.speaktutor.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Creamos un único esquema de color oscuro basado en Pizarra
private val PizarraColorScheme = darkColorScheme(
    primary = AzulPrincipal,
    secondary = AzulSecundario,
    tertiary = AzulClaro,

    // Aquí pintamos la "Pizarra" en el fondo de toda la app
    background = PizarraFondo,
    surface = PizarraTarjeta,

    // Colores de los textos para que contrasten bien
    onPrimary = Color.White,
    onBackground = TizaTexto,
    onSurface = TizaTexto
)

@Composable
fun SPEAKTUTORTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // APAGADO para evitar que Android 12+ cambie tus colores
    content: @Composable () -> Unit
) {
    // Forzamos la paleta de Pizarra sin importar si el celular está en modo claro u oscuro
    val colorScheme = PizarraColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}