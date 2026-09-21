# Contexto para Asistentes de IA (AI Context) 🤖🧠

Este archivo proporciona el contexto técnico, principios de diseño y directrices de desarrollo para cualquier modelo de Inteligencia Artificial que trabaje con el código de **DocuSheet**.

---

## 🎯 Propósito del Proyecto
DocuSheet es un procesador de textos para Android cuyo objetivo primordial es replicar la experiencia de trabajar sobre una o múltiples **hojas de papel físico en cascada continua**, similar al área de trabajo de los procesadores de texto de PC (Word, LibreOffice Writer, Google Docs), pero adaptado a la ergonomía de un teléfono móvil.

---

## 🧭 Principios Clave de Diseño y Experiencia de Usuario

1. **La Hoja es el Centro Visual (`PaperSheet.kt`)**:
   - Todo el contenido editable debe residir sobre la superficie visual de la hoja de papel.
   - En **Modo Cascada**, los documentos extensos se presentan como una secuencia apilada verticalmente de hojas físicas separadas por márgenes de escritorio, cada una con su sombra, marcas guía y pie de página correlativo (*«— Página X de Y —»*).
   - Soporte para jerarquía tipográfica estructurada: Título 1 (#), Subtítulo (##), Apartado (###), Citas reflexivas (>), Viñetas (•), Listas numeradas y Casillas de tareas ([ ], [x]).

2. **No al Minimalismo Extremo**:
   - Al usuario le gustan las aplicaciones completas, ricas en herramientas visuales, métricas detalladas y paneles profesionales.
   - La aplicación cuenta con pantallas especializadas e interconectadas (`DocumentListScreen`, `DocumentEditorScreen`, `DocumentSettingsScreen`, `AboutScreen`).

3. **Exportación Digital Nativa Multi-Formato (`DocumentExporter.kt`)**:
   - Soporte para generar documentos **PDF digital** en formato A4 estándar con encabezados, márgenes y pie de página, archivos **Markdown (.md)** universales, **HTML Editorial Estructurado (.html)** con estilos CSS de imprenta embebidos, y **Documentos de Texto Plano (.txt)** para máxima portabilidad.
   - Se distribuyen mediante `FileProvider` sin exigir permisos invasivos de almacenamiento del sistema operativo.

4. **Visor Nativo de PDF e Integración "Abrir Con" (`PdfViewerScreen.kt`)**:
   - Filtros de intención `ACTION_VIEW` y `ACTION_SEND` en `AndroidManifest.xml` para interceptar y abrir archivos PDF desde cualquier app (WhatsApp, Drive, exploradores de archivos).
   - Renderizado en alta definición 2x con `PdfRenderer` en segundo plano (`Dispatchers.IO`), con sombras de hoja de papel idénticas a DocuSheet, zoom gestual (Pinch-to-zoom 75% a 350%), barra de estado con contador de páginas y botón para compartir.
   - Acceso desde la pantalla principal mediante el selector de documentos del sistema (`OpenDocument`).

5. **Métricas Detalladas y Gráficas de Productividad**:
   - El sistema calcula palabras, caracteres, párrafos, oraciones estimadas, tiempos de lectura y alocución oral en vivo.
   - Incluye fijación de meta diaria de escritura con anillo de progreso animado y gráficas de distribución de contenido y actividad semanal.

5. **Inmunidad a la Fuente del Sistema (Font Scale Shield)**:
   - Para evitar roturas en la maquetación milimétrica de la hoja de papel, el tema define `fontScale = 1.0f` a través de `LocalDensity`. Cualquier nueva pantalla o componente debe respetar esta jerarquía para no distorsionar las tarjetas ni las herramientas.

6. **Persistencia Local Segura (Offline First)**:
   - Toda información se almacena localmente mediante Room Database en SQLite.
   - Las operaciones de I/O a base de datos deben realizarse siempre en `Dispatchers.IO` a través de Coroutines y Flow, nunca en el hilo principal de la UI.

7. **Arquitectura Nativa Híbrida (Kotlin + Rust 2021 + C++20)**:
   - **Kotlin / Compose**: Capa visual de UI táctil, animaciones, navegación y control de eventos.
   - **Rust (rust-core, Edición 2021)**: Manipulación de texto a gran escala mediante `PieceTable` y paginación en cascada (`CascadePaginator`) para eliminar pausas del Garbage Collector.
   - **C++20 (app/src/main/cpp)**: Motor tipográfico, medición de glifos y maquetación de alto rendimiento compilado con Android NDK y CMake.
   - **Interoperabilidad CXX / JNI**: Comunicación de cero costo entre Rust y C++, y puentes JNI seguros mediante `NativeEngineBridge.kt`.
   - **Soporte de Arquitecturas**: Soporte simultáneo obligatorio para 64 bits (`arm64-v8a`, `x86_64`) y 32 bits (`armeabi-v7a`, `x86`).

8. **Potencia de Procesador de PC en Android 9.0+ (API 28)**:
   - Superación de limitaciones del sistema antiguo para incorporar renderizado de procesador de escritorio.
   - **Alineación Cuádruple con Justificado Real**: Directivas por párrafo (`[align:left|center|right|justify]`) y global de documento (`alignment`), con cálculo nativo en C++20 (`compute_justified_spacing`).
   - **Formatos Tipográficos Avanzados**: Subrayado (`<u>`, `__`), tachado (`~~`, `<s>`), subíndice (`<sub>`), superíndice (`<sup>`) y familias tipográficas por fragmento (`[font:serif|sans|mono|cursive]`).
   - **Inserción de Imágenes de Galería y Web con Ajuste de Hoja (Layout & Wrap)**: Modos `full`, `center`, `left`, `right`. Soporte nativo para selección de fotos de la galería mediante Android Photo Picker (`PickVisualMedia`) sin permisos invasivos, compresión asíncrona balanceada y guardado seguro en almacenamiento interno (`filesDir/doc_images`). Carga asíncrona con Coil Compose y reserva de altura en el paginador matemático de Rust.

9. **Selector Contextual Estilo PC y Desactivación del Selector Nativo**:
   - Reemplazo total del menú contextual por defecto del fabricante mediante `LocalTextToolbar` con `DocuSheetDisabledSystemToolbar`.
   - Control de cursor y selección precisa con `TextFieldValue` en `DocumentViewModel`.
   - Barra contextual `DocuSheetPcSelectionBar` con botones táctiles de 48dp: Copiar, Cortar, Pegar, Seleccionar Todo.
   - Selector dinámico de tipografía de fragmento (`[font:...]`) y paleta de colores de tinta directa (`[color:#HEX]...[/color]`).
   - El parser de texto enriquecido en `PaperSheet.kt` colorea de forma instantánea el texto mediante `SpanStyle(color = parsedColor)`.

10. **Herramienta de Tablas y Cuadrículas Editoriales**:
   - Diálogo interactivo táctil `TableInsertDialog.kt` adaptado a pantallas táctiles móviles con botones de al menos 48x48 dp.
   - Permite configurar dimensiones (1..6 columnas, 1..10 filas), interruptor para diferenciar la fila de encabezados y 4 estilos de maquetación editorial (Clásica, Editorial, Rayada cebra y Compacta).
   - Cálculo de distribución métrica ejecutado nativamente en C++20 (`computeTableColumnWidthsSafe`) con compilación integral para las 4 ABIs de Android (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`).
   - Renderizado visual integrado en `PaperSheet.kt` mediante bloques `SheetBlock.Table` y componente especializado `TableSheetBlock.kt`.

11. **Modos de Edición Avanzada de PC (Mover e Intercambiar / Transposición Atómica)**:
   - Transposición bidireccional atómica entre dos fragmentos arbitrarios (Bloque A ⇄ Bloque B) con banner de confirmación en la barra contextual.
   - Transposición rápida de párrafos (Swap ↑ / Swap ↓) e intercambio dinámico con el portapapeles.
   - Desplazamiento rápido de bloques seleccionados al inicio de la hoja (`moveSelectionToStart`) o al final (`moveSelectionToEnd`).
   - Algoritmo de bajo nivel implementado en C++20 (`TextManipulator`) y en el `PieceTable` de Rust (`swap_ranges` y `move_range`), con respaldo seguro en Kotlin a través de `NativeEngineBridge.kt`.

12. **Gestión Inteligente de Memoria Caché y Almacenamiento Móvil (`DocuSheetCacheManager`)**:
   - Políticas estrictas de consumo para smartphones: límite máximo del 25% de memoria RAM disponible para Coil ImageLoader con reciclaje de bitmaps y caché en disco acotada a 50 MB.
   - Poda programada en segundo plano de imágenes locales huérfanas y residuos temporales de exportación superiores a 7 días.
   - Panel de control y monitoreo en `AboutScreen` con botón de limpieza y optimización bajo demanda.

13. **Formatos Físicos de Papel y Límite de Palabras con Paginación Dinámica (`PageFormat`)**:
   - Catálogo de tamaños de hoja: A4 Estándar, Carta / Letter, Oficio / Legal, Cuartilla / A5 y Personalizado.
   - Control de densidad y umbral de palabras por hoja mediante control deslizante (Slider) y ajustes rápidos (+/- 50 palabras) en `DocumentSettingsScreen`.
   - Motor de paginación automática continua: cuando el texto supera el umbral fijado para el formato de papel seleccionado, el sistema divide y añade una nueva hoja correlativa en la cascada continua sin interrumpir la escritura.
   - Esquema Room v3 con migración segura `MIGRATION_2_3` para los campos `pageSize` y `wordsPerPage`.

---

## 🛡️ Reglas y Restricciones Estrictas

- **Derechos de autor**: No utilizar marcas registradas de terceros en nombres de clases, archivos o cadenas de texto. El proyecto se llama y se identifica como **DocuSheet**.
- **Canal de distribución**: El proyecto está preparado para distribución libre (APK independiente / Uptodown) y no debe incorporar servicios dependientes exclusivamente de los servicios de Google Play.
- **Explicación en código**: Todo archivo de código nuevo o editado debe contener comentarios y documentación KDoc clara en español explicando su propósito y funcionamiento.
- **Rendimiento móvil**: Priorizar el uso de `remember`, `derivedStateOf` y operaciones asíncronas para garantizar 60 FPS estables.
- **Preservación del Pipeline Nativo**: Nunca omitir o eliminar las configuraciones de `ndk`, `externalNativeBuild` ni las tareas de Cargo en `build.gradle.kts`.
