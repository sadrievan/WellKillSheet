package com.example.wellkillsheet

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CalcMethod {
    TEST_PRESSURE,
    FRAC_PRESSURE_OR_GRAD
}

data class WellKillInput(
    val date: String = todayDate(),
    val engineer: String = "",
    val calcMethod: CalcMethod = CalcMethod.TEST_PRESSURE,
    val testPressureA: Double = 0.0,
    val testDensityB: Double = 0.0,
    val fracValue: Double = 0.0,
    val casingShoeMD: Double = 0.0,
    val casingShoeTVD: Double = 0.0,
    val wellDepthMD: Double = 0.0,
    val wellDepthTVD: Double = 0.0,
    val currentMudDensity: Double = 0.0,
    val bhaLength: Double = 0.0,
    val bhaCapacity: Double = 0.0,
    val hwdpLength: Double = 0.0,
    val hwdpCapacity: Double = 0.0,
    val dpCapacity: Double = 0.0,
    val osBhaCapacity: Double = 0.0,
    val osHwdpBiCapacity: Double = 0.0,
    val okBiCapacity: Double = 0.0,
    val bhaInOsLength: Double = 0.0,
    val hwdpInOsLength: Double = 0.0,
    val surfaceVolumeJ: Double = 0.0,
    val pumpRate: Double = 0.0,
    val pumpOutput: Double = 0.0,
    val pumpPressure: Double = 0.0,
    val sidpp: Double = 0.0,
    val sicp: Double = 0.0,
    val influxVolume: Double = 0.0
)

fun todayDate(): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Calendar.getInstance().time)
}

object WellKillCalculator {

    private const val CONST = 0.00981
    private const val FRAC_THRESHOLD = 0.1

    fun roundDown1(x: Double): Double = Math.floor(x * 10.0) / 10.0
    fun roundDown2(x: Double): Double = Math.floor(x * 100.0) / 100.0

    fun roundUp1(x: Double): Double {
        val scaled = x * 10.0
        val rounded = Math.round(scaled * 1e9) / 1e9
        return if (rounded > Math.floor(rounded)) (Math.floor(rounded) + 1) / 10.0 else rounded / 10.0
    }

    fun roundUp2Strict(x: Double): Double {
        val scaled = x * 100.0
        val rounded = Math.round(scaled * 1e9) / 1e9
        return if (rounded > Math.floor(rounded)) (Math.floor(rounded) + 1) / 100.0 else rounded / 100.0
    }

    fun roundUpInt(x: Double): Int {
        val rounded = Math.round(x * 1e9) / 1e9
        return Math.ceil(rounded).toInt()
    }

    fun isFracPressure(value: Double): Boolean = value >= FRAC_THRESHOLD

    fun maxAllowableDensityRaw(input: WellKillInput): Double {
        if (input.casingShoeTVD <= 0.0) return 0.0
        return when (input.calcMethod) {
            CalcMethod.TEST_PRESSURE -> input.testPressureA / (input.casingShoeTVD * CONST) + input.testDensityB
            CalcMethod.FRAC_PRESSURE_OR_GRAD -> if (input.fracValue < FRAC_THRESHOLD) input.fracValue / CONST else input.fracValue / (input.casingShoeTVD * CONST)
        }
    }

    fun maxAllowableDensity(input: WellKillInput): Double = roundDown2(maxAllowableDensityRaw(input))

    fun mdudkpRaw(input: WellKillInput): Double {
        val c = maxAllowableDensity(input)
        return (c - input.currentMudDensity) * input.casingShoeTVD * CONST
    }

    fun mdudkp(input: WellKillInput): Double = roundDown1(mdudkpRaw(input))

    fun dpLength(input: WellKillInput): Double = (input.wellDepthMD - input.bhaLength - input.hwdpLength).coerceAtLeast(0.0)
    fun openHoleLength(input: WellKillInput): Double = (input.wellDepthMD - input.casingShoeMD).coerceAtLeast(0.0)
    fun casedHoleLength(input: WellKillInput): Double = input.casingShoeMD

    fun bhaVolumeRaw(input: WellKillInput) = input.bhaCapacity * input.bhaLength
    fun hwdpVolumeRaw(input: WellKillInput) = input.hwdpCapacity * input.hwdpLength
    fun dpVolumeRaw(input: WellKillInput) = input.dpCapacity * dpLength(input)

    fun drillPipeVolume(input: WellKillInput): Int = roundUpInt(bhaVolumeRaw(input) + hwdpVolumeRaw(input) + dpVolumeRaw(input))

