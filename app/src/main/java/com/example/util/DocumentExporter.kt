package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.UnderlineSpan
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DocumentExporter: Motor de exportación digital para DocuSheet.
 * 
 * Permite convertir cualquier documento redactado en la aplicación a:
 * 1. Documento PDF digital (.pdf): Renderizado en alta resolución con formato A4,
 *    márgenes proporcionales, encabezados, numeración de páginas correlativa,
 *    tablas editoriales vectoriales formateadas, citas estilizadas, casillas de tareas,
 *    títulos jerárquicos y formato enriquecido en línea (negritas, cursivas, colores).
 * 2. Archivo Markdown (.md): Formato universal ligero para respaldo y edición multiplataforma.
 * 3. Documento HTML editorial (.html): Maquetación web lista para publicación o impresión.
 * 4. Documento de texto plano (.txt): Compatibilidad universal.
 * 
 * Utiliza FileProvider seguro para permitir la apertura directa o guardado mediante
 * cualquier aplicación instalada en el teléfono (Google Drive, WhatsApp, Adobe Reader, etc.).
 */
object DocumentExporter {

    // Dimensiones estándar A4 a 72 DPI (puntos de impresión)
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    private const val MARGIN_HORIZONTAL = 44
    private const val MARGIN_TOP = 52
    private const val MARGIN_BOTTOM = 52
    private const val CONTENT_WIDTH = A4_WIDTH - (MARGIN_HORIZONTAL * 2)
    private const val USABLE_PAGE_HEIGHT = A4_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM

    /**
     * Bloques editoriales estructurados para el renderizado vectorial de PDF.
     */
    private sealed class PdfBlock {
        data class Heading(val text: String, val level: Int, val align: Layout.Alignment) : PdfBlock()
        data class Paragraph(val text: CharSequence, val align: Layout.Alignment) : PdfBlock()
        data class Blockquote(val text: String, val align: Layout.Alignment) : PdfBlock()
        data class ListItem(val text: CharSequence, val isNumbered: Boolean, val index: Int) : PdfBlock()
        data class TaskItem(val text: CharSequence, val isChecked: Boolean) : PdfBlock()
        data class Table(val rows: List<List<String>>, val style: String) : PdfBlock()
        data class Shape(
            val shapeType: String,
            val widthPt: Float,
            val heightPt: Float,
            val align: String,
            val fillColor: Int,
            val strokeColor: Int,
            val borderWidth: Float,
            val cornerRadius: Float,
            val text: String
        ) : PdfBlock()
        data class Nodes(
            val nodes: List<String>,
            val align: String,
            val layout: String,
            val fillColor: Int,
            val strokeColor: Int
        ) : PdfBlock()
        object Divider : PdfBlock()
        object PageBreak : PdfBlock()
        object EmptyLine : PdfBlock()
    }

    /**
     * Exporta el documento como archivo PDF digital multipágina con renderizado vectorial completo.
     */
    fun exportToPdf(
        context: Context,
        title: String,
        content: String,
        fontStyle: String
    ): Uri? {
        val pdfDocument = PdfDocument()

        try {
            val baseTypeface = when (fontStyle) {
                "SANS_SERIF" -> Typeface.SANS_SERIF
                "MONOSPACE" -> Typeface.MONOSPACE
                "CURSIVE" -> Typeface.create("casual", Typeface.NORMAL)
                else -> Typeface.SERIF
            }

            // Pinceles tipográficos
            val bodyPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 11.5f
                color = Color.rgb(30, 41, 59)
                typeface = baseTypeface
            }

            val h1Paint = TextPaint().apply {
                isAntiAlias = true
                textSize = 19f
                color = Color.rgb(15, 23, 42)
                typeface = Typeface.create(baseTypeface, Typeface.BOLD)
            }

            val h2Paint = TextPaint().apply {
                isAntiAlias = true
                textSize = 15f
                color = Color.rgb(30, 41, 59)
                typeface = Typeface.create(baseTypeface, Typeface.BOLD)
            }

            val h3Paint = TextPaint().apply {
                isAntiAlias = true
                textSize = 13f
                color = Color.rgb(51, 65, 85)
                typeface = Typeface.create(baseTypeface, Typeface.BOLD)
            }

            val quotePaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 11f
                color = Color.rgb(71, 85, 105)
                typeface = Typeface.create(baseTypeface, Typeface.ITALIC)
            }

