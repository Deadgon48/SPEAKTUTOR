package com.itsx.speaktutor.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class ModoTema(val etiqueta: String) {
    SISTEMA("Sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro")
}

object TemaStorage {
    private const val PREFS_NAME = "speaktutor_tema_prefs"
    private const val KEY_MODO_TEMA = "modo_tema"

    var modoActual by mutableStateOf(ModoTema.SISTEMA)
        private set

    fun inicializar(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val guardado = prefs.getString(KEY_MODO_TEMA, ModoTema.SISTEMA.name)
        modoActual = runCatching { ModoTema.valueOf(guardado ?: ModoTema.SISTEMA.name) }
            .getOrDefault(ModoTema.SISTEMA)
    }

    fun cambiarModo(context: Context, nuevoModo: ModoTema) {
        modoActual = nuevoModo
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODO_TEMA, nuevoModo.name)
            .apply()
    }
}

private val PizarraDarkColorScheme = darkColorScheme(
    primary = AzulPrincipal,
    secondary = AzulSecundario,
    tertiary = AzulClaro,
    background = PizarraFondo,
    surface = PizarraTarjeta,
    surfaceVariant = Color(0xFF455A64),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TizaTexto,
    onSurface = TizaTexto,
    onSurfaceVariant = Color(0xFFCFD8DC)
)

private val SpeakTutorLightColorScheme = lightColorScheme(
    primary = AzulPrincipal,
    secondary = AzulSecundario,
    tertiary = AzulClaro,
    background = ClaroFondo,
    surface = ClaroTarjeta,
    surfaceVariant = Color(0xFFE3ECF2),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = ClaroTexto,
    onSurface = ClaroTexto,
    onSurfaceVariant = Color(0xFF455A64)
)

@Composable
fun SPEAKTUTORTheme(
    modoTema: ModoTema = TemaStorage.modoActual,
    content: @Composable () -> Unit
) {
    val usarOscuro = when (modoTema) {
        ModoTema.SISTEMA -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }

    val colorScheme = if (usarOscuro) PizarraDarkColorScheme else SpeakTutorLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}