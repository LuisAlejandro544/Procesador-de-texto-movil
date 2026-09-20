//! ==============================================================================
//! DocuSheet - Puente CXX Rust <-> C++20 (bridge.rs)
//! ==============================================================================
//!
//! Define la interfaz de comunicación de cero costo (*zero-cost abstractions*)
//! entre el núcleo en Rust y el subsistema tipográfico en C++20.

#[cxx::bridge(namespace = "docusheet::bridge")]
pub mod ffi {
    /// Dimensiones tipográficas compartidas entre Rust y C++20
    #[derive(Debug, Clone)]
    pub struct PageMetrics {
        pub width_pt: f32,
        pub height_pt: f32,
        pub margin_horizontal: f32,
        pub margin_vertical: f32,
    }

    extern "Rust" {
        /// Función exportada desde Rust hacia C++ para calcular páginas
        fn rust_paginate_text(content: &str, chars_per_page: usize) -> Vec<String>;

        /// Función exportada desde Rust hacia C++ para obtener versión
        fn rust_get_version() -> String;
    }
}

pub fn rust_paginate_text(content: &str, chars_per_page: usize) -> Vec<String> {
    let paginator = crate::pagination::CascadePaginator::new(chars_per_page);
    paginator.paginate(content)
}

pub fn rust_get_version() -> String {
    crate::get_rust_core_version().to_string()
}
