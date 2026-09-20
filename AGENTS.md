# Directrices para Agentes de Código (AGENTS.md) 🤖📋

Este documento define las reglas de comportamiento, convenciones de ingeniería y directrices operativas que deben seguir todos los agentes de programación que colaboren en el desarrollo de **DocuSheet**.

---

## 👤 Perfil del Usuario y Entorno de Trabajo

- **Dispositivo**: El usuario opera y prueba la aplicación directamente desde su **teléfono móvil** (sin ordenador de escritorio). Las interfaces deben estar completamente optimizadas para interacción táctil (mínimo 48x48 dp en elementos interactivos).
- **Preferencia de Diseño**: Al usuario **no le gusta el minimalismo extremo**. Prefiere herramientas visuales, métricas detalladas, botones accesibles, barras de herramientas especializadas y pantallas dedicadas bien estructuradas en lugar de interfaces excesivamente vacías o simplistas.
- **Distribución del Software**: La aplicación se compilará para ser distribuida como APK en tiendas alternativas como **Uptodown** o instalación directa, evitando ataduras exclusivas a servicios cerrados o dependencias invasivas de Google Play.
- **Idioma del Proyecto**: La comunicación con el usuario, la documentación de los archivos y los comentarios dentro del código fuente deben redactarse en **español**.

---

## 🛠️ Reglas Obligatorias de Desarrollo

### 1. Modularidad y Separación de Pantallas
- No amontonar todas las funciones en una sola pantalla. Se debe mantener el flujo modular a través de `DocuSheetNavGraph` y pantallas dedicadas:
  - `DocumentListScreen`: Explorador, biblioteca de hojas y miniaturas.
  - `DocumentEditorScreen`: Área de trabajo con la hoja de papel física, barra de formato y barra de estado de PC.
  - `DocumentSettingsScreen`: Personalización física del papel, acabados y familias tipográficas.
  - `AboutScreen`: Centro de métricas cuantitativas, objetivo diario y gráficas de productividad.

### 2. Manejo de Hilos y Rendimiento
- **Prohibido bloquear el hilo principal (Main Thread)** con operaciones de lectura, escritura, generación de PDF o cómputos pesados de base de datos.
- Toda operación con `AppDatabase` o `DocumentDao` debe ejecutarse mediante Kotlin Coroutines en `Dispatchers.IO`.
- Usar `collectAsStateWithLifecycle` en Jetpack Compose para recolectar flujos reactivos de datos.

### 3. Integridad de la Hoja de Papel y Cascada Continua
- La hoja de papel (`PaperSheet.kt`) es el activo visual insignia de la aplicación.
- En **Modo Cascada Continua**, las hojas deben renderizarse con apariencia tridimensional independiente, manteniendo reglas graduadas, delimitadores de márgenes y pies de página correlativos (*«— Página X de Y —»*).
- No retirar la protección de escala de fuente (`fontScale = 1.0f`) implementada en `Theme.kt`, ya que previene distorsiones provocadas por configuraciones de tamaño de letra extremo en el sistema operativo del teléfono.

### 4. Exportación Segura de Archivos
- Las exportaciones a **PDF digital** y **Markdown (.md)** deben realizarse mediante `DocumentExporter` utilizando `FileProvider` con `content://` URIs seguros, respetando las rutas autorizadas en `res/xml/file_paths.xml`.

### 5. Documentación y Claridad del Código
- Cada archivo de código en Kotlin, C++ o Rust debe incluir un encabezado KDoc / doc comments y explicaciones en español que aclaren de forma transparente su propósito, su lógica y sus componentes principales.

### 6. Cimientos Nativos e Integración de C++20 y Rust
- **Pipeline de Compilación Obligatorio**: Nunca eliminar ni comentar las directivas de compilación nativa en `app/build.gradle.kts` (`ndk`, `externalNativeBuild`, `buildRustCore`).
- **Soporte de Arquitecturas**: Garantizar siempre que la configuración admita tanto procesadores modernos de 64 bits (`arm64-v8a`, `x86_64`) como procesadores de 32 bits (`armeabi-v7a`, `x86`).
- **Control de Versiones y Limpieza**: Mantener actualizado el archivo `.gitignore` para impedir que se suban carpetas temporales de compilación nativa (`.cxx/`, `.externalNativeBuild/`, `target/`, `CMakeFiles/`, `.ninja`, librerías binarias `.so` intermedias o ejecutables).

### 7. Verificación de Compilación
- Cada vez que se realicen cambios en el código, se debe verificar la compilación exitosa utilizando la herramienta de compilación (`compile_applet`) antes de dar por finalizada la tarea.
