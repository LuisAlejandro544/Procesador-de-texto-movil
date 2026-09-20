package com.example.util

import android.util.Log

/**
 * NativeEngineBridge: Puente de enlace entre la capa de Kotlin/Compose y los núcleos
 * de alto rendimiento en C++20 y Rust.
 * 
 * Funcionalidad:
 * - Carga de forma segura y tolerante a fallos la biblioteca compartida nativa 'docusheet_core'.
 * - Expone los métodos nativos (JNI) para la medición tipográfica y paginación rápida.
 * - Proporciona estado informativo sobre si el motor nativo se encuentra enlazado.
 */
object NativeEngineBridge {
    private const val TAG = "NativeEngineBridge"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("docusheet_core")
            isNativeLoaded = true
            Log.i(TAG, "Biblioteca nativa 'docusheet_core' cargada exitosamente.")
        } catch (e: UnsatisfiedLinkError) {
            isNativeLoaded = false
            Log.w(TAG, "Motor nativo en preparación (esperando compilación NDK/Cargo): ${e.message}")
        }
    }

    /**
     * Retorna verdadero si la biblioteca compilada en C++20/Rust está activa en memoria.
     */
    fun isAvailable(): Boolean = isNativeLoaded

    /**
     * Versión reportada por el núcleo en C++20.
     */
    external fun getNativeEngineVersion(): String
}
