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

3. **Exportación Digital Nativa (`DocumentExporter.kt`)**:
   - Soporte para generar documentos **PDF digital** en formato A4 estándar con encabezados, márgenes y pie de página, y archivos **Markdown (.md)** universales.
   - Se distribuyen mediante `FileProvider` sin exigir permisos invasivos de almacenamiento del sistema operativo.

4. **Métricas Detalladas y Gráficas de Productividad**:
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

---

## 🛡️ Reglas y Restricciones Estrictas

- **Derechos de autor**: No utilizar marcas registradas de terceros en nombres de clases, archivos o cadenas de texto. El proyecto se llama y se identifica como **DocuSheet**.
- **Canal de distribución**: El proyecto está preparado para distribución libre (APK independiente / Uptodown) y no debe incorporar servicios dependientes exclusivamente de los servicios de Google Play.
- **Explicación en código**: Todo archivo de código nuevo o editado debe contener comentarios y documentación KDoc clara en español explicando su propósito y funcionamiento.
- **Rendimiento móvil**: Priorizar el uso de `remember`, `derivedStateOf` y operaciones asíncronas para garantizar 60 FPS estables.
- **Preservación del Pipeline Nativo**: Nunca omitir o eliminar las configuraciones de `ndk`, `externalNativeBuild` ni las tareas de Cargo en `build.gradle.kts`.
