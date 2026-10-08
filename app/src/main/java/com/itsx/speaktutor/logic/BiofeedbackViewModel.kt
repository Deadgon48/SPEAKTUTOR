package com.itsx.speaktutor.logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BiofeedbackViewModel : ViewModel() {

    // 1. Creamos la instancia de tu clase existente
    private val motorDSP = MotorAudioDSP()

    var rmsActual by mutableFloatStateOf(0f)
        private set

    // 2. Corregido el espacio en blanco que marcaba error
    var ataqueBruscoActual by mutableFloatStateOf(0f)
        private set

    fun iniciarMonitoreo() {
        // 3. Llamamos a las funciones usando la instancia
        motorDSP.startOboe()

        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                // Obtenemos las métricas desde la instancia
                val metricas = motorDSP.getMetrics()

                withContext(Dispatchers.Main) {
                    if (metricas.size >= 6) {
                        rmsActual = metricas[0]
                        ataqueBruscoActual = metricas[5]
                    }
                }
                delay(50)
            }
        }
    }

    fun detenerMonitoreo() {
        motorDSP.stopOboe()
    }
}