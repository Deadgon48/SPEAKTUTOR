
# -*- coding: utf-8 -*-
"""
Generador del Capítulo V – Desarrollo del proyecto
Aplicación SPEAKTUTOR
"""

from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
import os

OUTPUT_PATH = r"C:\Users\h4k48\AndroidStudioProjects\SPEAKTUTOR\CapituloV_Desarrollo.docx"

doc = Document()

# ─────────────────────────────────────────────────────────────────────────────
# HELPERS
# ─────────────────────────────────────────────────────────────────────────────

def set_heading(doc, text, level=1, center=False):
    h = doc.add_heading(text, level=level)
    h.style.font.color.rgb = RGBColor(0, 0, 0)
    if center:
        h.alignment = WD_ALIGN_PARAGRAPH.CENTER
    else:
        h.alignment = WD_ALIGN_PARAGRAPH.LEFT
    return h

def add_paragraph(doc, text, justify=True):
    p = doc.add_paragraph(text)
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY if justify else WD_ALIGN_PARAGRAPH.LEFT
    p.style.font.size = Pt(12)
    return p

def add_code_inline(paragraph, text):
    """Add inline monospace text (file path style) to an existing paragraph."""
    run = paragraph.add_run(text)
    run.font.name = "Courier New"
    run.font.size = Pt(9)
    return run

def add_mixed_paragraph(doc, parts, justify=True):
    """
    parts: list of (text, is_code)
    """
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY if justify else WD_ALIGN_PARAGRAPH.LEFT
    for text, is_code in parts:
        run = p.add_run(text)
        if is_code:
            run.font.name = "Courier New"
            run.font.size = Pt(9)
        else:
            run.font.size = Pt(12)
    return p

def add_bullet(doc, text):
    p = doc.add_paragraph(text, style="List Bullet")
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    return p

# ─────────────────────────────────────────────────────────────────────────────
# TÍTULO PRINCIPAL
# ─────────────────────────────────────────────────────────────────────────────

title = doc.add_heading("Capítulo V. Desarrollo del proyecto", level=1)
title.alignment = WD_ALIGN_PARAGRAPH.CENTER
for run in title.runs:
    run.bold = True
    run.font.color.rgb = RGBColor(0, 0, 0)

doc.add_paragraph()

# ─────────────────────────────────────────────────────────────────────────────
# 1. CONFIGURACIÓN DEL ENTORNO
# ─────────────────────────────────────────────────────────────────────────────

set_heading(doc, "1. Configuración del entorno y estructura base del proyecto", level=2)
add_paragraph(doc,
    "El desarrollo del proyecto SPEAKTUTOR se inició el 26 de agosto de 2026 mediante la "
    "creación del repositorio en GitHub y la configuración del entorno de desarrollo en Android Studio. "
    "En esta etapa se estableció la arquitectura base de la aplicación, se definieron las dependencias "
    "del proyecto y se crearon los archivos de configuración necesarios para garantizar la compilación "
    "correcta del código fuente."
)

set_heading(doc, "1.1 Inicialización de Gradle y dependencias", level=3)
add_paragraph(doc,
    "La configuración del sistema de compilación se realizó mediante Kotlin DSL (Domain Specific Language), "
    "sustituyendo la sintaxis Groovy tradicional por archivos build.gradle.kts. Se definió el archivo "
    "settings.gradle.kts para establecer el nombre del proyecto y los repositorios de dependencias. "
    "En el archivo build.gradle.kts del módulo :app se declararon las siguientes dependencias principales:"
)
for dep in [
    "Jetpack Compose BOM (Bill of Materials) para la gestión de versiones de los componentes UI.",
    "Navigation Compose para la navegación entre pantallas mediante NavHost.",
    "Lifecycle ViewModel Compose para la integración del patrón MVVM.",
    "CameraX para funcionalidades de captura de video y audio.",
    "Media3 ExoPlayer para la reproducción de contenido multimedia.",
    "iText7 para la generación de reportes en formato PDF.",
    "Kotlin Coroutines para la programación asíncrona.",
    "JUnit y Espresso para las pruebas unitarias e instrumentadas.",
]:
    add_bullet(doc, dep)

