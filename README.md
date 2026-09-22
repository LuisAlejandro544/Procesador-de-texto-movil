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

### 8. Sistema Universal de Interoperabilidad: Importación y Exportación Multi-Formato (.DOCX, .RTF, .TEX, .PDF, .MD, .HTML y .TXT)
- **Microsoft Word (.docx)**:
  - *Exportación*: Generación nativa con empaquetado OpenXML comprimido en ZIP (`ZipOutputStream`), generando una estructura válida compatible con Microsoft Word, Google Docs, Apple Pages y LibreOffice Writer con tablas, jerarquías y colores.
  - *Importación*: Extracción y lectura asíncrona de `word/document.xml` mediante `XmlPullParser`, traduciendo títulos, párrafos y tablas directamente a la hoja de papel de DocuSheet.
- **Texto Enriquecido Universal (.rtf / Rich Text Format)**:
  - *Exportación*: Documentos ofimáticos ligeros compatibles con WordPad, Word, TextEdit de Apple y cualquier editor, con cabecera RTF estándar, tabla de fuentes (`Calibri`, `Times New Roman`, `Courier New`), paleta cromática de 5 tintas, tablas formateadas con celdas y saltos de página físicos (`\page`).
  - *Importación*: Parser de secuencias de escape RTF con soporte de caracteres ANSI/Unicode, negritas (`\b`), cursivas (`\i`), listas con viñetas y filas de tablas.
- **LaTeX Académico y Científico (.tex)**:
  - *Exportación*: Código fuente LaTeX limpio y compilable para pdfLaTeX y XeLaTeX en formato de imprenta A4 (`article`), con preámbulo en español (`babel`), paquetes matemáticos (`amsmath`), tablas académicas (`booktabs`), cuadros decorativos (`tcolorbox`) para figuras y diagramas de nodos, listas formateadas y caracteres especiales debidamente escapados.
  - *Importación*: Parser estructurado que interpreta `\title`, `\section`, `\subsection`, `\subsubsection`, `\begin{tabular}`, `\item`, citas y estilos enriquecidos hacia el lienzo interactivo.
- **Exportación a PDF Digital (.pdf)**: Generación nativa en resolución vectorial formato A4 estándar (595x842 pt), con saltos de página limpios, encabezado, pie de página formal y respeto de la tipografía seleccionada.
- **Exportación a HTML Editorial Estructurado (.html)**: Creación de documentos web autosuficientes con hojas de estilo CSS integradas, diseñadas con maquetación de libro impreso (textura de papel, márgenes, tipografía cuidada, citas con bordes destacados y tablas estilizadas).
- **Exportación a Documento de Texto Plano (.txt)**: Archivo universal sin etiquetas de marcado para máxima compatibilidad con cualquier editor de texto o sistema externo.
- **Exportación e Importación Markdown (.md)**: Archivo universal con cabecera de metadatos, fecha de exportación y estructura de párrafos.
- **Importador Centralizado (`DocumentImporter`)**: Selector universal en la pantalla de inicio y en el diálogo de plantillas que detecta automáticamente extensiones y números mágicos de cabecera para abrir archivos externos sin fricción.
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

### 12. Buscador de PC, Radar de Redundancia (Análisis de Proximidad) y Diccionario de Sinónimos Offline
- **Buscador y Reemplazador Estilo PC (`DocuSheetSearchRadarBar`)**:
  - Panel flotante táctil con botones mínimos de 48x48 dp, búsqueda en vivo e integración con el cursor de la hoja física.
  - Contador de posición de ocurrencias en tiempo real (ejemplo: *«3 / 14»*).
  - Carrusel de navegación rápido con botones de salto anterior (`←`) y siguiente (`→`).
  - Modo expandible de **Reemplazar** individual y **Reemplazar Todo** con retroalimentación instantánea.
