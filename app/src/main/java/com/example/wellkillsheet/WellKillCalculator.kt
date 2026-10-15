package com.example.wellkillsheet

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class WellKillInput(
    // Шапка
    val date: String = todayDate(),
    val engineer: String = "",

    // Данные о прочности пород
    val testPressureA: Double = 0.0,        // (A) МПа
    val testDensityB: Double = 0.0,         // (B) кг/л
    val casingShoeTVD: Double = 0.0,        // Глубина башмака по вертикали, м

    // Текущие данные по скважине
    val casingShoeMD: Double = 0.0,         // Глубина башмака по стволу, м
    val wellDepthMD: Double = 0.0,          // Глубина скважины по стволу, м
    val wellDepthTVD: Double = 0.0,         // Глубина скважины по вертикали, м
    val currentMudDensity: Double = 0.0,    // кг/л

    // Бурильная колонна
    val bhaLength: Double = 0.0,
    val bhaCapacity: Double = 0.0,
    val hwdpLength: Double = 0.0,
    val hwdpCapacity: Double = 0.0,
    val dpLength: Double = 0.0,
    val dpCapacity: Double = 0.0,

    // Кольцевое пространство
    val openHoleAnnulusCapacity: Double = 0.0,
    val casedHoleAnnulusCapacity: Double = 0.0,

    // Наземная обвязка
    val surfaceVolumeJ: Double = 0.0,

    // Насос (один)
    val pumpRate: Double = 0.0,             // Скорость насоса при тесте на МХ, ход/мин
    val pumpOutput: Double = 0.0,           // Удельный объём, л/ход
    val pumpPressure: Double = 0.0,         // Давление прокачки, МПа

    // Приток
    val sidpp: Double = 0.0,                // Давление стабилизации в трубах, МПа
    val sicp: Double = 0.0,                 // Давление стабилизации в КП, МПа
    val influxVolume: Double = 0.0          // Объём притока, л
)

fun todayDate(): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Calendar.getInstance().time)
}

object WellKillCalculator {

    private const val CONST = 0.00981  // МПа / (кг/л × м)

    // ---- Блок 1. Прочность пород ----

    // (C) Максимально допустимая плотность раствора, кг/л (по TVD башмака)
    fun maxAllowableDensity(input: WellKillInput): Double {
        if (input.casingShoeTVD <= 0.0) return 0.0
        return input.testPressureA / (input.casingShoeTVD * CONST) + input.testDensityB
    }

    // МДУДКП — Максимальное допустимое давление в кольцевом пространстве, МПа
    fun mdudkp(input: WellKillInput): Double {
        val c = maxAllowableDensity(input)
        return (c - input.currentMudDensity) * input.casingShoeTVD * CONST
    }

    // ---- Блок 2. Длины (авто) ----

    // Длина открытого ствола = MD_скв − MD_башмак
    fun openHoleLength(input: WellKillInput): Double {
        return (input.wellDepthMD - input.casingShoeMD).coerceAtLeast(0.0)
    }

    // Длина обсадной части = MD_башмак
    fun casedHoleLength(input: WellKillInput): Double {
        return input.casingShoeMD
    }

    // ---- Блок 3. Объёмы ----

    fun bhaVolume(input: WellKillInput) = input.bhaCapacity * input.bhaLength
    fun hwdpVolume(input: WellKillInput) = input.hwdpCapacity * input.hwdpLength
    fun dpVolume(input: WellKillInput) = input.dpCapacity * input.dpLength

    fun drillPipeVolume(input: WellKillInput): Double {
        return bhaVolume(input) + hwdpVolume(input) + dpVolume(input)
    }

    fun openHoleAnnulusVolume(input: WellKillInput): Double {
        return input.openHoleAnnulusCapacity * openHoleLength(input)
    }

    fun casedHoleAnnulusVolume(input: WellKillInput): Double {
        return input.casedHoleAnnulusCapacity * casedHoleLength(input)
    }

    fun totalAnnulusVolume(input: WellKillInput): Double {
        return openHoleAnnulusVolume(input) + casedHoleAnnulusVolume(input)
    }

    fun totalWellVolume(input: WellKillInput): Double {
        return drillPipeVolume(input) + totalAnnulusVolume(input)
    }

    fun totalVolumeWithSurface(input: WellKillInput): Double {
        return totalWellVolume(input) + input.surfaceVolumeJ
    }

    // ---- Блок 4. Насос ----

    // (E) Ходов до долота
    fun strokesToBit(input: WellKillInput): Double {
        if (input.pumpOutput <= 0.0) return 0.0
        return drillPipeVolume(input) / input.pumpOutput
    }

    // Ходов на полную циркуляцию
    fun strokesFullCirculation(input: WellKillInput): Double {
        if (input.pumpOutput <= 0.0) return 0.0
        return totalVolumeWithSurface(input) / input.pumpOutput
    }

    // ---- Блок 5. Приток и давления ----

    // Плотность раствора глушения, кг/л (по TVD скважины)
    fun killMudDensity(input: WellKillInput): Double {
        if (input.wellDepthTVD <= 0.0) return 0.0
        return input.currentMudDensity + input.sidpp / (input.wellDepthTVD * CONST)
    }

    // Рнач — начальное давление циркуляции, МПа
    fun initialCirculatingPressure(input: WellKillInput): Double {
        return input.pumpPressure + input.sidpp
    }

    // Ркон — конечное давление циркуляции, МПа
    fun finalCirculatingPressure(input: WellKillInput): Double {
        if (input.currentMudDensity <= 0.0) return 0.0
        val kill = killMudDensity(input)
        return (kill / input.currentMudDensity) * input.pumpPressure
    }

    // (К) = Рнач − Ркон, МПа
    fun pressureDifferenceK(input: WellKillInput): Double {
        return initialCirculatingPressure(input) - finalCirculatingPressure(input)
    }

    // Снижение давления на 100 ходов, МПа
    fun pressureDropPer100Strokes(input: WellKillInput): Double {
        val e = strokesToBit(input)
        if (e <= 0.0) return 0.0
        return pressureDifferenceK(input) * 100.0 / e
    }

    // ---- Блок 6. Таблица графика ----

    data class PressureRow(val strokes: Int, val pressure: Double)

    // Строит таблицу с авто-шагом, чтобы влезло в maxRows строк
    fun pressureSchedule(input: WellKillInput, maxRows: Int = 40): List<PressureRow> {
        val e = strokesToBit(input)
        if (e <= 0.0) return emptyList()

        val pStart = initialCirculatingPressure(input)
        val pEnd = finalCirculatingPressure(input)

        // Подбираем шаг: 100, 200, 300, ... такой, чтобы строк было <= maxRows
        var step = 100
        while ((e / step).toInt() + 2 > maxRows && step < 10000) {
            step += 100
        }

        val rows = mutableListOf<PressureRow>()
        rows.add(PressureRow(0, pStart))

        var strokes = step
        while (strokes < e.toInt()) {
            val drop = pressureDifferenceK(input) * strokes / e
            rows.add(PressureRow(strokes, pStart - drop))
            strokes += step
        }

        // финальная строка
        rows.add(PressureRow(e.toInt(), pEnd))
        return rows
    }
}
