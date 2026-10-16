package com.itsx.speaktutor.logic

import android.content.Context
import com.itsx.speaktutor.ui.screens.ProgresoStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

private var bloqueosTonicos = 0
private var bloqueosClonicos = 0
private var iniciosSuaves = 0
private var iniciosBruscos = 0
private var tiempoInicioMs = 0L
private var latenciaAlcanzadaMs = 0L
private var fonacionIniciada = false

enum class EstadoVozDSP {
    SILENCIO, FLUIDO, BLOQUEO_TONICO, BLOQUEO_CLONICO
}

data class ParametrosAcusticos(
    val rmsEnergia: Float = 0f,
    val zeroCrossingRate: Int = 0,
    val estado: EstadoVozDSP = EstadoVozDSP.SILENCIO,
    val tramasVocales: Int = 0,
    val tramasBloqueo: Int = 0,
    val ataqueBrusco: Float = 0f,
    // --- NUEVAS MÉTRICAS EXPUESTAS EN TIEMPO REAL ---
    val tonicosActuales: Int = 0,
    val clonicosActuales: Int = 0,
    val esClonico: Boolean = false
)

class MotorAudioDSP {
    init { System.loadLibrary("oboe_dsp") }

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
        tiempoInicioMs = System.currentTimeMillis()
        fonacionIniciada = false
        startOboe()

        pollingJob = coroutineScope.launch {
            var bloqueosConsecutivos = 0 // Filtro anti-rebotes
            var ultimoEstadoVoz = 0

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
                    2 -> EstadoVozDSP.BLOQUEO_TONICO
                    3 -> EstadoVozDSP.BLOQUEO_CLONICO
                    else -> EstadoVozDSP.SILENCIO
                }

                // 1. Cálculo de latencia (Solo ocurre la primera vez que hay voz)
                if (!fonacionIniciada && estadoInt != 0) {
                    latenciaAlcanzadaMs = System.currentTimeMillis() - tiempoInicioMs
                    fonacionIniciada = true
                }

                // 2. Registro inmediato de cómo inicia la voz desde el silencio
                if (estadoInt == 2 && ultimoEstadoVoz == 0) {
                    iniciosBruscos++
                } else if (estadoInt == 1 && ultimoEstadoVoz == 0) {
                    iniciosSuaves++
                }

                // FILTRO ANTI-REBOTES CLÍNICO
                var esClonicoActual = false
                if (estadoInt == 2 || estadoInt == 3) {
                    bloqueosConsecutivos++
                    // Solo contamos el bloqueo si persiste por al menos 3 ciclos seguidos (~50ms)
                    if (bloqueosConsecutivos == 3) {
                        if (estadoInt == 3) {
                            bloqueosClonicos++
                            esClonicoActual = true
                        } else {
                            bloqueosTonicos++
                            esClonicoActual = false
                        }
                        bloqueosConsecutivos = 0 // Reseteamos para no sumar infinitamente
                    }
                } else {
                    bloqueosConsecutivos = 0
                }

                ultimoEstadoVoz = estadoInt

                metricasAcusticas.value = ParametrosAcusticos(
                    rmsEnergia = rms,
                    zeroCrossingRate = zcr,
                    estado = estado,
                    tramasVocales = tramasVocalesTotales,
                    tramasBloqueo = tramasConBloqueo,
                    ataqueBrusco = ataqueBruscoVal,
                    tonicosActuales = bloqueosTonicos,
                    clonicosActuales = bloqueosClonicos,
                    esClonico = esClonicoActual
                )
                delay(16L)
            }
        }
    }

    fun pausarAnalisisTemporal() {
        if (!isRecording) return
        isRecording = false
        pollingJob?.cancel()
        stopOboe()
    }

    fun reanudarAnalisis() {
        if (isRecording) return
        iniciarAnalisisDSP()
    }

    fun detenerAnalisis(context: Context? = null, nombreModulo: String = "Módulo DSP") {
        isRecording = false
        pollingJob?.cancel()
        stopOboe()

        if (context != null && tramasVocalesTotales > 10) {
            val aciertos = tramasVocalesTotales - tramasConBloqueo
            // Se elimina la condición redundante '> 0' porque ya pasó el filtro '> 10'
            val calificacionFinal = ((aciertos.toFloat() / tramasVocalesTotales) * 100).toInt().coerceIn(0, 100)
            val totalInicios = iniciosSuaves + iniciosBruscos
            val pctContactosLigeros = if (totalInicios > 0) ((iniciosSuaves.toFloat() / totalInicios) * 100).toInt() else 100

            ProgresoStorage.guardarSesionMultidimensional(
                context = context, modulo = nombreModulo, aciertos = aciertos,
                erroresTotales = tramasConBloqueo, calificacion = calificacionFinal,
                bloqueosTonicos = bloqueosTonicos, bloqueosClonicos = bloqueosClonicos,
                latenciaMedia = latenciaAlcanzadaMs.toInt(), contactosLigeros = pctContactosLigeros
            )
        }

        tramasVocalesTotales = 0; tramasConBloqueo = 0; bloqueosTonicos = 0; bloqueosClonicos = 0
        iniciosSuaves = 0; iniciosBruscos = 0; latenciaAlcanzadaMs = 0L
        metricasAcusticas.value = ParametrosAcusticos()
    }
}