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

## 🖼️ Fase 11: Inserción desde Galería, Gestión Inteligente de Caché y Formatos de Hoja (Completada ✅)
- [x] **Inserción de Imágenes desde Galería Móvil**:
  - Integración del Android Photo Picker oficial (`ActivityResultContracts.PickVisualMedia`) sin permisos invasivos.
  - Previsualización en tiempo real dentro del diálogo `ImageDialog` con Coil `AsyncImage`.
  - Compresión y procesamiento en segundo plano (`Dispatchers.IO`) a resolución optimizada (JPEG 86%, máx. 1920 px).
  - Almacenamiento local persistente en `filesDir/doc_images` blindando las imágenes contra la expiración de URIs de Android.
- [x] **Gestión Inteligente de Memoria Caché y Almacenamiento (`DocuSheetCacheManager`)**:
  - Configuración optimizada de Coil con política LRU en memoria RAM (tope 25%) y límite de disco de 50 MB.
  - Poda automática programada (`autoPruneIfExceeded`) de imágenes huérfanas y archivos temporales de exportación mayores a 7 días.
  - Panel interactivo de estado de almacenamiento en `AboutScreen` con botón de liberación inmediata de memoria caché.
- [x] **Formatos Físicos de Hoja y Paginación Dinámica (`PageFormat`)**:
  - Formatos estándar: **A4 Estándar** (210×297 mm), **Carta / Letter** (216×279 mm), **Oficio / Legal** (216×356 mm), **Cuartilla / A5** (148×210 mm) y **Personalizado**.
  - Control de palabras por hoja mediante control deslizante (Slider 100 a 800 palabras) y botones +/- 50 palabras en `DocumentSettingsScreen`.
  - Paginación automática ininterrumpida: generación progresiva de hojas (Pág. 2, 3...) al sobrepasar el límite de palabras sin cortar la escritura.
  - Migración de base de datos Room v2 a v3 (`MIGRATION_2_3`) con campos `pageSize` y `wordsPerPage`.

---

## 🔍 Fase 12: Buscador de PC, Radar de Redundancia (Apache Lucene) y Diccionario de Sinónimos Offline (Completada ✅)
- [x] **Integración de Motor Lingüístico Apache Lucene**:
  - Integración de `lucene-core` y `lucene-analyzers-common` (v8.11.2) en Gradle y Version Catalog.
  - Módulo `StyleRadarEngine.kt` con análisis tokenizado formal en español (`SpanishAnalyzer`) y lematización morfológica con `SpanishLightStemmer`.
  - Detección de proximidad léxica basada en ventanas móviles de palabras (umbrales: Crítico <40 palabras, Moderado 40..120 palabras, Aceptable >120 palabras).
  - Cálculo de densidad léxica y diagnóstico automático con recomendaciones de estilo en español.
- [x] **Diccionario Local de Sinónimos (100% Offline)**:
  - Creación de entidades Room `SynonymEntity` y `SynonymDao` con migración segura a Room v4 (`MIGRATION_3_4`).
  - Repositorio `ThesaurusRepository` y catálogo de precarga inicial `ThesaurusSeedData` con cientos de equivalencias léxicas en español.
  - Búsqueda híbrida instantánea: concordancia exacta del vocablo y concordancia por raíz lematizada (stem).
- [x] **Buscador Táctil de PC y Carrusel de Navegación (`DocuSheetSearchRadarBar.kt`)**:
  - Panel superior flotante con campo de búsqueda, contador de ocurrencias estilo PC (ej. *«1 / 5»*) y botones de salto secuencial anterior/siguiente.
  - Modo desplegable de Reemplazo individual y Reemplazo masivo en todo el documento.
  - Carrusel horizontal deslizable (`LazyRow`) con chips táctiles de sinónimos offline: sustitución instantánea en la hoja física al tocarlos.
- [x] **Integración con la Barra Contextual de PC (`DocuSheetPcSelectionBar`)**:
  - Botón directo *«Sinónimos/Radar»* al seleccionar cualquier palabra en la hoja para consultar alternativas de inmediato.

---

## ⚡ Fase 13: Sistema de Macros, Automatizaciones y Expansión Dinámica de Plantillas (Completada ✅)
- [x] **Motor Nativo de Sustitución Acelerada en C++20 (`expand_macro_template`)**:
  - Implementación con `std::string_view` y reserva de memoria previa en `docusheet_core.cpp` para evaluar plantillas al instante sin impacto en la tasa de refresco (60 FPS).
  - Enlace JNI en `NativeEngineBridge.kt` (`expandMacroTemplateSafe`) y motor en Kotlin `MacroEngine.kt`.