set_heading(doc, "1.2 Archivo principal: MainActivity.kt, TarjetasShaderScreen.kt y ComponentesComunes.kt", level=3)
add_mixed_paragraph(doc, [
    ("El punto de entrada de la aplicación se implementó en el archivo ", False),
    ("app/src/main/java/com/h4k48/speaktutor/ui/screens/MainActivity.kt", True),
    (". En este archivo se configuró el NavHost de Jetpack Compose Navigation, registrando todas las rutas "
     "de navegación disponibles en la aplicación. Cada ruta corresponde a una pantalla funcional del sistema, "
     "y la navegación se gestiona de manera declarativa mediante la clase NavController.", False),
])
add_mixed_paragraph(doc, [
    ("La pantalla de inicio se implementó en ", False),
    ("TarjetasShaderScreen.kt", True),
    (", que presenta al usuario un menú principal compuesto por tarjetas interactivas con efecto visual shader. "
     "Cada tarjeta dirige al usuario hacia uno de los módulos principales de la aplicación: Metrónomo, "
     "Habla Estirada, Ritmo y Fluidez, Simulación de Situaciones, Pronunciación al Instante, Progreso, "
     "Ejercicios Adaptativos, Biofeedback, Tarjetas de Articulación, Conciencia Fonológica y Reporte Multidimensional.", False),
])
add_mixed_paragraph(doc, [
    ("Los componentes reutilizables de la interfaz se concentraron en ", False),
    ("ComponentesComunes.kt", True),
    (". Este archivo define los siguientes composables compartidos entre múltiples pantallas:", False),
])
for comp in [
    "ControlVelocidadTts: control deslizante para ajustar la velocidad del sintetizador de voz (TTS).",
    "GrabadoraProgresoComponent: componente de grabación de audio con indicador de nivel.",
    "MetronomoReutilizableComponent: metrónomo embebible configurable en BPM.",
    "BarraNavegacionModulos: barra inferior de navegación entre módulos.",
    "MiniTarjetaBoton: tarjeta compacta de acceso rápido.",
]:
    add_bullet(doc, comp)

set_heading(doc, "1.3 Pantallas base: MetronomoScreen.kt, HablaEstiradaScreen.kt y RitmoFluidezScreen.kt", level=3)
add_mixed_paragraph(doc, [
    ("El módulo de Metrónomo, implementado en ", False),
    ("MetronomoScreen.kt", True),
    (", permite al usuario ajustar el tempo entre 40 y 200 BPM mediante un control deslizante. "
     "El metrónomo emite pulsos sonoros a través de la API AudioTrack de Android, sincronizando el ritmo del habla "
     "con el pulso auditivo. Se incluyó también un indicador visual de pulso que parpadea en sincronía con el audio.", False),
])
add_mixed_paragraph(doc, [
    ("La pantalla de Habla Estirada, en ", False),
    ("HablaEstiradaScreen.kt", True),
    (", organiza palabras clasificadas por categoría fonética en un archivo JSON "
     "ubicado en ", False),
    ("assets/HablaEstirada/palabras_habla_estirada.json", True),
    (". La pantalla presenta cada palabra al usuario junto con la indicación de prolongar "
     "las vocales, apoyándose en el sintetizador de voz (TTS) para la reproducción guiada.", False),
])
add_mixed_paragraph(doc, [
    ("La pantalla de Ritmo y Fluidez, en ", False),
    ("RitmoFluidezScreen.kt", True),
    (", integra tres modos de ejercitación: bloques de ritmo por nivel (principiante, intermedio y avanzado), "
     "lectura guiada con texto extenso y grabadora de progreso. Los bloques de ritmo se cargan desde los archivos ", False),
    ("assets/RitmoFluidez/bloques_principiante.json", True),
    (", ", False),
    ("bloques_intermedio.json", True),
    (", ", False),
    ("bloques_avanzado.json", True),
    (" y ", False),
    ("lecturas_extensas.json", True),
    (".", False),
])

# ─────────────────────────────────────────────────────────────────────────────
# 2. IMPLEMENTACIÓN DE MÓDULOS BASE
# ─────────────────────────────────────────────────────────────────────────────

set_heading(doc, "2. Implementación de módulos base (commits: logo, Pronunciacion-al-instante — 26 y 27 de agosto de 2026)", level=2)
add_paragraph(doc,
    "En los commits realizados el 26 y 27 de agosto de 2026 se incorporaron elementos visuales de identidad de "
    "la aplicación y el primer módulo funcional de reconocimiento de voz."
)

set_heading(doc, "2.1 Integración del logo y configuración de dependabot", level=3)
add_paragraph(doc,
    "Se añadió el recurso gráfico del logotipo de la aplicación en la carpeta de recursos de drawables. "
    "Paralelamente, se creó la carpeta .github con el archivo dependabot.yaml para automatizar la detección "
    "de actualizaciones de dependencias en el repositorio de GitHub."
)

