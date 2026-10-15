package com.example.wellkillsheet

data class WellKillInput(
    val wellDepthMD: Double = 0.0,
    val wellDepthTVD: Double = 0.0,
    val casingShoeTVD: Double = 0.0,
    val currentMudDensity: Double = 0.0,
    val sidpp: Double = 0.0,
    val sicp: Double = 0.0,
    val pumpOutput: Double = 0.0,
    val slowPumpRate: Double = 0.0,
    val slowCirculatingPressure: Double = 0.0,
    val drillPipeCapacity: Double = 0.0,
    val drillPipeAnnulusCapacity: Double = 0.0,
    val maxAllowableDensity: Double = 0.0
)

object WellKillCalculator {

    fun killMudDensity(input: WellKillInput): Double {
        if (input.wellDepthTVD <= 0) return 0.0
        return input.currentMudDensity + (input.sidpp * 10.2) / input.wellDepthTVD
    }

    fun initialCirculatingPressure(input: WellKillInput): Double {
        return input.slowCirculatingPressure + input.sidpp
    }

    fun finalCirculatingPressure(input: WellKillInput): Double {
        if (input.currentMudDensity <= 0) return 0.0
        val killDensity = killMudDensity(input)
        return input.slowCirculatingPressure * (killDensity / input.currentMudDensity)
    }

    fun maasp(input: WellKillInput): Double {
        return (input.maxAllowableDensity - input.currentMudDensity) *
                input.casingShoeTVD * 0.0981
    }

    fun drillStringVolume(input: WellKillInput): Double {
        return input.drillPipeCapacity * input.wellDepthMD
    }

    fun strokesToBit(input: WellKillInput): Double {
        if (input.pumpOutput <= 0) return 0.0
        return drillStringVolume(input) / input.pumpOutput
    }

    fun annulusVolume(input: WellKillInput): Double {
        return input.drillPipeAnnulusCapacity * input.wellDepthMD
    }

    fun strokesBitToSurface(input: WellKillInput): Double {
        if (input.pumpOutput <= 0) return 0.0
        return annulusVolume(input) / input.pumpOutput
    }

    fun totalStrokes(input: WellKillInput): Double {
        return strokesToBit(input) + strokesBitToSurface(input)
    }

    fun circulationTimeMinutes(strokes: Double, pumpRate: Double): Double {
        if (pumpRate <= 0) return 0.0
        return strokes / pumpRate
    }
}