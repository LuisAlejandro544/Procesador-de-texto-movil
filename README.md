# DocuSheet 📄✨
### Procesador de Textos Móvil con Lienzo de Hoja Realista

**DocuSheet** es un procesador de textos para Android diseñado para trasladar la potencia, precisión y estética de un procesador de oficina de PC a la palma de tu mano, permitiéndote escribir directamente sobre un lienzo que simula fielmente una o múltiples hojas de papel físico en cascada continua.

---

## 🚀 Características Principales

### 1. Paginación Continua en Cascada (Múltiples Hojas de Papel)
- **Lienzo físico en cascada**: Las hojas flotan verticalmente sobre el escritorio con elevación y sombras tridimensionales independientes.
- **Saltos de página físicos interactivos**: Inserción de saltos de página que dividen el texto de forma natural en hojas sucesivas.
- **Regla graduada estilo PC**: Medición graduada con marcas de precisión en centímetros.
- **Marcas guía de márgenes (L-corners)**: Delimitación visual de esquinas de impresión y corte en cada hoja.
- **Encabezado y pie de página dinámicos**: Título en cabecera y numeración formal correlativa *«— Página X de Y —»*.
- **Separadores visuales de escritorio**: Indicador estético entre el fin de una página y el inicio de la siguiente.
- **Control de escala de fuente seguro**: Implementación de densidad controlada (`LocalDensity`, `fontScale = 1.0f`) para blindar el diseño contra distorsiones por ajustes de accesibilidad de Android.

### 2. Tipografías, Formatos Avanzados y Alineación Cuádruple
- **Alineación Cuádruple con Justificado Real (PC Engine)**:
  - Soporte tanto para **párrafos individuales** (`[align:left|center|right|justify]`) como para **todo el documento global** (Izquierda, Centrado, Derecha, Justificado Real).
  - Cálculo de espaciado inter-palabras mediante motor nativo en **C++20** (`compute_justified_spacing`), logrando una distribución armónica de texto idéntica a procesadores de escritorio.
- **Tipografía Dinámica Inline y Global**:
  - Posibilidad de cambiar la tipografía para fragmentos de texto en cualquier momento (`[font:serif]`, `[font:sans]`, `[font:mono]`, `[font:cursive]`) o alternar la familia global de la hoja (Serif, Sans-Serif, Monospace, Caligráfica).
- **Formatos de Texto Enriquecido de PC**:
  - *Subrayado*: `<u>texto</u>` o `__texto__`.
  - *Tachado*: `~~texto~~` o `<s>texto</s>`.
  - *Subíndices*: `<sub>fórmula</sub>` (ideal para química y notas).
  - *Superíndices*: `<sup>exponente</sup>` (ideal para potencias, citas bibliográficas y llamadas de pie de página).
  - *Énfasis clásico*: Negrita (`**texto**`) y Cursiva (`*texto*`).
- **Jerarquía Estructurada**:
  - *Título Principal (H1)*, *Subtítulo (H2)*, *Apartado (H3)*.
  - *Citas destacadas (`>`)*, *Listas con viñetas (`•`)*, *Listas numeradas (`1. 2.`)* y *Casillas de tareas (`[ ]`, `[x]`)*.

### 3. Selector Contextual de PC Propio y Modos de Edición Avanzada (Mover e Intercambiar)
- **Desactivación del Selector Nativo del Teléfono**:
  - Reemplazo total del menú contextual flotante impuesto por el fabricante del móvil (`LocalTextToolbar` con `DocuSheetDisabledSystemToolbar`).
  - Cero distracciones o ventanas emergentes del sistema operativo sobre la hoja de papel.
- **Barra de Selección Contextual Estilo PC (`DocuSheetPcSelectionBar`)**:
  - Se activa de forma automática e inmediata al seleccionar texto con el cursor sobre la hoja.
  - **Operaciones de Portapapeles de PC**: Copiar, Cortar, Pegar y Seleccionar Todo con interacción táctil optimizada (mínimo 48x48 dp).
  - **Paleta de Colores de Tinta**: Modificación instantánea del color de texto seleccionado con vista previa visual (Negro Carbón, Azul Pluma, Rojo Carmesí, Verde Esmeralda, Púrpura Real, Ámbar Dorado, Marrón Sepia, etc.) mediante etiquetas estructuradas `[color:#HEX]...[/color]`.
  - **Tipografía de Selección Dinámica**: Cambio veloz de familia tipográfica sobre el fragmento marcado (`Serif`, `Sans`, `Mono`, `Cursive`).
  - **Estilos Rápidos**: Negrita, Cursiva, Subrayado y Tachado directos con un solo toque.
