# Roadmap de Desarrollo — DocuSheet 🗺️📌

Este documento traza las fases evolutivas para transformar DocuSheet desde una experiencia de escritura en hoja de papel hasta un procesador de textos completo para dispositivos móviles.

---

## 🏁 Fase 1: Núcleo de la Hoja y Persistencia (Completada ✅)
- [x] Representación visual de hoja de papel flotante con elevación y sombras tridimensionales.
- [x] Regla superior graduada con marcas de centímetros.
- [x] Delimitadores de esquinas de margen (L-corners).
- [x] Persistencia local offline con Room Database y SQLite.
- [x] Control fijo y protegido de escala de fuente del sistema para salvaguardar el diseño.
- [x] Barra de herramientas rápida: Deshacer / Rehacer, viñetas, tabulaciones, fecha y separadores.
- [x] Gestión de múltiples documentos con búsqueda y plantillas iniciales.
- [x] Personalización de 6 tipos de papel, 3 familias de fuente, tamaños e interlineados.
- [x] Contador dinámico de palabras, caracteres y páginas estimadas.

---

## 🚀 Fase 2: Formato Estructurado y Tipografías (Completada ✅)
- [x] Soporte para jerarquía de títulos: **H1 Título Principal**, **H2 Subtítulo**, **H3 Apartado**.
- [x] Bloques de **Citas destacadas (`>`)** con estilo visual editorial.
- [x] **Énfasis tipográfico**: Formato para Negrita (`**texto**`) y Cursiva (`*texto*`).
- [x] **Listas y Tareas**: Viñetas (`•`), lista numerada secuencial (`1. 2.`) y casillas interactivas (`[ ]`, `[x]`).
- [x] Incorporación de la tipografía **Caligráfica / Manuscrita (Cursive)** sumada a Serif, Sans-Serif y Monospace.

---

## 📄 Fase 3: Paginación Continua en Cascada (Completada ✅)
- [x] **Paginación Continua en Cascada**: Renderizado de múltiples hojas de papel apiladas verticalmente con sombras individuales.
- [x] Inserción de saltos de página físicos (`[--- Salto de Página ---]`) y segmentación automática de hojas.
- [x] Separadores visuales de escritorio entre hojas con indicador de transición.
- [x] Encabezados y pie de página correlativos (*«— Página X de Y —»*).
- [x] Botón interactivo para añadir hojas físicas subsiguientes al documento.

---

## 📤 Fase 4: Exportación Digital y Métricas de Productividad (Completada ✅)
- [x] **Exportador a PDF Digital (.pdf)**: Renderizado en formato A4 estándar con márgenes, encabezados y paginación.
- [x] **Exportador a Markdown (.md)**: Archivo universal con metadatos y fecha de exportación.
- [x] Integración de `FileProvider` en `AndroidManifest.xml` para compartir de forma segura con cualquier app del sistema.
- [x] **Métricas Detalladas en Vivo**: Conteo de párrafos, oraciones estimadas, tiempo de lectura (200 ppm) y tiempo de discurso (130 ppm).
- [x] **Meta Diaria de Escritura**: Selector interactivo de palabras (250, 500, 1000 p) con anillo de progreso animado.
- [x] **Gráfica de distribución de contenido**: Comparador visual de barras por documento.
- [x] **Gráfica semanal de ritmo de escritura**: Visualización de los 7 días de la semana.

---

## ⚡ Fase 5: Cimientos Nativos de Alto Rendimiento (Rust 2021 + C++20) (Completada ✅)
- [x] **Configuración de Rust (Cargo)**: Creación del módulo `rust-core` con Rust 2021 Edition, dependencias `cxx`, `serde` y perfiles LTO.
- [x] **Estructura Piece Table**: Implementación de búfer inmutable y add-buffer en Rust para ediciones de texto infinitas sin Garbage Collection.
- [x] **Paginador Matemático**: Módulo `pagination.rs` para segmentación limpia de párrafos y hojas en cascada continua.
- [x] **Configuración C++20 & NDK CMake**: Creación de `CMakeLists.txt` con estándar C++20, flags de advertencia y enlaces `log`.
- [x] **Motor Tipográfico C++20**: Implementación de `TypographyEngine` (`std::span`, conceptos C++20) y puente JNI en `docusheet_core.cpp`.
- [x] **Puente Interoperable CXX**: Definición de la interfaz `bridge.rs` para comunicación de cero costo entre Rust y C++.
- [x] **Enlace Kotlin**: Clase `NativeEngineBridge.kt` con carga tolerante a fallos de librerías nativas.
- [x] **Automatización en Gradle**: Configuración de `abiFilters` (32 y 64 bits), flags CMake y tarea `buildRustCore` en `build.gradle.kts`.
- [x] **Blindaje `.gitignore`**: Reglas para ignorar carpetas y artefactos temporales de C++, CMake, NDK, Rust y Cargo.

---

## 🔮 Fase 6: Próximas Mejoras Planificadas
- [ ] Conexión completa del bucle de eventos del teclado táctil con el búfer Piece Table de Rust.
- [ ] Alineación de párrafos avanzada (Centrado, Derecha, Justificado completo con algoritmo Knuth-Plass en C++).
- [ ] Selector de color de tinta para el texto (Negro clásico, Azul tinta, Borgoña, Grafito).
- [ ] Inserción de imágenes y firmas manuscritas dentro de la hoja.
- [ ] Modo Enfoque Zen (pantalla completa sin ningún botón visible durante la escritura continua).
- [ ] Copia de seguridad y restauración local mediante archivo comprimido.