- **Detector de Redundancia y Radar de Estilo (Análisis de Proximidad)**:
  - Potenciado por **Apache Lucene** (`lucene-core` y `lucene-analyzers-common`) con tokenización profunda mediante `SpanishAnalyzer` y lematización avanzada mediante `SpanishLightStemmer`.
  - Mide la distancia en palabras entre apariciones de un mismo lexema:
    - *Distancia Crítica (<40 palabras)*: Advertencia destacada en rojo ante repetición excesiva en el mismo párrafo o párrafos adyacentes.
    - *Distancia Moderada (40 a 120 palabras)*: Advertencia preventiva en ámbar.
    - *Distancia Aceptable (>120 palabras)*: Distribución léxica sana.
  - Diagnóstico editorial con cálculo de densidad léxica y sugerencia de acción correctiva en español.
- **Diccionario Local de Sinónimos en un Solo Toque (100% Offline)**:
  - Base de datos local en Room (`SynonymEntity`, `SynonymDao`) precargada con términos y sinónimos en español clasificados por categoría gramatical.
  - Búsqueda dual: coincidencia exacta del término y búsqueda por raíz lematizada (stemming).
  - **Carrusel horizontal deslizable (`LazyRow`) de chips táctiles**: al tocar un chip de sinónimo, este sustituye inmediatamente la palabra en la hoja física preservando mayúsculas, actualizando el historial de deshacer/rehacer y recalculando el radar de redundancia en vivo.
- **Acceso Directo desde la Selección de Texto**:
  - Al seleccionar cualquier palabra en la hoja, la barra contextual de PC `DocuSheetPcSelectionBar` ofrece el botón directo *«Sinónimos/Radar»* para abrir el panel instantáneamente.

### 13. Sistema de Macros, Automatizaciones y Expansión Dinámica de Plantillas
- **Motor de Reemplazo de Alto Rendimiento en C++20**:
  - Sustitución instantánea de variables de plantilla mediante `expand_macro_template` en `docusheet_core.cpp` con `std::string_view` y asignación de memoria previa, con respaldo seguro en Kotlin (`MacroEngine.kt`).
- **Variables Dinámicas del Sistema y Documento**:
  - `{FECHA}`: Fecha formal en español (ejemplo: *21 de septiembre de 2026*).
  - `{FECHA_ISO}`: Fecha estándar internacional (ejemplo: *2026-09-21*).
  - `{HORA}`: Hora y minutos actuales (ejemplo: *16:45*).
  - `{TITULO}`: Título asignado a la hoja actual.
  - `{AUTOR}`: Nombre o firma del redactor responsable.
  - `{TOTAL_PALABRAS}`: Conteo de palabras exactas del documento al momento de la ejecución.
  - `{PAGINA_ACTUAL}`: Número de hoja física actual.
  - `{TOTAL_PAGINAS}`: Total de hojas físicas proyectadas del documento.
  - `{FORMATO_HOJA}`: Formato físico seleccionado (A4, Letter, Legal, A5, Custom).
  - `{CLIPBOARD}`: Texto copiado actualmente en el portapapeles del dispositivo.
  - `{SELECCION}`: Texto seleccionado por el usuario en la hoja al invocar la macro.
  - `{DISPARADOR}`: Palabra clave o atajo activado.
  - `{TABLA_2X3}` / `{TABLA_3X3}`: Tablas preformateadas listas para rellenar.
  - `{LISTA_TAREAS}`: Bloque de tareas formateadas con casillas interactivas `[ ]`.
  - `{ALEATORIO_ID}`: Código alfanumérico único aleatorio para radicación y folios (ejemplo: *DOC-94821*).
  - `{HASH_DOC}`: Firma criptográfica o identificador de integridad del texto.
- **Colección de Macros Pre-Construidas Listas para Usar**:
  - **Acta de Reunión (`:acta:`)**: Estructura ejecutiva formal con fecha, hora, asistentes, orden del día, acuerdos y compromisos.
  - **Carta Formal de Solicitud (`:carta:`)**: Modelo corporativo con fecha, encabezado formal, cuerpo petitorio y espacio de firma.
  - **Minuta Técnica (`:minuta:`)**: Ficha técnica de ingeniería con objetivos, metodología, arquitectura y checklist de control.
  - **Resumen Ejecutivo (`:resumen:`)**: Ficha de dirección con contexto, objetivos, métricas y conclusiones clave.
  - **Ficha de Proyecto (`:proyecto:`)**: Documento estructurado con alcance, responsables, hitos y tabla de balance.
  - **Lista de Tareas / Sprint (`:tareas:`)**: Matriz operativa con casillas `[ ]` categorizadas en tareas prioritarias, secundarias y de seguimiento.
  - **Cita Bibliográfica Formal (`:cita:`)**: Bloque de referencia bibliográfica con estilo editorial destacado.
