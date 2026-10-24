package com.itsx.speaktutor.logic

import android.content.Context
import com.itsx.speaktutor.ui.screens.ProgresoStorage
import java.util.UUID

enum class CategoriaAdaptativa {
    BLOQUES_RITMO,
    LECTURA_GUIADA,
    HABLA_ESTIRADA,
    ESCENARIO_RAPIDO,
    GUION_ESTRESANTE,
    METRONOMO_PURO,
    BIOFEEDBACK_RELAJACION,
    EVOCACION_SEMANTICA,
    PRONUNCIACION_COMPLEJA
}

data class EjercicioGenerado(
    val id: String = UUID.randomUUID().toString(),
    val categoria: CategoriaAdaptativa,
    val titulo: String,
    val bpmSugerido: Int,
    val textoPractica: String,
    val instruccionEspecial: String,
    val audioInterlocutor: String? = null
)

object MotorAdaptativo {

    fun generarConsejoContextual(context: Context): String {
        val sesiones = ProgresoStorage.obtenerSesionesClinicas(context)
        if (sesiones.isEmpty()) return "👋 ¡Bienvenido al Laboratorio IA! Completa ejercicios para que la red neuronal perfile tu fluidez."

        val recientes = sesiones.take(5)
        val fluidez = recientes.map { it.calificacionGlobal }.average()
        val tonicos = recientes.map { it.bloqueosTonicos }.average()
        val clonicos = recientes.map { it.bloqueosClonicos }.average()
        val latencia = recientes.map { it.latenciaMediaMs }.average()

        return when {
            tonicos > 5 -> "🚨 Alta tensión vocal detectada. Priorizaremos la relajación diafragmática y los inicios suaves (contacto ligero)."
            clonicos > 5 -> "🧱 Tienes repeticiones silábicas. El metrónomo y los bloques de ritmo te ayudarán a encarrilar la fonación."
            latencia > 2500 -> "⏱️ Tu latencia de respuesta es elevada. Entrenaremos con evocación rápida y escenarios sociales dinámicos."
            fluidez >= 85 -> "🌟 Nivel de fluidez óptimo. La IA te retará hoy con pronunciación compleja y situaciones estresantes."
            else -> "⚖️ Rendimiento equilibrado. Mantendremos un entrenamiento cruzado para fortalecer todas las áreas."
        }
    }

    fun generarEjercicioNuevo(context: Context, categoriaEvitada: CategoriaAdaptativa? = null): EjercicioGenerado {
        val sesiones = ProgresoStorage.obtenerSesionesClinicas(context)
        val pesos = mutableMapOf<CategoriaAdaptativa, Double>()
        CategoriaAdaptativa.entries.forEach { pesos[it] = Math.random() * 20.0 }

        if (sesiones.isNotEmpty()) {
            val cortoPlazo = sesiones.take(3)
            val tonicosCP = cortoPlazo.map { it.bloqueosTonicos }.average()
            val clonicosCP = cortoPlazo.map { it.bloqueosClonicos }.average()
            val latenciaCP = cortoPlazo.map { it.latenciaMediaMs }.average()
            val fluidezCP = cortoPlazo.map { it.calificacionGlobal }.average()

            if (tonicosCP > 3) {
                pesos[CategoriaAdaptativa.BIOFEEDBACK_RELAJACION] = (pesos[CategoriaAdaptativa.BIOFEEDBACK_RELAJACION] ?: 0.0) + 50.0
                pesos[CategoriaAdaptativa.HABLA_ESTIRADA] = (pesos[CategoriaAdaptativa.HABLA_ESTIRADA] ?: 0.0) + 40.0
            }
            if (clonicosCP > 3) {
                pesos[CategoriaAdaptativa.METRONOMO_PURO] = (pesos[CategoriaAdaptativa.METRONOMO_PURO] ?: 0.0) + 50.0
                pesos[CategoriaAdaptativa.BLOQUES_RITMO] = (pesos[CategoriaAdaptativa.BLOQUES_RITMO] ?: 0.0) + 40.0
            }
            if (latenciaCP > 2000) {
                pesos[CategoriaAdaptativa.ESCENARIO_RAPIDO] = (pesos[CategoriaAdaptativa.ESCENARIO_RAPIDO] ?: 0.0) + 45.0
                pesos[CategoriaAdaptativa.EVOCACION_SEMANTICA] = (pesos[CategoriaAdaptativa.EVOCACION_SEMANTICA] ?: 0.0) + 35.0
            }
            if (fluidezCP > 80) {
                pesos[CategoriaAdaptativa.PRONUNCIACION_COMPLEJA] = (pesos[CategoriaAdaptativa.PRONUNCIACION_COMPLEJA] ?: 0.0) + 40.0
                pesos[CategoriaAdaptativa.GUION_ESTRESANTE] = (pesos[CategoriaAdaptativa.GUION_ESTRESANTE] ?: 0.0) + 50.0
            }
        }

        if (categoriaEvitada != null) {
            pesos.remove(categoriaEvitada)
        }

        val categoriaSeleccionada = pesos.maxByOrNull { it.value }?.key ?: CategoriaAdaptativa.LECTURA_GUIADA
        val clonicos = siHay(sesiones) { take(3).map { it.bloqueosClonicos }.average() }
        val tonicos = siHay(sesiones) { take(3).map { it.bloqueosTonicos }.average() }
        val latencia = siHay(sesiones) { take(3).map { it.latenciaMediaMs }.average() }
        val fluidez = siHay(sesiones) { take(3).map { it.calificacionGlobal }.average() }

        return when (categoriaSeleccionada) {
            CategoriaAdaptativa.BLOQUES_RITMO -> generarBloques(clonicos)
            CategoriaAdaptativa.LECTURA_GUIADA -> generarLectura(fluidez)
            CategoriaAdaptativa.HABLA_ESTIRADA -> generarHablaEstirada(tonicos)
            CategoriaAdaptativa.ESCENARIO_RAPIDO -> generarEscenario(latencia)
            CategoriaAdaptativa.GUION_ESTRESANTE -> generarGuionEstresante(fluidez)
            CategoriaAdaptativa.METRONOMO_PURO -> generarMetronomoPuro(clonicos)
            CategoriaAdaptativa.BIOFEEDBACK_RELAJACION -> generarBiofeedbackRelajacion(tonicos)
            CategoriaAdaptativa.EVOCACION_SEMANTICA -> generarEvocacionSemantica()
            CategoriaAdaptativa.PRONUNCIACION_COMPLEJA -> generarPronunciacionCompleja()
        }
    }

    private fun <T> siHay(lista: List<Any>, bloque: List<com.itsx.speaktutor.ui.screens.SesionClinica>.() -> T): Double {
        val casted = lista as? List<com.itsx.speaktutor.ui.screens.SesionClinica> ?: return 0.0
        return if (casted.isNotEmpty()) casted.bloque() as? Double ?: 0.0 else 0.0
    }

