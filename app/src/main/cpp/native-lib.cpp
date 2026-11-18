#include <jni.h>
#include <oboe/Oboe.h>
#include <algorithm>
#include <atomic>
#include <cmath>
#include <complex>
#include <cstdint>
#include <memory>
#include <mutex>
#include <vector>

namespace {
    constexpr float PI = 3.14159265358979323846f;
    constexpr int HISTORIA_TAMANO = 20;
    constexpr int TAMANO_METRICAS = 8;
    constexpr int FFT_SIZE = 512;

    // Métricas atómicas hacia Kotlin
    std::atomic<float> currentRms{0.0f};
    std::atomic<int> currentZcr{0};
    std::atomic<int> currentState{0};
    std::atomic<int> tramasVocales{0};
    std::atomic<int> tramasBloqueo{0};
    std::atomic<float> ataqueBrusco{0.0f};
    std::atomic<float> currentHfRatio{0.0f};
    std::atomic<float> currentSpectralFlux{0.0f};

    // Configuración DAF / FAF
    std::atomic<int> delayMs{100};
    std::atomic<float> pitchOctaves{0.0f};
    std::atomic<float> fafPhaseAccum{0.0f};
    std::atomic<float> gateEnvelope{0.0f};
    std::atomic<float> outputGainSmooth{2.3f};

    // Variables internas del detector tiempo-frecuencia
    float noiseFloor = 0.005f;
    float agcTargetGain = 2.3f;
    int framesSilencio = 0;
    float historiaRms[HISTORIA_TAMANO] = {};
    float historiaFlux[HISTORIA_TAMANO] = {};
    int framesTensionSostenida = 0;
    int picosClonicosRecientes = 0;
    int framesDesdeUltimoPico = 0;
    bool enPicoSilabico = false;

    // Filtros entrada
    float hpYPrev = 0.0f;
    float hpXPrev = 0.0f;
    float lpYPrev = 0.0f;

    // Búferes para FFT (512 muestras)
    float fftRingBuffer[FFT_SIZE] = {};
    int fftWritePos = 0;
    int fftSamplesAccum = 0;
    float hannWindow[FFT_SIZE] = {};
    float prevMagnitudes[FFT_SIZE / 2] = {};
    bool hannInitialized = false;

    // Búfer Circular Lock-Free para DAF / FAF
    std::vector<float> dafBuffer;
    std::atomic<int> writeIndex{0};
    std::atomic<bool> audioReadyFlag{false};
    int sampleRateGlobal = 48000;

    // Seguimiento continuo de lectura para eliminar jitter robótico entre captura y salida
    double continuousReadPos = -1.0;

    std::shared_ptr<oboe::AudioStream> inputStream;
    std::shared_ptr<oboe::AudioStream> outputStream;
    std::mutex streamMutex;

    void initHannWindow() {
        if (hannInitialized) return;
        for (int i = 0; i < FFT_SIZE; ++i) {
            hannWindow[i] = 0.5f * (1.0f - std::cos((2.0f * PI * i) / (FFT_SIZE - 1)));
        }
        hannInitialized = true;
    }

    void computeFFT(std::complex<float>* x, int N) {
        int j = 0;
        for (int i = 0; i < N; ++i) {
            if (i < j) std::swap(x[i], x[j]);
            int m = N >> 1;
            while (m >= 1 && j >= m) {
                j -= m;
                m >>= 1;
            }
            j += m;
        }
        for (int s = 1; s <= static_cast<int>(std::log2(N)); ++s) {
            int m = 1 << s;
            int halfM = m >> 1;
            std::complex<float> wm(std::cos(-2.0f * PI / m), std::sin(-2.0f * PI / m));
            for (int k = 0; k < N; k += m) {
                std::complex<float> w(1.0f, 0.0f);
                for (int j2 = 0; j2 < halfM; ++j2) {
                    std::complex<float> t = w * x[k + j2 + halfM];
                    std::complex<float> u = x[k + j2];
                    x[k + j2] = u + t;
                    x[k + j2 + halfM] = u - t;
                    w *= wm;
                }
            }
        }
    }

