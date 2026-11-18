package com.itsx.speaktutor.logic

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock

import com.itsx.speaktutor.ui.screens.ProgresoStorage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EstadoVozDSP {
    SILENCIO,
    FLUIDO,
    BLOQUEO_TONICO,
    BLOQUEO_CLONICO
}

data class ParametrosAcusticos(
    val rmsEnergia: Float = 0f,
    val zeroCrossingRate: Int = 0,
    val estado: EstadoVozDSP = EstadoVozDSP.SILENCIO,
    val tramasVocales: Int = 0,
    val tramasBloqueo: Int = 0,
    val ataqueBrusco: Float = 0f,
    val tonicosActuales: Int = 0,
    val clonicosActuales: Int = 0,
    val esClonico: Boolean = false,
    val hfTensionRatio: Float = 0f,
    val spectralFlux: Float = 0f
)

class MotorAudioDSP {

    companion object {
        const val INTERVALO_METRICAS_MS = 16L
        const val TAMANO_METRICAS = 8

        const val UMBRAL_ALERTA_BLOQUEOS = 40
        const val TRAMAS_EVALUACION = 250

        const val DAF_MIN_MS = 30
        const val DAF_MAX_MS = 400
        const val FAF_MIN_OCT = -1.2f
        const val FAF_MAX_OCT = 0.8f

        init {
            System.loadLibrary("oboe_dsp")
        }
    }

    external fun startOboe(): Boolean
    external fun stopOboe()
    external fun resetOboeMetrics()
    external fun getMetrics(): FloatArray

    external fun setDAFDelay(delayMs: Int)
    external fun setFAFPitch(pitchOctaves: Float)

    val _metricasAcusticas = MutableStateFlow(ParametrosAcusticos())
    val metricasAcusticas: StateFlow<ParametrosAcusticos> = _metricasAcusticas.asStateFlow()

    val _alertaEmpeoramiento = MutableStateFlow(false)
    val alertaEmpeoramiento: StateFlow<Boolean> = _alertaEmpeoramiento.asStateFlow()

    private val _dafDelayState = MutableStateFlow(100)
    val dafDelayState: StateFlow<Int> = _dafDelayState.asStateFlow()

    private val _fafPitchState = MutableStateFlow(0f)
    val fafPitchState: StateFlow<Float> = _fafPitchState.asStateFlow()

    val motorLock = Any()

    @Volatile
    var isRecording = false

    var pollingJob: Job? = null
    val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var tramasVocalesTotales = 0
    var tramasConBloqueo = 0

    var bloqueosTonicos = 0
    var bloqueosClonicos = 0

    var iniciosSuaves = 0
    var iniciosBruscos = 0

    var tiempoInicioMs = 0L
    var latenciaAlcanzadaMs = 0L

    var fonacionIniciada = false
    var ultimoEstadoVoz = 0

    var bloqueosEnEvaluacion = 0
    var tramasEvaluadas = 0

    @Volatile
    var dafDelayActual = 100

    @Volatile
    var fafPitchActual = 0f

    fun configurarDAF(delayMs: Int) {
        val valor = delayMs.coerceIn(DAF_MIN_MS, DAF_MAX_MS)
        synchronized(motorLock) {
            dafDelayActual = valor
            _dafDelayState.value = valor
            setDAFDelay(valor)
        }
    }

    fun configurarFAF(octavas: Float) {
        if (!octavas.isFinite()) return
        val valor = octavas.coerceIn(FAF_MIN_OCT, FAF_MAX_OCT)
        synchronized(motorLock) {
            fafPitchActual = valor
            _fafPitchState.value = valor
            setFAFPitch(valor)
        }
    }

    fun configurarAudioAudifonos(context: Context, activar: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (activar) {
            audioManager.mode = AudioManager.MODE_NORMAL
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val tieneA2dpOCable = devices.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            }
            if (!tieneA2dpOCable && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                @Suppress("DEPRECATION")
                if (audioManager.isBluetoothScoAvailableOffCall) {
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                if (audioManager.isBluetoothScoOn) {
                    audioManager.isBluetoothScoOn = false
                    audioManager.stopBluetoothSco()
                }
            }
        }
    }

    fun reiniciarEstadisticas() {
        tramasVocalesTotales = 0
        tramasConBloqueo = 0
        bloqueosTonicos = 0
        bloqueosClonicos = 0
        iniciosSuaves = 0
        iniciosBruscos = 0
        tiempoInicioMs = SystemClock.elapsedRealtime()
        latenciaAlcanzadaMs = 0L
        fonacionIniciada = false
        ultimoEstadoVoz = 0
        bloqueosEnEvaluacion = 0
        tramasEvaluadas = 0
        _alertaEmpeoramiento.value = false
        _metricasAcusticas.value = ParametrosAcusticos()
    }

    fun iniciarMotor(reiniciar: Boolean, context: Context? = null) {
        synchronized(motorLock) {
            if (isRecording) return

            try {
                context?.let { configurarAudioAudifonos(it, true) }

                if (reiniciar) {
                    reiniciarEstadisticas()
                    resetOboeMetrics()
                }

                val iniciadoCorrectamente = startOboe()
                if (!iniciadoCorrectamente) {
                    isRecording = false
                    return
                }

                setDAFDelay(dafDelayActual)
                setFAFPitch(fafPitchActual)

                isRecording = true
                iniciarPolling()

            } catch (e: Exception) {
                isRecording = false
                pollingJob?.cancel()
                pollingJob = null
                try {
                    stopOboe()
                } catch (_: Exception) {
                }
                throw e
            }
        }
    }

