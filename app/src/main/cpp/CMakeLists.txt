# ==============================================================================
# DocuSheet - CMakeLists.txt
# Configuración del motor nativo C++20 para procesamiento de texto e imprenta
# ==============================================================================

cmake_minimum_required(VERSION 3.22.1)

project("docusheet_core" LANGUAGES CXX C)

# Estándar C++20 moderno estricto
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Flags de optimización y diagnóstico
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -Wpedantic")

# Biblioteca compartida nativa de Rust si está disponible para la arquitectura
set(RUST_LIB_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libdocusheet_rust.so")

if(EXISTS "${RUST_LIB_PATH}")
    add_library(docusheet_rust SHARED IMPORTED)
    set_target_properties(docusheet_rust PROPERTIES
        IMPORTED_LOCATION "${RUST_LIB_PATH}"
    )
    message(STATUS "DocuSheet CMake: Enlazando con docusheet_rust para ABI ${ANDROID_ABI}")
endif()

# Definición de la biblioteca compartida nativa
add_library(
    docusheet_core
    SHARED
    docusheet_core.cpp
)

# Inclusión de directorios de cabeceras
target_include_directories(
    docusheet_core
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Enlace con la biblioteca de log del sistema Android
find_library(log-lib log)

if(TARGET docusheet_rust)
    target_link_libraries(
        docusheet_core
        PRIVATE
        ${log-lib}
        docusheet_rust
    )
else()
    target_link_libraries(
        docusheet_core
        PRIVATE
        ${log-lib}
    )
endif()