- **Modo Intercambiar de PC (Swap / Transposición Atómica)**:
  - *Transposición Bloque A ⇄ Bloque B*: Fija un texto seleccionado como «Bloque A», permitiendo seleccionar cualquier otro tramo distante en la hoja («Bloque B») e intercambiar sus posiciones de manera atómica con un solo toque. Incluye banner persistente con previsualización y cancelación rápida.
  - *Intercambio Rápido de Párrafos (Swap ↑ / Swap ↓)*: Permite permutar el párrafo actual con el inmediatamente anterior o posterior preservando saltos de línea e indentación.
  - *Intercambio con Portapapeles*: Permite sustituir la selección actual por el contenido del portapapeles y cargar en memoria el texto reemplazado simultáneamente.
- **Modo Mover / Arrastrar de PC (Move / Drag & Drop)**:
  - *Mover al Inicio (Top)*: Traslada el fragmento seleccionado a la primera línea del documento recalculando la posición del cursor.
  - *Mover al Final (Bottom)*: Envía el fragmento seleccionado al final de la última hoja del documento.
  - *Motor Nativo en C++20 y Rust*: Operaciones de permutación procesadas a alta velocidad en `TextManipulator` (`docusheet_core.cpp`) y métodos `swap_ranges` / `move_range` en el `PieceTable` de Rust, con respaldo tolerante a fallos en Kotlin.

### 4. Inserción de Imágenes de Galería y Web con Ajuste de Hoja (Layout & Wrap)
- **Inserción desde Galería del Dispositivo (Android Photo Picker)**:
  - Selector nativo de imágenes sin necesidad de permisos invasivos (`ActivityResultContracts.PickVisualMedia`).
  - Previsualización en miniatura instantánea en el diálogo antes de insertar en el documento.
  - Compresión y optimización en segundo plano (`Dispatchers.IO`) a resolución equilibrada (JPEG 86% / máx. 1920 px) para un rendimiento ágil y bajo consumo de memoria.
  - Almacenamiento persistente en directorio interno seguro (`filesDir/doc_images`) para evitar que los permisos temporales de URIs del sistema expiren al reiniciar el teléfono.
  - Soporte continuo para URLs e imágenes web con carga asíncrona.
- **Modos de Ajuste Físico**:
  - *Ancho Completo (`full`)*: La imagen abarca el ancho imprimible de la hoja.
  - *Centrada (`center`)*: Bloque destacado con márgenes simétricos.
  - *Alineada a la Izquierda (`left`)* y *Alineada a la Derecha (`right`)*.
- **Integración con Coil & Motor de Rust**:
  - Carga eficiente asíncrona mediante Coil con soporte de caché LRU y crossfade.
  - El motor en Rust (`CascadePaginator`) computa la altura y peso de las imágenes insertadas para calcular saltos de página y paginación en cascada de forma exacta.
  - Marco con sombreado de papel, bordes sutiles y pie de figura opcional.

### 5. Gestión Inteligente de Memoria Caché y Almacenamiento
- **Control Activo de Recursos Móviles (`DocuSheetCacheManager`)**:
  - **Límites de Memoria RAM**: Tope estricto del 25% de la memoria de la aplicación para el almacenamiento en caché de imágenes decodificadas, con política LRU (Least Recently Used) y reciclaje de mapas de bits.
  - **Caché en Disco Acotada**: Límite de 50 MB para descargas y cachés de red de Coil.
  - **Poda Automática Programada**: Detección y limpieza de imágenes huérfanas y archivos temporales de exportación (PDF/Markdown/HTML/TXT) con más de 7 días de antigüedad.
  - **Centro de Control de Caché en `AboutScreen`**: Panel de métricas que informa el espacio ocupado por la caché y archivos locales, con botón táctil directo para «Liberar y Optimizar Caché» en cualquier momento.

### 6. Formatos Físicos de Papel y Paginación Automática por Límite de Palabras
- **Catálogo de Formatos Físicos de Hoja (`PageFormat`)**:
  - *A4 Estándar*: 210 × 297 mm (capacidad estándar: ~350 palabras).
  - *Carta / Letter*: 216 × 279 mm (capacidad estándar: ~300 palabras).
  - *Oficio / Legal*: 216 × 356 mm (capacidad estándar: ~450 palabras).
  - *Cuartilla / A5*: 148 × 210 mm (capacidad estándar: ~180 palabras).
  - *Personalizado*: Ajustable libremente según la necesidad del redactor.