    fun iniciarPolling() {
        pollingJob?.cancel()
        pollingJob = coroutineScope.launch {
            while (isActive && isRecording) {
                var continuar = true
                synchronized(motorLock) {
                    if (!isRecording) {
                        continuar = false
                    } else {
                        try {
                            val metrics = getMetrics()
                            if (metrics.size >= TAMANO_METRICAS) {
                                procesarMetricas(metrics)
                            }
                        } catch (_: Exception) {
                            isRecording = false
                            continuar = false
                            try {
                                stopOboe()
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
                if (!continuar) break
                delay(INTERVALO_METRICAS_MS)
            }
        }
    }

    fun procesarMetricas(metrics: FloatArray) {
        val rms = metrics[0].takeIf { it.isFinite() } ?: 0f
        val zcr = metrics[1].takeIf { it.isFinite() }?.toInt()?.coerceAtLeast(0) ?: 0
        val estadoInt = metrics[2].takeIf { it.isFinite() }?.toInt() ?: 0
        tramasVocalesTotales = metrics[3].takeIf { it.isFinite() }?.toInt()?.coerceAtLeast(0) ?: 0
        tramasConBloqueo = metrics[4].takeIf { it.isFinite() }?.toInt()?.coerceAtLeast(0) ?: 0
        val ataqueBruscoVal = metrics[5].takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val hfRatioVal = metrics[6].takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
        val spectralFluxVal = metrics[7].takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f

        val estado = when (estadoInt) {
            1 -> EstadoVozDSP.FLUIDO
            2 -> EstadoVozDSP.BLOQUEO_TONICO
            3 -> EstadoVozDSP.BLOQUEO_CLONICO
            else -> EstadoVozDSP.SILENCIO
        }

        val esBloqueo = estadoInt == 2 || estadoInt == 3
        val eraBloqueo = ultimoEstadoVoz == 2 || ultimoEstadoVoz == 3

        if (esBloqueo && !eraBloqueo) {
            when (estadoInt) {
                2 -> bloqueosTonicos++
                3 -> bloqueosClonicos++
            }
        }

        if (!fonacionIniciada && estadoInt != 0) {
            latenciaAlcanzadaMs = SystemClock.elapsedRealtime() - tiempoInicioMs
            fonacionIniciada = true
        }

        if (estadoInt != 0 && ultimoEstadoVoz == 0) {
            when (estadoInt) {
                1 -> iniciosSuaves++
                2, 3 -> iniciosBruscos++
            }
        }

        ultimoEstadoVoz = estadoInt

        tramasEvaluadas++
        if (esBloqueo) {
            bloqueosEnEvaluacion++
        }

        if (tramasEvaluadas >= TRAMAS_EVALUACION) {
            val empeoramiento = bloqueosEnEvaluacion > UMBRAL_ALERTA_BLOQUEOS
            _alertaEmpeoramiento.value = empeoramiento

            if (empeoramiento) {
                val nuevoDelay = (dafDelayActual - 15).coerceAtLeast(DAF_MIN_MS)
                if (nuevoDelay != dafDelayActual) {
                    configurarDAF(nuevoDelay)
                }
            }

            bloqueosEnEvaluacion = 0
            tramasEvaluadas = 0
        }

        _metricasAcusticas.value = ParametrosAcusticos(
            rmsEnergia = rms,
            zeroCrossingRate = zcr,
            estado = estado,
            tramasVocales = tramasVocalesTotales,
            tramasBloqueo = tramasConBloqueo,
            ataqueBrusco = ataqueBruscoVal,
            tonicosActuales = bloqueosTonicos,
            clonicosActuales = bloqueosClonicos,
            esClonico = estadoInt == 3,
            hfTensionRatio = hfRatioVal,
            spectralFlux = spectralFluxVal
        )
    }

    fun iniciarAnalisisDSP(context: Context? = null) {
        iniciarMotor(reiniciar = true, context = context)
    }

    fun pausarAnalisisTemporal() {
        synchronized(motorLock) {
            if (!isRecording) return
            isRecording = false
            pollingJob?.cancel()
            pollingJob = null
            try {
                stopOboe()
            } catch (_: Exception) {
            }
        }
    }

    fun reanudarAnalisis(context: Context? = null) {
        iniciarMotor(reiniciar = false, context = context)
    }

    fun detenerAnalisis(
        context: Context? = null,
        nombreModulo: String = "Módulo DSP"
    ) {
        synchronized(motorLock) {
            isRecording = false
            pollingJob?.cancel()
            pollingJob = null
            try {
                stopOboe()
            } catch (_: Exception) {
            }
            context?.let { configurarAudioAudifonos(it, false) }
        }

        if (context != null && tramasVocalesTotales > 10) {
            val aciertos = (tramasVocalesTotales - tramasConBloqueo).coerceAtLeast(0)
            val calificacionFinal = (aciertos.toFloat() / tramasVocalesTotales.toFloat() * 100f)
                .toInt().coerceIn(0, 100)

            val totalInicios = iniciosSuaves + iniciosBruscos
            val pctContactosLigeros = if (totalInicios > 0) {
                (iniciosSuaves.toFloat() / totalInicios.toFloat() * 100f).toInt()
            } else {
                100
            }

            ProgresoStorage.guardarSesionMultidimensional(
                context = context,
                modulo = nombreModulo,
                aciertos = aciertos,
                erroresTotales = tramasConBloqueo,
                calificacion = calificacionFinal,
                bloqueosTonicos = bloqueosTonicos,
                bloqueosClonicos = bloqueosClonicos,
                latenciaMedia = latenciaAlcanzadaMs.toInt(),
                contactosLigeros = pctContactosLigeros,
                dafDelayMs = dafDelayActual,
                fafPitchOctavas = fafPitchActual
            )
        }

        synchronized(motorLock) {
            reiniciarEstadisticas()
        }
    }
}