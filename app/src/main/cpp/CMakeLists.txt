
cmake_minimum_required(VERSION 3.22.1)

project("speaktutor")

add_library(
        oboe_dsp
        SHARED
        native-lib.cpp
)

find_package(oboe REQUIRED CONFIG)

find_library(
        log-lib
        log
)

target_compile_features(
        oboe_dsp
        PRIVATE
        cxx_std_17
)

target_compile_options(
        oboe_dsp
        PRIVATE
        -Wall
        -Wextra
)

target_link_libraries(
        oboe_dsp
        oboe::oboe
        ${log-lib}
)
