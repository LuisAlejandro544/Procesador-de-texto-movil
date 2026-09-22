# Estructura del Proyecto — DocuSheet 🏛️📦

Este documento detalla la arquitectura modular, el árbol de directorios, el modelo de datos y las responsabilidades de cada componente en la aplicación.

---

## 🏗️ Patrón Arquitectónico: Híbrido Tri-Lenguaje (Kotlin + Rust + C++)

El proyecto sigue una arquitectura desacoplada y de alto rendimiento que combina lo mejor de tres mundos:

```
┌────────────────────────────────────────────────────────┐
│           Capa Visual Móvil (UI Táctil)                │
│       Jetpack Compose + Material 3 (Kotlin)            │
└───────────────────────────▲────────────────────────────┘
                            │ JNI / Enlaces Kotlin (NativeEngineBridge)
┌───────────────────────────▼────────────────────────────┐
│      Motor de Lógica y Documentos (Rust 2021)          │
│   • Piece Table (búfer inmutable + add buffer)         │
│   • Paginación matemática en cascada (CascadePaginator)│
└───────────────────────────▲────────────────────────────┘
                            │ Puente directo CXX (Cero Costo)
┌───────────────────────────▼────────────────────────────┐
│   Motor Gráfico y Tipográfico de Imprenta (C++20)      │
│   • TypographyEngine (C++20 Concepts, std::span)       │
│   • CMakeLists.txt & Android NDK (32 y 64 bits)        │
└────────────────────────────────────────────────────────┘
```

---

## 📁 Árbol de Archivos del Proyecto

