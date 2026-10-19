package com.example.wellkillsheet

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import kotlin.math.min

object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    // Размер шрифта в точках PDF. 11f — стандартный читаемый размер.
    private const val PDF_FONT_SIZE = 11f

    fun generate(context: Context, input: WellKillInput, outputStream: OutputStream) {
        val document = PdfDocument()

        // ==========================================
        //        СТРАНИЦА 1
        // ==========================================
        val page1Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.page1)
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        var canvas = page.canvas

        // Вычисляем масштаб и отступы
        val scale1 = min(PAGE_WIDTH.toFloat() / page1Bitmap.width, PAGE_HEIGHT.toFloat() / page1Bitmap.height)
        val offsetX1 = (PAGE_WIDTH - page1Bitmap.width * scale1) / 2f
        val offsetY1 = (PAGE_HEIGHT - page1Bitmap.height * scale1) / 2f

        // Рисуем подложку
        val destRect1 = RectF(offsetX1, offsetY1, offsetX1 + page1Bitmap.width * scale1, offsetY1 + page1Bitmap.height * scale1)
        canvas.drawBitmap(page1Bitmap, null, destRect1, null)

        // Настройки кистей (размер фиксирован, не зависит от масштаба картинки)
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = PDF_FONT_SIZE // <-- ТЕПЕРЬ РОВНО 11
            isAntiAlias = true
            textAlign = Paint.Align.CENTER // Центрируем текст по координатам
        }

        // Функция для наложения текста
        fun drawTextAt(text: String, x: Float, y: Float, paint: Paint = textPaint) {
            val pdfX = x * scale1 + offsetX1
            // Небольшая коррекция по Y, так как drawText рисует текст от базовой линии (низа)
            // 11 * 0.35f = ~4 точки вверх, чтобы центр текста совпал с координатой Y
            val pdfY = y * scale1 + offsetY1 + (PDF_FONT_SIZE * 0.35f)
            canvas.drawText(text, pdfX, pdfY, paint)
        }

        // === ДАННЫЕ ===

        // Дата и ФИО (пока без точных координат, ставим примерно)
        drawTextAt(input.date, 1600f, 200f)
        drawTextAt(input.engineer, 1600f, 300f)

        // 1. (A) Давление на устье
        drawTextAt("%.1f".format(input.testPressureA), 1100f, 530f)

        // 2. (B) Плотность при испытании
        drawTextAt("%.2f".format(input.testDensityB), 1100f, 607f)

        // 3. (C) Макс. допустимая плотность
        drawTextAt("%.2f".format(WellKillCalculator.maxAllowableDensity(input)), 1100f, 780f)

        // 4. МДУДКП
        drawTextAt("%.2f".format(WellKillCalculator.mdudkp(input)), 1120f, 1023f)

        // 5. Буровой раствор (Плотность)
        drawTextAt("%.2f".format(input.currentMudDensity), 1790f, 815f)

        // 6-9. Глубины
        drawTextAt("%.0f".format(input.casingShoeMD), 1780f, 1155f)
        drawTextAt("%.0f".format(input.casingShoeTVD), 1780f, 1235f)
        drawTextAt("%.0f".format(input.wellDepthMD), 1780f, 1540f)
        drawTextAt("%.0f".format(input.wellDepthTVD), 1780f, 1620f)

        // 10. Таблица объёмов
        // БТ
        drawTextAt("%.0f".format(WellKillCalculator.dpLength(input)), 900f, 1981f)
        drawTextAt("%.4f".format(input.dpCapacity), 1135f, 1981f)
        drawTextAt(WellKillCalculator.roundUpInt(WellKillCalculator.dpVolumeRaw(input)).toString(), 1415f, 1981f)
        // ТБТ
        drawTextAt("%.0f".format(input.hwdpLength), 900f, 2065f)
        drawTextAt("%.4f".format(input.hwdpCapacity), 1135f, 2065f)
        drawTextAt(WellKillCalculator.roundUpInt(WellKillCalculator.hwdpVolumeRaw(input)).toString(), 1415f, 2065f)
        // УБТ (КНБК)
        drawTextAt("%.0f".format(input.bhaLength), 900f, 2140f)
        drawTextAt("%.4f".format(input.bhaCapacity), 1135f, 2140f)
        drawTextAt(WellKillCalculator.roundUpInt(WellKillCalculator.bhaVolumeRaw(input)).toString(), 1415f, 2140f)

        // 11. Объёмы (D, E, F, G, H, I, J)
        drawTextAt(WellKillCalculator.drillPipeVolume(input).toString(), 1431f, 2210f)
        drawTextAt(WellKillCalculator.strokesToBit(input).toString(), 1785f, 2210f)
        drawTextAt(WellKillCalculator.openHoleAnnulusVolume(input).toString(), 1430f, 2455f)
        drawTextAt(WellKillCalculator.casedHoleAnnulusVolume(input).toString(), 1430f, 2565f)
        drawTextAt(WellKillCalculator.totalAnnulusVolume(input).toString(), 1400f, 2645f)
        drawTextAt(WellKillCalculator.totalWellVolume(input).toString(), 1400f, 2790f)
        drawTextAt(WellKillCalculator.roundUpInt(input.surfaceVolumeJ).toString(), 1400f, 2880f)

        document.finishPage(page)


        // ==========================================
        //        СТРАНИЦА 2 (Пока просто подложка)
        // ==========================================
        val page2Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.page2)
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create())
        canvas = page.canvas

        val scale2 = min(PAGE_WIDTH.toFloat() / page2Bitmap.width, PAGE_HEIGHT.toFloat() / page2Bitmap.height)
        val offsetX2 = (PAGE_WIDTH - page2Bitmap.width * scale2) / 2f
        val offsetY2 = (PAGE_HEIGHT - page2Bitmap.height * scale2) / 2f

        val destRect2 = RectF(offsetX2, offsetY2, offsetX2 + page2Bitmap.width * scale2, offsetY2 + page2Bitmap.height * scale2)
        canvas.drawBitmap(page2Bitmap, null, destRect2, null)

        document.finishPage(page)

        document.writeTo(outputStream)
        document.close()
    }
}