    fun openHoleAnnulusVolume(input: WellKillInput): Int {
        val f1 = input.bhaInOsLength * input.osBhaCapacity
        val osLength = openHoleLength(input)
        val f2 = (osLength - input.bhaInOsLength) * input.osHwdpBiCapacity
        return roundUpInt(f1 + f2)
    }

    fun casedHoleAnnulusVolume(input: WellKillInput): Int = roundUpInt(input.casingShoeMD * input.okBiCapacity)

    fun totalAnnulusVolume(input: WellKillInput): Int = openHoleAnnulusVolume(input) + casedHoleAnnulusVolume(input)
    fun totalWellVolume(input: WellKillInput): Int = drillPipeVolume(input) + totalAnnulusVolume(input)
    fun totalVolumeWithSurface(input: WellKillInput): Int = totalWellVolume(input) + roundUpInt(input.surfaceVolumeJ)

    fun strokesToBit(input: WellKillInput): Int = if (input.pumpOutput <= 0.0) 0 else roundUpInt(drillPipeVolume(input).toDouble() / input.pumpOutput)
    fun strokesFullCirculation(input: WellKillInput): Int = if (input.pumpOutput <= 0.0) 0 else roundUpInt(totalVolumeWithSurface(input).toDouble() / input.pumpOutput)

    fun killMudDensity(input: WellKillInput): Double {
        if (input.wellDepthTVD <= 0.0) return 0.0
        return roundUp2Strict(input.currentMudDensity + input.sidpp / (input.wellDepthTVD * CONST))
    }

    fun initialCirculatingPressure(input: WellKillInput): Double = roundUp1(input.pumpPressure + input.sidpp)

    fun finalCirculatingPressure(input: WellKillInput): Double {
        if (input.currentMudDensity <= 0.0) return 0.0
        val kill = killMudDensity(input)
        return roundUp1((kill / input.currentMudDensity) * input.pumpPressure)
    }

    fun pressureDifferenceK(input: WellKillInput): Double = roundUp1(initialCirculatingPressure(input) - finalCirculatingPressure(input))

    fun pressureDropPer100Strokes(input: WellKillInput): Double {
        val e = strokesToBit(input)
        if (e <= 0) return 0.0
        return Math.round((pressureDifferenceK(input) * 100.0 / e.toDouble()) * 1000.0) / 1000.0
    }

    data class PressureRow(val strokes: Int, val pressure: Double)

    fun pressureSchedule(input: WellKillInput, maxRows: Int = 40): List<PressureRow> {
        val e = strokesToBit(input)
        if (e <= 0) return emptyList()
        val pStart = initialCirculatingPressure(input)
        val pEnd = finalCirculatingPressure(input)
        var step = 100
        while ((e / step) + 2 > maxRows && step < 10000) step += 100
        val rows = mutableListOf<PressureRow>()
        rows.add(PressureRow(0, pStart))
        var strokes = step
        while (strokes < e) {
            val drop = pressureDifferenceK(input) * strokes.toDouble() / e.toDouble()
            rows.add(PressureRow(strokes, roundUp1(pStart - drop)))
            strokes += step
        }
        rows.add(PressureRow(e, pEnd))
        return rows
    }

    sealed class ValidationResult {
        object Ok : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    fun validateInput(input: WellKillInput): ValidationResult {
        // Правка 4: Проверка плотности
        if (input.currentMudDensity > 3.99) {
            return ValidationResult.Error("Поправьте значение плотности раствора. Оно не может быть больше 3,99 кг/л.")
        }

        if (input.calcMethod == CalcMethod.FRAC_PRESSURE_OR_GRAD) {
            if (input.fracValue <= 0.0) return ValidationResult.Error("Введены некорректные данные гидроразрыва. Значение должно быть больше нуля.")
        }
        if (mdudkp(input) < 0.0) return ValidationResult.Error("Несовместимый интервал бурения. Пересмотрите данные.")
        val dpLen = input.wellDepthMD - input.bhaLength - input.hwdpLength
        if (dpLen < 0) return ValidationResult.Error("Сумма длин КНБК и ТБТ превышает глубину скважины.")
        if (input.bhaInOsLength > input.bhaLength) return ValidationResult.Error("Длина КНБК в открытом стволе превышает общую длину КНБК.")
        if (input.hwdpInOsLength > input.hwdpLength) return ValidationResult.Error("Длина ТБТ в открытом стволе превышает общую длину ТБТ.")
        
        val osLen = openHoleLength(input)
        if (input.bhaInOsLength + input.hwdpInOsLength > osLen && osLen > 0) {
            return ValidationResult.Error("Сумма длин КНБК и ТБТ в открытом стволе превышает длину открытого ствола.")
        }

        return ValidationResult.Ok
    }
}