    // =========================================================================
    // 1. BLOQUES DE RITMO (55 VARIANTES EXACTAS)
    // =========================================================================
    private fun generarBloques(clonicos: Double): EjercicioGenerado {
        val bloques = listOf(
            "El / a / ma / ne / cer / es / her / mo / so", "Cuan / do / llue / ve / me / que / do",
            "La / tec / no / lo / gí / a / a / van / za / rá / pi / do", "Un / ca / fé / ca / lien / te / por / fa / vor",
            "Siem / pre / hay / que / mi / rar / a / de / lan / te", "El / pe / rro / de / San / Ro / que / no / tie / ne",
            "Me / gus / ta / ca / mi / nar / por / el / par / que", "Las / es / tre / llas / bri / llan / de / no / che",
            "Hoy / es / un / buen / dí / a / pa / ra / a / pren / der", "La / mú / si / ca / cal / ma / la / an / sie / dad",
            "Res / pi / rar / pro / fun / do / a / yu / da / mu / cho", "Man / te / ner / la / cal / ma / es / la / cla / ve",
            "El / a / gua / del / rí / o / es / muy / frí / a", "La / mon / ta / ña / se / ve / a / lo / le / jos",
            "Tra / ba / jar / en / e / qui / po / es / me / jor", "El / de / por / te / da / mu / cha / e / ner / gí / a",
            "U / na / bue / na / lec / tu / ra / te / re / la / ja", "El / vien / to / so / pla / con / mu / cha / fuer / za",
            "La / ciu / dad / nun / ca / duer / me / de / no / che", "El / au / to / bus / lle / ga / rá / muy / pron / to",
            "La / co / mi / da / es / tá / lis / ta / en / la / me / sa", "Un / a / bra / zo / fuer / te / pa / ra / ti",
            "El / pro / yec / to / se / en / tre / ga / el / vier / nes", "La / pan / ta / lla / del / ci / ne / es / gran / de",
            "Es / cu / char / a / los / de / más / es / bue / no", "El / a / i / re / a / con / di / cio / na / do",
            "Los / co / lo / res / del / ar / co / i / ris", "La / bi / ci / cle / ta / a / mar / i / lla",
            "U / ti / li / zar / el / me / tró / no / mo / sir / ve", "Las / o / las / del / mar / son / al / tas",
            "El / re / loj / mar / ca / las / do / ce / en / pun / to", "La / ven / ta / na / es / tá / a / bier / ta",
            "Es / cri / bir / un / li / bro / to / ma / tiem / po", "El / pa / so / del / tiem / po / no / se / de / tie / ne",
            "La / me / mo / ria / se / de / be / e / jer / ci / tar", "El / a / pren / di / za / je / es / con / ti / nuo",
            "Los / a / ni / ma / les / de / ben / ser / li / bres", "El / in / vier / no / tra / e / mu / cho / frí / o",
            "La / pri / ma / ve / ra / lle / na / to / do / de / flo / res", "Un / via / je / lar / go / de / ja / lec / cio / nes",
            "El / a / mor / es / el / mo / tor / del / mun / do", "La / pa / cien / cia / es / u / na / gran / vir / tud",
            "Los / sue / ños / se / pue / den / ha / cer / re / a / li / dad", "El / es / fuer / zo / siem / pre / da / fru / tos",
            "La / na / tu / ra / le / za / es / muy / sa / bia", "El / cie / lo / a / zul / me / da / es / pe / ran / za",
            "La / in / te / li / gen / cia / se / pue / de / cul / ti / var", "El / res / pe / to / es / fun / da / men / tal",
            "La / a / mis / tad / ver / da / de / ra / per / du / ra", "El / si / len / cio / tam / bién / co / mu / ni / ca",
            "Ar / qui / tec / tu / ra / en / la / nu / be", "Mi / gra / ción / de / da / tos / se / gu / ra",
            "Con / te / ne / do / res / en / pro / duc / ción", "Re / de / s / neu / ro / na / les / ar / ti / fi / cia / les",
            "In / ge / nie / rí / a / de / soft / ware / mo / der / na"
        )
        val bpm = if (clonicos > 3) 50 else 70
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.BLOQUES_RITMO,
            titulo = "Bloques de Ritmo Dinámicos IA",
            bpmSugerido = bpm,
            textoPractica = bloques.random(),
            instruccionEspecial = "Sincroniza estrictamente cada sílaba separada (/) con un golpe del metrónomo."
        )
    }

    // =========================================================================
    // 2. LECTURA GUIADA (55 PARRÁFROS EXACTOS)
    // =========================================================================
    private fun generarLectura(fluidez: Double): EjercicioGenerado {
        val parrafos = listOf(
            "La computación cuántica promete revolucionar el procesamiento de datos al usar qubits, permitiendo resolver en segundos problemas que tomarían milenios.",
            "El ajolote mexicano tiene la asombrosa capacidad de regenerar extremidades completas, incluyendo partes de su corazón y cerebro, sin dejar cicatrices.",
            "Durante el Renacimiento, el arte y la ciencia se fusionaron. Da Vinci estudió anatomía humana en secreto para que sus pinturas fueran perfectamente realistas.",
            "Las auroras boreales se producen cuando las partículas cargadas del sol chocan contra la atmósfera terrestre, creando un espectáculo de luces verdes y violetas.",
            "El océano profundo sigue siendo un misterio. A miles de metros bajo el agua, existen criaturas bioluminiscentes que generan su propia luz para sobrevivir.",
            "Aprender un nuevo idioma no solo mejora la comunicación, sino que retrasa el envejecimiento cerebral al crear nuevas conexiones neuronales llamadas sinapsis.",
            "La arquitectura sostenible busca integrar los edificios con su entorno natural, utilizando paneles solares y sistemas de recolección de agua de lluvia.",
            "En la antigua Roma, el Coliseo podía albergar a más de cincuenta mil espectadores, y su sistema de túneles subterráneos era una maravilla de la ingeniería.",
            "El café es la segunda bebida más consumida del mundo después del agua. Su descubrimiento se le atribuye a un pastor de cabras en Etiopía hace siglos.",
            "La migración de las mariposas monarca es un fenómeno increíble. Viajan miles de kilómetros desde Canadá hasta los bosques de Michoacán en México.",
            "La inteligencia artificial generativa puede redactar textos, componer música y programar código, desafiando nuestra percepción de la creatividad humana.",
            "El telescopio James Webb ha permitido observar galaxias que se formaron poco después del Big Bang, reescribiendo los libros de astronomía moderna.",
            "El ciclo del agua es un proceso vital. La evaporación, condensación y precipitación mantienen el equilibrio de los ecosistemas en todo el planeta.",
            "Practicar la gratitud diariamente tiene efectos medibles en la salud mental. Reduce el estrés y aumenta la producción de serotonina en el cerebro.",
            "La Gran Muralla China no es una sola estructura continua, sino una red de fortificaciones construidas por diferentes dinastías a lo largo de dos milenios.",
            "El bambú es una de las plantas de más rápido crecimiento en el mundo. Algunas especies pueden crecer hasta un metro completo en un solo día.",
            "La música clásica tiene un impacto positivo en la concentración. Estudios demuestran que escuchar a Mozart mejora temporalmente el razonamiento espacial.",
            "Los agujeros negros tienen una gravedad tan inmensa que ni siquiera la luz puede escapar de ellos. Se forman cuando estrellas masivas colapsan sobre sí mismas.",
            "La dieta mediterránea, rica en aceite de oliva, pescado y vegetales, es considerada mundialmente como una de las más saludables para el corazón.",
            "El sistema inmunológico humano es un ejército microscópico. Los glóbulos blancos patrullan constantemente la sangre en busca de virus y bacterias.",
            "La invención de la imprenta por Gutenberg democratizó el conocimiento. Antes de ella, los libros eran copiados a mano por monjes durante años.",
            "El Amazonas produce alrededor del veinte por ciento del oxígeno del mundo y alberga a millones de especies de plantas y animales aún no descubiertos.",
            "La teoría de la relatividad de Einstein demostró que el tiempo y el espacio están entrelazados, cambiando para siempre nuestra comprensión del universo.",
            "El origami es el arte japonés del plegado de papel. Requiere paciencia y precisión geométrica para transformar una hoja plana en una figura tridimensional.",
            "La energía geotérmica aprovecha el calor interno de la Tierra para generar electricidad de forma limpia, continua y sin depender de las condiciones climáticas.",
            "Las hormigas son insectos eusociales asombrosos. Pueden cargar varias veces su propio peso y coordinarse en colonias de millones de individuos.",
            "El cine mudo dependía exclusivamente de la expresión corporal y facial de los actores, acompañados por un pianista en vivo dentro de la sala de proyección.",
            "La fotosíntesis es el proceso mediante el cual las plantas convierten la luz solar y el dióxido de carbono en oxígeno y glucosa, sustentando la vida terrestre.",
            "El cerebro humano está compuesto por un setenta y tres por ciento de agua. Una deshidratación leve puede afectar severamente la memoria y la atención.",
            "La Revolución Industrial marcó un antes y un después en la historia. Las máquinas a vapor transformaron la producción artesanal en manufactura a gran escala.",
            "El sistema solar se formó hace más de cuatro mil quinientos millones de años a partir del colapso gravitacional de una gran nube molecular gigante.",
            "Los arrecifes de coral son conocidos como las selvas tropicales del mar, ya que sustentan a una cuarta parte de toda la vida marina existente.",
            "La criptografía moderna protege nuestras comunicaciones financieras globales mediante algoritmos matemáticos complejos que son prácticamente imposibles de descifrar.",
            "El ejercicio aeróbico regular incrementa el tamaño del hipocampo, el área cerebral asociada con la memoria verbal y el aprendizaje cognitivo.",
            "La penicilina fue descubierta por accidente en mil novecientos veintiocho por Alexander Fleming, marcando el inicio de la era de los antibióticos modernos.",
            "El desierto de Atacama en Chile es el lugar no polar más árido del mundo, donde algunas estaciones meteorológicas jamás han registrado una sola gota de lluvia.",
            "La literatura fantástica nos permite explorar mundos alternativos y reflexionar sobre la condición humana a través de metáforas épicas y personajes memorables.",
            "La neuroplasticidad demuestra que el cerebro humano puede reorganizarse formando nuevas conexiones neuronales a lo largo de toda nuestra vida adulta.",
            "El vuelo espacial tripulado ha cambiado nuestra perspectiva cósmica, permitiéndonos observar nuestro frágil planeta azul flotando en la inmensidad oscura.",
            "La nanotecnología trabaja a una escala ultramicroscópica para diseñar materiales con propiedades físicas y químicas completamente revolucionarias.",
            "La música jazz se originó a finales del siglo diecinueve en Nueva Orleans, fusionando elementos de la tradición musical africana con la armonía europea.",
            "El Parque Nacional Yellowstone fue establecido como el primer parque nacional del mundo, protegiendo géiseres únicos y una fauna salvaje inigualable.",
            "La psicología positiva estudia las bases del bienestar humano y la felicidad, enfocándose en las fortalezas personales más allá de los trastornos clínicos.",
            "Los satélites meteorológicos en órbita geoestacionaria nos permiten rastrear huracanes y tormentas severas con una precisión milimétrica en tiempo real.",
            "La historia de la cartografía refleja la evolución del pensamiento humano y nuestro incansable deseo de explorar y comprender cada rincón del planeta.",
            "El diseño de interfaces de usuario busca simplificar la interacción digital humana mediante principios ergonómicos de claridad, eficiencia y accesibilidad.",
            "La biodiversidad de los bosques tropicales húmedos es un tesoro farmacológico incalculable que aún guarda innumerables curas médicas por descubrirse.",
            "El desarrollo de software ágil fomenta la colaboración continua, la flexibilidad ante los cambios y la entrega rápida de valor funcional al usuario final.",
            "La filosofía estoica nos enseña a distinguir entre lo que podemos controlar y lo que no, cultivando la resiliencia mental ante cualquier adversidad.",
            "La exploración de Marte busca responder una de las preguntas fundamentales de la humanidad: ¿existió alguna vez vida microbiana en otro planeta?",
            "El arte rupestre encontrado en cuevas profundas nos conecta directamente con el pensamiento simbólico y espiritual de nuestros antepasados más lejanos.",
            "La economía circular propone rediseñar los procesos productivos para eliminar los residuos y maximizar el aprovechamiento continuo de los recursos naturales.",
            "El ajedrez es un juego de estrategia milenario que entrena la concentración, la previsión táctica y la toma de decisiones bajo presión temporal.",
            "La biometría moderna utiliza patrones físicos únicos, como la huella dactilar o el iris, para garantizar sistemas de autenticación altamente seguros.",
            "La inteligencia emocional es un factor determinante en el liderazgo efectivo, la resolución pacífica de conflictos y la empatía interpersonal."
        )
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.LECTURA_GUIADA,
            titulo = "Lectura Continua de Contexto",
            bpmSugerido = 0,
            textoPractica = parrafos.random(),
            instruccionEspecial = if (fluidez > 80) "Reto: Mantén el flujo de aire ininterrumpido a lo largo de toda la oración." else "Apoyo: Respira profundo al ver cada punto y cada coma."
        )
    }

    // =========================================================================
    // 3. HABLA ESTIRADA / INICIOS SUAVES (50 FRASES EXACTAS)
    // =========================================================================
    private fun generarHablaEstirada(tonicos: Double): EjercicioGenerado {
        val frases = listOf(
            "Ffffácilmente terminamos el proyecto de ssssoftware a tiempo.",
            "Ssssiempre que salgo, el ssssol brilla en la sssciudad.",
            "Jjjjustamente ayer, el jjjjefe me pidió un jjjjuego nuevo.",
            "Sssseguramente el ssssistema de sssseguridad es sssseguro.",
            "Ffffelizmente, la ffffamilia ffffestejó en la ffffinca.",
            "Zzzzumbaba el zzzzancudo cerca del zzzzoológico.",
            "Ssssuavemente, el ssssonido sssse desvaneció en el ssssilencio.",
            "Jjjjamás jjjjuzgues un jjjjuguete por su jjjjaula.",
            "Ffffabricaron ffffuertes ffffrenos para el fffferrocarril.",
            "Ssssensiblemente, la sssseñora ssssirvió la ssssopa.",
            "Mmmmañana mmmmisma mmmme mmmmudará a Mmmmérida.",
            "Nnnnunca nnnnoté que la nnnnaturaleza era tan nnnnoble.",
            "Mmmmientras mmmmiraba, la mmmmariposa mmmmostró sus mmmmanchas.",
            "Nnnningún nnnniño nnnnecesita nnnnegar su nnnnaturaleza.",
            "Mmmmuchos mmmmarineros mmmmueven el mmmmarco de mmmmadera.",
            "Nnnnuestro nnnnuevo nnnnegocio nnnnació en Nnnnoviembre.",
            "Mmmmisteriosamente, el mmmmapa mmmmarcó la mmmmisteriosa mmmmontaña.",
            "Nnnnormalmente nnnno nnnnadamos de nnnnoche.",
            "Mmmmi mmmmejor mmmmemoria es de la mmmmúsica de mmmmamá.",
            "Nnnnueve nnnnubes nnnnegras nnnnavegan al nnnnorte.",
            "Ffffrenético ffffusilamiento de ffffalsas ffffórmulas.",
            "Ssssorpresivamente sssurgió un ssssubmarino sssilencioso.",
            "Jjjoven jjjornalero jjjura jjjugar con jjjardín.",
            "Vvvvelozmente vvvuelo hacia vvviejos vvvalles verdes.",
            "Rrrápidamente rrronda un rrrinoceronte rrruidoso.",
            "Lllluviosamente llllega el lllunes a lllima.",
            "Bnnn... Bbbienvenido al bbbello bbbosque bbbajo.",
            "Dddelicadamente dddibuja un dddelfín dddorado.",
            "Gggigantesco gggrupo gggira en el gggimnasio.",
            "Pppacientemente ppprepara un pppostre ppprofesional.",
            "Tttanquilamente tttoca la tttrompeta tttallada.",
            "Chhharly chhharlaba chhhistosamente en chhhile.",
            "LLllorosamente lllamenta la lllluvia en el lllago.",
            "Rrrica rrrusia rrronda por rrronda.",
            "Sssiempre sssuena sssuave la sssinfonía sssagrada.",
            "Mmmaravillosa mmmeta mmmerece mmmayor mmmando.",
            "Nnnorteño nnnavegante nnnavega nnnuevos nnnmares.",
            "Ffffabricante ffffirma ffffactura ffffiscal.",
            "Zzzafiro zzzurdo zzzuma con zzzampa.",
            "Jjjulia jjjunta jjjugos de jjjitomate.",
            "Sssaludable sssorbo de sssumo sssilvestre.",
            "Mmmédico mmmontañés mmmide su mmmPulso.",
            "Nnnatación nnnocturna nnnecesita nnnadar.",
            "Ffflorido fffestival fffelicita a fffamiliares.",
            "Vvvocalista vvvocaliza vvvatios vvvirtuales.",
            "Rrrío rrrápido rrriega rrranchos.",
            "Lllibreta lllimpia lllleva lllápices.",
            "Bbbsque bbbuscador bbboga bbbien.",
            "Dddistinguido dddirector dddirige dddiscusión.",
            "Sssoberbio sssargento sssaluda sssoldados."
        )
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.HABLA_ESTIRADA,
            titulo = "Inicios Suaves (Contacto Ligero)",
            bpmSugerido = 0,
            textoPractica = frases.random(),
            instruccionEspecial = "Elongación de sonido inicial: Mantén la primera letra estirada y con volumen muy bajo para evitar bloqueos tónicos."
        )
    }

    // =========================================================================
    // 4. ESCENARIOS RÁPIDOS (50 ESCENARIOS EXACTOS)
    // =========================================================================
    private fun generarEscenario(latencia: Double): EjercicioGenerado {
        val escenarios = listOf(
            Triple("Taxi", "Buenas tardes, ¿a dónde le llevo?", "Buenas tardes, a la estación de autobuses por favor."),
            Triple("Panadería", "¿Qué le damos el día de hoy?", "Me da dos panes dulces y una baguette, por favor."),
            Triple("Cine", "¿Para qué función busca boletos?", "Dos boletos para la película de las seis, por favor."),
            Triple("Peluquería", "¿Cómo le cortamos el cabello hoy?", "Solo las puntas y un poco corto de los lados, por favor."),
            Triple("Restaurante", "¿Listos para ordenar o necesitan más tiempo?", "Ya estamos listos, yo quiero la ensalada César."),
            Triple("Recepción", "Aquí tiene su llave, habitación trescientos dos.", "Muchas gracias, ¿el ascensor está por allá?"),
            Triple("Heladería", "¿De qué sabor le preparo su helado?", "Uno doble de chocolate y vainilla en cono, por favor."),
            Triple("Aeropuerto", "¿Lleva equipaje para documentar o solo de mano?", "Solo llevo esta maleta de mano y mi mochila."),
            Triple("Banco", "Siguiente en la fila. ¿Qué trámite va a realizar?", "Vengo a hacer un depósito en efectivo a esta cuenta."),
            Triple("Tienda Ropa", "¿Gusta que le ayude a buscar alguna talla?", "No, muchas gracias, por el momento solo estoy mirando."),
            Triple("Farmacia", "Buenos días, ¿busca algún medicamento en especial?", "Sí, necesito un frasco de aspirinas y jarabe para la tos."),
            Triple("Librería", "¿Le puedo ayudar a encontrar algún título?", "Busco el libro de arquitectura de software avanzado."),
            Triple("Cafetería", "¿Le pongo leche entera o deslactosada a su café?", "Por favor ponle leche deslactosada y sin azúcar añadida."),
            Triple("Gasolinera", "¿Le pongo magna, premium o diésel?", "Llénala con gasolina magna, por favor, por favor."),
            Triple("Gimnasio", "Hola, ¿vienes a inscribirte o a una clase muestra?", "Vengo a mi primer día de prueba en el área de pesas."),
            Triple("Supermercado", "¿Encontró todo lo que buscaba el día de hoy?", "Sí, todo excelente, muchas gracias por la atención."),
            Triple("Veterinaria", "Buenas tardes, ¿qué le pasa a su mascota?", "Viene a su revisión de rutina y vacunación anual."),
            Triple("Óptica", "¿Te puedo ayudar a revisar tu graduación?", "Sí, noto que me cuesta leer de noche en la computadora."),
            Triple("Estacionamiento", "Su boleto por favor. ¿Perdió su comprobante?", "No, aquí lo tengo conmigo, listo para pagar."),
            Triple("Florería", "¿Qué tipo de arreglo floral prefiere?", "Un ramo sencillo con girasoles y follaje verde."),
            Triple("Ferretería", "¿Busca algún tornillo o herramienta específica?", "Necesito un desarmador plano y taquetes de un cuarto."),
            Triple("Lavandería", "¿Cuándo pasaría por sus camisas limpias?", "Me gustaría pasar por ellas el próximo viernes por la tarde."),
            Triple("Papelería", "¿Tiene copias tamaño carta a color?", "Sí, necesito imprimir y encuadernar este documento escolar."),
            Triple("Metro", "Disculpe, ¿esta línea va hacia el centro?", "Sí, directito al centro, faltan tres estaciones más."),
            Triple("Hotel", "¿Cuál es su nombre para la reservación?", "A nombre de Carlos Acosta, suite ejecutiva doble."),
            Triple("Museo", "¿Desea boleto general o incluye la exposición temporal?", "Quiero el acceso completo que incluye la sala temporal."),
            Triple("Estadio", "Muestre su boleto y código QR en el lector.", "Listo, aquí está escaneado desde mi teléfono celular."),
            Triple("Zoológico", "¿Cuántas entradas de adulto requiere hoy?", "Serían dos boletos de adulto y uno de estudiante."),
            Triple("Renta Autos", "¿Requiere seguro ampliado contra daños?", "Sí, por favor agregue cobertura amplia por los tres días."),
            Triple("Hospital", "¿A qué área o consultorio se dirige?", "Voy al área de laboratorio clínico para unos análisis."),
            Triple("Sastrería", "¿Qué ajuste necesita este pantalón de vestir?", "Necesito que le suban cinco centímetros de la botaguarda."),
            Triple("Joyería", "¿Le muestro los anillos de plata o de oro?", "Quisiera ver los modelos minimalistas en plata fina."),
            Triple("Juguetería", "¿Busca algo para niño o niña de qué edad?", "Es para un niño de ocho años que ama la ciencia."),
            Triple("Inmobiliaria", "¿Agendamos cita para ver el departamento amueblado?", "Sí, me gustaría visitarlo este próximo sábado a mediodía."),
            Triple("Teatro", "¿Prefiere luneta o preferente general?", "Prefiero sección luneta fila central, por favor."),
            Triple("Editorial", "¿Trae su manuscrito impreso o en formato digital?", "Lo traigo cargado en esta memoria USB en formato PDF."),
            Triple("Editorial", "Buenos días, ¿viene a recoger su tiraje?", "Sí, vengo por quinientos ejemplares impresos."),
            Triple("Escuela", "¿A qué grado escolar solicita informes?", "Para el primer semestre de ingeniería en sistemas."),
            Triple("Soporte Técnico", "Bienvenido a mesa de ayuda. ¿Cuál es su reporte?", "Mi equipo portátil no enciende tras la actualización."),
            Triple("Agencia Viajes", "¿A qué destino planea sus próximas vacaciones?", "Planeo viajar a Canadá a finales de este mismo año."),
            Triple("Estudio Grabación", "¿Listos para probar los micrófonos de condensador?", "Todo listo, probando uno dos tres probando audio."),
            Triple("Comisaría", "Buenas tardes, ¿en qué podemos servirle?", "Vengo a tramitar la reposición de mi identificación."),
            Triple("Centro Comercial", "¿Sabe dónde queda la salida de emergencia?", "Sí, está señalizada cruzando el pasillo principal."),
            Triple("Paquetería", "Vengo a rastrear un paquete internacional con guía.", "Claro, permítame el número de guía para revisarlo."),
            Triple("Estudio Fotográfico", "¿Sesión individual o fotográfica familiar?", "Una sesión de retrato profesional para mi currículum."),
            Triple("Editorial Revista", "Hola, vengo a proponer un artículo técnico.", "Excelente, preséntanos tu propuesta redactada."),
            Triple("Centro Cultural", "¿A qué taller de música desea inscribirse?", "Al taller de guitarra clásica avanzada los martes."),
            Triple("Auditorio", "¿Dónde debo colocar mi acreditación de prensa?", "Colóquela visible en la solapa izquierda por favor."),
            Triple("Editor", "Hola, ¿revisaste el capítulo sobre redes?", "Sí, ya realicé las correcciones y diagramas solicitados."),
            Triple("Laboratorio", "¿Entregamos muestra en ayunas obligatoriamente?", "Sí, doce horas exactas de ayuno sin consumir líquidos.")
        )
        val esc = escenarios.random()
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.ESCENARIO_RAPIDO,
            titulo = "Simulación Rápida: ${esc.first}",
            bpmSugerido = 0,
            textoPractica = esc.third,
            instruccionEspecial = if (latencia > 2000) "Inhala lento antes de hablar. La IA no te apresurará." else "Responde con naturalidad a la pregunta.",
            audioInterlocutor = esc.second
        )
    }

    // =========================================================================
    // 5. GUIONES DE ALTA DEMANDA / ESTRESANTES (50 GUIONES EXACTOS)
    // =========================================================================
    private fun generarGuionEstresante(fluidez: Double): EjercicioGenerado {
        val guiones = listOf(
            Triple("Entrevista de Trabajo", "Muy interesante su perfil. ¿Por qué le gustaría trabajar con nosotros?", "Porque admiro su cultura laboral y creo que puedo aportar mucho valor a su equipo."),
            Triple("Aumento de Sueldo", "He notado que has tomado más responsabilidades. ¿De qué quieres hablar?", "Así es. Basado en mis resultados, me gustaría solicitar un ajuste salarial formal."),
            Triple("Confrontar Amigo", "Solo era una broma ayer, no pensé que te lo tomarías tan a pecho.", "Puede ser, pero cruzaste un límite personal y te pido que no lo vuelvas a hacer."),
            Triple("Queja de Internet", "Entiendo que no tiene red. ¿Me proporciona su número de cuenta a diez dígitos?", "Claro, es el cinco, cinco, ocho, nueve, tres, uno, cero, cuatro."),
            Triple("Presentación Directivos", "Vemos en la pantalla la gráfica. ¿A qué se debe este aumento en ventas?", "Principalmente a la nueva campaña de marketing digital que lanzamos el mes pasado."),
            Triple("Reclamar Platillo", "Aquí tiene su pasta con mariscos, señor. Buen provecho.", "Disculpa, creo que hubo una confusión. Yo ordené la pasta con pollo."),
            Triple("Renuncia Laboral", "Vaya, es una sorpresa que renuncies. ¿Tienes alguna otra oferta de trabajo?", "Sí, recibí una oferta que se alinea más con mis metas profesionales a largo plazo."),
            Triple("Llamada de Emergencia", "Nueve uno uno. ¿Hay personas lesionadas? ¿Cuál es la dirección exacta?", "Sí, hay un herido. Estamos en la esquina de Avenida Juárez y Calle Morelos."),
            Triple("Defensa de Tesis", "Su metodología parece incompleta. ¿Cómo justifica este margen de error?", "El margen se calibró considerando las fluctuaciones térmicas del entorno de prueba."),
            Triple("Negociación Proveedor", "El costo subió un veinte por ciento este mes. ¿Aceptas la tarifa?", "No podemos aceptar ese incremento sin revisar antes el contrato de exclusividad."),
            Triple("Disculpa Vecino", "Tus perros ladran toda la noche y no me dejan dormir.", "Te pido una disculpa sincera. Tomaré medidas inmediatas esta misma noche."),
            Triple("Discusión de Pareja", "Siento que ya no me escuchas cuando te hablo de mis cosas.", "Tienes razón, he estado muy distraído con el trabajo, pero quiero escucharte ahora."),
            Triple("Revisión de Crédito", "Su tarjeta fue rechazada por fondos insuficientes en la terminal.", "Qué extraño, permítame autorizar la transacción desde mi aplicación bancaria."),
            Triple("Falla de Servidor", "El clúster principal de producción cayó hace diez minutos.", "Iniciando protocolo de respaldo y levantando contenedor de emergencia en la nube."),
            Triple("Baja de Servicio", "¿Por qué desea cancelar su membresía anual con nosotros?", "Porque mudé mi residencia fuera del estado y ya no podré utilizar las instalaciones."),
            Triple("Reclamo de Garantía", "El equipo presenta daños estéticos, la garantía no aplica.", "El rasguño ya venía de fábrica al abrir la caja sellada de origen."),
            Triple("Asamblea Condóminos", "Nadie quiere pagar la cuota extraordinaria de mantenimiento.", "Es obligatoria por ley para reparar la cisterna central del edificio."),
            Triple("Entrevista Radio", "Cuéntenos, ¿cuál es el propósito principal de su aplicación?", "Ayudar a pacientes con tartamudez mediante biofeedback de audio en tiempo real."),
            Triple("Aclaración de Impuestos", "Su declaración fiscal presenta discrepancias en el ejercicio anterior.", "Presentaré las facturas originales para aclarar el saldo a favor pendiente."),
            Triple("Auditoría de Seguridad", "¿Dónde están almacenadas las llaves privadas de cifrado?", "Residen en un cofre de seguridad con cifrado asimétrico fuera de línea."),
            Triple("Examen Profesional", "Proceda a explicar su proyecto final ante el jurado sínodo.", "Con gusto. Desarrollé una arquitectura de microservicios tolerante a fallos."),
            Triple("Reclamo Seguros", "Su póliza no cubre daños ocasionados por fenómenos hidrometeorológicos.", "Revisaré la cláusula especial de cobertura amplia que contraté en enero."),
            Triple("Rechazo de Propuesta", "Lamento informarle que elegimos a otro candidato para la vacante.", "Agradezco el tiempo y la retroalimentación brindada durante el proceso."),
            Triple("Reunión Urgente", "El cliente canceló el contrato debido al retraso en la entrega.", "Comprendo la gravedad. Llamaré de inmediato para ofrecer una compensación."),
            Triple("Aclaración de Factura", "Este cobro doble no aparece registrado en nuestro sistema interno.", "Aquí tengo el comprobante digital emitido por su propia pasarela de pago."),
            Triple("Defensa Legal", "El ministerio público solicita una prórroga de dos meses.", "Nos oponemos formalmente por violentar los plazos constitucionales del imputado."),
            Triple("Negativa de Préstamo", "Su historial crediticio no cumple con los requisitos mínimos actuales.", "Solicitaré una revisión manual aportando mis estados de cuenta recientes."),
            Triple("Reclamo de Aerolínea", "Su vuelo fue cancelado por condiciones climáticas adversas.", "Exijo la reasignación inmediata en el siguiente vuelo disponible sin costo."),
            Triple("Evaluación de Desempeño", "Tus métricas de entrega disminuyeron este último trimestre.", "Reconozco el bajón y presentaré un plan de recuperación para la siguiente semana."),
            Triple("Citatorio Escolar", "Su hijo ha mostrado conductas disruptivas en clase de matemáticas.", "Hablaré con él esta tarde para entender qué está ocurriendo en la escuela."),
            Triple("Disputa de Linderos", "Esta barda invade medio metro de mi propiedad privada.", "Tengo las escrituras originales y el plano topográfico que delimitan el terreno."),
            Triple("Reclamo de Encomienda", "El paquete llegó completamente abierto y falta mercancía dentro.", "Levantaré un reporte formal con evidencia fotográfica del estado en que llegó."),
            Triple("Queja en Hospital", "Llevamos cuatro horas esperando en urgencias sin recibir atención.", "Exijo hablar con el médico en turno para conocer el estatus del paciente."),
            Triple("Auditoría Financiera", "No encuentro la factura correspondiente al asiento contable trece.", "Está archivada en la carpeta digital de gastos operativos del mes pasado."),
            Triple("Aclaración Bancaria", "Este cargo desconocido de cinco mil pesos no lo reconocemos.", "Solicito la cancelación inmediata del plástico y la aclaración del cargo."),
            Triple("Queja de Inquilinato", "El calentador de agua dejó de funcionar y no hay gas.", "Enviaré al técnico especialista a primera hora de mañana sin falta."),
            Triple("Entrevista Televisión", "¿No teme que la inteligencia artificial desplace a los humanos?", "Más que un reemplazo, la IA debe verse como una herramienta de apoyo colaborativo."),
            Triple("Aclaración de Aduanas", "Su paquete se encuentra retenido por pago de aranceles.", "Realizaré el pago correspondiente en ventanilla para liberarlo hoy mismo."),
            Triple("Revisión Médica", "Los resultados de la biopsia muestran valores ligeramente elevados.", "Doctor, explíqueme con calma qué significan estos números en el estudio."),
            Triple("Disputa Laboral", "Se le solicita la renuncia voluntaria con indemnización mínima.", "No firmaré ningún documento hasta consultarlo con mi asesor legal laboral."),
            Triple("Reclamo de Garantía Auto", "La falla de transmisión no entra en la póliza extendida.", "El manual especifica claramente que cubre defectos de fábrica internos."),
            Triple("Junta Directiva Inversores", "El retorno de inversión trimestral estuvo por debajo de la meta.", "Implementamos nuevas estrategias operativas para revertir la tendencia actual."),
            Triple("Defensa de Presupuesto", "Su departamento solicita un presupuesto demasiado elevado este año.", "Cada peso solicitado está justificado en la optimización de servidores."),
            Triple("Aclaración de Siniestro", "El perito determinó que usted tuvo la culpa del choque.", "Tengo el video de la cámara trasera que demuestra lo contrario exactamente."),
            Triple("Reclamo Editorial", "Su libro contiene errores tipográficos en el capítulo cuatro.", "Agradezco el aviso; envéreme la fe de erratas para corregirlo de inmediato."),
            Triple("Revisión de Tesis Doctoral", "El comité exige repetir el experimento principal con más muestras.", "Realizaré las pruebas adicionales necesarias para validar la hipótesis central."),
            Triple("Negociación de Alquiler", "El dueño pretende subir la renta un treinta por ciento anual.", "Es un incremento desproporcionado respecto a la inflación oficial actual."),
            Triple("Reclamo de Facturación SAT", "Su CFDI tiene un error en el régimen fiscal receptor.", "Solicito la cancelación y reexpedición de la factura con los datos correctos."),
            Triple("Queja de Servicio Eléctrico", "Lleva todo el sector sin energía eléctrica desde anoche.", "Reporté la falla a la comisión federal exigiendo una cuadrilla urgente."),
            Triple("Aclaración de Becas", "Su beca académica fue suspendida por un error en promedio.", "Aquí presento la tira de materias oficial sellada que acredita mi promedio.")
        )
        val guion = guiones.random()
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.GUION_ESTRESANTE,
            titulo = "Guion de Alta Demanda: ${guion.first}",
            bpmSugerido = 0,
            textoPractica = guion.third,
            instruccionEspecial = "Situación estresante. Concéntrate en mantener la calma y el flujo diafragmático mientras respondes.",
            audioInterlocutor = guion.second
        )
    }

    // =========================================================================
    // 6. EVOCACIÓN SEMÁNTICA (50 CATEGORÍAS Y LISTAS EXACTAS)
    // =========================================================================
    private fun generarEvocacionSemantica(): EjercicioGenerado {
        val categorias = mapOf(
            "ANIMALES" to "Perro / Gato / León / Tigre / Caballo / Elefante / Jirafa / Oso / Lobo / Delfín",
            "FRUTAS" to "Manzana / Plátano / Naranja / Uva / Pera / Sandía / Fresa / Mango / Kiwi / Cereza",
            "PAÍSES" to "México / España / Colombia / Argentina / Chile / Francia / Japón / Canadá / Brasil",
            "COLORES" to "Rojo / Azul / Verde / Amarillo / Blanco / Negro / Morado / Naranja / Gris / Turquesa",
            "PROFESIONES" to "Doctor / Ingeniero / Abogado / Profesor / Arquitecto / Enfermera / Policía / Bombero",
            "DEPORTES" to "Fútbol / Básquetbol / Béisbol / Tenis / Natación / Atletismo / Boxeo / Golf / Ciclismo",
            "HERRAMIENTAS" to "Martillo / Desarmador / Pinzas / Llave / Taladro / Sierra / Clavo / Tornillo",
            "VEHÍCULOS" to "Automóvil / Autobús / Tren / Avión / Motocicleta / Bicicleta / Barco / Helicóptero",
            "INSTRUMENTOS" to "Guitarra / Piano / Violín / Batería / Flauta / Trompeta / Saxofón / Bajo",
            "PRENDAS" to "Camisa / Pantalón / Zapatos / Saco / Abrigo / Bufanda / Gorra / Calcetines",
            "ELECTRÓNICA" to "Computadora / Teléfono / Tableta / Televisor / Cámara / Consola / Router",
            "PLANETAS" to "Mercurio / Venus / Tierra / Marte / Júpiter / Saturno / Urano / Neptuno",
            "FLORES" to "Rosa / Girasol / Orquídea / Tulipán / Margarita / Clavel / Lirio / Azucena",
            "MUEBLES" to "Mesa / Silla / Sillón / Cama / Ropero / Escritorio / Librero / Buró",
            "ALIMENTOS" to "Arroz / Frijol / Pollo / Carne / Pescado / Huevo / Queso / Leche / Pan",
            "OCÉANOS" to "Pacífico / Atlántico / Índico / Ártico / Antártico",
            "VERBOS" to "Correr / Saltar / Pensar / Escribir / Leer / Hablar / Cantar / Dormir / Comer",
            "EMOCIONES" to "Alegría / Tristeza / Enojo / Miedo / Sorpresa / Calma / Amor / Ansiedad",
            "MATEMÁTICAS" to "Suma / Resta / Multiplicación / División / Integral / Derivada / Matriz",
            "CIENCIAS" to "Física / Química / Biología / Astronomía / Geología / Matemáticas / Lógica",
            "TECNOLOGÍA" to "Algoritmo / Base de datos / Servidor / Ciberseguridad / Nube / Kernel",
            "OFICINA" to "Pluma / Libreta / Grapadora / Carpeta / Escritorio / Silla / Impresora",
            "PASTELERÍA" to "Pastel / Galleta / Gelatina / Flan / Chocolate / Dona / Panqué / Pay",
            "BELLEZA" to "Shampoo / Jabón / Perfume / Crema / Espejo / Cepillo / Maquillaje",
            "JARDINERÍA" to "Tierra / Abono / Regadera / Manguera / Pala / Tijeras / Maceta",
            "CONSTRUCCIÓN" to "Cemento / Tabique / Varilla / Arena / Grava / Cuchara / Nivel",
            "METALES" to "Oro / Plata / Cobre / Hierro / Acero / Bronce / Aluminio / Zinc",
            "ASTROLOGÍA" to "Aries / Tauro / Géminis / Cáncer / Leo / Virgo / Libra / Escorpión",
            "UTENSILIOS" to "Cuchara / Tenedor / Cuchillo / Plato / Vaso / Taza / Servilleta",
            "CLIMA" to "Lluvia / Viento / Sol / Nube / Tormenta / Granizo / Nieves / Rayo",
            "DEPORTES_EXTREMOS" to "Paracaidismo / Surf / Escalada / Rafting / Motocross / Puenting",
            "MUSEOS" to "Louvre / Prado / Metropolitano / Vaticano / Louvre / British / Hermitage",
            "IDIOMAS" to "Español / Inglés / Francés / Alemán / Italiano / Portugués / Mandarín / Japonés",
            "GÉNEROS_CINE" to "Acción / Comedia / Drama / Terror / Suspenso / Ciencia Ficción / Animación",
            "UNIVERSIDADES" to "Harvard / MIT / Stanford / Oxford / Cambridge / UNAM / ITSX / IPN",
            "REDES_SOCIALES" to "YouTube / GitHub / LinkedIn / Twitter / Instagram / TikTok / Reddit",
            "PROGRAMACIÓN" to "Kotlin / Java / Python / JavaScript / C++ / Rust / Swift / Go",
            "SUPERHEROES" to "Batman / Superman / Spider-Man / Iron Man / Wonder Woman / Flash",
            "SUPERVIVENCIA" to "Fuego / Agua / Refugio / Brújula / Cuchillo / Cuerda / Botiquín",
            "REDES_REDES" to "Router / Switch / Firewall / Cable UTP / Fibra Óptica / Gateway",
            "JUEGOS_VIDEO" to "Mario / Zelda / Halo / Minecraft / Fortnite / God of War / Tetris",
            "MONEDAS" to "Peso / Dólar / Euro / Yen / Libra / Yuan / Real / Franco",
            "MEDICINA" to "Jeringa / Estetoscopio / Termómetro / Venda / Bisturí / Gasa",
            "ARTE" to "Pintura / Escultura / Danza / Teatro / Música / Fotografía / Arquitectura",
            "ESCUELA" to "Lápiz / Borrador / Mochila / Cuaderno / Regla / Compás / Colores",
            "TRANSPORTES" to "Avión / Barco / Tren / Metro / Autobús / Taxi / Tranvía",
            "ESPECIAS" to "Sal / Pimienta / Orégano / Comino / Canela / Ajo / Cilantro / Albahaca",
            "HOTELES" to "Suite / Cama / Alberca / Recepción / Llave / Toalla / Ascensor",
            "FIESTA" to "Música / Baile / Pastel / Globos / Invitados / Regalos / Brindis",
            "ESPACIO" to "Cohete / Estación / Satélite / Astronauta / Gravedad / Cometa / Órbita"
        )
        val categoria = categorias.entries.random()
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.EVOCACION_SEMANTICA,
            titulo = "Agilidad Mental: ${categoria.key}",
            bpmSugerido = 0,
            textoPractica = categoria.value,
            instruccionEspecial = "Lee esta lista de elementos sin detenerte a pensar. Prioriza la velocidad de evocación verbal."
        )
    }

    // =========================================================================
    // 7. PRONUNCIACIÓN COMPLEJA Y TRABALENGUAS (50 RETOS EXACTOS)
    // =========================================================================
    private fun generarPronunciacionCompleja(): EjercicioGenerado {
        val retos = listOf(
            "La paralelepípeda estructura del edificio anticonstitucional.",
            "El otorrinolaringólogo me hizo un electroencefalograma ayer.",
            "La desoxirribonucleica base de la biodiversidad pluricelular.",
            "Tres tristes tigres tragaban trigo en un trigal muy lejano.",
            "El cielo está encapotado, ¿quién lo desencapotará magistralmente?",
            "Pablito clavó un clavito en la calva de un calvito, ¿qué clavito clavó?",
            "El perro de San Roque no tiene rabo porque Ramón Ramírez se lo ha cortado.",
            "Compadre, cómpreme un coco. Compadre, coco no compro, porque el que poco coco come...",
            "La institucionalización de las telecomunicaciones internacionales.",
            "El esternocleidomastoideo me duele por la tensión muscular.",
            "Características neuropsiquiátricas de la magnetoterapia moderna.",
            "Inconmensurable responsabilidad interdisciplinaria.",
            "Desoxirribonucleico ácido sintetizado en laboratorio.",
            "Electroencefalografista especialista en neurotecnología.",
            "Hipopotomonstrosesquipedaliofobia es el miedo a las palabras largas.",
            "Tararabilla tararabilla tararabillando sin cesar.",
            "Eratóstenes midió la circunferencia terrestre con sombras.",
            "Constantinopolitana ciudad sitiada por antiguos ejércitos.",
            "Pseudopodo reptiliano desplazándose sigilosamente.",
            "Criptografía cuántica descentralizada de bloques seguros.",
            "Arquitectura microservicios tolerante a fallos distribuidos.",
            "Termodinámica aplicada a motores de combustión interna.",
            "Fotorrefractometría láser de alta precisión óptica.",
            "Paralelepípedo rectángulo de volúmenes simétricos.",
            "Otorrinolaringología pediátrica especializada en foniatría.",
            "Esplenomegalias secundarias diagnosticadas en clínica.",
            "Farmacocinética molecular de anticuerpos monoclonales.",
            "Neuroendocrinología clínica de glándulas pituitarias.",
            "Magnetohidrodinámica aplicada a plasmas estelares.",
            "Paleontología de vertebrados mesozoicos continentales.",
            "Espectrofotometría infrarroja de compuestos orgánicos.",
            "Cibernética teórica de sistemas autónomos adaptativos.",
            "Radiocomunicaciones satelitales de banda ancha.",
            "Inmunohistoquímica diagnóstica de neoplasias malignas.",
            "Nanobiotecnología celular de liberación dirigida.",
            "Biogeografía evolutiva de especies endémicas insulares.",
            "Psicolingüística cognitiva de la adquisición verbal.",
            "Metrología dimensional de instrumentos de precisión.",
            "Sociolingüística histórica de lenguas romances.",
            "Etnografía antropológica de comunidades rurales.",
            "Bioinformática estructural de proteínas globulares.",
            "Criptoanálisis algebraico de curvas elípticas.",
            "Astrofísica relativista de agujeros negros supermasivos.",
            "Termoelektrónica de semiconductores avanzados.",
            "Topología algebraica de variedades diferenciables.",
            "Geometría riemanniana de espacios curvos.",
            "Hidrodinámica computacional de fluidos viscosos.",
            "Mecánica cuántica pendular de estados coherentes.",
            "Bioquímica metabólica de carbohidratos complejos.",
            "Neurotecnología de interfaces cerebro-computadora."
        )
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.PRONUNCIACION_COMPLEJA,
            titulo = "Reto de Articulación y Trabalenguas",
            bpmSugerido = 0,
            textoPractica = retos.random(),
            instruccionEspecial = "El objetivo NO es la velocidad. Abre exageradamente la boca y marca cada sílaba trabada o compleja."
        )
    }

    // =========================================================================
    // 8. METRÓNOMO FONOLÓGICO BASE (50 PATRONES EXACTOS)
    // =========================================================================
    private fun generarMetronomoPuro(clonicos: Double): EjercicioGenerado {
        val patrones = listOf(
            "Ma - me - mi - mo - mu", "Pa - pe - pi - po - pu", "La - le - li - lo - lu",
            "Ta - te - ti - to - tu", "Sa - se - si - so - su", "Ra - re - ri - ro - ru",
            "Bla - ble - bli - blo - blu", "Cra - cre - cri - cro - cru", "Pra - pre - pri - pro - pru",
            "Tra - tre - tri - tro - tru", "Gla - gle - gli - glo - glu", "Fra - fre - fri - fro - fru",
            "Bba - bbe - bbi - bbo - bbu", "Dda - dde - ddi - ddo - ddu", "Gga - gge - ggi - ggo - ggu",
            "Kka - kke - kki - kko - kku", "Mma - mme - mmi - mmo - mmu", "Nna - nne - nni - nno - nnu",
            "Vva - vve - vvi - vvo - vvu", "Zza - zze - zzi - zzo - zzu", "Cha - che - chi - cho - chu",
            "Ppa - ppe - ppi - ppo - ppu", "Tta - tte - tti - tto - ttu", "Cca - cce - cci - cco - ccu",
            "Ssa - sse - ssi - sso - ssu", "LLla - LLle - LLli - LLlo - LLlu", "Rra - rre - rri - rro - rru",
            "Fla - fle - fli - flo - flu", "Plu - ple - pli - plo - pla", "Clu - cle - cli - clo - cla",
            "Glu - gle - gli - glo - gla", "Blu - ble - bli - blo - bla", "Flra - flre - flri - flro - flru",
            "Trla - trle - trli - trlo - trlu", "Bli - blo - blu - bla - ble", "Pli - plo - plu - pla - ple",
            "Cli - clo - clu - cla - cle", "Gli - glo - glu - gla - gle", "Fli - flo - flu - fla - fle",
            "Mma - nna - bba - dda - gga", "Ppa - tta - kka - ssa - ffa", "Vva - zza - lla - rra - cha",
            "Aba - ebe - ibi - obo - ubu", "Ata - ete - iti - oto - utu", "Apa - epe - ipi - opo - upu",
            "Ala - ele - ili - olo - ulu", "Ara - ere - iri - oro - uru", "Asa - ese - isi - oso - usu",
            "Aca - ece - ici - oco - ucu", "Afa - efe - ifi - ofo - ufu"
        )
        val bpm = if (clonicos > 4) 45 else 60
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.METRONOMO_PURO,
            titulo = "Metrónomo Fonológico Base",
            bpmSugerido = bpm,
            textoPractica = patrones.random(),
            instruccionEspecial = "Enfócate SOLO en el ritmo. Sincroniza el inicio de cada sílaba con el 'Tic' del metrónomo."
        )
    }

    // =========================================================================
    // 9. BIOFEEDBACK Y RELAJACIÓN (50 FONACIONES EXACTAS)
    // =========================================================================
    private fun generarBiofeedbackRelajacion(tonicos: Double): EjercicioGenerado {
        val fonaciones = listOf(
            "Aaaaaaaaaaaaaaaaaah", "Eeeeeeeeeeeeeeeeeeh", "Iiiiiiiiiiiiiiiiiih",
            "Ooooooooooooooooooh", "Uuuuuuuuuuuuuuuuuuh", "Mmmmmmmmmmmmmmmmmmm",
            "Sssssssssssssssssss", "Fffffffffffffffffff", "Zzzzzzzzzzzzzzzzzzz",
            "Mmmmmaaaaaaaaah", "Sssseeeeeeeeeeh", "Fffffooooooooh",
            "Vvvvvvvvvvvvvvvvvvv", "B𝗯𝗯𝗯𝗯𝗯𝗯𝗯𝗯𝗯𝗯𝗯𝗯𝗯𝗯𝗯𝗯", "Nnnnnnnnnnnnnnnnnnn",
            "Lllllllllllllllllll", "Rrrrrrrrrrrrrrrrrrr", "Chhhhhhhhhhhhhhhhhh",
            "Shhhhhhhhhhhhhhhhhh", "Jjjjjjjjjjjjjjjjjjj", "Hhhhhhhhhhhhhhhhhhh",
            "Aaaaaaah - Eeeeeeh", "Iiiiiih - Oooooooh", "Uuuuuuh - Mmmmmmm",
            "Ssssss - Ffffffff", "Zzzzzz - Vvvvvvvv", "Mmmmaaa - Nnnnaaa",
            "Llllaaa - Rrrraaa", "Bbbbaaa - Ddddaaa", "Ggggaaa - Kkkaaa",
            "Ppppaaa - Ttttaaa", "Ccccaaa - Ssssaaa", "Ffffaaa - Vvvvaaa",
            "Zzzzaaa - Ssssaaa", "Jjjjaaa - Ggggaaa", "Hhhhaaa - Mmmmmmm",
            "Ooooooh - Aaaaaah", "Eeeeeeh - Uuuuuuh", "Iiiiiih - Mmmmmmm",
            "Uuuuuuh - Ooooooh", "Aaaaaah - Iiiiiih", "Mmmmmmm - Sssssss",
            "Ffffffff - Vvvvvvv", "Zzzzzzzz - Sssssss", "Nnnnnnn - Lllllll",
            "Rrrrrrr - Sssssss", "Chhhhhh - Sssssss", "Shhhhhh - Fffffff",
            "Vvvvvvv - Zzzzzzz", "Bbbbbbb - Mmmmmmm"
        )
        val urgencia = if (tonicos > 5) "Tensión severa detectada." else "Relajación preventiva."
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.BIOFEEDBACK_RELAJACION,
            titulo = "Fonación Continua Relajada",
            bpmSugerido = 0,
            textoPractica = fonaciones.random(),
            instruccionEspecial = "$urgencia Inhala, y suelta este sonido suavemente sin forzar la garganta. Mantén el Biofeedback en verde."
        )
    }
}