- **Límite Dinámico de Palabras por Hoja**:
  - Selector táctil con control deslizante (Slider de 100 a 800 palabras) y botones de ajuste rápido (+/- 50 palabras) en la pantalla `DocumentSettingsScreen`.
  - Paginación automática ininterrumpida: al alcanzar el umbral de palabras configurado, DocuSheet segmenta el flujo de texto y genera automáticamente una nueva hoja subordinada (Página 2, 3...) en Cascada Continua sin interrumpir la escritura ni perder la posición del cursor.
  - Migración segura en base de datos Room de versión 2 a 3 (`MIGRATION_2_3`), preservando íntegros los documentos existentes.

### 7. Herramienta de Tablas y Cuadrículas Editoriales
- **Diálogo Táctil de Configuración e Inserción (`TableInsertDialog`)**:
  - Ajuste interactivo de columnas (1 a 6) y filas (1 a 10) con controles táctiles optimizados (mínimo 48x48 dp).
  - Interruptor para fila de encabezados diferenciada (con negrita y contraste visual).
  - Selector de 4 estilos visuales con vista previa en miniatura a escala en tiempo real:
    1. *Clásica*: Bordes completos y celdas delineadas.
    2. *Editorial*: Líneas horizontales prominentes sin cortes verticales (estilo libro y periódico).
    3. *Rayada*: Filas alternadas con sombreado cebra.
    4. *Compacta*: Densidad condensada para múltiples datos.
- **Renderizador Físico sobre la Hoja (`TableSheetBlock`)**:
  - Se adapta a la tipografía seleccionada en el documento, respetando el tamaño de fuente e interlineado.
  - Cálculo de distribución métrica de columnas mediante el núcleo nativo en **C++20** (`computeTableColumnWidthsSafe`).
  - Sintaxis de marcado estructurada (`[table:style]...[/table]`) y compatibilidad con tablas estándar en Markdown (`| col 1 | col 2 |`).

### 8. Sistema de Exportación Digital Multi-Formato (.PDF, .MD, .HTML y .TXT)
- **Exportación a PDF Digital (.pdf)**: Generación nativa en resolución vectorial formato A4 estándar (595x842 pt), con saltos de página limpios, encabezado, pie de página formal y respeto de la tipografía seleccionada.
- **Exportación a HTML Editorial Estructurado (.html)**: Creación de documentos web autosuficientes con hojas de estilo CSS integradas, diseñadas con maquetación de libro impreso (textura de papel, márgenes, tipografía cuidada, citas con bordes destacados y tablas estilizadas).
- **Exportación a Documento de Texto Plano (.txt)**: Archivo universal sin etiquetas de marcado para máxima compatibilidad con cualquier editor de texto o sistema externo.
- **Exportación a Markdown (.md)**: Archivo universal con cabecera de metadatos, fecha de exportación y estructura de párrafos.
- **Compartir Seguro mediante FileProvider**: Apertura y envío inmediato a cualquier aplicación del teléfono (Google Drive, WhatsApp, Adobe Reader, Correo o almacenamiento local).

### 9. Visor Nativo de PDF de Alta Fidelidad e Integración "Abrir Con" (Open With)
- **Integración con el Sistema Android ("Abrir Con")**:
  - Registro de filtros de intención (`ACTION_VIEW` y `ACTION_SEND`) en `AndroidManifest.xml` para el tipo MIME `application/pdf`.
  - Permite seleccionar DocuSheet desde administradores de archivos, WhatsApp, Gmail, navegadores web o almacenamiento externo para visualizar documentos PDF de inmediato.
- **Pantalla Dedicada del Visor (`PdfViewerScreen`)**:
  - Renderizado vectorial de alta resolución (factor de escala nítido) con `android.graphics.pdf.PdfRenderer` en hilos en segundo plano (`Dispatchers.IO`).
  - Representación física de hojas de papel idéntica a la estética de DocuSheet: sombras tridimensionales, bordes discretos e indicadores de página A4.
  - **Zoom táctil interactivo (Pinch-to-zoom)** de 75% hasta 350%, desplazamiento libre (pan) y botones de escala (+/-) rápidos con botón de reajuste de encuadre.
  - Barra de estado con contador de página actual en tiempo real y botón directo para compartir.
  - Apertura local de PDFs desde la biblioteca de documentos mediante el selector de archivos del sistema.

