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
│   │   ├── CMakeLists.txt               # Configuración CMake estándar C++20 estricto
│   │   ├── docusheet_core.hpp           # Cabecera C++20 con conceptos, métricas de hoja y clases
│   │   └── docusheet_core.cpp           # Implementación del motor tipográfico y llamadas JNI
│   │
│   └── src/main/java/com/example/
│       ├── MainActivity.kt              # Punto de entrada de la app, activa Edge-to-Edge y carga NavGraph
│       │
│       ├── data/                        # Capa de Persistencia Local (Room / SQLite)
│       │   ├── DocumentEntity.kt        # Entidad Room que modela la hoja de texto y sus atributos
│       │   ├── DocumentDao.kt           # Interfaz DAO con consultas SQL reactivas (Flow) y operaciones CRUD
│       │   ├── AppDatabase.kt           # Base de datos Room con migración y precarga de documento inicial
│       │   └── DocumentRepository.kt    # Repositorio que aísla las operaciones de base de datos en Dispatchers.IO
│       │
│       ├── ui/
│       │   ├── DocumentViewModel.kt     # Gestor de estado: historial deshacer/rehacer, autoguardado, formato y métricas
│       │   │
│       │   ├── components/              # Componentes visuales reutilizables
│       │   │   └── PaperSheet.kt        # Lienzo de hoja de papel, soporte de Cascada Continua (Múltiples Hojas), reglas y guías
│       │   │
│       │   ├── navigation/              # Capa de Navegación
│       │   │   └── NavGraph.kt          # Grafo central con rutas: documents, editor, settings, about
│       │   │
│       │   ├── screens/                 # Pantallas completas de la aplicación
│       │   │   ├── DocumentListScreen.kt# Biblioteca de documentos, miniaturas de hojas y plantillas
│       │   │   ├── DocumentEditorScreen.kt # Pantalla del editor con barra de herramientas, exportación y cascada
│       │   │   ├── DocumentSettingsScreen.kt # Ajustes de papel (texturas, fuentes Serif/Sans/Mono/Cursive)
│       │   │   └── AboutScreen.kt       # Centro de Métricas Detalladas, anillo de progreso y Gráficas
│       │   │
│       │   └── theme/                   # Sistema de Diseño y Tokens
│       │       ├── Color.kt             # Paleta de colores M3
│       │       ├── Theme.kt             # Configuración del tema y límite de escala tipográfica fija (1.0f)
│       │       └── Type.kt              # Jerarquía tipográfica base
│       │
│       └── util/
│           ├── DocumentExporter.kt      # Generación de archivos PDF multipágina (A4) y Markdown (.md)
│           └── NativeEngineBridge.kt    # Puente seguro de carga JNI para 'docusheet_core'
│
└── res/
    ├── xml/
    │   └── file_paths.xml               # Rutas autorizadas para FileProvider para compartir PDF y MD
    ├── drawable/                        # Recursos gráficos vectoriales (ic_doc_logo, launcher)
    └── values/strings.xml               # Textos de la aplicación en español
```

---

## 🗄️ Modelo de Datos: `DocumentEntity`

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
| `createdAt` | `Long` | Timestamp de creación en milisegundos |
| `updatedAt` | `Long` | Timestamp de última modificación |

---

## 🔒 Protección Tipográfica del Sistema

En `Theme.kt`, la aplicación encapsula el árbol de componentes dentro de un `CompositionLocalProvider` que suministra una instancia de `Density` con `fontScale = 1.0f`. Esto impide que los ajustes de accesibilidad de fuente del sistema operativo Android deformen la proporción calculada de la hoja, la regla o las barras de herramientas.
