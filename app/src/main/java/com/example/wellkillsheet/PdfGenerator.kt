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
    private const val LINE_HEIGHT = 14f
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
        var y = MARGIN

        // Заголовок
        canvas.drawText("ЛИСТ ГЛУШЕНИЯ", MARGIN, y + 16f, titlePaint)
        y += 30f
        canvas.drawText("Дата: ${input.date}", MARGIN, y, textPaint)
        canvas.drawText("ФИО: ${input.engineer}", MARGIN + 200f, y, textPaint)
        y += 20f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 10f

        // ===== Схема скважины (слева) =====
        val schemeLeft = MARGIN
        val schemeTop = y
        val schemeWidth = 120f
        val schemeHeight = 200f
        drawWellScheme(canvas, schemeLeft, schemeTop, schemeWidth, schemeHeight,
            linePaint, boxPaint, textPaint)

        // ===== Правая колонка: Данные о прочности и текущие данные =====
        val rightX = schemeLeft + schemeWidth + 15f
        val rightWidth = PAGE_WIDTH - MARGIN - rightX
        var rightY = schemeTop

        rightY = drawSectionHeader(canvas, "Данные о прочности пород",
            rightX, rightY, rightWidth, subTitlePaint, fillGrayPaint, linePaint)

        rightY = drawTwoColumnRow(canvas,
            "(A) Давление на устье при испытании",
            "%.2f МПа".format(input.testPressureA),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY = drawTwoColumnRow(canvas,
            "(B) Плотность раствора при испытании",
            "%.2f кг/л".format(input.testDensityB),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY = drawTwoColumnRow(canvas,
            "(C) Макс. допустимая плотность",
            "%.2f кг/л".format(WellKillCalculator.maxAllowableDensity(input)),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY = drawTwoColumnRow(canvas,
            "МДУДКП",
            "%.2f МПа".format(WellKillCalculator.mdudkp(input)),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY += 10f

        rightY = drawSectionHeader(canvas, "Текущие данные по скважине",
            rightX, rightY, rightWidth, subTitlePaint, fillGrayPaint, linePaint)

        rightY = drawTwoColumnRow(canvas,
            "Глубина башмака по стволу (MD)",
            "%.1f м".format(input.casingShoeMD),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY = drawTwoColumnRow(canvas,
            "Глубина башмака по вертикали (TVD)",
            "%.1f м".format(input.casingShoeTVD),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY = drawTwoColumnRow(canvas,
            "Глубина скважины по стволу (MD)",
            "%.1f м".format(input.wellDepthMD),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY = drawTwoColumnRow(canvas,
            "Глубина скважины по вертикали (TVD)",
            "%.1f м".format(input.wellDepthTVD),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)
        rightY = drawTwoColumnRow(canvas,
            "Плотность раствора в скважине",
            "%.2f кг/л".format(input.currentMudDensity),
            rightX, rightY, rightWidth, textPaint, boldPaint, linePaint)

        y = maxOf(schemeTop + schemeHeight, rightY) + 15f

        // ===== Блок «Объёмы» =====
        y = drawSectionHeader(canvas, "Объёмы",
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, subTitlePaint, fillGrayPaint, linePaint)

        y = drawTwoColumnRow(canvas,
            "КНБК: %.1f м × %.4f л/м".format(input.bhaLength, input.bhaCapacity),
            "%.1f л".format(WellKillCalculator.bhaVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "ТБТ: %.1f м × %.4f л/м".format(input.hwdpLength, input.hwdpCapacity),
            "%.1f л".format(WellKillCalculator.hwdpVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "Бурильный инструмент: %.1f м × %.4f л/м".format(input.dpLength, input.dpCapacity),
            "%.1f л".format(WellKillCalculator.dpVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(D) Объём бурильной колонны",
            "%.1f л".format(WellKillCalculator.drillPipeVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, boldPaint, boldPaint, linePaint)

        y = drawTwoColumnRow(canvas,
            "(F) Открытый ствол: %.1f м × %.4f л/м".format(
                WellKillCalculator.openHoleLength(input),
                input.openHoleAnnulusCapacity),
            "%.1f л".format(WellKillCalculator.openHoleAnnulusVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(G) Обсадная часть: %.1f м × %.4f л/м".format(
                WellKillCalculator.casedHoleLength(input),
                input.casedHoleAnnulusCapacity),
            "%.1f л".format(WellKillCalculator.casedHoleAnnulusVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(H) Объём кольцевого пространства",
            "%.1f л".format(WellKillCalculator.totalAnnulusVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, boldPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(J) Объём наземной обвязки",
            "%.1f л".format(input.surfaceVolumeJ),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(I) Объём в скважине",
            "%.1f л".format(WellKillCalculator.totalWellVolume(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, boldPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(I+J) Общий объём с обвязкой",
            "%.1f л".format(WellKillCalculator.totalVolumeWithSurface(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, boldPaint, boldPaint, linePaint)

        y += 10f
        y = drawSectionHeader(canvas, "Насос",
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, subTitlePaint, fillGrayPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "Скорость насоса при тесте на МХ",
            "%.1f ход/мин".format(input.pumpRate),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "Удельный объём насоса",
            "%.2f л/ход".format(input.pumpOutput),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "(E) Ходов до долота",
            "%.0f ход".format(WellKillCalculator.strokesToBit(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, boldPaint, boldPaint, linePaint)
        y = drawTwoColumnRow(canvas,
            "Ходов на полную циркуляцию",
            "%.0f ход".format(WellKillCalculator.strokesFullCirculation(input)),
            MARGIN, y, PAGE_WIDTH - 2 * MARGIN, textPaint, boldPaint, linePaint)

        y += 25f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 15f
        canvas.drawText("Подпись бурильщика: ______________________",
            MARGIN, y, textPaint)
        canvas.drawText("Подпись инженера: ______________________",
            MARGIN + 280f, y, textPaint)

        document.finishPage(page)

        // ==============================
        //        СТРАНИЦА 2
        // ==============================
        page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
        )
        canvas = page.canvas
        y = MARGIN

        canvas.drawText("ЛИСТ ГЛУШЕНИЯ (продол
