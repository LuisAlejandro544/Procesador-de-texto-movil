package com.example.ui.components.paper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Transformación visual en vivo para el editor de hojas DocuSheet.
 * Permite atenuar las etiquetas de sintaxis (como [font:...], [align:...], [color:...], **, etc.)
 * y aplicar en tiempo real la tipografía cursiva, negrita, color o estilo en el texto interior,
 * manteniendo el mapeo 1:1 de caracteres para que el cursor y la selección táctil no salten.
 */
class DocuSheetRichVisualTransformation(private val baseColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val builder = AnnotatedString.Builder(raw)
        val tagDimColor = baseColor.copy(alpha = 0.38f)

        // 1. Tipografías: [font:serif], [font:sans], [font:mono], [font:cursive]
        val fontRegex = Regex("""\[font:(serif|sans|mono|cursive)\](.*?)\[/font\]""", RegexOption.IGNORE_CASE)
        for (m in fontRegex.findAll(raw)) {
            val fullRange = m.range
            val type = m.groupValues[1].lowercase()
            val contentStart = m.groups[2]!!.range.first
            val contentEnd = m.groups[2]!!.range.last + 1

            builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), fullRange.first, contentStart)
            builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), contentEnd, fullRange.last + 1)

            val fam = when (type) {
                "sans" -> FontFamily.SansSerif
                "mono" -> FontFamily.Monospace
                "cursive" -> FontFamily.Cursive
                else -> FontFamily.Serif
            }
            builder.addStyle(SpanStyle(fontFamily = fam), contentStart, contentEnd)
        }

        // 2. Colores: [color:#123456]...[/color] o [color:blue]...[/color]
        val colorRegex = Regex("""\[color:(#[0-9a-fA-F]{3,8}|[a-zA-Z]+)\](.*?)\[/color\]""", RegexOption.IGNORE_CASE)
        for (m in colorRegex.findAll(raw)) {
            val fullRange = m.range
            val colVal = m.groupValues[1]
            val contentStart = m.groups[2]!!.range.first
            val contentEnd = m.groups[2]!!.range.last + 1

            builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), fullRange.first, contentStart)
            builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), contentEnd, fullRange.last + 1)

            try {
                val hex = when (colVal.lowercase()) {
                    "black", "negro" -> "#1E293B"
                    "white", "blanco" -> "#FFFFFF"
                    "blue", "azul" -> "#2563EB"
                    "red", "rojo" -> "#DC2626"
                    "green", "verde" -> "#16A34A"
                    "purple", "morado" -> "#9333EA"
                    "yellow", "amarillo", "amber" -> "#D97706"
                    "cyan", "teal" -> "#0D9488"
                    "pink", "rosa" -> "#DB2777"
                    "orange", "naranja" -> "#EA580C"
                    "sepia", "brown", "marron", "café" -> "#78350F"
                    "gray", "grey", "gris" -> "#475569"
                    "gold", "dorado" -> "#B45309"
                    else -> if (colVal.startsWith("#")) colVal else "#$colVal"
                }
                val parsedColor = Color(android.graphics.Color.parseColor(hex))
                builder.addStyle(SpanStyle(color = parsedColor), contentStart, contentEnd)
            } catch (_: Exception) {}
        }

        // 3. Grosor personalizado: [weight:light|normal|medium|semibold|bold|black|100..900]...[/weight]
        val weightRegex = Regex("""\[weight:(thin|extralight|light|normal|regular|medium|semibold|bold|extrabold|black|\d{3})\](.*?)\[/weight\]""", RegexOption.IGNORE_CASE)
        for (m in weightRegex.findAll(raw)) {
            val fullRange = m.range
            val weightStr = m.groupValues[1].lowercase()
            val contentStart = m.groups[2]!!.range.first
            val contentEnd = m.groups[2]!!.range.last + 1

            builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), fullRange.first, contentStart)
            builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), contentEnd, fullRange.last + 1)

            val resolvedWeight = parseFontWeightSafe(weightStr)
            builder.addStyle(SpanStyle(fontWeight = resolvedWeight), contentStart, contentEnd)
        }

        // 4. Efecto y Color 3D: [3d:#shadowColor,#frontColor]...[/3d] o [3d:#shadowColor]...[/3d] o [3d]...[/3d]
        val threeDRegex = Regex("""\[3d(?::([#0-9a-zA-Z]+))?(?:,([#0-9a-zA-Z]+))?\](.*?)\[/3d\]""", RegexOption.IGNORE_CASE)
        for (m in threeDRegex.findAll(raw)) {
            val fullRange = m.range
            val shadowParam = m.groupValues[1].trim()
            val frontParam = m.groupValues[2].trim()
            val contentStart = m.groups[3]!!.range.first
            val contentEnd = m.groups[3]!!.range.last + 1

            builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), fullRange.first, contentStart)
            builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), contentEnd, fullRange.last + 1)

            val shadowCol = if (shadowParam.isNotEmpty()) parseColorSafe(shadowParam, Color(0xFF1E293B)) else Color(0xFF0F172A)
            val frontCol = if (frontParam.isNotEmpty()) parseColorSafe(frontParam, Color.Unspecified) else Color.Unspecified

            builder.addStyle(
                SpanStyle(
                    color = if (frontCol != Color.Unspecified) frontCol else Color.Unspecified,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = shadowCol.copy(alpha = 0.88f),
                        offset = Offset(3.5f, 3.5f),
                        blurRadius = 1.8f
                    )
                ),
                contentStart,
                contentEnd
            )
        }

        // 5. Alineación en línea: [align:center]...[/align]
        val alignRegex = Regex("""\[align:(left|center|right|justify)\](.*?)\[/align\]""", RegexOption.IGNORE_CASE)
        for (m in alignRegex.findAll(raw)) {
            val fullRange = m.range
            val contentStart = m.groups[2]!!.range.first
            val contentEnd = m.groups[2]!!.range.last + 1
            builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), fullRange.first, contentStart)
            builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), contentEnd, fullRange.last + 1)
        }

        // 6. Negritas: **texto**
        val boldRegex = Regex("""\*\*(.*?)\*\*""")
        for (m in boldRegex.findAll(raw)) {
            val contentStart = m.groups[1]!!.range.first
            val contentEnd = m.groups[1]!!.range.last + 1
            builder.addStyle(SpanStyle(color = tagDimColor), m.range.first, contentStart)
            builder.addStyle(SpanStyle(color = tagDimColor), contentEnd, m.range.last + 1)
            builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), contentStart, contentEnd)
        }

        // 7. Cursivas: *texto*
        val italicRegex = Regex("""(?<!\*)\*([^*]+)\*(?!\*)""")
        for (m in italicRegex.findAll(raw)) {
            val contentStart = m.groups[1]!!.range.first
            val contentEnd = m.groups[1]!!.range.last + 1
            builder.addStyle(SpanStyle(color = tagDimColor), m.range.first, contentStart)
            builder.addStyle(SpanStyle(color = tagDimColor), contentEnd, m.range.last + 1)
            builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), contentStart, contentEnd)
        }

        // 8. Subrayado: <u>texto</u> o __texto__
        val underlineRegex = Regex("""(<u>(.*?)</u>)|(__(.*?)__)""")
        for (m in underlineRegex.findAll(raw)) {
            val inner = m.groups[2] ?: m.groups[4]
            inner?.let {
                builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), m.range.first, it.range.first)
                builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), it.range.last + 1, m.range.last + 1)
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), it.range.first, it.range.last + 1)
            }
        }

        // 9. Tachado: ~~texto~~ o <s>texto</s>
        val strikeRegex = Regex("""(~~(.*?)~~)|(<s>(.*?)</s>)""")
        for (m in strikeRegex.findAll(raw)) {
            val inner = m.groups[2] ?: m.groups[4]
            inner?.let {
                builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), m.range.first, it.range.first)
                builder.addStyle(SpanStyle(color = tagDimColor, fontSize = 11.sp), it.range.last + 1, m.range.last + 1)
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), it.range.first, it.range.last + 1)
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