- [x] **Variables Contextuales Dinámicas de Documento y Sistema**:
  - Integración de variables: `{FECHA}`, `{FECHA_ISO}`, `{HORA}`, `{TITULO}`, `{AUTOR}`, `{TOTAL_PALABRAS}`, `{PAGINA_ACTUAL}`, `{TOTAL_PAGINAS}`, `{FORMATO_HOJA}`, `{CLIPBOARD}`, `{SELECCION}`, `{DISPARADOR}`, `{TABLA_2X3}`, `{TABLA_3X3}`, `{LISTA_TAREAS}`, `{ALEATORIO_ID}`, `{HASH_DOC}`.
- [x] **Catálogo Extenso de Macros Pre-Construidas**:
  - Plantillas de alta fidelidad: Acta de Reunión (`:acta:`), Carta Formal de Solicitud (`:carta:`), Minuta Técnica (`:minuta:`), Resumen Ejecutivo (`:resumen:`), Ficha de Proyecto (`:proyecto:`), Lista de Tareas / Sprint (`:tareas:`), Cita Bibliográfica Formal (`:cita:`).
- [x] **Detección y Expansión en Vivo al Escribir (In-place Triggering)**:
  - Detección reactiva en `onTextFieldValueChange`: al escribir un atajo como `:acta:`, `:carta:` o `:minuta:` seguido de dos puntos o espacio, el texto se expande automáticamente reemplazando el disparador sin interacción manual.
- [x] **Panel Modal Rápido en el Editor (`DocuSheetMacroBottomSheet`)**:
  - Acceso directo mediante el botón *«Macros»* en la barra de herramientas del editor.
  - Pestañas por categoría (Documentos, Editorial, Trabajo, Estructura), previsualización de variables y creador de macros personalizadas.
- [x] **Pantalla Dedicada de Gestión y Laboratorio (`MacroManagerScreen`)**:
  - Nueva ruta en `NavGraph.kt` (`macros`) con acceso desde `AboutScreen`.
  - Pestaña de biblioteca de macros, pestaña de simulador en vivo que evalúa las variables contra el documento activo, y catálogo explicativo de variables dinámicas.
- [x] **Persistencia en Room v5**:
  - Entidad `MacroEntity`, interfaz `MacroDao`, repositorio asíncrono `MacroRepository` y migración segura `MIGRATION_4_5`.

---

## 🎨 Fase 14: Tipografía Avanzada 3D, Figuras Geométricas y Nodos de Diagrama de PC (Completada ✅)
- [x] **Motor y Diálogo de Tipografía Avanzada de PC (`TextStyle3dDialog.kt`)**:
  - Selector de color de texto con paleta visual y soporte para código hexadecimal libre (`[color:#HEX]...[/color]`).
  - Selector de grosor métrico de letra con 9 niveles de peso tipográfico: Fino (Thin 100) a Negro (Black 900) con etiquetas estructuradas `[weight:grosor]...[/weight]`.
  - Motor de relieve y sombra estereoscópica 3D (`[3d:#sombra,#relieve]...[/3d]`) con controles deslizantes e interruptores táctiles.
  - Renderizado en tiempo real sobre la hoja física (`PaperRichVisualTransformation.kt`) con `Shadow(color, offset, blurRadius)` de Compose.
  - Renderizado vectorial de sombras y efectos 3D en la exportación a PDF digital multipágina en `DocumentExporter.kt`.
- [x] **Motor Gráfico de Figuras Geométricas Vectoriales de PC (`InsertShapeOrNodeDialog.kt`)**:
  - Catálogo de formas: Rectángulo, Rectángulo redondeado, Círculo / Óvalo, Triángulo, Rombo, Estrella de 5 puntas, Flecha derecha, Flecha izquierda y Llamada de texto.
  - Configuración completa de PC: ancho y alto (60 a 450 pt), alineación en la página (izquierda, centro, derecha), color de relleno, color de contorno y texto interior opcional.
  - Sintaxis de marcado estructurada: `[shape:tipo,w=...,h=...,align=...,fill=...,stroke=...]Texto interior[/shape]`.
