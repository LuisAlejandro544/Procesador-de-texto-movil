package com.example.util

/**
 * PageFormat: Define los formatos estándar de hoja física de papel en DocuSheet.
 *
 * Asocia a cada formato:
 * - Nombre legible y dimensiones en milímetros
 * - Capacidad estándar de palabras recomendada antes del salto automático a la siguiente hoja
 * - Dimensiones en puntos (DPI 72) para la exportación a PDF
 */
enum class PageFormat(
    val id: String,
    val displayName: String,
    val description: String,
    val defaultWordsLimit: Int,
    val dimensionsMm: String,
    val pdfWidthPt: Int,
    val pdfHeightPt: Int
) {
    A4(
        id = "A4",
        displayName = "A4 Estándar",
        description = "Estándar internacional europeo (210 × 297 mm)",
        defaultWordsLimit = 350,
        dimensionsMm = "210 × 297 mm",
        pdfWidthPt = 595,
        pdfHeightPt = 842
    ),
    LETTER(
        id = "LETTER",
        displayName = "Carta / Letter",
        description = "Estándar oficina americana (216 × 279 mm)",
        defaultWordsLimit = 300,
        dimensionsMm = "216 × 279 mm",
        pdfWidthPt = 612,
        pdfHeightPt = 792
    ),
    LEGAL(
        id = "LEGAL",
        displayName = "Oficio / Legal",
        description = "Hoja extendida para contratos y actas (216 × 356 mm)",
        defaultWordsLimit = 450,
        dimensionsMm = "216 × 356 mm",
        pdfWidthPt = 612,
        pdfHeightPt = 1008
    ),
    A5(
        id = "A5",
        displayName = "Cuartilla / A5",
        description = "Formato compacto de libreta o novela (148 × 210 mm)",
        defaultWordsLimit = 180,
        dimensionsMm = "148 × 210 mm",
        pdfWidthPt = 420,
        pdfHeightPt = 595
    ),
    CUSTOM(
        id = "CUSTOM",
        displayName = "Personalizado",
        description = "Capacidad y formato de palabras ajustable por el usuario",
        defaultWordsLimit = 350,
        dimensionsMm = "Ajustable",
        pdfWidthPt = 595,
        pdfHeightPt = 842
    );

    companion object {
        fun fromId(id: String): PageFormat {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: A4
        }
    }
}
