/**
 * ==============================================================================
 * DocuSheet - Implementación del Núcleo Nativo en C++20 (docusheet_core.cpp)
 * ==============================================================================
 * 
 * Implementación de la lógica de medición de líneas e infraestructura JNI
 * para conectar el motor de alto rendimiento con la capa de Jetpack Compose.
 */

#include "docusheet_core.hpp"
#include <jni.h>
#include <android/log.h>
#include <sstream>

#define LOG_TAG "DocuSheetNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace docusheet::core {

TypographyEngine::TypographyEngine() {
    LOGI("DocuSheet C++20 TypographyEngine inicializado correctamente.");
}

std::vector<LineBreakResult> TypographyEngine::compute_line_breaks(
    std::string_view text,
    float max_width_pt
) {
    std::vector<LineBreakResult> breaks;
    if (text.empty() || max_width_pt <= 0.0f) {
        return breaks;
    }

    // Algoritmo preliminar de maquetación lineal
    float current_width = 0.0f;
    const float approx_char_width = 7.2f; // Aproximación base para tipografías proporcionales

    for (size_t i = 0; i < text.size(); ++i) {
        char c = text[i];
        if (c == '\n') {
            breaks.push_back({i, current_width, true});
            current_width = 0.0f;
            continue;
        }

        current_width += approx_char_width;
        if (current_width >= max_width_pt) {
            breaks.push_back({i, current_width, false});
            current_width = 0.0f;
        }
    }

    return breaks;
}

std::string TypographyEngine::get_engine_version() const {
    return "DocuSheet C++20 Typography Engine v1.0.0 (ISO C++20)";
}

} // namespace docusheet::core

// ==============================================================================
// Enlaces JNI para comunicación con Kotlin
// ==============================================================================
extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_util_NativeEngineBridge_getNativeEngineVersion(
    JNIEnv* env,
    jobject /* this */
) {
    docusheet::core::TypographyEngine engine;
    std::string version = engine.get_engine_version();
    return env->NewStringUTF(version.c_str());
}

}