set_heading(doc, "2.2 Módulo de Pronunciación al Instante: PronunciacionInstanteScreen.kt", level=3)
add_mixed_paragraph(doc, [
    ("El módulo de Pronunciación al Instante se implementó en el archivo ", False),
    ("app/src/main/java/com/h4k48/speaktutor/ui/screens/PronunciacionInstanteScreen.kt", True),
    (". Este módulo presenta al usuario un juego de pronunciación estructurado en tres niveles de dificultad: "
     "Fácil, Medio y Difícil. Las palabras de cada nivel se cargan desde los archivos JSON alojados en ", False),
    ("assets/PronunciacionInstante/palabras_faciles.json", True),
    (", ", False),
    ("palabras_medias.json", True),
    (" y ", False),
    ("palabras_dificiles.json", True),
    (".", False),
])
add_paragraph(doc,
    "El reconocimiento de voz se implementó mediante la API SpeechRecognizer de Android, que transcribe "
    "en tiempo real la pronunciación del usuario. El sistema compara fonéticamente la transcripción obtenida "
    "con la palabra objetivo utilizando distancia de Levenshtein normalizada. El resultado es mostrado "
    "al usuario como porcentaje de similitud fonética, acompañado de retroalimentación visual positiva o "
    "de corrección según el umbral de aceptación configurado."
)

# ─────────────────────────────────────────────────────────────────────────────
# 3. MÓDULO DE PROGRESO
# ─────────────────────────────────────────────────────────────────────────────

set_heading(doc, "3. Módulo de Progreso clínico (commit: Progreso — 3 de septiembre de 2026)", level=2)
add_mixed_paragraph(doc, [
    ("El 3 de septiembre de 2026 se implementó el módulo de seguimiento del progreso clínico del usuario, "
     "concentrado en el archivo ", False),
    ("app/src/main/java/com/h4k48/speaktutor/ui/screens/ProgresoScreen.kt", True),
    (".", False),
])
add_paragraph(doc,
    "La pantalla de progreso organiza el historial de sesiones del usuario en secciones temáticas: "
    "precisión fonética por categoría, fluencia (palabras por minuto), control de ritmo (desviación del BPM objetivo) "
    "y consistencia de sesiones. Los datos se almacenan localmente mediante SharedPreferences y se proyectan "
    "visualmente en la pantalla mediante gráficas de barras implementadas con Canvas de Jetpack Compose."
)
add_paragraph(doc,
    "Se implementó la funcionalidad de exportación del historial clínico en formato PDF utilizando la librería "
    "iText7. El documento generado incluye encabezado con datos del usuario, fecha de generación, tablas de "
    "resultados por sesión y observaciones automáticas basadas en tendencias detectadas. El archivo PDF se "
    "almacena en el directorio de descargas del dispositivo y puede compartirse mediante la API de compartición "
    "de Android (Intent.ACTION_SEND)."
)
add_paragraph(doc,
    "Adicionalmente, se actualizaron los menús internos de navegación para incluir acceso directo a la "
    "pantalla de progreso desde la barra inferior de navegación."
)

# ─────────────────────────────────────────────────────────────────────────────
# 4. MOTOR ADAPTATIVO Y EJERCICIOS ADAPTATIVOS
# ─────────────────────────────────────────────────────────────────────────────

set_heading(doc, "4. Motor Adaptativo y Ejercicios Adaptativos (commits: Ejercicios_Personalizado y Cambios-y-mejoras — septiembre 2026)", level=2)
add_paragraph(doc,
    "Durante septiembre de 2026 se desarrollaron dos componentes centrales que dotan a la aplicación de "
    "capacidad de personalización clínica: el motor de inteligencia adaptativa y la pantalla de ejercicios generados dinámicamente."
)

