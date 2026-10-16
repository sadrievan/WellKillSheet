package com.example.wellkillsheet

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Метод расчёта максимально допустимой плотности
enum class CalcMethod {
    TEST_PRESSURE,          // По давлению на устье при испытании (по умолчанию)
    FRAC_PRESSURE_OR_GRAD   // По давлению/градиенту гидроразрыва
}

data class WellKillInput(
    // Шапка
    val date: String = todayDate(),
    val engineer: String = "",

    // Метод расчёта (C)
    val calcMethod: CalcMethod = CalcMethod.TEST_PRESSURE,

    // Метод 1: испытание на приёмистость
    val testPressureA: Double = 0.0,        // (A) МПа
    val testDensityB: Double = 0.0,         // (B) кг/л

    // Метод 2: ГРП (одно поле — либо давление, либо градиент)
    val fracValue: Double = 0.0,            // МПа (если ≥ 0.1) или МПа/м (если < 0.1)

    // Текущие данные по скважине
    val casingShoeMD: Double = 0.0,         // Глубина башмака по стволу, м
    val casingShoeTVD: Double = 0.0,        // Глубина башмака по вертикали, м
    val wellDepthMD: Double = 0.0,          // Глубина скважины по стволу, м
    val wellDepthTVD: Double = 0.0,         // Глубина скважины по вертикали, м
    val currentMudDensity: Double = 0.0,    // Плотность раствора в скважине, кг/л

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

    // Насос
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
    private const val FRAC_THRESHOLD = 0.1  // порог между градиентом и давлением ГРП

    // ============ ФУНКЦИИ ОКРУГЛЕНИЯ ============

    // Вниз до десятых (floor к 0.1)
    fun roundDown1(x: Double): Double {
        return Math.floor(x * 10.0) / 10.0
    }

    // Вниз до сотых (floor к 0.01)
    fun roundDown2(x: Double): Double {
        return Math.floor(x * 100.0) / 100.0
    }

    // Вверх до десятых (строго: если есть что-то после — округляем)
    fun roundUp1(x: Double): Double {
        val scaled = x * 10.0
        val rounded = Math.round(scaled * 1e9) / 1e9
        return if (rounded > Math.floor(rounded)) {
            (Math.floor(rounded) + 1) / 10.0
        } else {
            rounded / 10.0
        }
    }

    // Вверх до сотых (строго: любое значение после сотых → вверх)
    fun roundUp2Strict(x: Double): Double {
        val scaled = x * 100.0
        val rounded = Math.round(scaled * 1e9) / 1e9
        return if (rounded > Math.floor(rounded)) {
            (Math.floor(rounded) + 1) / 100.0
        } else {
            rounded / 100.0
        }
    }

    // Вверх до целых
    fun roundUpInt(x: Double): Int {
        val rounded = Math.round(x * 1e9) / 1e9
        return Math.ceil(rounded).toInt()
    }

    // Математическое округление до целых (для ввода)
    fun roundMathInt(x: Double): Double {
        return Math.round(x).toDouble()
    }

    // ============ ОПРЕДЕЛЕНИЕ ТИПА ЗНАЧЕНИЯ ГРП ============

    // true — если значение интерпретируется как давление (≥ 0.1)
    // false — если как градиент (0 < X < 0.1)
    fun isFracPressure(value: Double): Boolean {
        return value >= FRAC_THRESHOLD
    }

    // ============ БЛОК 1: ПРОЧНОСТЬ ПОРОД ============

    // (C) Максимально допустимая плотность раствора, кг/л
    // Возвращает точное значение до округления — округление снаружи
    fun maxAllowableDensityRaw(input: WellKillInput): Double {
        if (input.casingShoeTVD <= 0.0) return 0.0

        return when (input.calcMethod) {
            CalcMethod.TEST_PRESSURE -> {
                input.testPressureA / (input.casingShoeTVD * CONST) + input.testDensityB
            }
            CalcMethod.FRAC_PRESSURE_OR_GRAD -> {
                if (input.fracValue < FRAC_THRESHOLD) {
                    // Градиент МПа/м
                    input.fracValue / CONST
                } else {
                    // Давление МПа
                    input.fracValue / (input.casingShoeTVD * CONST)
                }
            }
        }
    }

    // (C) с округлением вниз до сотых
    fun maxAllowableDensity(input: WellKillInput): Double {
        return roundDown2(maxAllowableDensityRaw(input))
    }

    // МДУДКП, МПа — точное значение до округления
    fun mdudkpRaw(input: WellKillInput): Double {
        val c = maxAllowableDensity(input)  // уже округлено
        return (c - input.currentMudDensity) * input.casingShoeTVD * CONST
    }

    // МДУДКП с округлением вниз до десятых
    fun mdudkp(input: WellKillInput): Double {
        return roundDown1(mdudkpRaw(input))
    }

    // ============ БЛОК 2: ДЛИНЫ (АВТО) ============

    fun openHoleLength(input: WellKillInput): Double {
        return (input.wellDepthMD - input.casingShoeMD).coerceAtLeast(0.0)
    }

    fun casedHoleLength(input: WellKillInput): Double {
        return input.casingShoeMD
    }

    // ============ БЛОК 3: ОБЪЁМЫ ============

    fun bhaVolumeRaw(input: WellKillInput) = input.bhaCapacity * input.bhaLength
    fun hwdpVolumeRaw(input: WellKillInput) = input.hwdpCapacity * input.hwdpLength
    fun dpVolumeRaw(input: WellKillInput) = input.dpCapacity * input.dpLength

    fun drillPipeVolume(input: WellKillInput): Int {
        val raw = bhaVolumeRaw(input) + hwdpVolumeRaw(input) + dpVolumeRaw(input)
        return roundUpInt(raw)
    }

    fun openHoleAnnulusVolume(input: WellKillInput): Int {
        val raw = input.openHoleAnnulusCapacity * openHoleLength(input)
        return roundUpInt(raw)
    }

    fun casedHoleAnnulusVolume(input: WellKillInput): Int {
        val raw = input.casedHoleAnnulusCapacity * casedHoleLength(input)
        return roundUpInt(raw)
    }

    fun totalAnnulusVolume(input: WellKillInput): Int {
        return openHoleAnnulusVolume(input) + casedHoleAnnulusVolume(input)
    }

    fun totalWellVolume(input: WellKillInput): Int {
        return drillPipeVolume(input) + totalAnnulusVolume(input)
    }

    fun totalVolumeWithSurface(input: WellKillInput): Int {
        return totalWellVolume(input) + roundUpInt(input.surfaceVolumeJ)
    }

    // ============ БЛОК 4: НАСОС ============

    // (E) Ходов до долота, округление вверх до целых
    fun strokesToBit(input: WellKillInput): Int {
        if (input.pumpOutput <= 0.0) return 0
        val raw = drillPipeVolume(input).toDouble() / input.pumpOutput
        return roundUpInt(raw)
    }

    // Ходов на полную циркуляцию, округление вверх
    fun strokesFullCirculation(input: WellKillInput): Int {
        if (input.pumpOutput <= 0.0) return 0
        val raw = totalVolumeWithSurface(input).toDouble() / input.pumpOutput
        return roundUpInt(raw)
    }

    // ============ БЛОК 5: ПРИТОК И ДАВЛЕНИЯ ============

    // Плотность раствора глушения, кг/л
    // Промежуточное округление: вверх до сотых (строго)
    fun killMudDensity(input: WellKillInput): Double {
        if (input.wellDepthTVD <= 0.0) return 0.0
        val raw = input.currentMudDensity + input.sidpp / (input.wellDepthTVD * CONST)
        return roundUp2Strict(raw)
    }

    // Рнач — начальное давление циркуляции, МПа
    // Округление вверх до десятых
    fun initialCirculatingPressure(input: WellKillInput): Double {
        val raw = input.pumpPressure + input.sidpp
        return roundUp1(raw)
    }

    // Ркон — конечное давление циркуляции, МПа
    // Округление вверх до десятых. Использует УЖЕ округлённую ρ_глуш
    fun finalCirculatingPressure(input: WellKillInput): Double {
        if (input.currentMudDensity <= 0.0) return 0.0
        val kill = killMudDensity(input)  // уже округлено
        val raw = (kill / input.currentMudDensity) * input.pumpPressure
        return roundUp1(raw)
    }

    // (К) = Рнач − Ркон, МПа
    fun pressureDifferenceK(input: WellKillInput): Double {
        return roundUp1(initialCirculatingPressure(input) - finalCirculatingPressure(input))
    }

    // Снижение давления на 100 ходов, МПа
    fun pressureDropPer100Strokes(input: WellKillInput): Double {
        val e = strokesToBit(input)
        if (e <= 0) return 0.0
        val raw = pressureDifferenceK(input) * 100.0 / e.toDouble()
        // Округляем до 3 знаков (для отображения)
        return Math.round(raw * 1000.0) / 1000.0
    }

    // ============ БЛОК 6: ТАБЛИЦА ГРАФИКА ============

    data class PressureRow(val strokes: Int, val pressure: Double)

    // Строит таблицу с авто-шагом, чтобы влезло в maxRows строк
    fun pressureSchedule(input: WellKillInput, maxRows: Int = 40): List<PressureRow> {
        val e = strokesToBit(input)
        if (e <= 0) return emptyList()

        val pStart = initialCirculatingPressure(input)
        val pEnd = finalCirculatingPressure(input)

        // Подбираем шаг: 100, 200, 300, ... такой, чтобы строк было <= maxRows
        var step = 100
        while ((e / step) + 2 > maxRows && step < 10000) {
            step += 100
        }

        val rows = mutableListOf<PressureRow>()
        rows.add(PressureRow(0, pStart))

        var strokes = step
        while (strokes < e) {
            val drop = pressureDifferenceK(input) * strokes.toDouble() / e.toDouble()
            val pressure = roundUp1(pStart - drop)
            rows.add(PressureRow(strokes, pressure))
            strokes += step
        }

        // Финальная строка
        rows.add(PressureRow(e, pEnd))
        return rows
    }

    // ============ ПРОВЕРКИ ============

    sealed class ValidationResult {
        object Ok : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    // Проверка ввода — вызывается при попытке экспорта или при переключении
    fun validateInput(input: WellKillInput): ValidationResult {
        // Проверка ГРП
        if (input.calcMethod == CalcMethod.FRAC_PRESSURE_OR_GRAD) {
            if (input.fracValue <= 0.0) {
                return ValidationResult.Error(
                    "Введены некорректные данные гидроразрыва. " +
                    "Значение должно быть больше нуля. Проверьте и введите заново."
                )
            }
        }

        // Проверка МДУДКП
        val mdudkp = mdudkp(input)
        if (mdudkp < 0.0) {
            return ValidationResult.Error(
                "Несовместимый интервал бурения. Пересмотрите данные."
            )
        }

        return ValidationResult.Ok
    }
}
