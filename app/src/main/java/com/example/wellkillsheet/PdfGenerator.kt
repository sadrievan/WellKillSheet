package com.example.wellkillsheet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object PdfGenerator {

    // Размер страницы PDF (A4 в точках)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun generate(context: Context, input: WellKillInput, outputStream: OutputStream) {
        val document = PdfDocument()

        // Кисти для текста
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 45f // Размер шрифта подобран под масштаб картинки 300dpi
            isAntiAlias = true
            textAlign = Paint.Align.CENTER // Выравнивание по центру!
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 45f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val linePaint = Paint().apply {
            color = Color.RED
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        // ==============================
        //        СТРАНИЦА 1
        // ==============================
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        var canvas = page.canvas

        // 1. Загружаем картинку page1 (Она должна быть в res/drawable)
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.page1)
        
        // 2. Масштабируем картинку ровно под A4
        val destRect = RectF(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat())
        canvas.drawBitmap(bitmap, null, destRect, null)

        // 3. Вычисляем коэффициент масштабирования
        // Если картинка была 2480px, а страница PDF 595pt, то коэффициент ~0.24
        val scaleX = PAGE_WIDTH.toFloat() / bitmap.width
        val scaleY = PAGE_HEIGHT.toFloat() / bitmap.height

        // Функция для наложения текста по координатам (X, Y) исходной картинки
        fun drawTextAt(text: String, x: Float, y: Float, paint: Paint = textPaint) {
            canvas.drawText(text, x * scaleX, y * scaleY, paint)
        }

        // Функция для наложения текста с выравниванием по правому краю (для денежных/числовых колонок)
        fun drawTextRight(text: String, x: Float, y: Float) {
            val rightPaint = Paint(textPaint)
            rightPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(text, x * scaleX, y * scaleY, rightPaint)
        }

        // === НАШИ ДАННЫЕ ===

        // Дата и ФИО (пока заглушки, нужны координаты)
        drawTextAt(input.date, 1600f, 200f, boldPaint)
        drawTextAt(input.engineer, 1600f, 300f, boldPaint)

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

        // === ДИАГНОСТИКА (КРАСНЫЕ ТОЧКИ) ===
        // Если текст "уезжает" от центра ячейки, вы увидите красную точку
        fun drawCross(x: Float, y: Float) {
            val cx = x * scaleX
            val cy = y * scaleY
            canvas.drawLine(cx - 5f, cy, cx + 5f, cy, linePaint)
            canvas.drawLine(cx, cy - 5f, cx, cy + 5f, linePaint)
        }
        drawCross(1100f, 530f) // (A)
        drawCross(1100f, 607f) // (B)
        drawCross(1431f, 2210f) // (D)

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }
}
