package com.example.wellkillsheet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object PdfGenerator {

    fun generate(
        context: Context,
        input: WellKillInput,
        outputStream: OutputStream
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val left = 40f
        var y = 60f
        val lineHeight = 22f

        canvas.drawText("WELL KILL SHEET", left, y, titlePaint)
        y += lineHeight * 1.5f
        canvas.drawLine(left, y, 555f, y, linePaint)
        y += lineHeight

        fun section(title: String, rows: List<Pair<String, String>>) {
            canvas.drawText(title, left, y, titlePaint.apply { textSize = 13f })
            y += lineHeight
            for ((label, value) in rows) {
                canvas.drawText(label, left, y, textPaint)
                canvas.drawText(value, left + 320f, y, textPaint)
                y += lineHeight
            }
            y += 8f
        }

        section("1. INPUT DATA", listOf(
            "Well Depth (MD), m" to "%.1f".format(input.wellDepthMD),
            "Well Depth (TVD), m" to "%.1f".format(input.wellDepthTVD),
            "Casing Shoe (TVD), m" to "%.1f".format(input.casingShoeTVD),
            "Current Mud Density, kg/l" to "%.2f".format(input.currentMudDensity),
            "SIDPP, bar" to "%.1f".format(input.sidpp),
            "SICP, bar" to "%.1f".format(input.sicp),
            "Pump Output, l/stroke" to "%.2f".format(input.pumpOutput),
            "Slow Pump Rate, spm" to "%.0f".format(input.slowPumpRate),
            "Slow Circulating Pressure, bar" to "%.1f".format(input.slowCirculatingPressure),
            "Drill Pipe Capacity, l/m" to "%.4f".format(input.drillPipeCapacity),
            "Annulus Capacity, l/m" to "%.4f".format(input.drillPipeAnnulusCapacity)
        ))

        val killDensity = WellKillCalculator.killMudDensity(input)
        val icp = WellKillCalculator.initialCirculatingPressure(input)
        val fcp = WellKillCalculator.finalCirculatingPressure(input)
        val maasp = WellKillCalculator.maasp(input)
        val strokesToBit = WellKillCalculator.strokesToBit(input)
        val strokesToSurface = WellKillCalculator.strokesBitToSurface(input)
        val totalStrokes = WellKillCalculator.totalStrokes(input)
        val circTime = WellKillCalculator.circulationTimeMinutes(totalStrokes, input.slowPumpRate)

        section("2. CALCULATED VALUES", listOf(
            "Kill Mud Density, kg/l" to "%.2f".format(killDensity),
            "Initial Circulating Pressure, bar" to "%.1f".format(icp),
            "Final Circulating Pressure, bar" to "%.1f".format(fcp),
            "MAASP, bar" to "%.1f".format(maasp),
            "Strokes to Bit" to "%.0f".format(strokesToBit),
            "Strokes Bit to Surface" to "%.0f".format(strokesToSurface),
            "Total Strokes" to "%.0f".format(totalStrokes),
            "Circulation Time, min" to "%.0f".format(circTime)
        ))

        canvas.drawText("Signature: ____________________", left, y + 40f, textPaint)
        canvas.drawText("Date: ____________________", left + 320f, y + 40f, textPaint)

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }
}