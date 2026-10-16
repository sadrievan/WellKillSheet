package com.example.wellkillsheet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
            color = Color.BLACK
            textSize = 16f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val subTitlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val redPaint = Paint().apply {
            color = Color.rgb(200, 0, 0)
            textSize = 11f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 0.6f
        }
        val boxPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val fillGrayPaint = Paint().apply {
            color = Color.rgb(230, 230, 230)
            style = Paint.Style.FILL
        }

        // ==============================
        //        СТРАНИЦА 1
        // ==============================
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        )
        var canvas = page.canvas
        var y = MARGIN + 10f

        // Заголовок
        canvas.drawText("ЛИСТ ГЛУШЕНИЯ", MARGIN, y, titlePaint)
        y += 22f
        canvas.drawText("Дата: ${input.date}", MARGIN, y, textPaint)
        canvas.drawText("ФИО: ${input.engineer}", MARGIN + 200f, y, textPaint)
        y += 14f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 12f

        // ===== Схема скважины (слева) =====
        val schemeLeft = MARGIN
        val schemeTop = y
        val schemeWidth = 130f
        val schemeHeight = 220f
        drawWellScheme(canvas, schemeLeft, schemeTop, schemeWidth, schemeHeight,
            linePaint, boxPaint, textPaint)

        // ===== Правая колонка =====
        val rightX = schemeLeft + schemeWidth + 15f
        val rightWidth = PAGE_WIDTH - MARGIN - rightX
        var rightY = schemeTop

        rightY = drawSectionHeader(canvas, "Данные о прочности пород",
            rightX, rightY, rightWidth, subTitlePaint, fillGrayPaint, linePaint)

        // Метод расчёта
        if (input.calcMethod == CalcMethod.TEST_PRESSURE) {
            rightY = drawTwoColumnRow(canvas,
                "(A) Давление на устье при испытании",
                "%.1f МПа".format(input.testPressureA),
                rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
            rightY = drawTwoColumnRow(canvas,
                "(B) Плотность раствора при испытании",
                "%.2f кг/л".format(input.testDensityB),
                rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        } else {
            val isPressure = WellKillCalculator.isFracPressure(input.fracValue)
            val label = if (isPressure) "Давление гидроразрыва" else "Градиент гидроразрыва"
            val unit = if (isPressure) "МПа" else "МПа/м"
            rightY = drawTwoColumnRow(canvas,
                label,
                "%.4f %s".format(input.fracValue, unit),
                rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        }

        rightY = drawTwoColumnRow(canvas,
            "(C) Макс. допустимая плотность",
            "%.2f кг/л".format(WellKillCalculator.maxAllowableDensity(input)),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)

        val mdudkp = WellKillCalculator.mdudkp(input)
        rightY = drawTwoColumnRow(canvas,
            "МДУДКП",
            "%.2f МПа".format(mdudkp),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY += 10f

        // Текущие данные
        rightY = drawSectionHeader(canvas, "Текущие данные по скважине",
            rightX, rightY, rightWidth, subTitlePaint, fillGrayPaint, linePaint)

        rightY = drawTwoColumnRow(canvas,
            "Глубина башмака по стволу (MD)",
            "%.0f м".format(input.casingShoeMD),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY = drawTwoColumnRow(canvas,
            "Глубина башмака по вертикали (TVD)",
            "%.0f м".format(input.casingShoeTVD),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY = drawTwoColumnRow(canvas,
            "Глубина скважины по стволу (MD)",
            "%.0f м".format(input.wellDepthMD),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY = drawTwoColumnRow(canvas,
            "Глубина скважины по вертикали (TVD)",
            "%.0f м".format(input.wellDepthTVD),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY = drawTwoColumnRow(canvas,
            "Плотность раствора в скважине",
            "%.2f кг/л".format(input.currentMudDensity),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)

        y = maxOf(schemeTop + schemeHeight, rightY) + 15f

        // ===== Объёмы =====
        y = drawSectionHeader(canvas, "Объёмы",
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, subTitlePaint, fillGrayPaint, linePaint)

        y = drawTwoColumnRow(canvas,
            "КНБК: %.0f м × %.4f л/м".format(input.bhaLength, input.bhaCapacity),
            "%d л".format(WellKillCalculator.roundUpInt(WellKillCalculator.bhaVolumeRaw(input))),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "ТБТ: %.0f м × %.4f л/м".format(input.hwdpLength, input.hwdpCapacity),
            "%d л".format(WellKillCalculator.roundUpInt(WellKillCalculator.hwdpVolumeRaw(input))),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "Бурильный инструмент: %.0f м × %.4f л/м".format(input.dpLength, input.dpCapacity),
            "%d л".format(WellKillCalculator.roundUpInt(WellKillCalculator.dpVolumeRaw(input))),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(D) Объём бурильной колонны",
            "%d л".format(WellKillCalculator.drillPipeVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, boldPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(F) Открытый ствол",
            "%d л".format(WellKillCalculator.openHoleAnnulusVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(G) Обсадная часть",
            "%d л".format(WellKillCalculator.casedHoleAnnulusVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(H) Объём кольцевого пространства",
            "%d л".format(WellKillCalculator.totalAnnulusVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, boldPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(J) Объём наземной обвязки",
            "%d л".format(WellKillCalculator.roundUpInt(input.surfaceVolumeJ)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(I) Объём в скважине",
            "%d л".format(WellKillCalculator.totalWellVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, boldPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(I+J) Общий объём с обвязкой",
            "%d л".format(WellKillCalculator.totalVolumeWithSurface(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, boldPaint, boldPaint, linePaint)
        y += 8f

        // ===== Насос =====
        y = drawSectionHeader(canvas, "Насос",
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, subTitlePaint, fillGrayPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "Скорость насоса при тесте на МХ",
            "%.0f ход/мин".format(input.pumpRate),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "Удельный объём насоса",
            "%.2f л/ход".format(input.pumpOutput),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(E) Ходов до долота",
            "%d ход".format(WellKillCalculator.strokesToBit(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, boldPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "Ходов на полную циркуляцию",
            "%d ход".format(WellKillCalculator.strokesFullCirculation(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)

        y += 25f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 20f
        canvas.drawText("Подпись бурильщика: ______________________", MARGIN, y, textPaint)
        canvas.drawText("Подпись инженера: ______________________", MARGIN + 280f, y, textPaint)

        document.finishPage(page)
