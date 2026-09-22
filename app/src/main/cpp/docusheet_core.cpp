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

std::vector<float> TypographyEngine::compute_justified_spacing(
    std::string_view line_text,
    float target_width_pt
) {
    std::vector<float> spacing;
    if (line_text.empty() || target_width_pt <= 0.0f) {
        return spacing;
    }

    // Contar palabras e identificar posiciones de espacios
    size_t space_count = 0;
    float natural_width = 0.0f;
    const float base_char_width = 7.2f;

    for (char c : line_text) {
        if (c == ' ') {
            space_count++;
        }
        natural_width += base_char_width;
    }

    if (space_count == 0 || natural_width >= target_width_pt) {
        // No se puede o no se requiere justificar
        return spacing;
    }

    // Espacio adicional a repartir equitativamente entre los espacios existentes
    float extra_width = target_width_pt - natural_width;
    float extra_per_space = extra_width / static_cast<float>(space_count);

    spacing.reserve(space_count);
    for (size_t i = 0; i < space_count; ++i) {
        spacing.push_back(base_char_width + extra_per_space);
    }

    return spacing;
}

std::string TypographyEngine::get_engine_version() const {
    return "DocuSheet C++20 Typography Engine v1.3.0 (Justified Text, Tables & Native Rust Core)";
}

TableLayoutResult TypographyEngine::compute_table_layout(
    int num_columns,
    float printable_width_pt,
    std::span<const float> custom_weights
) {
    TableLayoutResult result{};
    if (num_columns <= 0 || printable_width_pt <= 0.0f) {
        return result;
    }

    result.total_table_width_pt = printable_width_pt;
    result.cell_padding_horizontal_pt = 6.0f; // 6 pt de margen interno de celda
    result.cell_padding_vertical_pt = 4.5f;   // 4.5 pt de espaciado vertical

    result.column_widths_pt.resize(num_columns);

    if (!custom_weights.empty() && custom_weights.size() == static_cast<size_t>(num_columns)) {
        float total_weight = 0.0f;
        for (float w : custom_weights) {
            total_weight += (w > 0.0f ? w : 1.0f);
        }
        if (total_weight > 0.0f) {
            for (int i = 0; i < num_columns; ++i) {
                float weight = custom_weights[i] > 0.0f ? custom_weights[i] : 1.0f;
                result.column_widths_pt[i] = (weight / total_weight) * printable_width_pt;
            }
            return result;
        }
    }

    // Distribución equitativa predeterminada
    float col_width = printable_width_pt / static_cast<float>(num_columns);
    for (int i = 0; i < num_columns; ++i) {
        result.column_widths_pt[i] = col_width;
    }

    return result;
}

// ==============================================================================
// Implementación de TextManipulator (Transposición e Intercambio de Texto en C++20)
// ==============================================================================

TextPermutationResult TextManipulator::swap_ranges(
    std::string_view full_text,
    size_t start_a,
    size_t end_a,
    size_t start_b,
    size_t end_b
) {
    TextPermutationResult res;
    size_t len = full_text.size();
    size_t a_s = std::min(start_a, end_a);
    size_t a_e = std::max(start_a, end_a);
    size_t b_s = std::min(start_b, end_b);
    size_t b_e = std::max(start_b, end_b);

    if (a_e > len || b_e > len) {
        res.success = false;
        res.new_text = std::string(full_text);
        res.message = "Índices de selección fuera de los límites del texto";
        return res;
    }

    if (a_s < b_e && a_e > b_s) {
        res.success = false;
        res.new_text = std::string(full_text);
        res.message = "Los bloques seleccionados no pueden solaparse";
        return res;
    }

    bool a_first = (a_s <= b_s);
    size_t first_s = a_first ? a_s : b_s;
    size_t first_e = a_first ? a_e : b_e;
    size_t second_s = a_first ? b_s : a_s;
    size_t second_e = a_first ? b_e : a_e;

    std::string out;
    out.reserve(len);
    out.append(full_text.substr(0, first_s));
    out.append(full_text.substr(second_s, second_e - second_s));
    out.append(full_text.substr(first_e, second_s - first_e));
    out.append(full_text.substr(first_s, first_e - first_s));
    out.append(full_text.substr(second_e));

    res.new_text = std::move(out);
    res.success = true;

    // Calcular la nueva posición del bloque A tras el intercambio
    if (a_first) {
        size_t b_len = second_e - second_s;
        size_t a_len = first_e - first_s;
        size_t mid_len = second_s - first_e;
        res.new_start = first_s + b_len + mid_len;
        res.new_end = res.new_start + a_len;
    } else {
        res.new_start = first_s;
        res.new_end = res.new_start + (a_e - a_s);
    }
    res.message = "Bloques de texto intercambiados exitosamente";
    return res;
}

