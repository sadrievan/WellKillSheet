package com.example.wellkillsheet

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun generate(context: Context, input: WellKillInput, outputStream: OutputStream) {
        val document = PdfDocument()

        // Загружаем картинку
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.page1)

        // Вычисляем коэффициент масштабирования (картинка -> PDF)
        val scaleX = PAGE_WIDTH.toFloat() / bitmap.width
        val scaleY = PAGE_HEIGHT.toFloat() / bitmap.height

        // Рассчитываем размер шрифта: 
        // Допустим, мы хотим, чтобы высота шрифта была примерно 15 пикселей на картинке.
        // 15 пикселей на картинке = 15 * scaleY точек в PDF.
        val baseFontSize = 40f // Условный размер на картинке (в пикселях)
        val pdfFontSize = baseFontSize * scaleY

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = pdfFontSize
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = pdfFontSize
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        var canvas = page.canvas

        // 1. Рисуем подложку на всю страницу
        val destRect = RectF(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat())
        canvas.drawBitmap(bitmap, null, destRect, null)

        // Функция для наложения текста
        fun drawTextAt(text: String, x: Float, y: Float, paint: Paint = textPaint) {
            // Координаты x, y масштабируются
            canvas.drawText(text, x * scaleX, y * scaleY, paint)
        }

        // === НАШИ ДАННЫЕ ===

        // Дата и ФИО (пока без координат, ставим примерно)
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
        drawTextAt("%.0f".format(WellKillCalculator.dpLength(input)), 900f, 1981f)
        drawTextAt("%.4f".format(input.dpCapacity), 1135f, 1981f)
        drawTextAt(WellKillCalculator.roundUpInt(WellKillCalculator.dpVolumeRaw(input)).toString(), 1415f, 1981f)

        drawTextAt("%.0f".format(input.hwdpLength), 900f, 2065f)
        drawTextAt("%.4f".format(input.hwdpCapacity), 1135f, 2065f)
        drawTextAt(WellKillCalculator.roundUpInt(WellKillCalculator.hwdpVolumeRaw(input)).toString(), 1415f, 2065f)

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
        document.writeTo(outputStream)
        document.close()
    }
}
