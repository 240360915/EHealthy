package ehealthy.connect.ui.patientDashboard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Base64
import java.io.OutputStream

/**
 * Creates the official patient prescription PDF completely on-device.
 *
 * The PDF includes:
 * - EHealthy branding
 * - patient details
 * - doctor details
 * - prescription reference
 * - medication / dosage / frequency / duration / instructions
 * - doctor signature
 * - doctor stamp
 *
 * No card/payment/secret data is involved.
 */
object PrescriptionPdfExporter {

    private const val PAGE_WIDTH =
        595

    private const val PAGE_HEIGHT =
        842

    private const val LEFT =
        46f

    private const val RIGHT =
        549f

    private const val CONTENT_WIDTH =
        RIGHT - LEFT


    fun writePrescriptionPdf(
        prescription: Prescription,
        fallbackDoctorName: String?,
        output: OutputStream
    ) {

        val pdf =
            PdfDocument()

        var pageNumber =
            0

        var page =
            createPage(
                pdf =
                    pdf,
                pageNumber =
                    ++pageNumber
            )

        var canvas =
            page.canvas

        var y =
            drawHeader(
                canvas =
                    canvas,
                prescription =
                    prescription
            )


        y =
            drawPatientSection(
                canvas =
                    canvas,
                prescription =
                    prescription,
                startY =
                    y
            )


        y =
            drawDoctorSection(
                canvas =
                    canvas,
                prescription =
                    prescription,
                fallbackDoctorName =
                    fallbackDoctorName,
                startY =
                    y
            )


        y +=
            10f


        val items =
            prescription
                .prescription_items
                ?.takeIf {
                    it.isNotEmpty()
                }
                ?: listOf(
                    PrescriptionItem(
                        medication =
                            prescription.medication
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Medication",
                        dosage =
                            "As prescribed",
                        frequency =
                            "As prescribed",
                        duration =
                            "As prescribed",
                        instructions =
                            null
                    )
                )


        y =
            drawSectionTitle(
                canvas =
                    canvas,
                title =
                    "PRESCRIBED MEDICATION",
                startY =
                    y
            )


        items.forEachIndexed {
                index,
                item ->

            val estimatedHeight =
                estimateMedicationHeight(
                    item
                )


            if (
                y +
                estimatedHeight >
                PAGE_HEIGHT -
                150f
            ) {

                drawFooter(
                    canvas =
                        canvas,
                    prescription =
                        prescription,
                    pageNumber =
                        pageNumber
                )

                pdf.finishPage(
                    page
                )

                page =
                    createPage(
                        pdf =
                            pdf,
                        pageNumber =
                            ++pageNumber
                    )

                canvas =
                    page.canvas

                y =
                    drawContinuationHeader(
                        canvas =
                            canvas,
                        prescription =
                            prescription
                    )

                y =
                    drawSectionTitle(
                        canvas =
                            canvas,
                        title =
                            "PRESCRIBED MEDICATION - CONTINUED",
                        startY =
                            y
                    )
            }


            y =
                drawMedicationItem(
                    canvas =
                        canvas,
                    item =
                        item,
                    number =
                        index +
                                1,
                    startY =
                        y
                )

            y +=
                8f
        }


        prescription
            .general_instructions
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let { instructions ->

                if (
                    y >
                    PAGE_HEIGHT -
                    270f
                ) {

                    drawFooter(
                        canvas =
                            canvas,
                        prescription =
                            prescription,
                        pageNumber =
                            pageNumber
                    )

                    pdf.finishPage(
                        page
                    )

                    page =
                        createPage(
                            pdf =
                                pdf,
                            pageNumber =
                                ++pageNumber
                        )

                    canvas =
                        page.canvas

                    y =
                        drawContinuationHeader(
                            canvas =
                                canvas,
                            prescription =
                                prescription
                        )
                }


                y =
                    drawSectionTitle(
                        canvas =
                            canvas,
                        title =
                            "GENERAL INSTRUCTIONS",
                        startY =
                            y +
                                    6f
                    )


                y =
                    drawWrappedText(
                        canvas =
                            canvas,
                        text =
                            instructions,
                        x =
                            LEFT,
                        y =
                            y,
                        maxWidth =
                            CONTENT_WIDTH,
                        paint =
                            bodyPaint(),
                        lineHeight =
                            17f
                    ) +
                            14f
            }


        if (
            y >
            PAGE_HEIGHT -
            230f
        ) {

            drawFooter(
                canvas =
                    canvas,
                prescription =
                    prescription,
                pageNumber =
                    pageNumber
            )

            pdf.finishPage(
                page
            )

            page =
                createPage(
                    pdf =
                        pdf,
                    pageNumber =
                        ++pageNumber
                )

            canvas =
                page.canvas

            y =
                drawContinuationHeader(
                    canvas =
                        canvas,
                    prescription =
                        prescription
                )
        }


        drawSignatureAndStamp(
            canvas =
                canvas,
            prescription =
                prescription,
            startY =
                y +
                        10f
        )


        drawFooter(
            canvas =
                canvas,
            prescription =
                prescription,
            pageNumber =
                pageNumber
        )


        pdf.finishPage(
            page
        )


        pdf.writeTo(
            output
        )

        pdf.close()
    }