TextPermutationResult TextManipulator::move_range(
    std::string_view full_text,
    size_t start,
    size_t end,
    size_t target_position
) {
    TextPermutationResult res;
    size_t len = full_text.size();
    size_t s = std::min(start, end);
    size_t e = std::max(start, end);

    if (e > len || target_position > len) {
        res.success = false;
        res.new_text = std::string(full_text);
        res.message = "Posición destino fuera de los límites del documento";
        return res;
    }

    if (s == e || (target_position >= s && target_position <= e)) {
        res.success = true;
        res.new_text = std::string(full_text);
        res.new_start = s;
        res.new_end = e;
        res.message = "La posición objetivo es idéntica al origen";
        return res;
    }

    std::string block(full_text.substr(s, e - s));
    std::string out;
    out.reserve(len);

    if (target_position < s) {
        out.append(full_text.substr(0, target_position));
        out.append(block);
        out.append(full_text.substr(target_position, s - target_position));
        out.append(full_text.substr(e));

        res.new_start = target_position;
        res.new_end = target_position + block.size();
    } else {
        out.append(full_text.substr(0, s));
        out.append(full_text.substr(e, target_position - e));
        out.append(block);
        out.append(full_text.substr(target_position));

        res.new_start = target_position - block.size();
        res.new_end = target_position;
    }

    res.new_text = std::move(out);
    res.success = true;
    res.message = "Texto reubicado exitosamente";
    return res;
}

TextPermutationResult TextManipulator::swap_paragraph(
    std::string_view full_text,
    size_t cursor_start,
    size_t cursor_end,
    bool swap_up
) {
    TextPermutationResult res;
    if (full_text.empty()) {
        res.success = false;
        res.new_text = "";
        res.message = "El documento se encuentra vacío";
        return res;
    }

    size_t len = full_text.size();
    size_t c_s = std::min(cursor_start, len);
    size_t c_e = std::min(cursor_end, len);
    if (c_s > c_e) std::swap(c_s, c_e);

    // Identificar el inicio del párrafo actual
    size_t curr_start = c_s;
    while (curr_start > 0 && full_text[curr_start - 1] != '\n') {
        --curr_start;
    }

    // Identificar el fin del párrafo actual
    size_t curr_end = c_e;
    while (curr_end < len && full_text[curr_end] != '\n') {
        ++curr_end;
    }

    if (swap_up) {
        if (curr_start == 0) {
            res.success = false;
            res.new_text = std::string(full_text);
            res.new_start = c_s;
            res.new_end = c_e;
            res.message = "El párrafo ya se encuentra al inicio del documento";
            return res;
        }

        // El párrafo anterior termina antes del salto de línea
        size_t prev_end = curr_start - 1;
        size_t prev_start = prev_end;
        while (prev_start > 0 && full_text[prev_start - 1] != '\n') {
            --prev_start;
        }

        return swap_ranges(full_text, curr_start, curr_end, prev_start, prev_end);
    } else {
        if (curr_end >= len) {
            res.success = false;
            res.new_text = std::string(full_text);
            res.new_start = c_s;
            res.new_end = c_e;
            res.message = "El párrafo ya se encuentra al final del documento";
            return res;
        }

        // El párrafo siguiente inicia después del salto de línea
        size_t next_start = curr_end + 1;
        size_t next_end = next_start;
        while (next_end < len && full_text[next_end] != '\n') {
            ++next_end;
        }

        return swap_ranges(full_text, curr_start, curr_end, next_start, next_end);
    }
}

