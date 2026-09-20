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

### 2. Tipografías y Formato Estructurado
- **Herramientas de Párrafo y Jerarquía**:
  - *Título Principal (H1)*: Encabezados mayores para títulos de obra o sección.
  - *Subtítulo (H2)*: Sub-párrafos y divisiones capitulares.
  - *Apartado (H3)*: Subsecciones temáticas.
  - *Citas destacadas (`>`)*: Bloques con borde lateral e inclinación reflexiva.
  - *Listas y Tareas*: Viñetas circulares (`•`), listas numeradas secuenciales (`1. 2.`) y casillas de tareas (`[ ]`, `[x]`).
  - *Énfasis tipográfico*: Negrita (`**texto**`) y Cursiva (`*texto*`).
- **4 Familias Tipográficas**:
  - *Serif*: Tipografía clásica y editorial para novelas y ensayos.
  - *Sans-Serif*: Tipografía limpia y moderna para lectura ágil.
  - *Monospace*: Tipografía de máquina de escribir o código.
  - *Caligráfica (Cursive)*: Estilo manuscrito elegante y personal.

### 3. Sistema de Exportación Digital (.PDF y .MD)
- **Exportación a PDF Digital (.pdf)**: Generación nativa en resolución vectorial formato A4 estándar (595x842 pt), con saltos de página limpios, encabezado, pie de página formal y respeto de la tipografía seleccionada.
- **Exportación a Markdown (.md)**: Archivo universal con cabecera de metadatos, fecha de exportación y estructura de párrafos.
- **Compartir Seguro mediante FileProvider**: Apertura y envío inmediato a cualquier aplicación del teléfono (Google Drive, WhatsApp, Adobe Reader, Correo o almacenamiento local).

### 4. Métricas Detalladas y Gráficas de Productividad
- **Meta Diaria de Escritura**: Ajuste del objetivo diario (250, 500, 1000 palabras) con indicador de anillo circular animado y porcentaje en vivo.
- **Métricas Cuantitativas en Vivo**:
  - Palabras totales, caracteres almacenados y hojas creadas.
  - Conteo automático de párrafos y oraciones estimadas.
  - Tiempo de lectura estimado (a 200 palabras por minuto).
  - Tiempo de alocución oral / discurso (a 130 palabras por minuto).
- **Gráficas Visuales Interactivas**:
  - *Gráfica de distribución*: Barras comparativas de longitud de contenido entre los documentos creados.
  - *Gráfica semanal de ritmo*: Visualización de actividad y volumen de los 7 días de la semana.

### 5. Barra de Herramientas y Escritorio de Edición
- **Deshacer y Rehacer (Undo / Redo)** en tiempo real con pila de cambios en memoria.
- **Zoom ajustable**: Alterna entre 85%, 100% y 115% para una lectura y escritura cómodas.
- **Modos de visualización**: Alterna entre el **Modo Edición** (con cursor y teclado) y el **Modo Lectura** (vista de hoja limpia e inmersiva con renderizado tipográfico).
- **6 Acabados de Papel**: Blanco Clásico, Marfil Cálido, Rayado Cuaderno, Cuadriculado Milimetrado, Sepia Papiro y Carbón Noche.

---

## 🛠️ Stack Tecnológico Multi-Lenguaje

- **Capa Visual y UI Táctil**: **Kotlin** con **Jetpack Compose** y **Material Design 3 (M3)**
- **Motor Central de Documentos**: **Rust (Edición 2021 / v1.75+)** (`rust-core`)
  - *Piece Table* y estructuras *Rope* para búfer de edición ilimitado sin pausas por Garbage Collection.
  - Paginación matemática y segmentación de hojas en cascada continua.
- **Motor Tipográfico y de Medición**: **C++20 (ISO C++20)** (`app/src/main/cpp`)
  - Medición milimétrica de caracteres y glifos tipográficos mediante CMake y Android NDK.
  - Diseñado para interoperar con librerías tipográficas estándar de la industria (HarfBuzz, FreeType).
- **Puente Interoperable**: **CXX** (Rust <-> C++20 de cero costo) y enlaces **JNI** (`NativeEngineBridge`).
- **Persistencia**: Room Database (KSP + Kotlin Coroutines & Flow)
- **Exportación**: `android.graphics.pdf.PdfDocument` + `FileProvider`
- **Navegación**: Navigation Compose (Rutas desacopladas)
- **Asincronía**: Kotlin Coroutines (`Dispatchers.IO`)
- **Arquitecturas Compatibles**: `arm64-v8a` (64 bits), `armeabi-v7a` (32 bits), `x86_64`, `x86`.
- **Compatibilidad del Sistema**: Android 7.0+ (API 24 o superior)

---

## 📱 Distribución e Instalación

La aplicación está preparada para ser empaquetada como APK universal directamente para instalación en dispositivos móviles o distribución en plataformas abiertas como **Uptodown** o tiendas de terceros, sin dependencias propietarias forzadas.