- **Expansión Automática al Escribir (In-place Triggering)**:
  - Al escribir en la hoja el atajo de la macro (ejemplo: `:acta:` o `:carta:`) seguido de dos puntos, espacio o salto de línea, DocuSheet detecta el disparador y lo expande automáticamente en el cursor sin necesidad de abrir menús.
- **Panel Rápido Desplegable (`DocuSheetMacroBottomSheet`)**:
  - Accesible con un toque desde el botón *«Macros»* de la barra de herramientas del editor.
  - Pestañas por categoría (*Documentos*, *Editorial*, *Trabajo*, *Estructura*), chips de variables dinámicas, botón de ejecución inmediata y modal de creación de macros personalizadas.
- **Pantalla Completa de Gestión y Laboratorio de Macros (`MacroManagerScreen`)**:
  - Pantalla dedicada con pestaña de catálogo, laboratorio de prueba y evaluación de variables en tiempo real contra el documento activo, y guía visual de todas las variables dinámicas disponibles.
- **Persistencia en Room Database v5**:
  - Entidad `MacroEntity`, `MacroDao`, repositorio asíncrono `MacroRepository` y migración incremental `MIGRATION_4_5`.

### 14. Tipografía Avanzada de PC: Colores, Grosores y Efectos 3D
- **Diálogo Interactivo Táctil de Estilo (`TextStyle3dDialog`)**:
  - Panel flotante accesible desde la barra de formato del editor y desde la barra contextual de selección de PC.
  - Previsualización en vivo en tiempo real del fragmento o muestra estilizada.
- **Paleta Cromática de Tinta y Hexadecimal**:
  - Selección de colores clásicos de imprenta (Negro Carbón, Azul Marino, Rojo Borgoña, Verde Bosque, Amatista, Oro Viejo, Terracota, etc.) y campo de código hexadecimal `#RRGGBB` para libertad cromática total (`[color:#HEX]...[/color]`).
- **Control Gradual de Grosor Tipográfico (Font Weight)**:
  - Selector de 9 niveles métricos de peso: Fino (Thin 100), Extra Ligero (ExtraLight 200), Ligero (Light 300), Normal (Normal 400), Medio (Medium 500), Semi-Negrita (SemiBold 600), Negrita (Bold 700), Extra Negrita (ExtraBold 800) y Negro (Black 900) con sintaxis estructurada `[weight:grosor]...[/weight]`.
- **Relieve y Sombra Estereoscópica 3D**:
  - Interruptor táctil de efecto 3D que proyecta sombras con ángulo, desenfoque y profundidad calculada (`[3d:#sombra,#relieve]...[/3d]`).
  - Paleta dedicada para color de sombra estereoscópica y color de relieve frontal.
  - Renderizado directo en la hoja de papel física en `PaperRichVisualTransformation.kt` con `androidx.compose.ui.graphics.Shadow` y renderizado vectorial idéntico en `DocumentExporter.kt` para impresión y PDF.

### 15. Figuras Geométricas y Nodos de Diagrama de Flujo de PC
- **Diálogo Especializado de Inserción (`InsertShapeOrNodeDialog`)**:
  - Pestañas ergonómicas de acceso para **Figuras** y **Nodos**, con vista previa en miniatura a escala en tiempo real antes de insertar en la hoja.
