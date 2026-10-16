package com.example.wellkillsheet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WellKillScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellKillScreen() {
    var input by remember { mutableStateOf(WellKillInput()) }
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Лист глушения") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ---------- Шапка ----------
            SectionTitle("Лист глушения")
            TextInput("Дата", input.date) { input = input.copy(date = it) }
            TextInput("ФИО", input.engineer) { input = input.copy(engineer = it) }

            // ---------- Данные о прочности пород ----------
            SectionTitle("Данные о прочности пород")

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = input.calcMethod == CalcMethod.TEST_PRESSURE,
                    onClick = { input = input.copy(calcMethod = CalcMethod.TEST_PRESSURE) }
                )
                Text("Давление на устье при испытании")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = input.calcMethod == CalcMethod.FRAC_PRESSURE_OR_GRAD,
                    onClick = { input = input.copy(calcMethod = CalcMethod.FRAC_PRESSURE_OR_GRAD) }
                )
                Text("Давление / градиент гидроразрыва")
            }

            Spacer(Modifier.height(4.dp))

            val isTestMethod = input.calcMethod == CalcMethod.TEST_PRESSURE

            NumberField("(A) Давление на устье при испытании, МПа",
                input.testPressureA, enabled = isTestMethod, roundToInt = false) {
                input = input.copy(testPressureA = it)
            }
            NumberField("(B) Плотность раствора при испытании, кг/л",
                input.testDensityB, enabled = isTestMethod, roundToInt = false) {
                input = input.copy(testDensityB = it)
            }
            NumberField("Давление / градиент гидроразрыва, МПа или МПа/м",
                input.fracValue, enabled = !isTestMethod, roundToInt = false) {
                input = input.copy(fracValue = it)
            }

            ResultRow("(C) Макс. допустимая плотность, кг/л",
                "%.2f".format(WellKillCalculator.maxAllowableDensity(input)))

            val mdudkp = WellKillCalculator.mdudkp(input)
            if (mdudkp < 0.0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("МДУДКП, МПа",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("%.2f".format(mdudkp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                Text("Несовместимый интервал бурения. Пересмотрите данные.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            } else {
                ResultRow("МДУДКП, МПа", "%.2f".format(mdudkp))
            }

            // ---------- Текущие данные по скважине ----------
            SectionTitle("Текущие данные по скважине")

            NumberField("Глубина башмака по стволу (MD), м", input.casingShoeMD) {
                input = input.copy(casingShoeMD = it)
            }
            NumberField("Глубина башмака по вертикали (TVD), м", input.casingShoeTVD) {
                input = input.copy(casingShoeTVD = it)
            }
            NumberField("Глубина скважины по стволу (MD), м", input.wellDepthMD) {
                input = input.copy(wellDepthMD = it)
            }
            NumberField("Глубина скважины по вертикали (TVD), м", input.wellDepthTVD) {
                input = input.copy(wellDepthTVD = it)
            }
            NumberField("Плотность раствора в скважине, кг/л",
                input.currentMudDensity, roundToInt = false) {
                input = input.copy(currentMudDensity = it)
            }

            ResultRow("Длина открытого ствола (авто), м",
                "%.0f".format(WellKillCalculator.openHoleLength(input)))
            ResultRow("Длина обсадной части (авто), м",
                "%.0f".format(WellKillCalculator.casedHoleLength(input)))

            // ---------- Бурильная колонна ----------
            SectionTitle("Бурильная колонна")
            NumberField("КНБК (УБТ): длина, м", input.bhaLength) {
                input = input.copy(bhaLength = it)
            }
            NumberField("КНБК: уд. объём (внутренний), л/м",
                input.bhaCapacity, roundToInt = false) {
                input = input.copy(bhaCapacity = it)
            }
            NumberField("ТБТ: длина, м", input.hwdpLength) {
                input = input.copy(hwdpLength = it)
            }
            NumberField("ТБТ: уд. объём (внутренний), л/м",
                input.hwdpCapacity, roundToInt = false) {
                input = input.copy(hwdpCapacity = it)
            }
            NumberField("БТ: уд. объём (внутренний), л/м",
                input.dpCapacity, roundToInt = false) {
                input = input.copy(dpCapacity = it)
            }

            ResultRow("БТ: длина (авто), м",
                "%.0f".format(WellKillCalculator.dpLength(input)))
            ResultRow("(D) Объём бурильной колонны, л",
                WellKillCalculator.drillPipeVolume(input).toString())

            // ---------- Кольцевое пространство ----------
            SectionTitle("Кольцевое пространство")

            Text("Удельные объёмы КП:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold)
            NumberField("Уд. объём ОС для КНБК, л/м",
                input.osBhaCapacity, roundToInt = false) {
                input = input.copy(osBhaCapacity = it)
            }
            NumberField("Уд. объём ОС для ТБТ/БИ, л/м",
                input.osHwdpBiCapacity, roundToInt = false) {
                input = input.copy(osHwdpBiCapacity = it)
            }
            NumberField("Уд. объём ОК для БИ, л/м",
                input.okBiCapacity, roundToInt = false) {
                input = input.copy(okBiCapacity = it)
            }

            Text("Длины в открытом стволе:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold)
            NumberField("Длина КНБК в ОС, м", input.bhaInOsLength) {
                input = input.copy(bhaInOsLength = it)
            }
            NumberField("Длина ТБТ в ОС, м", input.hwdpInOsLength) {
                input = input.copy(hwdpInOsLength = it)
            }

            ResultRow("(F) Объём КП в открытом стволе, л",
                WellKillCalculator.openHoleAnnulusVolume(input).toString())
            ResultRow("(G) Объём КП в обсадной колонне, л",
                WellKillCalculator.casedHoleAnnulusVolume(input).toString())
            ResultRow("(H) Объём КП общий, л",
                WellKillCalculator.totalAnnulusVolume(input).toString())

            NumberField("(J) Объём наземной обвязки, л", input.surfaceVolumeJ) {
                input = input.copy(surfaceVolumeJ = it)
            }

            ResultRow("(I) Объём в скважине, л",
                WellKillCalculator.totalWellVolume(input).toString())
            ResultRow("(I+J) Общий объём, л",
                WellKillCalculator.totalVolumeWithSurface(input).toString())

            // ---------- Насос ----------
            SectionTitle("Насос")
            NumberField("Скорость насоса при тесте на МХ, ход/мин", input.pumpRate) {
                input = input.copy(pumpRate = it)
            }
            NumberField("Удельный объём насоса, л/ход",
                input.pumpOutput, roundToInt = false) {
                input = input.copy(pumpOutput = it)
            }
            NumberField("Давление прокачки, МПа",
                input.pumpPressure, roundToInt = false) {
                input = input.copy(pumpPressure = it)
            }

            ResultRow("(E) Ходов до долота",
                WellKillCalculator.strokesToBit(input).toString())
            ResultRow("Ходов на полную циркуляцию",
                WellKillCalculator.strokesFullCirculation(input).toString())

            // ---------- Приток ----------
            SectionTitle("Данные о притоке")
            NumberField("Давление стабилизации в трубах, МПа",
                input.sidpp, roundToInt = false) {
                input = input.copy(sidpp = it)
            }
            NumberField("Давление стабилизации в кольцевом пространстве, МПа",
                input.sicp, roundToInt = false) {
                input = input.copy(sicp = it)
            }
            NumberField("Объём притока, л", input.influxVolume) {
                input = input.copy(influxVolume = it)
            }

            // ---------- Результаты ----------
            SectionTitle("Результаты")
            ResultRow("Плотность раствора глушения, кг/л",
                "%.2f".format(WellKillCalculator.killMudDensity(input)))
            ResultRow("Рнач (начальное давление циркуляции), МПа",
                "%.1f".format(WellKillCalculator.initialCirculatingPressure(input)))
            ResultRow("Ркон (конечное давление циркуляции), МПа",
                "%.1f".format(WellKillCalculator.finalCirculatingPressure(input)))
            ResultRow("(К) = Рнач − Ркон, МПа",
                "%.1f".format(WellKillCalculator.pressureDifferenceK(input)))
            ResultRow("Снижение, МПа на 100 ходов",
                "%.3f".format(WellKillCalculator.pressureDropPer100Strokes(input)))

            // ---------- Таблица ----------
            SectionTitle("График снижения давления")
            val schedule = WellKillCalculator.pressureSchedule(input)
            if (schedule.isEmpty()) {
                Text("Введите данные для расчёта.",
                    style = MaterialTheme.typography.bodyMedium)
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Ходов", modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold)
                            Text("Давление, МПа", modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()
                        for (row in schedule) {
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)) {
                                Text(row.strokes.toString(), modifier = Modifier.weight(1f))
                                Text("%.1f".format(row.pressure),
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val result = WellKillCalculator.validateInput(input)
                    when (result) {
                        is WellKillCalculator.ValidationResult.Ok ->
                            exportPdf(context, input)
                        is WellKillCalculator.ValidationResult.Error ->
                            errorMessage = result.message
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Экспорт PDF")
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Ошибка ввода") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("ОК")
                }
            }
        )
    }
}

// ============ ВСПОМОГАТЕЛЬНЫЕ COMPOSABLE ============

@Composable
fun SectionTitle(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold)
    HorizontalDivider()
}

@Composable
fun NumberField(
    label: String,
    value: Double,
    enabled: Boolean = true,
    roundToInt: Boolean = true,
    onChange: (Double) -> Unit
) {
    var text by remember(value) {
        mutableStateOf(if (value == 0.0) "" else value.toString())
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            var parsed = it.replace(',', '.').toDoubleOrNull() ?: 0.0
            if (roundToInt) parsed = Math.round(parsed).toDouble()
            onChange(parsed)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled
    )
}

@Composable
fun TextInput(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun ResultRow(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
        }
    }
}

fun exportPdf(context: Context, input: WellKillInput) {
    val file = File(context.cacheDir, "well_kill_sheet.pdf")
    FileOutputStream(file).use { out ->
        PdfGenerator.generate(context, input, out)
    }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Отправить PDF"))
}
