package com.itsx.speaktutor.logic

import android.content.Context
import com.itsx.speaktutor.ui.screens.ProgresoStorage
import java.util.UUID

enum class CategoriaAdaptativa {
    BLOQUES_RITMO, LECTURA_GUIADA, HABLA_ESTIRADA, SIMULACION,
    METRONOMO_PURO, BIOFEEDBACK_RELAJACION, EVOCACION_SEMANTICA, PRONUNCIACION_COMPLEJA
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

    // 1. CONSEJOS BASADOS EN TENDENCIAS RECIENTES (Corto Plazo)
    fun generarConsejoContextual(context: Context): String {
        val sesiones = ProgresoStorage.obtenerSesionesClinicas(context)
        if (sesiones.isEmpty()) return "👋 ¡Bienvenido al Laboratorio IA! Completa ejercicios para que pueda analizar tu perfil clínico."

        val sesionesRecientes = sesiones.take(5)
        val promedioFluidez = sesionesRecientes.map { it.calificacionGlobal }.average()
        val tonicosRecientes = sesionesRecientes.map { it.bloqueosTonicos }.average()
        val clonicosRecientes = sesionesRecientes.map { it.bloqueosClonicos }.average()
        val latenciaReciente = sesionesRecientes.map { it.latenciaMediaMs }.average()

        return when {
            tonicosRecientes > 4 -> "🚨 Nivel alto de tensión detectado. Haremos ejercicios de Relajación (Koeppen) e Inicios Suaves."
            clonicosRecientes > 4 -> "🧱 Estás sufriendo repeticiones (clónicos). Usaremos el Metrónomo Puro para estabilizar el ritmo base."
            latenciaReciente > 2500 -> "⏱️ Tu latencia está elevada. Practicaremos Evocación y Simulaciones para agilizar tu respuesta vocal."
            promedioFluidez >= 85 -> "🌟 Tu fluidez es excelente. Hoy te pondré a prueba con Pronunciación Compleja y Lecturas fluidas."
            else -> "⚖️ Tienes fluctuaciones mixtas. Hoy combinaremos varios módulos de la app para un entrenamiento integral."
        }
    }

    // 2. GENERADOR AUTÓNOMO AVANZADO MULTI-MÓDULO
    fun generarEjercicioNuevo(context: Context): EjercicioGenerado {
        val sesiones = ProgresoStorage.obtenerSesionesClinicas(context)

        if (sesiones.isEmpty()) return generarLectura(100.0)

        val cortoPlazo = sesiones.take(3)
        val largoPlazo = sesiones.drop(3)

        val tonicosCP = cortoPlazo.map { it.bloqueosTonicos }.average()
        val clonicosCP = cortoPlazo.map { it.bloqueosClonicos }.average()
        val latenciaCP = cortoPlazo.map { it.latenciaMediaMs }.average()
        val fluidezCP = cortoPlazo.map { it.calificacionGlobal }.average()

        val tonicosLP = if (largoPlazo.isNotEmpty()) largoPlazo.map { it.bloqueosTonicos }.average() else tonicosCP
        val clonicosLP = if (largoPlazo.isNotEmpty()) largoPlazo.map { it.bloqueosClonicos }.average() else clonicosCP

        val categoria = when {
            tonicosCP > 4 -> CategoriaAdaptativa.BIOFEEDBACK_RELAJACION
            tonicosCP > tonicosLP && tonicosCP > 2 -> CategoriaAdaptativa.HABLA_ESTIRADA
            clonicosCP > 4 -> CategoriaAdaptativa.METRONOMO_PURO
            clonicosCP > clonicosLP && clonicosCP > 2 -> CategoriaAdaptativa.BLOQUES_RITMO
            latenciaCP > 2500 -> CategoriaAdaptativa.SIMULACION
            fluidezCP >= 90 -> listOf(CategoriaAdaptativa.EVOCACION_SEMANTICA, CategoriaAdaptativa.PRONUNCIACION_COMPLEJA).random()
            else -> listOf(CategoriaAdaptativa.LECTURA_GUIADA, CategoriaAdaptativa.BLOQUES_RITMO, CategoriaAdaptativa.HABLA_ESTIRADA).random()
        }

        return when (categoria) {
            CategoriaAdaptativa.BLOQUES_RITMO -> generarBloques(clonicosCP)
            CategoriaAdaptativa.LECTURA_GUIADA -> generarLectura(fluidezCP)
            CategoriaAdaptativa.HABLA_ESTIRADA -> generarHablaEstirada(tonicosCP)
            CategoriaAdaptativa.SIMULACION -> generarSimulacion(latenciaCP)
            CategoriaAdaptativa.METRONOMO_PURO -> generarMetronomoPuro(clonicosCP)
            CategoriaAdaptativa.BIOFEEDBACK_RELAJACION -> generarBiofeedbackRelajacion(tonicosCP)
            CategoriaAdaptativa.EVOCACION_SEMANTICA -> generarEvocacionSemantica()
            CategoriaAdaptativa.PRONUNCIACION_COMPLEJA -> generarPronunciacionCompleja()
        }
    }

