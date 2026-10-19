package com.example.wellkillsheet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 30f
    private const val ROW_HEIGHT = 16f

    fun generate(context: Context, input: WellKillInput, outputStream: OutputStream) {
        val document = PdfDocument()

        val titlePaint = Paint().apply {
            color = Color.BLACK; textSize = 16f; isAntiAlias = true; isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            color = Color.BLACK; textSize = 9f; isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK; textSize = 9f; isAntiAlias = true; isFakeBoldText = true
        }
        val redPaint = Paint().apply {
            color = Color.rgb(200, 0, 0); textSize = 10f; isAntiAlias = true; isFakeBoldText = true
        }
        val linePaint = Paint().apply {
            color = Color.BLACK; strokeWidth = 0.6f; style = Paint.Style.STROKE
        }
        val fillGrayPaint = Paint().apply {
            color = Color.rgb(230, 230, 230); style = Paint.Style.FILL
        }
        val fillWhitePaint = Paint().apply {
            color = Color.WHITE; style = Paint.Style.FILL
        }
        val arrowPaint = Paint().apply {
            color = Color.BLACK; strokeWidth = 1f; style = Paint.Style.FILL_AND_STROKE; isAntiAlias = true
        }

        // ==============================
        //        СТРАНИЦА 1
        // ==============================
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        var canvas = page.canvas
        var y = MARGIN + 10f

        // Заголовок
        canvas.drawText("Лист глушения", MARGIN, y, titlePaint)
        y += 25f
        canvas.drawText("Дата: ${input.date}", MARGIN, y, textPaint)
        canvas.drawText("ФИО: ${input.engineer}", MARGIN + 250f, y, textPaint)
        y += 15f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 10f

        // === СХЕМА СКВАЖИНЫ (слева) ===
        val schemeLeft = MARGIN
        val schemeTop = y
        val schemeWidth = 140f
        val schemeHeight = 280f
        drawWellScheme(canvas, schemeLeft, schemeTop, schemeWidth, schemeHeight, linePaint, textPaint, arrowPaint)

        // === ПРАВАЯ КОЛОНКА (Прочность и Текущие) ===
        val rightX = schemeLeft + schemeWidth + 10f
        val rightWidth = PAGE_WIDTH - MARGIN - rightX
        var rightY = schemeTop

        // Блок 1: Прочность
        canvas.drawRect(RectF(rightX, rightY, rightX + rightWidth, rightY + ROW_HEIGHT), fillGrayPaint)
        canvas.drawText("Данные о прочности пород:", rightX + 4f, rightY + 11f, boldPaint)
        canvas.drawRect(RectF(rightX, rightY, rightX + rightWidth, rightY + ROW_HEIGHT), linePaint)
        rightY += ROW_HEIGHT

        val wL = rightWidth * 0.6f
        val wR = rightWidth - wL

        // Функция для ячейки с двумя колонками
        fun drawRightCell(label: String, value: String) {
            canvas.drawRect(RectF(rightX, rightY, rightX + wL, rightY + ROW_HEIGHT), linePaint)
            canvas.drawText(label, rightX + 4f, rightY + 11f, textPaint)
            canvas.drawRect(RectF(rightX + wL, rightY, rightX + wL + wR, rightY + ROW_HEIGHT), linePaint)
            canvas.drawText(value, rightX + wL + 4f, rightY + 11f, textPaint)
            rightY += ROW_HEIGHT
        }
        
        // Функция для объединенной ячейки
        fun drawRightSpan(text: String, bold: Boolean = false) {
            canvas.drawRect(RectF(rightX, rightY, rightX + rightWidth, rightY + ROW_HEIGHT), linePaint)
            canvas.drawText(text, rightX + 4f, rightY + 11f, if (bold) boldPaint else textPaint)
            rightY += ROW_HEIGHT
        }

        drawRightCell("Давление на устье при испытании (A)", "%.1f МПа".format(input.testPressureA))
        drawRightCell("Плотность раствора при испытании (B)", "%.2f кг/л".format(input.testDensityB))
        drawRightSpan("Максимально допустимая плотность раствора:", true)
        drawRightCell("((A) / (ГСВ до башмака x 0.00981) + (B))", "%.2f кг/л".format(WellKillCalculator.maxAllowableDensity(input)))
        drawRightSpan("Максимально допустимое давление на устье в КП:", true)
        drawRightCell("((C) - Плотность р-ра) x ГСВ x 0.00981", "%.1f МПа".format(WellKillCalculator.mdudkp(input)))
        
        rightY += 5f

        // Блок 2: Текущие данные
        canvas.drawRect(RectF(rightX, rightY, rightX + rightWidth, rightY + ROW_HEIGHT), fillGrayPaint)
        canvas.drawText("Текущие данные по скважине:", rightX + 4f, rightY + 11f, boldPaint)
        canvas.drawRect(RectF(rightX, rightY, rightX + rightWidth, rightY + ROW_HEIGHT), linePaint)
        rightY += ROW_HEIGHT

        drawRightCell("Глубина башмака по стволу (MD)", "%.0f м".format(input.casingShoeMD))
        drawRightCell("Глубина башмака по вертикали (TVD)", "%.0f м".format(input.casingShoeTVD))
        drawRightCell("Глубина скважины по стволу (MD)", "%.0f м".format(input.wellDepthMD))
        drawRightCell("Глубина скважины по вертикали (TVD)", "%.0f м".format(input.wellDepthTVD))
        drawRightCell("Плотность раствора в скважине", "%.2f кг/л".format(input.currentMudDensity))

        y = maxOf(schemeTop + schemeHeight, rightY) + 15f

        // === БЛОК 3: ПРОКАЧКА НАСОСОВ ===
        canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT), fillGrayPaint)
        canvas.drawText("Данные о прокачке насосов:", MARGIN + 4f, y + 11f, boldPaint)
        canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT), linePaint)
        y += ROW_HEIGHT

        val halfW = (PAGE_WIDTH - 2 * MARGIN) / 2f
        canvas.drawRect(RectF(MARGIN, y, MARGIN + halfW, y + ROW_HEIGHT), linePaint)
        canvas.drawText("Насос 1: %.1f ход/мин".format(input.pumpRate), MARGIN + 4f, y + 11f, textPaint)
        canvas.drawRect(RectF(MARGIN + halfW, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT), linePaint)
        canvas.drawText("Насос 2: _______ ход/мин", MARGIN + halfW + 4f, y + 11f, textPaint)
        y += ROW_HEIGHT + 10f

        // === БЛОК 4: ОБЪЁМЫ ===
        canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT), fillGrayPaint)
        canvas.drawText("Объёмы:", MARGIN + 4f, y + 11f, boldPaint)
        canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT), linePaint)
        y += ROW_HEIGHT

        canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT), linePaint)
        canvas.drawText("Общий объём КП", MARGIN + 4f, y + 11f, textPaint)
        canvas.drawText("(D+H) = %d л (I)".format(WellKillCalculator.totalWellVolume(input)), MARGIN + 150f, y + 11f, boldPaint)
        y += ROW_HEIGHT

        canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT), linePaint)
        canvas.drawText("Общий объём раствора в скважине", MARGIN + 4f, y + 11f, textPaint)
        canvas.drawText("%d л (J)".format(WellKillCalculator.roundUpInt(input.surfaceVolumeJ)), MARGIN + 150f, y + 11f, boldPaint)
        y += ROW_HEIGHT

        canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT), linePaint)
        canvas.drawText("Объём поверхностной обвязки", MARGIN + 4f, y + 11f, textPaint)
        canvas.drawText("(I+J) = %d л".format(WellKillCalculator.totalVolumeWithSurface(input)), MARGIN + 150f, y + 11f, boldPaint)
        y += ROW_HEIGHT + 10f

        // === БЛОК 5: УДЕЛЬНЫЙ ОБЪЁМ / ХОДЫ / ВРЕМЯ ===
        canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT), fillGrayPaint)
        canvas.drawText("Удельный объём / Ходы / Время:", MARGIN + 4f, y + 11f, boldPaint)
        canvas.drawRect(RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT), linePaint)
        y += ROW_HEIGHT

        val colWidth = (PAGE_WIDTH - 2 * MARGIN) / 4f
        val tableY = y

        // Шапка таблицы
        canvas.drawRect(RectF(MARGIN, y, MARGIN + colWidth * 4, y + ROW_HEIGHT), fillWhitePaint)
        canvas.drawRect(RectF(MARGIN, y, MARGIN + colWidth * 4, y + ROW_HEIGHT), linePaint)
        canvas.drawText("Удельный объём, л", MARGIN + 4f, y + 11f, boldPaint)
        canvas.drawText("Объём, л", MARGIN + colWidth + 4f, y + 11f, boldPaint)
        canvas.drawText("Число ходов", MARGIN + colWidth * 2 + 4f, y + 11f, boldPaint)
        canvas.drawText("Время, мин", MARGIN + colWidth * 3 + 4f, y + 11f, boldPaint)
        y += ROW_HEIGHT

        // Функция для строки таблицы
        fun drawTableRow(val1: String, val2: String, val3: String, val4: String) {
            canvas.drawRect(RectF(MARGIN, y, MARGIN + colWidth, y + ROW_HEIGHT), linePaint)
            canvas.drawRect(RectF(MARGIN + colWidth, y, MARGIN + colWidth * 2, y + ROW_HEIGHT), linePaint)
            canvas.drawRect(RectF(MARGIN + colWidth * 2, y, MARGIN + colWidth * 3, y + ROW_HEIGHT), linePaint)
            canvas.drawRect(RectF(MARGIN + colWidth * 3, y, MARGIN + colWidth * 4, y + ROW_HEIGHT), linePaint)
            
            canvas.drawText(val1, MARGIN + 4f, y + 11f, textPaint)
            canvas.drawText(val2, MARGIN + colWidth + 4f, y + 11f, textPaint)
            canvas.drawText(val3, MARGIN + colWidth * 2 + 4f, y + 11f, textPaint)
            canvas.drawText(val4, MARGIN + colWidth * 3 + 4f, y + 11f, textPaint)
            y += ROW_HEIGHT
        }

        val q = input.pumpOutput
        val rate = input.pumpRate

        drawTableRow(
            "%.4f".format(q),
            "%d".format(WellKillCalculator.drillPipeVolume(input)),
            "%d".format(WellKillCalculator.strokesToBit(input)),
            if (rate > 0) "%.0f".format(WellKillCalculator.strokesToBit(input).toDouble() / rate) else "-"
        )
        drawTableRow(
            "%.4f".format(q),
            "%d".format(WellKillCalculator.totalAnnulusVolume(input)),
            "%d".format(WellKillCalculator.roundUpInt(WellKillCalculator.totalAnnulusVolume(input).toDouble() / q)),
            if (rate > 0) "%.0f".format(WellKillCalculator.roundUpInt(WellKillCalculator.totalAnnulusVolume(input).toDouble() / q).toDouble() / rate) else "-"
        )
        drawTableRow(
            "%.4f".format(q),
            "%d".format(WellKillCalculator.totalWellVolume(input)),
            "%d".format(WellKillCalculator.strokesFullCirculation(input)),
            if (rate > 0) "%.0f".format(WellKillCalculator.strokesFullCirculation(input).toDouble() / rate) else "-"
        )
        drawTableRow(
            "%.4f".format(q),
            "%d".format(WellKillCalculator.roundUpInt(input.surfaceVolumeJ)),
            "%d".format(WellKillCalculator.roundUpInt(input.surfaceVolumeJ / q)),
            if (rate > 0) "%.0f".format(WellKillCalculator.roundUpInt(input.surfaceVolumeJ / q).toDouble() / rate) else "-"
        )

        document.finishPage(page)
        
        // ==============================
        //        СТРАНИЦА 2
        // ==============================
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create())
        canvas = page.canvas
        y = MARGIN + 10f

        // Заголовок
        canvas.drawText("Лист глушения", MARGIN, y, titlePaint)
        y += 25f
        canvas.drawText("Дата: ${input.date}", MARGIN, y, textPaint)
        canvas.drawText("ФИО: ${input.engineer}", MARGIN + 250f, y, textPaint)
        y += 15f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 15f

        val fullW = PAGE_WIDTH - 2 * MARGIN

        // === БЛОК 1: ДАННЫЕ О ПРИТОКЕ ===
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), fillGrayPaint)
        canvas.drawText("Данные о притоке:", MARGIN + 4f, y + 11f, boldPaint)
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        y += ROW_HEIGHT

        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        canvas.drawText("Давление стабилизации в БТ", MARGIN + 4f, y + 11f, textPaint)
        canvas.drawText("%.1f МПа".format(input.sidpp), MARGIN + 200f, y + 11f, boldPaint)
        y += ROW_HEIGHT

        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        canvas.drawText("Давление стабилизации в КП", MARGIN + 4f, y + 11f, textPaint)
        canvas.drawText("%.1f МПа".format(input.sicp), MARGIN + 200f, y + 11f, boldPaint)
        y += ROW_HEIGHT

        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        canvas.drawText("Объём притока", MARGIN + 4f, y + 11f, textPaint)
        canvas.drawText("%.0f л".format(input.influxVolume), MARGIN + 200f, y + 11f, boldPaint)
        y += ROW_HEIGHT + 10f

        // === БЛОК 2: ПЛОТНОСТЬ РАСТВОРА ГЛУШЕНИЯ ===
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), fillGrayPaint)
        canvas.drawText("Плотность раствора глушения", MARGIN + 4f, y + 11f, boldPaint)
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        y += ROW_HEIGHT

        // Формула
        canvas.drawText("Плотность р-ра в скважине + (Давление стаб. в БТ / (ГСВ x 0.00981))", MARGIN + 10f, y + 11f, textPaint)
        y += ROW_HEIGHT
        canvas.drawText("+ = %.2f кг/л".format(WellKillCalculator.killMudDensity(input)), MARGIN + 10f, y + 11f, boldPaint)
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        y += ROW_HEIGHT + 10f

        // === БЛОК 3: НАЧАЛЬНОЕ ДАВЛЕНИЕ ЦИРКУЛЯЦИИ ===
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), fillGrayPaint)
        canvas.drawText("Начальное давление циркуляции (Рнач)", MARGIN + 4f, y + 11f, boldPaint)
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        y += ROW_HEIGHT

        canvas.drawText("Давление прокачки + Давление стабилизации в БТ", MARGIN + 10f, y + 11f, textPaint)
        y += ROW_HEIGHT
        canvas.drawText("+ = %.1f МПа".format(WellKillCalculator.initialCirculatingPressure(input)), MARGIN + 10f, y + 11f, boldPaint)
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        y += ROW_HEIGHT + 10f

        // === БЛОК 4: КОНЕЧНОЕ ДАВЛЕНИЕ ЦИРКУЛЯЦИИ ===
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), fillGrayPaint)
        canvas.drawText("Конечное давление циркуляции (Ркон)", MARGIN + 4f, y + 11f, boldPaint)
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        y += ROW_HEIGHT

        canvas.drawText("(Плотность р-ра глушения / Плотность р-ра в скв) x Давление прокачки", MARGIN + 10f, y + 11f, textPaint)
        y += ROW_HEIGHT
        canvas.drawText("x = %.1f МПа".format(WellKillCalculator.finalCirculatingPressure(input)), MARGIN + 10f, y + 11f, boldPaint)
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        y += ROW_HEIGHT + 10f

        // === БЛОК 5: (К) и Снижение ===
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        canvas.drawText("(К) = Рнач - Ркон", MARGIN + 4f, y + 11f, boldPaint)
        canvas.drawText("%.1f МПа".format(WellKillCalculator.pressureDifferenceK(input)), MARGIN + 200f, y + 11f, boldPaint)
        y += ROW_HEIGHT

        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        canvas.drawText("(К) x 100 / (Е)", MARGIN + 4f, y + 11f, boldPaint)
        canvas.drawText("%.3f МПа / 100 ходов".format(WellKillCalculator.pressureDropPer100Strokes(input)), MARGIN + 200f, y + 11f, boldPaint)
        y += ROW_HEIGHT + 10f

        // Пометка при отрицательном МДУДКП
        val mdudkpCheck = WellKillCalculator.mdudkp(input)
        if (mdudkpCheck < 0.0) {
            canvas.drawText("ВНИМАНИЕ: МДУДКП отрицательный! Несовместимый интервал бурения.", MARGIN, y, redPaint)
            y += 15f
        }

        // === БЛОК 6: ТАБЛИЦА СНИЖЕНИЯ ДАВЛЕНИЯ (30 строк) ===
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), fillGrayPaint)
        canvas.drawText("График снижения давления:", MARGIN + 4f, y + 11f, boldPaint)
        canvas.drawRect(RectF(MARGIN, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        y += ROW_HEIGHT

        // Заголовок таблицы
        val col1W = fullW * 0.5f
        val col2W = fullW * 0.5f
        canvas.drawRect(RectF(MARGIN, y, MARGIN + col1W, y + ROW_HEIGHT), fillWhitePaint)
        canvas.drawRect(RectF(MARGIN + col1W, y, MARGIN + fullW, y + ROW_HEIGHT), fillWhitePaint)
        canvas.drawRect(RectF(MARGIN, y, MARGIN + col1W, y + ROW_HEIGHT), linePaint)
        canvas.drawRect(RectF(MARGIN + col1W, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
        canvas.drawText("Ходов", MARGIN + 4f, y + 11f, boldPaint)
        canvas.drawText("Давление [МПа]", MARGIN + col1W + 4f, y + 11f, boldPaint)
        y += ROW_HEIGHT

        // Заполняем таблицу
        val schedule = WellKillCalculator.pressureSchedule(input, maxRows = 30)
        val totalRows = 30
        for (i in 0 until totalRows) {
            canvas.drawRect(RectF(MARGIN, y, MARGIN + col1W, y + ROW_HEIGHT), linePaint)
            canvas.drawRect(RectF(MARGIN + col1W, y, MARGIN + fullW, y + ROW_HEIGHT), linePaint)
            
            if (i < schedule.size) {
                canvas.drawText(schedule[i].strokes.toString(), MARGIN + 4f, y + 11f, textPaint)
                canvas.drawText("%.1f".format(schedule[i].pressure), MARGIN + col1W + 4f, y + 11f, textPaint)
            } else {
                canvas.drawText("-", MARGIN + 4f, y + 11f, textPaint)
                canvas.drawText("", MARGIN + col1W + 4f, y + 11f, textPaint)
            }
            y += ROW_HEIGHT
        }

        y += 15f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 15f
        canvas.drawText("Подпись бурильщика: ______________________", MARGIN, y, textPaint)
        canvas.drawText("Подпись инженера: ______________________", MARGIN + 280f, y, textPaint)

        document.finishPage(page)

        document.writeTo(outputStream)
        document.close()
    }
    
    // ============ ВСПОМОГАТЕЛЬНЫЕ ============

    private fun drawWellScheme(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        linePaint: Paint,
        textPaint: Paint,
        arrowPaint: Paint
    ) {
        val centerX = left + width / 2f
        val surfaceY = top + 30f
        val totalH = height - 40f

        // 1. Манометры (два кружка со стрелками)
        val g1cx = centerX - 25f
        val g2cx = centerX + 25f
        val gcy = top + 15f
        val gr = 12f
        canvas.drawCircle(g1cx, gcy, gr, linePaint)
        canvas.drawCircle(g2cx, gcy, gr, linePaint)
        // Стрелки внутри манометров
        canvas.drawLine(g1cx, gcy, g1cx - 5f, gcy - 5f, arrowPaint)
        canvas.drawLine(g2cx, gcy, g2cx + 5f, gcy - 5f, arrowPaint)
        
        // Линии от манометров до устья
        canvas.drawLine(g1cx, gcy + gr, g1cx, surfaceY, linePaint)
        canvas.drawLine(g2cx, gcy + gr, g2cx, surfaceY, linePaint)

        // 2. Обсадная колонна (внешняя)
        val casingLeft = centerX - 20f
        val casingRight = centerX + 20f
        val casingBottom = surfaceY + totalH * 0.6f
        canvas.drawLine(casingLeft, surfaceY, casingLeft, casingBottom, linePaint)
        canvas.drawLine(casingRight, surfaceY, casingRight, casingBottom, linePaint)
        
        // 3. Открытый ствол (расширяется ниже башмака)
        val openLeft = casingLeft - 8f
        val openRight = casingRight + 8f
        val openHoleBottom = surfaceY + totalH
        canvas.drawLine(casingLeft, casingBottom, openLeft, casingBottom + 10f, linePaint)
        canvas.drawLine(casingRight, casingBottom, openRight, casingBottom + 10f, linePaint)
        canvas.drawLine(openLeft, casingBottom + 10f, openLeft, openHoleBottom, linePaint)
        canvas.drawLine(openRight, casingBottom + 10f, openRight, openHoleBottom, linePaint)
        canvas.drawLine(openLeft, openHoleBottom, openRight, openHoleBottom, linePaint)

        // 4. Бурильная колонна (внутренняя, до долота)
        val drillLeft = centerX - 6f
        val drillRight = centerX + 6f
        val drillBottom = openHoleBottom - 15f
        canvas.drawLine(drillLeft, surfaceY, drillLeft, drillBottom, linePaint)
        canvas.drawLine(drillRight, surfaceY, drillRight, drillBottom, linePaint)

        // 5. КНБК и Долото (утолщение внизу)
        val bhaLeft = centerX - 10f
        val bhaRight = centerX + 10f
        val bhaTop = drillBottom - 25f
        canvas.drawLine(drillLeft, bhaTop, bhaLeft, bhaTop, linePaint)
        canvas.drawLine(drillRight, bhaTop, bhaRight, bhaTop, linePaint)
        canvas.drawLine(bhaLeft, bhaTop, bhaLeft, drillBottom, linePaint)
        canvas.drawLine(bhaRight, bhaTop, bhaRight, drillBottom, linePaint)
        canvas.drawLine(bhaLeft, drillBottom, bhaRight, drillBottom, linePaint)

        // 6. Стрелки и подписи (как в бланке)
        val textX = left + width + 2f
        val arrowLen = 12f

        // Башмак
        val shoeY = casingBottom
        canvas.drawLine(openRight + 10f, shoeY, openRight + 10f + arrowLen, shoeY, linePaint)
        canvas.drawText("Башмак", openRight + 10f + arrowLen + 2f, shoeY + 3f, textPaint)

        // Забой
        val bitY = drillBottom
        canvas.drawLine(openRight + 10f, bitY, openRight + 10f + arrowLen, bitY, linePaint)
        canvas.drawText("Забой", openRight + 10f + arrowLen + 2f, bitY + 3f, textPaint)

        // Устье
        canvas.drawLine(openRight + 10f, surfaceY, openRight + 10f + arrowLen, surfaceY, linePaint)
        canvas.drawText("Устье", openRight + 10f + arrowLen + 2f, surfaceY + 3f, textPaint)

        // Заголовок схемы
        canvas.drawText("Схема скважины", left, top - 5f, textPaint)
    }
}