set_heading(doc, "4.1 Motor Adaptativo: MotorAdaptativo.kt", level=3)
add_mixed_paragraph(doc, [
    ("El motor de inteligencia adaptativa se implementó en ", False),
    ("app/src/main/java/com/h4k48/speaktutor/logic/MotorAdaptativo.kt", True),
    (". Este componente analiza el historial de sesiones del usuario para calcular un índice de rendimiento "
     "por categoría de ejercicio. El algoritmo asigna un peso dinámico a cada ejercicio en función de los "
     "resultados previos: las categorías con menor porcentaje de acierto reciben mayor probabilidad de aparición "
     "en la siguiente sesión, mientras que las categorías dominadas se presentan con menor frecuencia.", False),
])
add_paragraph(doc,
    "Los ejercicios disponibles se cargan desde nueve archivos JSON organizados por categoría, ubicados en "
    "la carpeta assets/MotorAdaptativo/. Cada archivo contiene un arreglo de objetos JSON con los campos: "
    "identificador único, enunciado, tipo de ejercicio, categoría fonética, dificultad base y metadatos "
    "de puntuación. El motor actualiza los pesos de ponderación al finalizar cada sesión de ejercicio."
)

set_heading(doc, "4.2 Pantalla de Ejercicios Adaptativos: EjerciciosAdaptativosScreen.kt", level=3)
add_mixed_paragraph(doc, [
    ("La pantalla de ejercicios adaptativos se implementó en ", False),
    ("app/src/main/java/com/h4k48/speaktutor/ui/screens/EjerciciosAdaptativosScreen.kt", True),
    (". Esta pantalla consume los servicios del MotorAdaptativo para generar una lista de ejercicios "
     "personalizada en cada sesión. El flujo de ejercitación presenta el enunciado al usuario, registra "
     "su respuesta mediante reconocimiento de voz o entrada táctil, evalúa el resultado y actualiza "
     "el historial de rendimiento.", False),
])
add_paragraph(doc,
    "El modelo de datos de los ejercicios personalizados se definió en la clase EjercicioPersonalizado.kt, "
    "ubicada en el paquete model. Este modelo encapsula los atributos del ejercicio, el estado de completitud "
    "y la puntuación obtenida en la sesión actual."
)

set_heading(doc, "4.3 Motor de Audio DSP: MotorAudioDSP.kt y componente DSP", level=3)
add_mixed_paragraph(doc, [
    ("En el commit Cambios-y-mejoras (28 de septiembre de 2026) se incorporó el motor de procesamiento "
     "digital de señal de audio, implementado en ", False),
    ("app/src/main/java/com/h4k48/speaktutor/logic/MotorAudioDSP.kt", True),
    (". Este componente establece la infraestructura base para la captura y el procesamiento en tiempo real "
     "del audio del micrófono mediante la librería Oboe de Google.", False),
])
add_paragraph(doc,
    "En esta etapa inicial del motor DSP se implementó la captura de audio en formato PCM de 16 bits "
    "a 44.1 kHz con un buffer de baja latencia. Se definieron las interfaces de devolución de llamada "
    "(callback) para la entrega de fotogramas de audio al procesador. También se creó el componente "
    "BiofeedbackVisualDSP.kt en la carpeta de componentes de UI, que implementa el visualizador de "
    "señal de audio en tiempo real mediante un Canvas de Jetpack Compose."
)
add_paragraph(doc,
    "Se incorporaron iconos personalizados en CustomIcons.kt para representar visualmente las funciones "
    "del DSP y se actualizaron los menús de navegación para incluir acceso al módulo de biofeedback."
)

# ─────────────────────────────────────────────────────────────────────────────
# 5. MOTOR DE AUDIO DSP Y BIOFEEDBACK
# ─────────────────────────────────────────────────────────────────────────────

set_heading(doc, "5. Motor de Audio DSP completo y módulo de Biofeedback (commit: Mejoras-y-sugerencias — 7 de octubre de 2026)", level=2)
add_paragraph(doc,
    "El 7 de octubre de 2026 se completó la implementación del motor de procesamiento digital de señal "
    "y se desarrolló el módulo de biofeedback con técnicas terapéuticas especializadas para el tratamiento "
    "de la tartamudez y otros trastornos de la fluidez del habla."
)

set_heading(doc, "5.1 Integración de la librería Oboe", level=3)
add_paragraph(doc,
    "Se incorporó la librería Oboe de Google mediante la adición del módulo de código nativo (NDK) al proyecto. "
    "Se configuró el archivo CMakeLists.txt para compilar el código nativo en C++ junto con la librería Oboe. "
    "La elección de Oboe se justifica por su capacidad de seleccionar automáticamente el backend de audio "
    "más eficiente disponible en el dispositivo (AAudio en Android 8.0+ o OpenSL ES en versiones anteriores), "
    "reduciendo la latencia de captura y reproducción de audio a valores inferiores a 10 ms en dispositivos compatibles."
)

