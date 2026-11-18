package com.itsx.speaktutor.logic

import android.content.Context
import com.itsx.speaktutor.ui.screens.ProgresoStorage
import java.util.UUID

enum class CategoriaAdaptativa {
    BLOQUES_RITMO,
    LECTURA_GUIADA,
    HABLA_ESTIRADA,
    ESCENARIO_RAPIDO,
    GUION_ESTRESANTE,
    METRONOMO_PURO,
    BIOFEEDBACK_RELAJACION,
    EVOCACION_SEMANTICA,
    PRONUNCIACION_COMPLEJA
}

data class EjercicioGenerado(
    val id: String = UUID.randomUUID().toString(),
    val categoria: CategoriaAdaptativa,
    val titulo: String,
    val bpmSugerido: Int,
    val textoPractica: String,
    val instruccionEspecial: String,
    val audioInterlocutor: String? = null
)

object MotorAdaptativo {

    fun generarConsejoContextual(context: Context): String {
        val sesiones = ProgresoStorage.obtenerSesionesClinicas(context)
        if (sesiones.isEmpty()) return "👋 ¡Bienvenido al Laboratorio IA! Completa ejercicios para que la red neuronal perfile tu fluidez."

        val recientes = sesiones.take(5)
        val fluidez = recientes.map { it.calificacionGlobal }.average()
        val tonicos = recientes.map { it.bloqueosTonicos }.average()
        val clonicos = recientes.map { it.bloqueosClonicos }.average()
        val latencia = recientes.map { it.latenciaMediaMs }.average()

        return when {
            tonicos > 5 -> "🚨 Alta tensión vocal detectada. Priorizaremos la relajación diafragmática y los inicios suaves (contacto ligero)."
            clonicos > 5 -> "🧱 Tienes repeticiones silábicas. El metrónomo y los bloques de ritmo te ayudarán a encarrilar la fonación."
            latencia > 2500 -> "⏱️ Tu latencia de respuesta es elevada. Entrenaremos con evocación rápida y escenarios sociales dinámicos."
            fluidez >= 85 -> "🌟 Nivel de fluidez óptimo. La IA te retará hoy con pronunciación compleja y situaciones estresantes."
            else -> "⚖️ Rendimiento equilibrado. Mantendremos un entrenamiento cruzado para fortalecer todas las áreas."
        }
    }

    fun generarEjercicioNuevo(context: Context, categoriaEvitada: CategoriaAdaptativa? = null): EjercicioGenerado {
        val sesiones = ProgresoStorage.obtenerSesionesClinicas(context)
        val pesos = mutableMapOf<CategoriaAdaptativa, Double>()
        CategoriaAdaptativa.entries.forEach { pesos[it] = Math.random() * 20.0 }

        if (sesiones.isNotEmpty()) {
            val cortoPlazo = sesiones.take(3)
            val tonicosCP = cortoPlazo.map { it.bloqueosTonicos }.average()
            val clonicosCP = cortoPlazo.map { it.bloqueosClonicos }.average()
            val latenciaCP = cortoPlazo.map { it.latenciaMediaMs }.average()
            val fluidezCP = cortoPlazo.map { it.calificacionGlobal }.average()

            if (tonicosCP > 3) {
                pesos[CategoriaAdaptativa.BIOFEEDBACK_RELAJACION] = (pesos[CategoriaAdaptativa.BIOFEEDBACK_RELAJACION] ?: 0.0) + 50.0
                pesos[CategoriaAdaptativa.HABLA_ESTIRADA] = (pesos[CategoriaAdaptativa.HABLA_ESTIRADA] ?: 0.0) + 40.0
            }
            if (clonicosCP > 3) {
                pesos[CategoriaAdaptativa.METRONOMO_PURO] = (pesos[CategoriaAdaptativa.METRONOMO_PURO] ?: 0.0) + 50.0
                pesos[CategoriaAdaptativa.BLOQUES_RITMO] = (pesos[CategoriaAdaptativa.BLOQUES_RITMO] ?: 0.0) + 40.0
            }
            if (latenciaCP > 2000) {
                pesos[CategoriaAdaptativa.ESCENARIO_RAPIDO] = (pesos[CategoriaAdaptativa.ESCENARIO_RAPIDO] ?: 0.0) + 45.0
                pesos[CategoriaAdaptativa.EVOCACION_SEMANTICA] = (pesos[CategoriaAdaptativa.EVOCACION_SEMANTICA] ?: 0.0) + 35.0
            }
            if (fluidezCP > 80) {
                pesos[CategoriaAdaptativa.PRONUNCIACION_COMPLEJA] = (pesos[CategoriaAdaptativa.PRONUNCIACION_COMPLEJA] ?: 0.0) + 40.0
                pesos[CategoriaAdaptativa.GUION_ESTRESANTE] = (pesos[CategoriaAdaptativa.GUION_ESTRESANTE] ?: 0.0) + 50.0
            }
        }

        if (categoriaEvitada != null) {
            pesos.remove(categoriaEvitada)
        }

        val categoriaSeleccionada = pesos.maxByOrNull { it.value }?.key ?: CategoriaAdaptativa.LECTURA_GUIADA
        val clonicos = siHay(sesiones) { take(3).map { it.bloqueosClonicos }.average() }
        val tonicos = siHay(sesiones) { take(3).map { it.bloqueosTonicos }.average() }
        val latencia = siHay(sesiones) { take(3).map { it.latenciaMediaMs }.average() }
        val fluidez = siHay(sesiones) { take(3).map { it.calificacionGlobal }.average() }

        return when (categoriaSeleccionada) {
            CategoriaAdaptativa.BLOQUES_RITMO -> generarBloques(context, clonicos)
            CategoriaAdaptativa.LECTURA_GUIADA -> generarLectura(context, fluidez)
            CategoriaAdaptativa.HABLA_ESTIRADA -> generarHablaEstirada(context, tonicos)
            CategoriaAdaptativa.ESCENARIO_RAPIDO -> generarEscenario(context, latencia)
            CategoriaAdaptativa.GUION_ESTRESANTE -> generarGuionEstresante(context, fluidez)
            CategoriaAdaptativa.METRONOMO_PURO -> generarMetronomoPuro(context, clonicos)
            CategoriaAdaptativa.BIOFEEDBACK_RELAJACION -> generarBiofeedbackRelajacion(context, tonicos)
            CategoriaAdaptativa.EVOCACION_SEMANTICA -> generarEvocacionSemantica(context)
            CategoriaAdaptativa.PRONUNCIACION_COMPLEJA -> generarPronunciacionCompleja(context)
        }
    }