    private fun createPage(
        pdf: PdfDocument,
        pageNumber: Int
    ): PdfDocument.Page {

        val info =
            PdfDocument
                .PageInfo
                .Builder(
                    PAGE_WIDTH,
                    PAGE_HEIGHT,
                    pageNumber
                )
                .create()

        return pdf.startPage(
            info
        )
    }


    private fun drawHeader(
        canvas: Canvas,
        prescription: Prescription
    ): Float {

        val headerPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        18,
                        73,
                        126
                    )
            }


        canvas.drawRect(
            0f,
            0f,
            PAGE_WIDTH.toFloat(),
            115f,
            headerPaint
        )


        // EHealthy logo mark
        val markPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        20,
                        184,
                        166
                    )
            }


        canvas.drawRoundRect(
            RectF(
                LEFT,
                27f,
                LEFT +
                        54f,
                81f
            ),
            13f,
            13f,
            markPaint
        )


        val crossPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.WHITE
            }


        canvas.drawRoundRect(
            RectF(
                LEFT +
                        23f,
                37f,
                LEFT +
                        31f,
                71f
            ),
            3f,
            3f,
            crossPaint
        )


        canvas.drawRoundRect(
            RectF(
                LEFT +
                        10f,
                50f,
                LEFT +
                        44f,
                58f
            ),
            3f,
            3f,
            crossPaint
        )


        val logoPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.WHITE

                textSize =
                    24f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        canvas.drawText(
            "EHealthy",
            LEFT +
                    68f,
            53f,
            logoPaint
        )


        val subLogo =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        207,
                        231,
                        255
                    )

                textSize =
                    10.5f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.NORMAL
                    )
            }


        canvas.drawText(
            "CONNECT - DIGITAL HEALTHCARE",
            LEFT +
                    69f,
            71f,
            subLogo
        )


        val titlePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.WHITE

                textSize =
                    18f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )

                textAlign =
                    Paint.Align.RIGHT
            }


        canvas.drawText(
            "PRESCRIPTION",
            RIGHT,
            48f,
            titlePaint
        )


        val smallRight =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        219,
                        234,
                        254
                    )

                textSize =
                    9.5f

                textAlign =
                    Paint.Align.RIGHT
            }


        val reference =
            prescription
                .prescription_reference
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: prescription.id


        canvas.drawText(
            "Ref: $reference",
            RIGHT,
            67f,
            smallRight
        )


        canvas.drawText(
            formatDate(
                prescription.created_at
            ),
            RIGHT,
            83f,
            smallRight
        )


        return 140f
    }


    private fun drawContinuationHeader(
        canvas: Canvas,
        prescription: Prescription
    ): Float {

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        18,
                        73,
                        126
                    )
            }


        canvas.drawRect(
            0f,
            0f,
            PAGE_WIDTH.toFloat(),
            67f,
            paint
        )


        val title =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.WHITE

                textSize =
                    17f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        canvas.drawText(
            "EHealthy - Prescription",
            LEFT,
            35f,
            title
        )


        val refPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.WHITE

                textSize =
                    9f

                textAlign =
                    Paint.Align.RIGHT
            }


        canvas.drawText(
            prescription
                .prescription_reference
                ?: prescription.id,
            RIGHT,
            35f,
            refPaint
        )


        return 92f
    }


    private fun drawPatientSection(
        canvas: Canvas,
        prescription: Prescription,
        startY: Float
    ): Float {

        var y =
            drawSectionTitle(
                canvas =
                    canvas,
                title =
                    "PATIENT",
                startY =
                    startY
            )


        val name =
            prescription
                .patient_name
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Patient"


        y =
            drawLabelValue(
                canvas =
                    canvas,
                label =
                    "Patient name",
                value =
                    name,
                y =
                    y
            )


        y =
            drawLabelValue(
                canvas =
                    canvas,
                label =
                    "Patient reference",
                value =
                    prescription.patient_id,
                y =
                    y
            )


        return y +
                10f
    }


    private fun drawDoctorSection(
        canvas: Canvas,
        prescription: Prescription,
        fallbackDoctorName: String?,
        startY: Float
    ): Float {

        var y =
            drawSectionTitle(
                canvas =
                    canvas,
                title =
                    "PRESCRIBING DOCTOR",
                startY =
                    startY
            )


        val doctorName =
            prescription
                .doctor_name
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: fallbackDoctorName
                ?: "Doctor"


        y =
            drawLabelValue(
                canvas =
                    canvas,
                label =
                    "Doctor",
                value =
                    doctorName,
                y =
                    y
            )


        prescription
            .doctor_discipline
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {

                y =
                    drawLabelValue(
                        canvas =
                            canvas,
                        label =
                            "Discipline",
                        value =
                            it,
                        y =
                            y
                    )
            }


        prescription
            .doctor_practice_number
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {

                y =
                    drawLabelValue(
                        canvas =
                            canvas,
                        label =
                            "Practice no.",
                        value =
                            it,
                        y =
                            y
                    )
            }


        prescription
            .doctor_hpcsa_number
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {

                y =
                    drawLabelValue(
                        canvas =
                            canvas,
                        label =
                            "HPCSA no.",
                        value =
                            it,
                        y =
                            y
                    )
            }


        return y
    }


    private fun drawSectionTitle(
        canvas: Canvas,
        title: String,
        startY: Float
    ): Float {

        val background =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        239,
                        246,
                        255
                    )
            }


        canvas.drawRoundRect(
            RectF(
                LEFT,
                startY,
                RIGHT,
                startY +
                        28f
            ),
            7f,
            7f,
            background
        )


        val titlePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        30,
                        64,
                        175
                    )

                textSize =
                    10.5f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        canvas.drawText(
            title,
            LEFT +
                    10f,
            startY +
                    18f,
            titlePaint
        )


        return startY +
                42f
    }


    private fun drawLabelValue(
        canvas: Canvas,
        label: String,
        value: String,
        y: Float
    ): Float {

        val labelPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        100,
                        116,
                        139
                    )

                textSize =
                    9f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        val valuePaint =
            bodyPaint()


        canvas.drawText(
            label,
            LEFT,
            y,
            labelPaint
        )


        canvas.drawText(
            value,
            LEFT +
                    115f,
            y,
            valuePaint
        )


        return y +
                20f
    }


    private fun drawMedicationItem(
        canvas: Canvas,
        item: PrescriptionItem,
        number: Int,
        startY: Float
    ): Float {

        val boxPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        248,
                        250,
                        252
                    )

                style =
                    Paint.Style.FILL
            }


        val borderPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        226,
                        232,
                        240
                    )

                style =
                    Paint.Style.STROKE

                strokeWidth =
                    1f
            }


        val itemHeight =
            estimateMedicationHeight(
                item
            )


        val rect =
            RectF(
                LEFT,
                startY,
                RIGHT,
                startY +
                        itemHeight
            )


        canvas.drawRoundRect(
            rect,
            10f,
            10f,
            boxPaint
        )


        canvas.drawRoundRect(
            rect,
            10f,
            10f,
            borderPaint
        )


        val numberPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        20,
                        184,
                        166
                    )

                textSize =
                    12f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        canvas.drawText(
            "$number.",
            LEFT +
                    12f,
            startY +
                    22f,
            numberPaint
        )


        val medicationPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        15,
                        23,
                        42
                    )

                textSize =
                    12.5f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        var y =
            drawWrappedText(
                canvas =
                    canvas,
                text =
                    item.medication,
                x =
                    LEFT +
                            38f,
                y =
                    startY +
                            22f,
                maxWidth =
                    CONTENT_WIDTH -
                            50f,
                paint =
                    medicationPaint,
                lineHeight =
                    16f
            )


        y +=
            7f


        val detail =
            "Dosage: ${item.dosage.ifBlank { "Not specified" }}    " +
                    "Frequency: ${item.frequency.ifBlank { "Not specified" }}"


        y =
            drawWrappedText(
                canvas =
                    canvas,
                text =
                    detail,
                x =
                    LEFT +
                            38f,
                y =
                    y,
                maxWidth =
                    CONTENT_WIDTH -
                            50f,
                paint =
                    smallBodyPaint(),
                lineHeight =
                    14f
            )


        y =
            drawWrappedText(
                canvas =
                    canvas,
                text =
                    "Duration: ${item.duration.ifBlank { "Not specified" }}",
                x =
                    LEFT +
                            38f,
                y =
                    y +
                            3f,
                maxWidth =
                    CONTENT_WIDTH -
                            50f,
                paint =
                    smallBodyPaint(),
                lineHeight =
                    14f
            )


        item.instructions
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {

                drawWrappedText(
                    canvas =
                        canvas,
                    text =
                        "Instructions: $it",
                    x =
                        LEFT +
                                38f,
                    y =
                        y +
                                3f,
                    maxWidth =
                        CONTENT_WIDTH -
                                50f,
                    paint =
                        smallBodyPaint(),
                    lineHeight =
                        14f
                )
            }


        return startY +
                itemHeight
    }


    private fun estimateMedicationHeight(
        item: PrescriptionItem
    ): Float {

        val instructionExtra =
            if (
                item.instructions
                    .isNullOrBlank()
            ) {
                0f
            } else {
                32f
            }

        val medicationExtra =
            if (
                item.medication.length >
                52
            ) {
                18f
            } else {
                0f
            }

        return 91f +
                instructionExtra +
                medicationExtra
    }


    private fun drawSignatureAndStamp(
        canvas: Canvas,
        prescription: Prescription,
        startY: Float
    ) {

        val signature =
            decodeDataImage(
                prescription
                    .doctor_signature_data
            )

        val stamp =
            decodeDataImage(
                prescription
                    .doctor_stamp_data
            )


        val divider =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        203,
                        213,
                        225
                    )

                strokeWidth =
                    1f
            }


        canvas.drawLine(
            LEFT,
            startY,
            RIGHT,
            startY,
            divider
        )


        val heading =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        15,
                        23,
                        42
                    )

                textSize =
                    10f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }


        canvas.drawText(
            "DOCTOR SIGNATURE",
            LEFT,
            startY +
                    23f,
            heading
        )


        canvas.drawText(
            "DOCTOR STAMP",
            LEFT +
                    300f,
            startY +
                    23f,
            heading
        )


        signature
            ?.let {

                drawBitmapFitCenter(
                    canvas =
                        canvas,
                    bitmap =
                        it,
                    bounds =
                        RectF(
                            LEFT,
                            startY +
                                    30f,
                            LEFT +
                                    210f,
                            startY +
                                    98f
                        )
                )
            }


        stamp
            ?.let {

                drawBitmapFitCenter(
                    canvas =
                        canvas,
                    bitmap =
                        it,
                    bounds =
                        RectF(
                            LEFT +
                                    300f,
                            startY +
                                    30f,
                            RIGHT,
                            startY +
                                    112f
                        )
                )
            }


        val linePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        71,
                        85,
                        105
                    )

                strokeWidth =
                    0.8f
            }


        canvas.drawLine(
            LEFT,
            startY +
                    106f,
            LEFT +
                    210f,
            startY +
                    106f,
            linePaint
        )


        val label =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        100,
                        116,
                        139
                    )

                textSize =
                    8.5f
            }


        canvas.drawText(
            "Prescribing practitioner's signature",
            LEFT,
            startY +
                    120f,
            label
        )


        canvas.drawText(
            "Official practice stamp",
            LEFT +
                    300f,
            startY +
                    120f,
            label
        )
    }


    private fun drawFooter(
        canvas: Canvas,
        prescription: Prescription,
        pageNumber: Int
    ) {

        val linePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        226,
                        232,
                        240
                    )
            }


        canvas.drawRect(
            LEFT,
            PAGE_HEIGHT -
                    53f,
            RIGHT,
            PAGE_HEIGHT -
                    52f,
            linePaint
        )


        val footer =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        100,
                        116,
                        139
                    )

                textSize =
                    8f
            }


        canvas.drawText(
            "Issued electronically through EHealthy Connect. Verify prescription details with the prescribing practitioner.",
            LEFT,
            PAGE_HEIGHT -
                    35f,
            footer
        )


        val right =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                color =
                    Color.rgb(
                        100,
                        116,
                        139
                    )

                textSize =
                    8f

                textAlign =
                    Paint.Align.RIGHT
            }


        canvas.drawText(
            "Page $pageNumber",
            RIGHT,
            PAGE_HEIGHT -
                    19f,
            right
        )


        canvas.drawText(
            prescription
                .prescription_reference
                ?: prescription.id,
            LEFT,
            PAGE_HEIGHT -
                    19f,
            footer
        )
    }


    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float
    ): Float {

        var currentY =
            y

        val paragraphs =
            text
                .split(
                    "\n"
                )


        paragraphs.forEach { paragraph ->

            val words =
                paragraph
                    .split(
                        " "
                    )

            var line =
                ""


            words.forEach { word ->

                val candidate =
                    if (
                        line.isBlank()
                    ) {
                        word
                    } else {
                        "$line $word"
                    }


                if (
                    paint.measureText(
                        candidate
                    ) <=
                    maxWidth
                ) {

                    line =
                        candidate

                } else {

                    if (
                        line.isNotBlank()
                    ) {

                        canvas.drawText(
                            line,
                            x,
                            currentY,
                            paint
                        )

                        currentY +=
                            lineHeight
                    }

                    line =
                        word
                }
            }


            if (
                line.isNotBlank()
            ) {

                canvas.drawText(
                    line,
                    x,
                    currentY,
                    paint
                )

                currentY +=
                    lineHeight
            }
        }


        return currentY
    }


    private fun drawBitmapFitCenter(
        canvas: Canvas,
        bitmap: Bitmap,
        bounds: RectF
    ) {

        val scale =
            minOf(
                bounds.width() /
                        bitmap.width,
                bounds.height() /
                        bitmap.height
            )


        val width =
            bitmap.width *
                    scale

        val height =
            bitmap.height *
                    scale


        val left =
            bounds.left +
                    (
                            bounds.width() -
                                    width
                            ) /
                    2f

        val top =
            bounds.top +
                    (
                            bounds.height() -
                                    height
                            ) /
                    2f


        canvas.drawBitmap(
            bitmap,
            null,
            RectF(
                left,
                top,
                left +
                        width,
                top +
                        height
            ),
            null
        )
    }


    private fun decodeDataImage(
        dataUrl: String?
    ): Bitmap? {

        if (
            dataUrl.isNullOrBlank()
        ) {
            return null
        }


        return runCatching {

            val encoded =
                dataUrl
                    .substringAfter(
                        ",",
                        ""
                    )


            if (
                encoded.isBlank()
            ) {
                return@runCatching null
            }


            val bytes =
                Base64.decode(
                    encoded,
                    Base64.DEFAULT
                )


            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size
            )

        }.getOrNull()
    }


    private fun bodyPaint(): Paint {

        return Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {

            color =
                Color.rgb(
                    30,
                    41,
                    59
                )

            textSize =
                10.5f
        }
    }


    private fun smallBodyPaint(): Paint {

        return Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {

            color =
                Color.rgb(
                    71,
                    85,
                    105
                )

            textSize =
                9.5f
        }
    }


    private fun formatDate(
        raw: String?
    ): String {

        if (
            raw.isNullOrBlank()
        ) {
            return "Date not available"
        }


        return raw
            .substringBefore(
                "T"
            )
    }
}
