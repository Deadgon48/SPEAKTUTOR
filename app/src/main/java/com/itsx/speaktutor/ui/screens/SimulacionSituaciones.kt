package com.itsx.speaktutor.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.itsx.speaktutor.logic.MotorAudioDSP
import com.itsx.speaktutor.logic.EstadoVozDSP
import com.itsx.speaktutor.ui.components.ArrowLeftCircleIcon
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.BiofeedbackVisualAvanzadoDSP
import com.itsx.speaktutor.ui.components.HomeIcon
import com.itsx.speaktutor.ui.components.MonitorFonologicoYCAA
import com.itsx.speaktutor.ui.navigation.Screen

enum class SimulacionSituaciones {
    MENU_PRINCIPAL, ESCENARIOS, GUIONES, EVOCACION_LEXICA
}

enum class FaseGuion {
    SELECCION, CHECK_IN, PRACTICA, CHECK_OUT, RESULTADOS
}

enum class EstadoEmocional(val icono: String, val texto: String, val color: Color) {
    TRANQUILO("😌", "Tranquilo", Color(0xFF2E7D32)),
    NERVIOSO("😬", "Nervioso", Color(0xFFF57C00)),
    ANSIOSO("😰", "Ansioso", Color(0xFFC62828)),
    NINGUNO("", "", Color.Transparent)
}

data class TurnoGuion(val rol: String, val texto: String, val esDeApp: Boolean)
data class GuionPractica(val titulo: String, val descripcion: String, val dialogos: List<TurnoGuion>)

val diccionariosEvocacion = mapOf(
    "ANIMALES" to listOf("perro", "gato", "león", "tigre", "elefante", "pájaro", "oso", "caballo", "vaca", "cerdo", "lobo", "zorro", "conejo", "serpiente", "mono", "rana", "pez", "jirafa", "rinoceronte", "hipopótamo", "cocodrilo", "águila", "ballena", "delfín", "tiburón", "tortuga", "pingüino", "cebra", "camello", "canguro", "koala", "hiena", "pantera", "guepardo", "gorila", "chimpancé", "ardilla", "castor", "nutria", "foca", "morsa", "alce", "ciervo", "buey", "oveja", "cabra", "gallina", "pato", "ganso", "pavo"),
    "COLORES" to listOf("rojo", "azul", "verde", "amarillo", "blanco", "negro", "naranja", "morado", "rosa", "café", "marrón", "gris", "celeste", "violeta", "turquesa", "beige", "dorado", "plateado", "fucsia", "magenta", "cian", "índigo", "lila", "burdeos", "carmesí", "zafiro", "coral", "ámbar", "ocre", "mostaza", "salmón", "oliva", "lavanda", "cobrizo", "platino", "amatista", "rubí", "esmeralda", "marfil", "perlado", "ébano", "caoba", "granate", "terracota", "aguamarina", "pistacho", "vainilla", "bronce", "índigo", "cian"),
    "FRUTAS" to listOf("manzana", "plátano", "naranja", "uva", "pera", "fresa", "sandía", "melón", "mango", "piña", "kiwi", "limón", "papaya", "cereza", "ciruela", "durazno", "frambuesa", "arándano", "coco", "guayaba", "mandarina", "toronja", "lima", "zarzamora", "higo", "granada", "maracuyá", "lichi", "mora", "nectarina", "mamey", "tamarindo", "chirimoya", "guanábana", "pitaya", "carambola", "pomelo", "níspero", "membrillo", "dátil", "aguacate", "tomate", "zapote", "tuna", "tejocote", "capulín", "chicozapote", "grosella", "persimón", "maracuyá"),
    "PAÍSES" to listOf("méxico", "españa", "argentina", "colombia", "chile", "perú", "francia", "italia", "alemania", "japón", "china", "brasil", "canadá", "ecuador", "venezuela", "bolivia", "uruguay", "paraguay", "rusia", "india", "australia", "inglaterra", "irlanda", "escocia", "gales", "portugal", "grecia", "turquía", "suecia", "noruega", "finlandia", "dinamarca", "suiza", "bélgica", "holanda", "polonia", "ucrania", "corea", "tailandia", "vietnam", "filipinas", "indonesia", "egipto", "marruecos", "kenia", "sudáfrica", "cuba", "jamaica", "panamá", "guatemala"),
    "PROFESIONES" to listOf("doctor", "médico", "ingeniero", "abogado", "profesor", "maestro", "arquitecto", "enfermera", "policía", "bombero", "carpintero", "piloto", "dentista", "cocinero", "chef", "mecánico", "electricista", "periodista", "psicólogo", "veterinario", "contador", "administrador", "diseñador", "programador", "astronauta", "científico", "investigador", "escritor", "pintor", "músico", "actor", "director", "fotógrafo", "traductor", "secretario", "recepcionista", "cajero", "vendedor", "mesero", "albañil", "plomero", "jardinero", "panadero", "carnicero", "peluquero", "barbero", "sastre", "chofer", "militar", "marinero"),
    "DEPORTES" to listOf("fútbol", "basquetbol", "béisbol", "tenis", "natación", "atletismo", "boxeo", "voleibol", "golf", "ciclismo", "gimnasia", "rugby", "surf", "esquí", "taekwondo", "karate", "judo", "esgrima", "lucha", "halterofilia", "remo", "canotaje", "vela", "waterpolo", "clavados", "bádminton", "ping-pong", "squash", "hockey", "patinaje", "alpinismo", "escalada", "senderismo", "triatlón", "maratón", "ajedrez", "billar", "boliche", "automovilismo", "motociclismo", "equitación", "polo", "críquet", "lacrosse", "softbol", "balonmano", "parkour", "capoeira", "tiro", "snowboard"),
    "VERDURAS" to listOf("tomate", "cebolla", "ajo", "zanahoria", "papa", "lechuga", "brócoli", "espinaca", "calabaza", "pepino", "chile", "pimiento", "apio", "aguacate", "chícharo", "col", "repollo", "coliflor", "berenjena", "calabacín", "rábano", "nabo", "betabel", "remolacha", "camote", "yuca", "espárrago", "alcachofa", "puerro", "poro", "cilantro", "perejil", "albahaca", "menta", "acelga", "berro", "chayote", "ejote", "judía", "vaina", "elote", "maíz", "jengibre", "hinojo", "champiñón", "hongo", "seta", "rúcula", "kale", "palmito"),
    "HERRAMIENTAS" to listOf("martillo", "desarmador", "destornillador", "pinzas", "alicate", "llave", "taladro", "sierra", "clavo", "tornillo", "tuerca", "metro", "cincel", "formón", "lima", "lija", "cepillo", "serrucho", "segueta", "matraca", "dado", "esmeril", "pulidora", "soldadora", "nivel", "escuadra", "plomada", "regla", "compás", "cúter", "navaja", "tijeras", "brocha", "rodillo", "espátula", "pala", "pico", "azadón", "rastrillo", "manguera", "carretilla", "yunque", "torno", "fresadora", "gato", "multímetro", "cautín", "soplete", "remachadora", "grapadora"),
    "VEHÍCULOS" to listOf("coche", "auto", "automóvil", "autobús", "camión", "motocicleta", "moto", "bicicleta", "tren", "avión", "barco", "helicóptero", "submarino", "yate", "lancha", "bote", "velero", "balsa", "canoa", "kayak", "crucero", "transbordador", "tranvía", "metro", "ferrocarril", "furgoneta", "camioneta", "tráiler", "tractor", "excavadora", "grúa", "ambulancia", "patrulla", "taxi", "limusina", "triciclo", "monopatín", "patineta", "scooter", "dirigible", "cohete", "planeador", "teleférico", "carroza", "carreta", "trineo", "cuatrimoto", "zeppelin", "góndola", "catamarán"),
    "INSTRUMENTOS" to listOf("guitarra", "piano", "violín", "batería", "flauta", "trompeta", "saxofón", "bajo", "teclado", "violonchelo", "arpa", "acordeón", "clarinete", "oboe", "fagot", "trombón", "tuba", "trompa", "corneta", "armónica", "órgano", "sintetizador", "contrabajo", "viola", "ukelele", "mandolina", "laúd", "banjo", "cítara", "lira", "timbal", "tambor", "bombo", "platillo", "pandereta", "maraca", "güiro", "xilófono", "marimba", "campana", "triángulo", "castañuela", "cajón", "gaita", "clavecín", "celesta", "theremin", "ocarina", "bongo", "conga"),
    "ROPA" to listOf("camisa", "camiseta", "pantalón", "jeans", "zapato", "tenis", "chamarra", "chaqueta", "abrigo", "sombrero", "gorra", "calcetín", "vestido", "falda", "suéter", "bufanda", "guante", "blusa", "short", "bermuda", "traje", "corbata", "pajarita", "chaleco", "gabardina", "impermeable", "poncho", "capa", "chal", "pashmina", "cinturón", "cinto", "bota", "botín", "sandalia", "chancla", "pantufla", "mocasín", "tacón", "pijama", "bata", "calzón", "brasier", "bikini", "mallón", "legging", "overol", "mameluco", "túnica", "corsé"),
    "MUEBLES" to listOf("silla", "mesa", "sillón", "sofá", "cama", "ropero", "armario", "escritorio", "estante", "librero", "buró", "espejo", "comedor", "vitrina", "alacena", "cajonera", "cómoda", "tocador", "taburete", "banco", "banqueta", "mecedora", "hamaca", "cuna", "corral", "litera", "colchón", "cabecera", "mesita", "perchero", "zapatero", "repisa", "consola", "credenza", "biombo", "baúl", "otomana", "diván", "puf", "secreter", "archivero", "pupitre", "encimera", "mostrador", "estantería", "carrito", "trinchador", "bidet", "lavabo", "inodoro"),
    "ACCIONES" to listOf("correr", "saltar", "nadar", "leer", "caminar", "dormir", "comer", "escribir", "bailar", "cantar", "jugar", "estudiar", "viajar", "reír", "llorar", "sonreír", "pensar", "hablar", "escuchar", "mirar", "ver", "oler", "tocar", "sentir", "amar", "odiar", "trabajar", "descansar", "comprar", "vender", "cocinar", "limpiar", "lavar", "planchar", "barrer", "trapear", "conducir", "manejar", "volar", "pintar", "dibujar", "construir", "destruir", "armar", "desarmar", "abrir", "cerrar", "encender", "apagar", "subir"),
    "EMOCIONES" to listOf("alegría", "tristeza", "enojo", "sorpresa", "miedo", "amor", "calma", "ansiedad", "paz", "angustia", "felicidad", "furia", "ira", "rabia", "euforia", "melancolía", "depresión", "terror", "pánico", "horror", "asco", "repugnancia", "desprecio", "culpa", "vergüenza", "orgullo", "satisfacción", "alivio", "esperanza", "desesperación", "aburrimiento", "apatía", "interés", "curiosidad", "compasión", "empatía", "simpatía", "envidia", "celos", "gratitud", "admiración", "asombro", "decepción", "frustración", "impaciencia", "nostalgia", "añoranza", "ternura", "cariño", "pasión")
)