TextPermutationResult TextManipulator::move_paragraph(
    std::string_view full_text,
    size_t cursor_start,
    size_t cursor_end,
    bool move_up
) {
    return swap_paragraph(full_text, cursor_start, cursor_end, move_up);
}

std::string TextManipulator::expand_macro_template(
    std::string_view template_text,
    const std::vector<std::pair<std::string, std::string>>& variables
) {
    std::string result(template_text);
    for (const auto& [token, value] : variables) {
        if (token.empty()) continue;
        size_t pos = 0;
        while ((pos = result.find(token, pos)) != std::string::npos) {
            result.replace(pos, token.length(), value);
            pos += value.length();
        }
    }
    return result;
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

JNIEXPORT jstring JNICALL
Java_com_example_util_NativeEngineBridge_getRustEngineVersion(
    JNIEnv* env,
    jobject /* this */
) {
    return env->NewStringUTF("DocuSheet Rust Core Engine v0.1.0 (PieceTable & CascadePaginator [Compiled])");
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_util_NativeEngineBridge_computeTableColumnWidths(
    JNIEnv* env,
    jobject /* this */,
    jint num_columns,
    jfloat printable_width_pt
) {
    if (num_columns <= 0 || printable_width_pt <= 0.0f) {
        return env->NewFloatArray(0);
    }

    docusheet::core::TypographyEngine engine;
    docusheet::core::TableLayoutResult layout = engine.compute_table_layout(num_columns, printable_width_pt);

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(layout.column_widths_pt.size()));
    if (!layout.column_widths_pt.empty()) {
        env->SetFloatArrayRegion(
            result,
            0,
            static_cast<jsize>(layout.column_widths_pt.size()),
            layout.column_widths_pt.data()
        );
    }
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_util_NativeEngineBridge_computeJustifiedSpacing(
    JNIEnv* env,
    jobject /* this */,
    jstring line_text,
    jfloat target_width_pt
) {
    if (line_text == nullptr) {
        return env->NewFloatArray(0);
    }

    const char* chars = env->GetStringUTFChars(line_text, nullptr);
    std::string_view text_view(chars);

    docusheet::core::TypographyEngine engine;
    std::vector<float> spacings = engine.compute_justified_spacing(text_view, target_width_pt);

    env->ReleaseStringUTFChars(line_text, chars);

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(spacings.size()));
    if (!spacings.empty()) {
        env->SetFloatArrayRegion(result, 0, static_cast<jsize>(spacings.size()), spacings.data());
    }
    return result;
}

// ------------------------------------------------------------------------------
// Métodos JNI para Transposición e Intercambio de Texto (PC Engine)
// ------------------------------------------------------------------------------

static jobject create_text_operation_result(
    JNIEnv* env,
    const docusheet::core::TextPermutationResult& perm
) {
    jclass clazz = env->FindClass("com/example/util/TextOperationResult");
    if (!clazz) return nullptr;

    jmethodID ctor = env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;IIZLjava/lang/String;)V");
    if (!ctor) return nullptr;

    jstring j_new_text = env->NewStringUTF(perm.new_text.c_str());
    jstring j_msg = env->NewStringUTF(perm.message.c_str());

    jobject obj = env->NewObject(
        clazz,
        ctor,
        j_new_text,
        static_cast<jint>(perm.new_start),
        static_cast<jint>(perm.new_end),
        static_cast<jboolean>(perm.success),
        j_msg
    );
    return obj;
}

