//! ==============================================================================
//! DocuSheet - Segmentador de Hojas en Cascada en Rust (pagination.rs)
//! ==============================================================================
//!
//! Algoritmo de partición determinista de páginas para el modo Cascada Continua.
//! Respeta saltos de página explícitos y realiza cortes inteligentes en límites
//! de párrafos para evitar cortes antiestéticos en medio de una oración.

pub struct CascadePaginator {
    chars_per_sheet_target: usize,
}

impl CascadePaginator {
    pub fn new(chars_per_sheet_target: usize) -> Self {
        Self {
            chars_per_sheet_target: if chars_per_sheet_target == 0 {
                1400
            } else {
                chars_per_sheet_target
            },
        }
    }

    /// Divide el texto de entrada en hojas individuales para renderizado en cascada.
    pub fn paginate(&self, content: &str) -> Vec<String> {
        if content.trim().is_empty() {
            return vec![String::new()];
        }

        let mut pages = Vec::new();

        // 1. Dividir primero por saltos de página manuales explícitos
        let raw_sections: Vec<&str> = content
            .split("\n[--- Salto de Página ---]\n")
            .collect();

        for section in raw_sections {
            if section.len() <= self.chars_per_sheet_target {
                pages.push(section.to_string());
            } else {
                // Segmentación inteligente por párrafos
                let paragraphs: Vec<&str> = section.split("\n\n").collect();
                let mut current_page = String::new();

                for p in paragraphs {
                    if !current_page.is_empty()
                        && (current_page.len() + p.len() > self.chars_per_sheet_target)
                    {
                        pages.push(current_page.trim_end().to_string());
                        current_page = String::new();
                    }

                    if !current_page.is_empty() {
                        current_page.push_str("\n\n");
                    }
                    current_page.push_str(p);
                }

                if !current_page.is_empty() {
                    pages.push(current_page);
                }
            }
        }

        if pages.is_empty() {
            vec![String::new()]
        } else {
            pages
        }
    }
}
