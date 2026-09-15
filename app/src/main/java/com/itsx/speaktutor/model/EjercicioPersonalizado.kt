package com.itsx.speaktutor.model

enum class TipoModulo {
    METRONOMO,
    HABLA_ESTIRADA,
    RITMO_FLUIDEZ,
    SIMULACION,
    PRONUNCIACION_INSTANTE
}

data class EjercicioPersonalizado(
    val id: String,
    val titulo: String,
    val modulo: TipoModulo,
    val nivelDificultad: Int, // 1 (Principiante), 2 (Intermedio), 3 (Avanzado)
    val objetivoBPM: Int?, // Útil para metrónomo o ritmo
    val contenidoTexto: String, // Frase, fonema o escenario a practicar
    val descripcionAdaptativa: String
)