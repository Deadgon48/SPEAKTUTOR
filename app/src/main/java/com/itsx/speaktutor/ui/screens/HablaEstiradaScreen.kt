package com.itsx.speaktutor.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.itsx.speaktutor.ui.components.BarraNavegacionModulos
import com.itsx.speaktutor.ui.components.ControlVelocidadTts
import com.itsx.speaktutor.ui.navigation.Screen
import java.util.*

// Estructura para clasificar por modo de articulación
data class CategoriaArticulacion(
    val nombre: String,
    val letras: List<String>
)

enum class SeccionHabla {
    CATEGORIAS, LETRAS, PALABRAS
}

@Composable
fun HablaEstiradaScreen(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current

    var velocidadHabla by remember { mutableFloatStateOf(0.9f) }

    // Instancia de TextToSpeech para que la app lea las palabras
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES") // Configurar en Español
                ttsInitialized = true
            }
        }
        tts = textToSpeech

        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    // Definición de categorías según modo de articulación
    val categorias = listOf(
        CategoriaArticulacion("Oclusivas", listOf("B", "D", "G", "K", "P", "T")),
        CategoriaArticulacion("Nasales", listOf("M", "N", "Ñ")),
        CategoriaArticulacion("Semivocales", listOf("Y", "W")),
        CategoriaArticulacion("Vocales", listOf("A", "E", "I", "O", "U")),
        CategoriaArticulacion("Consonantes Restantes", listOf("C", "F", "H", "J", "L", "Q", "R", "S", "V", "X", "Z", "CH"))
    )

    var seccionActual by remember { mutableStateOf(SeccionHabla.CATEGORIAS) }
    var categoriaSeleccionada by remember { mutableStateOf<CategoriaArticulacion?>(null) }
    var letraSeleccionada by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Barra de navegación rápida superior entre módulos
        BarraNavegacionModulos(
            onNavigateTarjetas = { navController.navigate(Screen.TarjetasShader.route) },
            onNavigateMetronomo = { navController.navigate(Screen.Metronomo.route) },
            onNavigateHablaEstirada = { /* Ya estás aquí */ },
            onNavigateRitmoFluidez = { navController.navigate(Screen.RitmoFluidez.route) },
            onNavigateSimulacionSituaciones = { navController.navigate(Screen.SimulacionSituaciones.route) },
            onNavigatePronunciacionInstante = { navController.navigate(Screen.PronunciacionInstante.route) },
            onNavigateProgreso = { navController.navigate(Screen.Progreso.route) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Título del módulo
        Text(
            text = "Habla Estirada",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Practica la pronunciación por modo de articulación y escucha los ejemplos.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Componente de control de velocidad de TextToSpeech
        ControlVelocidadTts(
            velocidadActual = velocidadHabla,
            onVelocidadChange = { nuevaVelocidad ->
                velocidadHabla = nuevaVelocidad
                tts?.setSpeechRate(nuevaVelocidad)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (seccionActual) {
            // VISTA 3: Mostrar Lista de Palabras para la letra seleccionada (en Tarjetas Degradadas)
            SeccionHabla.PALABRAS -> {
                Button(
                    onClick = { seccionActual = SeccionHabla.LETRAS },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(text = "← Volver a Letras")
                }

                val colorPalabrasFondo = when (categoriaSeleccionada?.nombre) {
                    "Oclusivas" -> listOf(Color(0xFF4A148C), Color(0xFF8E24AA))
                    "Nasales" -> listOf(Color(0xFF006064), Color(0xFF00ACC1))
                    "Semivocales" -> listOf(Color(0xFF1B5E20), Color(0xFF43A047))
                    "Vocales" -> listOf(Color(0xFFE65100), Color(0xFFF57C00))
                    else -> listOf(Color(0xFF37474F), Color(0xFF546E7A))
                }

                Text(
                    text = "Letra: $letraSeleccionada",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorPalabrasFondo[1]
                )
                Spacer(modifier = Modifier.height(12.dp))

                val palabras = obtenerPalabrasPorLetra(letraSeleccionada ?: "")

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(palabras) { palabra ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(colorPalabrasFondo))
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = palabra,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Button(
                                    onClick = {
                                        if (ttsInitialized) {
                                            tts?.speak(palabra, TextToSpeech.QUEUE_FLUSH, null, null)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                                ) {
                                    Text(text = "🔊 Escuchar", fontSize = 14.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // VISTA 2: Mostrar Letras de la categoría seleccionada (en Tarjetas Degradadas Grid)
            SeccionHabla.LETRAS -> {
                Button(
                    onClick = { seccionActual = SeccionHabla.CATEGORIAS },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(text = "← Volver a Categorías")
                }

                val colorLetraFondoGrid = when (categoriaSeleccionada?.nombre) {
                    "Oclusivas" -> listOf(Color(0xFF4A148C), Color(0xFF8E24AA))
                    "Nasales" -> listOf(Color(0xFF006064), Color(0xFF00ACC1))
                    "Semivocales" -> listOf(Color(0xFF1B5E20), Color(0xFF43A047))
                    "Vocales" -> listOf(Color(0xFFE65100), Color(0xFFF57C00))
                    else -> listOf(Color(0xFF37474F), Color(0xFF546E7A))
                }

                Text(
                    text = categoriaSeleccionada?.nombre ?: "",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorLetraFondoGrid[1]
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categoriaSeleccionada?.letras ?: emptyList()) { letra ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(colorLetraFondoGrid))
                                .clickable {
                                    letraSeleccionada = letra
                                    seccionActual = SeccionHabla.PALABRAS
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letra,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // VISTA 1: Mostrar Categorías de Articulación iniciales (en Tarjetas Degradadas)
            SeccionHabla.CATEGORIAS -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categorias) { cat ->
                        val colorLetraFondo = when (cat.nombre) {
                            "Oclusivas" -> listOf(Color(0xFF4A148C), Color(0xFF8E24AA))
                            "Nasales" -> listOf(Color(0xFF006064), Color(0xFF00ACC1))
                            "Semivocales" -> listOf(Color(0xFF1B5E20), Color(0xFF43A047))
                            "Vocales" -> listOf(Color(0xFFE65100), Color(0xFFF57C00))
                            else -> listOf(Color(0xFF37474F), Color(0xFF546E7A))
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = cat.nombre,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorLetraFondo[1]
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(colorLetraFondo))
                                    .clickable {
                                        categoriaSeleccionada = cat
                                        seccionActual = SeccionHabla.LETRAS
                                    }
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = cat.nombre,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${cat.letras.size} fonemas",
                                            fontSize = 13.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(text = "Regresar al Menú Principal", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

// Banco de palabras de ejemplo clasificadas por letra para la práctica
fun obtenerPalabrasPorLetra(letra: String): List<String> {
    return when (letra) {
        "A" -> listOf("Abanico", "Acupuntura", "Ajo", "Abeto", "Adaptación", "Albañil", "Abismo", "Adelanto", "Albóndiga", "Abuelo", "Adición", "Alcachofa", "Academia", "Adolescente", "Alcohol", "Aceptar", "Advertencia", "Alcoba", "Aceituna", "Aficionado", "Alfabeto", "Acero", "África", "Algodón", "Acuario", "Agente", "Alienígena", "Acordeón", "Agricultura", "Alimentación", "Acróbata", "Ahogar", "Alivio", "Actividad", "Ajedrez", "Almendra", "Almohada", "Anís", "Armario", "Alondra", "Anochecer", "Aroma", "Alpaca", "Ansiedad", "Arruga", "Alteración", "Antena", "Arte", "Altura", "Antídoto", "Artículo", "Alucinación", "Anual", "Ascensor", "Amabilidad", "Anzuelo", "Asepsia", "Amalgama", "Apagón", "Asfalto", "Amante", "Apetito", "Asilo", "Ambulancia", "Apicultura", "Asistencia", "Amigo", "Aplauso", "Aspiradora", "Amuleto", "Aplicación", "Asunto", "Análisis", "Apocalipsis", "Atajo", "Anatomía", "Apuesta", "Atardecer", "Anciano", "Árbol", "Atento", "Andar", "Arco", "Atmósfera", "Andén", "Arena", "Átomo", "Anécdota", "Argumento", "Atractivo", "Anillo", "Aristocracia", "Atuendo", "Audaz", "Audiencia", "Aula", "Aumentar", "Aventura", "Ave")
        "B" -> listOf("Babero", "Balcón", "Barcaza", "Babilonia", "Baldosa", "Barítono", "Babor", "Balsa", "Barómetro", "Bache", "Bambú", "Barranco", "Bacteria", "Bandera", "Barrera", "Bahía", "Bandido", "Barril", "Bailar", "Banda", "Barrio", "Bailarina", "Bañador", "Barro", "Baile", "Baño", "Barrita", "Bajada", "Barbacoa", "Basalto", "Bajista", "Bárbaro", "Bazar", "Bajo", "Barbero", "Báscula", "Bastón", "Bienestar", "Boceto", "Bastidor", "Bigote", "Bochorno", "Batalla", "Billete", "Boda", "Batería", "Billetera", "Bodega", "Batiendo", "Biólogo", "Bogavante", "Baúl", "Biografía", "Boina", "Bebé", "Biología", "Bola", "Bebida", "Biomasa", "Boleta", "Belga", "Bipolar", "Boliche", "Belén", "Bisagra", "Bolillo", "Belleza", "Bisabuelo", "Bolivariano", "Bencina", "Bistec", "Bolígrafo", "Beneficio", "Bizcocho", "Bolsillo", "Bendición", "Blanquear", "Bolsa", "Bengala", "Blanco", "Bombardeo", "Beso", "Blusa", "Bondad", "Biblia", "Bobina", "Bonito", "Biblioteca", "Boca", "Bonificación", "Bicentenario", "Bocadillo", "Boquete", "Bosque", "Bostezar", "Bote", "Botella", "Botiquín", "Boya", "Brújula")
        "C" -> listOf("Cable", "Camión", "Caravana", "Cabra", "Campo", "Carbón", "Cactus", "Canasta", "Cárcel", "Café", "Cangrejo", "Cardumen", "Cajón", "Cantar", "Careta", "Calabaza", "Cántaro", "Caricia", "Calamar", "Cantidad", "Caridad", "Calor", "Caparazón", "Carne", "Calzado", "Capilla", "Carnaval", "Cama", "Capitán", "Carpintero", "Cámara", "Capítulo", "Carroza", "Caminata", "Caracol", "Carta", "Cartera", "Cereza", "Cinturón", "Casaquilla", "Cerro", "Ciruela", "Casco", "Certamen", "Cisne", "Casita", "Certeza", "Ciudad", "Castaña", "Cerveza", "Civilización", "Castillo", "Cesárea", "Clamor", "Catálogo", "Cesta", "Clase", "Catedral", "Cicatriz", "Clavel", "Caverna", "Ciclo", "Clavo", "Cebolla", "Ciego", "Cliente", "Cebra", "Cielo", "Clima", "Ceguedad", "Ciencia", "Cocción", "Celda", "Ciempiés", "Cocinero", "Célula", "Cigarra", "Código", "Cementerio", "Cigüeña", "Cogollo", "Ceniza", "Cima", "Colaboración", "Cepillo", "Cimiento", "Colcha", "Cerámica", "Cinco", "Colibrí", "Cercanía", "Cine", "Collar", "Colmena", "Colonia")
        "D" -> listOf("Dado", "Dedo", "Derecha", "Daga", "Defensa", "Derrota", "Dalmatense", "Deidad", "Desafío", "Dama", "Deja", "Desayuno", "Danza", "Dejo", "Descanso", "Daño", "Delantal", "Descenso", "Datos", "Delirio", "Desierto", "Debate", "Demanda", "Desliz", "Decisión", "Democracia", "Destino", "Declaración", "Denuncia", "Desvelo", "Decoración", "Dentista", "Detección", "Dedal", "Depósito", "Detergente", "Detonación", "Disciplina", "Donde", "Devoción", "Disco", "Donut", "Devorador", "Disfraz", "Dormitorio", "Diablo", "Diseño", "Dosel", "Diagnóstico", "Disfrute", "Dragón", "Diamante", "Disolución", "Drama", "Diapasón", "Distancia", "Drenaje", "Dibujo", "Distinción", "Ducha", "Dictado", "Diversión", "Duelo", "Diccionario", "División", "Dueño", "Dicha", "Doble", "Dulce", "Diez", "Doce", "Duración", "Dificultad", "Docencia", "Dureza", "Difusión", "Documento", "Duvet", "Digestión", "Dogma", "Dinastía", "Dilema", "Dolencia", "Dictadura", "Dimensión", "Dominio", "Desdén", "Dinosaurio", "Domingo", "Dote", "Dirección", "Donación", "Distribuidor", "Dictamen", "Dinamismo", "Duende")
        "E" -> listOf("Ebanista", "Efeméride", "Elegancia", "Ébano", "Efecto", "Elemento", "Ebriedad", "Eficiencia", "Elipse", "Ebullición", "Efímero", "Elixir", "Eco", "Egipto", "Ello", "Ecológico", "Egoísmo", "Elocuencia", "Economía", "Eje", "Eludir", "Ecosistema", "Ejemplo", "Embajador", "Edén", "Ejercicio", "Embarcación", "Edificio", "Ejército", "Emblema", "Edición", "Elástico", "Emergencia", "Educación", "Electricidad", "Emigración", "Emoción", "Entendimient", "Escalera", "Emolumento", "o", "Escapada", "Empatía", "Entereza", "Escarlata", "Empleo", "Entorno", "Esclerosis", "Empresa", "Entrada", "Escondite", "Empuje", "Entusiasmo", "Escopeta", "Enano", "Entidad", "Escritura", "Encanto", "Envidia", "Escuela", "Encomienda", "Epidemia", "Esencia", "Encuentro", "Epílogo", "Esfinge", "Enciclopedia", "Epístola", "Esfuerzo", "Endemia", "Equilibrio", "Esgrima", "Energía", "Equipo", "Esmerado", "Enigma", "Equitación", "Esotérico", "Enlace", "Equivalente", "Esperanza", "Enojo", "Eranio", "Espía", "Enredadera", "Erizo", "Espina", "Ensalada", "Erudición", "Espuma", "Escala", "Esqueleto", "Estadio", "Estalactita", "Estilo", "Estímulo", "Estrella", "Estrategia", "Estructura")
        "F" -> listOf("Fabricación", "Farsa", "Fermentación", "Fábula", "Fascinación", "Fértil", "Faceta", "Fascismo", "Fervor", "Facilidad", "Fase", "Festival", "Factor", "Fatiga", "Feudalismo", "Factoría", "Fauna", "Fibra", "Fallo", "Favor", "Ficción", "Familiar", "Febrero", "Fidelidad", "Fango", "Fechoría", "Fideo", "Fantasma", "Felino", "Fiesta", "Faraón", "Femenino", "Figura", "Farmacia", "Feria", "Fijación", "Filamento", "Fluctuación", "Fragor", "Filo", "Flujo", "Frambuesa", "Filósofo", "Fluvial", "Franqueza", "Filtro", "Fobia", "Franja", "Final", "Fogata", "Frasco", "Finca", "Follaje", "Fraternidad", "Finura", "Fondo", "Fraude", "Fisiología", "Fontanero", "Fresa", "Fisura", "Forastero", "Frescura", "Flacidez", "Forja", "Fricción", "Flagelo", "Forma", "Frijol", "Flama", "Formalidad", "Frío", "Flan", "Formulario", "Frontera", "Flauta", "Foro", "Fruta", "Flecha", "Fortaleza", "Fuego", "Flexibilidad", "Fortuna", "Fuente", "Flor", "Fotón", "Fuerza", "Florecimiento", "Fragancia", "Función", "Flotador", "Fragmento", "Fundición", "Funeral", "Furgoneta")
        "G" -> listOf("Gabardina", "Gamba", "Gárgola", "Gabinete", "Ganado", "Garrote", "Gacela", "Ganancia", "Gasolina", "Gaita", "Gancho", "Gastronomía", "Gala", "Ganga", "Gato", "Galaxia", "Ganso", "Gaviota", "Galeón", "Garaje", "Geisha", "Galimatías", "Garantía", "Gelatina", "Gallego", "Garceta", "Gemelos", "Gallina", "Garduña", "Gemido", "Gallinero", "Garfio", "Gen", "Gallo", "Garganta", "Genealogía", "Generación", "Glándula", "Grado", "General", "Globo", "Granja", "Generosidad", "Gloria", "Granizo", "Genética", "Glosario", "Granola", "Genio", "Glotonería", "Granulado", "Gente", "Gobernador", "Grasa", "Geranio", "Gobierno", "Gratitud", "Gerente", "Gol", "Gravedad", "Germen", "Golondrina", "Gravilla", "Gestación", "Golpe", "Grecia", "Gesto", "Goma", "Greñudo", "Gigantismo", "Gorila", "Grieta", "Gimnasia", "Gorra", "Grifo", "Gimnasta", "Gorrión", "Grillo", "Girasol", "Gota", "Gris", "Giro", "Gotera", "Grúa", "Gitano", "Grabación", "Grupo", "Glaciar", "Grabado", "Guacamayo", "Gladiador", "Gracia", "Guante", "Guarida", "Guerrero", "Guitarra", "Gusano")
        "H" -> listOf("Habitar", "Hechizo", "Héroe", "Habitación", "Hedor", "Heterogéneo", "Hacha", "Hegemonía", "Huevo", "Hado", "Hembra", "Hijo", "Halagar", "Hemiciclo", "Hilar", "Halcón", "Hemorragia", "Himno", "Hambre", "Hena", "Hipnosis", "Hámster", "Herida", "Hipócrita", "Hondo", "Herbívoro", "Histeria", "Harto", "Heredar", "Hogar", "Hazaña", "Hermano", "Hoja", "Hecho", "Hermoso", "Holístico", "Hombre", "Hongo", "Honor", "Horrible", "Hormiga", "Horno", "Hospital", "Hostil", "Humedad", "Humor", "Hundir", "Huracán", "Huraño", "Hurgar", "Huso", "Hiena", "Hígado")
        "I" -> listOf("Icono", "Imaginación", "Indicación", "Idea", "Imán", "Índice", "Identidad", "Imitar", "Individuo", "Idioma", "Impacto", "Influencia", "Idilio", "Imperio", "Información", "Idoneidad", "Impulso", "Infraganti", "Iglesia", "Incendio", "Infracción", "Ignición", "Incienso", "Ingeniero", "Ignorancia", "Inclusión", "Ingenio", "Ilusión", "Incompetencia", "Ingreso", "Indecisión", "Ilustración", "Inhalación", "Independencia", "Imagen", "Iniciativa", "Inyección", "Interacción", "Invierno", "Injusticia", "Intercambio", "Innovación", "Interés", "Ion", "Inoculación", "Interior", "Iridio", "Inodoro", "Intermedio", "Ironía", "Inscripción", "Internado", "Irrigación", "Inseguridad", "Internet", "Irritación", "Inserción", "Interrupción", "Isótopo", "Insignia", "Intervalo", "Itinerario", "Inspiración", "Intervención", "Ítaca", "Instante", "Intestino", "Ivori", "Instinto", "Intimidad", "Izquierda", "Institución", "Intolerancia", "Instructor", "Intriga", "Instrumento", "Introducción", "Insulto", "Intuición", "Integración", "Inundación", "Intención", "Invitación", "Intensidad", "Inversión")
        "J" -> listOf("Jabón", "Jesuita", "Jubileo", "Jacinto", "Jet", "Jubilación", "Jacuzzi", "Jirón", "Juguete", "Jade", "Joaquín", "Juguetería", "Jalea", "Joroba", "Juicio", "Jamba", "Jornada", "Juicioso", "Jardín", "Jornal", "Jugo", "Jarra", "Joropo", "Juguetón", "Jarabe", "José", "Julio", "Jarrón", "Jota", "Jumenta", "Jazmín", "Joven", "Juntar", "Jefe", "Joyero", "Junto", "Júpiter", "Jurado", "Jurisdicción", "Juramento", "Justicia", "Justo", "Juventud", "Juzgado")
        "K" -> listOf("Kamikaze", "Kilohercio", "Kanji", "Kilovatio", "Kárate", "Kilimanjaro", "Katana", "Kilt", "Kilocaloría", "Kioto", "Kilómetro", "Kleenex", "Kimono", "Knockout", "Kiosco", "Koan", "Kit", "Koala", "Kryptonita", "Kuwait")
        "L" -> listOf("Laberinto", "Legión", "Límite", "Laca", "Legumbre", "Lince", "Ladrillo", "Lejía", "Linda", "Lago", "Lente", "Lingüista", "Lana", "Leopardo", "Lirio", "Lancha", "Letargo", "Lista", "Lápiz", "Levadura", "Litera", "Laringe", "Liana", "Literatura", "Lasagna", "Libertad", "Litio", "Lazo", "Libro", "Liturgia", "Leal", "Licencia", "Lluvia", "Legado", "Liderazgo", "Loa", "Loción", "Loco", "Lógica", "Lomo", "Lombriz", "Lonja", "Lote", "Lucha", "Lucidez", "Lucero", "Luciérnaga", "Lujo", "Luminosidad", "Lumbre", "Lunares", "Luto")
        "M" -> listOf("Madre", "Manso", "Masaje", "Madrugada", "Mantel", "Máscara", "Maíz", "Manual", "Mástil", "Maleta", "Manzana", "Mata", "Malicia", "Mapa", "Material", "Malva", "Máquina", "Matiz", "Mamut", "Maravilla", "Matriarca", "Manada", "Margarita", "Máximo", "Mandato", "Marinero", "Mechón", "Mandíbula", "Mariposa", "Medalla", "Manía", "Mármol", "Médico", "Manjar", "Martillo", "Mejilla", "Melancolía", "Molécula", "Melón", "Molino", "Membrana", "Momento", "Memoria", "Monasterio", "Mente", "Moneda", "Menú", "Montaña", "Mercado", "Monte", "Merluza", "Motor", "Meseta", "Movimiento", "Metal", "Mural", "Metro", "Música", "Microbio", "Músculo", "Micrófono", "Milagro", "Mina", "Mirador", "Mirar", "Misterio", "Modelo")
        "N" -> listOf("Nacer", "Nina", "Nudillo", "Nación", "Ninguno", "Numeral", "Nadie", "Ninfa", "Número", "Nariz", "Nobel", "Nutrición", "Nave", "Noche", "Nube", "Navegación", "Nómada", "Núcleo", "Nectar", "Nombrar", "Negocio", "Norte", "Nudismo", "Nervioso", "Noticia", "Nudosa", "Nicho", "Nuez", "Nerviosismo", "Nieve", "Nublar", "Neurona", "Nimbo", "Negro", "Normal", "Norteña", "Nostalgia", "Norteamerica", "No", "Nacionalismo", "Navegante", "Naranjo", "Neutral", "Niñez", "Nudoso", "Navegar", "Nocturno", "Navegador", "Nacionalidad")
        "Ñ" -> listOf("Ñandú", "Ñapa", "Ñato", "Ñoño", "Ñuño", "Ñecora", "Ñique", "Ñorco", "Ñigra", "Ñiquiñaque")
        "O" -> listOf("Obelisco", "Ola", "Orquídea", "Obispo", "Olivo", "Oruga", "Objeto", "Olor", "Oscuro", "Obligación", "Ombra", "Osito", "Obra", "Onda", "Oso", "Obrero", "Oportuna", "Ostra", "Obviedad", "Opresión", "Otoño", "Ocaso", "Óptica", "Otorgar", "Ocho", "Óptimo", "Ovación", "Ocurre", "Origen", "Ovelia", "Odisea", "Orificio", "Oveja", "Ojo", "Orquesta", "Oxígeno", "Óxido", "Oír", "Órgano", "Oportunidad", "Oprobio", "Ornamento", "Ofensa", "Oficina", "Ofrecimiento", "Obedecer", "Operar", "Optimismo", "Orbe")
        "P" -> listOf("Pacto", "Partido", "Peligro", "Padrino", "Pasarela", "Película", "Palabra", "Paseo", "Peluca", "Paleontología", "Pelea", "Palestra", "Pasta", "Perdón", "Palillo", "Patente", "Perdiz", "Papel", "Periódico", "Pared", "Pato", "Permiso", "Paradoja", "Perro", "Paraguas", "Pecado", "Persiana", "Parlamento", "Pecera", "Pescado", "Parque", "Pedazo", "Pescar", "Pesimismo", "Petición", "Piedra", "Piedrita", "Pijo", "Pizarra", "Planta", "Plaza", "Plenitud", "Plenilunio", "Playa", "Plan")
        "Q" -> listOf("Quesera", "Quería", "Queloide", "Químico", "Quásar", "Quebradizo", "Quinto", "Quimérico", "Quirúrgico", "Quirófano", "Quemar", "Quisiera", "Querido", "Quijote", "Quedar", "Quiniela", "Querencia", "Quid", "Querubín", "Quieto", "Quema", "Quimera", "Quizás", "Quotum", "Quedo", "Quiebra")
        "R" -> listOf("Ración", "Relación", "Rizo", "Rastro", "Relieve", "Rocas", "Rama", "Reloj", "Romántico", "Rebaño", "Remolino", "Ropa", "Rebosar", "Renovar", "Rosa", "Receta", "Repisa", "Rueda", "Reclamo", "Reptil", "Rufián", "Red", "Resaca", "Rugir", "Reflejo", "Reserva", "Rima", "Regalo", "Respirar", "Rincón", "Reina", "Restaurante", "Risa", "Reír", "Río", "Ritmo", "Rival", "Rizado", "Rodillo", "Rodear", "Rumbo", "Rugido", "Riel", "Rehenes", "Reino", "Rápido", "Renta", "Rebote", "Recreo")
        "S" -> listOf("Sabor", "Sombrero", "Sopa", "Sacar", "Sombra", "Sustituir", "Salón", "Siguiente", "Sur", "Salvación", "Soplar", "Suerte", "Silla", "Sordo", "Seguridad", "Simplicidad", "Soleado", "Sierra", "Sangre", "Sello", "Silencio", "Sinfonía", "Sugerir", "Súbito", "Sincero", "Seco", "Saltar", "Solemnidad", "Salsa", "Sol", "Susto", "Silueta", "Salud", "Satisfacción", "Sueño", "Salvaje", "Secuela", "Serio", "Sanción", "Sísmico")
        "T" -> listOf("Táctil", "Terraza", "Tobillo", "Tacto", "Termómetro", "Toda", "Tarde", "Testimonio", "Tolerancia", "Tarea", "Teórico", "Tomate", "Té", "Teorías", "Torneo", "Teatro", "Tetraedro", "Tormenta", "Tecnología", "Tiburón", "Torrente", "Tela", "Tienda", "Tracción", "Temblor", "Tilde", "Traducción", "Temperatura", "Tinta", "Traer", "Temor", "Tocino", "Travesía", "Tener", "Toalla", "Trenza", "Tremendo", "Tristeza", "Triángulo", "Truco", "Triste", "Trabajo", "Tráfico", "Trampa", "Tremor", "Tránsito", "Trinchera", "Tronar", "Trozo", "Tronco", "Trilogía", "Tumba", "Túnel", "Turbina", "Túnica")
        "U" -> listOf("Ubiquitario", "Urgente", "Uveítis", "Ubicación", "Urológico", "Uva", "Uber", "Usabilidad", "Urgir", "Ulla", "Usar", "Unánime", "Ultimátum", "Usado", "Umpire", "Útil", "Usura", "Unir", "Última", "Uso", "Unión", "Ultraje", "Usos", "Unidad", "Ultra", "Usted", "Unilateral", "Ultimación", "Utilidad", "Uve", "Utilizar", "Urgencia", "Utopía", "Urbano", "Urinal", "Urge", "Urgen")
        "V" -> listOf("Valle", "Vestido", "Visita", "Valor", "Veterinario", "Visto", "Válido", "Vez", "Vivir", "Vapor", "Viento", "Volar", "Variedad", "Vigente", "Voluntad", "Vaso", "Vigilancia", "Volumen", "Venganza", "Vila", "Voto", "Ventana", "Vilo", "Vulgar", "Veracidad", "Vinagre", "Vulcanización", "Verruga", "Vino", "Vulnerabilidad", "Vasija", "Versátil", "Violín", "Vestigio", "Versión", "Visibilidad", "Verificar", "Vicente", "Volver", "Viajero")
        "W" -> listOf("Waffle", "Wolverine", "Wildlife", "Wagner", "Wombat", "Windmill", "Waikiki", "Woodstock", "Wanderlust", "Walkie-talkie", "Workaholic", "Whistle", "Wallace", "World", "Wipe", "Waltz", "Worm", "Wisdom", "Wellington", "Wristband", "Whisky", "Witch", "Wonder", "Wigwam", "Wobble", "Workout", "Wilco", "Warrant", "Wink", "Wilson", "Wasteland", "Whisk", "Wizard", "Workshop", "Waddle", "Webcam", "Whale", "Wealth", "Weld", "Whiz")
        "Y" -> listOf("Yacer", "Yegua", "Yihad", "Yodo", "Yunta", "Yerno", "Yuyu", "Youtuber", "Yugoslavia")
        "Z" -> listOf("Zafiro", "Zarpa", "Zócalo", "Zanco", "Zinc", "Zarandear", "Zarpar", "Zóonosis", "Zodiaco", "Zambullida", "Zorrito", "Zaragoza", "Zulema", "Zen", "Zarza", "Zafarse", "Zafra", "Zarzuela", "Zas", "Zóster", "Zancada", "Zumbón", "Zapatilla")
        "CH" -> listOf("Chivo", "Chocolate", "Chaleco", "Chorro", "Chicle")
        else -> listOf("Palabra 1", "Palabra 2", "Palabra 3")
    }
}