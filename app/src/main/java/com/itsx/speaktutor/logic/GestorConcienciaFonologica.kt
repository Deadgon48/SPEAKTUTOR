package com.itsx.speaktutor.logic

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GestorConcienciaFonologica {
    private const val PREF_NAME = "SpeakTutorFonologia"
    private const val KEY_GRAFEMAS = "grafemas_pendientes"

    // Ahora recibe también lo que el STT alcanzó a escuchar
    fun guardarGrafemaProblematico(context: Context, textoObjetivo: String, textoEscuchadoParcial: String = "") {
        if (textoObjetivo.isBlank()) return

        // 1. EXTRAER SÓLO LA PALABRA EXACTA DEL BLOQUEO (No frases)
        val palabraAislada = aislarPalabraExacta(textoObjetivo, textoEscuchadoParcial)
        if (palabraAislada.isBlank()) return

        // 2. Extraer la primera sílaba de esa única palabra
        val silaba = extraerSilabaInicial(palabraAislada)
        val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val actuales = prefs.getStringSet(KEY_GRAFEMAS, mutableSetOf()) ?: mutableSetOf()

        // Formato: "silaba|palabra|fecha"
        actuales.add("$silaba|$palabraAislada|$fechaActual")
        prefs.edit().putStringSet(KEY_GRAFEMAS, actuales).apply()
    }

    private fun aislarPalabraExacta(objetivo: String, escuchado: String): String {
        // Limpiar puntuación y dividir en palabras sueltas
        val regex = Regex("[^a-záéíóúñ ]")
        val palabrasObjetivo = objetivo.lowercase().replace(regex, "").split("\\s+".toRegex()).filter { it.isNotBlank() }

        if (palabrasObjetivo.isEmpty()) return ""
        if (palabrasObjetivo.size == 1) return palabrasObjetivo[0] // Si ya es una sola palabra, se devuelve

        // Si es una frase, comparamos con lo que el STT alcanzó a escuchar
        val palabrasEscuchadas = escuchado.lowercase().replace(regex, "").split("\\s+".toRegex()).filter { it.isNotBlank() }

        // Si el objetivo tiene 6 palabras y el usuario dijo 2, significa que se bloqueó en la 3ra palabra.
        if (palabrasEscuchadas.isNotEmpty() && palabrasEscuchadas.size < palabrasObjetivo.size) {
            return palabrasObjetivo[palabrasEscuchadas.size]
        }

        // Plan B: Si no hay STT o no concuerda, tomamos la palabra más larga de la oración (suele ser la trabada)
        return palabrasObjetivo.maxByOrNull { it.length } ?: palabrasObjetivo.last()
    }

    fun obtenerTarjetasPendientes(context: Context): List<Pair<String, String>> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val datos = prefs.getStringSet(KEY_GRAFEMAS, emptySet()) ?: emptySet()

        return datos.mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 2) Pair(parts[0], parts[1]) else null
        }
    }

    fun marcarComoSuperado(context: Context, silaba: String, palabra: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val actuales = prefs.getStringSet(KEY_GRAFEMAS, mutableSetOf()) ?: mutableSetOf()

        val elementoAEliminar = actuales.find { it.startsWith("$silaba|$palabra") }
        if (elementoAEliminar != null) {
            actuales.remove(elementoAEliminar)
            prefs.edit().putStringSet(KEY_GRAFEMAS, actuales).apply()
        }
    }

    private fun extraerSilabaInicial(palabra: String): String {
        val vocales = listOf('a', 'e', 'i', 'o', 'u', 'á', 'é', 'í', 'ó', 'ú')
        var indiceCorte = 2
        for (i in palabra.indices) {
            if (vocales.contains(palabra[i].lowercaseChar())) {
                indiceCorte = i + 1
                if (i + 1 < palabra.length && !vocales.contains(palabra[i+1].lowercaseChar()) &&
                    (i + 2 >= palabra.length || !vocales.contains(palabra[i+2].lowercaseChar()))) {
                    indiceCorte = i + 2
                }
                break
            }
        }
        return palabra.substring(0, minOf(indiceCorte, palabra.length)).lowercase()
    }
}