    void analizarEspectroFFT(
            int sampleRate,
            float& outHfRatio,
            float& outSpectralFlux,
            float& outSpectralFlatness,
            float& outVoiceBandRatio
    ) {
        std::complex<float> fftData[FFT_SIZE];
        for (int i = 0; i < FFT_SIZE; ++i) {
            int idx = (fftWritePos + i) % FFT_SIZE;
            fftData[i] = std::complex<float>(fftRingBuffer[idx] * hannWindow[i], 0.0f);
        }

        computeFFT(fftData, FFT_SIZE);

        const int halfN = FFT_SIZE / 2;
        const float binHz = static_cast<float>(sampleRate) / static_cast<float>(FFT_SIZE);

        const int binVoiceCoreStart = std::max(1, static_cast<int>(110.0f / binHz));
        const int binVoiceCoreEnd = std::min(halfN - 1, static_cast<int>(1200.0f / binHz));

        const int binLowStart = std::max(1, static_cast<int>(100.0f / binHz));
        const int binLowEnd = std::min(halfN - 1, static_cast<int>(2400.0f / binHz));
        const int binHighStart = std::min(halfN - 1, static_cast<int>(2600.0f / binHz));
        const int binHighEnd = std::min(halfN - 1, static_cast<int>(7500.0f / binHz));

        float energiaNucleoVoz = 0.0f;
        float energiaTotalEspectro = 1e-6f;
        float energiaBaja = 1e-6f;
        float energiaAlta = 0.0f;
        float flux = 0.0f;

        const int flatStart = std::max(1, static_cast<int>(150.0f / binHz));
        const int flatEnd = std::min(halfN - 1, static_cast<int>(6500.0f / binHz));
        double sumLogPower = 0.0;
        double sumPower = 0.0;
        int flatBins = 0;

        for (int k = 1; k < halfN; ++k) {
            float mag = std::abs(fftData[k]);
            float power = mag * mag + 1e-10f;
            energiaTotalEspectro += power;

            float diff = mag - prevMagnitudes[k];
            if (diff > 0.0f) {
                flux += diff;
            }
            prevMagnitudes[k] = mag;

            if (k >= binVoiceCoreStart && k <= binVoiceCoreEnd) {
                energiaNucleoVoz += power;
            }

            if (k >= binLowStart && k <= binLowEnd) {
                energiaBaja += power;
            } else if (k >= binHighStart && k <= binHighEnd) {
                energiaAlta += power;
            }

            if (k >= flatStart && k <= flatEnd) {
                sumLogPower += std::log(static_cast<double>(power));
                sumPower += static_cast<double>(power);
                flatBins++;
            }
        }

        outHfRatio = energiaAlta / (energiaBaja + energiaAlta);
        outSpectralFlux = flux / static_cast<float>(halfN);
        outVoiceBandRatio = energiaNucleoVoz / energiaTotalEspectro;

        if (flatBins > 0 && sumPower > 1e-9) {
            double geoMean = std::exp(sumLogPower / flatBins);
            double arithMean = sumPower / flatBins;
            outSpectralFlatness = static_cast<float>(geoMean / arithMean);
        } else {
            outSpectralFlatness = 1.0f;
        }
    }

    inline float softLimitTransparent(float x) {
        const float absX = std::abs(x);
        if (absX <= 0.85f) return x;
        const float sign = (x >= 0.0f) ? 1.0f : -1.0f;
        const float excess = absX - 0.85f;
        return sign * (0.85f + 0.14f * (excess / (excess + 0.25f)));
    }

    void reiniciarDetector() {
        initHannWindow();
        noiseFloor = 0.005f;
        agcTargetGain = 2.3f;
        framesSilencio = 0;
        continuousReadPos = -1.0;
        std::fill(std::begin(historiaRms), std::end(historiaRms), 0.0f);
        std::fill(std::begin(historiaFlux), std::end(historiaFlux), 0.0f);
        std::fill(std::begin(fftRingBuffer), std::end(fftRingBuffer), 0.0f);
        std::fill(std::begin(prevMagnitudes), std::end(prevMagnitudes), 0.0f);
        fftWritePos = 0;
        fftSamplesAccum = 0;
        framesTensionSostenida = 0;
        picosClonicosRecientes = 0;
        framesDesdeUltimoPico = 0;
        enPicoSilabico = false;
        hpYPrev = 0.0f;
        hpXPrev = 0.0f;
        lpYPrev = 0.0f;
        gateEnvelope.store(0.0f, std::memory_order_relaxed);
        outputGainSmooth.store(2.3f, std::memory_order_relaxed);
    }

