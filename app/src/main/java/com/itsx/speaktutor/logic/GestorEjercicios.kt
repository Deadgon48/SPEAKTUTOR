package com.itsx.speaktutor.logic

import android.content.Context
import org.json.JSONArray
import java.nio.charset.Charset

data class EjercicioRitmo(
    val texto: String,
    val titulo: String? = null // El título es opcional, usado principalmente en lecturas.
)

// NUEVA DATA CLASS PARA LLAMADAS
data class LlamadaTelefonica(
    val id: String,
    val contexto: String,
    val operadora: String,
    val respuestaSugerida: String
)

// NUEVA DATA CLASS PARA CONCIENCIA FONOLÓGICA (Incluyendo Nivel)
data class EjercicioConciencia(
    val palabraVisual: String,
    val fonemaFaltante: String,
    val palabraCompleta: String,
    val nivel: Int
)

object GestorEjercicios {

    // Funcion de ritmo y fluidez
    fun obtenerEjerciciosAleatorios(
        context: Context,
        nombreArchivo: String,
        cantidad: Int
    ): List<EjercicioRitmo> {
        val listaCompleta = mutableListOf<EjercicioRitmo>()

        try {
            val inputStream = context.assets.open(nombreArchivo)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()

            val jsonString = String(buffer, Charset.forName("UTF-8"))
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                listaCompleta.add(
                    EjercicioRitmo(
                        texto = jsonObject.getString("texto"),
                        titulo = jsonObject.optString("titulo", null)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Retorna lista vacía si hay un error (ej. archivo no encontrado o JSON malformado)
            return emptyList()
        }

        return listaCompleta.shuffled().take(cantidad)
    }


// Funciones de simulacion de situaciones, escenarios y guiones

    fun obtenerEscenarios(
        context: Context,
        nombreArchivo: String,
        cantidad: Int
    ): List<Triple<String, String, String>> {
        val lista = mutableListOf<Triple<String, String, String>>()
        try {
            val isStream = context.assets.open(nombreArchivo)
            val buffer = ByteArray(isStream.available())
            isStream.read(buffer)
            isStream.close()

            val jsonArray =
                org.json.JSONArray(String(buffer, java.nio.charset.Charset.forName("UTF-8")))
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                lista.add(
                    Triple(
                        obj.getString("titulo"),
                        obj.getString("audio"),
                        obj.getString("respuesta")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista.shuffled().take(cantidad)
    }

    fun obtenerGuiones(
        context: Context,
        nombreArchivo: String,
        cantidad: Int
    ): List<com.itsx.speaktutor.ui.screens.GuionPractica> {
        val lista = mutableListOf<com.itsx.speaktutor.ui.screens.GuionPractica>()
        try {
            val isStream = context.assets.open(nombreArchivo)
            val buffer = ByteArray(isStream.available())
            isStream.read(buffer)
            isStream.close()

            val jsonArray =
                org.json.JSONArray(String(buffer, java.nio.charset.Charset.forName("UTF-8")))
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val dialogosJson = obj.getJSONArray("dialogos")
                val dialogos = mutableListOf<com.itsx.speaktutor.ui.screens.TurnoGuion>()

                for (j in 0 until dialogosJson.length()) {
                    val turnoObj = dialogosJson.getJSONObject(j)
                    dialogos.add(
                        com.itsx.speaktutor.ui.screens.TurnoGuion(
                            rol = turnoObj.getString("rol"),
                            texto = turnoObj.getString("texto"),
                            esDeApp = turnoObj.getBoolean("esDeApp")
                        )
                    )
                }

                lista.add(
                    com.itsx.speaktutor.ui.screens.GuionPractica(
                        titulo = obj.getString("titulo"),
                        descripcion = obj.getString("descripcion"),
                        dialogos = dialogos
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista.shuffled().take(cantidad)
    }

    fun obtenerDiccionarioEvocacion(
        context: android.content.Context,
        nombreArchivo: String
    ): Map<String, List<String>> {
        val mapa = mutableMapOf<String, List<String>>()
        try {
            val isStream = context.assets.open(nombreArchivo)
            val buffer = ByteArray(isStream.available())
            isStream.read(buffer)
            isStream.close()

            val jsonObject =
                org.json.JSONObject(String(buffer, java.nio.charset.Charset.forName("UTF-8")))
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val jsonArray = jsonObject.getJSONArray(key)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    list.add(jsonArray.getString(i))
                }
                mapa[key] = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mapa
    }

    // NUEVA FUNCIÓN PARA LLAMADAS
    fun obtenerLlamadas(context: Context, nombreArchivo: String, cantidad: Int): List<LlamadaTelefonica> {
        val lista = mutableListOf<LlamadaTelefonica>()
        try {
            val isStream = context.assets.open(nombreArchivo)
            val buffer = ByteArray(isStream.available())
            isStream.read(buffer)
            isStream.close()
            val jsonArray = org.json.JSONArray(String(buffer, java.nio.charset.Charset.forName("UTF-8")))
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                lista.add(LlamadaTelefonica(obj.getString("id"), obj.getString("contexto"), obj.getString("operadora"), obj.getString("respuesta_sugerida")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista.shuffled().take(cantidad)
    }

    // Funcion de pronunciacion al instante
    fun obtenerListaPalabras(context: Context, nombreArchivo: String): List<String> {
        val lista = mutableListOf<String>()
        try {
            val isStream = context.assets.open(nombreArchivo)
            val buffer = ByteArray(isStream.available())
            isStream.read(buffer)
            isStream.close()

            val jsonArray = JSONArray(String(buffer, Charset.forName("UTF-8")))
            for (i in 0 until jsonArray.length()) {
                lista.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista
    }

    // Función para obtener el diccionario completo de Habla Estirada
    fun obtenerDiccionarioHablaEstirada(
        context: Context,
        nombreArchivo: String
    ): Map<String, List<String>> {
        val mapa = mutableMapOf<String, List<String>>()
        try {
            val isStream = context.assets.open(nombreArchivo)
            val buffer = ByteArray(isStream.available())
            isStream.read(buffer)
            isStream.close()

            val jsonObject = org.json.JSONObject(String(buffer, java.nio.charset.Charset.forName("UTF-8")))
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val jsonArray = jsonObject.getJSONArray(key)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    list.add(jsonArray.getString(i))
                }
                mapa[key] = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mapa
    }

    // NUEVA FUNCIÓN PARA CONCIENCIA FONOLÓGICA (Lee la variable 'nivel')
    fun obtenerEjerciciosConciencia(context: Context, nombreArchivo: String): List<EjercicioConciencia> {
        val lista = mutableListOf<EjercicioConciencia>()
        try {
            val inputStream = context.assets.open(nombreArchivo)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()

            val jsonArray = JSONArray(String(buffer, Charset.forName("UTF-8")))
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                lista.add(
                    EjercicioConciencia(
                        palabraVisual = obj.getString("palabraVisual"),
                        fonemaFaltante = obj.getString("fonemaFaltante"),
                        palabraCompleta = obj.getString("palabraCompleta"),
                        nivel = obj.getInt("nivel")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista.shuffled() // Revuelve los ejercicios automáticamente
    }
}