    // --- EXPANSIONES DE GENERADORES DE EJERCICIOS ---

    private fun generarBloques(clonicos: Double): EjercicioGenerado {
        val sujetos = listOf(
            "El pi / lo / to", "La doc / to / ra", "Mi her / ma / no", "El in / ge / nie / ro",
            "A / que / lla / jo / ven", "Un es / tu / dian / te", "El / pe / rro / blan / co",
            "La / ca / sa / gran / de", "El / au / to / mó / vil", "El / pro / fe / sor",
            "La / com / pu / ta / do / ra", "El / ár / bol / ver / de"
        )
        val verbos = listOf(
            "na / ve / ga / ba", "es / cu / cha / ba", "pro / gra / ma / ba", "ca / mi / na / ba",
            "res / pon / dí / a", "ob / ser / va / ba", "co / rrí / a", "dor / mí / a",
            "sa / lu / da / ba", "com / pra / ba", "es / pe / ra / ba", "lle / ga / ba"
        )
        val complementos = listOf(
            "por / la / ciu / dad", "en / el / ser / vi / dor", "con / mu / cha / cal / ma",
            "du / ran / te / la / tar / de", "ha / cia / su / ca / sa", "en / el / par / que",
            "to / da / la / no / che", "por / la / ma / ña / na", "con / sus / a / mi / gos",
            "en / la / bi / blio / te / ca", "muy / de / pri / sa", "sin / pro / ble / mas"
        )

        val fraseDinamica = "${sujetos.random()} / ${verbos.random()} / ${complementos.random()}"
        val bpm = if (clonicos > 3) 50 else 70

        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.BLOQUES_RITMO,
            titulo = "Bloques de Ritmo Dinámicos",
            bpmSugerido = bpm,
            textoPractica = fraseDinamica,
            instruccionEspecial = "Sincroniza cada sílaba separada con un golpe del metrónomo para evitar repeticiones."
        )
    }

    private fun generarLectura(fluidez: Double): EjercicioGenerado {
        val parrafos = listOf(
            "Caminar por la mañana temprano, ayuda a despejar la mente de las preocupaciones y te prepara para un día muy productivo.",
            "En el mundo de la tecnología actual, la computación en la nube permite a las empresas escalar sus recursos de forma inmediata y segura.",
            "Durante la temporada de otoño en el bosque, las hojas caen lentamente sobre los senderos formando un hermoso paisaje de colores cálidos.",
            "El desarrollo de aplicaciones móviles requiere paciencia, dedicación y una constante adaptación a los nuevos sistemas operativos.",
            "La respiración diafragmática es fundamental no solo para cantar o actuar, sino para mantener una fonación relajada y libre de tensión.",
            "La inteligencia artificial ofrece nuevas herramientas para el aprendizaje, pero la práctica humana constante es lo que realmente consolida el conocimiento.",
            "Preparar una taza de café por la mañana es un ritual que muchas personas disfrutan para iniciar el día con energía y buena disposición.",
            "Los océanos cubren la mayor parte de nuestro planeta y esconden ecosistemas que la ciencia aún está intentando comprender en su totalidad.",
            "Mantener una postura correcta al sentarse frente a la computadora ayuda a prevenir dolores de espalda y mejora significativamente la circulación sanguínea.",
            "El arte de la fotografía consiste en capturar la luz de tal forma que una imagen pueda contar una historia sin necesidad de usar palabras."
        )

        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.LECTURA_GUIADA,
            titulo = "Lectura Fluida Adaptativa",
            bpmSugerido = 0,
            textoPractica = parrafos.random(),
            instruccionEspecial = if (fluidez > 85) "Reto: Lee de corrido sin detenerte manteniendo un flujo de aire constante." else "Apoyo: Toma una pausa consciente y respira en cada signo de puntuación."
        )
    }

    private fun generarHablaEstirada(tonicos: Double): EjercicioGenerado {
        val palabrasObjetivo = listOf(
            "Sssservidor", "Ssssistema", "Fffferrocarril", "Ffffiesta", "Ssssoluciones",
            "Mmmmemoria", "Nnnnúcleo", "Vvvvolumen", "Mmmmanzana", "Nnnnaturaleza",
            "Jjjjirafa", "Ssssilencio", "Mmmmariposa", "Ffffelicidad", "Llllibertad"
        )
        val palabra = palabrasObjetivo.random()
        val frases = listOf(
            "El $palabra es fundamental para el proyecto de hoy.",
            "Ayer por la tarde descubrimos un $palabra nuevo.",
            "Tengo que revisar detalladamente el $palabra mañana.",
            "Increíblemente, el $palabra funciona de maravilla.",
            "Me gustaría aprender más sobre el $palabra."
        )

        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.HABLA_ESTIRADA,
            titulo = "Control de Inicios Suaves",
            bpmSugerido = 0,
            textoPractica = frases.random(),
            instruccionEspecial = "Alarga el primer sonido de la palabra principal durante ${if (tonicos > 3) 4 else 2} segundos. Inicia con muy bajo volumen (contacto ligero)."
        )
    }

    private fun generarSimulacion(latencia: Double): EjercicioGenerado {
        val escenarios = listOf(
            Triple("Recepción de Hotel", "Bienvenido, ¿tiene alguna reservación?", "Sí, tengo una reserva a nombre de Humberto Acosta."),
            Triple("Taquilla de Autobuses", "Buenas tardes, ¿cuál es su destino?", "Buenas tardes, quiero un boleto para Xalapa."),
            Triple("Cafetería Local", "Hola, buenos días. ¿Qué le preparo el día de hoy?", "Hola, me da un café americano grande, por favor."),
            Triple("Tienda de Cómputo", "Hola, ¿busca algún equipo o componente en especial?", "Sí, busco un adaptador de red y memoria RAM."),
            Triple("Entrevista de Trabajo", "Hábleme de su experiencia con infraestructura de redes.", "Tengo experiencia configurando servidores y contenedores Docker."),
            Triple("Consulta Médica", "Buenos días, ¿cuáles son sus síntomas principales?", "Tengo dolor de cabeza y fiebre desde la noche de ayer."),
            Triple("Restaurante", "¿Están listos para ordenar o necesitan unos minutos?", "Sí, estamos listos. Quisiera la pasta con salsa de tomate."),
            Triple("Farmacia", "Buenas tardes, ¿en qué le puedo servir el día de hoy?", "Buenas tardes, busco una caja de analgésicos por favor."),
            Triple("Sucursal Bancaria", "Bienvenido al banco, ¿qué trámite viene a realizar?", "Buenos días, quiero abrir una cuenta de ahorros."),
            Triple("Taquilla de Cine", "Hola, ¿para qué película busca boletos?", "Dos boletos para la función de las ocho, por favor."),
            Triple("Soporte Técnico", "Soporte técnico, ¿en qué le puedo ayudar hoy?", "Hola, mi conexión a internet ha estado fallando desde la mañana.")
        )
        val escenario = escenarios.random()

        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.SIMULACION,
            titulo = "Simulación: ${escenario.first}",
            bpmSugerido = 0,
            textoPractica = escenario.third,
            instruccionEspecial = if (latencia > 2000) "Respira profundo (diafragma) antes de tocar el micrófono. No hay prisa para contestar, tómate tu tiempo." else "Escucha al interlocutor y responde con naturalidad.",
            audioInterlocutor = escenario.second
        )
    }

    private fun generarMetronomoPuro(clonicosRecientes: Double): EjercicioGenerado {
        val combinaciones = listOf(
            "Ma - me - mi - mo - mu",
            "Pa - pe - pi - po - pu",
            "La - le - li - lo - lu",
            "Ta - te - ti - to - tu",
            "Sa - se - si - so - su",
            "Ra - re - ri - ro - ru",
            "Fa - fe - fi - fo - fu",
            "Na - ne - ni - no - nu",
            "Pra - pre - pri - pro - pru",
            "Bla - ble - bli - blo - blu",
            "Cra - cre - cri - cro - cru",
            "Pla - ple - pli - plo - plu"
        )
        val bpm = if (clonicosRecientes > 5) 45 else 65
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.METRONOMO_PURO,
            titulo = "Control de Impulsividad (Metrónomo Puro)",
            bpmSugerido = bpm,
            textoPractica = combinaciones.random(),
            instruccionEspecial = "Eliminaremos el significado semántico para enfocarnos SOLO en el ritmo motor. Sincroniza cada sílaba exactamente con el pulso a $bpm BPM."
        )
    }

    private fun generarBiofeedbackRelajacion(tonicosRecientes: Double): EjercicioGenerado {
        val fonaciones = listOf(
            "Aaaaaaaaaaaaaaaaaah",
            "Eeeeeeeeeeeeeeeeeeh",
            "Iiiiiiiiiiiiiiiiiih",
            "Ooooooooooooooooooh",
            "Uuuuuuuuuuuuuuuuuuh",
            "Mmmmmmmmmmmmmmmmmmm",
            "Sssssssssssssssssss",
            "Fffffffffffffffffff",
            "Zzzzzzzzzzzzzzzzzzz"
        )

        val exigencia = if (tonicosRecientes > 6) {
            "muy suavemente, casi susurrando"
        } else {
            "con un volumen normal y constante"
        }

        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.BIOFEEDBACK_RELAJACION,
            titulo = "Biofeedback de Fonación Continua",
            bpmSugerido = 0,
            textoPractica = fonaciones.random(),
            instruccionEspecial = "Toma aire profundo. Activa el DSP y emite este sonido $exigencia. Mantén la barra del Biofeedback estable en la zona verde, evitando picos rojos."
        )
    }

    private fun generarEvocacionSemantica(): EjercicioGenerado {
        val categorias = mapOf(
            "ANIMALES" to "Perro, Gato, León, Tigre, Caballo, Elefante, Jirafa, Oso, Lobo, Delfín",
            "FRUTAS" to "Manzana, Plátano, Naranja, Uva, Pera, Sandía, Fresa, Mango, Kiwi, Cereza",
            "PAÍSES" to "México, España, Colombia, Argentina, Chile, Francia, Japón, Canadá, Brasil",
            "COLORES" to "Rojo, Azul, Verde, Amarillo, Blanco, Negro, Morado, Naranja, Gris, Turquesa",
            "PROFESIONES" to "Doctor, Ingeniero, Abogado, Profesor, Arquitecto, Enfermera, Policía, Bombero, Chef",
            "DEPORTES" to "Fútbol, Básquetbol, Béisbol, Tenis, Natación, Atletismo, Boxeo, Golf, Ciclismo",
            "VERDURAS" to "Tomate, Cebolla, Ajo, Zanahoria, Papa, Lechuga, Brócoli, Espinaca, Pepino",
            "HERRAMIENTAS" to "Martillo, Desarmador, Pinzas, Llave, Taladro, Sierra, Clavo, Tornillo",
            "VEHÍCULOS" to "Automóvil, Autobús, Tren, Avión, Motocicleta, Bicicleta, Barco, Helicóptero",
            "INSTRUMENTOS" to "Guitarra, Piano, Violín, Batería, Flauta, Trompeta, Saxofón, Bajo",
            "ROPA" to "Camisa, Pantalón, Zapato, Chamarra, Sombrero, Calcetín, Vestido, Bufanda, Suéter",
            "MUEBLES" to "Silla, Mesa, Sillón, Sofá, Cama, Ropero, Escritorio, Estante, Espejo"
        )
        val categoriaAleatoria = categorias.entries.random()

        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.EVOCACION_SEMANTICA,
            titulo = "Evocación Bajo Presión: ${categoriaAleatoria.key}",
            bpmSugerido = 0,
            textoPractica = categoriaAleatoria.value.replace(", ", " / "),
            instruccionEspecial = "Reto de agilidad léxica. Nombra estos elementos de la categoría [${categoriaAleatoria.key}] de forma fluida. Intenta no hacer pausas tensas entre cada palabra."
        )
    }

    private fun generarPronunciacionCompleja(): EjercicioGenerado {
        val retosComplejos = listOf(
            "Desoxirribonucleico / Electroencefalograma",
            "Paralelepípedo / Otorrinolaringología",
            "Inconstitucionalidad / Esternocleidomastoideo",
            "Telecomunicaciones / Psicopedagogía / Biodiversidad",
            "Idiosincrasia / Institucionalización / Desoxirribonucleótido",
            "El cielo está encapotado, ¿quién lo desencapotará?",
            "Tres tristes tigres tragaban trigo en un trigal.",
            "Cuando cuentes cuentos, cuenta cuántos cuentos cuentas.",
            "Pablito clavó un clavito en la calva de un calvito.",
            "El perro de San Roque no tiene rabo porque Ramón Ramírez se lo ha cortado.",
            "Compadre, cómpreme un coco. Compadre, coco no compro."
        )
        return EjercicioGenerado(
            categoria = CategoriaAdaptativa.PRONUNCIACION_COMPLEJA,
            titulo = "Pronunciación de Alta Complejidad",
            bpmSugerido = 0,
            textoPractica = retosComplejos.random(),
            instruccionEspecial = "Tienes una fluidez excelente en tus últimas sesiones. Te reto a articular estas palabras largas o trabalenguas con total precisión y articulación marcada."
        )
    }
}