```
/
├── build_rust.sh                        # Script automatizado de compilación cruzada Rust para 4 ABIs NDK
├── rust-core/                           # Núcleo de Alto Rendimiento en Rust (Edición 2021)
│   ├── Cargo.toml                       # Dependencias: cxx, serde, serde_json y perfiles de compilación
│   └── src/
│       ├── lib.rs                       # Exportación del crate y versión del motor
│       ├── piece_table.rs               # Estructura Piece Table para búfer de texto infinito
│       ├── pagination.rs                # Algoritmo de partición de hojas en cascada continua
│       └── bridge.rs                    # Puente de enlace CXX de cero costo con C++20
│
├── app/
│   ├── src/main/cpp/                    # Núcleo Tipográfico y de Medición en C++20
│   │   ├── CMakeLists.txt               # Configuración CMake estándar C++20 estricto y enlace libdocusheet_rust
│   │   ├── docusheet_core.hpp           # Cabecera C++20 con conceptos, métricas de hoja y clases
│   │   └── docusheet_core.cpp           # Motor tipográfico, cálculo de tablas y llamadas JNI
│   │
│   ├── src/main/jniLibs/                # Binarios compartidos compilados de Rust para las 4 arquitecturas
│   │   ├── arm64-v8a/libdocusheet_rust.so
│   │   ├── armeabi-v7a/libdocusheet_rust.so
│   │   ├── x86/libdocusheet_rust.so
│   │   └── x86_64/libdocusheet_rust.so
│   │
│   └── src/main/java/com/example/
│       ├── MainActivity.kt              # Punto de entrada de la app, activa Edge-to-Edge y carga NavGraph
│       │
│       ├── data/                        # Capa de Persistencia Local (Room / SQLite)
│       │   ├── DocumentEntity.kt        # Entidad Room que modela la hoja de texto y sus atributos
│       │   ├── DocumentDao.kt           # Interfaz DAO con consultas SQL reactivas (Flow) y operaciones CRUD
│       │   ├── AppDatabase.kt           # Base de datos Room (v5) con migraciones y precarga
│       │   ├── DocumentRepository.kt    # Repositorio que aísla las operaciones de base de datos en Dispatchers.IO
│       │   ├── macro/                   # Motor de Macros y Plantillas Dinámicas
│       │   │   ├── MacroEntity.kt       # Entidad Room para plantillas, disparadores y variables dinámicas
│       │   │   ├── MacroDao.kt          # Consultas para macros del sistema y personalizadas
│       │   │   ├── MacroSeedData.kt     # Catálogo de macros pre-hechas (:acta:, :carta:, :minuta:, etc.)
│       │   │   └── MacroRepository.kt   # Repositorio asíncrono en Dispatchers.IO
│       │   └── synonym/                 # Motor de Diccionario de Sinónimos Local (100% Offline)
│       │       ├── SynonymEntity.kt     # Entidad Room que mapea vocablos, sinónimos, raíz lematizada y categoría
│       │       ├── SynonymDao.kt        # Consultas de coincidencia exacta y por lema (stemming)
│       │       ├── ThesaurusSeedData.kt # Catálogo base en español precargado al inicializar la base de datos
│       │       └── ThesaurusRepository.kt # Acceso asíncrono y resolución de sinónimos en segundo plano
│       │
│       ├── ui/
│       │   ├── DocumentViewModel.kt     # Gestor de estado: macros, buscador, radar de estilo, sinónimos, deshacer/rehacer y métricas
│       │   │
│       │   ├── components/              # Componentes visuales reutilizables
│       │   │   ├── PaperSheet.kt        # Lienzo de hoja de papel, Cascada Continua, reglas, guías y parser de bloques
│       │   │   ├── TableSheetBlock.kt   # Renderizador físico de tablas y cuadrículas editoriales sobre la hoja
│       │   │   ├── TableInsertDialog.kt # Diálogo táctil de configuración e inserción de tablas
│       │   │   ├── DocuSheetSearchRadarBar.kt # Panel táctil de búsqueda, reemplazo, carrusel y radar de redundancia
│       │   │   ├── DocuSheetSelectionToolbar.kt # Barra contextual estilo PC (reemplazo del selector del fabricante) y paleta de tinta
│       │   │   └── DocuSheetMacroBottomSheet.kt # Panel táctil modal de macros, variables dinámicas y creación rápida
│       │   │
│       │   ├── navigation/              # Capa de Navegación
│       │   │   └── NavGraph.kt          # Grafo central con rutas: documents, editor, settings, about, macros
│       │   │
│       │   ├── screens/                 # Pantallas completas de la aplicación
│       │   │   ├── DocumentListScreen.kt# Biblioteca de documentos, selector de PDFs externos, miniaturas y plantillas
│       │   │   ├── DocumentEditorScreen.kt # Pantalla del editor con barra de herramientas, macros, exportación y cascada
│       │   │   ├── DocumentSettingsScreen.kt # Ajustes de papel (texturas, fuentes Serif/Sans/Mono/Cursive)
│       │   │   ├── AboutScreen.kt       # Centro de Métricas Detalladas, anillo de progreso, gráficas y acceso a macros
│       │   │   ├── PdfViewerScreen.kt   # Visor nativo de PDF de alta resolución con zoom táctil y estética de hoja
│       │   │   └── MacroManagerScreen.kt# Gestor integral de macros, simulador de evaluación y catálogo de variables
│       │   │
│       │   └── theme/                   # Sistema de Diseño y Tokens
│       │       ├── Color.kt             # Paleta de colores M3
│       │       ├── Theme.kt             # Configuración del tema y límite de escala tipográfica fija (1.0f)
│       │       └── Type.kt              # Jerarquía tipográfica base
│       │
│       └── util/
│           ├── DocumentExporter.kt      # Generación de PDF (A4), Markdown (.md), HTML Editorial y Texto Plano (.txt)
│           ├── DocuSheetCacheManager.kt # Gestión inteligente de caché (RAM LRU 25%, disco 50MB, poda y persistencia de fotos)
│           ├── PageFormat.kt            # Catálogo de formatos de hoja (A4, Letter, Legal, A5, Custom) y capacidad estándar
│           ├── StyleRadarEngine.kt      # Motor de redundancia con Apache Lucene (SpanishAnalyzer, SpanishLightStemmer)
│           ├── MacroEngine.kt           # Motor de evaluación de variables y detección de disparadores in-place
│           └── NativeEngineBridge.kt    # Puente seguro de carga JNI para 'docusheet_core' (C++20 y Rust)
│
└── res/
    ├── xml/
    │   └── file_paths.xml               # Rutas autorizadas para FileProvider para compartir PDF, MD, HTML y TXT
    ├── drawable/                        # Recursos gráficos vectoriales (ic_doc_logo, launcher)
    └── values/strings.xml               # Textos de la aplicación en español
```

---