set_heading(doc, "5.2 Procesamiento DSP: DAF, FAF, RMS, ZCR y detección de bloqueos", level=3)
add_mixed_paragraph(doc, [
    ("El archivo ", False),
    ("MotorAudioDSP.kt", True),
    (" se amplió con la implementación de los siguientes algoritmos de procesamiento de señal:", False),
])
for alg in [
    "DAF (Delayed Auditory Feedback): introduce un retardo configurable (20–250 ms) en la reproducción del audio capturado por el micrófono, técnica clínicamente validada para reducir la tartamudez.",
    "FAF (Frequency-Altered Feedback): modifica en tiempo real la frecuencia fundamental del audio reproducido al usuario (desplazamiento de semitono configurable), generando un efecto coral que facilita la fluidez.",
    "RMS (Root Mean Square): calcula el nivel de energía de la señal de audio en cada fotograma, utilizado como indicador de volumen y actividad vocal.",
    "ZCR (Zero Crossing Rate): calcula la tasa de cruces por cero de la señal, indicador del contenido frecuencial y la sonoridad de los fonemas.",
    "Detección de bloqueos tónicos y clónicos: algoritmo basado en la duración y el patrón de energía de la señal para clasificar automáticamente los eventos de disfluencia.",
]:
    add_bullet(doc, alg)

set_heading(doc, "5.3 Pantalla de Biofeedback: BiofeedbackScreen.kt y BiofeedbackViewModel.kt", level=3)
add_mixed_paragraph(doc, [
    ("El módulo de biofeedback se implementó en ", False),
    ("app/src/main/java/com/h4k48/speaktutor/ui/screens/BiofeedbackScreen.kt", True),
    (" siguiendo el patrón de arquitectura MVVM. La lógica de negocio se centralizó en ", False),
    ("BiofeedbackViewModel.kt", True),
    (", que gestiona el estado de la sesión de biofeedback, coordina el MotorAudioDSP y expone flujos "
     "de datos reactivos (StateFlow) a la capa de UI.", False),
])
add_paragraph(doc,
    "La pantalla integra tres técnicas terapéuticas seleccionables por el usuario:"
)
for tec in [
    "Técnica Koeppen: guía al usuario a través de ejercicios de tensión y relajación muscular progresiva con instrucciones de voz sintetizada y temporizadores visuales.",
    "Técnica de Soplido: presenta animaciones de retroalimentación que responden en tiempo real al flujo de aire detectado por el micrófono, mediante el análisis de la energía RMS de la señal.",
    "Técnica de la Pluma: variante del soplido donde la animación de una pluma virtual responde proporcionalmente a la intensidad del soplo del usuario, estimulando la regulación del flujo de aire espiratorio.",
]:
    add_bullet(doc, tec)

set_heading(doc, "5.4 Ampliación de RitmoFluidezScreen.kt: evocación léxica", level=3)
add_mixed_paragraph(doc, [
    ("En este mismo commit se amplió ", False),
    ("RitmoFluidezScreen.kt", True),
    (" con un cuarto modo de ejercitación: la evocación léxica. En este modo el usuario dispone de "
     "categorías semánticas y antónimos para evocar palabras de forma fluida bajo restricción temporal. "
     "Las categorías y antónimos se cargan desde los archivos ", False),
    ("assets/SimulacionSituaciones/evocacion_categorias.json", True),
    (" y ", False),
    ("assets/SimulacionSituaciones/evocacion_antonimos.json", True),
    (".", False),
])

# ─────────────────────────────────────────────────────────────────────────────
# 6. MÓDULOS DE FONOLOGÍA Y ARTICULACIÓN
# ─────────────────────────────────────────────────────────────────────────────

set_heading(doc, "6. Módulos de Fonología y Articulación (commit: Fonologica — 13 de octubre de 2026)", level=2)
add_paragraph(doc,
    "El 13 de octubre de 2026 se implementó el conjunto de módulos orientados al entrenamiento de la "
    "conciencia fonológica y la articulación de fonemas, ampliando significativamente las capacidades "
    "terapéuticas de la aplicación."
)

set_heading(doc, "6.1 Pantalla de Tarjetas de Articulación: TarjetasArticulacionScreen.kt", level=3)
add_mixed_paragraph(doc, [
    ("La pantalla de articulación, implementada en ", False),
    ("app/src/main/java/com/h4k48/speaktutor/ui/screens/TarjetasArticulacionScreen.kt", True),
    (", presenta al usuario un catálogo de praxias articulatorias: ejercicios de movilidad de los "
     "órganos fonoarticuladores (lengua, labios, mandíbula y paladar blando). Cada tarjeta muestra "
     "una ilustración descriptiva, una instrucción textual y una demostración animada del movimiento "
     "requerido. Las tarjetas se organizan por grupo articulatorio y el usuario puede filtrarlas "
     "según la zona de articulación que desea ejercitar.", False),
])

