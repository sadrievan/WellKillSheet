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

    // Размер шрифта (в пикселях исходной картинки)
    private const val BASE_FONT_SIZE = 40f

    // Исходные размеры картинок ДО сжатия (важно для правильных координат!)
    private const val ORIGINAL_PAGE1_WIDTH = 2550f
    private const val ORIGINAL_PAGE2_WIDTH = 2409f

    // Функция для загрузки и сжатия картинки
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
        //        СТРАНИЦА 1 (Сжатая)
        // ==========================================
        val page1Bitmap = loadAndScale(context, R.drawable.page1, 1240)
        
        // Коэффициент, на который мы сжали картинку (2550 / 1240 ≈ 2.056)
        val compressRatio1 = ORIGINAL_PAGE1_WIDTH / page1Bitmap.width

        val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        var page = document.startPage(pageInfo1)
        var canvas = page.canvas

        val scale1 = min(PAGE_WIDTH.toFloat() / page1Bitmap.width, PAGE_HEIGHT.toFloat() / page1Bitmap.height)
        val offsetX1 = (PAGE_WIDTH - page1Bitmap.width * scale1) / 2f
        val offsetY1 = (PAGE_HEIGHT - page1Bitmap.height * scale1) / 2f

        val destRect1 = RectF(offsetX1, offsetY1, offsetX1 + page1Bitmap.width * scale1, offsetY1 + page1Bitmap.height * scale1)
        canvas.drawBitmap(page1Bitmap, null, destRect1, null)

        // Шрифт уменьшаем вместе с картинкой
        val pdfFontSize1 = (BASE_FONT_SIZE / compressRatio1) * scale1
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = pdfFontSize1
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        fun drawTextAt(text: String, x: Float, y: Float, paint: Paint = textPaint) {
            // ВОТ ГЛАВНОЕ ИСПРАВЛЕНИЕ: умножаем координаты на compressRatio1
            val adjustedX = x / compressRatio1
            val adjustedY = y / compressRatio1

            val pdfX = adjustedX * scale1 + offsetX1
            val pdfY = adjustedY * scale1 + offsetY1 + (pdfFontSize1 * 0.35f)
            canvas.drawText(text, pdfX, pdfY, paint)
        }

        // === ДАННЫЕ СТРАНИЦЫ 1 ===
        drawTextAt(input.date, 2005f, 135f)
        if (input.engineer.isNotEmpty()) {
            drawTextAt(input.engineer, 2005f, 210f)
        }

        drawTextAt("%.1f".format(input.testPressureA), 1100f, 530f)
        drawTextAt("%.2f".format(input.testDensityB), 1100f, 607f)
        drawTextAt("%.2f".format(WellKillCalculator.maxAllowableDensity(input)), 1100f, 780f)
        drawTextAt("%.2f".format(WellKillCalculator.mdudkp(input)), 1120f, 1023f)

        drawTextAt("%.2f".format(input.currentMudDensity), 1790f, 815f)
        drawTextAt("%.0f".format(input.casingShoeMD), 1780f, 1155f)
        drawTextAt("%.0f".format(input.casingShoeTVD), 1780f, 1235f)
        drawTextAt("%.0f".format(input.wellDepthMD), 1780f, 1540f)
        drawTextAt("%.0f".format(input.wellDepthTVD), 1780f, 1620f)

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

        drawTextAt(WellKillCalculator.drillPipeVolume(input).toString(), 1431f, 2210f)
        drawTextAt(WellKillCalculator.strokesToBit(input).toString(), 1785f, 2210f)
        drawTextAt(WellKillCalculator.openHoleAnnulusVolume(input).toString(), 1430f, 2455f)
        drawTextAt(WellKillCalculator.casedHoleAnnulusVolume(input).toString(), 1430f, 2565f)
        drawTextAt(WellKillCalculator.totalAnnulusVolume(input).toString(), 1400f, 2645f)
        drawTextAt(WellKillCalculator.totalWellVolume(input).toString(), 1400f, 2790f)
        drawTextAt(WellKillCalculator.roundUpInt(input.surfaceVolumeJ).toString(), 1400f, 2880f)

        document.finishPage(page)

        // ==========================================
        //        СТРАНИЦА 2 (Сжатая)
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
        
        // (Если будете рисовать текст на 2-й странице, не забудьте тоже делить координаты на compressRatio2)
        
        document.finishPage(page)

        document.writeTo(outputStream)
        document.close()
    }
}
