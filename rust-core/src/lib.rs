//! ==============================================================================
//! DocuSheet - Núcleo Central en Rust (lib.rs)
//! ==============================================================================
//!
//! Este módulo contiene la lógica de procesamiento de documentos y estructuras
//! de datos optimizadas para evitar pausas por Garbage Collection en Android:
//!
//! - **Piece Table**: Almacena ediciones infinitas sin recolocar memoria masiva.
//! - **Pagination**: Segmentación matemática de hojas en cascada continua.
//! - **Bridge**: Enlaces CXX con el subsistema tipográfico en C++20.

pub mod piece_table;
pub mod pagination;
pub mod bridge;

pub use piece_table::PieceTable;
pub use pagination::CascadePaginator;

/// Retorna la versión actual del motor central de Rust.
pub fn get_rust_core_version() -> &'static str {
    "DocuSheet Rust Core Engine v0.1.0 (Rust 2021 Edition)"
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_core_version() {
        assert!(get_rust_core_version().contains("Rust 2021 Edition"));
    }
}