val diccionarioAntonimos = mapOf(
    "Día" to listOf("noche"),
    "Frío" to listOf("calor", "caliente"),
    "Rápido" to listOf("lento", "despacio"),
    "Feliz" to listOf("triste", "infeliz"),
    "Arriba" to listOf("abajo"),
    "Blanco" to listOf("negro"),
    "Grande" to listOf("chico", "pequeño"),
    "Lleno" to listOf("vacío"),
    "Largo" to listOf("corto"),
    "Caro" to listOf("barato"),
    "Alto" to listOf("bajo", "chaparro"),
    "Fuerte" to listOf("débil"),
    "Duro" to listOf("blando", "suave"),
    "Claro" to listOf("oscuro"),
    "Bueno" to listOf("malo", "pésimo"),
    "Nuevo" to listOf("viejo", "antiguo"),
    "Joven" to listOf("viejo", "anciano", "mayor"),
    "Rico" to listOf("pobre"),
    "Gordo" to listOf("flaco", "delgado"),
    "Limpio" to listOf("sucio"),
    "Fácil" to listOf("difícil", "complejo"),
    "Abierto" to listOf("cerrado"),
    "Ganar" to listOf("perder"),
    "Verdad" to listOf("mentira", "falsedad"),
    "Amor" to listOf("odio"),
    "Cerca" to listOf("lejos"),
    "Dentro" to listOf("fuera", "afuera"),
    "Luz" to listOf("oscuridad", "sombra"),
    "Comprar" to listOf("vender"),
    "Dar" to listOf("recibir", "quitar"),
    "Empezar" to listOf("terminar", "acabar"),
    "Subir" to listOf("bajar", "descender"),
    "Reír" to listOf("llorar"),
    "Valiente" to listOf("cobarde", "miedoso"),
    "Temprano" to listOf("tarde"),
    "Sano" to listOf("enfermo"),
    "Mojado" to listOf("seco"),
    "Liso" to listOf("rugoso", "áspero"),
    "Ancho" to listOf("estrecho", "angosto"),
    "Lindo" to listOf("feo", "horrible"),
    "Inteligente" to listOf("tonto", "ignorante"),
    "Útil" to listOf("inútil"),
    "Cierto" to listOf("falso"),
    "Silencio" to listOf("ruido", "bullicio"),
    "Paz" to listOf("guerra", "conflicto"),
    "Vida" to listOf("muerte"),
    "Principio" to listOf("fin", "final"),
    "Primero" to listOf("último"),
    "Norte" to listOf("sur"),
    "Este" to listOf("oeste")
)