    private fun <T> siHay(lista: List<Any>, bloque: List<com.itsx.speaktutor.ui.screens.SesionClinica>.() -> T): Double {
        val casted = lista as? List<com.itsx.speaktutor.ui.screens.SesionClinica> ?: return 0.0
        return if (casted.isNotEmpty()) casted.bloque() as? Double ?: 0.0 else 0.0
    }

    // =========================================================================
    // 1. BLOQUES DE RITMO
    // =========================================================================
    private fun generarBloques(context: Context, clonicos: Double): EjercicioGenerado {
        val bloques = GestorEjercicios.obtenerListaPalabras(context, "MotorAdaptativo/bloques_ritmo.json")
        val textoElegido = if (bloques.isNotEmpty()) bloques.random() else "El / a / ma / ne / cer / es / her / mo / so"
        val bpm = if (clonicos > 3) 50 else 70
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.BLOQUES_RITMO,
            titulo = "Bloques de Ritmo Dinámicos IA",
            bpmSugerido = bpm,
            textoPractica = textoElegido,
            instruccionEspecial = "Sincroniza estrictamente cada sílaba separada (/) con un golpe del metrónomo."
        )
    }

    // =========================================================================
    // 2. LECTURA GUIADA
    // =========================================================================
    private fun generarLectura(context: Context, fluidez: Double): EjercicioGenerado {
        val parrafos = GestorEjercicios.obtenerListaPalabras(context, "MotorAdaptativo/lectura_guiada.json")
        val textoElegido = if (parrafos.isNotEmpty()) parrafos.random() else "La computación cuántica promete revolucionar el procesamiento de datos al usar qubits."
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.LECTURA_GUIADA,
            titulo = "Lectura Continua de Contexto",
            bpmSugerido = 0,
            textoPractica = textoElegido,
            instruccionEspecial = if (fluidez > 80) "Reto: Mantén el flujo de aire ininterrumpido a lo largo de toda la oración." else "Apoyo: Respira profundo al ver cada punto y cada coma."
        )
    }

    // =========================================================================
    // 3. HABLA ESTIRADA / INICIOS SUAVES
    // =========================================================================
    private fun generarHablaEstirada(context: Context, tonicos: Double): EjercicioGenerado {
        val frases = GestorEjercicios.obtenerListaPalabras(context, "MotorAdaptativo/habla_estirada.json")
        val textoElegido = if (frases.isNotEmpty()) frases.random() else "Ffffácilmente terminamos el proyecto de ssssoftware a tiempo."
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.HABLA_ESTIRADA,
            titulo = "Inicios Suaves (Contacto Ligero)",
            bpmSugerido = 0,
            textoPractica = textoElegido,
            instruccionEspecial = "Elongación de sonido inicial: Mantén la primera letra estirada y con volumen muy bajo para evitar bloqueos tónicos."
        )
    }

    // =========================================================================
    // 4. ESCENARIOS RÁPIDOS
    // =========================================================================
    private fun generarEscenario(context: Context, latencia: Double): EjercicioGenerado {
        val listaEscenarios = GestorEjercicios.obtenerEscenarios(context, "MotorAdaptativo/escenarios_rapidos.json", 100)
        val esc = if (listaEscenarios.isNotEmpty()) listaEscenarios.random() else Triple("Taxi", "Buenas tardes, ¿a dónde le llevo?", "Buenas tardes, a la estación de autobuses por favor.")
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.ESCENARIO_RAPIDO,
            titulo = "Simulación Rápida: ${esc.first}",
            bpmSugerido = 0,
            textoPractica = esc.third,
            instruccionEspecial = if (latencia > 2000) "Inhala lento antes de hablar. La IA no te apresurará." else "Responde con naturalidad a la pregunta.",
            audioInterlocutor = esc.second
        )
    }

    // =========================================================================
    // 5. GUIONES DE ALTA DEMANDA / ESTRESANTES
    // =========================================================================
    private fun generarGuionEstresante(context: Context, fluidez: Double): EjercicioGenerado {
        val listaGuiones = GestorEjercicios.obtenerEscenarios(context, "MotorAdaptativo/guiones_estresantes.json", 100)
        val guion = if (listaGuiones.isNotEmpty()) listaGuiones.random() else Triple("Entrevista de Trabajo", "Muy interesante su perfil. ¿Por qué le gustaría trabajar con nosotros?", "Porque admiro su cultura laboral y creo que puedo aportar mucho valor a su equipo.")
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.GUION_ESTRESANTE,
            titulo = "Guion de Alta Demanda: ${guion.first}",
            bpmSugerido = 0,
            textoPractica = guion.third,
            instruccionEspecial = "Situación estresante. Concéntrate en mantener la calma y el flujo diafragmático mientras respondes.",
            audioInterlocutor = guion.second
        )
    }

    // =========================================================================
    // 6. EVOCACIÓN SEMÁNTICA
    // =========================================================================
    private fun generarEvocacionSemantica(context: Context): EjercicioGenerado {
        val dic = GestorEjercicios.obtenerDiccionarioEvocacion(context, "MotorAdaptativo/evocacion_semantica.json")

        val categoria = if (dic.isNotEmpty()) {
            dic.entries.random()
        } else {
            mapOf("ANIMALES" to listOf("Perro", "Gato", "León", "Tigre", "Elefante")).entries.first()
        }

        val listaStr = categoria.value.joinToString(" / ")

        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.EVOCACION_SEMANTICA,
            titulo = "Agilidad Mental: ${categoria.key}",
            bpmSugerido = 0,
            textoPractica = listaStr,
            instruccionEspecial = "Lee esta lista de elementos sin detenerte a pensar. Prioriza la velocidad de evocación verbal."
        )
    }

    // =========================================================================
    // 7. PRONUNCIACIÓN COMPLEJA Y TRABALENGUAS
    // =========================================================================
    private fun generarPronunciacionCompleja(context: Context): EjercicioGenerado {
        val retos = GestorEjercicios.obtenerListaPalabras(context, "MotorAdaptativo/pronunciacion_compleja.json")
        val textoElegido = if (retos.isNotEmpty()) retos.random() else "La paralelepípeda estructura del edificio anticonstitucional."
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.PRONUNCIACION_COMPLEJA,
            titulo = "Reto de Articulación y Trabalenguas",
            bpmSugerido = 0,
            textoPractica = textoElegido,
            instruccionEspecial = "El objetivo NO es la velocidad. Abre exageradamente la boca y marca cada sílaba trabada o compleja."
        )
    }

    // =========================================================================
    // 8. METRÓNOMO FONOLÓGICO BASE
    // =========================================================================
    private fun generarMetronomoPuro(context: Context, clonicos: Double): EjercicioGenerado {
        val patrones = GestorEjercicios.obtenerListaPalabras(context, "MotorAdaptativo/metronomo_puro.json")
        val textoElegido = if (patrones.isNotEmpty()) patrones.random() else "Ma - me - mi - mo - mu"
        val bpm = if (clonicos > 4) 45 else 60
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.METRONOMO_PURO,
            titulo = "Metrónomo Fonológico Base",
            bpmSugerido = bpm,
            textoPractica = textoElegido,
            instruccionEspecial = "Enfócate SOLO en el ritmo. Sincroniza el inicio de cada sílaba con el 'Tic' del metrónomo."
        )
    }

    // =========================================================================
    // 9. BIOFEEDBACK Y RELAJACIÓN
    // =========================================================================
    private fun generarBiofeedbackRelajacion(context: Context, tonicos: Double): EjercicioGenerado {
        val fonaciones = GestorEjercicios.obtenerListaPalabras(context, "MotorAdaptativo/biofeedback_relajacion.json")
        val textoElegido = if (fonaciones.isNotEmpty()) fonaciones.random() else "Aaaaaaaaaaaaaaaaaah"
        val urgencia = if (tonicos > 5) "Tensión severa detectada." else "Relajación preventiva."
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.BIOFEEDBACK_RELAJACION,
            titulo = "Fonación Continua Relajada",
            bpmSugerido = 0,
            textoPractica = textoElegido,
            instruccionEspecial = "$urgencia Inhala, y suelta este sonido suavemente sin forzar la garganta. Mantén el Biofeedback en verde."
        )
    }
}