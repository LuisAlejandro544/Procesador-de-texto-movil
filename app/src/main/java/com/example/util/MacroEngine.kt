package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MacroVariableDefinition: Describe una variable dinámica disponible para construir macros.
 */
data class MacroVariableDefinition(
    val token: String,
    val name: String,
    val description: String,
    val example: String
)

/**
 * MacroExecutionContext: Proporciona los valores de entorno del documento actual
 * para resolver las variables al momento de ejecutar o expandir una macro.
 */
data class MacroExecutionContext(
    val documentTitle: String,
    val authorName: String = "Redactor DocuSheet",
    val pageFormat: String = "A4",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalWords: Int = 0,
    val selectedText: String = "",
    val clipboardText: String = "",
    val currentDate: Date = Date()
)

/**
 * MacroEngine: Motor de resolución y evaluación de variables dinámicas y macros.
 * 
 * Soporta variables contextuales calculadas en tiempo real para agilizar la redacción
 * táctil en dispositivos móviles sin necesidad de escribir estructuras repetitivas.
 */
object MacroEngine {

    /**
     * Catálogo completo de variables dinámicas admitidas en DocuSheet.
     */
    val AVAILABLE_VARIABLES: List<MacroVariableDefinition> = listOf(
        MacroVariableDefinition(
            token = "{FECHA}",
            name = "Fecha Formal",
            description = "Inserta la fecha actual en formato extendido en español.",
            example = "21 de septiembre de 2026"
        ),
        MacroVariableDefinition(
            token = "{FECHA_CORTA}",
            name = "Fecha Corta",
            description = "Inserta la fecha actual en formato numérico DD/MM/AAAA.",
            example = "21/09/2026"
        ),
        MacroVariableDefinition(
            token = "{HORA}",
            name = "Hora Actual",
            description = "Inserta la hora y minutos del sistema en formato 24h.",
            example = "16:45"
        ),
        MacroVariableDefinition(
            token = "{TITULO}",
            name = "Título del Documento",
            description = "Nombre o título asignado a la hoja activa.",
            example = "Mi Primera Hoja"
        ),
        MacroVariableDefinition(
            token = "{AUTOR}",
            name = "Nombre del Autor",
            description = "Nombre del redactor asignado al documento o usuario.",
            example = "Redactor DocuSheet"
        ),
        MacroVariableDefinition(
            token = "{FORMATO_HOJA}",
            name = "Tamaño de Papel",
            description = "Formato físico de hoja configurado (A4, Carta, Legal, etc.).",
            example = "A4"
        ),
        MacroVariableDefinition(
            token = "{PAGINA}",
            name = "Página Actual",
            description = "Número correlativo de página en el que se ubica el cursor.",
            example = "1"
        ),
        MacroVariableDefinition(
            token = "{TOTAL_PAGINAS}",
            name = "Total de Hojas",
            description = "Cantidad total estimada de hojas del documento actual.",
            example = "3"
        ),
        MacroVariableDefinition(
            token = "{TOTAL_PALABRAS}",
            name = "Conteo de Palabras",
            description = "Total de palabras acumuladas en la redacción.",
            example = "450"
        ),
        MacroVariableDefinition(
            token = "{SELECCION}",
            name = "Texto Seleccionado",
            description = "Inserta el fragmento de texto marcado con el cursor (macro envolvente).",
            example = "[Texto previamente seleccionado]"
        ),
        MacroVariableDefinition(
            token = "{PORTAPAPELES}",
            name = "Texto del Portapapeles",
            description = "Inserta el contenido de texto copiado en el portapapeles del teléfono.",
            example = "[Contenido copiado]"
        ),
        MacroVariableDefinition(
            token = "{SALTO_PAGINA}",
            name = "Salto de Hoja",
            description = "Inserta la directiva de salto de página físico de DocuSheet.",
            example = "[--- Salto de Página ---]"
        )
    )

    /**
     * Resuelve y reemplaza todas las variables dinámicas dentro de una plantilla de texto.
     */
    fun evaluateTemplate(template: String, context: MacroExecutionContext): String {
        val localeSpanish = Locale("es", "ES")
        val formalDateFormat = SimpleDateFormat("d 'de' MMMM 'de' yyyy", localeSpanish)
        val shortDateFormat = SimpleDateFormat("dd/MM/yyyy", localeSpanish)
        val timeFormat = SimpleDateFormat("HH:mm", localeSpanish)

        val dateStr = formalDateFormat.format(context.currentDate)
        val shortDateStr = shortDateFormat.format(context.currentDate)
        val timeStr = timeFormat.format(context.currentDate)

        val titleStr = if (context.documentTitle.isNotBlank()) context.documentTitle else "Documento Sin Título"
        val authorStr = if (context.authorName.isNotBlank()) context.authorName else "Redactor DocuSheet"
        val pageStr = context.currentPage.toString()
        val totalPagesStr = context.totalPages.toString()
        val totalWordsStr = context.totalWords.toString()
        val formatStr = context.pageFormat
        val selectionStr = if (context.selectedText.isNotBlank()) context.selectedText else "[Texto seleccionado]"
        val clipboardStr = if (context.clipboardText.isNotBlank()) context.clipboardText else ""
        val pageBreakStr = "\n\n[--- Salto de Página ---]\n\n"

        // Lista ordenada de reemplazos
        val replacements = listOf(
            "{FECHA}" to dateStr,
            "{FECHA_CORTA}" to shortDateStr,
            "{HORA}" to timeStr,
            "{TITULO}" to titleStr,
            "{AUTOR}" to authorStr,
            "{FORMATO_HOJA}" to formatStr,
            "{PAGINA}" to pageStr,
            "{TOTAL_PAGINAS}" to totalPagesStr,
            "{TOTAL_PALABRAS}" to totalWordsStr,
            "{SELECCION}" to selectionStr,
            "{PORTAPAPELES}" to clipboardStr,
            "{SALTO_PAGINA}" to pageBreakStr
        )

        // Intento de expansión de alto rendimiento vía puente nativo si está disponible
        return if (NativeEngineBridge.isAvailable()) {
            try {
                val keys = replacements.map { it.first }.toTypedArray()
                val values = replacements.map { it.second }.toTypedArray()
                NativeEngineBridge.expandMacroTemplateSafe(template, keys, values)
            } catch (_: Throwable) {
                fallbackEvaluate(template, replacements)
            }
        } else {
            fallbackEvaluate(template, replacements)
        }
    }

    private fun fallbackEvaluate(template: String, replacements: List<Pair<String, String>>): String {
        var result = template
        for ((token, value) in replacements) {
            result = result.replace(token, value)
        }
        return result
    }

    /**
     * Examina el fragmento de texto inmediatamente anterior al cursor para detectar
     * si termina con un trigger de macro (ejemplo: ":acta:", ":carta:").
     * 
     * Retorna el trigger detectado y su longitud si existe coincidencia.
     */
    fun detectTriggerBeforeCursor(textBeforeCursor: String): Pair<String, Int>? {
        if (textBeforeCursor.length < 3) return null
        val lastColon = textBeforeCursor.lastIndexOf(':')
        if (lastColon < 0) return null

        val secondToLastColon = textBeforeCursor.lastIndexOf(':', lastColon - 1)
        if (secondToLastColon < 0) return null

        val candidate = textBeforeCursor.substring(secondToLastColon, lastColon + 1)
        if (candidate.matches(Regex(":[a-zA-Z0-9_-]+:"))) {
            return Pair(candidate, candidate.length)
        }
        return null
    }
}
