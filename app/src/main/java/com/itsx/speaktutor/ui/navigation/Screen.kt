package com.itsx.speaktutor.ui.navigation

sealed class Screen(val route: String) {
    object Menu : Screen("menu")

    object TarjetasShader : Screen("tarjetas_shader_screen")
    object Metronomo : Screen("metronomo")
    object HablaEstirada : Screen("habla_estirada")

    object RitmoFluidez : Screen("entrenamiento_ritmo")

    object SimulacionSituaciones : Screen("simulacion_situaciones")

    // Agrega esta línea dentro de tu clase Screen sealed
    object PronunciacionInstante : Screen("pronunciacion_instante")
}