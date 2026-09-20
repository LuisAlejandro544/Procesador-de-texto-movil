//! ==============================================================================
//! DocuSheet - Estructura Piece Table en Rust (piece_table.rs)
//! ==============================================================================
//!
//! Implementación de la estructura Piece Table utilizada por procesadores de
//! texto de escritorio como Microsoft Word o VS Code.
//!
//! Principio de funcionamiento:
//! - Mantiene un buffer inmutable con el texto original (`original_buffer`).
//! - Mantiene un buffer en el que solo se anexan las nuevas escrituras (`add_buffer`).
//! - Mantiene una secuencia de descriptores (`pieces`) que apuntan a tramos de
//!   cualquiera de los dos buffers. De esta forma, insertar o borrar texto en
//!   un documento de 1.000 páginas solo altera pequeños descriptores (O(1) o O(log N)),
//!   sin mover megabytes de memoria en el teléfono.

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BufferType {
    Original,
    Add,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Piece {
    pub buffer_type: BufferType,
    pub start: usize,
    pub length: usize,
}

pub struct PieceTable {
    original_buffer: String,
    add_buffer: String,
    pieces: Vec<Piece>,
}

impl PieceTable {
    /// Inicializa una nueva Piece Table a partir de un texto base.
    pub fn new(initial_text: &str) -> Self {
        let length = initial_text.len();
        let pieces = if length > 0 {
            vec![Piece {
                buffer_type: BufferType::Original,
                start: 0,
                length,
            }]
        } else {
            Vec::new()
        };

        Self {
            original_buffer: initial_text.to_string(),
            add_buffer: String::new(),
            pieces,
        }
    }

    /// Anexa texto al final del documento de forma instantánea.
    pub fn append(&mut self, text: &str) {
        if text.is_empty() {
            return;
        }

        let start = self.add_buffer.len();
        let length = text.len();
        self.add_buffer.push_str(text);

        self.pieces.push(Piece {
            buffer_type: BufferType::Add,
            start,
            length,
        });
    }

    /// Reconstruye el texto completo consolidado.
    pub fn get_text(&self) -> String {
        let total_capacity: usize = self.pieces.iter().map(|p| p.length).sum();
        let mut result = String::with_capacity(total_capacity);

        for piece in &self.pieces {
            let slice = match piece.buffer_type {
                BufferType::Original => {
                    &self.original_buffer[piece.start..piece.start + piece.length]
                }
                BufferType::Add => {
                    &self.add_buffer[piece.start..piece.start + piece.length]
                }
            };
            result.push_str(slice);
        }

        result
    }

    /// Calcula la longitud total del documento en caracteres UTF-8.
    pub fn len(&self) -> usize {
        self.pieces.iter().map(|p| p.length).sum()
    }

    pub fn is_empty(&self) -> bool {
        self.pieces.is_empty()
    }
}
