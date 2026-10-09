package ehealthy.connect.ui.patientDashboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import ehealthy.connect.R
import ehealthy.connect.data.patient.PatientDoctorSummary
import ehealthy.connect.data.patient.PhysicalVisitInvoice
import java.io.OutputStream
import java.util.Locale

/**
 * Exports a physical-visit invoice using existing, server-issued invoice values.
 * The document is an INVOICE, not a payment receipt or confirmation of payment.
 * No payment details or client-calculated charges are introduced.
 */
object InvoicePdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val LEFT = 46f
    private const val RIGHT = 549f
    private const val CONTENT_WIDTH = RIGHT - LEFT
    private const val FOOTER_TOP = 782f

    private val Navy = Color.rgb(18, 73, 126)
    private val Blue = Color.rgb(20, 143, 186)
    private val TextDark = Color.rgb(30, 41, 59)
    private val TextMuted = Color.rgb(100, 116, 139)
    private val PaleBlue = Color.rgb(239, 246, 255)

    fun writeInvoicePdf(
        context: Context,
        invoice: PhysicalVisitInvoice,
        doctor: PatientDoctorSummary?,
        output: OutputStream
    ) {
        val logo = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo)
        }.getOrNull()

        val pdf = PdfDocument()
        try {
            var pageNumber = 1
            var page = startPage(pdf, pageNumber)
            var canvas = page.canvas
            var y = drawHeader(canvas, invoice, logo, continued = false)

            fun nextPage() {
                drawFooter(canvas, invoice, pageNumber)
                pdf.finishPage(page)
                pageNumber += 1
                page = startPage(pdf, pageNumber)
                canvas = page.canvas
                y = drawHeader(canvas, invoice, logo, continued = true)
            }

            fun ensureSpace(needed: Float) {
                if (y + needed > FOOTER_TOP - 17f) nextPage()
            }

            fun section(title: String) {
                ensureSpace(50f)
                y += 8f
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PaleBlue }
                canvas.drawRoundRect(RectF(LEFT, y, RIGHT, y + 28f), 7f, 7f, fill)
                val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Blue }
                canvas.drawRoundRect(RectF(LEFT, y, LEFT + 3f, y + 28f), 1.5f, 1.5f, accent)
                canvas.drawText(title, LEFT + 13f, y + 19f, paint(Navy, 10.5f, bold = true))
                y += 43f
            }

            fun field(label: String, value: String?) {
                if (value.isNullOrBlank()) return
                val labelPaint = paint(TextMuted, 9f, bold = true)
                val valuePaint = paint(TextDark, 10.5f)
                val labelColumn = 124f
                val x = LEFT + labelColumn
                val lines = wrapText(value.trim(), valuePaint, CONTENT_WIDTH - labelColumn)
                val needed = maxOf(21f, lines.size * 15f + 6f)
                ensureSpace(needed)
                canvas.drawText(label, LEFT, y, labelPaint)
                for ((index, line) in lines.withIndex()) {
                    canvas.drawText(line, x, y + index * 15f, valuePaint)
                }
                y += needed
            }

            fun paragraph(value: String?) {
                if (value.isNullOrBlank()) return
                val paragraphPaint = paint(TextDark, 10.5f)
                val lines = wrapText(value.trim(), paragraphPaint, CONTENT_WIDTH - 20f)
                for (line in lines) {
                    ensureSpace(16f)
                    canvas.drawText(line, LEFT + 10f, y, paragraphPaint)
                    y += 15f
                }
                y += 3f
            }

            section("INVOICE DETAILS")
            field("Invoice reference", invoice.id)
            field("Issued on", formatDate(invoice.issued_at))
            field("Status", displayStatus(invoice.status))
            invoice.accepted_at?.let { field("Accepted on", formatDate(it)) }
            invoice.declined_at?.let { field("Declined on", formatDate(it)) }

            section("PATIENT AND PRACTITIONER")
            field("Patient", invoice.patient_name)
            field("Doctor", doctor?.fullName ?: "Doctor details unavailable")
            doctor?.discipline?.let { field("Specialty", it) }
            doctor?.phone?.let { field("Doctor telephone", it) }

            section("PHYSICAL VISIT ARRANGEMENT")
            field("Service", invoice.service_description)
            field("Appointment date", formatDate(invoice.proposed_date))
            field("Appointment time", formatTime(invoice.proposed_time))
            field("Meeting place", invoice.location_name?.takeIf { it.isNotBlank() } ?: "Doctor practice")
            field("Address", invoice.location)
            if (invoice.location_latitude != null && invoice.location_longitude != null) {
                field(
                    "Coordinates",
                    String.format(
                        Locale.US, "%.6f, %.6f",
                        invoice.location_latitude, invoice.location_longitude
                    )
                )
            }
            if (!invoice.notes.isNullOrBlank()) {
                section("DOCTOR'S NOTES")
                paragraph(invoice.notes)
            }

            section("AMOUNT INVOICED")
            ensureSpace(94f)
            val amountRect = RectF(LEFT, y, RIGHT, y + 65f)
            canvas.drawRoundRect(
                amountRect, 12f, 12f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PaleBlue }
            )
            canvas.drawText(
                "TOTAL INVOICED", LEFT + 16f, y + 22f,
                paint(TextMuted, 10f, bold = true)
            )
            canvas.drawText(
                formattedAmount(invoice), LEFT + 16f, y + 50f,
                paint(Navy, 21f, bold = true)
            )
            y += 79f
            paragraph("Invoice acceptance is not proof of payment. No payment or tax amount is inferred by this PDF.")

            drawFooter(canvas, invoice, pageNumber)
            pdf.finishPage(page)
            pdf.writeTo(output)
        } finally {
            pdf.close()
        }
    }

    private fun startPage(pdf: PdfDocument, pageNumber: Int): PdfDocument.Page {
        return pdf.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        )
    }

    private fun drawHeader(
        canvas: Canvas,
        invoice: PhysicalVisitInvoice,
        logo: Bitmap?,
        continued: Boolean
    ): Float {
        // White background blends with a white-backed brand logo.
        canvas.drawColor(Color.WHITE)
        val top = if (continued) 14f else 18f
        val bottom = if (continued) 75f else 92f
        if (logo != null) {
            drawBitmapFitCenter(canvas, logo, RectF(LEFT, top, LEFT + 205f, bottom))
        } else {
            canvas.drawText("EHealthy", LEFT, 56f, paint(Navy, 23f, bold = true))
        }

        val titleY = if (continued) 36f else 45f
        val referenceY = if (continued) 54f else 65f
        val dateY = if (continued) 68f else 80f
        val title = paint(Navy, if (continued) 15f else 17f, bold = true).apply {
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("INVOICE", RIGHT, titleY, title)
        val small = paint(TextMuted, 9f).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Ref: ${invoice.id.take(12)}", RIGHT, referenceY, small)
        if (!continued) {
            canvas.drawText(formatDate(invoice.issued_at), RIGHT, dateY, small)
        }
        val dividerY = if (continued) 92f else 112f
        canvas.drawLine(
            LEFT, dividerY, RIGHT, dividerY,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Blue
                strokeWidth = 2f
            }
        )
        return dividerY + 27f
    }

    private fun drawFooter(canvas: Canvas, invoice: PhysicalVisitInvoice, page: Int) {
        canvas.drawLine(
            LEFT, FOOTER_TOP, RIGHT, FOOTER_TOP,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }
        )
        canvas.drawText(
            "EHealthy Demo invoice | Not proof of payment", LEFT, FOOTER_TOP + 18f,
            paint(TextMuted, 8.5f)
        )
        canvas.drawText(
            "${invoice.id.take(8)}  |  Page $page", RIGHT, FOOTER_TOP + 18f,
            paint(TextMuted, 8.5f).apply { textAlign = Paint.Align.RIGHT }
        )
    }

    private fun formattedAmount(invoice: PhysicalVisitInvoice): String {
        val amount = invoice.amount_minor / 100.0
        return if (invoice.currency.equals("ZAR", true)) {
            "R ${String.format(Locale.US, "%,.2f", amount)}"
        } else {
            "${invoice.currency} ${String.format(Locale.US, "%,.2f", amount)}"
        }
    }

    private fun displayStatus(status: String): String = when (status.lowercase(Locale.ROOT)) {
        "sent" -> "Awaiting patient response"
        "accepted" -> "Accepted (not a payment confirmation)"
        "declined" -> "Declined"
        "cancelled", "canceled" -> "Cancelled"
        else -> status.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
    }

    private fun formatDate(value: String): String {
        return value.substringBefore('T').ifBlank { "Not specified" }
    }

    private fun formatTime(value: String): String {
        val bits = value.take(5).split(':')
        if (bits.size != 2) return value
        val hour = bits[0].toIntOrNull() ?: return value
        val minute = bits[1].toIntOrNull() ?: return value
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, period)
    }

    private fun paint(colorValue: Int, size: Float, bold: Boolean = false): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorValue
            textSize = size
            typeface = Typeface.create(
                Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL
            )
        }
    }

    /** Split text into printable lines that fit the available width. */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        for (paragraph in text.replace("\r", "").split('\n')) {
            var current = ""
            for (word in paragraph.split(Regex("\\s+")).filter { it.isNotEmpty() }) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current = candidate
                } else {
                    if (current.isNotEmpty()) result.add(current)
                    current = word
                    if (paint.measureText(current) > maxWidth) {
                        var chunk = ""
                        for (char in current) {
                            val tryChunk = chunk + char
                            if (paint.measureText(tryChunk) > maxWidth && chunk.isNotEmpty()) {
                                result.add(chunk)
                                chunk = char.toString()
                            } else {
                                chunk = tryChunk
                            }
                        }
                        current = chunk
                    }
                }
            }
            if (current.isNotEmpty()) result.add(current)
        }
        return if (result.isEmpty()) listOf("") else result
    }

    private fun drawBitmapFitCenter(canvas: Canvas, bitmap: Bitmap, bounds: RectF) {
        if (bitmap.width <= 0 || bitmap.height <= 0) return
        val scale = minOf(bounds.width() / bitmap.width, bounds.height() / bitmap.height)
        val width = bitmap.width * scale
        val height = bitmap.height * scale
        val x = bounds.left + (bounds.width() - width) / 2f
        val y = bounds.top + (bounds.height() - height) / 2f
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
        canvas.drawBitmap(bitmap, null, RectF(x, y, x + width, y + height), bitmapPaint)
    }
}
