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
    private const val BASE_FONT_SIZE = 40f

    private const val ORIGINAL_PAGE1_WIDTH = 2550f
    private const val ORIGINAL_PAGE2_WIDTH = 2409f

    private fun loadAndScale(context: Context, resId: Int, targetWidth: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, options)
        
        var scale = 1
        if (options.outWidth > targetWidth) {
            while (options.outWidth / scale > targetWidth * 2) scale *= 2
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
            val pdfX = (x / compressRatio1) * scale1 + offsetX1
            val pdfY = (y / compressRatio1) * scale1 + offsetY1 + (pdfFontSize1 * 0.35f)
            canvas.drawText(text, pdfX, pdfY, paint)
        }

        // === ДАННЫЕ СТРАНИЦЫ 1 ===
        drawTextAt1(input.date, 2005f, 135f)
        if (input.engineer.isNotEmpty()) drawTextAt1(input.engineer, 2005f, 210f)

        // Правка 3: Оставляем (A) и (B) пустыми, если выбран градиент
        if (input.calcMethod == CalcMethod.TEST_PRESSURE) {
            drawTextAt1("%.1f".format(input.testPressureA), 1100f, 530f)
            drawTextAt1("%.2f".format(input.testDensityB), 1100f, 607f)
        }

        // Правка 1 и 2: Выводим градиент, если выбран он и он меньше 0.1
        if (input.calcMethod == CalcMethod.FRAC_PRESSURE_OR_GRAD) {
            if (input.fracValue < 0.1) {
                drawTextAt1("Градиент гидроразрыва, МПа/м", 500f, 1085f)
                drawTextAt1("%.4f".format(input.fracValue), 1110f, 1085f)
            }
        }

        drawTextAt1("%.2f".format(WellKillCalculator.maxAllowableDensity(input)), 1100f, 770f)
        drawTextAt1("%.2f".format(WellKillCalculator.mdudkp(input)), 1120f, 1023f)
        drawTextAt1("%.2f".format(input.currentMudDensity),
                            drawTextAt1("%.2f".format(input.currentMudDensity), 1790f, 815f)
        drawTextAt1("%.0f".format(input.casingShoeMD), 1780f, 1155f)
        drawTextAt1("%.0f".format(input.casingShoeTVD), 1780f, 1235f)
        drawTextAt1("%.0f".format(input.wellDepthMD), 1780f, 1540f)
        drawTextAt1("%.0f".format(input.wellDepthTVD), 1780f, 1620f)

        drawTextAt1("%.4f".format(input.pumpOutput), 425f, 1300f)
        drawTextAt1("%.1f".format(input.pumpPressure), 700f, 1650f)
        drawTextAt1("%.0f".format(input.pumpRate), 280f, 1650f)

        // Таблица объемов (левая часть)
        drawTextAt1("%.0f".format(WellKillCalculator.dpLength(input)), 900f, 1981f)
        drawTextAt1("%.2f".format(input.dpCapacity), 1135f, 1981f)
        drawTextAt1(WellKillCalculator.roundUpInt(WellKillCalculator.dpVolumeRaw(input)).toString(), 1415f, 1981f)
        
        drawTextAt1("%.0f".format(input.hwdpLength), 900f, 2065f)
        drawTextAt1("%.2f".format(input.hwdpCapacity), 1135f, 2065f)
        drawTextAt1(WellKillCalculator.roundUpInt(WellKillCalculator.hwdpVolumeRaw(input)).toString(), 1415f, 2065f)
        
        drawTextAt1("%.0f".format(input.bhaLength), 900f, 2140f)
        drawTextAt1("%.2f".format(input.bhaCapacity), 1135f, 2140f)
        drawTextAt1(WellKillCalculator.roundUpInt(WellKillCalculator.bhaVolumeRaw(input)).toString(), 1415f, 2140f)

        // (D) и (E)
        drawTextAt1(WellKillCalculator.drillPipeVolume(input).toString(), 1431f, 2210f)
        drawTextAt1(WellKillCalculator.strokesToBit(input).toString(), 1785f, 2210f)
        
        if (input.pumpRate > 0) {
            val timeToBit = WellKillCalculator.roundUpInt(WellKillCalculator.strokesToBit(input).toDouble() / input.pumpRate)
            drawTextAt1(timeToBit.toString(), 2160f, 2210f)
        }

        // (F)
        drawTextAt1(WellKillCalculator.openHoleAnnulusVolume(input).toString(), 1430f, 2455f)
        if (input.pumpOutput > 0) {
            val strokesF = WellKillCalculator.roundUpInt(WellKillCalculator.openHoleAnnulusVolume(input).toDouble() / input.pumpOutput)
            drawTextAt1(strokesF.toString(), 1760f, 2450f)
            if (input.pumpRate > 0) {
                val timeF = WellKillCalculator.roundUpInt(strokesF.toDouble() / input.pumpRate)
                drawTextAt1(timeF.toString(), 2160f, 2450f)
            }
        }

        // (G)
        drawTextAt1(WellKillCalculator.casedHoleAnnulusVolume(input).toString(), 1430f, 2565f)
        if (input.pumpOutput > 0) {
            val strokesG = WellKillCalculator.roundUpInt(WellKillCalculator.casedHoleAnnulusVolume(input).toDouble() / input.pumpOutput)
            drawTextAt1(strokesG.toString(), 1760f, 2560f)
            if (input.pumpRate > 0) {
                val timeG = WellKillCalculator.roundUpInt(strokesG.toDouble() / input.pumpRate)
                drawTextAt1(timeG.toString(), 2160f, 2560f)
            }
        }

        // (H)
        drawTextAt1(WellKillCalculator.totalAnnulusVolume(input).toString(), 1400f, 2645f)
        if (input.pumpOutput > 0) {
            val strokesH = WellKillCalculator.roundUpInt(WellKillCalculator.totalAnnulusVolume(input).toDouble() / input.pumpOutput)
            drawTextAt1(strokesH.toString(), 1760f, 2640f)
            if (input.pumpRate > 0) {
                val timeH = WellKillCalculator.roundUpInt(strokesH.toDouble() / input.pumpRate)
                drawTextAt1(timeH.toString(), 2160f, 2640f)
            }
        }

        // (I)
        drawTextAt1(WellKillCalculator.totalWellVolume(input).toString(), 1400f, 2790f)
        if (input.pumpOutput > 0) {
            val strokesI = WellKillCalculator.roundUpInt(WellKillCalculator.totalWellVolume(input).toDouble() / input.pumpOutput)
            drawTextAt1(strokesI.toString(), 1760f, 2790f)
            if (input.pumpRate > 0) {
                val timeI = WellKillCalculator.roundUpInt(strokesI.toDouble() / input.pumpRate)
                drawTextAt1(timeI.toString(), 2160f, 2790f)
            }
        }

        // (J)
        drawTextAt1(WellKillCalculator.roundUpInt(input.surfaceVolumeJ).toString(), 1400f, 2880f)
        if (input.pumpOutput > 0) {
            val strokesJ = WellKillCalculator.roundUpInt(WellKillCalculator.roundUpInt(input.surfaceVolumeJ).toDouble() / input.pumpOutput)
            drawTextAt1(strokesJ.toString(), 1760f, 2875f)
        }

        // (I+J)
        drawTextAt1(WellKillCalculator.totalVolumeWithSurface(input).toString(), 1330f, 2965f)
        if (input.pumpOutput > 0) {
            val strokesIJ = WellKillCalculator.roundUpInt(WellKillCalculator.totalVolumeWithSurface(input).toDouble() / input.pumpOutput)
            drawTextAt1(strokesIJ.toString(), 1760f, 2965f)
        }

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
            val pdfX = (x / compressRatio2) * scale2 + offsetX2
            val pdfY = (y / compressRatio2) * scale2 + offsetY2 + (pdfFontSize2 * 0.35f)
            canvas.drawText(text, pdfX, pdfY, paint)
        }

        // === ДАННЫЕ СТРАНИЦЫ 2 ===
        
        // Шапка
        drawTextAt2(input.date, 1900f, 155f)
        if (input.engineer.isNotEmpty()) drawTextAt2(input.engineer, 1900f, 230f)

        // Приток
        drawTextAt2("%.1f".format(input.sidpp), 680f, 435f)
        drawTextAt2("%.1f".format(input.sicp), 1350f, 435f)
        drawTextAt2("%.0f".format(input.influxVolume), 2000f, 435f)

        // Формула плотности глушения
        drawTextAt2("%.2f".format(input.currentMudDensity), 740f, 620f)
        drawTextAt2("%.1f".format(input.sidpp), 1140f, 600f)
        
        val tvdDenominator = Math.round((input.casingShoeTVD * 0.00981) * 100.0) / 100.0
        drawTextAt2("%.2f".format(tvdDenominator), 1120f, 660f)

        // Результаты и данные
        drawTextAt2("%.2f".format(WellKillCalculator.killMudDensity(input)), 2100f, 640f) 
        drawTextAt2("%.1f".format(WellKillCalculator.initialCirculatingPressure(input)), 2100f, 860f) 
        drawTextAt2("%.1f".format(WellKillCalculator.finalCirculatingPressure(input)), 2100f, 1120f) 
        drawTextAt2("%.2f".format(WellKillCalculator.killMudDensity(input)), 750f, 1100f) 
        drawTextAt2("%.1f".format(input.pumpPressure), 1000f, 1100f) 

        // Формула Рнач
        drawTextAt2("%.1f".format(input.pumpPressure), 740f, 855f)
        drawTextAt2("%.1f".format(input.sidpp), 980f, 855f)

        // Плотность раствора в скважине
        drawTextAt2("%.2f".format(input.currentMudDensity), 750f, 1150f)

        // Строка (К) = Рнач - Ркон
        drawTextAt2("=", 420f, 1230f)
        drawTextAt2("%.1f".format(WellKillCalculator.initialCirculatingPressure(input)), 520f, 1230f)
        drawTextAt2("-", 620f, 1230f)
        drawTextAt2("%.1f".format(WellKillCalculator.finalCirculatingPressure(input)), 720f, 1230f)
        drawTextAt2("=", 820f, 1230f)
        drawTextAt2("%.1f".format(WellKillCalculator.pressureDifferenceK(input)), 920f, 1230f)

        // Снижение давления
        drawTextAt2("%.3f".format(WellKillCalculator.pressureDropPer100Strokes(input)), 1600f, 1250f)

        // Таблица 30 строк
        val schedule = WellKillCalculator.pressureSchedule(input, maxRows = 30)
        val startX_Strokes = 270f
        val startX_Pressure = 500f
        val startY = 1580f
        val stepY = 59f 

        for (i in 0 until 30) {
            val rowY = startY + i * stepY
            if (i < schedule.size) {
                drawTextAt2(schedule[i].strokes.toString(), startX_Strokes, rowY)
                drawTextAt2("%.1f".format(schedule[i].pressure), startX_Pressure, rowY)
            }
        }

        // График
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        
        val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = (30f / compressRatio2) * scale2
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        val labelPaintX = Paint().apply {
            color = Color.BLACK
            textSize = (30f / compressRatio2) * scale2
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val gLeft = 890f
        val gBottom = 2932f
        val gRight = 2280f
        val gTop = 1431f

        fun toPdfX(x: Float) = (x / compressRatio2) * scale2 + offsetX2
        fun toPdfY(y: Float) = (y / compressRatio2) * scale2 + offsetY2

        val maxPressure = 13f
        for (p in 0..13 step 2) {
            val yPos = gBottom - (p.toFloat() / maxPressure) * (gBottom - gTop)
            canvas.drawText("$p", toPdfX(gLeft - 20f), toPdfY(yPos) + (pdfFontSize2 * 0.35f), labelPaint)
        }

        val maxStrokes = 2400f
        for (s in 0..2400 step 600) {
            val xPos = gLeft + (s.toFloat() / maxStrokes) * (gRight - gLeft)
            canvas.drawText("$s", toPdfX(xPos), toPdfY(gBottom + 60f), labelPaintX)
        }

        val pStart = WellKillCalculator.initialCirculatingPressure(input)
        val pEnd = WellKillCalculator.finalCirculatingPressure(input)
        val e = WellKillCalculator.strokesToBit(input).toFloat()

        val startPxY = gBottom - (pStart.toFloat() / maxPressure) * (gBottom - gTop)
        val endPxX = gLeft + (e / maxStrokes) * (gRight - gLeft)
        val endPxY = gBottom - (pEnd.toFloat() / maxPressure) * (gBottom - gTop)

        val safeStartY = startPxY.coerceIn(gTop, gBottom)
        val safeEndX = endPxX.coerceIn(gLeft, gRight)
        val safeEndY = endPxY.coerceIn(gTop, gBottom)

        // Линия падения давления
        canvas.drawLine(toPdfX(gLeft), toPdfY(safeStartY), toPdfX(safeEndX), toPdfY(safeEndY), linePaint)

        // Горизонтальная линия (плато) от Ркон до 2400 ходов
        val endOfLineStrokes = 2400f
        val plateauEndX = gLeft + (endOfLineStrokes / maxStrokes) * (gRight - gLeft)
        val safePlateauX = plateauEndX.coerceIn(gLeft, gRight)
        canvas.drawLine(toPdfX(safeEndX), toPdfY(safeEndY), toPdfX(safePlateauX), toPdfY(safeEndY), linePaint)

        document.finishPage(page)

        document.writeTo(outputStream)
        document.close()
    }
}
