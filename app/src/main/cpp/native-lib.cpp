#include <jni.h>
#include <oboe/Oboe.h>
#include <cmath>
#include <atomic>

std::atomic<float> currentRms{0.0f};
std::atomic<float> currentZcr{0.0f};
std::atomic<int> currentState{0};
std::atomic<int> tramasVocales{0};
std::atomic<int> tramasBloqueo{0};
std::atomic<float> ataqueBrusco{0.0f};

float noiseFloor = 0.002f;
float historiaRms[10] = {0.0f};
int framesTensionSostenida = 0;

class DspAudioCallback : public oboe::AudioStreamDataCallback {
public:
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override {
        auto *floatData = static_cast<float *>(audioData);
        float sumEnergia = 0.0f;
        int zcrCount = 0;
        float previousValue = 0.0f;

        for (int i = 0; i < numFrames; i++) {
            float currentValue = floatData[i];
            sumEnergia += std::abs(currentValue);
            if ((previousValue > 0 && currentValue <= 0) || (previousValue < 0 && currentValue >= 0)) {
                zcrCount++;
            }
            previousValue = currentValue;
        }

        float energiaPromedio = sumEnergia / static_cast<float>(numFrames);
        float tasaZcr = static_cast<float>(zcrCount) / static_cast<float>(numFrames);
        float rmsEscalado = energiaPromedio * 20000.0f;

        if (energiaPromedio < noiseFloor) {
            noiseFloor = energiaPromedio;
        } else {
            noiseFloor = (noiseFloor * 0.995f) + (energiaPromedio * 0.005f);
        }

        float umbralVoz = noiseFloor * 1.5f;
        if (umbralVoz < 0.005f) umbralVoz = 0.005f;

        for (int i = 9; i > 0; i--) historiaRms[i] = historiaRms[i - 1];
        historiaRms[0] = rmsEscalado;

        int estado = 0;
        // Se bajó el umbral de energía de 80.0f a 50.0f para captar voces más suaves
        if (energiaPromedio > umbralVoz && rmsEscalado > 50.0f) {
            float varianzaRms = std::abs(historiaRms[0] - historiaRms[3]);

            // Detección Clónica: Umbrales suavizados para detectar el "rebote"
            bool esClonico = (varianzaRms > 80.0f && tasaZcr > 0.02f);

            // Detección Tónica: Se permite un poco más de varianza natural humana
            bool esTonico = false;
            if (varianzaRms < 60.0f && rmsEscalado > 120.0f) {
                framesTensionSostenida++;
                if (framesTensionSostenida > 3) { // Reacciona más rápido
                    esTonico = true;
                }
            } else {
                framesTensionSostenida = 0;
            }

            // Asignación directa de estados
            if (esClonico) {
                estado = 3; // CLÓNICO
            } else if (esTonico) {
                estado = 2; // TÓNICO
            } else {
                estado = 1; // FLUIDO
            }
        } else {
            estado = 0; // SILENCIO
            framesTensionSostenida = 0;
        }

        if (estado != 0) {
            tramasVocales++;
            if (estado == 2 || estado == 3) tramasBloqueo++;
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
    builder.setDirection(oboe::Direction::Input)->setPerformanceMode(oboe::PerformanceMode::LowLatency)->setSharingMode(oboe::SharingMode::Exclusive)->setFormat(oboe::AudioFormat::Float)->setChannelCount(1)->setInputPreset(oboe::InputPreset::VoiceCommunication)->setDataCallback(&callback);
    tramasVocales.store(0); tramasBloqueo.store(0); framesTensionSostenida = 0;
    if (builder.openStream(stream) == oboe::Result::OK) stream->requestStart();
}

extern "C" JNIEXPORT void JNICALL
Java_com_itsx_speaktutor_logic_MotorAudioDSP_stopOboe(JNIEnv *env, jobject thiz) {
    if (stream) { stream->requestStop(); stream->close(); stream.reset(); }
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_itsx_speaktutor_logic_MotorAudioDSP_getMetrics(JNIEnv *env, jobject thiz) {
    jfloatArray result = env->NewFloatArray(6);
    jfloat fill[6] = { currentRms.load(), currentZcr.load(), static_cast<float>(currentState.load()),
                       static_cast<float>(tramasVocales.load()), static_cast<float>(tramasBloqueo.load()), ataqueBrusco.load() };
    env->SetFloatArrayRegion(result, 0, 6, fill);
    return result;
}