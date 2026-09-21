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

## 💎 Fase 6: Capacidades Potentes de Procesador de PC (Android 9.0+ Pie) (Completada ✅)
- [x] **Elevación a Android 9.0+ (API 28)**: Superación de restricciones de sistemas antiguos para habilitar renderizado gráfico de alta fidelidad, nuevas APIs de fuentes y pipeline nativo moderno.
- [x] **Alineación Cuádruple con Justificado Real**:
  - Soporte de alineación a nivel de párrafo individual (`[align:left|center|right|justify]`) y global de documento (`alignment`).
  - Motor nativo en C++20 (`compute_justified_spacing`) expuesto vía JNI para computar espaciado inter-palabras perfecto.
- [x] **Formatos Tipográficos Avanzados**:
  - Subrayado (`<u>texto</u>` y `__texto__`).
  - Tachado (`~~texto~~` y `<s>texto</s>`).
  - Subíndices (`<sub>fórmula</sub>`).
  - Superíndices (`<sup>exponente</sup>`).
  - Cambio tipográfico en cualquier momento (`[font:serif|sans|mono|cursive]`) y selector global en pantalla de ajustes.
- [x] **Inserción de Imágenes con Ajuste de Hoja (Layout & Wrap)**:
  - Modos de ajuste: Ancho Completo (`full`), Centrado (`center`), Flotante Izquierda (`left`), Flotante Derecha (`right`).
  - Diálogo interactivo para inserción de URL y pie de foto.
  - Integración asíncrona con Coil Compose con bordes, sombreado de papel y recorte de esquinas.
  - Ponderación matemática en el paginador de Rust (`pagination.rs`) para reservar espacio de hoja exacto.
- [x] **Migración de Base de Datos Room a v2**:
  - Inclusión del campo `alignment` con migración `MIGRATION_1_2` transparente para preservar datos del usuario.

---

## 🖥️ Fase 7: Selector Contextual de PC y Paleta de Colores (Completada ✅)
- [x] **Desactivación del Selector Nativo del Teléfono**: Sustitución del menú emergente impuesto por el fabricante (`DocuSheetDisabledSystemToolbar`) para mantener una vista limpia de la hoja.
- [x] **Barra de Selección Contextual Estilo PC (`DocuSheetPcSelectionBar`)**:
  - Activación automática al seleccionar texto con el cursor en la hoja de papel.
  - Operaciones de portapapeles: Copiar, Cortar, Pegar, Seleccionar Todo.
- [x] **Paleta de Colores de Tinta**:
  - Selector de color de texto seleccionado (Negro Carbón, Azul Pluma, Rojo Carmesí, Verde Esmeralda, Púrpura Real, Ámbar Dorado, Marrón Sepia, etc.).
  - Sintaxis de marcado: `[color:#HEX]texto[/color]`.
  - Parseo y renderizado de texto enriquecido con color en tiempo real en `PaperSheet.kt`.
- [x] **Tipografía y Estilos Dinámicos de Selección**:
  - Aplicación directa de fuentes (`Serif`, `Sans`, `Mono`, `Cursive`) a la selección.
  - Acciones rápidas para Negrita, Cursiva, Subrayado y Tachado.

---

