package com.itsx.speaktutor.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.logic.EstadoVozDSP
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.BiofeedbackVisualAvanzadoDSP
import com.itsx.speaktutor.ui.components.ControlVelocidadTts
import com.itsx.speaktutor.ui.components.HomeIcon
import com.itsx.speaktutor.ui.components.MetronomoReutilizableComponent
import com.itsx.speaktutor.ui.components.MonitorFonologicoYCAA
import com.itsx.speaktutor.ui.navigation.Screen

enum class SeccionRitmo { MENU_PRINCIPAL, BLOQUES, LECTURA, GRABADORA }
enum class NivelBloques { PRINCIPIANTE, INTERMEDIO, AVANZADO }

fun calcularPrecisionRitmo(textoEsperado: String, textoReconocido: String): Int {
    if (textoReconocido.isBlank()) return 0
    val textoLimpioEsperado = textoEsperado.replace(" / ", " ").lowercase().replace(Regex("[^a-záéíóúñ ]"), "")
    val palabrasEsperadas = textoLimpioEsperado.split("\\s+".toRegex())
    val palabrasReconocidas = textoReconocido.lowercase().replace(Regex("[^a-záéíóúñ ]"), "").split("\\s+".toRegex())

    var coincidencias = 0
    for (palabra in palabrasReconocidas) {
        if (palabrasEsperadas.contains(palabra)) {
            coincidencias++
        }
    }
    return ((coincidencias.toFloat() / palabrasEsperadas.size.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RitmoFluidezScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var seccionActual by remember { mutableStateOf(SeccionRitmo.MENU_PRINCIPAL) }

    // --- VARIABLES DE ANIMACIÓN Y VIBRACIÓN ---
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    var tensionDetectada by remember { mutableStateOf(false) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }
    var velocidadHabla by remember { mutableFloatStateOf(0.9f) }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var textoEscuchado by remember { mutableStateOf("") }
    var ultimaCalificacion by remember { mutableStateOf(0) }

    var nivelBloqueSeleccionado by remember { mutableStateOf(NivelBloques.PRINCIPIANTE) }

    var isRecordingLibre by remember { mutableStateOf(false) }
    var isPlayingLibre by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val audioFile = remember { File(context.filesDir, "audio_practica_ritmo.m4a") }

    // Motor DSP
    val motorDSP = remember { MotorAudioDSP() }
    val metricasDSP by motorDSP.metricasAcusticas.collectAsState()
    var isDSPActiveId by remember { mutableStateOf<String?>(null) }
    var feedbackDSP by remember { mutableStateOf("Esperando voz...") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // DEBOUNCER DSP PARA RITMO
    var procesandoAudio by remember { mutableStateOf(false) }
    LaunchedEffect(metricasDSP.estado) {
        if (isDSPActiveId != null && !procesandoAudio) {
            if (metricasDSP.estado == EstadoVozDSP.BLOQUEO_TONICO || metricasDSP.estado == EstadoVozDSP.BLOQUEO_CLONICO) {
                procesandoAudio = true
                tensionDetectada = true
                feedbackDSP = "⚠️ Bloqueo de ritmo detectado"

                // Disparar vibración
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                // Disparar animación
                coroutineScope.launch {
                    offsetX.animateTo(15f, tween(50))
                    offsetX.animateTo(-15f, tween(50))
                    offsetX.animateTo(10f, tween(50))
                    offsetX.animateTo(-10f, tween(50))
                    offsetX.animateTo(0f, tween(50))
                }

                delay(1200)
                tensionDetectada = false
                procesandoAudio = false
            } else if (metricasDSP.estado == EstadoVozDSP.FLUIDO) {
                procesandoAudio = true
                feedbackDSP = "✅ Habla fluida"
                delay(1200)
                procesandoAudio = false
            }
        }
    }

    val bloquesPrincipiante = listOf(
        "El / pe / rro / de / San / ro / que",
        "Ca / mi / na / ba / sin / gen / te",
        "Pau / sa / y / res / pi / ra / bien",
        "Un / pa / so / a / la / vez",
        "El / sol / bri / lla / hoy",
        "Me / gus / ta / can / tar",
        "Voy / a / la / ciu / dad",
        "La / me / sa / es / ro / ja",
        "Quie / ro / un / ca / fé",
        "Bue / nos / dí / as / hoy",
        "Mi / ga / to / duer / me / más",
        "El / cie / lo / es / a / zul",
        "La / lu / na / bri / lla / ya",
        "Tú / pue / des / ha / cer / lo",
        "Té / de / li / món / ca / lien / te",
        "U / na / tar / de / de / paz",
        "Hoy / es / un / buen / dí / a",
        "Yo / quie / ro / a / pren / der",
        "El / vien / to / so / pla / luz",
        "Las / flo / res / son / li / las",
        "Pe / ces / en / el / a / gua",
        "U / na / bue / na / vi / da",
        "La / ma / ña / na / es / hoy",
        "Le / er / te / da / sa / lud",
        "Mi / li / bro / de / no / tas",
        "El / co / che / va / muy / bien",
        "U / na / luz / en / la / red",
        "El / mar / es / muy / pro / fun / do",
        "Me / gus / ta / di / bu / jar",
        "El / ár / bol / es / muy / gran / de",
        "La / no / che / es / os / cu / ra",
        "To / do / va / a / es / tar / bien",
        "La / bri / sa / es / muy / sua / ve",
        "El / pe / z / na / da / rá",
        "Voy / a / co / mer / pan",
        "La / so / pa / tie / ne / sal",
        "El / a / gua / es / vi / da",
        "Mi / ca / sa / es / bo / ni / ta",
        "El / pa / pel / es / blan / co",
        "La / si / lla / de / ma / de / ra",
        "Un / nue / vo / pro / yec / to",
        "El / có / di / go / fun / cio / na",
        "La / nu / be / es / se / gu / ra",
        "A / bue / lo / ven / a / quí",
        "El / re / loj / mar / ca / ya",
        "La / tar / de / cae / len / ta",
        "Un / ba / so / de / a / gua",
        "Me / a / gra / da / tu / voz",
        "El / pi / no / es / al / to",
        "La / cla / ve / es / la / paz"
    )

    val bloquesIntermedio = listOf(
        "La / cons / tan / cia / me / jo / ra / tu / ha / bla",
        "El / a / i / re / flu / ye / con / na / tu / ra / li / dad",
        "Ha / blar / des / pa / cio / te / da / con / fian / za",
        "Res / pi / ra / pro / fun / do / an / tes / de / ha / blar",
        "Las / o / las / del / mar / son / re / la / jan / tes",
        "El / a / pren / di / za / je / to / ma / su / tiem / po",
        "Man / tén / el / rit / mo / con / el / me / tró / no / mo",
        "To / ma / te / tu / tiem / po / pa / ra / res / pon / der",
        "El / pro / ce / so / de / mi / gra / ción / es / se / gu / ro",
        "Con / fi / gu / rar / re / des / re / quie / re / pa / cien / cia",
        "Cre / ar / a / pli / ca / cio / nes / mo / de / ra / la / an / sie / dad",
        "El / a / i / re / de / la / mon / ta / ña / sa / na",
        "La / ges / tión / de / ru / tas / en / la / nu / be",
        "La / vir / tua / li / za / ción / op / ti / mi / za / re / cur / sos",
        "Es / cri / bir / có / di / go / te / a / yu / da / a / pen / sar",
        "El / a / gua / del / rí / o / ba / ja / muy / frí / a",
        "Tra / ba / jar / en / e / qui / po / su / ma / fuer / zas",
        "Un / en / tor / no / li / nux / te / da / con / trol",
        "Las / ba / ses / de / da / tos / guar / dan / he / chos",
        "U / sar / con / te / ne / do / res / es / muy / prác / ti / co",
        "El / via / je / lar / go / de / ja / lec / cio / nes",
        "Di / bu / jar / en / la / pan / ta / lla / re / la / ja",
        "La / mú / si / ca / sua / ve / cal / ma / la / men / te",
        "Pau / sar / en / me / dio / del / rui / do / es / sa / no",
        "El / a / zú / car / mo / re / no / sa / be / me / jor",
        "La / res / pi / ra / ción / pro / fun / da / es / la / cla / ve",
        "Los / ser / vi / do / res / pro / ce / san / los / da / tos",
        "La / in / te / li / gen / cia / ar / ti / fi / cial / a / yu / da",
        "El / pro / gra / ma / se / e / je / cu / ta / sin / fa / llos",
        "A / pren / der / a / pro / gra / mar / a / bre / puer / tas",
        "Las / he / rra / mien / tas / web / son / fa / sci / nan / tes",
        "La / se / gu / ri / dad / in / for / má / ti / ca / cre / ce",
        "Un / di / se / ño / lim / pio / a / trae / la / mi / ra / da",
        "El / a / na / lis / ta / re / vi / sa / los / lo / gs",
        "Com / pi / lar / el / pro / yec / to / to / ma / tiem / po",
        "El / ar / te / di / gi / tal / re / quie / re / ta / len / to",
        "La / na / tu / ra / le / za / siem / pre / nos / a / som / bra",
        "Un / pa / se / o / por / el / par / que / es / muy / bue / no",
        "Los / a / ni / ma / les / de / ben / ser / pro / te / gi / dos",
        "El / a / ma / ne / cer / pin / ta / de / ro / jo / to / do",
        "Las / no / tas / mu / si / ca / les / for / man / a / cor / des",
        "El / vo / lu / men / de / voz / de / be / ser / cons / tan / te",
        "La / co / mu / ni / ca / ción / u / ne / a / la / gen / te",
        "Los / li / bros / an / ti / guos / guar / dan / se / cre / tos",
        "U / na / bue / na / pos / tu / ra / da / se / gu / ri / dad",
        "El / a / tlé / ta / se / pre / pa / ra / ca / da / dí / a",
        "Las / i / de / as / cla / ras / bri / llan / por / sí / so / las",
        "Un / de / sa / rro / llo / á / gil / es / más / e / fi / caz",
        "El / de / pur / a / dor / en / cuen / tra / los / bu / gs",
        "La / tec / no / lo / gí / a / me / jo / ra / vi / das"
    )

    val bloquesAvanzado = listOf(
        "La / ar / ti / cu / la / ción / cla / ra / fa / ci / li / ta / la / co / mu / ni / ca / ción",
        "E / je / cu / tar / pau / sas / es / truc / tu / ra / das / re / du / ce / los / blo / que / os",
        "El / pen / sa / mien / to / po / si / ti / vo / trans / for / ma / nues / tra / vi / da / dia / ria",
        "La / tec / no / lo / gí / a / a / van / za / de / for / ma / in / con / te / ni / ble / men / te",
        "De / sa / rro / llar / u / na / bue / na / res / pi / ra / ción / dia / frag / má / ti / ca / es / cla / ve",
        "La / in / te / li / gen / cia / ar / ti / fi / cial / o / fre / ce / nue / vas / he / rra / mien / tas",
        "El / des / plie / gue / de / nues / tros / clús / ters / de / ku / ber / ne / tes / fue / e / xi / to / so",
        "Mi / grar / los / ser / vi / do / res / a / nues / tra / nu / be / to / ma / me / ses",
        "Las / me / to / do / lo / gí / as / á / gi / les / me / jo / ran / el / flu / jo / de / tra / ba / jo",
        "U / ti / li / zar / con / te / ne / do / res / de / soft / ware / a / ís / la / el / en / tor / no",
        "La / in / fra / es / truc / tu / ra / co / mo / có / di / go / e / vi / ta / fa / llos / hu / ma / nos",
        "Mo / ni / to / re / ar / las / re / des / pre / vie / ne / ca / í / das / to / ta / les / del / sis / te / ma",
        "La / pro / gra / ma / ción / a / sín / cro / na / e / vi / ta / blo / que / os / en / la / red",
        "E / va / lu / ar / el / ren / di / mien / to / de / la / ba / se / de / da / tos / es / pri / mor / dial",
        "Cre / ar / per / so / na / jes / y / es / ce / na / rios / en / tres / de / fo / men / ta / la / cre / a / ti / vi / dad",
        "Los / pa / tro / nes / de / ar / qui / tec / tu / ra / mi / cro / ser / vi / cios / op / ti / mi / zan / re / cur / sos",
        "Los / pro / to / co / los / de / en / ru / ta / mien / to / fa / ci / li / tan / el / trán / si / to / de / pa / que / tes",
        "Man / te / ner / un / re / gis / tro / e / xac / to / de / los / fa / llos / a / yu / da / al / diag / nós / ti / co",
        "El / con / trol / de / ver / sio / nes / dis / tri / bui / do / re / vo / lu / cio / nó / la / in / dus / tria",
        "La / i / ni / cia / li / za / ción / de / má / qui / nas / vir / tua / les / re / quie / re / mu / cha / me / mo / ria",
        "La / nor / ma / li / za / ción / de / da / tos / per / mi / te / cons / truir / me / jo / res / ta / blas",
        "A / pli / car / prin / ci / pios / de / in / ge / nie / rí / a / de / soft / ware / a / se / gu / ra / ca / li / dad",
        "La / in / te / gra / ción / de / mo / de / los / ge / ne / ra / ti / vos / a / ce / le / ra / las / ta / re / as",
        "El / an / cho / de / ban / da / de / be / ser / su / fi / cien / te / pa / ra / e / vi / tar / re / tra / sos",
        "Las / prue / bas / de / se / gu / ri / dad / de / tec / tan / vul / ne / ra / bi / li / da / des / o / cul / tas",
        "O / pti / mi / zar / el / có / di / go / re / du / ce / los / tiem / pos / de / car / ga / no / ta / ble / men / te",
        "La / res / pon / sa / bi / li / dad / so / cial / cor / po / ra / ti / va / a / trae / nue / vos / in / ver / so / res",
        "El / an / á / li / sis / pre / dic / ti / vo / a / ci / er / ta / con / as / om / bro / sa / pre / ci / sión",
        "El / au / to / ma / ti / s / mo / in / dus / trial / me / jo / ra / la / pro / duc / ci / ón / en / ca / de / na",
        "La / ex / pe / rien / cia / del / u / sua / rio / de / ter / mi / na / el / é / xi / to / de / la / a / pli / ca / ción",
        "Es / ta / ble / cer / li / ne / a / mien / tos / de / di / se / ño / ga / ran / ti / za / u / na / i / gen / ti / dad / vi / sual",
        "La / re / a / li / dad / vir / tual / su / mer / ge / al / es / pec / ta / dor / en / un / mun / do / pa / ra / le / lo",
        "E / va / lu / ar / la / la / ten / cia / de / los / pa / que / tes / a / se / gu / ra / co / ne / xio / nes / rá / pi / das",
        "El / des / plie / gue / con / ti / nuo / mi / ni / mi / za / los / e / rro / res / en / pro / duc / ción",
        "La / sin / cro / ni / za / ción / de / hi / los / en / se / gun / do / pla / no / pre / vie / ne / el / con / ge / la / mien / to",
        "El / man / te / ni / mien / to / pre / ven / ti / vo / pro / lon / ga / la / vi / da / ú / til / de / los / e / qui / pos",
        "La / bi / blio / te / ca / de / com / po / nen / tes / a / ce / le / ra / el / de / sa / rro / llo / fron / tend",
        "La / co / mu / ni / ca / ción / in / ter / pro / ce / sos / es / vi / tal / en / sis / te / mas / dis / tri / bui / dos",
        "Im / ple / men / tar / au / ten / ti / ca / ción / do / ble / pro / te / ge / nues / tros / da / tos / sen / si / bles",
        "La / fi / lo / so / fí / a / es / toi / ca / pro / mue / ve / la / re / si / lien / cia / e / mo / cio / nal",
        "El / ca / len / ta / mien / to / glo / bal / a / fe / cta / los / e / co / sis / te / mas / más / vul / ne / ra / bles",
        "La / ex / plo / ra / ción / sub / ma / ri / na / re / ve / la / es / pe / cies / nun / ca / an / tes / vis / tas",
        "El / de / re / cho / a / la / pri / va / ci / dad / es / un / pi / lar / fun / da / men / tal / en / in / ter / net",
        "La / neu / ro / plas / ti / ci / dad / per / mi / te / que / el / ce / re / bro / se / re / or / ga / ni / ce / siem / pre",
        "El / an / á / li / sis / e / co / nó / mi / co / re / fle / ja / u / na / re / cu / pe / ra / ción / pau / la / ti / na",
        "La / re / frac / ción / de / la / luz / ex / pli / ca / la / for / ma / ción / del / ar / co / i / ris",
        "El / me / ta / bo / lis / mo / ce / lu / lar / con / vier / te / los / nu / trien / tes / en / e / ner / gí / a",
        "Las / es / tra / te / gias / de / mi / ti / ga / ción / dis / mi / nu / yen / el / ries / go / fi / nan / cie / ro",
        "La / se / man / ti / ca / de / los / len / gua / jes / de / pro / gra / ma / ción / fa / ci / li / ta / su / com / pren / sión",
        "El / pro / ce / sa / mien / to / del / len / gua / je / na / tu / ral / a / cer / ca / a / las / má / qui / nas / al / hom / bre"
    )

    val lecturasExtensas = listOf(
        Pair("🌊 El curso del río", "Un río nunca se detiene ante las rocas; simplemente encuentra la forma de rodearlas y continuar su camino. De la misma manera, al hablar no debemos apresurar las palabras ni temer a las pausas. El secreto de una fluidez serena reside en permitir que el aire fluya sin prisa."),
        Pair("🌳 La fuerza del árbol", "En medio del bosque, un viejo roble enseñaba a los árboles jóvenes el arte de resistir las tormentas. Les decía que el árbol que intenta ser rígido frente al viento termina por romperse, mientras que aquel que se mece suavemente al ritmo de la brisa permanece siempre firme."),
        Pair("💡 El valor de la pausa", "Hacer una pausa antes de hablar no es un signo de duda, sino una muestra de dominio y serenidad. Cuando hacemos un alto para inhalar aire de forma diafragmática, preparamos las cuerdas vocales para articular cada sílaba con precisión."),
        Pair("🌅 La rutina matutina", "Caminar por la mañana temprano ayuda a despejar la mente de las preocupaciones y te prepara para un día muy productivo. Observar el amanecer nos recuerda que cada día es una nueva oportunidad para avanzar a nuestro propio ritmo."),
        Pair("☁️ La computación en la nube", "En el mundo de la tecnología actual, la computación en la nube permite a las empresas escalar sus recursos de forma inmediata y segura. Los servidores distribuidos garantizan que la información siempre esté disponible."),
        Pair("🍂 El paisaje de otoño", "Durante la temporada de otoño en el bosque, las hojas caen lentamente sobre los senderos formando un hermoso paisaje de colores cálidos. El crujir de las ramas secas bajo los zapatos anuncia la llegada del frío."),
        Pair("📱 El desarrollo de software", "El desarrollo de aplicaciones móviles requiere paciencia, dedicación y una constante adaptación a los nuevos sistemas operativos. El código limpio y bien estructurado es el equivalente a una mente clara y organizada."),
        Pair("🌬️ La respiración diafragmática", "La respiración diafragmática es fundamental no solo para cantar o actuar, sino para mantener una fonación relajada y libre de tensión. Controlar el flujo de aire nos permite proyectar la voz sin lastimar las cuerdas vocales."),
        Pair("🚀 El viaje espacial", "La exploración del espacio exterior siempre ha fascinado a la humanidad. Las estrellas distantes nos invitan a soñar en grande y a recordar que nuestros problemas cotidianos son pequeños en comparación con la inmensidad del universo."),
        Pair("🧠 La plasticidad cerebral", "Nuestro cerebro tiene la increíble capacidad de adaptarse y aprender nuevas habilidades a lo largo de toda la vida. Esta plasticidad neuronal nos demuestra que siempre podemos superar nuestros propios límites con práctica constante."),
        Pair("🐳 Orquestación de contenedores", "La virtualización y los contenedores transforman la manera en que desplegamos aplicaciones. Al aislar cada servicio, evitamos conflictos de dependencias y aseguramos que el entorno de desarrollo sea idéntico al de producción, permitiendo una escalabilidad fluida."),
        Pair("☁️ Arquitectura de migración", "Mover una infraestructura tradicional hacia la nube requiere un diseño meticuloso. Al utilizar zonas de aterrizaje y conexiones dedicadas, se logra una alta disponibilidad y recuperaciones ante desastres rápidas, protegiendo las bases de datos más críticas."),
        Pair("📊 Inteligencia de negocios", "El análisis de datos con herramientas avanzadas nos permite transformar números fríos en historias visuales. Al normalizar tablas y utilizar fórmulas complejas, descubrimos tendencias ocultas que guían las decisiones estratégicas de cualquier organización."),
        Pair("🎮 El arte de los píxeles", "Diseñar arte digital requiere una paciencia meticulosa. Cada bloque de color debe colocarse con intención para crear la ilusión de volumen y movimiento. Es un proceso detallado donde la simplicidad visual esconde una enorme complejidad técnica."),
        Pair("🐧 Administración del sistema", "Navegar por una terminal de comandos nos otorga un control absoluto sobre el sistema operativo. Configurar interfaces de red, gestionar permisos y automatizar tareas de mantenimiento mediante scripts no solo optimiza el rendimiento, sino que fortalece la seguridad."),
        Pair("⏳ La gestión del tiempo", "Administrar nuestro tiempo no significa llenar cada minuto con tareas, sino saber priorizar lo que realmente importa. Dejar espacios vacíos en la agenda es fundamental para mantener la creatividad y evitar el agotamiento."),
        Pair("📚 El hábito de la lectura", "Sumergirse en un buen libro es como viajar a otro universo sin mover los pies. La lectura constante expande nuestro vocabulario, mejora la concentración y fomenta una profunda empatía hacia experiencias distintas a las nuestras."),
        Pair("🏔️ Escalar la montaña", "Llegar a la cima de una gran montaña requiere más que fuerza física; exige una voluntad inquebrantable. Cada paso cuesta arriba es una victoria sobre las dudas y los miedos que amenazan con hacernos retroceder."),
        Pair("☕ El café de la mañana", "Preparar una taza de café recién molido es un ritual que despierta los sentidos. El aroma tostado y el calor de la taza en las manos nos otorgan unos minutos de calma antes de enfrentar las demandas del mundo moderno."),
        Pair("🎨 La creatividad infinita", "La creatividad no es un talento exclusivo de unos pocos, sino un músculo que todos podemos ejercitar. Pintar, escribir o programar son formas distintas de canalizar nuestra imaginación para construir algo completamente nuevo."),
        Pair("🤖 Algoritmos de aprendizaje", "Las redes neuronales artificiales están inspiradas en el cerebro humano. Entrenar un modelo de inteligencia artificial implica alimentar algoritmos con miles de ejemplos para que aprendan a reconocer patrones ocultos en los datos."),
        Pair("🛡️ Ciberseguridad proactiva", "Proteger una red no se trata solo de construir muros altos, sino de anticipar los movimientos del adversario. Las auditorías constantes y la encriptación sólida son escudos esenciales en la era de la información digital."),
        Pair("🎵 La teoría musical", "Comprender la teoría detrás de una melodía nos permite apreciar la música en una dimensión completamente nueva. Los intervalos, las escalas y los acordes son el lenguaje matemático que da forma a nuestras emociones sonoras."),
        Pair("🌌 Agujeros negros", "Estos gigantes cósmicos poseen una gravedad tan inmensa que ni siquiera la luz puede escapar de ellos. Estudiarlos nos desafía a cuestionar todo lo que creemos saber sobre el tiempo, el espacio y las leyes fundamentales de la física."),
        Pair("🌱 Cultivar un jardín", "Plantar semillas y cuidar un jardín requiere de una paciencia infinita. Ver crecer las primeras hojas verdes es una lección humilde de que los grandes resultados toman su tiempo y no pueden ser apresurados."),
        Pair("🚴 El ciclismo de ruta", "Pedalear por carreteras interminables ofrece una sensación de libertad inigualable. El viento en la cara y el sonido rítmico de las ruedas contra el asfalto se convierten en una meditación en movimiento que despeja el estrés."),
        Pair("📷 La fotografía nocturna", "Capturar la belleza de la noche exige dominar la luz y la exposición. Un buen trípode y la paciencia para esperar el momento exacto permiten transformar una escena oscura en una obra de arte deslumbrante."),
        Pair("🧩 Resolver problemas", "Enfrentarse a un desafío lógico o a un error de código es como armar un rompecabezas complejo. La frustración inicial da paso a una profunda satisfacción cuando finalmente encontramos la pieza que faltaba para solucionarlo."),
        Pair("🎭 Hablar en público", "Dirigirse a una audiencia puede acelerar el pulso de cualquiera. Sin embargo, recordar que el mensaje es más importante que los nervios nos ayuda a encontrar nuestro centro, respirar y proyectar la voz con confianza."),
        Pair("🔥 El calor del fuego", "Sentarse alrededor de una fogata en una noche fría tiene un efecto casi hipnótico. Las llamas danzantes invitan a la introspección y a compartir historias antiguas bajo un cielo estrellado y silencioso."),
        Pair("⛵ Navegar a vela", "Ajustar las velas para aprovechar la fuerza del viento requiere habilidad y respeto por la naturaleza. El marinero experto no lucha contra la tormenta, sino que utiliza las corrientes para llegar a salvo a su destino."),
        Pair("✍️ La escritura creativa", "Enfrentarse a una página en blanco es el primer paso de un viaje asombroso. Al soltar las ideas sin juzgarlas, permitimos que los personajes cobren vida y que la historia encuentre su propio camino hacia el lector."),
        Pair("🧬 Genética y futuro", "El estudio del ADN nos ha permitido entender la receta de la vida misma. Las nuevas tecnologías de edición genética prometen erradicar enfermedades hereditarias, abriendo un debate fascinante sobre la ética y nuestro futuro."),
        Pair("🏗️ La arquitectura moderna", "Los grandes rascacielos combinan la ingeniería de vanguardia con la visión artística. El uso del acero y el cristal permite crear estructuras que parecen desafiar la gravedad, redefiniendo el horizonte de nuestras ciudades."),
        Pair("🧘 Meditación guiada", "Cerrar los ojos y concentrarse únicamente en el sonido de la propia respiración es un ancla poderosa. La meditación nos enseña a observar nuestros pensamientos sin aferrarnos a ellos, cultivando una paz interior duradera."),
        Pair("🎙️ El arte del podcasting", "Crear un podcast nos permite compartir nuestras pasiones con el mundo entero. Una voz clara, una narrativa estructurada y un buen micrófono son suficientes para construir una comunidad leal de oyentes en torno a cualquier tema."),
        Pair("🔍 Depuración de errores", "Buscar un error en miles de líneas de código requiere una mentalidad detectivesca. Aislar el problema, revisar los registros y probar hipótesis sistemáticamente es la única forma de restaurar la funcionalidad de un sistema."),
        Pair("🚂 El viaje en tren", "Observar el paisaje a través de la ventana de un tren en movimiento tiene un encanto nostálgico. El traqueteo rítmico sobre las vías invita a la lectura sosegada y a dejar que los pensamientos vaguen libremente."),
        Pair("🏛️ La historia antigua", "Caminar por las ruinas de civilizaciones pasadas nos conecta con nuestras raíces. Entender cómo vivían, pensaban y construían nuestros ancestros nos da una perspectiva invaluable sobre los desafíos de nuestra sociedad actual."),
        Pair("🍎 Nutrición consciente", "Elegir alimentos frescos y naturales no solo nutre nuestro cuerpo, sino que fortalece nuestra mente. Una dieta equilibrada nos proporciona la energía sostenida necesaria para mantenernos enfocados durante las jornadas más exigentes."),
        Pair("🌐 La red global", "Internet ha conectado a la humanidad de formas que antes parecían de ciencia ficción. A través de cables submarinos y satélites, la información cruza océanos en milisegundos, democratizando el acceso al conocimiento universal."),
        Pair("🧩 Dinámicas de equipo", "El éxito de un proyecto rara vez depende de un solo individuo. La colaboración efectiva, la empatía y la comunicación abierta son los pilares que transforman a un grupo de personas en un equipo de alto rendimiento."),
        Pair("🎨 La teoría del color", "Comprender cómo los colores interactúan entre sí es fundamental para cualquier diseñador. Un buen contraste puede guiar la mirada del usuario, transmitir emociones profundas y mejorar significativamente la usabilidad de una interfaz."),
        Pair("🦅 El vuelo del águila", "Observar a un águila planear sobre los cañones es un espectáculo de majestuosidad pura. Sin mover apenas las alas, utiliza las corrientes térmicas ascendentes para elevarse y vigilar su territorio con una visión increíblemente aguda."),
        Pair("🖥️ Sistemas operativos", "El sistema operativo es el director de orquesta que coordina el hardware y el software. Asigna memoria, gestiona procesos y proporciona la base estable sobre la cual corren todas las aplicaciones que usamos a diario."),
        Pair("🧘‍♀️ Flexibilidad y yoga", "Practicar yoga no se trata solo de lograr posturas complejas, sino de conectar el movimiento con la respiración. Estirar los músculos con suavidad libera la tensión acumulada y mejora nuestra postura y bienestar general."),
        Pair("🚀 Startups tecnológicas", "Lanzar una nueva empresa tecnológica requiere asumir riesgos y tolerar la incertidumbre. La capacidad de iterar rápidamente, escuchar a los usuarios y pivotar cuando sea necesario es lo que separa a las ideas de los negocios exitosos."),
        Pair("❄️ El invierno nórdico", "Los inviernos cerca del círculo polar ártico son largos y oscuros. Sin embargo, la nieve inmaculada y el espectáculo luminoso de las auroras boreales compensan el frío extremo con una belleza natural sobrecogedora."),
        Pair("📖 La oratoria", "Dominar el arte de la oratoria implica mucho más que memorizar un discurso. Requiere el uso consciente de las pausas, la modulación del tono de voz y una conexión visual genuina para persuadir y conmover a la audiencia."),
        Pair("⚙️ Ingeniería de datos", "Transformar datos crudos en información estructurada es el motor de la economía actual. Construir tuberías de datos robustas asegura que la información fluya sin interrupciones hacia los tableros de análisis y toma de decisiones.")
    )

    DisposableEffect(context) {
        tts = TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                ttsInitialized = true
            }
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        onDispose {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.destroy()
            mediaRecorder?.release()
            mediaPlayer?.release()
            motorDSP.detenerAnalisis(null)
        }
    }

    LaunchedEffect(velocidadHabla, ttsInitialized) {
        if (ttsInitialized) {
            tts?.setSpeechRate(velocidadHabla)
        }
    }

    fun iniciarEscuchaSTTRitmo(textoObjetivo: String, nombreModulo: String) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                textoEscuchado = "Escuchando..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }
            override fun onError(error: Int) {
                isListening = false
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val textoDicho = matches?.firstOrNull()?.trim() ?: ""

                if (textoDicho.isBlank()) {
                    textoEscuchado = "No escuché nada. Intenta hablar más cerca."
                    return
                }

                textoEscuchado = textoDicho

                ultimaCalificacion = calcularPrecisionRitmo(textoObjetivo, textoEscuchado)

                ProgresoStorage.guardarSesionMultidimensional(
                    context,
                    nombreModulo,
                    if (ultimaCalificacion >= 60) 1 else 0,
                    if (ultimaCalificacion < 60) 1 else 0,
                    ultimaCalificacion
                )
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (seccionActual) {
                        SeccionRitmo.MENU_PRINCIPAL -> "Ritmo y Fluidez"
                        SeccionRitmo.BLOQUES -> "Bloques de Ritmo"
                        SeccionRitmo.LECTURA -> "Lectura Guiada"
                        SeccionRitmo.GRABADORA -> "Grabadora de Progreso"
                    })
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (seccionActual == SeccionRitmo.MENU_PRINCIPAL) {
                            onBack()
                        } else {
                            if (isListening) speechRecognizer?.stopListening()
                            motorDSP.detenerAnalisis(null)
                            isDSPActiveId = null
                            seccionActual = SeccionRitmo.MENU_PRINCIPAL
                        }
                    }) { Icon(ArrowLeftCircleIcon, "Regresar", tint = MaterialTheme.colorScheme.onSurface) }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(HomeIcon, "Menú", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regresar al Menú Principal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            BarraNavegacionModulos(
                onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                onNavigateRitmoFluidez = { /* Ya estás aquí */ },
                onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
                onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                onNavigateProgreso = { navController.navigate(Screen.Progreso.route) },
                onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) },
                onNavigateBio = { navController.navigate(Screen.Biofeedback.route) },
                onNavigateTarjetasArticulacion = { navController.navigate(Screen.TarjetasArticulacion.route) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (seccionActual) {
                SeccionRitmo.MENU_PRINCIPAL -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(85.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF8E24AA)))).clickable { textoEscuchado = ""; seccionActual = SeccionRitmo.BLOQUES }.padding(20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text("🧱 Bloques de Ritmo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Separa sílabas y evalúa tu precisión paso a paso", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth().height(85.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF006064), Color(0xFF00ACC1)))).clickable { textoEscuchado = ""; seccionActual = SeccionRitmo.LECTURA }.padding(20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text("📖 Lectura Guiada", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Lee párrafos con evaluación nativa (DSP)", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth().height(85.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF43A047)))).clickable { seccionActual = SeccionRitmo.GRABADORA }.padding(20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text("🎙️ Grabadora de Progreso", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Grábate libremente y detecta tus bloqueos", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }

                SeccionRitmo.BLOQUES, SeccionRitmo.LECTURA -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (seccionActual == SeccionRitmo.BLOQUES) {
                            MetronomoReutilizableComponent()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = nivelBloqueSeleccionado == NivelBloques.PRINCIPIANTE,
                                    onClick = { nivelBloqueSeleccionado = NivelBloques.PRINCIPIANTE },
                                    label = { Text("Fácil") }
                                )
                                FilterChip(
                                    selected = nivelBloqueSeleccionado == NivelBloques.INTERMEDIO,
                                    onClick = { nivelBloqueSeleccionado = NivelBloques.INTERMEDIO },
                                    label = { Text("Medio") }
                                )
                                FilterChip(
                                    selected = nivelBloqueSeleccionado == NivelBloques.AVANZADO,
                                    onClick = { nivelBloqueSeleccionado = NivelBloques.AVANZADO },
                                    label = { Text("Difícil") }
                                )
                            }
                        } else {
                            ControlVelocidadTts(velocidadActual = velocidadHabla, onVelocidadChange = { velocidadHabla = it })
                        }

                        if (textoEscuchado.isNotEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Reconocido: \"$textoEscuchado\"", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Precisión: $ultimaCalificacion%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        val listaMostrada = if (seccionActual == SeccionRitmo.BLOQUES) {
                            when (nivelBloqueSeleccionado) {
                                NivelBloques.PRINCIPIANTE -> bloquesPrincipiante
                                NivelBloques.INTERMEDIO -> bloquesIntermedio
                                NivelBloques.AVANZADO -> bloquesAvanzado
                            }
                        } else {
                            lecturasExtensas.map { it.second }
                        }

                        val titulosMostrados = if (seccionActual == SeccionRitmo.LECTURA) lecturasExtensas.map { it.first } else emptyList()
                        val colorFondoBase = if (seccionActual == SeccionRitmo.BLOQUES) Color(0xFF4A148C) else Color(0xFF006064)
                        val colorFondoDegradado = if (seccionActual == SeccionRitmo.BLOQUES) Color(0xFF8E24AA) else Color(0xFF00ACC1)

                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listaMostrada.size) { index ->
                                val contenido = listaMostrada[index]
                                val titulo = titulosMostrados.getOrNull(index)
                                val isThisDSPActive = isDSPActiveId == contenido

                                // Lógica visual de tensión
                                val fondoTarjeta = if (tensionDetectada && isThisDSPActive) {
                                    Brush.linearGradient(listOf(Color(0xFFC62828), Color(0xFFD32F2F)))
                                } else {
                                    Brush.linearGradient(listOf(colorFondoBase, colorFondoDegradado))
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(x = if (isThisDSPActive) offsetX.value.dp else 0.dp) // Aplicamos la animación
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(fondoTarjeta)
                                        .padding(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (titulo != null) {
                                            Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(contenido, fontSize = 14.sp, color = Color.White.copy(alpha = 0.95f), lineHeight = 20.sp)
                                        } else {
                                            Text(contenido, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                        }

                                        if (isThisDSPActive) {
                                            BiofeedbackVisualAvanzadoDSP(metricasDSP)
                                            Text(feedbackDSP, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                                            MonitorFonologicoYCAA(
                                                motorDSP = motorDSP,
                                                palabraObjetivoActual = contenido,
                                                textoEscuchado = textoEscuchado
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { iniciarEscuchaSTTRitmo(contenido, if (seccionActual == SeccionRitmo.BLOQUES) "Ritmo (Bloques)" else "Ritmo (Lectura Guiada)") },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color.White),
                                                modifier = Modifier.weight(1f),
                                                enabled = isDSPActiveId == null
                                            ) {
                                                Text(
                                                    text = if (isListening) "⏹️ Voz" else "📝 Texto STT",
                                                    color = if (isListening) Color.White else colorFondoBase,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                        return@Button
                                                    }

                                                    if (isThisDSPActive) {
                                                        motorDSP.detenerAnalisis(context, if (seccionActual == SeccionRitmo.BLOQUES) "Ritmo DSP (Bloques)" else "Ritmo DSP (Lectura)")
                                                        isDSPActiveId = null
                                                    } else {
                                                        feedbackDSP = "Esperando voz..."
                                                        motorDSP.detenerAnalisis(null)
                                                        motorDSP.iniciarAnalisisDSP()
                                                        isDSPActiveId = contenido
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                enabled = !isListening,
                                                colors = ButtonDefaults.buttonColors(containerColor = if (isThisDSPActive) Color(0xFFD32F2F) else Color(0xFF00897B))
                                            ) {
                                                Text(if (isThisDSPActive) "⏹️ Guardar DSP" else "🎛️ Activar DSP", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SeccionRitmo.GRABADORA -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Grabadora de Progreso", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Grábate libremente. Activa el DSP para detectar bloqueos durante tu grabación.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

                        if (isDSPActiveId != null) {
                            BiofeedbackVisualAvanzadoDSP(metricasDSP)
                            Text(feedbackDSP, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        // Lógica visual de tensión para la grabadora
                        val colorFondoGrabadora = if (tensionDetectada) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.surfaceVariant

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .offset(x = if (isDSPActiveId != null) offsetX.value.dp else 0.dp), // Aplicamos animación
                            colors = CardDefaults.cardColors(containerColor = colorFondoGrabadora),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                val estadoTexto = when {
                                    isRecordingLibre -> "🔴 Grabando audio..."
                                    isPlayingLibre -> "🔊 Reproduciendo..."
                                    else -> "Lista para grabar"
                                }
                                Text(estadoTexto, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isRecordingLibre) Color.Red else MaterialTheme.colorScheme.onSurface)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    FloatingActionButton(
                                        onClick = {
                                            if (isPlayingLibre) {
                                                Toast.makeText(context, "Detén la reproducción primero", Toast.LENGTH_SHORT).show()
                                                return@FloatingActionButton
                                            }
                                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                return@FloatingActionButton
                                            }

                                            if (isRecordingLibre) {
                                                try {
                                                    mediaRecorder?.stop()
                                                    mediaRecorder?.release()
                                                    mediaRecorder = null
                                                    isRecordingLibre = false
                                                    motorDSP.detenerAnalisis(context, "Grabación Libre DSP")
                                                    isDSPActiveId = null
                                                    Toast.makeText(context, "Grabación guardada", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            } else {
                                                try {
                                                    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
                                                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                                                    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                                    recorder.setOutputFile(audioFile.absolutePath)
                                                    recorder.prepare()
                                                    recorder.start()
                                                    mediaRecorder = recorder
                                                    isRecordingLibre = true
                                                    feedbackDSP = "Analizando bloqueos..."
                                                    motorDSP.iniciarAnalisisDSP()
                                                    isDSPActiveId = "GRABADORA"
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        },
                                        containerColor = if (isRecordingLibre) Color.Red else MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ) {
                                        Icon(if (isRecordingLibre) Icons.Default.Stop else Icons.Default.Mic, "Grabar", modifier = Modifier.size(32.dp))
                                    }

                                    FloatingActionButton(
                                        onClick = {
                                            if (isRecordingLibre) {
                                                Toast.makeText(context, "Detén la grabación primero", Toast.LENGTH_SHORT).show()
                                                return@FloatingActionButton
                                            }

                                            if (isPlayingLibre) {
                                                mediaPlayer?.stop()
                                                mediaPlayer?.release()
                                                mediaPlayer = null
                                                isPlayingLibre = false
                                            } else {
                                                if (audioFile.exists()) {
                                                    try {
                                                        val player = MediaPlayer()
                                                        player.setDataSource(audioFile.absolutePath)
                                                        player.prepare()
                                                        player.start()
                                                        player.setOnCompletionListener { isPlayingLibre = false }
                                                        mediaPlayer = player
                                                        isPlayingLibre = true
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "No hay grabación guardada", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        containerColor = if (isPlayingLibre) Color(0xFFF57C00) else MaterialTheme.colorScheme.secondary,
                                        contentColor = Color.White
                                    ) {
                                        Icon(if (isPlayingLibre) Icons.Default.Stop else Icons.Default.PlayArrow, "Reproducir", modifier = Modifier.size(32.dp))
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (isRecordingLibre || isPlayingLibre) { return@Button }
                                        if (audioFile.exists()) {
                                            try {
                                                val valores = ContentValues().apply {
                                                    put(MediaStore.MediaColumns.DISPLAY_NAME, "SpeakTutor_${System.currentTimeMillis()}.m4a")
                                                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                                    }
                                                }
                                                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
                                                if (uri != null) {
                                                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                                        FileInputStream(audioFile).use { inputStream ->
                                                            inputStream.copyTo(outputStream)
                                                        }
                                                    }
                                                    Toast.makeText(context, "✅ Descargado en 'Descargas'", Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    enabled = !isRecordingLibre,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD))
                                ) {
                                    Icon(Icons.Default.Download, "Descargar")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Descargar Audio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}