fun calcularPrecisionHabla(textoEsperado: String, textoReconocido: String): Int {
    if (textoReconocido.isBlank()) return 0
    val palabrasEsperadas = textoEsperado.lowercase().replace(Regex("[^a-záéíóúñ ]"), "").split("\\s+".toRegex())
    val palabrasReconocidas = textoReconocido.lowercase().replace(Regex("[^a-záéíóúñ ]"), "").split("\\s+".toRegex())
    var coincidencias = 0
    for (palabra in palabrasReconocidas) {
        if (palabrasEsperadas.contains(palabra)) coincidencias++
    }
    return ((coincidencias.toFloat() / palabrasEsperadas.size.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulacionSituacionesScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    var seccionActual by remember { mutableStateOf(SimulacionSituaciones.MENU_PRINCIPAL) }

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    var tensionDetectada by remember { mutableStateOf(false) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var textoEscuchado by remember { mutableStateOf("") }
    var ultimaCalificacion by remember { mutableStateOf(0) }

    // ESTADOS DE GUIONES Y EMOCIONES
    var faseGuion by remember { mutableStateOf(FaseGuion.SELECCION) }
    var guionSeleccionado by remember { mutableStateOf<GuionPractica?>(null) }
    var emocionInicial by remember { mutableStateOf(EstadoEmocional.NINGUNO) }
    var emocionFinal by remember { mutableStateOf(EstadoEmocional.NINGUNO) }
    var indiceGuionActual by remember { mutableStateOf(0) }
    var aciertosGuion by remember { mutableStateOf(0) }
    var erroresGuion by remember { mutableStateOf(0) }

    // ESTADO DEL "TIEMPO FUERA"
    var mostrarPausaRespiracion by remember { mutableStateOf(false) }

    var escenarioSeleccionadoIndex by remember { mutableStateOf(0) }

    val motorDSP = remember { MotorAudioDSP() }
    val metricasDSP by motorDSP.metricasAcusticas.collectAsState()
    var isDSPActive by remember { mutableStateOf(false) }

    // ESTADOS DE EVOCACIÓN
    val TIEMPO_MAXIMO_EVOCACION = 30
    var tiempoRestante by remember { mutableStateOf(TIEMPO_MAXIMO_EVOCACION) }
    var estadoEvocacion by remember { mutableStateOf("INICIO") }
    var modoEvocacion by remember { mutableStateOf("CATEGORIAS") }
    val categoriasEvocacion = diccionariosEvocacion.keys.toList()
    var categoriaActual by remember { mutableStateOf(categoriasEvocacion.random()) }
    var antonimoActual by remember { mutableStateOf(diccionarioAntonimos.keys.random()) }
    var aciertosEvocacion by remember { mutableIntStateOf(0) }
    var erroresEvocacion by remember { mutableIntStateOf(0) }
    var pistaActual by remember { mutableStateOf("") }
    var pausarTimer by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // LISTA DE ESCENARIOS RÁPIDOS (50 Ejercicios de 1 Turno)
    val listaEscenarios = listOf(
        Triple("☕ Pedir un café", "Hola, buenos días. ¿Qué le gustaría ordenar hoy?", "Hola, quisiera un café americano bien caliente por favor."),
        Triple("🛒 En el supermercado", "Hola, ¿encontró todo lo que buscaba?", "Sí, encontré todo lo que buscaba, muchas gracias."),
        Triple("🚌 Dirección en la calle", "Disculpe, ¿sabe por dónde queda el metro?", "Siga derecho dos cuadras y dé vuelta a la izquierda."),
        Triple("💊 Farmacia", "Buenas tardes, ¿en qué le puedo servir el día de hoy?", "Buenas tardes, busco una caja de analgésicos por favor."),
        Triple("🚕 Taxi", "Buenas tardes, ¿a dónde le llevo?", "Buenas tardes, a la estación de autobuses por favor."),
        Triple("🥖 Panadería", "¿Qué le damos el día de hoy?", "Me da dos panes dulces y una baguette, por favor."),
        Triple("🏦 Banco", "Siguiente en la fila. ¿Qué trámite va a realizar?", "Vengo a hacer un depósito en efectivo a esta cuenta."),
        Triple("⌚ Preguntar la hora", "Disculpe, ¿necesita algo?", "Sí, disculpe, ¿tendrá la hora exacta?"),
        Triple("🍽️ Restaurante", "¿Listos para ordenar o necesitan más tiempo?", "Ya estamos listos, yo quiero la ensalada César."),
        Triple("📞 Teléfono (Desconocido)", "Bueno, ¿quién habla?", "Hola, soy Humberto, ¿se encuentra Roberto?"),
        Triple("🎟️ Cine", "¿Para qué función busca boletos?", "Dos boletos para la película de las seis, por favor."),
        Triple("👕 Tienda de Ropa", "¿Gusta que le ayude a buscar alguna talla?", "No, muchas gracias, por el momento solo estoy mirando."),
        Triple("🙋 Saludar al vecino", "¡Hola! Qué calor hace hoy, ¿verdad?", "¡Hola! Sí, la verdad está haciendo muchísimo calor."),
        Triple("📦 Paquetería", "Vengo a entregar un paquete a nombre de Carlos.", "Soy yo, muchas gracias. Ahorita le firmo de recibido."),
        Triple("💇 Peluquería", "¿Cómo le cortamos el cabello hoy?", "Solo las puntas y un poco corto de los lados, por favor."),
        Triple("🚗 Gasolinera", "¿De cuál le ponemos y de a cuánto?", "Lleno de magna, por favor."),
        Triple("📱 Tienda de electrónica", "¿Busca algún modelo de teléfono en especial?", "Estoy buscando fundas para este modelo en específico."),
        Triple("🎫 Estacionamiento", "Serían cincuenta pesos por el boleto perdido.", "Aquí tiene, ¿me da mi recibo por favor?"),
        Triple("🏨 Recepción rápida", "Aquí tiene su llave, habitación trescientos dos.", "Muchas gracias, ¿el ascensor está por allá?"),
        Triple("🏥 Consultorio médico", "El doctor lo atenderá en unos minutos, tome asiento.", "De acuerdo, muchas gracias, aquí espero."),
        Triple("🎓 Biblioteca", "¿Va a llevarse estos libros a préstamo?", "Sí, por favor, aquí le entrego mi credencial."),
        Triple("🏃‍♂️ Gimnasio", "Disculpa, ¿estás usando esta máquina?", "No, ya terminé, adelante puedes usarla."),
        Triple("🛍️ Zapatería", "¿Le traigo medio número más grande?", "Sí por favor, me quedan un poco ajustados."),
        Triple("🐶 Veterinaria", "¿Qué le pasa al perrito hoy?", "Le toca su vacuna anual y una revisión general."),
        Triple("🍦 Heladería", "¿De qué sabor le preparo su helado?", "Uno doble de chocolate y vainilla en cono, por favor."),
        Triple("📬 Oficina de correos", "¿Qué tipo de envío desea para su paquete?", "Quiero el envío estándar con número de rastreo, por favor."),
        Triple("🏛️ Museo", "La entrada general cuesta cien pesos.", "Aquí tiene, ¿podría darme un mapa del museo?"),
        Triple("🎫 Concierto", "Por favor, muestre su boleto en la pantalla.", "Claro, aquí está el código con el brillo al máximo."),
        Triple("✈️ Aeropuerto", "¿Lleva equipaje para documentar o solo de mano?", "Solo llevo esta maleta de mano y mi mochila."),
        Triple("🛂 Aduana", "¿Cuál es el motivo de su viaje a este país?", "Vengo de vacaciones por una semana para visitar la ciudad."),
        Triple("🍔 Comida Rápida", "¿Desea agrandar su combo por quince pesos más?", "No, muchas gracias, así está perfecto."),
        Triple("🍽️ Pedir la cuenta", "¿Todo estuvo bien con su comida?", "Todo excelente, ¿me podría traer la cuenta por favor?"),
        Triple("💳 Pagar con tarjeta", "¿Va a pagar con tarjeta de crédito o débito?", "Con tarjeta de débito, por favor. ¿Es de insertar?"),
        Triple("🚻 Preguntar por el baño", "Disculpe, ¿le puedo ayudar en algo?", "Sí, ¿me podría indicar dónde se encuentran los sanitarios?"),
        Triple("🖊️ Pedir prestado", "Claro que sí, ¿necesitas algo más?", "Nada más, ¿de casualidad tendrás un bolígrafo que me prestes?"),
        Triple("🤝 Presentación", "Hola, mucho gusto, soy el nuevo gerente de ventas.", "Mucho gusto, mi nombre es Humberto, bienvenido al equipo."),
        Triple("📶 Pedir Wi-Fi", "¿Desea ordenar algún postre con su café?", "No gracias. Disculpe, ¿cuál es la contraseña del Wi-Fi?"),
        Triple("📰 Puesto de revistas", "Buenos días, ¿buscaba el periódico de hoy?", "Buenos días, sí, me da un ejemplar del periódico local."),
        Triple("💐 Florería", "¿Para qué ocasión busca el arreglo floral?", "Es para un aniversario, busco algo con rosas rojas."),
        Triple("📅 Reservar mesa", "Restaurante La Loma, ¿para cuántas personas sería?", "Quisiera reservar una mesa para cuatro personas a las ocho."),
        Triple("💸 Pedir descuento", "El total de estas prendas sería de mil pesos.", "Disculpe, ¿tienen algún descuento de estudiante aplicable?"),
        Triple("🏫 Escuela", "Hola, ¿vienes a recoger a tu hermano?", "Sí, estoy esperando a que salga de su clase."),
        Triple("🐕 Paseando al perro", "¡Qué bonito perro! ¿Es muy bravo?", "No, es muy amigable. Puede acariciarlo si gusta."),
        Triple("🙏 Pedir un favor", "Dime, ¿en qué te puedo ayudar?", "Quería pedirte el favor de revisar este documento rápido."),
        Triple("🙅‍♂️ Rechazar oferta", "Señor, le ofrezco cambiar de compañía telefónica hoy mismo.", "Le agradezco la oferta, pero no estoy interesado por ahora."),
        Triple("💼 Recepción de oficina", "Buenas tardes, ¿tiene cita con el licenciado?", "Buenas tardes, sí, tengo cita a las tres de la tarde."),
        Triple("🛠️ Ferretería", "¿De qué medida busca los tornillos?", "Busco tornillos de dos pulgadas para madera, por favor."),
        Triple("🎟️ Metro", "La taquilla está cerrada, solo hay máquinas.", "Disculpe, ¿sabe si la máquina da cambio de billetes grandes?"),
        Triple("🎭 Teatro", "Sus lugares están en la fila H, del lado derecho.", "Muchas gracias, ¿sabe cuánto dura el intermedio?"),
        Triple("🌧️ Clima", "Parece que va a llover muy fuerte en un rato.", "Sí, el cielo está muy nublado. Qué bueno que traje paraguas.")
    )

    // JERARQUÍA DE GUIONES (30 Guiones de Desensibilización Sistemática)
    val jerarquiaGuiones = listOf(
        // ==========================================
        // NIVEL 1: BAJA DEMANDA SOCIAL
        // ==========================================
        GuionPractica("🛎️ Recepción de Hotel", "Baja demanda social. Interacción de rutina.", listOf(
            TurnoGuion("Recepcionista:", "Bienvenido al hotel. ¿Tiene una reserva a su nombre?", true),
            TurnoGuion("Tú:", "Sí, reservé una habitación individual para dos noches.", false),
            TurnoGuion("Recepcionista:", "Perfecto, ¿me proporciona una identificación?", true),
            TurnoGuion("Tú:", "Claro que sí, aquí tiene mi credencial.", false)
        )),
        GuionPractica("🎫 Comprar Boletos de Autobús", "Baja demanda. Conversación transaccional predecible.", listOf(
            TurnoGuion("Taquillero:", "Buenas tardes, ¿a dónde viaja el día de hoy?", true),
            TurnoGuion("Tú:", "Buenas tardes. Quiero un boleto para la Ciudad de México.", false),
            TurnoGuion("Taquillero:", "Tengo salidas a las dos y a las cuatro. ¿Cuál prefiere?", true),
            TurnoGuion("Tú:", "El de las dos de la tarde, por favor. Pago con tarjeta.", false)
        )),
        GuionPractica("🍿 Taquilla del Cine", "Baja demanda. Entorno ruidoso pero predecible.", listOf(
            TurnoGuion("Taquilla:", "Hola, ¿qué película vas a ver?", true),
            TurnoGuion("Tú:", "Hola, quiero dos boletos para la película de acción.", false),
            TurnoGuion("Taquilla:", "Me quedan lugares en la fila G o hasta el frente en la fila A.", true),
            TurnoGuion("Tú:", "En la fila G está perfecto, en los asientos del centro.", false)
        )),
        GuionPractica("📚 Buscar un libro en la biblioteca", "Baja demanda. Pedir información simple.", listOf(
            TurnoGuion("Bibliotecario:", "Buenas tardes, ¿buscas algún título en especial?", true),
            TurnoGuion("Tú:", "Sí, estoy buscando libros sobre historia universal.", false),
            TurnoGuion("Bibliotecario:", "Claro, esos están en el pasillo cuatro, sección B.", true),
            TurnoGuion("Tú:", "Muchas gracias, iré a buscar en esa sección.", false)
        )),
        GuionPractica("🍔 Autoconsumo de Comida Rápida", "Baja demanda. Presión de tiempo moderada.", listOf(
            TurnoGuion("Bocina:", "Bienvenido, ¿toma su orden por favor?", true),
            TurnoGuion("Tú:", "Hola, quiero el combo número tres, sin cebolla.", false),
            TurnoGuion("Bocina:", "Combo tres sin cebolla. ¿De tomar qué le ofrezco?", true),
            TurnoGuion("Tú:", "Un refresco de cola grande, con mucho hielo.", false)
        )),
        GuionPractica("🏦 Cambiar Billetes en el Banco", "Baja demanda. Explicar una necesidad transaccional.", listOf(
            TurnoGuion("Cajero:", "Siguiente. ¿Qué trámite desea realizar?", true),
            TurnoGuion("Tú:", "Hola, necesito cambiar este billete de quinientos por billetes más pequeños.", false),
            TurnoGuion("Cajero:", "¿Cómo quiere que se lo dé? ¿De a cien o de a cincuenta?", true),
            TurnoGuion("Tú:", "Deme cuatro de a cien y dos de cincuenta, por favor.", false)
        )),
        GuionPractica("🍩 Pedido grande en panadería", "Baja demanda. Enunciar una lista de elementos.", listOf(
            TurnoGuion("Vendedor:", "Hola, ¿qué le empacamos hoy?", true),
            TurnoGuion("Tú:", "Hola, necesito pedir varias cosas para una reunión familiar.", false),
            TurnoGuion("Vendedor:", "Claro, dígame qué le pongo en la charola.", true),
            TurnoGuion("Tú:", "Diez conchas, cinco cuernos y una gelatina grande de fresa.", false)
        )),
        GuionPractica("🛒 Pidiendo indicaciones en tienda", "Baja demanda. Iniciar la interacción con un desconocido.", listOf(
            TurnoGuion("Tú:", "Disculpe, ¿trabaja usted aquí?", false),
            TurnoGuion("Empleado:", "Sí, dígame, ¿en qué le puedo ayudar?", true),
            TurnoGuion("Tú:", "Estoy buscando el pasillo de los detergentes y jabones.", false),
            TurnoGuion("Empleado:", "Están al fondo a la derecha, en el pasillo nueve.", true),
            TurnoGuion("Tú:", "Perfecto, muchas gracias por la ayuda.", false)
        )),

        // ==========================================
        // NIVEL 2: DEMANDA MEDIA
        // ==========================================
        GuionPractica("🍕 Pedir Pizza (Teléfono)", "Demanda media. El teléfono elimina contacto visual.", listOf(
            TurnoGuion("Pizzería:", "Pizzería Luigi, buenas noches. ¿En qué le puedo ayudar?", true),
            TurnoGuion("Tú:", "Buenas noches. Quisiera pedir una pizza a domicilio.", false),
            TurnoGuion("Pizzería:", "Claro. ¿De qué especialidad y tamaño sería?", true),
            TurnoGuion("Tú:", "Una pizza grande de pepperoni con orilla de queso.", false),
            TurnoGuion("Pizzería:", "Entendido. ¿Me proporciona su dirección?", true),
            TurnoGuion("Tú:", "Calle Independencia número cincuenta y dos.", false)
        )),
        GuionPractica("🩺 Agendar Cita Médica (Teléfono)", "Demanda media. Sincronizar horarios bajo presión.", listOf(
            TurnoGuion("Asistente:", "Consultorio del Doctor Ramírez, ¿en qué le puedo ayudar?", true),
            TurnoGuion("Tú:", "Hola, quisiera agendar una cita de revisión para esta semana.", false),
            TurnoGuion("Asistente:", "Tengo un espacio el jueves a las cuatro de la tarde, ¿le queda bien?", true),
            TurnoGuion("Tú:", "Se me complica un poco. ¿Tendrá algo libre el viernes por la mañana?", false),
            TurnoGuion("Asistente:", "Déjeme revisar. Sí, el viernes a las diez de la mañana.", true),
            TurnoGuion("Tú:", "Ese horario está perfecto, anóteme por favor.", false)
        )),
        GuionPractica("👕 Devolución en Tienda", "Demanda media. Requiere asertividad para explicar un problema.", listOf(
            TurnoGuion("Cajero:", "Hola, ¿en qué te puedo ayudar el día de hoy?", true),
            TurnoGuion("Tú:", "Hola, me gustaría devolver esta camisa. Tiene un defecto de fábrica.", false),
            TurnoGuion("Cajero:", "Oh, lo siento. ¿Tienes tu recibo de compra contigo?", true),
            TurnoGuion("Tú:", "Sí, aquí está. La compré ayer por la tarde.", false),
            TurnoGuion("Cajero:", "¿Prefieres el reembolso o cambiarla por otro artículo?", true),
            TurnoGuion("Tú:", "Prefiero el reembolso directo a mi tarjeta, por favor.", false)
        )),
        GuionPractica("🙋‍♂️ Pedir Ayuda a un Compañero", "Demanda media. Exponer vulnerabilidad ante un par.", listOf(
            TurnoGuion("Compañero:", "¡Hola! ¿Cómo va todo con el proyecto?", true),
            TurnoGuion("Tú:", "Hola. Va bien, pero me atoré en una parte del código.", false),
            TurnoGuion("Compañero:", "¿En serio? ¿En qué módulo tienes el problema?", true),
            TurnoGuion("Tú:", "En la conexión a la base de datos, ¿tienes unos minutos para revisarlo?", false)
        )),
        GuionPractica("💳 Reportar cargo no reconocido", "Demanda media. Llamada telefónica seria a institución financiera.", listOf(
            TurnoGuion("Operador:", "Banco central, le atiende Carlos. ¿Cuál es su emergencia?", true),
            TurnoGuion("Tú:", "Buenas tardes, quiero reportar un cargo que yo no realicé en mi tarjeta.", false),
            TurnoGuion("Operador:", "Comprendo. Para su seguridad, ¿me indica los últimos cuatro dígitos del plástico?", true),
            TurnoGuion("Tú:", "Claro, los números son cuatro, nueve, uno, siete.", false)
        )),
        GuionPractica("📅 Cambiar una reservación", "Demanda media. Pedir disculpas y renegociar condiciones.", listOf(
            TurnoGuion("Recepcionista:", "Restaurante El Balcón, ¿en qué le sirvo?", true),
            TurnoGuion("Tú:", "Hola, tengo una reservación para hoy, pero necesito cambiar la hora.", false),
            TurnoGuion("Recepcionista:", "¿A nombre de quién está y para qué hora era originalmente?", true),
            TurnoGuion("Tú:", "A nombre de Humberto, era para las ocho, quisiera cambiarla a las nueve.", false)
        )),
        GuionPractica("🎓 Pedir información de un curso", "Demanda media. Formular preguntas abiertas.", listOf(
            TurnoGuion("Asesor:", "Instituto de Idiomas, ¿te puedo dar información de algún curso?", true),
            TurnoGuion("Tú:", "Sí, estoy interesado en los cursos intensivos de inglés para adultos.", false),
            TurnoGuion("Asesor:", "Tenemos clases entre semana y sabatinas. ¿Cuál te interesa más?", true),
            TurnoGuion("Tú:", "Las sabatinas me convienen más. ¿Qué costo tienen y cuánto duran?", false)
        )),
        GuionPractica("☕ Invitar a un conocido a comer", "Demanda media. Riesgo social de rechazo.", listOf(
            TurnoGuion("Conocido:", "¡Qué tal! Tiempo sin verte por la oficina.", true),
            TurnoGuion("Tú:", "¡Hola! Sí, he estado trabajando desde casa estas semanas.", false),
            TurnoGuion("Conocido:", "Deberíamos ponernos al día pronto.", true),
            TurnoGuion("Tú:", "Me parece una gran idea. ¿Tienes tiempo para ir a comer este viernes?", false)
        )),

        // ==========================================
        // NIVEL 3: ALTA DEMANDA SOCIAL
        // ==========================================
        GuionPractica("🎤 Exposición en Clase", "Demanda alta. Hablar frente a un grupo genera ansiedad anticipatoria.", listOf(
            TurnoGuion("Profesor:", "Muy bien, es el turno del siguiente equipo. Puedes comenzar con tu introducción.", true),
            TurnoGuion("Tú:", "Buenos días a todos. Hoy presentaremos nuestro proyecto final.", false),
            TurnoGuion("Profesor:", "Excelente, ¿cuál es el objetivo principal de su investigación?", true),
            TurnoGuion("Tú:", "El objetivo es analizar el impacto del cambio climático en la región de Xalapa.", false)
        )),
        GuionPractica("😠 Queja en Servicio al Cliente", "Demanda alta. Explicar detalles técnicos estando frustrado.", listOf(
            TurnoGuion("Operador:", "Soporte técnico, le atiende Laura. ¿Cuál es su problema?", true),
            TurnoGuion("Tú:", "Hola, llevo dos días sin servicio de internet en mi domicilio.", false),
            TurnoGuion("Operador:", "Entiendo. ¿Me proporciona su número de cuenta a diez dígitos?", true),
            TurnoGuion("Tú:", "Claro, es el cinco, cinco, ocho, nueve, tres, uno, cero, cuatro.", false),
            TurnoGuion("Operador:", "Gracias. Veo una falla en su zona. Quedará resuelta en unas horas.", true),
            TurnoGuion("Tú:", "De acuerdo. ¿Habrá algún ajuste en mi factura por los días sin servicio?", false)
        )),
        GuionPractica("👋 Conocer a los Suegros", "Demanda alta. Presión por dar una buena primera impresión.", listOf(
            TurnoGuion("Suegro:", "¡Hola! Por fin nos conocemos. He escuchado mucho sobre ti.", true),
            TurnoGuion("Tú:", "¡Hola! Mucho gusto en conocerlos, gracias por invitarme a su casa.", false),
            TurnoGuion("Suegro:", "Pasa, ponte cómodo. ¿Qué tal estuvo el tráfico de camino hacia acá?", true),
            TurnoGuion("Tú:", "Un poco pesado, pero afortunadamente llegamos a buena hora.", false)
        )),
        GuionPractica("🗣️ Pregunta en un Auditorio", "Demanda alta. Silencio absoluto mientras tú hablas.", listOf(
            TurnoGuion("Ponente:", "Esa fue nuestra presentación. ¿Alguien tiene alguna pregunta?", true),
            TurnoGuion("Tú:", "Sí, aquí. Quería preguntar sobre la implementación técnica de la nube.", false),
            TurnoGuion("Ponente:", "Adelante, te escuchamos. ¿Cuál es tu duda específica?", true),
            TurnoGuion("Tú:", "¿Cómo aseguraron la integridad de los datos durante la migración masiva?", false)
        )),
        GuionPractica("📝 Feedback con el Profesor", "Demanda alta. Figura de autoridad evaluando tu trabajo.", listOf(
            TurnoGuion("Profesor:", "Revisé tu ensayo. Tienes buenos puntos, pero la estructura es confusa.", true),
            TurnoGuion("Tú:", "Entiendo. ¿Me podría señalar qué partes son las más problemáticas?", false),
            TurnoGuion("Profesor:", "Principalmente la conclusión. Parece que no cierra las ideas presentadas.", true),
            TurnoGuion("Tú:", "Tiene razón. Lo voy a reescribir hoy mismo enfocándome en un cierre sólido.", false)
        )),
        GuionPractica("👮 Retén de Tránsito", "Demanda alta. Ansiedad ante autoridades policiales.", listOf(
            TurnoGuion("Oficial:", "Buenas tardes, control de rutina. ¿Me permite su licencia de conducir?", true),
            TurnoGuion("Tú:", "Buenas tardes oficial, claro que sí. Permítame sacarla de mi cartera.", false),
            TurnoGuion("Oficial:", "¿De dónde viene y hacia dónde se dirige?", true),
            TurnoGuion("Tú:", "Vengo de mi trabajo y me dirijo hacia mi domicilio en el centro.", false)
        )),
        GuionPractica("🚫 Reclamar un platillo equivocado", "Demanda alta. Corregir asertivamente a alguien que está trabajando.", listOf(
            TurnoGuion("Mesero:", "Aquí tiene su pasta con mariscos, buen provecho.", true),
            TurnoGuion("Tú:", "Disculpa, creo que hubo una confusión. Yo ordené la pasta con pollo.", false),
            TurnoGuion("Mesero:", "¡Ay, tiene toda la razón! Mil disculpas, se la cambio de inmediato.", true),
            TurnoGuion("Tú:", "No hay problema, te espero. Muchas gracias por la atención.", false)
        )),
        GuionPractica("🚑 Llamada de Emergencia", "Demanda alta. Simulación de urgencia, presión máxima de tiempo.", listOf(
            TurnoGuion("Operador:", "Nueve uno uno, ¿cuál es su emergencia?", true),
            TurnoGuion("Tú:", "Hola, necesito una ambulancia. Hubo un accidente de auto en la avenida principal.", false),
            TurnoGuion("Operador:", "¿Hay personas lesionadas? ¿Cuál es la dirección exacta?", true),
            TurnoGuion("Tú:", "Sí, hay un herido. Estamos en la esquina de Avenida Juárez y Calle Morelos.", false)
        )),

        // ==========================================
        // NIVEL 4: DEMANDA MUY ALTA
        // ==========================================
        GuionPractica("💼 Entrevista de Trabajo", "Demanda muy alta. Tensión de evaluación directa y competencia.", listOf(
            TurnoGuion("Entrevistador:", "Hola, toma asiento. Veo tu currículum aquí. Cuéntame sobre tu experiencia.", true),
            TurnoGuion("Tú:", "Hola, muchas gracias. Tengo tres años de experiencia en desarrollo web.", false),
            TurnoGuion("Entrevistador:", "Muy interesante. ¿Por qué te gustaría trabajar con nosotros?", true),
            TurnoGuion("Tú:", "Porque admiro su cultura laboral y creo que puedo aportar mucho valor a su equipo.", false)
        )),
        GuionPractica("📈 Solicitar Aumento de Sueldo", "Demanda muy alta. Negociación, justificación y riesgo de negativa.", listOf(
            TurnoGuion("Jefe:", "Hola, me pediste unos minutos para hablar. ¿De qué se trata?", true),
            TurnoGuion("Tú:", "Hola. Sí, quería revisar mi crecimiento dentro de la empresa.", false),
            TurnoGuion("Jefe:", "Claro. He notado que has tomado más responsabilidades últimamente.", true),
            TurnoGuion("Tú:", "Así es. Basado en mis resultados y métricas, me gustaría solicitar un ajuste salarial.", false)
        )),
        GuionPractica("📊 Presentación ante Directivos", "Demanda muy alta. Exposición formal, interrupciones y datos duros.", listOf(
            TurnoGuion("Director:", "Bien, el siguiente punto es el reporte de finanzas. Tienes la palabra.", true),
            TurnoGuion("Tú:", "Gracias. Si ven la pantalla, los ingresos del trimestre subieron un diez por ciento.", false),
            TurnoGuion("Director:", "¿A qué se debe este aumento tan repentino en las ventas?", true),
            TurnoGuion("Tú:", "Principalmente a la nueva campaña de marketing digital que lanzamos el mes pasado.", false)
        )),
        GuionPractica("🚪 Renunciar a un trabajo", "Demanda muy alta. Conversación incómoda con un superior.", listOf(
            TurnoGuion("Jefe:", "Pasa, siéntate. Me comentaste que tenías algo importante que decirme.", true),
            TurnoGuion("Tú:", "Sí, quiero agradecerte por la oportunidad, pero he decidido presentar mi renuncia.", false),
            TurnoGuion("Jefe:", "Vaya, es una sorpresa. ¿Tienes alguna otra oferta de trabajo?", true),
            TurnoGuion("Tú:", "Sí, recibí una oferta que se alinea más con mis metas profesionales a largo plazo.", false)
        )),
        GuionPractica("💔 Confrontar a un amigo", "Demanda muy alta. Carga emocional, conflicto interpersonal.", listOf(
            TurnoGuion("Amigo:", "Hola, ¿qué pasa? Me dijiste que querías hablar conmigo muy seriamente.", true),
            TurnoGuion("Tú:", "Sí. La verdad es que me sentí muy mal por lo que dijiste ayer frente a todos.", false),
            TurnoGuion("Amigo:", "Solo era una broma, no pensé que te lo tomarías tan a pecho.", true),
            TurnoGuion("Tú:", "Puede ser, pero cruzaste un límite personal y te pido que no lo vuelvas a hacer.", false)
        )),
        GuionPractica("🙇‍♂️ Disculpa formal por un error", "Demanda muy alta. Aceptar responsabilidad por un fallo crítico.", listOf(
            TurnoGuion("Gerente:", "Tuvimos una caída en el servidor principal. ¿Sabes qué fue lo que pasó?", true),
            TurnoGuion("Tú:", "Sí. Fui yo. Ejecuté un comando incorrecto durante la actualización de rutina.", false),
            TurnoGuion("Gerente:", "Eso nos costó horas de inactividad. ¿Cómo planeas arreglarlo?", true),
            TurnoGuion("Tú:", "Asumo toda la responsabilidad. Ya documenté el error y creé un script de prevención.", false)
        ))
    )

    LaunchedEffect(estadoEvocacion, tiempoRestante, pausarTimer) {
        if (estadoEvocacion == "JUGANDO" && tiempoRestante > 0 && !pausarTimer && !mostrarPausaRespiracion) {
            delay(1000)
            tiempoRestante--
            if (tiempoRestante == 15) {
                pistaActual = if (modoEvocacion == "CATEGORIAS") {
                    "Empieza con '${diccionariosEvocacion[categoriaActual]?.random()?.first()?.uppercaseChar() ?: ""}'"
                } else {
                    "Empieza con '${diccionarioAntonimos[antonimoActual]?.first()?.first()?.uppercaseChar() ?: ""}'"
                }
            }
        } else if (tiempoRestante == 0 && estadoEvocacion == "JUGANDO") {
            estadoEvocacion = "FIN"
            motorDSP.detenerAnalisis(null)
            isDSPActive = false
            if (isListening) {
                speechRecognizer?.stopListening()
                isListening = false
            }
        }
    }

    var procesandoAudio by remember { mutableStateOf(false) }
    LaunchedEffect(metricasDSP.estado) {
        if (isDSPActive && !procesandoAudio) {
            if (metricasDSP.estado == EstadoVozDSP.BLOQUEO_TONICO || metricasDSP.estado == EstadoVozDSP.BLOQUEO_CLONICO) {
                procesandoAudio = true
                tensionDetectada = true

                if (estadoEvocacion == "JUGANDO") {
                    erroresEvocacion++
                    pausarTimer = true
                }

                textoEscuchado = "⚠️ Bloqueo detectado. Inhala... Exhala..."
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                coroutineScope.launch {
                    offsetX.animateTo(15f, tween(50))
                    offsetX.animateTo(-15f, tween(50))
                    offsetX.animateTo(10f, tween(50))
                    offsetX.animateTo(-10f, tween(50))
                    offsetX.animateTo(0f, tween(50))
                }
                delay(3000)
                tensionDetectada = false
                procesandoAudio = false
                pausarTimer = false
            } else if (metricasDSP.estado == EstadoVozDSP.FLUIDO) {
                procesandoAudio = true
                textoEscuchado = "✅ Fonación fluida"
                delay(1200)
                procesandoAudio = false
            }
        }
    }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                ttsInitialized = true
            }
        }
        tts = textToSpeech

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
            recognizer.destroy()
            motorDSP.detenerAnalisis(null)
        }
    }

    fun reproducirAudioApp(texto: String) {
        if (ttsInitialized) {
            tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun avanzarTurnoGuion() {
        if (guionSeleccionado != null && indiceGuionActual < guionSeleccionado!!.dialogos.size - 1) {
            indiceGuionActual++
            val turno = guionSeleccionado!!.dialogos[indiceGuionActual]
            if (turno.esDeApp) {
                reproducirAudioApp(turno.texto)
            }
        } else {
            faseGuion = FaseGuion.CHECK_OUT
        }
    }

    fun iniciarEscuchaTextoSemantico() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                textoEscuchado = "Error al escuchar"
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val textoDicho = matches?.firstOrNull()?.trim() ?: ""

                if (textoDicho.isBlank()) {
                    textoEscuchado = "No escuché nada. Intenta hablar más cerca."
                    return
                }

                val palabraDicha = textoDicho.lowercase()
                val esValido = if (modoEvocacion == "CATEGORIAS") {
                    diccionariosEvocacion[categoriaActual]?.any { palabraDicha.contains(it) } == true
                } else {
                    diccionarioAntonimos[antonimoActual]?.any { palabraDicha.contains(it) } == true
                }

                if (esValido) {
                    aciertosEvocacion++
                    textoEscuchado = "✅ Correcto: $palabraDicha"
                    pistaActual = ""
                    if (modoEvocacion == "ANTONIMOS") {
                        antonimoActual = diccionarioAntonimos.keys.random()
                    }
                } else {
                    erroresEvocacion++
                    textoEscuchado = "❌ Incorrecto/No es: $palabraDicha"
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    fun iniciarEscuchaSTT(textoObjetivo: String, esGuion: Boolean) {
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
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val textoDicho = matches?.firstOrNull()?.trim() ?: ""

                if (textoDicho.isBlank()) {
                    textoEscuchado = "No escuché nada. Intenta hablar más cerca."
                    return
                }

                textoEscuchado = textoDicho
                ultimaCalificacion = calcularPrecisionHabla(textoObjetivo, textoDicho)

                if (esGuion) {
                    if (ultimaCalificacion >= 60) aciertosGuion++ else erroresGuion++
                    avanzarTurnoGuion()
                } else {
                    val aciertos = if (ultimaCalificacion >= 60) 1 else 0
                    val errores = if (ultimaCalificacion < 60) 1 else 0
                    ProgresoStorage.guardarSesionMultidimensional(context, "Simulación (Escenario)", aciertos, errores, ultimaCalificacion)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    // ==========================================
    // MODAL DE RESPIRACIÓN (TIEMPO FUERA)
    // ==========================================
    if (mostrarPausaRespiracion) {
        Dialog(onDismissRequest = { mostrarPausaRespiracion = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    var faseRespiracion by remember { mutableStateOf("INHALA (4s)") }
                    val transition = rememberInfiniteTransition(label = "respiracion")
                    val scale by transition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(4000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "escala"
                    )

                    LaunchedEffect(scale) {
                        faseRespiracion = when {
                            scale > 1.35f -> "SOSTÉN (4s)"
                            scale < 0.65f -> "PAUSA"
                            scale > 1.0f -> "INHALA (4s)"
                            else -> "EXHALA (4s)"
                        }
                    }

                    Text(
                        text = "Tiempo Fuera",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006064)
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((100 * scale).dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00ACC1).copy(alpha = 0.5f))
                        )
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00ACC1))
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Text(
                        text = faseRespiracion,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00838F)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { mostrarPausaRespiracion = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006064))
                    ) {
                        Text("Reanudar Práctica")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (seccionActual) {
                            SimulacionSituaciones.MENU_PRINCIPAL -> "Simulación"
                            SimulacionSituaciones.ESCENARIOS -> "Escenarios Cotidianos"
                            SimulacionSituaciones.GUIONES -> "Guiones Estresantes"
                            SimulacionSituaciones.EVOCACION_LEXICA -> "Evocación Léxica"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (seccionActual == SimulacionSituaciones.MENU_PRINCIPAL) {
                            onBack()
                        } else {
                            if (isListening) speechRecognizer?.stopListening()
                            motorDSP.detenerAnalisis(null)
                            isDSPActive = false
                            estadoEvocacion = "INICIO"
                            seccionActual = SimulacionSituaciones.MENU_PRINCIPAL
                            faseGuion = FaseGuion.SELECCION
                        }
                    }) {
                        Icon(ArrowLeftCircleIcon, contentDescription = "Regresar")
                    }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = HomeIcon,
                        contentDescription = "Menú Principal",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Regresar al Menú Principal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (seccionActual == SimulacionSituaciones.MENU_PRINCIPAL) {
                BarraNavegacionModulos(
                    onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
                    onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
                    onNavigateHablaEstirada = { navController.navigate(Screen.HablaEstirada.route) },
                    onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
                    onNavigateSimulacionSituaciones = { /* Ya estás aquí */ },
                    onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
                    onNavigateProgreso = { navController.navigate(Screen.Progreso.route) },
                    onNavigateEjerciciosAdaptativos = { navController.navigate(Screen.EjerciciosAdaptativos.route) },
                    onNavigateBio = { navController.navigate(Screen.Biofeedback.route) },
                    onNavigateTarjetasArticulacion = { navController.navigate(Screen.TarjetasArticulacion.route) }
                )
            }

            when (seccionActual) {
                SimulacionSituaciones.MENU_PRINCIPAL -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF0277BD), Color(0xFF00ACC1))))
                            .clickable { seccionActual = SimulacionSituaciones.ESCENARIOS }
                            .padding(20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text("🎧 Escenarios Cotidianos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Interacciones cortas de 1 turno", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFEF6C00), Color(0xFFFFA726))))
                            .clickable {
                                seccionActual = SimulacionSituaciones.GUIONES
                                faseGuion = FaseGuion.SELECCION
                            }
                            .padding(20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text("💬 Guiones Estresantes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Jerarquía de desensibilización social", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))))
                            .clickable {
                                tiempoRestante = TIEMPO_MAXIMO_EVOCACION
                                estadoEvocacion = "INICIO"
                                seccionActual = SimulacionSituaciones.EVOCACION_LEXICA
                            }
                            .padding(20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text("⏱️ Evocación bajo Presión", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Piensa rápido sin bloquearte", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                SimulacionSituaciones.GUIONES -> {
                    when (faseGuion) {
                        FaseGuion.SELECCION -> {
                            val niveles = listOf(
                                "Nivel 1: Baja Demanda (Rutina)" to jerarquiaGuiones.subList(0, 8),
                                "Nivel 2: Demanda Media (Teléfono/Negociación)" to jerarquiaGuiones.subList(8, 16),
                                "Nivel 3: Alta Demanda (Autoridad/Público)" to jerarquiaGuiones.subList(16, 24),
                                "Nivel 4: Demanda Muy Alta (Impacto Crítico)" to jerarquiaGuiones.subList(24, 30)
                            )
                            var nivelSeleccionadoIndex by remember { mutableIntStateOf(0) }

                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "Selecciona tu nivel de exposición:",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                ScrollableTabRow(
                                    selectedTabIndex = nivelSeleccionadoIndex,
                                    edgePadding = 0.dp,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                ) {
                                    niveles.forEachIndexed { index, nivel ->
                                        Tab(
                                            selected = nivelSeleccionadoIndex == index,
                                            onClick = { nivelSeleccionadoIndex = index },
                                            text = {
                                                Text(
                                                    text = "Nivel ${index + 1}",
                                                    fontWeight = if (nivelSeleccionadoIndex == index) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = niveles[nivelSeleccionadoIndex].first,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    niveles[nivelSeleccionadoIndex].second.forEach { guion ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    guionSeleccionado = guion
                                                    faseGuion = FaseGuion.CHECK_IN
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(guion.titulo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = guion.descripcion,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        }
                        FaseGuion.CHECK_IN, FaseGuion.CHECK_OUT -> {
                            val esInicio = faseGuion == FaseGuion.CHECK_IN
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (esInicio) "¿Cómo te sientes antes de comenzar?" else "¿Cómo te sientes ahora que terminaste?",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(40.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    EstadoEmocional.entries.filter { it != EstadoEmocional.NINGUNO }.forEach { estado ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable {
                                                    if (esInicio) {
                                                        emocionInicial = estado
                                                        indiceGuionActual = 0
                                                        aciertosGuion = 0
                                                        erroresGuion = 0
                                                        faseGuion = FaseGuion.PRACTICA
                                                        if (guionSeleccionado?.dialogos?.get(0)?.esDeApp == true) {
                                                            reproducirAudioApp(guionSeleccionado!!.dialogos[0].texto)
                                                        }
                                                    } else {
                                                        emocionFinal = estado
                                                        faseGuion = FaseGuion.RESULTADOS
                                                    }
                                                }
                                                .padding(8.dp)
                                        ) {
                                            Text(estado.icono, fontSize = 50.sp)
                                            Text(estado.texto, fontWeight = FontWeight.Bold, color = estado.color)
                                        }
                                    }
                                }
                            }
                        }
                        FaseGuion.PRACTICA -> {
                            val turnoActual = guionSeleccionado!!.dialogos[indiceGuionActual]

                            val bgBrush = if (tensionDetectada && !turnoActual.esDeApp) {
                                Brush.linearGradient(listOf(Color(0xFFC62828), Color(0xFFD32F2F)))
                            } else if (turnoActual.esDeApp) {
                                Brush.linearGradient(listOf(Color(0xFFEF6C00), Color(0xFFFFA726)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF546E7A)))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Práctica de Guion",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                OutlinedButton(
                                    onClick = {
                                        if (isListening) speechRecognizer?.stopListening()
                                        motorDSP.detenerAnalisis(null)
                                        isDSPActive = false
                                        tts?.stop()
                                        mostrarPausaRespiracion = true
                                    }
                                ) {
                                    Text("🌬️ Respirar", color = Color(0xFF00838F))
                                }
                            }

                            val progresoGuion = (indiceGuionActual + 1).toFloat() / guionSeleccionado!!.dialogos.size
                            LinearProgressIndicator(
                                progress = { progresoGuion },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(x = if (!turnoActual.esDeApp) offsetX.value.dp else 0.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bgBrush)
                                    .padding(20.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Turno ${indiceGuionActual + 1} de ${guionSeleccionado!!.dialogos.size}",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = turnoActual.rol,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        text = turnoActual.texto,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )

                                    if (turnoActual.esDeApp) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { reproducirAudioApp(turnoActual.texto) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("🔊 Escuchar", color = Color.White)
                                            }

                                            Button(
                                                onClick = { avanzarTurnoGuion() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Siguiente ➡️", color = Color(0xFFEF6C00))
                                            }
                                        }
                                    }
                                }
                            }

                            if (!turnoActual.esDeApp) {
                                if (isDSPActive) {
                                    BiofeedbackVisualAvanzadoDSP(metricasDSP)
                                    MonitorFonologicoYCAA(
                                        motorDSP = motorDSP,
                                        palabraObjetivoActual = turnoActual.texto,
                                        textoEscuchado = textoEscuchado
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (isListening) speechRecognizer?.stopListening()
                                            else iniciarEscuchaSTT(turnoActual.texto, true)
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isDSPActive,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isListening) Color.Red else Color.White
                                        )
                                    ) {
                                        Text(
                                            text = if (isListening) "⏹️ Voz" else "📝 Texto STT",
                                            color = if (isListening) Color.White else Color(0xFF37474F)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                return@Button
                                            }
                                            if (isDSPActive) {
                                                motorDSP.detenerAnalisis(context, "Simulación DSP")
                                                isDSPActive = false
                                            } else {
                                                motorDSP.detenerAnalisis(null)
                                                motorDSP.iniciarAnalisisDSP()
                                                isDSPActive = true
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isListening,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDSPActive) Color(0xFFD32F2F) else Color(0xFF00897B)
                                        )
                                    ) {
                                        Text(if (isDSPActive) "⏹️ Stop DSP" else "🎛️ DSP Activo")
                                    }
                                }

                                if (textoEscuchado.isNotEmpty()) {
                                    Text(
                                        text = "Reconocido: \"$textoEscuchado\" ($ultimaCalificacion%)",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        FaseGuion.RESULTADOS -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("🎉 ¡Guion Completado!", fontSize = 22.sp, fontWeight = FontWeight.Bold)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Validación Emocional", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Antes", fontSize = 12.sp)
                                                Text(emocionInicial.icono, fontSize = 30.sp)
                                                Text(emocionInicial.texto, fontSize = 12.sp)
                                            }
                                            Text("➡️", fontSize = 24.sp)
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Después", fontSize = 12.sp)
                                                Text(emocionFinal.icono, fontSize = 30.sp)
                                                Text(emocionFinal.texto, fontSize = 12.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (emocionInicial == EstadoEmocional.ANSIOSO && emocionFinal != EstadoEmocional.ANSIOSO) {
                                            Text(
                                                text = "¡Excelente! Notaste cómo la ansiedad bajó al enfrentar la situación. La evitación alimenta el miedo, la exposición lo reduce.",
                                                textAlign = TextAlign.Center,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                text = "Recuerda que cada práctica fortalece tu confianza y tus redes neuronales.",
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                val totalTurnos = guionSeleccionado!!.dialogos.count { !it.esDeApp }
                                val calificacionFinal = if (totalTurnos > 0) ((aciertosGuion.toFloat() / totalTurnos) * 100).toInt() else 100

                                Text(
                                    text = "Precisión de habla: $calificacionFinal%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )

                                Button(
                                    onClick = {
                                        ProgresoStorage.guardarSesionMultidimensional(
                                            context = context,
                                            modulo = "Simulación (${guionSeleccionado!!.titulo})",
                                            aciertos = aciertosGuion,
                                            erroresTotales = erroresGuion,
                                            calificacion = calificacionFinal
                                        )
                                        faseGuion = FaseGuion.SELECCION
                                    }
                                ) {
                                    Text("Guardar y Volver")
                                }
                            }
                        }
                    }
                }

                SimulacionSituaciones.ESCENARIOS -> {
                    val (titulo, audioSimulado, respuestaSugerida) = listaEscenarios[escenarioSeleccionadoIndex]

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Escenarios Rápidos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OutlinedButton(
                            onClick = {
                                if (isListening) speechRecognizer?.stopListening()
                                motorDSP.detenerAnalisis(null)
                                isDSPActive = false
                                tts?.stop()
                                mostrarPausaRespiracion = true
                            }
                        ) {
                            Text("🌬️ Respirar", color = Color(0xFF00838F))
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = offsetX.value.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (tensionDetectada) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("🗣️ Interlocutor: \"$audioSimulado\"", fontSize = 14.sp)
                            Text("🎯 Respuesta objetivo: \"$respuestaSugerida\"", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Button(onClick = { reproducirAudioApp(audioSimulado) }) {
                                Text("🔊 Escuchar Interlocutor")
                            }
                        }
                    }

                    if (isDSPActive) {
                        BiofeedbackVisualAvanzadoDSP(metricasDSP)
                        MonitorFonologicoYCAA(
                            motorDSP = motorDSP,
                            palabraObjetivoActual = respuestaSugerida,
                            textoEscuchado = textoEscuchado
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isListening) speechRecognizer?.stopListening()
                                else iniciarEscuchaSTT(respuestaSugerida, false)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isDSPActive,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening) Color.Red else Color(0xFF0277BD)
                            )
                        ) {
                            Text(if (isListening) "⏹️ Voz" else "📝 Texto")
                        }

                        Button(
                            onClick = {
                                if (isDSPActive) {
                                    motorDSP.detenerAnalisis(context, "Escenario DSP")
                                    isDSPActive = false
                                } else {
                                    motorDSP.detenerAnalisis(null)
                                    motorDSP.iniciarAnalisisDSP()
                                    isDSPActive = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isListening,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDSPActive) Color(0xFFD32F2F) else Color(0xFF00897B)
                            )
                        ) {
                            Text(if (isDSPActive) "⏹️ Stop DSP" else "🎛️ DSP")
                        }
                    }

                    if (textoEscuchado.isNotEmpty()) {
                        Text(
                            text = "\"$textoEscuchado\" ($ultimaCalificacion%)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { if (escenarioSeleccionadoIndex > 0) escenarioSeleccionadoIndex-- },
                            enabled = escenarioSeleccionadoIndex > 0
                        ) {
                            Text("Anterior")
                        }

                        OutlinedButton(
                            onClick = { if (escenarioSeleccionadoIndex < listaEscenarios.size - 1) escenarioSeleccionadoIndex++ },
                            enabled = escenarioSeleccionadoIndex < listaEscenarios.size - 1
                        ) {
                            Text("Siguiente")
                        }
                    }
                }

                SimulacionSituaciones.EVOCACION_LEXICA -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = offsetX.value.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (tensionDetectada) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (estadoEvocacion == "INICIO") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    FilterChip(
                                        selected = modoEvocacion == "CATEGORIAS",
                                        onClick = { modoEvocacion = "CATEGORIAS" },
                                        label = { Text("Categorías") }
                                    )
                                    FilterChip(
                                        selected = modoEvocacion == "ANTONIMOS",
                                        onClick = { modoEvocacion = "ANTONIMOS" },
                                        label = { Text("Antónimos") }
                                    )
                                }
                            } else {
                                Text(
                                    text = if (modoEvocacion == "CATEGORIAS") categoriaActual else antonimoActual,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (pausarTimer) "RESCATE RESPIRATORIO" else "Tiempo Restante",
                            color = if (pausarTimer) Color.Red else Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val progresoTiempo = tiempoRestante.toFloat() / TIEMPO_MAXIMO_EVOCACION
                        val colorTiempo = when {
                            tiempoRestante > 15 -> Color(0xFF4CAF50)
                            tiempoRestante > 5 -> Color(0xFFFFA000)
                            else -> Color(0xFFD32F2F)
                        }

                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progresoTiempo },
                                modifier = Modifier.size(140.dp),
                                color = colorTiempo,
                                strokeWidth = 12.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "00:${tiempoRestante.toString().padStart(2, '0')}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (tiempoRestante <= 10) Color.Red else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        if (pistaActual.isNotEmpty() && estadoEvocacion == "JUGANDO") {
                            Text(
                                text = pistaActual,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57F17),
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        when (estadoEvocacion) {
                            "INICIO" -> {
                                Button(
                                    onClick = {
                                        tiempoRestante = TIEMPO_MAXIMO_EVOCACION
                                        aciertosEvocacion = 0
                                        erroresEvocacion = 0
                                        estadoEvocacion = "JUGANDO"
                                    },
                                    modifier = Modifier.fillMaxWidth(0.7f).height(50.dp)
                                ) {
                                    Text("Iniciar Desafío", fontSize = 18.sp)
                                }
                            }
                            "FIN" -> {
                                Text("Aciertos: $aciertosEvocacion | Errores: $erroresEvocacion", fontSize = 16.sp)
                                Button(
                                    onClick = {
                                        ProgresoStorage.guardarSesionMultidimensional(context, "Evocación ($modoEvocacion)", aciertosEvocacion, erroresEvocacion, 100)
                                        estadoEvocacion = "INICIO"
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Guardar Progreso")
                                }
                            }
                            "JUGANDO" -> {
                                if (textoEscuchado.isNotEmpty()) {
                                    Text(
                                        text = textoEscuchado,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }

                                if (isDSPActive) {
                                    BiofeedbackVisualAvanzadoDSP(metricasDSP)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (isListening) speechRecognizer?.stopListening()
                                            else iniciarEscuchaTextoSemantico()
                                        },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        enabled = !isDSPActive
                                    ) {
                                        Text(if (isListening) "⏹️ Escuchando..." else "🎤 Hablar")
                                    }

                                    Button(
                                        onClick = {
                                            if (isDSPActive) {
                                                motorDSP.detenerAnalisis(null)
                                                isDSPActive = false
                                            } else {
                                                motorDSP.detenerAnalisis(null)
                                                motorDSP.iniciarAnalisisDSP()
                                                isDSPActive = true
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        enabled = !isListening
                                    ) {
                                        Text(if (isDSPActive) "⏹️ Stop DSP" else "🎛️ DSP (Fluidez)")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}