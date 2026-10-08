package com.itsx.speaktutor.logic

import android.content.Context
import com.itsx.speaktutor.ui.screens.ProgresoStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

enum class EstadoVozDSP {
    SILENCIO, FLUIDO, BLOQUEO
}

data class ParametrosAcusticos(
    val rmsEnergia: Float = 0f,
    val zeroCrossingRate: Int = 0,
    val estado: EstadoVozDSP = EstadoVozDSP.SILENCIO,
    val tramasVocales: Int = 0,
    val tramasBloqueo: Int = 0,
    val ataqueBrusco: Float = 0f
)

class MotorAudioDSP {
    init {
        System.loadLibrary("oboe_dsp")
    }

    external fun startOboe()
    external fun stopOboe()
    external fun getMetrics(): FloatArray

    val metricasAcusticas = MutableStateFlow(ParametrosAcusticos())

    private var isRecording = false
    private var pollingJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var tramasVocalesTotales = 0
    private var tramasConBloqueo = 0

    fun iniciarAnalisisDSP() {
        if (isRecording) return
        isRecording = true
        startOboe()

        pollingJob = coroutineScope.launch {
            while (isActive && isRecording) {
                val metrics = getMetrics()

                val rms = metrics[0]
                val zcr = metrics[1].toInt()
                val estadoInt = metrics[2].toInt()
                tramasVocalesTotales = metrics[3].toInt()
                tramasConBloqueo = metrics[4].toInt()
                val ataqueBruscoVal = metrics[5]

                val estado = when (estadoInt) {
                    1 -> EstadoVozDSP.FLUIDO
                    2 -> EstadoVozDSP.BLOQUEO
                    else -> EstadoVozDSP.SILENCIO
                }

                metricasAcusticas.value = ParametrosAcusticos(
                    rmsEnergia = rms,
                    zeroCrossingRate = zcr,
                    estado = estado,
                    tramasVocales = tramasVocalesTotales,
                    tramasBloqueo = tramasConBloqueo,
                    ataqueBrusco = ataqueBruscoVal
                )

                delay(16)
            }
        }
    }

    fun detenerAnalisis(context: Context? = null, nombreModulo: String = "Módulo DSP") {
        isRecording = false
        pollingJob?.cancel()
        stopOboe()

        if (context != null && tramasVocalesTotales > 10) {
            val aciertos = tramasVocalesTotales - tramasConBloqueo
            val calificacionFinal = if (tramasVocalesTotales > 0) ((aciertos.toFloat() / tramasVocalesTotales) * 100).toInt().coerceIn(0, 100) else 0

            ProgresoStorage.guardarSesion(
                context = context,
                dificultad = nombreModulo,
                aciertos = aciertos,
                errores = tramasConBloqueo,
                calificacion = calificacionFinal
            )
        }

        tramasVocalesTotales = 0
        tramasConBloqueo = 0
        metricasAcusticas.value = ParametrosAcusticos()
    }
}