- **Catálogo de Figuras Geométricas Vectoriales**:
  - Formas disponibles: *Rectángulo*, *Rectángulo redondeado*, *Círculo / Óvalo*, *Triángulo*, *Rombo / Diamante*, *Estrella de 5 puntas*, *Flecha a la Derecha*, *Flecha a la Izquierda* y *Llamada / Bocadillo de texto editorial*.
  - **Personalización de PC**:
    - Ajuste táctil independiente de **Ancho (W)** y **Alto (H)** en puntos (60 a 450 pt).
    - Alineación física sobre la hoja: **Izquierda**, **Centrado** o **Derecha**.
    - Color de fondo / relleno (Relleno blanco, ámbar suave, azul cielo, verde menta, pizarra, negro o transparente).
    - Color de borde y contorno con grosor métrico.
    - **Texto interior integrado**: Permite titular o anotar conceptos dentro de la propia figura geométrica.
    - Sintaxis en la hoja: `[shape:tipo,w=...,h=...,align=...,fill=...,stroke=...]Texto interior[/shape]`.
- **Diagramas de Nodos y Grafos de Flujo Secuenciales de PC**:
  - Herramienta rápida para crear mapas conceptuales, diagramas de procesos y pipelines lógicos directamente en el procesador.
  - Orientación flexible: **Horizontal** (cajas consecutivas enlazadas por flechas) o **Vertical** (secuencia vertical apilada).
  - Personalización de color de cajas de nodo, color de trazos y líneas de conexión.
  - Sintaxis limpia y legible: `[nodes:orientacion,color=...,line=...]Paso 1 -> Paso 2 -> Paso 3[/nodes]`.
- **Renderizado Físico y Exportación Vectorial**:
  - En pantalla: Renderizado con `Canvas` nativo de Compose integrado en el flujo de la hoja en `PaperSheet.kt` y `PaperBlockRenderer.kt`.
  - En PDF digital: Renderizado vectorial nativo con `android.graphics.Canvas`, `Path` de trazado geométrico y pinceles de alta resolución en `DocumentExporter.kt`, garantizando calidad de imprenta sin pérdida de nitidez.

---

## 🛠️ Stack Tecnológico Multi-Lenguaje y Compilación Nativa

- **Capa Visual y UI Táctil**: **Kotlin** con **Jetpack Compose** y **Material Design 3 (M3)**
- **Motor de Macros y Automatizaciones**: **C++20** (`expand_macro_template` con `std::string_view`) + **Kotlin** (`MacroEngine`, `MacroRepository`, `MacroManagerScreen`, `DocuSheetMacroBottomSheet`)
- **Motor de Análisis de Texto y Lingüística**: **Apache Lucene (v8.11.2)** (`lucene-core`, `lucene-analyzers-common`)
  - Tokenización formal en español (`SpanishAnalyzer`), lematización (`SpanishLightStemmer`) y análisis métrico de proximidad de lexemas.
- **Diccionario de Sinónimos Offline**: Base de datos local integrada en Room con repositorio asíncrono y precarga de seed léxico en español.
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
- **Persistencia**: Room Database (v5 con `documents`, `synonyms` y `macros`, KSP + Kotlin Coroutines & Flow)
- **Gestión Inteligente de Memoria y Caché**: `DocuSheetCacheManager` (política LRU, tope RAM 25%, poda de huérfanos y compresión balanceada)
- **Procesamiento de Imágenes**: Android Photo Picker nativo (`PickVisualMedia`) + Coil Compose (`AsyncImage`)
- **Formatos de Hoja y Paginación**: Catálogo `PageFormat` (A4, Letter, Legal, A5, Custom) con segmentación dinámica por palabras
- **Interoperabilidad de Documentos**: Motores nativos OpenXML Word (`DocxHandler`), Rich Text Format (`RtfHandler`), LaTeX Científico (`LatexHandler`), `DocumentImporter` y `DocumentExporter` con `FileProvider` y `PdfDocument`
- **Navegación**: Navigation Compose (Rutas desacopladas)
- **Asincronía**: Kotlin Coroutines (`Dispatchers.IO`)
- **Arquitecturas Compatibles**: `arm64-v8a` (64 bits), `armeabi-v7a` (32 bits), `x86_64`, `x86`.
- **Compatibilidad del Sistema**: Android 9.0+ (API 28 Pie o superior)

---

## 📱 Distribución e Instalación

La aplicación está preparada para ser empaquetada como APK universal directamente para instalación en dispositivos móviles o distribución en plataformas abiertas como **Uptodown** o tiendas de terceros, sin dependencias propietarias forzadas.