set_heading(doc, "6.2 Monitor Fonológico: MonitorFonologico.kt", level=3)
add_mixed_paragraph(doc, [
    ("El componente MonitorFonológico, en ", False),
    ("app/src/main/java/com/h4k48/speaktutor/ui/components/MonitorFonologico.kt", True),
    (", implementa un panel de seguimiento de la producción fonológica del usuario. "
     "El monitor muestra en tiempo real los fonemas detectados mediante el reconocedor de voz, "
     "diferenciando entre fonemas correctamente producidos y fonemas sustituidos o distorsionados. "
     "Adicionalmente, integra un módulo de Comunicación Aumentativa y Alternativa (CAA) que permite "
     "al usuario seleccionar pictogramas para comunicarse sin producción verbal.", False),
])

set_heading(doc, "6.3 Gestor de Conciencia Fonológica: GestorConcienciaFonologica.kt", level=3)
add_mixed_paragraph(doc, [
    ("La lógica de los ejercicios de conciencia fonológica se centralizó en ", False),
    ("app/src/main/java/com/h4k48/speaktutor/logic/GestorConcienciaFonologica.kt", True),
    (". Este componente carga y administra los ejercicios definidos en ", False),
    ("assets/ConcienciaFonologica/Ejercicios.json", True),
    (", clasifica las actividades en cinco categorías y gestiona la secuencia de presentación "
     "y la evaluación de respuestas:", False),
])
for cat in [
    "Discriminación auditiva: identificación de pares mínimos y sonidos ambientales.",
    "Conciencia silábica: segmentación, conteo y manipulación de sílabas.",
    "Cubos de Elkonin: representación visual de los fonemas constituyentes de una palabra.",
    "Conciencia de palabra: identificación de palabras en oraciones.",
    "Conciencia de frase: segmentación y estructura de enunciados.",
]:
    add_bullet(doc, cat)

set_heading(doc, "6.4 Diálogo de Perfil Clínico: DialogoPerfilClinico.kt", level=3)
add_mixed_paragraph(doc, [
    ("Se implementó el diálogo de perfil clínico en ", False),
    ("app/src/main/java/com/h4k48/speaktutor/ui/components/DialogoPerfilClinico.kt", True),
    (". Este componente AlertDialog recopila al inicio de la aplicación los datos clínicos del usuario: "
     "diagnóstico principal, edad, objetivo terapéutico y nivel de experiencia. Los datos capturados "
     "son utilizados por el MotorAdaptativo para configurar los parámetros iniciales de la sesión de "
     "ejercitación.", False),
])

# ─────────────────────────────────────────────────────────────────────────────
# 7. INTEGRACIÓN DE PUBLICIDAD Y REFINAMIENTO
# ─────────────────────────────────────────────────────────────────────────────

set_heading(doc, "7. Integración de publicidad y refinamiento de módulos (commit: cambios — 16 de octubre de 2026)", level=2)
add_paragraph(doc,
    "El 16 de octubre de 2026 se incorporó el sistema de monetización mediante publicidad de Google "
    "AdMob y se aplicaron refinamientos a los módulos DSP y de articulación."
)

set_heading(doc, "7.1 Banner de Anuncio: BannerAd", level=3)
add_paragraph(doc,
    "Se integró la dependencia del SDK de Google Mobile Ads (AdMob) en el archivo build.gradle.kts. "
    "Se configuró el Application ID de AdMob en el AndroidManifest.xml. Se implementó el composable "
    "BannerAd, que encapsula el AdView de AdMob dentro de un contenedor AndroidView de Jetpack Compose. "
    "El banner se posicionó en la parte inferior de las pantallas principales sin interferir con los "
    "controles interactivos de las sesiones terapéuticas. Se utilizó el ID de unidad de anuncio de prueba "
    "durante la fase de desarrollo."
)

set_heading(doc, "7.2 Actualización del módulo DSP y TarjetasArticulacionScreen", level=3)
add_paragraph(doc,
    "Se realizaron correcciones en el pipeline de procesamiento del MotorAudioDSP relacionadas con la "
    "gestión del ciclo de vida del stream de audio Oboe, garantizando la liberación correcta de recursos "
    "al pausar o destruir la actividad. Se actualizó TarjetasArticulacionScreen.kt para incorporar "
    "nuevas praxias articulatorias y mejorar la navegación entre grupos de ejercicios."
)

