package com.itsx.speaktutor.logic

import android.content.Context
import com.itsx.speaktutor.ui.screens.ProgresoStorage
import java.util.UUID

enum class CategoriaAdaptativa {
    BLOQUES_RITMO, LECTURA_GUIADA, HABLA_ESTIRADA, SIMULACION
}

data class EjercicioGenerado(
    val id: String = UUID.randomUUID().toString(),
    val categoria: CategoriaAdaptativa,
    val titulo: String,
    val bpmSugerido: Int,
    val textoPractica: String,
    val instruccionEspecial: String,
    val audioInterlocutor: String? = null // 👈 Nuevo campo para que la app "hable" en simulaciones
)

object MotorAdaptativo {

    fun generarConsejoContextual(context: Context): String {
        val sesiones = ProgresoStorage.obtenerSesiones(context)
        if (sesiones.isEmpty()) return "👋 ¡Bienvenido al Laboratorio IA! Completa ejercicios para que pueda analizar tu fluidez."

        val promedio = sesiones.map { it.calificacion }.average()
        val tendenciaMejora = if (sesiones.size > 1) sesiones[0].calificacion >= sesiones[1].calificacion else true

        return when {
            promedio >= 85 -> "🌟 Tu fluidez es sólida. El algoritmo generará escenarios de alta exigencia comunicativa. Mantén la fonación continua."
            promedio in 65.0..84.9 && tendenciaMejora -> "📈 Tienes buen ritmo. Si sientes un bloqueo inminente, aplica la técnica 'Pull-out': congela la tensión y deslízate suavemente."
            promedio in 65.0..84.9 && !tendenciaMejora -> "⚖️ Hay fluctuaciones en tu fluidez reciente. Hoy priorizaremos ejercicios con metrónomo lento para reestructurar tu respiración."
            else -> "💡 Estamos detectando alta tensión. Relaja los articuladores. Usaremos Inicios Suaves (Easy Onsets) y textos fraccionados."
        }
    }

    // GENERADOR AUTÓNOMO INFINITO
    fun generarEjercicioNuevo(context: Context): EjercicioGenerado {
        val sesiones = ProgresoStorage.obtenerSesiones(context)
        val calificacionPromedio = if (sesiones.isNotEmpty()) sesiones.map { it.calificacion }.average() else 100.0

        val categoria = CategoriaAdaptativa.values().random()

        return when (categoria) {
            CategoriaAdaptativa.BLOQUES_RITMO -> generarBloques(calificacionPromedio)
            CategoriaAdaptativa.LECTURA_GUIADA -> generarLectura(calificacionPromedio)
            CategoriaAdaptativa.HABLA_ESTIRADA -> generarHablaEstirada(calificacionPromedio)
            CategoriaAdaptativa.SIMULACION -> generarSimulacion(calificacionPromedio)
        }
    }

    private fun generarBloques(promedio: Double): EjercicioGenerado {
        val sujetos = listOf("El piloto", "La doctora", "Un estudiante", "El ingeniero")
        val verbos = listOf("na / ve / ga / ba", "es / cu / cha / ba", "pro / gra / ma / ba", "cai / mi / na / ba")
        val complementos = listOf("por / la / ciu / dad", "en / el / ser / vi / dor", "con / su / fa / mi / lia")

        val bpm = if (promedio > 80) 90 else 60
        val frase = "${sujetos.random()} / ${verbos.random()} / ${complementos.random()}"

        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.BLOQUES_RITMO,
            titulo = "Bloques de Ritmo Dinámicos",
            bpmSugerido = bpm,
            textoPractica = frase,
            instruccionEspecial = "Sincroniza cada bloque con un golpe del metrónomo."
        )
    }

    private fun generarLectura(promedio: Double): EjercicioGenerado {
        val inicios = listOf("En el mundo de la tecnología actual,", "Durante el diseño de la arquitectura,", "A pesar de las caídas de red,")
        val desarrollos = listOf("los contenedores permiten un despliegue rápido", "la nube ofrece escalabilidad infinita", "el equipo logró levantar los servicios")
        val finales = listOf("y los usuarios navegaron sin problemas.", "demostrando una gran eficiencia operativa.", "marcando el éxito del proyecto.")

        val texto = "${inicios.random()} ${desarrollos.random()} ${finales.random()}"

        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.LECTURA_GUIADA,
            titulo = "Lectura Inédita",
            bpmSugerido = 0,
            textoPractica = texto,
            instruccionEspecial = "Lee el párrafo completo manteniendo el flujo de aire constante."
        )
    }

    private fun generarHablaEstirada(promedio: Double): EjercicioGenerado {
        val palabrasEstirables = listOf("Sssservidor", "Mmmmemoria", "Ssssistema", "Vvvvolumen", "Nnnnúcleo")
        val bpm = if (promedio > 80) 70 else 50

        val frase = "${palabrasEstirables.random()} es la clave del ${palabrasEstirables.random().lowercase()}"

        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.HABLA_ESTIRADA,
            titulo = "Control de Inicios (Easy Onsets)",
            bpmSugerido = bpm,
            textoPractica = frase,
            instruccionEspecial = "Estira el primer fonema de las palabras marcadas durante 2 segundos."
        )
    }

    // 🎯 SIMULACIÓN PROCEDIMENTAL INTERACTIVA
    private fun generarSimulacion(promedio: Double): EjercicioGenerado {
        // Matriz de escenarios: Contexto, Texto del Interlocutor, Respuesta Objetivo del Usuario
        val escenarios = listOf(
            Triple("Taquilla de Autobuses", "Buenas tardes, ¿cuál es su destino?", "Buenas tardes, quiero un boleto de autobús para Xalapa."),
            Triple("Recepción de Hotel", "Bienvenido, ¿tiene alguna reservación?", "Sí, tengo una reserva a nombre de Humberto Acosta."),
            Triple("Entrevista Técnica", "Hábleme de su experiencia con infraestructura de redes.", "Tengo experiencia configurando Ubuntu Server y BIND9."),
            Triple("Tienda de Cómputo", "Hola, ¿busca algún equipo o componente en especial?", "Sí, busco un adaptador de red y memoria RAM."),
            Triple("Cafetería Local", "Hola, buenos días. ¿Qué le preparo el día de hoy?", "Hola, me da un café americano grande, por favor.")
        )
        val escenarioSeleccionado = escenarios.random()

        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.SIMULACION,
            titulo = "Simulación: ${escenarioSeleccionado.first}",
            bpmSugerido = 0,
            textoPractica = escenarioSeleccionado.third, // Lo que el algoritmo evaluará
            instruccionEspecial = "Escucha la situación planteada y responde de forma natural grabando tu voz.",
            audioInterlocutor = escenarioSeleccionado.second // Lo que la app dirá en voz alta
        )
    }
}