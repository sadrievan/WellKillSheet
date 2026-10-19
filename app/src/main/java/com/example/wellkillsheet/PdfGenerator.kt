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
import kotlin.math.min

object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    // Размер шрифта
    private const val BASE_FONT_SIZE = 40f

    private const val ORIGINAL_PAGE1_WIDTH = 2550f
    private const val ORIGINAL_PAGE2_WIDTH = 2409f

    private fun loadAndScale(context: Context, resId: Int, targetWidth: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, options)
        
        var scale = 1
        if (options.outWidth > targetWidth) {
            while (options.outWidth / scale > targetWidth * 2) {
                scale *= 2
            }
        }
        options.inSampleSize = scale
        options.inJustDecodeBounds = false
        
        val bmp = BitmapFactory.decodeResource(context.resources, resId, options)
        return if (bmp.width > targetWidth) {
            Bitmap.createScaledBitmap(bmp, targetWidth, (bmp.height * (targetWidth.toFloat() / bmp.width)).toInt(), true)
        } else bmp
    }

    fun generate(context: Context, input: WellKillInput, outputStream: OutputStream) {
        val document = PdfDocument()

        // ==========================================
        //        СТРАНИЦА 1
        // ==========================================
        val page1Bitmap = loadAndScale(context, R.drawable.page1, 1240)
        val compressRatio1 = ORIGINAL_PAGE1_WIDTH / page1Bitmap.width

        val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        var page = document.startPage(pageInfo1)
        var canvas = page.canvas

        val scale1 = min(PAGE_WIDTH.toFloat() / page1Bitmap.width, PAGE_HEIGHT.toFloat() / page1Bitmap.height)
        val offsetX1 = (PAGE_WIDTH - page1Bitmap.width * scale1) / 2f
        val offsetY1 = (PAGE_HEIGHT - page1Bitmap.height * scale1) / 2f

        val destRect1 = RectF(offsetX1, offsetY1, offsetX1 + page1Bitmap.width * scale1, offsetY1 + page1Bitmap.height * scale1)
        canvas.drawBitmap(page1Bitmap, null, destRect1, null)

        val pdfFontSize1 = (BASE_FONT_SIZE / compressRatio1) * scale1
        val textPaint1 = Paint().apply {
            color = Color.BLACK
            textSize = pdfFontSize1
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        fun drawTextAt1(text: String, x: Float, y: Float, paint: Paint = textPaint1) {
            val adjustedX = x / compressRatio1
            val adjustedY = y / compressRatio1
            val pdfX = adjustedX * scale1 + offsetX1
            val pdfY = adjustedY * scale1 + offsetY1 + (pdfFontSize1 * 0.35f)
            canvas.drawText(text, pdfX, pdfY, paint)
        }

        // === ДАННЫЕ СТРАНИЦЫ 1 ===
        drawTextAt1(input.date, 2005f, 135f)
        if (input.engineer.isNotEmpty()) {
            drawTextAt1(input.engineer, 2005f, 210f)
        }
        drawTextAt1("%.1f".format(input.testPressureA), 1100f, 530f)
        drawTextAt1("%.2f".format(input.testDensityB), 1100f, 607f)
        drawTextAt1("%.2f".format(WellKillCalculator.maxAllowableDensity(input)), 1100f, 770f)
        drawTextAt1("%.2f".format(WellKillCalculator.mdudkp(input)), 1120f, 1023f)

        drawTextAt1("%.2f".format(input.currentMudDensity), 1790f, 815f)
        drawTextAt1("%.0f".format(input.casingShoeMD), 1780f, 1155f)
        drawTextAt1("%.0f".format(input.casingShoeTVD), 1780f, 1235f)
        drawTextAt1("%.0f".format(input.wellDepthMD), 1780f, 1540f)
        drawTextAt1("%.0f".format(input.wellDepthTVD), 1780f, 1620f)

        // Насосы
        drawTextAt1("%.4f".format(input.pumpOutput), 425f, 1300f)
        drawTextAt1("%.1f".format(input.pumpPressure), 700f, 1650f)
        drawTextAt1("%.0f".format(input.pumpRate), 280f, 1650f)

        // Таблица объёмов
        drawTextAt1("%.0f".format(WellKillCalculator.dpLength(input)), 900f, 1981f)
        drawTextAt1("%.4f".format(input.dpCapacity), 1135f, 1981f)
        drawTextAt1(WellKillCalculator.roundUpInt(WellKillCalculator.dpVolumeRaw(input)).toString(), 1415f, 1981f)
        
        drawTextAt1("%.0f".format(input.hwdpLength), 900f, 2065f)
        drawTextAt1("%.4f".format(input.hwdpCapacity), 1135f, 2065f)
        drawTextAt1(WellKillCalculator.roundUpInt(WellKillCalculator.hwdpVolumeRaw(input)).toString(), 1415f, 2065f)
        
        drawTextAt1("%.0f".format(input.bhaLength), 900f, 2140f)
        drawTextAt1("%.4f".format(input.bhaCapacity), 1135f, 2140f)
        drawTextAt1(WellKillCalculator.roundUpInt(WellKillCalculator.bhaVolumeRaw(input)).toString(), 1415f, 2140f)

        drawTextAt1(WellKillCalculator.drillPipeVolume(input).toString(), 1431f, 2210f)
        drawTextAt1(WellKillCalculator.strokesToBit(input).toString(), 1785f, 2210f)
        drawTextAt1(WellKillCalculator.openHoleAnnulusVolume(input).toString(), 1430f, 2455f)
        drawTextAt1(WellKillCalculator.casedHoleAnnulusVolume(input).toString(), 1430f, 2565f)
        drawTextAt1(WellKillCalculator.totalAnnulusVolume(input).toString(), 1400f, 2645f)
        drawTextAt1(WellKillCalculator.totalWellVolume(input).toString(), 1400f, 2790f)
        drawTextAt1(WellKillCalculator.roundUpInt(input.surfaceVolumeJ).toString(), 1400f, 2880f)
        drawTextAt1(WellKillCalculator.totalVolumeWithSurface(input).toString(), 1330f, 2965f)

        document.finishPage(page)

        // ==========================================
        //        СТРАНИЦА 2
        // ==========================================
        val page2Bitmap = loadAndScale(context, R.drawable.page2, 1240)
        val compressRatio2 = ORIGINAL_PAGE2_WIDTH / page2Bitmap.width
        
        val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
        page = document.startPage(pageInfo2)
        canvas = page.canvas
        
        val scale2 = min(PAGE_WIDTH.toFloat() / page2Bitmap.width, PAGE_HEIGHT.toFloat() / page2Bitmap.height)
        val offsetX2 = (PAGE_WIDTH - page2Bitmap.width * scale2) / 2f
        val offsetY2 = (PAGE_HEIGHT - page2Bitmap.height * scale2) / 2f
        val destRect2 = RectF(offsetX2, offsetY2, offsetX2 + page2Bitmap.width * scale2, offsetY2 + page2Bitmap.height * scale2)
        canvas.drawBitmap(page2Bitmap, null, destRect2, null)

        val pdfFontSize2 = (BASE_FONT_SIZE / compressRatio2) * scale2
        val textPaint2 = Paint().apply {
            color = Color.BLACK
            textSize = pdfFontSize2
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        fun drawTextAt2(text: String, x: Float, y: Float, paint: Paint = textPaint2) {
            val adjustedX = x / compressRatio2
            val adjustedY = y / compressRatio2
            val pdfX = adjustedX * scale2 + offsetX2
            val pdfY = adjustedY * scale2 + offsetY2 + (pdfFontSize2 * 0.35f)
            canvas.drawText(text, pdfX, pdfY, paint)
        }

        // === ДАННЫЕ СТРАНИЦЫ 2 ===
        
        // Приток
        drawTextAt2("%.1f".format(input.sidpp), 680f, 435f)
        drawTextAt2("%.1f".format(input.sicp), 1350f, 435f)
        drawTextAt2("%.0f".format(input.influxVolume), 2000f, 435f)

        // Расчеты
        drawTextAt2("%.2f".format(WellKillCalculator.killMudDensity(input)), 2100f, 640f)
        drawTextAt2("%.1f".format(WellKillCalculator.initialCirculatingPressure(input)), 2100f, 865f)
        drawTextAt2("%.1f".format(WellKillCalculator.finalCirculatingPressure(input)), 2100f, 1120f)
        drawTextAt2("%.1f".format(WellKillCalculator.pressureDifferenceK(input)), 860f, 1250f)
        drawTextAt2("%.3f".format(WellKillCalculator.pressureDropPer100Strokes(input)), 1600f, 1250f)

        // === ТАБЛИЦА 30 СТРОК ===
        val schedule = WellKillCalculator.pressureSchedule(input, maxRows = 30)
        
        // Координаты первой строки
        val startX_Strokes = 275f
        val startX_Pressure = 500f
        val startY = 1587f
        // Вычисляем шаг между строками (расстояние от 1-й до 30-й строки делим на 29)
        // Поскольку таблица заканчивается где-то внизу, мы подберем шаг. 
        // Возьмем стандартный шаг ~32 пикселя (подгонишь, если будет наезжать)
        val stepY = 31.5f 

        for (i in 0 until 30) {
            if (i < schedule.size) {
                val rowY = startY + i * stepY
                drawTextAt2(schedule[i].strokes.toString(), startX_Strokes, rowY)
                drawTextAt2("%.1f".format(schedule[i].pressure), startX_Pressure, rowY)
            }
        }

        // === ГРАФИК ===
        // Рисуем линию от Рнач (левый нижний угол) до Ркон (правый верхний)
        val gLeft = 887f
        val gBottom = 2935f
        val gRight = 2280f
        val gTop = 1370f

        // Переводим координаты графика в PDF-координаты
        fun toPdfX(x: Float) = (x / compressRatio2) * scale2 + offsetX2
        fun toPdfY(y: Float) = (y / compressRatio2) * scale2 + offsetY2

        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        // Начальная точка (0 ходов, Рнач) - на оси Y (слева)
        val startPointY = gBottom - ((WellKillCalculator.initialCirculatingPressure(input) / 15.0f) * (gBottom - gTop)).toFloat()
        // Конечная точка (E ходов, Ркон) - на оси X (справа)
        val endPointY = gBottom - ((WellKillCalculator.finalCirculatingPressure(input) / 15.0f) * (gBottom - gTop)).toFloat()
        
        // Ограничиваем точки, чтобы они не вылетали за сетку
        val safeStartY = startPointY.coerceIn(gTop, gBottom)
        val safeEndY = endPointY.coerceIn(gTop, gBottom)

        canvas.drawLine(toPdfX(gLeft), toPdfY(safeStartY), toPdfX(gRight), toPdfY(safeEndY), linePaint)

        document.finishPage(page)

        document.writeTo(outputStream)
        document.close()
    }
}