# ─────────────────────────────────────────────────────────────────────────────
# 8. AMPLIACIÓN DE BASE DE EJERCICIOS Y SISTEMA DE RECOMPENSAS
# ─────────────────────────────────────────────────────────────────────────────

set_heading(doc, "8. Ampliación de la base de ejercicios y sistema de recompensas (commits: mejoras y actualizacion-de-ejercicios — 24 de octubre al 18 de noviembre de 2026)", level=2)
add_paragraph(doc,
    "En los commits finales del periodo de desarrollo se amplió sustancialmente el contenido clínico "
    "de la aplicación, se refinó el motor adaptativo y se incorporó un sistema de recompensas para "
    "motivar la adherencia del usuario al plan terapéutico."
)

set_heading(doc, "8.1 Alertas y ampliación de ProgresoScreen (commit: mejoras — 24 de octubre de 2026)", level=3)
add_mixed_paragraph(doc, [
    ("Se implementó el componente ", False),
    ("AlertaInPlace.kt", True),
    (", que muestra mensajes contextuales no bloqueantes directamente en la pantalla de progreso. "
     "Las alertas informan al usuario sobre hitos alcanzados (p. ej., número de sesiones completadas "
     "o mejora porcentual en una categoría), sobre categorías con rendimiento por debajo del umbral "
     "clínico y sobre recomendaciones de frecuencia de práctica.", False),
])
add_paragraph(doc,
    "Se incrementó la base de ejercicios disponibles en los archivos JSON del MotorAdaptativo, "
    "añadiendo nuevos ítems en las categorías de fluidez, ritmo, fonética y discriminación auditiva. "
    "Se refinó el algoritmo de ponderación del MotorAdaptativo para incorporar un factor de decaimiento "
    "temporal, de forma que el rendimiento de sesiones recientes tiene mayor peso que el de sesiones antiguas."
)

set_heading(doc, "8.2 Simulación de Situaciones: SimulacionSituaciones.kt", level=3)
add_mixed_paragraph(doc, [
    ("La pantalla de simulación de situaciones cotidianas se implementó en ", False),
    ("app/src/main/java/com/h4k48/speaktutor/ui/screens/SimulacionSituaciones.kt", True),
    (". Este módulo expone al usuario a contextos comunicativos de dificultad creciente, "
     "organizados en cuatro categorías:", False),
])
for cat in [
    ("Escenarios cotidianos: situaciones del día a día (compras, transporte, presentaciones) cargadas desde "
     "assets/SimulacionSituaciones/escenarios.json."),
    ("Guiones estresantes: diálogos con carga emocional y temporal creciente distribuidos en cuatro niveles "
     "de dificultad (guiones_nivel1.json a guiones_nivel4.json)."),
    ("Simulación de llamadas telefónicas: escenarios de comunicación telefónica con retroalimentación de voz "
     "sintetizada, cargados desde llamadas.json."),
    ("Evocación léxica: actividades de fluencia semántica por categorías y de recuperación de antónimos."),
]:
    add_bullet(doc, cat)

set_heading(doc, "8.3 Controles AAF: ControlesAAF.kt", level=3)
add_mixed_paragraph(doc, [
    ("En el commit actualizacion-de-ejercicios (18 de noviembre de 2026) se implementó el componente "
     "de Controles de Alteración Auditiva de la Frecuencia en ", False),
    ("app/src/main/java/com/h4k48/speaktutor/ui/components/ControlesAAF.kt", True),
    (". Este componente agrupa en un panel expandible los controles de DAF y FAF disponibles en "
     "el MotorAudioDSP: control deslizante de retardo DAF (0–250 ms), control deslizante de desplazamiento "
     "de semitono FAF (–12 a +12 semitonos), interruptor de activación/desactivación de cada efecto "
     "y visualizador compacto del nivel RMS.", False),
])