### 10. Métricas Detalladas y Gráficas de Productividad
- **Meta Diaria de Escritura**: Ajuste del objetivo diario (250, 500, 1000 palabras) con indicador de anillo circular animado y porcentaje en vivo.
- **Métricas Cuantitativas en Vivo**:
  - Palabras totales, caracteres almacenados y hojas creadas.
  - Conteo automático de párrafos y oraciones estimadas.
  - Tiempo de lectura estimado (a 200 palabras por minuto).
  - Tiempo de alocución oral / discurso (a 130 palabras por minuto).
- **Gráficas Visuales Interactivas**:
  - *Gráfica de distribución*: Barras comparativas de longitud de contenido entre los documentos creados.
  - *Gráfica semanal de ritmo*: Visualización de actividad y volumen de los 7 días de la semana.

### 11. Barra de Herramientas y Escritorio de Edición
- **Deshacer y Rehacer (Undo / Redo)** en tiempo real con pila de cambios en memoria.
- **Zoom ajustable**: Alterna entre 85%, 100% y 115% para una lectura y escritura cómodas.
- **Modos de visualización**: Alterna entre el **Modo Edición** (con cursor y teclado) y el **Modo Lectura** (vista de hoja limpia e inmersiva con renderizado tipográfico).
- **6 Acabados de Papel**: Blanco Clásico, Marfil Cálido, Rayado Cuaderno, Cuadriculado Milimetrado, Sepia Papiro y Carbón Noche.

---

## 🛠️ Stack Tecnológico Multi-Lenguaje y Compilación Nativa

- **Capa Visual y UI Táctil**: **Kotlin** con **Jetpack Compose** y **Material Design 3 (M3)**
- **Motor Central de Documentos**: **Rust (Edición 2021 / v1.75+)** (`rust-core`)
  - *Piece Table* y estructuras *Rope* para búfer de edición ilimitado sin pausas por Garbage Collection.
  - Métodos nativos de transposición atómica `swap_ranges` y desplazamiento `move_range` con punteros de índice continuos.
  - Paginación matemática y segmentación de hojas en cascada continua con peso de imágenes.
  - Compilación cruzada para las 4 ABIs de Android (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) generando `libdocusheet_rust.so`.
- **Motor Tipográfico y de Medición**: **C++20 (ISO C++20)** (`app/src/main/cpp`)
  - Medición milimétrica de caracteres y cálculo nativo de justificado (`compute_justified_spacing`).
  - Cálculo de ancho y distribución métrica de columnas para tablas editoriales (`compute_table_layout`).
  - Módulo `TextManipulator` de alto rendimiento para traslación e intercambio atómico de rangos de texto (`swap_ranges`, `move_range`, `swap_paragraph`).
  - Compilación mediante Android NDK (r27b+) y CMake 3.22.1 generando `libdocusheet_core.so` y enlazando `libdocusheet_rust.so`.
- **Automatización del Pipeline de Compilación**:
  - Tarea Gradle `buildRustCore` vinculada a `preBuild` que ejecuta la compilación de Rust antes del ensamblado de CMake.
  - Empaquetado automático de ambos binarios `.so` en el APK final para todas las arquitecturas de 32 y 64 bits.
- **Puente Interoperable**: **CXX** (Rust <-> C++20 de cero costo) y enlaces **JNI** (`NativeEngineBridge`).
- **Persistencia**: Room Database (v3 con `alignment`, `pageSize` y `wordsPerPage`, KSP + Kotlin Coroutines & Flow)
- **Gestión Inteligente de Memoria y Caché**: `DocuSheetCacheManager` (política LRU, tope RAM 25%, poda de huérfanos y compresión balanceada)
- **Procesamiento de Imágenes**: Android Photo Picker nativo (`PickVisualMedia`) + Coil Compose (`AsyncImage`)
- **Formatos de Hoja y Paginación**: Catálogo `PageFormat` (A4, Letter, Legal, A5, Custom) con segmentación dinámica por palabras
- **Exportación**: `android.graphics.pdf.PdfDocument` + `FileProvider`
- **Navegación**: Navigation Compose (Rutas desacopladas)
- **Asincronía**: Kotlin Coroutines (`Dispatchers.IO`)
- **Arquitecturas Compatibles**: `arm64-v8a` (64 bits), `armeabi-v7a` (32 bits), `x86_64`, `x86`.
- **Compatibilidad del Sistema**: Android 9.0+ (API 28 Pie o superior)

---

## 📱 Distribución e Instalación

La aplicación está preparada para ser empaquetada como APK universal directamente para instalación en dispositivos móviles o distribución en plataformas abiertas como **Uptodown** o tiendas de terceros, sin dependencias propietarias forzadas.
