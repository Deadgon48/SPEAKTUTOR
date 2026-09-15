package com.itsx.speaktutor.logic

import com.itsx.speaktutor.model.EjercicioPersonalizado
import com.itsx.speaktutor.model.TipoModulo

object AdaptadorEjercicios {

    // Función que selecciona el ejercicio adaptado basándose en el porcentaje de aciertos del usuario
    fun generarEjercicioSugerido(porcentajeAciertosUltimo: Float): EjercicioPersonalizado {
        return when {
            porcentajeAciertosUltimo < 60f -> {
                // Nivel de refuerzo / Principiante (Ritmo más lento, oraciones cortas)
                EjercicioPersonalizado(
                    id = "ej_refuerzo_01",
                    titulo = "Ritmo Asistido Lento",
                    modulo = TipoModulo.METRONOMO,
                    nivelDificultad = 1,
                    objetivoBPM = 60, // Pulsos lentos para control de respiración
                    contenidoTexto = "Pa-la-bras cla-ras y pau-sa-das.",
                    descripcionAdaptativa = "Detectamos que necesitas afianzar la base. Este ejercicio reduce la velocidad para priorizar tu articulación."
                )
            }
            porcentajeAciertosUltimo in 60f..85f -> {
                // Nivel Intermedio (Ritmo moderado, práctica estándar)
                EjercicioPersonalizado(
                    id = "ej_intermedio_01",
                    titulo = "Fluidez en Bloques Dinámicos",
                    modulo = TipoModulo.RITMO_FLUIDEZ,
                    nivelDificultad = 2,
                    objetivoBPM = 90,
                    contenidoTexto = "La práctica constante mejora la seguridad al hablar en público.",
                    descripcionAdaptativa = "¡Buen trabajo! Mantenemos un ritmo constante para ganar naturalidad y fluidez."
                )
            }
            else -> {
                // Nivel Avanzado (Mayor exigencia y velocidad)
                EjercicioPersonalizado(
                    id = "ej_avanzado_01",
                    titulo = "Simulación Conversacional Avanzada",
                    modulo = TipoModulo.SIMULACION,
                    nivelDificultad = 3,
                    objetivoBPM = 110,
                    contenidoTexto = "Simulación de entrevista laboral o exposición compleja.",
                    descripcionAdaptativa = "Tienes un excelente dominio. Subimos el nivel con escenarios de alta exigencia comunicativa."
                )
            }
        }
    }
}
