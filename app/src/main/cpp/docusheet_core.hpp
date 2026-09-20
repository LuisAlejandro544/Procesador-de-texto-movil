/**
 * ==============================================================================
 * DocuSheet - Núcleo Nativo en C++20 (docusheet_core.hpp)
 * ==============================================================================
 * 
 * Propósito del módulo:
 * Proporcionar la infraestructura de bajo nivel para el procesamiento tipográfico
 * y la medición milimétrica de caracteres sin interferencia del recolector de
 * basura (GC) de Android Runtime.
 * 
 * Características clave:
 * - Estándar C++20 (uso de std::span, concepts y tipos seguros).
 * - Diseñado para soportar interoperabilidad con Rust (CXX) y JNI con Kotlin.
 * - Preparado para arquitecturas de 32 y 64 bits (armeabi-v7a, arm64-v8a, x86, x86_64).
 */

#pragma once

#include <string>
#include <string_view>
#include <span>
#include <vector>
#include <cstdint>
#include <memory>

namespace docusheet::core {

/**
 * Concepto C++20 para delimitar tipos de medidas tipográficas seguras.
 */
template <typename T>
concept NumericMeasure = std::is_arithmetic_v<T>;

/**
 * Estructura para dimensiones métricas de página física (A4, Carta, Oficio).
 */
struct PageDimensions {
    float width_pt;     // Ancho en puntos tipográficos (1/72 pulgada)
    float height_pt;    // Alto en puntos tipográficos
    float margin_left;
    float margin_right;
    float margin_top;
    float margin_bottom;
};

/**
 * Métrica de salto de página y distribución de líneas.
 */
struct LineBreakResult {
    size_t char_index;
    float line_width;
    bool is_hard_break;
};

/**
 * Clase base del motor tipográfico y maquetación en C++20.
 */
class TypographyEngine {
public:
    TypographyEngine();
    ~TypographyEngine() = default;

    /**
     * Calcula la posición óptima de salto de línea según los márgenes de la hoja.
     */
    [[nodiscard]] std::vector<LineBreakResult> compute_line_breaks(
        std::string_view text,
        float max_width_pt
    );

    /**
     * Obtiene la versión del motor C++20 compilado.
     */
    [[nodiscard]] std::string get_engine_version() const;

private:
    float default_dpi_{72.0f};
};

} // namespace docusheet::core
