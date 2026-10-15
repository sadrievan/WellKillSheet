package com.example.wellkillsheet

data class WellKillInput(
    // Шапка
    val date: String = "",
    val engineer: String = "",

    // Данные о прочности пород
    val testPressureA: Double = 0.0,        // (A) МПа
    val testDensityB: Double = 0.0,         // (B) кг/л
    val casingShoeTVD: Double = 0.0,        // ГСВ до башмака, м

    // Текущие данные по скважине
    val wellDepthMD: Double = 0.0,          // м
    val wellDepthTVD: Double = 0.0,         // м
    val currentMudDensity: Double = 0.0,    // кг/л
    val openHoleLength: Double = 0.0,       // м
    val casedHoleLength: Double = 0.0,      // м

    // Бурильная колонна
    val bhaLength: Double = 0.0,            // КНБК длина, м
    val bhaCapacity: Double = 0.0,          // КНБК уд. объём, л/м
    val hwdpLength: Double = 0.0,           // ТБТ длина, м
    val hwdpCapacity: Double = 0.0,         // ТБТ уд. объём, л/м
    val dpLength: Double = 0.0,             // БТ длина, м
    val dpCapacity: Double = 0.0,           // БТ уд. объём, л/м

    // Кольцевое пространство
    val openHoleAnnulusCapacity: Double = 0.0,  // л/м
    val casedHoleAnnulusCapacity: Double = 0.0, // л/м

    // Наземная обвязка
    val surfaceVolumeJ: Double = 0.0,       // (J) л

    // Насосы
    val pump1Rate: Double = 0.0,            // ход/мин
    val pump1Output: Double = 0.0,          // л/ход
    val pump2Rate: Double = 0.0,            // ход/мин
    val pump2Output: Double = 0.0,          // л/ход
    val activePump: Int = 1,                // 1 или 2

    // Давление прокачки
    val pumpPressure: Double = 0.0,         // МПа

    // Приток
    val sidpp: Double = 0.0,                // МПа
    val sicp: Double = 0.0,                 // МПа
    val influxVolume: Double = 0.0          // л
)

object WellKillCalculator {

    private const val CONST = 0.00981  // МПа / (кг/л × м)

    // ---- Блок 1. Прочность пород ----

    // (C) Максимально допустимая плотность раствора, кг/л
    fun maxAllowableDensity(input: WellKillInput): Double {
        if (input.casingShoeTVD <= 0.0) return 0.0
        return input.testPressureA / (input.casingShoeTVD * CONST) + input.testDensityB
    }

    // МДУДКП — Максимальное допустимое давление в кольцевом пространстве, МПа
    fun mdudkp(input: WellKillInput): Double {
        val c = maxAllowableDensity(input)
        return (c - input.currentMudDensity) * input.casingShoeTVD * CONST
    }

    // ---- Блок 2. Объёмы ----

    fun drillPipeVolume(input: WellKillInput): Double {
        return input.bhaCapacity * input.bhaLength +
               input.hwdpCapacity * input.hwdpLength +
               input.dpCapacity * input.dpLength
    }

    fun openHoleAnnulusVolume(input: WellKillInput): Double {
        return input.openHoleAnnulusCapacity * input.openHoleLength
    }

    fun casedHoleAnnulusVolume(input: WellKillInput): Double {
        return input.casedHoleAnnulusCapacity * input.casedHoleLength
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

    // ---- Блок 3. Насос ----

    fun activePumpOutput(input: WellKillInput): Double {
        return if (input.activePump == 2) input.pump2Output else input.pump1Output
    }

    fun activePumpRate(input: WellKillInput): Double {
        return if (input.activePump == 2) input.pump2Rate else input.pump1Rate
    }

    // (E) Ходов насоса на прокачку объёма БК до долота
    fun strokesToBit(input: WellKillInput): Double {
        val q = activePumpOutput(input)
        if (q <= 0.0) return 0.0
        return drillPipeVolume(input) / q
    }

    // Ходов на полную циркуляцию (I + J)
    fun strokesFullCirculation(input: WellKillInput): Double {
        val q = activePumpOutput(input)
        if (q <= 0.0) return 0.0
        return totalVolumeWithSurface(input) / q
    }

    // ---- Блок 4. Приток и давления ----

    // Плотность раствора глушения, кг/л
    fun killMudDensity(input: WellKillInput): Double {
        if (input.wellDepthTVD <= 0.0) return 0.0
        return input.currentMudDensity + input.sidpp / (input.wellDepthTVD * CONST)
    }

    // Начальное давление циркуляции, МПа
    fun initialCirculatingPressure(input: WellKillInput): Double {
        return input.pumpPressure + input.sidpp
    }

    // Конечное давление циркуляции, МПа
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

    // ---- Блок 5. Таблица снижения давления ----

    data class PressureRow(val strokes: Int, val pressure: Double)

    // Строит таблицу от Рнач до Ркон с шагом 100 ходов
    fun pressureSchedule(input: WellKillInput): List<PressureRow> {
        val e = strokesToBit(input)
        if (e <= 0.0) return emptyList()

        val pStart = initialCirculatingPressure(input)
        val pEnd = finalCirculatingPressure(input)
        val dropPer100 = pressureDropPer100Strokes(input)

        val rows = mutableListOf<PressureRow>()
        rows.add(PressureRow(0, pStart))

        var strokes = 100
        while (strokes < e.toInt()) {
            rows.add(PressureRow(strokes, pStart - dropPer100 * (strokes / 100.0)))
            strokes += 100
        }

        // финальная строка — фактическое (E) и Ркон
        rows.add(PressureRow(e.toInt(), pEnd))
        return rows
    }
}