set_heading(doc, "8.4 Actualización del código nativo: native-lib.cpp", level=3)
add_mixed_paragraph(doc, [
    ("Se actualizó el archivo de código nativo ", False),
    ("app/src/main/cpp/native-lib.cpp", True),
    (" para incorporar mejoras en el pipeline de procesamiento DSP: optimización del algoritmo de "
     "retardo circular (buffer de retardo DAF), implementación del filtro de modificación de frecuencia "
     "mediante resampling de alta calidad (FAF) y refinamiento de los detectores de bloqueo tónico "
     "y clónico basados en análisis de energía por ventana temporal.", False),
])
add_mixed_paragraph(doc, [
    ("Se actualizó ", False),
    ("MotorAudioDSP.kt", True),
    (" para exponer los nuevos parámetros del procesador nativo mediante la interfaz JNI (Java Native Interface). "
     "Se actualizó ", False),
    ("BiofeedbackVisualDSP.kt", True),
    (" para incorporar la visualización de los parámetros DAF/FAF activos en tiempo real.", False),
])

set_heading(doc, "8.5 Archivos de ejercicios JSON y recursos tipográficos", level=3)
add_paragraph(doc,
    "Se agregaron nuevos archivos JSON de ejercicios en las carpetas de assets correspondientes, "
    "ampliando el catálogo terapéutico disponible en los módulos de Conciencia Fonológica, "
    "Habla Estirada, Pronunciación al Instante, Ritmo y Fluidez, Simulación de Situaciones "
    "y Motor Adaptativo. Los archivos siguen la estructura de objetos JSON previamente definida, "
    "garantizando la compatibilidad con los gestores de ejercicios existentes."
)
add_paragraph(doc,
    "Se incorporaron fuentes tipográficas personalizadas en formatos .ttf y .otf en la carpeta "
    "assets/fonts/, utilizadas en los componentes del sistema de recompensas para ofrecer una "
    "presentación visual diferenciada y atractiva."
)

set_heading(doc, "8.6 Sistema de recompensas: InsigniaPremio.kt y TicketRecompensa.kt", level=3)
add_mixed_paragraph(doc, [
    ("Se diseñó e implementó un sistema de recompensas para incentivar la constancia en la práctica "
     "terapéutica. El componente ", False),
    ("InsigniaPremio.kt", True),
    (" muestra insignias desbloqueables al alcanzar hitos de sesiones y puntuación. "
     "El componente ", False),
    ("TicketRecompensa.kt", True),
    (" genera y presenta al usuario un ticket visual de logro al completar una sesión con "
     "rendimiento superior al umbral configurado. Ambos componentes utilizan las fuentes "
     "personalizadas incorporadas en este commit para su visualización.", False),
])
add_paragraph(doc,
    "El estado de las insignias y los tickets generados se persiste en SharedPreferences, "
    "permitiendo al usuario consultar su historial de logros en la pantalla de Progreso."
)

# ─────────────────────────────────────────────────────────────────────────────
# REPORTE MULTIDIMENSIONAL (componente final)
# ─────────────────────────────────────────────────────────────────────────────

set_heading(doc, "9. Reporte Multidimensional: ReporteMultidimensionalScreen.kt", level=2)
add_mixed_paragraph(doc, [
    ("La pantalla de reporte multidimensional, implementada en ", False),
    ("app/src/main/java/com/h4k48/speaktutor/ui/screens/ReporteMultidimensionalScreen.kt", True),
    (", consolida las métricas clínicas del usuario en una visualización de tipo radar (spider chart) "
     "dibujado con Canvas de Jetpack Compose. Los ejes del gráfico representan las dimensiones clínicas "
     "evaluadas por la aplicación:", False),
])
for dim in [
    "Precisión fonética (porcentaje de fonemas correctamente producidos).",
    "Fluidez (palabras por minuto en lectura guiada).",
    "Control de ritmo (adherencia al metrónomo en BPM).",
    "Conciencia fonológica (puntuación acumulada en ejercicios).",
    "Articulación (completitud de las praxias articulatorias practicadas).",
    "Consistencia de sesiones (regularidad de la práctica diaria).",
]:
    add_bullet(doc, dim)
add_paragraph(doc,
    "El gráfico radar se actualiza automáticamente al finalizar cada sesión de ejercitación. "
    "El reporte puede exportarse en formato PDF junto con el historial de sesiones desde la "
    "pantalla de Progreso, constituyendo un documento de seguimiento clínico formal para "
    "el intercambio entre el usuario y su terapeuta del habla."
)

# ─────────────────────────────────────────────────────────────────────────────
# GUARDAR
# ─────────────────────────────────────────────────────────────────────────────

doc.save(OUTPUT_PATH)
print(f"Documento generado exitosamente en: {OUTPUT_PATH}")
print(f"Tamaño: {os.path.getsize(OUTPUT_PATH):,} bytes")