/**
 * Parsea texto enriquecido reconociendo negrita, cursiva, subrayado, tachado, subíndices, superíndices,
 * tipografías específicas inline ([font:...]) y colores ([color:...]), soportando anidamiento limpio.
 */
fun parseRichInlineText(
    text: String,
    defaultFontFamily: FontFamily,
    baseFontSize: Int,
    defaultColor: Color
): AnnotatedString {
    if (text.isEmpty()) return buildAnnotatedString { append(" ") }

    // Limpiar cualquier residuo de etiquetas de alineación [align:xxx] y [/align]
    val cleanText = text
        .replace(Regex("""\[align:(left|center|right|justify)\]""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\[/align\]""", RegexOption.IGNORE_CASE), "")

    val pattern = Regex(
        """(\[font:(serif|sans|mono|cursive)\](.*?)\[/font\])|(\[color:(#[0-9a-fA-F]{3,8}|[a-zA-Z]+)\](.*?)\[/color\])|(\[weight:(thin|extralight|light|normal|regular|medium|semibold|bold|extrabold|black|\d{3})\](.*?)\[/weight\])|(\[3d(?::([#0-9a-zA-Z]+))?(?:,([#0-9a-zA-Z]+))?\](.*?)\[/3d\])|(\*\*(.*?)\*\*)|((?<!\*)\*([^*]+)\*(?!\*))|(<u>(.*?)</u>)|(__(.*?)__)|(~~(.*?)~~)|(<s>(.*?)</s>)|(<sub>(.*?)</sub>)|(<sup>(.*?)</sup>)""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    return buildAnnotatedString {
        var lastIndex = 0
        for (match in pattern.findAll(cleanText)) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > lastIndex) {
                append(cleanText.substring(lastIndex, start))
            }

            val fullMatch = match.value
            when {
                // [font:xxx]...[/font]
                fullMatch.startsWith("[font:", ignoreCase = true) && fullMatch.endsWith("[/font]", ignoreCase = true) -> {
                    val fontType = fullMatch.substringAfter("[font:", "").substringBefore("]", "").lowercase()
                    val innerText = fullMatch.substringAfter("]", "").removeSuffix("[/font]")
                    val fontFam = when (fontType) {
                        "sans" -> FontFamily.SansSerif
                        "mono" -> FontFamily.Monospace
                        "cursive" -> FontFamily.Cursive
                        else -> FontFamily.Serif
                    }
                    val nested = parseRichInlineText(innerText, fontFam, baseFontSize, defaultColor)
                    withStyle(SpanStyle(fontFamily = fontFam)) {
                        append(nested)
                    }
                }
                // [color:xxx]...[/color]
                fullMatch.startsWith("[color:", ignoreCase = true) && fullMatch.endsWith("[/color]", ignoreCase = true) -> {
                    val colorTag = fullMatch.substringAfter("[color:", "").substringBefore("]", "").trim()
                    val innerText = fullMatch.substringAfter("]", "").removeSuffix("[/color]")
                    val parsedColor = try {
                        val hex = if (colorTag.startsWith("#")) colorTag else when (colorTag.lowercase()) {
                            "black", "negro" -> "#1E293B"
                            "blue", "azul" -> "#1D4ED8"
                            "red", "rojo" -> "#BE123C"
                            "green", "verde" -> "#047857"
                            "purple", "morado" -> "#7E22CE"
                            "amber", "yellow", "amarillo" -> "#D97706"
                            "cyan", "teal" -> "#0284C7"
                            "pink", "rosa" -> "#E11D48"
                            "sepia", "brown", "marron", "café" -> "#78350F"
                            "gray", "grey", "gris" -> "#475569"
                            else -> "#$colorTag"
                        }
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (_: Exception) {
                        defaultColor
                    }
                    val nested = parseRichInlineText(innerText, defaultFontFamily, baseFontSize, parsedColor)
                    withStyle(SpanStyle(color = parsedColor)) {
                        append(nested)
                    }
                }
                // [weight:xxx]...[/weight]
                fullMatch.startsWith("[weight:", ignoreCase = true) && fullMatch.endsWith("[/weight]", ignoreCase = true) -> {
                    val weightStr = fullMatch.substringAfter("[weight:", "").substringBefore("]", "").trim()
                    val innerText = fullMatch.substringAfter("]", "").removeSuffix("[/weight]")
                    val resolvedWeight = parseFontWeightSafe(weightStr)
                    val nested = parseRichInlineText(innerText, defaultFontFamily, baseFontSize, defaultColor)
                    withStyle(SpanStyle(fontWeight = resolvedWeight)) {
                        append(nested)
                    }
                }
                // [3d:...]...[/3d] o [3d]...[/3d]
                fullMatch.startsWith("[3d", ignoreCase = true) && fullMatch.endsWith("[/3d]", ignoreCase = true) -> {
                    val header = fullMatch.substringAfter("[3d", "").substringBefore("]", "").trim()
                    val innerText = fullMatch.substringAfter("]", "").removeSuffix("[/3d]")
                    val params = if (header.startsWith(":")) header.removePrefix(":") else ""
                    val parts = params.split(",")
                    val shadowHex = parts.getOrNull(0)?.trim() ?: ""
                    val frontHex = parts.getOrNull(1)?.trim() ?: ""

                    val shadowCol = if (shadowHex.isNotEmpty()) parseColorSafe(shadowHex, Color(0xFF1E293B)) else Color(0xFF0F172A)
                    val frontCol = if (frontHex.isNotEmpty()) parseColorSafe(frontHex, defaultColor) else defaultColor

                    val nested = parseRichInlineText(innerText, defaultFontFamily, baseFontSize, frontCol)
                    withStyle(
                        SpanStyle(
                            color = frontCol,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = shadowCol.copy(alpha = 0.88f),
                                offset = Offset(3.5f, 3.5f),
                                blurRadius = 1.8f
                            )
                        )
                    ) {
                        append(nested)
                    }
                }
                // **negrita**
                fullMatch.startsWith("**") && fullMatch.endsWith("**") -> {
                    val inner = fullMatch.removePrefix("**").removeSuffix("**")
                    val nested = parseRichInlineText(inner, defaultFontFamily, baseFontSize, defaultColor)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(nested)
                    }
                }
                // *cursiva*
                fullMatch.startsWith("*") && fullMatch.endsWith("*") && (!fullMatch.startsWith("**")) -> {
                    val inner = fullMatch.removePrefix("*").removeSuffix("*")
                    val nested = parseRichInlineText(inner, defaultFontFamily, baseFontSize, defaultColor)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(nested)
                    }
                }
                // <u>subrayado</u> o __subrayado__
                (fullMatch.startsWith("<u>") && fullMatch.endsWith("</u>")) || (fullMatch.startsWith("__") && fullMatch.endsWith("__")) -> {
                    val inner = if (fullMatch.startsWith("<u>")) fullMatch.removePrefix("<u>").removeSuffix("</u>") else fullMatch.removePrefix("__").removeSuffix("__")
                    val nested = parseRichInlineText(inner, defaultFontFamily, baseFontSize, defaultColor)
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(nested)
                    }
                }
                // ~~tachado~~ o <s>tachado</s>
                (fullMatch.startsWith("~~") && fullMatch.endsWith("~~")) || (fullMatch.startsWith("<s>") && fullMatch.endsWith("</s>")) -> {
                    val inner = if (fullMatch.startsWith("~~")) fullMatch.removePrefix("~~").removeSuffix("~~") else fullMatch.removePrefix("<s>").removeSuffix("</s>")
                    val nested = parseRichInlineText(inner, defaultFontFamily, baseFontSize, defaultColor)
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(nested)
                    }
                }
                // <sub>subíndice</sub>
                fullMatch.startsWith("<sub>") && fullMatch.endsWith("</sub>") -> {
                    withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = (baseFontSize * 0.72f).sp)) {
                        append(fullMatch.removePrefix("<sub>").removeSuffix("</sub>"))
                    }
                }
                // <sup>superíndice</sup>
                fullMatch.startsWith("<sup>") && fullMatch.endsWith("</sup>") -> {
                    withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = (baseFontSize * 0.72f).sp)) {
                        append(fullMatch.removePrefix("<sup>").removeSuffix("</sup>"))
                    }
                }
                else -> {
                    append(fullMatch)
                }
            }
            lastIndex = end
        }

        if (lastIndex < cleanText.length) {
            append(cleanText.substring(lastIndex))
        }
    }
}

/**
 * Resuelve de forma segura una cadena de grosor a [FontWeight].
 */
fun parseFontWeightSafe(weightStr: String): FontWeight {
    return when (weightStr.lowercase().trim()) {
        "thin", "100" -> FontWeight.W100
        "extralight", "200" -> FontWeight.W200
        "light", "300" -> FontWeight.W300
        "normal", "regular", "400" -> FontWeight.W400
        "medium", "500" -> FontWeight.W500
        "semibold", "600" -> FontWeight.W600
        "bold", "700" -> FontWeight.W700
        "extrabold", "800" -> FontWeight.W800
        "black", "900" -> FontWeight.W900
        else -> FontWeight.Bold
    }
}

/**
 * Resuelve de forma segura nombres de color comunes o código hexadecimal a [Color].
 */
fun parseColorSafe(colVal: String, defaultColor: Color): Color {
    return try {
        val trimmed = colVal.trim()
        val hex = when (trimmed.lowercase()) {
            "black", "negro" -> "#1E293B"
            "white", "blanco" -> "#FFFFFF"
            "blue", "azul" -> "#2563EB"
            "red", "rojo" -> "#DC2626"
            "green", "verde" -> "#16A34A"
            "purple", "morado" -> "#9333EA"
            "amber", "yellow", "amarillo" -> "#D97706"
            "cyan", "teal" -> "#0D9488"
            "pink", "rosa" -> "#DB2777"
            "orange", "naranja" -> "#EA580C"
            "sepia", "brown", "marron", "café" -> "#78350F"
            "gray", "grey", "gris" -> "#475569"
            "gold", "dorado" -> "#B45309"
            else -> if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        }
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        defaultColor
    }
}