    void reiniciarMetricas() {
        currentRms.store(0.0f);
        currentZcr.store(0);
        currentState.store(0);
        tramasVocales.store(0);
        tramasBloqueo.store(0);
        ataqueBrusco.store(0.0f);
        currentHfRatio.store(0.0f);
        currentSpectralFlux.store(0.0f);
    }

    // Callback de Entrada (Micrófono)
    class DspInputCallback : public oboe::AudioStreamDataCallback {
    public:
        oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData, int32_t numFrames) override {
            if (audioData == nullptr || numFrames <= 0) return oboe::DataCallbackResult::Continue;
            const float* input = static_cast<const float*>(audioData);

            const int sr = stream->getSampleRate();
            const float dt = 1.0f / static_cast<float>(sr);

            // Pasa-altas 85 Hz para voz natural cálida sin retumbo
            const float rcHigh = 1.0f / (2.0f * PI * 85.0f);
            const float alphaHigh = rcHigh / (rcHigh + dt);
            // Pasa-bajas 11 kHz SOLO para la rama de análisis DSP
            const float freqLow = std::min(11000.0f, static_cast<float>(sr) * 0.46f);
            const float rcLow = 1.0f / (2.0f * PI * freqLow);
            const float alphaLow = dt / (rcLow + dt);

            double sumaCuadrados = 0.0;
            float picoAbsoluto = 0.0f;
            int crucesCero = 0;
            float muestraAnterior = hpYPrev;

            int currentWrite = writeIndex.load(std::memory_order_relaxed);
            int bufferSize = static_cast<int>(dafBuffer.size());

            for (int i = 0; i < numFrames; ++i) {
                float muestra = std::clamp(input[i], -1.0f, 1.0f);
                const float hpY = alphaHigh * (hpYPrev + muestra - hpXPrev);
                hpXPrev = muestra;
                hpYPrev = hpY;

                // En el búfer DAF guardamos hpY con todo el brillo y naturalidad vocal
                if (bufferSize > 0) {
                    dafBuffer[currentWrite] = hpY;
                    currentWrite = (currentWrite + 1) % bufferSize;
                }

                // Rama de análisis espectral
                const float lpY = lpYPrev + alphaLow * (hpY - lpYPrev);
                lpYPrev = lpY;

                const float absY = std::abs(lpY);
                if (absY > picoAbsoluto) picoAbsoluto = absY;
                sumaCuadrados += static_cast<double>(lpY) * lpY;

                if ((muestraAnterior >= 0.0f && lpY < 0.0f) || (muestraAnterior < 0.0f && lpY >= 0.0f)) {
                    crucesCero++;
                }
                muestraAnterior = lpY;

                fftRingBuffer[fftWritePos] = lpY;
                fftWritePos = (fftWritePos + 1) % FFT_SIZE;
                fftSamplesAccum++;
            }

            if (bufferSize > 0) {
                writeIndex.store(currentWrite, std::memory_order_release);
                audioReadyFlag.store(true, std::memory_order_release);
            }

            static float spectralFlatness = 1.0f;
            static float voiceBandRatio = 0.0f;
            float hfRatio = currentHfRatio.load(std::memory_order_relaxed);
            float spectralFlux = currentSpectralFlux.load(std::memory_order_relaxed);

            if (fftSamplesAccum >= (FFT_SIZE / 2)) {
                analizarEspectroFFT(sr, hfRatio, spectralFlux, spectralFlatness, voiceBandRatio);
                fftSamplesAccum = 0;
                currentHfRatio.store(hfRatio, std::memory_order_relaxed);
                currentSpectralFlux.store(spectralFlux, std::memory_order_relaxed);
            }

            const float rms = static_cast<float>(std::sqrt(sumaCuadrados / numFrames));
            const float tasaZcr = static_cast<float>(crucesCero) / static_cast<float>(numFrames);
            const float crestFactor = (rms > 1e-6f) ? (picoAbsoluto / rms) : 0.0f;

            const bool esFondoNoVocal =
                    (spectralFlatness > 0.45f) ||
                    (voiceBandRatio < 0.32f && crestFactor < 2.4f);

            if (esFondoNoVocal || rms < noiseFloor * 2.5f) {
                noiseFloor = noiseFloor * 0.90f + rms * 0.10f;
            }
            noiseFloor = std::clamp(noiseFloor, 0.002f, 0.06f);

            const float rmsNeto = std::max(0.0f, rms - noiseFloor * 1.35f);
            const float rmsEscalado = rmsNeto * 20000.0f;

            const float umbralVozCercana = std::max(noiseFloor * 1.85f, 0.007f);
            const bool tieneFirmaVocal = (!esFondoNoVocal) || (crestFactor > 3.1f && rms > noiseFloor * 2.2f);
            const bool vozActiva = tieneFirmaVocal && (rms > umbralVozCercana) && (rmsEscalado > 85.0f);

            // AGC muy lento y estable (0.97 / 0.03) para que la ganancia no fluctúe dentro de una palabra
            if (vozActiva && rms > 0.008f) {
                float desired = std::clamp(0.22f / rms, 1.6f, 3.6f);
                agcTargetGain = agcTargetGain * 0.97f + desired * 0.03f;
                outputGainSmooth.store(agcTargetGain, std::memory_order_relaxed);
            }

            float env = gateEnvelope.load(std::memory_order_relaxed);
            if (vozActiva) {
                env = std::min(1.0f, env + 0.65f);
                framesSilencio = 0;
            } else {
                framesSilencio++;
                if (framesSilencio > 14) {
                    env = std::max(0.0f, env - 0.10f);
                }
            }
            gateEnvelope.store(env, std::memory_order_relaxed);

            for (int i = HISTORIA_TAMANO - 1; i > 0; --i) {
                historiaRms[i] = historiaRms[i - 1];
                historiaFlux[i] = historiaFlux[i - 1];
            }
            historiaRms[0] = rmsEscalado;
            historiaFlux[0] = spectralFlux;

            int estado = 0;
            if (vozActiva) {
                framesDesdeUltimoPico++;

                bool pulsoSilabico = (spectralFlux > 0.28f) && (historiaRms[0] > historiaRms[2] * 1.25f) && (historiaRms[0] > 95.0f);
                if (pulsoSilabico && !enPicoSilabico) {
                    if (framesDesdeUltimoPico >= 3 && framesDesdeUltimoPico <= 22) {
                        picosClonicosRecientes++;
                    } else {
                        picosClonicosRecientes = 1;
                    }
                    framesDesdeUltimoPico = 0;
                    enPicoSilabico = true;
                } else if (historiaRms[0] < historiaRms[1] * 0.88f) {
                    enPicoSilabico = false;
                }

                if (framesDesdeUltimoPico > 25) {
                    picosClonicosRecientes = 0;
                }

                const bool esClonico = (picosClonicosRecientes >= 2);

                const float variacionEnvolvente = std::abs(historiaRms[0] - historiaRms[4]);
                const bool tensionEspectral = (hfRatio > 0.26f) && (rmsEscalado > 110.0f);
                const bool esfuerzoEstatico = (variacionEnvolvente < 45.0f) && (rmsEscalado > 145.0f) && (hfRatio > 0.16f);

                if (esClonico) {
                    estado = 3;
                    framesTensionSostenida = 0;
                } else if (tensionEspectral || esfuerzoEstatico) {
                    framesTensionSostenida++;
                    estado = (framesTensionSostenida >= 4) ? 2 : 1;
                } else {
                    estado = 1;
                    framesTensionSostenida = 0;
                }
            } else {
                estado = 0;
                framesTensionSostenida = 0;
                picosClonicosRecientes = 0;
                framesDesdeUltimoPico = 0;
                enPicoSilabico = false;
            }

            if (estado != 0) tramasVocales.fetch_add(1, std::memory_order_relaxed);
            if (estado == 2 || estado == 3) tramasBloqueo.fetch_add(1, std::memory_order_relaxed);

            const float ataque = std::max(0.0f, rmsEscalado - currentRms.load(std::memory_order_relaxed));

            currentRms.store(rmsEscalado, std::memory_order_relaxed);
            currentZcr.store(static_cast<int>(tasaZcr * 100.0f), std::memory_order_relaxed);
            currentState.store(estado, std::memory_order_relaxed);
            ataqueBrusco.store(ataque, std::memory_order_relaxed);

            return oboe::DataCallbackResult::Continue;
        }
    };

    // Callback de Salida (Audífonos DAF / FAF con lectura continua libre de jitter)
    class DspOutputCallback : public oboe::AudioStreamDataCallback {
    public:
        oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData, int32_t numFrames) override {
            float* output = static_cast<float*>(audioData);
            int bufferSize = static_cast<int>(dafBuffer.size());

            if (bufferSize == 0 || !audioReadyFlag.load(std::memory_order_acquire)) {
                std::fill_n(output, numFrames, 0.0f);
                return oboe::DataCallbackResult::Continue;
            }

            float targetEnv = gateEnvelope.load(std::memory_order_relaxed);
            static float envSmooth = 0.0f;
            static float gainSmooth = 2.3f;

            int currentWriteIndex = writeIndex.load(std::memory_order_acquire);
            float pitch = pitchOctaves.load(std::memory_order_relaxed);
            float targetGain = outputGainSmooth.load(std::memory_order_relaxed);

            int requestedDelayFrames = static_cast<int>((delayMs.load(std::memory_order_relaxed) / 1000.0f) * sampleRateGlobal);
            int baseDelayFrames = std::max(requestedDelayFrames, numFrames * 2);

            // --- 1. MODO DAF PURO CON SEGUIMIENTO CONTINUO (CERO SALTOS DE FASE) ---
            if (std::abs(pitch) < 0.01f) {
                double desiredStart = static_cast<double>(currentWriteIndex - numFrames - baseDelayFrames);
                while (desiredStart < 0.0) desiredStart += bufferSize;
                desiredStart = std::fmod(desiredStart, static_cast<double>(bufferSize));

                if (continuousReadPos < 0.0) {
                    continuousReadPos = desiredStart;
                }

                // Calculamos diferencia circular entre posición continua y retardo deseado
                double diff = desiredStart - continuousReadPos;
                if (diff > bufferSize * 0.5) diff -= bufferSize;
                if (diff < -bufferSize * 0.5) diff += bufferSize;

                // Si el usuario movió drásticamente el slider (> 20 ms), reajustamos;
                // si es solo jitter del reloj de Android (< 20 ms), compensamos microscópicamente sin saltos
                double step = 1.0;
                if (std::abs(diff) > sampleRateGlobal * 0.025) {
                    continuousReadPos = desiredStart;
                } else {
                    step = 1.0 + std::clamp(diff * 0.0005, -0.004, 0.004);
                }

                if (targetEnv <= 0.001f && envSmooth <= 0.001f) {
                    envSmooth = 0.0f;
                    continuousReadPos = std::fmod(continuousReadPos + step * numFrames, static_cast<double>(bufferSize));
                    if (continuousReadPos < 0.0) continuousReadPos += bufferSize;
                    std::fill_n(output, numFrames, 0.0f);
                    return oboe::DataCallbackResult::Continue;
                }

                for (int i = 0; i < numFrames; ++i) {
                    envSmooth += (targetEnv - envSmooth) * 0.003f;
                    gainSmooth += (targetGain - gainSmooth) * 0.001f;

                    int idx1 = static_cast<int>(continuousReadPos) % bufferSize;
                    int idx2 = (idx1 + 1) % bufferSize;
                    float frac = static_cast<float>(continuousReadPos - std::floor(continuousReadPos));

                    float rawSample = dafBuffer[idx1] * (1.0f - frac) + dafBuffer[idx2] * frac;
                    output[i] = softLimitTransparent(rawSample * gainSmooth * envSmooth);

                    continuousReadPos += step;
                    if (continuousReadPos >= bufferSize) continuousReadPos -= bufferSize;
                    if (continuousReadPos < 0.0) continuousReadPos += bufferSize;
                }
                return oboe::DataCallbackResult::Continue;
            }

            // Resetear puntero continuo para cuando regrese a DAF puro
            continuousReadPos = -1.0;

            if (targetEnv <= 0.001f && envSmooth <= 0.001f) {
                envSmooth = 0.0f;
                std::fill_n(output, numFrames, 0.0f);
                return oboe::DataCallbackResult::Continue;
            }

            // --- 2. MODO FAF + DAF ---
            float ratio = std::pow(2.0f, pitch);
            float rate = 1.0f - ratio;
            float windowFrames = (65.0f / 1000.0f) * sampleRateGlobal;
            float phase = fafPhaseAccum.load(std::memory_order_relaxed);

            for (int i = 0; i < numFrames; ++i) {
                envSmooth += (targetEnv - envSmooth) * 0.003f;
                gainSmooth += (targetGain - gainSmooth) * 0.001f;

                phase += rate;
                if (phase >= windowFrames) phase -= windowFrames;
                if (phase < 0.0f) phase += windowFrames;

                float x = phase / windowFrames;
                // Ventana coseno alzado (Hann) en lugar de triangular para crossfade sin modulación robótica
                float w1 = 0.5f * (1.0f - std::cos(2.0f * PI * x));
                float w2 = 1.0f - w1;

                float phase2 = phase + windowFrames * 0.5f;
                if (phase2 >= windowFrames) phase2 -= windowFrames;

                float delay1 = baseDelayFrames + phase;
                float delay2 = baseDelayFrames + phase2;

                auto readFrac = [&](float dFrames) -> float {
                    float exactIdx = static_cast<float>(currentWriteIndex - numFrames + i) - dFrames;
                    while (exactIdx < 0.0f) exactIdx += bufferSize;
                    while (exactIdx >= bufferSize) exactIdx -= bufferSize;

                    int idx1 = static_cast<int>(exactIdx) % bufferSize;
                    int idx2 = (idx1 + 1) % bufferSize;
                    float frac = exactIdx - idx1;

                    return dafBuffer[idx1] * (1.0f - frac) + dafBuffer[idx2] * frac;
                };

                float mixed = (readFrac(delay1) * w1 + readFrac(delay2) * w2) * gainSmooth * envSmooth;
                output[i] = softLimitTransparent(mixed);
            }

            fafPhaseAccum.store(phase, std::memory_order_relaxed);
            return oboe::DataCallbackResult::Continue;
        }
    };

    DspInputCallback inputCallback;
    DspOutputCallback outputCallback;

    void cerrarStreamsSinLock() {
        audioReadyFlag.store(false, std::memory_order_release);
        if (inputStream != nullptr) {
            inputStream->requestStop();
            inputStream->close();
            inputStream.reset();
        }
        if (outputStream != nullptr) {
            outputStream->requestStop();
            outputStream->close();
            outputStream.reset();
        }
    }

    bool iniciarOboeInterno() {
        std::lock_guard<std::mutex> lock(streamMutex);
        if (inputStream != nullptr || outputStream != nullptr) return false;

        reiniciarDetector();

        oboe::AudioStreamBuilder outBuilder;
        outBuilder.setDirection(oboe::Direction::Output);
        outBuilder.setPerformanceMode(oboe::PerformanceMode::None);
        outBuilder.setSharingMode(oboe::SharingMode::Shared);
        outBuilder.setUsage(oboe::Usage::Media);
        outBuilder.setContentType(oboe::ContentType::Speech);
        outBuilder.setFormat(oboe::AudioFormat::Float);
        outBuilder.setChannelCount(1);
        outBuilder.setDataCallback(&outputCallback);

        if (outBuilder.openStream(outputStream) != oboe::Result::OK) {
            outputStream.reset();
            return false;
        }

        int32_t framesPerBurst = outputStream->getFramesPerBurst();
        if (framesPerBurst > 0) {
            outputStream->setBufferSizeInFrames(framesPerBurst * 2);
        }

        sampleRateGlobal = outputStream->getSampleRate();
        dafBuffer.resize(sampleRateGlobal * 2, 0.0f);
        std::fill(dafBuffer.begin(), dafBuffer.end(), 0.0f);
        writeIndex.store(0, std::memory_order_release);
        fafPhaseAccum.store(0.0f, std::memory_order_relaxed);

        oboe::AudioStreamBuilder inBuilder;
        inBuilder.setDirection(oboe::Direction::Input);
        inBuilder.setPerformanceMode(oboe::PerformanceMode::None);
        inBuilder.setSharingMode(oboe::SharingMode::Shared);
        inBuilder.setFormat(oboe::AudioFormat::Float);
        inBuilder.setChannelCount(1);
        inBuilder.setSampleRate(sampleRateGlobal);
        inBuilder.setInputPreset(oboe::InputPreset::VoiceRecognition);
        inBuilder.setDataCallback(&inputCallback);

        if (inBuilder.openStream(inputStream) != oboe::Result::OK) {
            inBuilder.setInputPreset(oboe::InputPreset::Unprocessed);
            if (inBuilder.openStream(inputStream) != oboe::Result::OK) {
                cerrarStreamsSinLock();
                return false;
            }
        }

        if (outputStream->requestStart() != oboe::Result::OK || inputStream->requestStart() != oboe::Result::OK) {
            cerrarStreamsSinLock();
            return false;
        }
        return true;
    }

    void detenerOboeInterno() {
        std::lock_guard<std::mutex> lock(streamMutex);
        cerrarStreamsSinLock();
    }
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_itsx_speaktutor_logic_MotorAudioDSP_startOboe(JNIEnv* env, jobject thiz) {
    return iniciarOboeInterno() ? JNI_TRUE : JNI_FALSE;
}
extern "C" JNIEXPORT void JNICALL Java_com_itsx_speaktutor_logic_MotorAudioDSP_stopOboe(JNIEnv* env, jobject thiz) {
    detenerOboeInterno();
}
extern "C" JNIEXPORT void JNICALL Java_com_itsx_speaktutor_logic_MotorAudioDSP_resetOboeMetrics(JNIEnv* env, jobject thiz) {
    reiniciarMetricas();
}
extern "C" JNIEXPORT jfloatArray JNICALL Java_com_itsx_speaktutor_logic_MotorAudioDSP_getMetrics(JNIEnv* env, jobject thiz) {
    const jfloat valores[TAMANO_METRICAS] = {
            currentRms.load(std::memory_order_relaxed),
            static_cast<float>(currentZcr.load(std::memory_order_relaxed)),
            static_cast<float>(currentState.load(std::memory_order_relaxed)),
            static_cast<float>(tramasVocales.load(std::memory_order_relaxed)),
            static_cast<float>(tramasBloqueo.load(std::memory_order_relaxed)),
            ataqueBrusco.load(std::memory_order_relaxed),
            currentHfRatio.load(std::memory_order_relaxed),
            currentSpectralFlux.load(std::memory_order_relaxed)
    };
    jfloatArray resultado = env->NewFloatArray(TAMANO_METRICAS);
    if (resultado != nullptr) env->SetFloatArrayRegion(resultado, 0, TAMANO_METRICAS, valores);
    return resultado;
}
extern "C" JNIEXPORT void JNICALL Java_com_itsx_speaktutor_logic_MotorAudioDSP_setDAFDelay(JNIEnv* env, jobject thiz, jint delay) {
    delayMs.store(std::clamp(static_cast<int>(delay), 30, 1000), std::memory_order_relaxed);
}
extern "C" JNIEXPORT void JNICALL Java_com_itsx_speaktutor_logic_MotorAudioDSP_setFAFPitch(JNIEnv* env, jobject thiz, jfloat pitch) {
    pitchOctaves.store(std::clamp(static_cast<float>(pitch), -1.2f, 0.8f), std::memory_order_relaxed);
}