## 🗄️ Modelo de Datos: `DocumentEntity` (Base de Datos Room v5)

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` (Primary Key) | Identificador único autoincremental |
| `title` | `String` | Nombre o título de la hoja |
| `content` | `String` | Texto redactado en la hoja (incluye saltos de página y formato) |
| `paperType` | `String` | Textura/color: `WHITE`, `IVORY`, `LINED`, `GRID`, `SEPIA`, `DARK` |
| `fontStyle` | `String` | Tipografía: `SERIF`, `SANS_SERIF`, `MONOSPACE`, `CURSIVE` |
| `fontSize` | `Int` | Tamaño de letra en SP (14 a 24) |
| `lineSpacing` | `Float` | Interlineado multiplicador (1.2f, 1.5f, 2.0f) |
| `marginStyle` | `String` | Márgenes de la hoja: `NARROW`, `NORMAL`, `WIDE` |
| `alignment` | `String` | Alineación base del documento: `LEFT`, `CENTER`, `RIGHT`, `JUSTIFY` |
| `pageSize` | `String` | Formato físico de papel: `A4`, `LETTER`, `LEGAL`, `A5`, `CUSTOM` |
| `wordsPerPage` | `Int` | Límite máximo de palabras por hoja antes de generar página automática |
| `createdAt` | `Long` | Timestamp de creación en milisegundos |
| `updatedAt` | `Long` | Timestamp de última modificación |

### 🗄️ Modelo de Datos: `MacroEntity` (Automatizaciones y Plantillas)

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` (Primary Key) | Identificador único autoincremental |
| `name` | `String` | Título representativo de la macro |
| `description` | `String` | Descripción explicativa de su propósito |
| `triggerKeyword` | `String` | Atajo de teclado/escritura (ejemplo: `:acta:`, `:carta:`) |
| `category` | `String` | Clasificación: `DOCUMENTOS`, `EDITORIAL`, `TRABAJO`, `ESTRUCTURA` |
| `templateContent` | `String` | Cuerpo de la plantilla con etiquetas `{VARIABLE}` |
| `isSystemDefault` | `Boolean` | Indicador si es plantilla de fábrica o creada por el usuario |
| `usageCount` | `Int` | Frecuencia de uso para ordenamiento ergonómico |
| `createdAt` | `Long` | Timestamp de registro |

---

## 🎨 Sistema de Formateo y Renderizado de PC (PaperSheet & Native)

DocuSheet incorpora un procesador híbrido de sintaxis enriquecida adaptado a hojas físicas:

1. **Alineación de Párrafos Cuádruple con Justificado Real**:
   - Directivas por párrafo: `[align:left]`, `[align:center]`, `[align:right]`, `[align:justify]`.
   - Propiedad global en `DocumentEntity.alignment`.
   - Cálculo JNI en C++20 (`compute_justified_spacing`) que determina la distribución proporcional de espacios para simular tipografía editorial de libro.
2. **Tipografía Dinámica Inline y Global**:
   - `[font:serif]`, `[font:sans]`, `[font:mono]`, `[font:cursive]`.
   - Modificación instantánea de fragmentos de texto o de la hoja entera.
3. **Formatos Enriquecidos**:
   - Subrayado (`<u>...</u>`, `__...__`)
   - Tachado (`~~...~~`, `<s>...</s>`)
   - Subíndice (`<sub>...</sub>`) con escalado `0.75x` y traslación vertical positiva.
   - Superíndice (`<sup>...</sup>`) con escalado `0.75x` y traslación vertical negativa.
4. **Inserción de Imágenes con Ajuste de Hoja (Layout & Wrap)**:
   - Sintaxis: `![wrap:full](url "pie de foto")`, `![wrap:center](url)`, `![wrap:left](url)`, `![wrap:right](url)`.
   - Renderizado asíncrono con Coil (`AsyncImage`) integrado con sombras, bordes de papel y pie de figura.
   - Algoritmo de altura matemática en Rust (`pagination.rs`) que evita el desbordamiento de las hojas.
5. **Selector Contextual Estilo PC, Modos Mover e Intercambiar y Paleta de Colores (`DocuSheetSelectionToolbar.kt`)**:
   - `DocuSheetDisabledSystemToolbar`: Silencia el menú contextual flotante del fabricante del teléfono (`LocalTextToolbar`).
   - `DocuSheetPcSelectionBar`: Barra flotante estilo procesador de PC que surge al existir texto seleccionado (`!textFieldValue.selection.collapsed`) o cuando hay un bloque marcado para intercambio.
   - Operaciones de portapapeles de PC: Copiar, Cortar, Pegar, Seleccionar Todo.
   - Modo Intercambiar (Swap A ⇄ B): Fijación de Bloque A en estado reactivo (`MarkedSwapBlock`), selección de Bloque B distante y transposición atómica bidireccional. Incluye botones dedicados para swap rápido de párrafo arriba/abajo (`swapParagraphUp`, `swapParagraphDown`) e intercambio con portapapeles.
   - Modo Mover (Move / Drag): Botones de acción directa y menú para reubicar fragmentos al inicio (`moveSelectionToStart`) o al final (`moveSelectionToEnd`) de la hoja de papel.
   - Paleta de colores de tinta: Formateo con etiquetas `[color:#HEX]...[/color]` soportadas por el motor de renderizado `PaperSheet.kt`.
   - Selector directo de fuente para la selección (`[font:serif|sans|mono|cursive]`).
