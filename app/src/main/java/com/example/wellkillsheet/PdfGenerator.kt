package com.example.wellkillsheet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object PdfGenerator {

    private const val PAGE_WIDTH = 595   // A4, точки
    private const val PAGE_HEIGHT = 842
    private const val LEFT = 40f
    private const val RIGHT = 555f
    private const val LINE = 16f

    fun generate(context: Context, input: WellKillInput, outputStream: OutputStream) {
        val document = PdfDocument()

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val linePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 0.7f
        }

        // ================= Страница 1 =================
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        )
        var canvas = page.canvas
        var y = 50f

        canvas.drawText("ЛИСТ ГЛУШЕНИЯ", LEFT, y, titlePaint)
        y += LINE * 1.8f
        canvas.drawText("Дата: ${input.date}", LEFT, y, textPaint)
        canvas.drawText("ФИО: ${input.engineer}", LEFT + 250f, y, textPaint)
        y += LINE * 1.5f
        canvas.drawLine(LEFT, y, RIGHT, y, linePaint)
        y += LINE

        // --- Блок 1 ---
        canvas.drawText("Данные о прочности пород", LEFT, y, boldPaint)
        y += LINE
        canvas.drawText("(A) Давление на устье при испытании, МПа", LEFT, y, textPaint)
        canvas.drawText("%.2f".format(input.testPressureA), RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("(B) Плотность раствора при испытании, кг/л", LEFT, y, textPaint)
        canvas.drawText("%.2f".format(input.testDensityB), RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("ГСВ до башмака, м", LEFT, y, textPaint)
        canvas.drawText("%.1f".format(input.casingShoeTVD), RIGHT - 60f, y, textPaint)
        y += LINE

        canvas.drawText("(C) Макс. доп. плотность, кг/л", LEFT, y, boldPaint)
        canvas.drawText("%.2f".format(WellKillCalculator.maxAllowableDensity(input)),
            RIGHT - 60f, y, boldPaint)
        y += LINE
        canvas.drawText("МДУДКП, МПа", LEFT, y, boldPaint)
        canvas.drawText("%.2f".format(WellKillCalculator.mdudkp(input)),
            RIGHT - 60f, y, boldPaint)
        y += LINE * 1.5f
        canvas.drawLine(LEFT, y, RIGHT, y, linePaint)
        y += LINE

        // --- Блок 2: Текущие данные ---
        canvas.drawText("Текущие данные по скважине", LEFT, y, boldPaint)
        y += LINE
        canvas.drawText("Глубина по стволу (MD), м", LEFT, y, textPaint)
        canvas.drawText("%.1f".format(input.wellDepthMD), RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("Глубина по вертикали (TVD), м", LEFT, y, textPaint)
        canvas.drawText("%.1f".format(input.wellDepthTVD), RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("Плотность раствора в скважине, кг/л", LEFT, y, textPaint)
        canvas.drawText("%.2f".format(input.currentMudDensity), RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("Длина открытого ствола, м", LEFT, y, textPaint)
        canvas.drawText("%.1f".format(input.openHoleLength), RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("Длина обсадной части, м", LEFT, y, textPaint)
        canvas.drawText("%.1f".format(input.casedHoleLength), RIGHT - 60f, y, textPaint)
        y += LINE * 1.5f
        canvas.drawLine(LEFT, y, RIGHT, y, linePaint)
        y += LINE

        // --- Блок 3: Объёмы ---
        canvas.drawText("Объёмы", LEFT, y, boldPaint)
        y += LINE
        canvas.drawText("(D) Объём бурильной колонны, л", LEFT, y, textPaint)
        canvas.drawText("%.1f".format(WellKillCalculator.drillPipeVolume(input)),
            RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("(F) Открытый ствол, л", LEFT, y, textPaint)
        canvas.drawText("%.1f".format(WellKillCalculator.openHoleAnnulusVolume(input)),
            RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("(G) Обсадная часть, л", LEFT, y, textPaint)
        canvas.drawText("%.1f".format(WellKillCalculator.casedHoleAnnulusVolume(input)),
            RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("(H) Объём кольцевого пространства, л", LEFT, y, textPaint)
        canvas.drawText("%.1f".format(WellKillCalculator.totalAnnulusVolume(input)),
            RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("(J) Объём наземной обвязки, л", LEFT, y, textPaint)
        canvas.drawText("%.1f".format(input.surfaceVolumeJ), RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("(I) Объём в скважине, л", LEFT, y, boldPaint)
        canvas.drawText("%.1f".format(WellKillCalculator.totalWellVolume(input)),
            RIGHT - 60f, y, boldPaint)
        y += LINE
        canvas.drawText("(I+J) Общий объём, л", LEFT, y, boldPaint)
        canvas.drawText("%.1f".format(WellKillCalculator.totalVolumeWithSurface(input)),
            RIGHT - 60f, y, boldPaint)
        y += LINE
        canvas.drawText("(E) Ходов до долота", LEFT, y, boldPaint)
        canvas.drawText("%.0f".format(WellKillCalculator.strokesToBit(input)),
            RIGHT - 60f, y, boldPaint)
        y += LINE

        document.finishPage(page)

        // ================= Страница 2 =================
        page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
        )
        canvas = page.canvas
        y = 50f

        canvas.drawText("ЛИСТ ГЛУШЕНИЯ (продолжение)", LEFT, y, titlePaint)
        y += LINE * 1.8f
        canvas.drawText("Дата: ${input.date}", LEFT, y, textPaint)
        canvas.drawText("ФИО: ${input.engineer}", LEFT + 250f, y, textPaint)
        y += LINE * 1.5f
        canvas.drawLine(LEFT, y, RIGHT, y, linePaint)
        y += LINE

        // --- Данные о притоке ---
        canvas.drawText("Данные о притоке", LEFT, y, boldPaint)
        y += LINE
        canvas.drawText("SIDPP (давление стабилизации в БТ), МПа", LEFT, y, textPaint)
        canvas.drawText("%.2f".format(input.sidpp), RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("SICP (давление стабилизации в КП), МПа", LEFT, y, textPaint)
        canvas.drawText("%.2f".format(input.sicp), RIGHT - 60f, y, textPaint)
        y += LINE
        canvas.drawText("Объём притока, л", LEFT, y, textPaint)
        canvas.drawText("%.1f".format(input.influxVolume), RIGHT - 60f, y, textPaint)
        y += LINE * 1.5f
        canvas.drawLine(LEFT, y, RIGHT, y, linePaint)
        y += LINE

        // --- Расчёты ---
        canvas.drawText("Плотность раствора глушения", LEFT, y, boldPaint)
        y += LINE
        canvas.drawText(
            "ρ_скв + SIDPP / (TVD × 0.00981) = %.2f кг/л".format(
                WellKillCalculator.killMudDensity(input)),
            LEFT + 10f, y, textPaint)
        y += LINE * 1.5f

        canvas.drawText("Начальное давление циркуляции (Рнач)", LEFT, y, boldPaint)
        y += LINE
        canvas.drawText(
            "Давление прокачки + SIDPP = %.2f МПа".format(
                WellKillCalculator.initialCirculatingPressure(input)),
            LEFT + 10f, y, textPaint)
        y += LINE * 1.5f

        canvas.drawText("Конечное давление циркуляции (Ркон)", LEFT, y, boldPaint)
        y += LINE
        canvas.drawText(
            "(ρ_глуш / ρ_скв) × Давление прокачки = %.2f МПа".format(
                WellKillCalculator.finalCirculatingPressure(input)),
            LEFT + 10f, y, textPaint)
        y += LINE * 1.5f

        canvas.drawText("(К) = Рнач − Ркон", LEFT, y, boldPaint)
        canvas.drawText("%.2f МПа".format(WellKillCalculator.pressureDifferenceK(input)),
            RIGHT - 100f, y, boldPaint)
        y += LINE
        canvas.drawText("Снижение на 100 ходов = (К) × 100 / (E)", LEFT, y, boldPaint)
        canvas.drawText("%.3f МПа".format(
            WellKillCalculator.pressureDropPer100Strokes(input)),
            RIGHT - 100f, y, boldPaint)
        y += LINE * 1.5f
        canvas.drawLine(LEFT, y, RIGHT, y, linePaint)
        y += LINE

        // --- Таблица снижения давления ---
        canvas.drawText("График снижения давления", LEFT, y, boldPaint)
        y += LINE

        // Заголовок таблицы
        canvas.drawText("Ходов", LEFT, y, boldPaint)
        canvas.drawText("Давление, МПа", LEFT + 200f, y, boldPaint)
        y += LINE * 0.8f
        canvas.drawLine(LEFT, y, RIGHT, y, linePaint)
        y += LINE * 0.8f

        val schedule = WellKillCalculator.pressureSchedule(input)
        val maxRowsPerPage = 40
        for ((idx, row) in schedule.withIndex()) {
            if (idx >= maxRowsPerPage) break
            canvas.drawText(row.strokes.toString(), LEFT, y, textPaint)
            canvas.drawText("%.2f".format(row.pressure), LEFT + 200f, y, textPaint)
            y += LINE
        }

        y += LINE * 1.5f
        canvas.drawLine(LEFT, y, RIGHT, y, linePaint)
        y += LINE
        canvas.drawText("Подпись бурильщика: ______________________", LEFT, y, textPaint)
        y += LINE * 2f
        canvas.drawText("Подпись инженера: ______________________", LEFT, y, textPaint)

        document.finishPage(page)

        document.writeTo(outputStream)
        document.close()
    }
}