JNIEXPORT jobject JNICALL
Java_com_example_util_NativeEngineBridge_swapTextRangesNative(
    JNIEnv* env,
    jobject /* this */,
    jstring full_text,
    jint start_a,
    jint end_a,
    jint start_b,
    jint end_b
) {
    if (!full_text) return nullptr;
    const char* chars = env->GetStringUTFChars(full_text, nullptr);
    std::string_view text_view(chars);

    docusheet::core::TextManipulator manipulator;
    auto res = manipulator.swap_ranges(
        text_view,
        static_cast<size_t>(start_a),
        static_cast<size_t>(end_a),
        static_cast<size_t>(start_b),
        static_cast<size_t>(end_b)
    );

    env->ReleaseStringUTFChars(full_text, chars);
    return create_text_operation_result(env, res);
}

JNIEXPORT jobject JNICALL
Java_com_example_util_NativeEngineBridge_moveTextRangeNative(
    JNIEnv* env,
    jobject /* this */,
    jstring full_text,
    jint start,
    jint end,
    jint target_position
) {
    if (!full_text) return nullptr;
    const char* chars = env->GetStringUTFChars(full_text, nullptr);
    std::string_view text_view(chars);

    docusheet::core::TextManipulator manipulator;
    auto res = manipulator.move_range(
        text_view,
        static_cast<size_t>(start),
        static_cast<size_t>(end),
        static_cast<size_t>(target_position)
    );

    env->ReleaseStringUTFChars(full_text, chars);
    return create_text_operation_result(env, res);
}

JNIEXPORT jobject JNICALL
Java_com_example_util_NativeEngineBridge_swapParagraphNative(
    JNIEnv* env,
    jobject /* this */,
    jstring full_text,
    jint cursor_start,
    jint cursor_end,
    jboolean swap_up
) {
    if (!full_text) return nullptr;
    const char* chars = env->GetStringUTFChars(full_text, nullptr);
    std::string_view text_view(chars);

    docusheet::core::TextManipulator manipulator;
    auto res = manipulator.swap_paragraph(
        text_view,
        static_cast<size_t>(cursor_start),
        static_cast<size_t>(cursor_end),
        static_cast<bool>(swap_up)
    );

    env->ReleaseStringUTFChars(full_text, chars);
    return create_text_operation_result(env, res);
}

JNIEXPORT jstring JNICALL
Java_com_example_util_NativeEngineBridge_expandMacroTemplateNative(
    JNIEnv* env,
    jobject /* this */,
    jstring template_text,
    jobjectArray keys_array,
    jobjectArray values_array
) {
    if (!template_text) return nullptr;
    const char* t_chars = env->GetStringUTFChars(template_text, nullptr);
    std::string_view tmpl_view(t_chars);

    std::vector<std::pair<std::string, std::string>> variables;
    if (keys_array && values_array) {
        jsize k_len = env->GetArrayLength(keys_array);
        jsize v_len = env->GetArrayLength(values_array);
        jsize count = std::min(k_len, v_len);
        variables.reserve(count);
        for (jsize i = 0; i < count; ++i) {
            auto j_key = (jstring)env->GetObjectArrayElement(keys_array, i);
            auto j_val = (jstring)env->GetObjectArrayElement(values_array, i);
            if (j_key && j_val) {
                const char* k_chars = env->GetStringUTFChars(j_key, nullptr);
                const char* v_chars = env->GetStringUTFChars(j_val, nullptr);
                variables.emplace_back(std::string(k_chars), std::string(v_chars));
                env->ReleaseStringUTFChars(j_key, k_chars);
                env->ReleaseStringUTFChars(j_val, v_chars);
            }
            if (j_key) env->DeleteLocalRef(j_key);
            if (j_val) env->DeleteLocalRef(j_val);
        }
    }

    docusheet::core::TextManipulator manipulator;
    std::string expanded = manipulator.expand_macro_template(tmpl_view, variables);

    env->ReleaseStringUTFChars(template_text, t_chars);
    return env->NewStringUTF(expanded.c_str());
}

}

