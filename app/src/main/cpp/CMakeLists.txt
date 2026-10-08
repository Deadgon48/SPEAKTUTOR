cmake_minimum_required(VERSION 3.18.1)
project("oboe_dsp")

# Buscar y cargar la librería Oboe desde Prefab
find_package(oboe REQUIRED CONFIG)

# Declarar nuestro archivo C++
add_library(oboe_dsp SHARED native-lib.cpp)

# Conectar nuestro código con Oboe y la librería de logs de Android
target_link_libraries(oboe_dsp oboe::oboe log)