## 📊 Fase 8: Tablas Editoriales y Compilación Nativa Integral en APK (Completada ✅)
- [x] **Pipeline de Compilación Cruzada Automatizada de Rust**:
  - Script `build_rust.sh` con configuración de linkers clang del NDK para compilar `rust-core` en `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
  - Generación directa de `libdocusheet_rust.so` en `app/src/main/jniLibs/<abi>/`.
  - Tarea Gradle `buildRustCore` integrada con `preBuild` que asegura compilación antes de invocar CMake.
- [x] **Enlace C++20 y Empaquetado APK Completo**:
  - `CMakeLists.txt` enlazando la librería compartida de Rust con el núcleo C++20 `libdocusheet_core.so`.
  - Inclusión de ambos binarios nativos en el APK final para 32 y 64 bits sin fallbacks ficticios.
  - Funciones nativas `computeTableColumnWidthsSafe` y `getRustEngineVersionSafe` conectadas mediante JNI en `NativeEngineBridge.kt`.
- [x] **Herramienta de Tablas y Cuadrículas Editoriales**:
  - Diálogo táctil interactivo `TableInsertDialog.kt` con controles +/- accesibles (mínimo 48x48 dp) para filas (1..10), columnas (1..6) y fila de encabezados.
  - Selector de 4 estilos tipográficos editoriales con miniatura gráfica previa:
    1. *Clásica* (cuadrícula completa y encabezado sombreado).
    2. *Editorial* (líneas horizontales sobrias sin cortes verticales).
    3. *Rayada* (filas alternadas cebra).
    4. *Compacta* (alta densidad de información).
  - Cálculo de distribución métrica de anchos de columna ejecutado por motor nativo en C++20 (`compute_table_layout`).
  - Renderizado modular sobre la hoja de papel física en `TableSheetBlock.kt` y parser estructurado en `PaperSheet.kt`.
  - Inserción inteligente desde la barra de herramientas del editor con `DocumentViewModel.insertTable()`.

---

## 🔄 Fase 9: Modos de Edición Avanzada de PC (Mover e Intercambiar / Transposición Atómica) (Completada ✅)
- [x] **Motor Nativo de Permutación y Desplazamiento**:
  - Implementación en **Rust** (`piece_table.rs`): funciones `swap_ranges` y `move_range` con punteros de intervalo exactos.
  - Implementación en **C++20** (`docusheet_core.hpp` / `.cpp`): clase `TextManipulator` con métodos JNI `nativeSwapTextRanges`, `nativeMoveTextRange` y `nativeSwapParagraph`.
  - Puente seguro y tolerante a fallos en `NativeEngineBridge.kt` (`swapTextRangesSafe`, `moveTextRangeSafe`, `swapParagraphSafe`).
- [x] **Modo Intercambiar de PC (Swap A ⇄ B)**:
  - Posibilidad de seleccionar un fragmento inicial, fijarlo como **«Bloque A»** en el estado reactivo (`MarkedSwapBlock`), seleccionar un **«Bloque B»** distante y completar la transposición atómica bidireccional.
  - Banner informativo en la barra contextual de PC mostrando la muestra del Bloque A fijado y controles para Intercambiar o Cancelar.
  - Transposición rápida de párrafos: botones directos y opciones de menú para **Swap ↑ (Arriba)** y **Swap ↓ (Abajo)** respecto al párrafo adyacente.
  - Intercambio directo con el portapapeles del teléfono (`swapSelectionWithClipboard`).
- [x] **Modo Mover / Arrastrar (Move / Drag de PC)**:
  - Acciones rápidas en la barra contextual para **Mover al Inicio (Top)** de la hoja o **Mover al Final (Bottom)** del documento.
  - Recolocación automática del cursor tras el movimiento y confirmación visual instantánea mediante Snackbar.
- [x] **Suite de Tests Unitarios de Permutación**:
  - `TextPermutationUnitTest.kt` validando swaps seguros, desplazamientos al inicio/final y transposición de párrafos adyacentes.

---

## 🔮 Fase 10: Integración con el Sistema "Abrir Con", Visor Nativo de PDF y Nuevas Exportaciones (Completada ✅)
- [x] **Visor Nativo de Documentos PDF (`PdfViewerScreen.kt`)**:
  - Renderizado vectorial de alta nitidez en pantallas de alta densidad mediante `PdfRenderer` en hilos secundarios `Dispatchers.IO`.
  - Presentación de páginas con estética idéntica a las hojas de DocuSheet (sombras de papel flotante, bordes nítidos e identificación de página).
  - Controles de zoom táctil (Pinch-to-zoom y doble toque) con rango de 75% a 350%, botones de escala (+/-) y recentrado instantáneo.
  - Barra de estado inferior con contador de páginas correlativo y botón directo de compartir.
  - Liberación segura de descriptores de archivos y memoria de bitmaps en el ciclo de vida de Compose (`DisposableEffect`).
- [x] **Integración con "Abrir Con" / "Open With" del Sistema**:
  - Filtros de intención `ACTION_VIEW` y `ACTION_SEND` en `AndroidManifest.xml` para tipo MIME `application/pdf`.
  - Extracción y normalización de URIs entrantes en `MainActivity.kt` con redirección automática al visor.
  - Botón directo en la barra superior de `DocumentListScreen` con `OpenDocument` para inspeccionar cualquier PDF almacenado en el dispositivo.
- [x] **Nuevos Formatos de Exportación Editorial**:
  - **HTML Editorial Estructurado (.html)**: Maquetación web autosuficiente con CSS embebido, tipografía de imprenta, márgenes y presentación de hoja de libro.
  - **Documento de Texto Plano (.txt)**: Archivo limpio sin etiquetas de formato para compatibilidad universal con cualquier dispositivo o terminal.
  - Inclusión en los menús de exportación del editor de texto y distribución mediante `FileProvider`.

---

## 🌟 Fase 11: Próximas Mejoras Planificadas
- [ ] Conexión completa del bucle de eventos del teclado táctil con el búfer Piece Table de Rust.
- [ ] Modo Enfoque Zen (pantalla completa sin ningún botón visible durante la escritura continua).
- [ ] Copia de seguridad y restauración local mediante archivo comprimido.
- [ ] Firmas manuscritas vectoriales insertables sobre la hoja.

