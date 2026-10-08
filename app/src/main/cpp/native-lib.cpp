#include <jni.h>
#include <oboe/Oboe.h>
#include <math.h>
#include <atomic>

std::atomic<float> currentRms{0.0f};
std::atomic<float> currentZcr{0.0f};
std::atomic<int> currentState{0}; // 0 = Silencio, 1 = Fluido, 2 = Bloqueo
std::atomic<int> tramasVocales{0};
std::atomic<int> tramasBloqueo{0};
std::atomic<float> ataqueBrusco{0.0f};

float noiseFloor = 0.005f;
// Ventana de memoria para analizar la voz en un rango de ~30ms
float historiaRms[5] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f};

class DspAudioCallback : public oboe::AudioStreamDataCallback {
public:
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override {
        float *floatData = static_cast<float *>(audioData);
        float sumEnergia = 0.0f;
        int zcrCount = 0;
        float previousValue = 0.0f;

        // Cálculo de energía (RMS) y cruces por cero (ZCR)
        for (int i = 0; i < numFrames; i++) {
            float currentValue = floatData[i];
            sumEnergia += fabs(currentValue);

            if ((previousValue > 0 && currentValue <= 0) || (previousValue < 0 && currentValue >= 0)) {
                zcrCount++;
            }
            previousValue = currentValue;
        }

        float energiaPromedio = sumEnergia / numFrames;
        float tasaZcr = (float)zcrCount / numFrames;

        // Suavizado dinámico del piso de ruido ambiental
        if (energiaPromedio < noiseFloor) {
            noiseFloor = energiaPromedio;
        } else {
            noiseFloor = (noiseFloor * 0.99f) + (energiaPromedio * 0.01f);
        }

        float umbralVoz = noiseFloor * 2.5f;
        if (umbralVoz < 0.01f) umbralVoz = 0.01f;

        float rmsEscalado = energiaPromedio * 15000.0f;

        // --- VENTANA DE MEMORIA PARA ATAQUES BRUSCOS ---
        // Desplazamos el historial hacia atrás
        for (int i = 4; i > 0; i--) {
            historiaRms[i] = historiaRms[i - 1];
        }
        historiaRms[0] = rmsEscalado;

        // Comparamos el audio actual con el de hace ~5 callbacks atrás
        float deltaRmsLargo = historiaRms[0] - historiaRms[4];

        // Si salta de silencio a voz fuerte muy rápido (Golpe de voz / Tartamudeo Inicial)
        if (historiaRms[4] < 600.0f && historiaRms[0] > 1800.0f && deltaRmsLargo > 1200.0f) {
            ataqueBrusco.store(1.0f);
        } else if (rmsEscalado < 400.0f) {
            // El ataque se reinicia únicamente cuando haces una pausa de silencio
            ataqueBrusco.store(0.0f);
        }

        // --- HEURÍSTICA CLÍNICA DE TARTAMUDEZ Y TENSION ---
        int estado = 0;

        if (energiaPromedio < umbralVoz) {
            estado = 0; // Silencio
        } else {
            // 1. Golpe de glotis: Inicio tenso que ya detectamos con la ventana de memoria.
            bool esGolpe = (ataqueBrusco.load() == 1.0f);

            // 2. Fricción Tensa: Quedarse trabado en "ssss", "fffff", "rrrr". Alto ZCR + Alta Energía.
            bool esFriccionTensa = (tasaZcr > 0.15f && rmsEscalado > 2500.0f);

            // 3. Bloqueo Cerrado: Hacer mucho esfuerzo pero que no salga el sonido (vocales trabadas). Bajo ZCR + Muchísima Energía.
            bool esBloqueoCerrado = (tasaZcr < 0.02f && rmsEscalado > 3000.0f);

            if (esGolpe || esFriccionTensa || esBloqueoCerrado) {
                estado = 2; // BLOQUEO (Fallo)
            } else {
                estado = 1; // FLUIDO (Acierto)
            }
        }

        // Actualización de contadores globales
        if (estado != 0) {
            tramasVocales++;
            if (estado == 2) tramasBloqueo++;
        }

        currentRms.store(rmsEscalado);
        currentZcr.store(tasaZcr * 100.0f);
        currentState.store(estado);

        return oboe::DataCallbackResult::Continue;
    }
};

DspAudioCallback callback;
std::shared_ptr<oboe::AudioStream> stream;

extern "C" JNIEXPORT void JNICALL
Java_com_itsx_speaktutor_logic_MotorAudioDSP_startOboe(JNIEnv *env, jobject thiz) {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Input)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(1)
            ->setInputPreset(oboe::InputPreset::VoiceCommunication)
            ->setDataCallback(&callback);

    tramasVocales.store(0);
    tramasBloqueo.store(0);
    ataqueBrusco.store(0.0f);

    oboe::Result result = builder.openStream(stream);
    if (result == oboe::Result::OK) {
        stream->requestStart();
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_itsx_speaktutor_logic_MotorAudioDSP_stopOboe(JNIEnv *env, jobject thiz) {
    if (stream) {
        stream->requestStop();
        stream->close();
        stream.reset();
    }
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_itsx_speaktutor_logic_MotorAudioDSP_getMetrics(JNIEnv *env, jobject thiz) {
    jfloatArray result = env->NewFloatArray(6);
    jfloat fill[6];
    fill[0] = currentRms.load();
    fill[1] = currentZcr.load();
    fill[2] = (float)currentState.load();
    fill[3] = (float)tramasVocales.load();
    fill[4] = (float)tramasBloqueo.load();
    fill[5] = ataqueBrusco.load();

    env->SetFloatArrayRegion(result, 0, 6, fill);
    return result;
}