6. **Herramienta de Tablas y Cuadrículas Editoriales (`TableSheetBlock.kt` & `TableInsertDialog.kt`)**:
   - Diálogo táctil accesible para configuración de dimensiones (1 a 10 filas, 1 a 6 columnas), interruptor de encabezado y selector de 4 acabados editoriales (Clásica, Editorial, Rayada, Compacta) con vista previa gráfica.
   - Cálculo nativo en C++20 (`compute_table_layout`) para la distribución métrica armónica de anchos de columna sobre la hoja física A4 (ancho imprimible de ~480 pt).
   - Renderizado en bloques independientes sobre la hoja de papel (`SheetBlock.Table`), respetando la familia tipográfica, el tamaño de letra e interlineado del documento.
   - Sintaxis estructurada `[table:estilo]...[/table]` y compatibilidad universal con tablas Markdown estándar (`| celda | celda |`).
7. **Motor Nativo de Manipulación de Texto (`TextManipulator` C++20 y Rust)**:
   - Procesamiento de bajo nivel para transposiciones y desplazamientos de bloques sin recomposiciones pesadas.
   - Algoritmo de permutación de 3 partes que reconstruye la cadena evitando fragmentación de memoria y manteniendo la coherencia de saltos de línea.
   - Respaldo de seguridad (fallback) transparente en `NativeEngineBridge.kt`.
8. **Visor Nativo de PDF e Integración "Abrir Con" (`PdfViewerScreen.kt` & `DocumentExporter.kt`)**:
   - Integración a nivel de sistema mediante filtros de intención `ACTION_VIEW` y `ACTION_SEND` en `AndroidManifest.xml` (`application/pdf`).
   - `MainActivity.kt` procesa y redirige el URI del documento hacia la pantalla modular `PdfViewerScreen`.
   - Renderizado en alta definición 2x con `android.graphics.pdf.PdfRenderer` en hilos de fondo (`Dispatchers.IO`), con reciclaje de bitmaps en `DisposableEffect`.
   - Estética idéntica a las hojas de DocuSheet, zoom gestual (pinch-to-zoom 0.75x a 3.5x), paneo, botones de escala y botón de compartir.
   - Exportador extendido a **HTML Editorial Estructurado** (con estilos CSS integrados para lectura tipo libro) y **Texto Plano (.txt)** para interoperabilidad total.
9. **Sistema de Macros, Automatizaciones y Expansión Dinámica (`MacroEngine.kt` & `docusheet_core.cpp`)**:
   - Sustitución de etiquetas `{VARIABLE}` ejecutada en C++20 con `std::string_view` y punteros de memoria directos (`expand_macro_template`).
   - Variables contextuales evaluadas: `{FECHA}`, `{FECHA_ISO}`, `{HORA}`, `{TITULO}`, `{AUTOR}`, `{TOTAL_PALABRAS}`, `{PAGINA_ACTUAL}`, `{TOTAL_PAGINAS}`, `{FORMATO_HOJA}`, `{CLIPBOARD}`, `{SELECCION}`, `{DISPARADOR}`, `{TABLA_2X3}`, `{TABLA_3X3}`, `{LISTA_TAREAS}`, `{ALEATORIO_ID}`, `{HASH_DOC}`.
   - Detección en vivo de disparadores (`checkAndExpandMacroTrigger`) al escribir en la hoja física (`:acta:`, `:carta:`, `:minuta:`, etc.).
   - Panel modal de selección rápida `DocuSheetMacroBottomSheet.kt` en el editor y pantalla de gestión `MacroManagerScreen.kt` con pestaña de simulación y evaluación en tiempo real.
   - Persistencia local en SQLite mediante Room v5 (`MacroEntity`, `MacroDao`, `MacroRepository`, `MIGRATION_4_5`).

---

## 🔒 Protección Tipográfica del Sistema

En `Theme.kt`, la aplicación encapsula el árbol de componentes dentro de un `CompositionLocalProvider` que suministra una instancia de `Density` con `fontScale = 1.0f`. Esto impide que los ajustes de accesibilidad de fuente del sistema operativo Android deformen la proporción calculada de la hoja, la regla o las barras de herramientas.
Requisito mínimo de sistema operativo: **Android 9.0 (API 28 - Pie)**.