            val headerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 8.5f
                color = Color.rgb(148, 163, 184)
                typeface = baseTypeface
            }

            val linePaint = Paint().apply {
                isAntiAlias = true
                strokeWidth = 0.8f
                color = Color.rgb(226, 232, 240)
            }

            // 1. Parsear el contenido estructurado en bloques
            val rawBlocks = parseContentToPdfBlocks(content)

            // 2. Distribuir los bloques en páginas virtuales respetando la altura física de la hoja A4
            val pages = paginatePdfBlocks(rawBlocks, bodyPaint, h1Paint, h2Paint, h3Paint, quotePaint)
            val totalPages = maxOf(1, pages.size)

            // 3. Renderizar cada página en el documento PDF
            for ((pageIndex, pageBlocks) in pages.withIndex()) {
                val pageNumber = pageIndex + 1
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // --- Encabezado editorial superior ---
                val displayTitle = if (title.isBlank()) "DOCUMENTO DOCUSHEET" else title.uppercase()
                canvas.drawText(displayTitle, MARGIN_HORIZONTAL.toFloat(), 34f, headerPaint)
                val dateLabel = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val dateWidth = headerPaint.measureText(dateLabel)
                canvas.drawText(dateLabel, (A4_WIDTH - MARGIN_HORIZONTAL - dateWidth).toFloat(), 34f, headerPaint)
                canvas.drawLine(
                    MARGIN_HORIZONTAL.toFloat(),
                    40f,
                    (A4_WIDTH - MARGIN_HORIZONTAL).toFloat(),
                    40f,
                    linePaint
                )

                // --- Pie de página correlativo formal (— Página X de Y —) ---
                val footerText = "— Página $pageNumber de $totalPages —"
                val footerWidth = headerPaint.measureText(footerText)
                canvas.drawText(
                    footerText,
                    (A4_WIDTH - footerWidth) / 2f,
                    (A4_HEIGHT - 28).toFloat(),
                    headerPaint
                )
                canvas.drawText(
                    "DocuSheet",
                    MARGIN_HORIZONTAL.toFloat(),
                    (A4_HEIGHT - 28).toFloat(),
                    headerPaint
                )

                // --- Renderizado secuencial de los bloques de la página ---
                var currentY = MARGIN_TOP.toFloat()

                for (block in pageBlocks) {
                    when (block) {
                        is PdfBlock.Heading -> {
                            val paint = when (block.level) {
                                1 -> h1Paint
                                2 -> h2Paint
                                else -> h3Paint
                            }
                            val topSpacing = if (block.level == 1) 12f else 8f
                            currentY += topSpacing

                            val layout = StaticLayout.Builder
                                .obtain(block.text, 0, block.text.length, paint, CONTENT_WIDTH)
                                .setAlignment(block.align)
                                .setIncludePad(true)
                                .build()

                            canvas.save()
                            canvas.translate(MARGIN_HORIZONTAL.toFloat(), currentY)
                            layout.draw(canvas)
                            canvas.restore()

                            currentY += layout.height + 5f

                            if (block.level == 1) {
                                canvas.drawLine(
                                    MARGIN_HORIZONTAL.toFloat(),
                                    currentY,
                                    (MARGIN_HORIZONTAL + 160).toFloat().coerceAtMost((A4_WIDTH - MARGIN_HORIZONTAL).toFloat()),
                                    currentY,
                                    Paint().apply {
                                        color = Color.rgb(37, 99, 235)
                                        strokeWidth = 1.5f
                                        isAntiAlias = true
                                    }
                                )
                                currentY += 6f
                            }
                        }

                        is PdfBlock.Paragraph -> {
                            val layout = StaticLayout.Builder
                                .obtain(block.text, 0, block.text.length, bodyPaint, CONTENT_WIDTH)
                                .setAlignment(block.align)
                                .setLineSpacing(3f, 1.15f)
                                .setIncludePad(true)
                                .build()

                            canvas.save()
                            canvas.translate(MARGIN_HORIZONTAL.toFloat(), currentY)
                            layout.draw(canvas)
                            canvas.restore()

                            currentY += layout.height + 6f
                        }

                        is PdfBlock.Blockquote -> {
                            val quoteContentWidth = CONTENT_WIDTH - 24
                            val layout = StaticLayout.Builder
                                .obtain(block.text, 0, block.text.length, quotePaint, quoteContentWidth)
                                .setAlignment(block.align)
                                .setLineSpacing(3f, 1.15f)
                                .setIncludePad(true)
                                .build()

                            val blockHeight = layout.height + 12f

                            // Fondo sombreado suave de la cita
                            val bgPaint = Paint().apply {
                                color = Color.rgb(241, 245, 249)
                                style = Paint.Style.FILL
                            }
                            canvas.drawRoundRect(
                                RectF(
                                    MARGIN_HORIZONTAL.toFloat(),
                                    currentY,
                                    (A4_WIDTH - MARGIN_HORIZONTAL).toFloat(),
                                    currentY + blockHeight
                                ),
                                4f,
                                4f,
                                bgPaint
                            )

                            // Barra vertical izquierda de acento
                            val barPaint = Paint().apply {
                                color = Color.rgb(37, 99, 235)
                                style = Paint.Style.FILL
                            }
                            canvas.drawRoundRect(
                                RectF(
                                    MARGIN_HORIZONTAL.toFloat(),
                                    currentY,
                                    (MARGIN_HORIZONTAL + 4).toFloat(),
                                    currentY + blockHeight
                                ),
                                2f,
                                2f,
                                barPaint
                            )

                            // Dibujar texto
                            canvas.save()
                            canvas.translate((MARGIN_HORIZONTAL + 14).toFloat(), currentY + 6f)
                            layout.draw(canvas)
                            canvas.restore()

                            currentY += blockHeight + 6f
                        }

                        is PdfBlock.ListItem -> {
                            val bulletWidth = 18f
                            val textWidth = CONTENT_WIDTH - bulletWidth.toInt()

                            val layout = StaticLayout.Builder
                                .obtain(block.text, 0, block.text.length, bodyPaint, textWidth)
                                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                .setLineSpacing(2f, 1.1f)
                                .setIncludePad(true)
                                .build()

                            // Dibujar viñeta o número
                            if (block.isNumbered) {
                                val numText = "${block.index}."
                                canvas.drawText(
                                    numText,
                                    MARGIN_HORIZONTAL.toFloat(),
                                    currentY + 12f,
                                    TextPaint(bodyPaint).apply { isFakeBoldText = true }
                                )
                            } else {
                                val dotPaint = Paint().apply {
                                    color = Color.rgb(37, 99, 235)
                                    style = Paint.Style.FILL
                                    isAntiAlias = true
                                }
                                canvas.drawCircle(
                                    MARGIN_HORIZONTAL + 6f,
                                    currentY + 8f,
                                    2.5f,
                                    dotPaint
                                )
                            }

                            canvas.save()
                            canvas.translate((MARGIN_HORIZONTAL + bulletWidth), currentY)
                            layout.draw(canvas)
                            canvas.restore()

                            currentY += layout.height + 4f
                        }

                        is PdfBlock.TaskItem -> {
                            val boxSize = 10f
                            val textWidth = CONTENT_WIDTH - 20

                            val layout = StaticLayout.Builder
                                .obtain(block.text, 0, block.text.length, bodyPaint, textWidth)
                                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                .setLineSpacing(2f, 1.1f)
                                .setIncludePad(true)
                                .build()

                            val boxRect = RectF(
                                MARGIN_HORIZONTAL.toFloat(),
                                currentY + 3f,
                                (MARGIN_HORIZONTAL + boxSize),
                                currentY + 3f + boxSize
                            )

                            val boxPaint = Paint().apply {
                                isAntiAlias = true
                                style = if (block.isChecked) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
                                strokeWidth = 1f
                                color = if (block.isChecked) Color.rgb(37, 99, 235) else Color.rgb(148, 163, 184)
                            }
                            canvas.drawRoundRect(boxRect, 2f, 2f, boxPaint)

                            if (block.isChecked) {
                                val checkPaint = Paint().apply {
                                    isAntiAlias = true
                                    color = Color.WHITE
                                    strokeWidth = 1.5f
                                    style = Paint.Style.STROKE
                                }
                                canvas.drawLine(boxRect.left + 2f, boxRect.centerY(), boxRect.left + 4.5f, boxRect.bottom - 2f, checkPaint)
                                canvas.drawLine(boxRect.left + 4.5f, boxRect.bottom - 2f, boxRect.right - 2f, boxRect.top + 2.5f, checkPaint)
                            }

                            canvas.save()
                            canvas.translate((MARGIN_HORIZONTAL + 18).toFloat(), currentY)
                            layout.draw(canvas)
                            canvas.restore()

                            currentY += layout.height + 4f
                        }

                        is PdfBlock.Table -> {
                            currentY = drawPdfTable(canvas, block, currentY, bodyPaint, baseTypeface) + 8f
                        }

                        is PdfBlock.Shape -> {
                            currentY = drawPdfShape(canvas, block, currentY, baseTypeface) + 8f
                        }

                        is PdfBlock.Nodes -> {
                            currentY = drawPdfNodes(canvas, block, currentY, baseTypeface) + 8f
                        }

                        is PdfBlock.Divider -> {
                            currentY += 6f
                            val divPaint = Paint().apply {
                                color = Color.rgb(203, 213, 225)
                                strokeWidth = 1f
                                isAntiAlias = true
                            }
                            canvas.drawLine(
                                MARGIN_HORIZONTAL.toFloat(),
                                currentY,
                                (A4_WIDTH - MARGIN_HORIZONTAL).toFloat(),
                                currentY,
                                divPaint
                            )
                            currentY += 10f
                        }

                        is PdfBlock.EmptyLine -> {
                            currentY += 8f
                        }

                        is PdfBlock.PageBreak -> {
                            // Los saltos de página ya dividieron los bloques entre páginas
                        }
                    }
                }

                pdfDocument.finishPage(page)
            }

            // Guardar en el directorio seguro de caché para exportaciones
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = sanitizeFileName(title)
            val outputFile = File(exportDir, "$cleanTitle.pdf")

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Dibuja una tabla editorial vectorial completa en el canvas del PDF.
     */
    private fun drawPdfTable(
        canvas: Canvas,
        table: PdfBlock.Table,
        startY: Float,
        basePaint: TextPaint,
        baseTypeface: Typeface
    ): Float {
        val rows = table.rows
        if (rows.isEmpty()) return startY

        val numCols = maxOf(1, rows.maxOfOrNull { it.size } ?: 1)
        val colWidth = CONTENT_WIDTH.toFloat() / numCols

        val headerBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        val stripedBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = if (table.style == "editorial") Color.rgb(100, 116, 139) else Color.rgb(203, 213, 225)
            strokeWidth = if (table.style == "editorial") 1.2f else 0.8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val cellHeaderPaint = TextPaint(basePaint).apply {
            typeface = Typeface.create(baseTypeface, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
        }

        val cellDataPaint = TextPaint(basePaint).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
        }

        var currentY = startY + 4f

        for ((rowIndex, row) in rows.withIndex()) {
            val isHeader = rowIndex == 0
            val paint = if (isHeader) cellHeaderPaint else cellDataPaint
            val horizontalPadding = 6f
            val verticalPadding = 5f
            val usableColWidth = maxOf(10f, colWidth - (horizontalPadding * 2))

            // Medir la altura de cada celda para determinar la altura de la fila
            val layouts = (0 until numCols).map { c ->
                val text = row.getOrNull(c) ?: ""
                val cleanText = text.ifBlank { "—" }
                StaticLayout.Builder
                    .obtain(cleanText, 0, cleanText.length, paint, usableColWidth.toInt())
                    .setAlignment(if (isHeader) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(true)
                    .build()
            }

            val maxCellHeight = layouts.maxOfOrNull { it.height } ?: 16
            val rowHeight = maxCellHeight + (verticalPadding * 2)

            // Fondo de la fila
            if (isHeader) {
                canvas.drawRect(
                    MARGIN_HORIZONTAL.toFloat(),
                    currentY,
                    (MARGIN_HORIZONTAL + CONTENT_WIDTH).toFloat(),
                    currentY + rowHeight,
                    headerBgPaint
                )
            } else if (table.style == "striped" && rowIndex % 2 == 1) {
                canvas.drawRect(
                    MARGIN_HORIZONTAL.toFloat(),
                    currentY,
                    (MARGIN_HORIZONTAL + CONTENT_WIDTH).toFloat(),
                    currentY + rowHeight,
                    stripedBgPaint
                )
            }

            // Dibujar bordes horizontales
            if (table.style == "editorial") {
                if (isHeader) {
                    canvas.drawLine(MARGIN_HORIZONTAL.toFloat(), currentY, (MARGIN_HORIZONTAL + CONTENT_WIDTH).toFloat(), currentY, borderPaint)
                    canvas.drawLine(MARGIN_HORIZONTAL.toFloat(), currentY + rowHeight, (MARGIN_HORIZONTAL + CONTENT_WIDTH).toFloat(), currentY + rowHeight, borderPaint)
                } else if (rowIndex == rows.size - 1) {
                    canvas.drawLine(MARGIN_HORIZONTAL.toFloat(), currentY + rowHeight, (MARGIN_HORIZONTAL + CONTENT_WIDTH).toFloat(), currentY + rowHeight, borderPaint)
                }
            } else {
                // Estilo classic o compact: cuadrícula completa
                canvas.drawRect(
                    MARGIN_HORIZONTAL.toFloat(),
                    currentY,
                    (MARGIN_HORIZONTAL + CONTENT_WIDTH).toFloat(),
                    currentY + rowHeight,
                    borderPaint
                )
            }

            // Dibujar celdas y bordes verticales
            for (colIndex in 0 until numCols) {
                val cellLeft = MARGIN_HORIZONTAL + (colIndex * colWidth)
                val layout = layouts[colIndex]

                if (table.style != "editorial" && colIndex > 0) {
                    canvas.drawLine(cellLeft, currentY, cellLeft, currentY + rowHeight, borderPaint)
                }

                canvas.save()
                canvas.translate(cellLeft + horizontalPadding, currentY + verticalPadding)
                layout.draw(canvas)
                canvas.restore()
            }

            currentY += rowHeight
        }

        return currentY
    }

    /**
     * Dibuja una figura geométrica vectorial en el canvas del PDF.
     */
    private fun drawPdfShape(
        canvas: Canvas,
        shape: PdfBlock.Shape,
        startY: Float,
        baseTypeface: Typeface
    ): Float {
        val shapeWidth = shape.widthPt.coerceIn(30f, CONTENT_WIDTH.toFloat())
        val shapeHeight = shape.heightPt.coerceIn(20f, 400f)

        val startX = when (shape.align) {
            "center" -> MARGIN_HORIZONTAL + (CONTENT_WIDTH - shapeWidth) / 2f
            "right" -> MARGIN_HORIZONTAL + CONTENT_WIDTH - shapeWidth
            else -> MARGIN_HORIZONTAL.toFloat()
        }

        val fillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = shape.fillColor
        }

        val strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = shape.strokeColor
            strokeWidth = shape.borderWidth.coerceAtLeast(0.5f)
        }

        val rectF = RectF(startX, startY, startX + shapeWidth, startY + shapeHeight)

        when (shape.shapeType.lowercase()) {
            "circle", "oval" -> {
                canvas.drawOval(rectF, fillPaint)
                canvas.drawOval(rectF, strokePaint)
            }
            "triangle" -> {
                val path = android.graphics.Path().apply {
                    moveTo(startX + shapeWidth / 2f, startY)
                    lineTo(startX + shapeWidth, startY + shapeHeight)
                    lineTo(startX, startY + shapeHeight)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
            }
            "diamond" -> {
                val path = android.graphics.Path().apply {
                    moveTo(startX + shapeWidth / 2f, startY)
                    lineTo(startX + shapeWidth, startY + shapeHeight / 2f)
                    lineTo(startX + shapeWidth / 2f, startY + shapeHeight)
                    lineTo(startX, startY + shapeHeight / 2f)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
            }
            "star" -> {
                val path = android.graphics.Path()
                val cx = startX + shapeWidth / 2f
                val cy = startY + shapeHeight / 2f
                val outerR = minOf(shapeWidth, shapeHeight) / 2f
                val innerR = outerR * 0.45f
                for (step in 0 until 10) {
                    val r = if (step % 2 == 0) outerR else innerR
                    val angle = Math.toRadians((step * 36.0) - 90.0)
                    val px = cx + (r * Math.cos(angle)).toFloat()
                    val py = cy + (r * Math.sin(angle)).toFloat()
                    if (step == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
            }
            "arrow" -> {
                val path = android.graphics.Path().apply {
                    val headW = shapeWidth * 0.4f
                    val shaftH = shapeHeight * 0.4f
                    val shaftTop = startY + (shapeHeight - shaftH) / 2f
                    moveTo(startX, shaftTop)
                    lineTo(startX + shapeWidth - headW, shaftTop)
                    lineTo(startX + shapeWidth - headW, startY)
                    lineTo(startX + shapeWidth, startY + shapeHeight / 2f)
                    lineTo(startX + shapeWidth - headW, startY + shapeHeight)
                    lineTo(startX + shapeWidth - headW, shaftTop + shaftH)
                    lineTo(startX, shaftTop + shaftH)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
            }
            "callout" -> {
                val path = android.graphics.Path().apply {
                    val cr = shape.cornerRadius.coerceIn(0f, 20f)
                    val bubbleH = shapeHeight * 0.75f
                    val bubbleRect = RectF(startX, startY, startX + shapeWidth, startY + bubbleH)
                    addRoundRect(bubbleRect, cr, cr, android.graphics.Path.Direction.CW)
                    moveTo(startX + shapeWidth * 0.25f, startY + bubbleH)
                    lineTo(startX + shapeWidth * 0.15f, startY + shapeHeight)
                    lineTo(startX + shapeWidth * 0.45f, startY + bubbleH)
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
            }
            else -> {
                val cr = shape.cornerRadius.coerceIn(0f, 30f)
                canvas.drawRoundRect(rectF, cr, cr, fillPaint)
                canvas.drawRoundRect(rectF, cr, cr, strokePaint)
            }
        }

        if (shape.text.isNotBlank()) {
            val textPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 10.5f
                typeface = Typeface.create(baseTypeface, Typeface.BOLD)
                color = if (Color.luminance(shape.fillColor) > 0.5) Color.BLACK else Color.WHITE
            }
            val textLayout = StaticLayout.Builder
                .obtain(shape.text, 0, shape.text.length, textPaint, (shapeWidth - 16f).toInt().coerceAtLeast(20))
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()

            canvas.save()
            val textY = startY + (shapeHeight - textLayout.height) / 2f
            canvas.translate(startX + 8f, textY)
            textLayout.draw(canvas)
            canvas.restore()
        }

        return startY + shapeHeight
    }

    /**
     * Dibuja un diagrama de nodos secuenciales en el canvas del PDF.
     */
    private fun drawPdfNodes(
        canvas: Canvas,
        nodesBlock: PdfBlock.Nodes,
        startY: Float,
        baseTypeface: Typeface
    ): Float {
        val items = nodesBlock.nodes
        if (items.isEmpty()) return startY

        val fillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = nodesBlock.fillColor
        }
        val strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = nodesBlock.strokeColor
            strokeWidth = 1.2f
        }
        val textPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(baseTypeface, Typeface.BOLD)
            color = if (Color.luminance(nodesBlock.fillColor) > 0.5) Color.BLACK else Color.WHITE
        }
        val arrowPaint = Paint().apply {
            isAntiAlias = true
            color = nodesBlock.strokeColor
            strokeWidth = 1.5f
            textSize = 12f
            typeface = Typeface.create(baseTypeface, Typeface.BOLD)
        }

        var currentY = startY

        if (nodesBlock.layout == "vertical") {
            val nodeW = 180f
            val nodeH = 26f
            val startX = when (nodesBlock.align) {
                "center" -> MARGIN_HORIZONTAL + (CONTENT_WIDTH - nodeW) / 2f
                "right" -> MARGIN_HORIZONTAL + CONTENT_WIDTH - nodeW
                else -> MARGIN_HORIZONTAL.toFloat()
            }

            for ((idx, nodeText) in items.withIndex()) {
                val rectF = RectF(startX, currentY, startX + nodeW, currentY + nodeH)
                canvas.drawRoundRect(rectF, 6f, 6f, fillPaint)
                canvas.drawRoundRect(rectF, 6f, 6f, strokePaint)

                val textLayout = StaticLayout.Builder
                    .obtain(nodeText, 0, nodeText.length, textPaint, (nodeW - 12f).toInt())
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build()
                canvas.save()
                canvas.translate(startX + 6f, currentY + (nodeH - textLayout.height) / 2f)
                textLayout.draw(canvas)
                canvas.restore()

                currentY += nodeH

                if (idx < items.size - 1) {
                    val arrowX = startX + nodeW / 2f
                    canvas.drawLine(arrowX, currentY, arrowX, currentY + 10f, arrowPaint)
                    canvas.drawLine(arrowX, currentY + 10f, arrowX - 3f, currentY + 7f, arrowPaint)
                    canvas.drawLine(arrowX, currentY + 10f, arrowX + 3f, currentY + 7f, arrowPaint)
                    currentY += 14f
                }
            }
        } else {
            val totalSpacing = 24f * (items.size - 1)
            val availableW = CONTENT_WIDTH - totalSpacing
            val nodeW = (availableW / items.size).coerceIn(40f, 130f)
            val nodeH = 28f
            val totalWidth = (nodeW * items.size) + totalSpacing

            var currentX = when (nodesBlock.align) {
                "center" -> MARGIN_HORIZONTAL + (CONTENT_WIDTH - totalWidth) / 2f
                "right" -> MARGIN_HORIZONTAL + CONTENT_WIDTH - totalWidth
                else -> MARGIN_HORIZONTAL.toFloat()
            }

            for ((idx, nodeText) in items.withIndex()) {
                val rectF = RectF(currentX, currentY, currentX + nodeW, currentY + nodeH)
                canvas.drawRoundRect(rectF, 6f, 6f, fillPaint)
                canvas.drawRoundRect(rectF, 6f, 6f, strokePaint)

                val textLayout = StaticLayout.Builder
                    .obtain(nodeText, 0, nodeText.length, textPaint, (nodeW - 8f).toInt().coerceAtLeast(20))
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build()
                canvas.save()
                canvas.translate(currentX + 4f, currentY + (nodeH - textLayout.height) / 2f)
                textLayout.draw(canvas)
                canvas.restore()

                currentX += nodeW

                if (idx < items.size - 1) {
                    val arrowY = currentY + nodeH / 2f
                    canvas.drawLine(currentX + 3f, arrowY, currentX + 20f, arrowY, arrowPaint)
                    canvas.drawLine(currentX + 20f, arrowY, currentX + 16f, arrowY - 3f, arrowPaint)
                    canvas.drawLine(currentX + 20f, arrowY, currentX + 16f, arrowY + 3f, arrowPaint)
                    currentX += 24f
                }
            }
            currentY += nodeH
        }

        return currentY
    }

    private fun parsePdfShapeDirective(line: String): PdfBlock.Shape? {
        val content = line.removePrefix("[shape:").removeSuffix("]").trim()
        val parts = content.split(",")
        var type = "rect"
        var w = 160f
        var h = 90f
        var align = "center"
        var fill = Color.rgb(241, 245, 249)
        var stroke = Color.rgb(37, 99, 235)
        var border = 2f
        var corner = 8f
        var text = ""

        for (p in parts) {
            val pair = p.split("=", limit = 2)
            if (pair.size == 2) {
                val k = pair[0].trim().lowercase()
                val v = pair[1].trim()
                when (k) {
                    "type" -> type = v
                    "w", "width" -> w = v.toFloatOrNull() ?: 160f
                    "h", "height" -> h = v.toFloatOrNull() ?: 90f
                    "align" -> align = v.lowercase()
                    "fill" -> {
                        try {
                            fill = Color.parseColor(if (v.startsWith("#")) v else "#$v")
                        } catch (_: Exception) {}
                    }
                    "stroke" -> {
                        try {
                            stroke = Color.parseColor(if (v.startsWith("#")) v else "#$v")
                        } catch (_: Exception) {}
                    }
                    "border" -> border = v.toFloatOrNull() ?: 2f
                    "corner" -> corner = v.toFloatOrNull() ?: 8f
                    "text" -> text = v
                }
            }
        }
        return PdfBlock.Shape(type, w, h, align, fill, stroke, border, corner, text)
    }

    private fun parsePdfNodesDirective(lines: List<String>, startIndex: Int): Pair<PdfBlock.Nodes, Int>? {
        val firstLine = lines[startIndex].trim()
        var align = "center"
        var layout = "horizontal"
        var fill = Color.rgb(238, 242, 255)
        var stroke = Color.rgb(99, 102, 241)

        val inlineParams = firstLine.removePrefix("[nodes").substringBefore("]").removePrefix(":").trim()
        if (inlineParams.isNotBlank()) {
            for (p in inlineParams.split(",")) {
                val pair = p.split("=", limit = 2)
                if (pair.size == 2) {
                    val k = pair[0].trim().lowercase()
                    val v = pair[1].trim()
                    when (k) {
                        "align" -> align = v.lowercase()
                        "layout" -> layout = v.lowercase()
                        "fill" -> {
                            try {
                                fill = Color.parseColor(if (v.startsWith("#")) v else "#$v")
                            } catch (_: Exception) {}
                        }
                        "stroke" -> {
                            try {
                                stroke = Color.parseColor(if (v.startsWith("#")) v else "#$v")
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }

        val nodes = mutableListOf<String>()
        var nextIndex = startIndex + 1

        if (firstLine.endsWith("[/nodes]") || (!firstLine.endsWith("]") && firstLine.contains("->"))) {
            val body = firstLine.removePrefix("[nodes:").removePrefix("[nodes]").removeSuffix("[/nodes]").trim()
            val tokens = body.split("->").map { it.trim() }.filter { it.isNotEmpty() }
            nodes.addAll(tokens)
            nextIndex = startIndex + 1
        } else {
            while (nextIndex < lines.size && lines[nextIndex].trim() != "[/nodes]") {
                val l = lines[nextIndex].trim()
                if (l.isNotBlank()) {
                    if (l.contains("->")) {
                        nodes.addAll(l.split("->").map { it.trim() }.filter { it.isNotEmpty() })
                    } else {
                        nodes.add(l.removePrefix("- ").removePrefix("• ").trim())
                    }
                }
                nextIndex++
            }
            if (nextIndex < lines.size && lines[nextIndex].trim() == "[/nodes]") {
                nextIndex++
            }
        }

        if (nodes.isEmpty()) return null
        return Pair(PdfBlock.Nodes(nodes, align, layout, fill, stroke), nextIndex)
    }

    /**
     * Convierte el texto en bruto del documento a una lista estructurada de bloques de renderizado PDF.
     */
    private fun parseContentToPdfBlocks(rawContent: String): List<PdfBlock> {
        val lines = rawContent.lines()
        val blocks = mutableListOf<PdfBlock>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 1. Salto de página explícito
            if (trimmed == "[--- Salto de Página ---]" || trimmed == "---" && lines.getOrNull(i - 1)?.isBlank() == true) {
                blocks.add(PdfBlock.PageBreak)
                i++
                continue
            }

            // 2. Tabla explícita con directiva [table:style] ... [/table]
            if (trimmed.startsWith("[table:") && trimmed.endsWith("]")) {
                val style = trimmed.removePrefix("[table:").removeSuffix("]").trim()
                val tableLines = mutableListOf<String>()
                i++
                while (i < lines.size && lines[i].trim() != "[/table]") {
                    if (lines[i].trim().startsWith("|")) {
                        tableLines.add(lines[i])
                    }
                    i++
                }
                val parsedTable = parseTableLines(tableLines, style.ifEmpty { "classic" })
                if (parsedTable != null) blocks.add(parsedTable)
                i++
                continue
            }

            // 2b. Figura geométrica vectorial: [shape:type=...,w=...,h=...,align=...,fill=...,stroke=...]
            if (trimmed.startsWith("[shape:") && trimmed.endsWith("]")) {
                val shapeBlock = parsePdfShapeDirective(trimmed)
                if (shapeBlock != null) {
                    blocks.add(shapeBlock)
                    i++
                    continue
                }
            }

            // 2c. Diagrama de nodos y flujo: [nodes:align=...,layout=...] ... [/nodes] o [nodes:a -> b -> c]
            if (trimmed.startsWith("[nodes") && (trimmed.endsWith("]") || trimmed.contains("->"))) {
                val nodesParsed = parsePdfNodesDirective(lines, i)
                if (nodesParsed != null) {
                    blocks.add(nodesParsed.first)
                    i = nodesParsed.second
                    continue
                }
            }

            // 3. Tabla Markdown implícita (| col 1 | col 2 |)
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length > 2) {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    tableLines.add(lines[i])
                    i++
                }
                val parsedTable = parseTableLines(tableLines, "classic")
                if (parsedTable != null) blocks.add(parsedTable)
                continue
            }

            // 4. Divisor horizontal
            if (trimmed == "───" || (trimmed.startsWith("---") && trimmed.length in 3..10)) {
                blocks.add(PdfBlock.Divider)
                i++
                continue
            }

            // 5. Línea vacía
            if (trimmed.isEmpty()) {
                blocks.add(PdfBlock.EmptyLine)
                i++
                continue
            }

            // 6. Directivas de alineación específica: [align:xxx]...[/align]
            val alignRegex = Regex("""\[align:(left|center|right|justify)\](.*?)\[/align\]""", RegexOption.DOT_MATCHES_ALL)
            val alignMatch = alignRegex.find(trimmed)
            val (effectiveLine, effectiveAlign) = if (alignMatch != null) {
                val mode = alignMatch.groupValues[1].lowercase()
                val content = trimmed.replace(alignMatch.value, alignMatch.groupValues[2]).trim()
                val align = when (mode) {
                    "center" -> Layout.Alignment.ALIGN_CENTER
                    "right" -> Layout.Alignment.ALIGN_OPPOSITE
                    else -> Layout.Alignment.ALIGN_NORMAL
                }
                Pair(content, align)
            } else {
                Pair(trimmed, Layout.Alignment.ALIGN_NORMAL)
            }

            // 7. Encabezados jerárquicos
            if (effectiveLine.startsWith("# ")) {
                blocks.add(PdfBlock.Heading(effectiveLine.removePrefix("# ").trim(), 1, effectiveAlign))
                i++
                continue
            }
            if (effectiveLine.startsWith("## ")) {
                blocks.add(PdfBlock.Heading(effectiveLine.removePrefix("## ").trim(), 2, effectiveAlign))
                i++
                continue
            }
            if (effectiveLine.startsWith("### ")) {
                blocks.add(PdfBlock.Heading(effectiveLine.removePrefix("### ").trim(), 3, effectiveAlign))
                i++
                continue
            }

            // 8. Cita destacada
            if (effectiveLine.startsWith("> ")) {
                blocks.add(PdfBlock.Blockquote(effectiveLine.removePrefix("> ").trim(), effectiveAlign))
                i++
                continue
            }

            // 9. Casillas de tareas
            if (effectiveLine.startsWith("[ ] ")) {
                val spanned = parseInlineFormattingToSpanned(effectiveLine.removePrefix("[ ] ").trim())
                blocks.add(PdfBlock.TaskItem(spanned, isChecked = false))
                i++
                continue
            }
            if (effectiveLine.startsWith("[x] ") || effectiveLine.startsWith("[X] ")) {
                val spanned = parseInlineFormattingToSpanned(effectiveLine.substring(4).trim())
                blocks.add(PdfBlock.TaskItem(spanned, isChecked = true))
                i++
                continue
            }

            // 10. Listas con viñeta o numeradas
            if (effectiveLine.startsWith("- ") || effectiveLine.startsWith("• ") || effectiveLine.startsWith("* ")) {
                val itemText = effectiveLine.removePrefix("- ").removePrefix("• ").removePrefix("* ").trim()
                val spanned = parseInlineFormattingToSpanned(itemText)
                blocks.add(PdfBlock.ListItem(spanned, isNumbered = false, index = 0))
                i++
                continue
            }
            val numMatch = Regex("""^(\d+)\.\s+(.*)""").find(effectiveLine)
            if (numMatch != null) {
                val index = numMatch.groupValues[1].toIntOrNull() ?: 1
                val itemText = numMatch.groupValues[2].trim()
                val spanned = parseInlineFormattingToSpanned(itemText)
                blocks.add(PdfBlock.ListItem(spanned, isNumbered = true, index = index))
                i++
                continue
            }

            // 11. Párrafo estándar con formato enriquecido en línea
            val spanned = parseInlineFormattingToSpanned(effectiveLine)
            blocks.add(PdfBlock.Paragraph(spanned, effectiveAlign))
            i++
        }

        return blocks
    }

    /**
     * Parsea las líneas de una tabla Markdown en una estructura de filas y celdas.
     */
    private fun parseTableLines(lines: List<String>, style: String): PdfBlock.Table? {
        val rows = mutableListOf<List<String>>()
        for (line in lines) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("|")) continue

            // Ignorar la línea separadora Markdown (|---|---|)
            if (trimmed.contains("---") && trimmed.replace("|", "").replace("-", "").replace(":", "").isBlank()) {
                continue
            }

            val cells = trimmed
                .removePrefix("|")
                .removeSuffix("|")
                .split("|")
                .map { it.trim() }

            if (cells.isNotEmpty() && cells.any { it.isNotEmpty() }) {
                rows.add(cells)
            }
        }
        return if (rows.isNotEmpty()) PdfBlock.Table(rows, style) else null
    }

    /**
     * Span vectorial especializado para renderizar texto 3D con relieve, biselado y sombra estereoscópica
     * en el Canvas vectorial del PdfDocument de Android sin distorsiones ni pérdida de resolución.
     */
    private class ThreeDWordSpan(
        val shadowColor: Int,
        val bevelColor: Int,
        val frontColor: Int
    ) : ReplacementSpan() {
        override fun getSize(
            paint: Paint,
            text: CharSequence,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?
        ): Int {
            val tp = TextPaint(paint).apply {
                typeface = Typeface.create(paint.typeface, Typeface.BOLD)
            }
            if (fm != null) {
                val pFm = tp.fontMetricsInt
                fm.ascent = pFm.ascent
                fm.descent = pFm.descent
                fm.top = pFm.top
                fm.bottom = pFm.bottom
            }
            return tp.measureText(text, start, end).toInt()
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint
        ) {
            val tp = TextPaint(paint).apply {
                typeface = Typeface.create(paint.typeface, Typeface.BOLD)
                isAntiAlias = true
            }

            // 1. Capa de sombra 3D profunda
            tp.color = shadowColor
            canvas.drawText(text, start, end, x + 2.0f, y.toFloat() + 2.0f, tp)

            // 2. Capa intermedia de extrusión / bisel 3D
            tp.color = bevelColor
            canvas.drawText(text, start, end, x + 1.0f, y.toFloat() + 1.0f, tp)

            // 3. Capa frontal nítida de relieve
            tp.color = frontColor
            canvas.drawText(text, start, end, x, y.toFloat(), tp)
        }
    }

    private fun blendColors(c1: Int, c2: Int, ratio: Float = 0.5f): Int {
        val inverse = 1f - ratio
        val r = (Color.red(c1) * inverse + Color.red(c2) * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(c1) * inverse + Color.green(c2) * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(c1) * inverse + Color.blue(c2) * ratio).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun parseHexColorSafe(hex: String, fallback: Int): Int {
        return try {
            val clean = hex.trim()
            if (clean.isEmpty()) return fallback
            val formatted = if (clean.startsWith("#")) clean else "#$clean"
            Color.parseColor(formatted)
        } catch (_: Exception) {
            fallback
        }
    }

    /**
     * Parsea etiquetas Markdown y directivas inline a un SpannableStringBuilder con estilos reales
     * (negrita, cursiva, subrayado, tachado, subíndice, superíndice, colores y efectos 3D vectoriales).
     */
    private fun parseInlineFormattingToSpanned(text: String): CharSequence {
        val ssb = SpannableStringBuilder()
        // Procesador de tokens con expresiones regulares para transformar el texto limpio y aplicar spans
        var clean = text

        // Extraer formato por reemplazos seguros acumulativos
        // 1. Color: [color:#HEX]texto[/color]
        val colorRegex = Regex("""\[color:(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8})\](.*?)\[/color\]""")
        // 2. Negrita: **texto**
        val boldRegex = Regex("""\*\*(.*?)\*\*""")
        // 3. Cursiva: *texto*
        val italicRegex = Regex("""\*(.*?)\*""")
        // 4. Subrayado: <u>texto</u>
        val underlineRegex = Regex("""<u>(.*?)</u>""")
        // 5. Tachado: ~~texto~~
        val strikeRegex = Regex("""~~(.*?)~~""")
        // 6. Superíndice: <sup>texto</sup>
        val superRegex = Regex("""<sup>(.*?)</sup>""")
        // 7. Subíndice: <sub>texto</sub>
        val subRegex = Regex("""<sub>(.*?)</sub>""")
        // 8. Grosor tipográfico: [weight:valor]texto[/weight]
        val weightRegex = Regex("""\[weight:([a-zA-Z0-9]+)\](.*?)\[/weight\]""")
        // 9. Color y efecto 3D: [3d:params]texto[/3d] o [3d]texto[/3d]
        val effect3dRegex = Regex("""\[3d(?::([^\]]+))?\](.*?)\[/3d\]""", RegexOption.IGNORE_CASE)

        data class SpanInstruction(val start: Int, val end: Int, val span: Any)
        val instructions = mutableListOf<SpanInstruction>()

        // Aplicamos un parser secuencial paso a paso sobre el texto resultante
        var workingText = clean

        // Funciones auxiliares para buscar y remover tags registrando coordenadas
        fun stripAndRecord(pattern: Regex, createSpan: (String) -> Any) {
            var match = pattern.find(workingText)
            while (match != null) {
                val fullMatch = match.value
                val innerText = if (match.groupValues.size > 2) match.groupValues[2] else match.groupValues[1]
                val span = createSpan(if (match.groupValues.size > 2) match.groupValues[1] else "")
                val start = match.range.first
                val end = start + innerText.length

                workingText = workingText.replaceRange(match.range, innerText)
                instructions.add(SpanInstruction(start, end, span))

                match = pattern.find(workingText)
            }
        }

        // Registrar colores
        stripAndRecord(colorRegex) { hex ->
            try {
                ForegroundColorSpan(Color.parseColor(hex))
            } catch (_: Exception) {
                ForegroundColorSpan(Color.BLACK)
            }
        }

        // Registrar negrita
        stripAndRecord(boldRegex) { StyleSpan(Typeface.BOLD) }

        // Registrar cursiva
        stripAndRecord(italicRegex) { StyleSpan(Typeface.ITALIC) }

        // Registrar subrayado
        stripAndRecord(underlineRegex) { UnderlineSpan() }

        // Registrar tachado
        stripAndRecord(strikeRegex) { StrikethroughSpan() }

        // Registrar superíndice
        stripAndRecord(superRegex) { SuperscriptSpan() }

        // Registrar subíndice
        stripAndRecord(subRegex) { SubscriptSpan() }

        // Registrar grosor tipográfico
        stripAndRecord(weightRegex) { weight ->
            when (weight.lowercase()) {
                "light", "thin" -> StyleSpan(Typeface.NORMAL)
                else -> StyleSpan(Typeface.BOLD)
            }
        }

        // Registrar color y estilos 3D vectoriales con relieve y sombra profunda
        var match3d = effect3dRegex.find(workingText)
        while (match3d != null) {
            val params = match3d.groupValues[1].trim()
            val innerText = match3d.groupValues[2]
            val start = match3d.range.first

            // Deducir shadowColor y frontColor
            var shadowColor = Color.rgb(15, 23, 42) // Carbón profundo
            var frontColor = Color.rgb(234, 88, 12) // Naranja brillante / contraste vivo

            if (params.isNotEmpty()) {
                val parts = params.split(",")
                if (parts.size >= 2) {
                    shadowColor = parseHexColorSafe(parts[0], shadowColor)
                    frontColor = parseHexColorSafe(parts[1], frontColor)
                } else if (parts.size == 1) {
                    val p = parts[0]
                    if (p.contains("front=", ignoreCase = true)) {
                        frontColor = parseHexColorSafe(p.substringAfter("="), frontColor)
                    } else if (p.contains("shadow=", ignoreCase = true)) {
                        shadowColor = parseHexColorSafe(p.substringAfter("="), shadowColor)
                    } else {
                        shadowColor = parseHexColorSafe(p, shadowColor)
                    }
                }
            }

            val bevelColor = blendColors(shadowColor, frontColor, 0.45f)

            workingText = workingText.replaceRange(match3d.range, innerText)

            // Aplicar 3D por token/palabra para permitir saltos de línea fluidos en el StaticLayout de PDF
            val tokens = innerText.split(Regex("(?<=\\s)|(?=\\s)"))
            var currentOffset = start
            for (token in tokens) {
                val tokenEnd = currentOffset + token.length
                if (token.isNotBlank()) {
                    instructions.add(SpanInstruction(currentOffset, tokenEnd, ThreeDWordSpan(shadowColor, bevelColor, frontColor)))
                }
                currentOffset = tokenEnd
            }

            match3d = effect3dRegex.find(workingText)
        }

        ssb.append(workingText)

        for (inst in instructions) {
            val safeStart = inst.start.coerceIn(0, ssb.length)
            val safeEnd = inst.end.coerceIn(safeStart, ssb.length)
            if (safeEnd > safeStart) {
                ssb.setSpan(inst.span, safeStart, safeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        return ssb
    }

    /**
     * Divide los bloques en páginas virtuales estimando la altura de cada bloque
     * para que nunca se desborde el contenido ni se corte la hoja A4.
     */
    private fun paginatePdfBlocks(
        blocks: List<PdfBlock>,
        bodyPaint: TextPaint,
        h1Paint: TextPaint,
        h2Paint: TextPaint,
        h3Paint: TextPaint,
        quotePaint: TextPaint
    ): List<List<PdfBlock>> {
        val pages = mutableListOf<MutableList<PdfBlock>>()
        var currentPage = mutableListOf<PdfBlock>()
        var currentHeight = 0f

        for (block in blocks) {
            if (block is PdfBlock.PageBreak) {
                if (currentPage.isNotEmpty()) {
                    pages.add(currentPage)
                    currentPage = mutableListOf()
                    currentHeight = 0f
                }
                continue
            }

            val blockHeight = estimateBlockHeight(block, bodyPaint, h1Paint, h2Paint, h3Paint, quotePaint)

            if (currentHeight + blockHeight > USABLE_PAGE_HEIGHT && currentPage.isNotEmpty()) {
                pages.add(currentPage)
                currentPage = mutableListOf()
                currentHeight = 0f
            }

            currentPage.add(block)
            currentHeight += blockHeight
        }

        if (currentPage.isNotEmpty() || pages.isEmpty()) {
            pages.add(currentPage)
        }

        return pages
    }

    /**
     * Calcula la altura aproximada de un bloque para la paginación.
     */
    private fun estimateBlockHeight(
        block: PdfBlock,
        bodyPaint: TextPaint,
        h1Paint: TextPaint,
        h2Paint: TextPaint,
        h3Paint: TextPaint,
        quotePaint: TextPaint
    ): Float {
        return when (block) {
            is PdfBlock.Heading -> {
                val paint = when (block.level) {
                    1 -> h1Paint
                    2 -> h2Paint
                    else -> h3Paint
                }
                val layout = StaticLayout.Builder
                    .obtain(block.text, 0, block.text.length, paint, CONTENT_WIDTH)
                    .setAlignment(block.align)
                    .build()
                layout.height + (if (block.level == 1) 22f else 14f)
            }

            is PdfBlock.Paragraph -> {
                val layout = StaticLayout.Builder
                    .obtain(block.text, 0, block.text.length, bodyPaint, CONTENT_WIDTH)
                    .setAlignment(block.align)
                    .setLineSpacing(3f, 1.15f)
                    .build()
                layout.height + 8f
            }

            is PdfBlock.Blockquote -> {
                val layout = StaticLayout.Builder
                    .obtain(block.text, 0, block.text.length, quotePaint, CONTENT_WIDTH - 24)
                    .setAlignment(block.align)
                    .build()
                layout.height + 18f
            }

            is PdfBlock.ListItem -> {
                val layout = StaticLayout.Builder
                    .obtain(block.text, 0, block.text.length, bodyPaint, CONTENT_WIDTH - 18)
                    .build()
                layout.height + 6f
            }

            is PdfBlock.TaskItem -> {
                val layout = StaticLayout.Builder
                    .obtain(block.text, 0, block.text.length, bodyPaint, CONTENT_WIDTH - 20)
                    .build()
                layout.height + 6f
            }

            is PdfBlock.Table -> {
                // Altura estimada de filas de la tabla
                var total = 10f
                for (row in block.rows) {
                    total += 24f // promedio por fila
                }
                total
            }

            is PdfBlock.Shape -> {
                block.heightPt + 16f
            }

            is PdfBlock.Nodes -> {
                if (block.layout == "vertical") {
                    block.nodes.size * 34f + 20f
                } else {
                    48f + 16f
                }
            }

            is PdfBlock.Divider -> 16f
            is PdfBlock.EmptyLine -> 10f
            is PdfBlock.PageBreak -> 0f
        }
    }

    /**
     * Exporta el documento como archivo Markdown (.md).
     */
    fun exportToMarkdown(
        context: Context,
        title: String,
        content: String
    ): Uri? {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = sanitizeFileName(title)
            val outputFile = File(exportDir, "$cleanTitle.md")

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val markdownContent = buildString {
                append("# ").append(title.ifBlank { "Documento sin título" }).append("\n\n")
                append("> *Exportado desde DocuSheet el $dateStr*\n\n")
                append("---\n\n")
                append(content)
                append("\n")
            }

            outputFile.writeText(markdownContent, Charsets.UTF_8)

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Exporta el documento como HTML editorial estructurado (.html).
     * 
     * Genera un documento HTML5 autónomo y responsivo con hoja de estilos CSS moderna,
     * respetando la tipografía elegida, justificado de texto editorial, tablas estilizadas,
     * citas elegantes, encabezados jerárquicos y simulación visual de hoja de papel física.
     */
    fun exportToHtml(
        context: Context,
        title: String,
        content: String,
        fontStyle: String = "SERIF"
    ): Uri? {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = sanitizeFileName(title)
            val outputFile = File(exportDir, "$cleanTitle.html")

            val fontFamilyCss = when (fontStyle) {
                "SANS_SERIF" -> "'Inter', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
                "MONOSPACE" -> "'JetBrains Mono', 'Fira Code', 'Courier New', Courier, monospace"
                "CURSIVE" -> "'Caveat', 'Dancing Script', 'Brush Script MT', cursive"
                else -> "'Merriweather', 'Georgia', 'Cambria', serif"
            }

            val dateStr = SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm", Locale.getDefault()).format(Date())
            val displayTitle = if (title.isBlank()) "Documento DocuSheet" else title

            val htmlBody = convertContentToHtmlBody(content)

            val fullHtml = """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${escapeHtml(displayTitle)}</title>
    <style>
        :root {
            --bg-canvas: #f1f5f9;
            --paper-bg: #ffffff;
            --text-main: #1e293b;
            --text-muted: #64748b;
            --border-color: #e2e8f0;
            --accent-color: #2563eb;
            --paper-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -2px rgba(0,0,0,0.1), 0 0 0 1px rgba(0,0,0,0.05);
        }
        @media (prefers-color-scheme: dark) {
            :root {
                --bg-canvas: #0f172a;
                --paper-bg: #1e293b;
                --text-main: #f8fafc;
                --text-muted: #94a3b8;
                --border-color: #334155;
                --accent-color: #38bdf8;
                --paper-shadow: 0 4px 6px -1px rgba(0,0,0,0.3);
            }
        }
        body {
            margin: 0;
            padding: 24px 16px;
            background-color: var(--bg-canvas);
            color: var(--text-main);
            font-family: $fontFamilyCss;
            line-height: 1.7;
            font-size: 16px;
            -webkit-font-smoothing: antialiased;
        }
        .page-container {
            max-width: 800px;
            margin: 0 auto;
            background: var(--paper-bg);
            padding: 48px 40px;
            border-radius: 8px;
            box-shadow: var(--paper-shadow);
            border: 1px solid var(--border-color);
            position: relative;
        }
        .doc-header {
            border-bottom: 2px solid var(--border-color);
            padding-bottom: 20px;
            margin-bottom: 32px;
        }
        .doc-title {
            font-size: 28px;
            font-weight: 800;
            margin: 0 0 8px 0;
            color: var(--text-main);
            letter-spacing: -0.02em;
        }
        .doc-meta {
            font-size: 13px;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.05em;
            display: flex;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 8px;
        }
        .doc-body {
            word-wrap: break-word;
        }
        h1, h2, h3 {
            color: var(--text-main);
            font-weight: 700;
            margin-top: 1.5em;
            margin-bottom: 0.5em;
            line-height: 1.3;
        }
        h1 { font-size: 24px; border-bottom: 1px solid var(--border-color); padding-bottom: 6px; }
        h2 { font-size: 20px; }
        h3 { font-size: 17px; }
        p {
            margin: 0 0 1.2em 0;
            text-align: justify;
            text-justify: inter-word;
        }
        blockquote {
            margin: 1.5em 0;
            padding: 12px 20px;
            border-left: 4px solid var(--accent-color);
            background: rgba(37, 99, 235, 0.05);
            font-style: italic;
            border-radius: 0 6px 6px 0;
        }
        hr {
            border: 0;
            height: 1px;
            background: var(--border-color);
            margin: 2em 0;
        }
        .page-break {
            margin: 3em 0;
            border-top: 2px dashed var(--border-color);
            text-align: center;
            position: relative;
        }
        .page-break::after {
            content: "— Salto de Página —";
            position: absolute;
            top: -10px;
            left: 50%;
            transform: translateX(-50%);
            background: var(--paper-bg);
            padding: 0 12px;
            font-size: 11px;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.1em;
        }
        ul, ol {
            padding-left: 24px;
            margin-bottom: 1.2em;
        }
        li {
            margin-bottom: 0.4em;
        }
        .doc-footer {
            margin-top: 48px;
            padding-top: 20px;
            border-top: 1px solid var(--border-color);
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: var(--text-muted);
        }
        @media print {
            body {
                background: none;
                padding: 0;
            }
            .page-container {
                box-shadow: none;
                border: none;
                padding: 0;
                max-width: 100%;
            }
            .page-break {
                page-break-after: always;
                border: none;
            }
            .page-break::after {
                display: none;
            }
        }
    </style>
</head>
<body>
    <article class="page-container">
        <header class="doc-header">
            <h1 class="doc-title">${escapeHtml(displayTitle)}</h1>
            <div class="doc-meta">
                <span>DocuSheet Editorial</span>
                <time datetime="${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}">$dateStr</time>
            </div>
        </header>
        <section class="doc-body">
            $htmlBody
        </section>
        <footer class="doc-footer">
            <span>DocuSheet Móvil</span>
            <span>Documento Formal</span>
        </footer>
    </article>
</body>
</html>
            """.trimIndent()

            outputFile.writeText(fullHtml, Charsets.UTF_8)

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Exporta el documento como Documento de Texto Plano (.txt).
     * Universal, ultra-ligero y compatible con cualquier dispositivo o sistema operativo.
     */
    fun exportToPlainText(
        context: Context,
        title: String,
        content: String
    ): Uri? {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = sanitizeFileName(title)
            val outputFile = File(exportDir, "$cleanTitle.txt")

            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val separatorLine = "=".repeat(60)
            val plainText = buildString {
                append(title.ifBlank { "DOCUMENTO SIN TÍTULO" }.uppercase())
                append("\n")
                append(separatorLine)
                append("\nExportado desde DocuSheet | Fecha: $dateStr\n")
                append(separatorLine)
                append("\n\n")
                append(content)
                append("\n\n")
                append(separatorLine)
                append("\nFin del documento - DocuSheet\n")
            }

            outputFile.writeText(plainText, Charsets.UTF_8)

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Exporta el documento como archivo Microsoft Word (.docx).
     * Empaquetado OpenXML nativo compatible con Word, Google Docs y LibreOffice.
     */
    fun exportToDocx(
        context: Context,
        title: String,
        content: String
    ): Uri? {
        return DocxHandler.exportToDocx(context, title, content)
    }

    /**
     * Exporta el documento como archivo Rich Text Format (.rtf).
     * Estándar ofimático ligero y universal para edición en cualquier plataforma.
     */
    fun exportToRtf(
        context: Context,
        title: String,
        content: String
    ): Uri? {
        return RtfHandler.exportToRtf(context, title, content)
    }

    /**
     * Exporta el documento como código fuente LaTeX (.tex).
     * Maquetación científica y académica con tipografía A4 de imprenta.
     */
    fun exportToLatex(
        context: Context,
        title: String,
        content: String
    ): Uri? {
        return LatexHandler.exportToLatex(context, title, content)
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun formatHtmlInline(text: String): String {
        var clean = escapeHtml(text)

        // 1. Efecto 3D con relieve y sombra CSS
        val effect3dRegex = Regex("""\[3d(?::([^,\]]+))?(?:,([^\]]+))?\](.*?)\[/3d\]""", RegexOption.IGNORE_CASE)
        clean = effect3dRegex.replace(clean) { m ->
            val shadow = m.groupValues[1].trim().ifBlank { "#0F172A" }
            val front = m.groupValues[2].trim().ifBlank { "#EA580C" }
            val inner = m.groupValues[3]
            """<span class="text-3d" style="color: $front; font-weight: bold; text-shadow: 1px 1px 0px $shadow, 2px 2px 0px $shadow, 3px 3px 2px rgba(0,0,0,0.45);">${formatHtmlInline(inner)}</span>"""
        }

        // 2. Colores personalizados
        val colorRegex = Regex("""\[color:(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8})\](.*?)\[/color\]""", RegexOption.IGNORE_CASE)
        clean = colorRegex.replace(clean) { m ->
            val hex = m.groupValues[1]
            val inner = m.groupValues[2]
            """<span style="color: $hex;">${formatHtmlInline(inner)}</span>"""
        }

        // 3. Grosor tipográfico
        val weightRegex = Regex("""\[weight:([a-zA-Z0-9]+)\](.*?)\[/weight\]""", RegexOption.IGNORE_CASE)
        clean = weightRegex.replace(clean) { m ->
            val weight = when (m.groupValues[1].lowercase()) {
                "light", "thin" -> "300"
                "medium" -> "500"
                "semibold" -> "600"
                "bold" -> "700"
                "black" -> "900"
                else -> "bold"
            }
            val inner = m.groupValues[2]
            """<span style="font-weight: $weight;">${formatHtmlInline(inner)}</span>"""
        }

        // 4. Formato estándar Markdown
        clean = clean.replace(Regex("""\*\*(.*?)\*\*""")) { "<strong>${it.groupValues[1]}</strong>" }
        clean = clean.replace(Regex("""\*(.*?)\*""")) { "<em>${it.groupValues[1]}</em>" }
        clean = clean.replace(Regex("""&lt;u&gt;(.*?)&lt;/u&gt;""")) { "<u>${it.groupValues[1]}</u>" }
        clean = clean.replace(Regex("""<u>(.*?)</u>""")) { "<u>${it.groupValues[1]}</u>" }
        clean = clean.replace(Regex("""~~(.*?)~~""")) { "<del>${it.groupValues[1]}</del>" }
        clean = clean.replace(Regex("""&lt;sup&gt;(.*?)&lt;/sup&gt;""")) { "<sup>${it.groupValues[1]}</sup>" }
        clean = clean.replace(Regex("""&lt;sub&gt;(.*?)&lt;/sub&gt;""")) { "<sub>${it.groupValues[1]}</sub>" }

        return clean
    }

    private fun convertContentToHtmlBody(rawContent: String): String {
        val lines = rawContent.lines()
        val sb = StringBuilder()
        var inList = false
        var listType = "" // "ul" or "ol"

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == "[--- Salto de Página ---]" || trimmed == "---") {
                if (inList) {
                    sb.append("</$listType>\n")
                    inList = false
                }
                sb.append("<div class=\"page-break\"></div>\n")
                continue
            }

            if (trimmed.startsWith("# ")) {
                if (inList) { sb.append("</$listType>\n"); inList = false }
                sb.append("<h1>").append(formatHtmlInline(trimmed.removePrefix("# "))).append("</h1>\n")
            } else if (trimmed.startsWith("## ")) {
                if (inList) { sb.append("</$listType>\n"); inList = false }
                sb.append("<h2>").append(formatHtmlInline(trimmed.removePrefix("## "))).append("</h2>\n")
            } else if (trimmed.startsWith("### ")) {
                if (inList) { sb.append("</$listType>\n"); inList = false }
                sb.append("<h3>").append(formatHtmlInline(trimmed.removePrefix("### "))).append("</h3>\n")
            } else if (trimmed.startsWith("> ")) {
                if (inList) { sb.append("</$listType>\n"); inList = false }
                sb.append("<blockquote>").append(formatHtmlInline(trimmed.removePrefix("> "))).append("</blockquote>\n")
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                if (!inList || listType != "ul") {
                    if (inList) sb.append("</$listType>\n")
                    sb.append("<ul>\n")
                    inList = true
                    listType = "ul"
                }
                val itemContent = trimmed.removePrefix("- ").removePrefix("• ")
                sb.append("  <li>").append(formatHtmlInline(itemContent)).append("</li>\n")
            } else if (trimmed.matches(Regex("^\\d+\\.\\s.*"))) {
                if (!inList || listType != "ol") {
                    if (inList) sb.append("</$listType>\n")
                    sb.append("<ol>\n")
                    inList = true
                    listType = "ol"
                }
                val itemContent = trimmed.replaceFirst(Regex("^\\d+\\.\\s*"), "")
                sb.append("  <li>").append(formatHtmlInline(itemContent)).append("</li>\n")
            } else if (trimmed.isEmpty()) {
                if (inList) {
                    sb.append("</$listType>\n")
                    inList = false
                }
                // Línea vacía: espacio entre párrafos
            } else {
                if (inList) {
                    sb.append("</$listType>\n")
                    inList = false
                }
                sb.append("<p>").append(formatHtmlInline(trimmed)).append("</p>\n")
            }
        }
        if (inList) {
            sb.append("</$listType>\n")
        }
        return sb.toString()
    }

    /**
     * Lanza el menú del sistema Android para compartir o guardar el archivo exportado.
     */
    fun shareExportedFile(
        context: Context,
        uri: Uri,
        mimeType: String,
        title: String
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Exportar $title")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun sanitizeFileName(name: String): String {
        val sanitized = name.trim().replace(Regex("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ_\\-\\s]"), "")
        return if (sanitized.isBlank()) "Documento_DocuSheet" else sanitized.replace("\\s+".toRegex(), "_")
    }
}
