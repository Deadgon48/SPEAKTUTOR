package com.itsx.speaktutor.logic

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.itsx.speaktutor.ui.screens.ProgresoStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

enum class EstadoVozDSP {
    SILENCIO, FLUIDO, BLOQUEO
}

data class ParametrosAcusticos(
    val rmsEnergia: Float = 0f,
    val zeroCrossingRate: Int = 0,
    val estado: EstadoVozDSP = EstadoVozDSP.SILENCIO
)

class MotorAudioDSP {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    val metricasAcusticas = MutableStateFlow(ParametrosAcusticos())

    // Contadores para enviar la evaluación a ProgresoStorage
    private var tramasVocalesTotales = 0
    private var tramasConBloqueo = 0

    @SuppressLint("MissingPermission")
    fun iniciarAnalisisDSP() {
        if (isRecording) return

        tramasVocalesTotales = 0
        tramasConBloqueo = 0

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        Thread {
            val audioBuffer = ShortArray(bufferSize)
            while (isRecording) {
                val readResult = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (readResult > 0) {
                    procesarTrama(audioBuffer, readResult)
                }
            }
        }.start()
    }

    private fun procesarTrama(buffer: ShortArray, bytesLeidos: Int) {
        var sumaEnergia = 0f
        var crucesPorCero = 0
        var valorAnterior = buffer[0]

        for (i in 0 until bytesLeidos) {
            val valorActual = buffer[i]
            sumaEnergia += abs(valorActual.toFloat())

            if ((valorAnterior > 0 && valorActual <= 0) || (valorAnterior < 0 && valorActual >= 0)) {
                crucesPorCero++
            }
            valorAnterior = valorActual
        }

        val energiaPromedio = sumaEnergia / bytesLeidos

        // Nueva Heurística Clínica con umbral de silencio
        val estadoActual = when {
            energiaPromedio < 250f -> EstadoVozDSP.SILENCIO // Ignorar ruido estático del micrófono
            (energiaPromedio < 800f && crucesPorCero < 15) || (crucesPorCero > 80) -> EstadoVozDSP.BLOQUEO // Cierre glótico o fricación excesiva
            else -> EstadoVozDSP.FLUIDO // Energía fuerte y ZCR estable
        }

        // Registrar estadísticas solo si el usuario está intentando hablar
        if (estadoActual != EstadoVozDSP.SILENCIO) {
            tramasVocalesTotales++
            if (estadoActual == EstadoVozDSP.BLOQUEO) {
                tramasConBloqueo++
            }
        }

        metricasAcusticas.value = ParametrosAcusticos(
            rmsEnergia = energiaPromedio,
            zeroCrossingRate = crucesPorCero,
            estado = estadoActual
        )
    }

    // Modificamos el método para recibir el contexto y guardar en Progreso
    fun detenerAnalisis(context: Context? = null, nombreModulo: String = "Módulo DSP") {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        // Guardar progreso si hubo suficiente actividad vocal (ej. más de 10 tramas detectadas)
        if (context != null && tramasVocalesTotales > 10) {
            val aciertos = tramasVocalesTotales - tramasConBloqueo
            val calificacionFinal = ((aciertos.toFloat() / tramasVocalesTotales) * 100).toInt().coerceIn(0, 100)

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