- [x] **Diagramas de Nodos y Flujos Secuenciales de PC**:
  - Creación interactiva de cadenas de pasos o mapas conceptuales con flechas conectoras continuas.
  - Orientación horizontal y vertical con colores de nodo y conectores configurables.
  - Marcado sintáctico: `[nodes:horizontal|vertical,color=...,line=...]Paso 1 -> Paso 2 -> Paso 3[/nodes]`.
- [x] **Integración en Lienzo de Papel y Exportador a PDF Nativo**:
  - Renderizado modular de figuras y grafos sobre el lienzo de la hoja física con Compose Canvas en `PaperSheet.kt` y `PaperBlockRenderer.kt`.
  - Exportación vectorial nítida de alta resolución a PDF digital con `android.graphics.Canvas`, trazado `Path` y pinceles en `DocumentExporter.kt`.
- [x] **Optimizaciones Ergonómicas del Entorno de Redacción**:
  - Buscador de PC expandido y adaptado a pantallas táctiles de smartphone para visualización cómoda de texto.
  - Asistente de inserción y edición intuitiva de tablas editoriales.

---

## 📦 Fase 15: Interoperabilidad Universal de Documentos (.DOCX, .RTF, .TEX, .MD, .TXT) (Completada ✅)
- [x] **Motor OpenXML para Microsoft Word (.docx)**:
  - Exportación sin dependencias pesadas mediante empaquetado ZIP nativo (`ZipOutputStream`), generando estructura OpenXML (`word/document.xml`, `[Content_Types].xml`, `_rels`) con jerarquías, tablas y colores.
  - Importación y descompresión asíncrona mediante `XmlPullParser` extrayendo párrafos, títulos y celdas directamente al lienzo de la hoja.
- [x] **Motor de Formato Enriquecido Universal (.rtf / Rich Text Format)**:
  - Generación de archivos RTF estándar con tabla de fuentes (`Calibri`, `Times New Roman`, `Courier New`), tabla cromática de 5 tintas, tablas formateadas con comandos de celda (`\cell`, `\row`) y saltos de página (`\page`).
  - Parser de lectura de secuencias de escape RTF con soporte de caracteres Unicode (`\uN`), negritas (`\b`), cursivas (`\i`), listas y tablas.
- [x] **Motor de Maquetación Científica y Académica LaTeX (.tex)**:
  - Exportación a código fuente LaTeX limpio y compilable para pdfLaTeX y XeLaTeX con preámbulo formal (`article`, A4, `babel[spanish]`, `amsmath`, `booktabs`, `tcolorbox`, `enumitem` y `hyperref`).
  - Conversión vectorial de figuras geométricas y diagramas de nodos a cajas decorativas `tcolorbox`.
  - Parser de importación que traduce `\title`, `\section`, `\subsection`, `\subsubsection`, `\item`, citas y tablas de entorno `tabular` a la sintaxis visual de DocuSheet.
- [x] **Coordinador Universal de Importación (`DocumentImporter.kt`)**:
  - Detección inteligente por extensión de archivo y análisis de firmas/números mágicos binarios en cabecera (`PK..`, `{\rtf`, `\documentclass`).
  - Ejecución completamente asíncrona en `Dispatchers.IO` para evitar bloqueos del hilo principal en teléfonos móviles.
- [x] **Integración de Usuario Móvil de Alta Accesibilidad**:
  - Botón táctil directo de importación (`Icons.Outlined.FileOpen`) en la barra superior de `DocumentListScreen`.
  - Tarjeta de opción «Importar Documento Externo» en el diálogo de plantillas de nueva hoja (`showNewDocDialog`).
  - Botón complementario «Importar» en la vista de estado vacío de la biblioteca de documentos.
  - Opciones completas de exportación en el menú desplegable del editor (`EditorTopBar`): Word (.docx), RTF (.rtf), LaTeX (.tex) con iconos y colores temáticos representativos.

---

## 🌟 Fase 16: Próximas Mejoras Planificadas
- [ ] Conexión completa del bucle de eventos del teclado táctil con el búfer Piece Table de Rust.
- [ ] Modo Enfoque Zen (pantalla completa sin ningún botón visible durante la escritura continua).
- [ ] Copia de seguridad y restauración local mediante archivo comprimido.
- [ ] Firmas manuscritas vectoriales insertables sobre la hoja.

