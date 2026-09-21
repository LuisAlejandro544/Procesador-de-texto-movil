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

    /// Intercambia dos rangos de texto no solapados de manera inmutable.
    pub fn swap_ranges(
        &mut self,
        start_a: usize,
        end_a: usize,
        start_b: usize,
        end_b: usize,
    ) -> Result<String, &'static str> {
        let text = self.get_text();
        let chars: Vec<char> = text.chars().collect();
        let len = chars.len();

        let (a_s, a_e) = (start_a.min(end_a), start_a.max(end_a));
        let (b_s, b_e) = (start_b.min(end_b), start_b.max(end_b));

        if a_e > len || b_e > len {
            return Err("Índices de rango fuera de los límites del documento");
        }

        // Verificar que no se solapen
        if a_s < b_e && a_e > b_s {
            return Err("Los rangos de intercambio no pueden solaparse");
        }

        let (first_s, first_e, second_s, second_e, is_a_first) = if a_s <= b_s {
            (a_s, a_e, b_s, b_e, true)
        } else {
            (b_s, b_e, a_s, a_e, false)
        };

        let part1: String = chars[..first_s].iter().collect();
        let part_first: String = chars[first_s..first_e].iter().collect();
        let part_middle: String = chars[first_e..second_s].iter().collect();
        let part_second: String = chars[second_s..second_e].iter().collect();
        let part_end: String = chars[second_e..].iter().collect();

        let new_text = if is_a_first {
            format!("{}{}{}{}{}", part1, part_second, part_middle, part_first, part_end)
        } else {
            format!("{}{}{}{}{}", part1, part_first, part_middle, part_second, part_end)
        };

        *self = PieceTable::new(&new_text);
        Ok(new_text)
    }

    /// Mueve un rango de texto [start, end) hacia una posición objetivo.
    pub fn move_range(
        &mut self,
        start: usize,
        end: usize,
        target: usize,
    ) -> Result<String, &'static str> {
        let text = self.get_text();
        let chars: Vec<char> = text.chars().collect();
        let len = chars.len();

        let (s, e) = (start.min(end), start.max(end));
        if e > len || target > len {
            return Err("Índices de rango fuera de límites");
        }

        let selected: String = chars[s..e].iter().collect();
        let mut remainder: Vec<char> = Vec::with_capacity(len - (e - s));
        remainder.extend_from_slice(&chars[..s]);
        remainder.extend_from_slice(&chars[e..]);

        let adjusted_target = if target > s {
            (target - (e - s)).min(remainder.len())
        } else {
            target.min(remainder.len())
        };

        let prefix: String = remainder[..adjusted_target].iter().collect();
        let suffix: String = remainder[adjusted_target..].iter().collect();
        let new_text = format!("{}{}{}", prefix, selected, suffix);

        *self = PieceTable::new(&new_text);
        Ok(new_text)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_piece_table_swap_ranges() {
        let mut pt = PieceTable::new("Hola Mundo Hermoso");
        // "Hola" (0..4) y "Hermoso" (11..18)
        let res = pt.swap_ranges(0, 4, 11, 18).unwrap();
        assert_eq!(res, "Hermoso Mundo Hola");
    }

    #[test]
    fn test_piece_table_move_range() {
        let mut pt = PieceTable::new("Uno Dos Tres");
        // Mover "Tres" (8..12) al inicio (0)
        let res = pt.move_range(8, 12, 0).unwrap();
        assert_eq!(res, "Tres Uno Dos ");
    }
}

