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
            TextFieldValue("Дата", input.date) { input = input.copy(date = it) }
            TextFieldValue("ФИО", input.engineer) { input = input.copy(engineer = it) }

            // ---------- Данные о прочности пород ----------
            SectionTitle("Данные о прочности пород")
            NumberField("(A) Давление на устье при испытании, МПа", input.testPressureA) {
                input = input.copy(testPressureA = it)
            }
            NumberField("(B) Плотность раствора при испытании, кг/л", input.testDensityB) {
                input = input.copy(testDensityB = it)
            }
            NumberField("ГСВ до башмака, м", input.casingShoeTVD) {
                input = input.copy(casingShoeTVD = it)
            }

            // ---------- Текущие данные по скважине ----------
            SectionTitle("Текущие данные по скважине")
            NumberField("Глубина по стволу (MD), м", input.wellDepthMD) {
                input = input.copy(wellDepthMD = it)
            }
            NumberField("Глубина по вертикали (TVD), м", input.wellDepthTVD) {
                input = input.copy(wellDepthTVD = it)
            }
            NumberField("Плотность раствора в скважине, кг/л", input.currentMudDensity) {
                input = input.copy(currentMudDensity = it)
            }
            NumberField("Длина открытого ствола, м", input.openHoleLength) {
                input = input.copy(openHoleLength = it)
            }
            NumberField("Длина обсадной части, м", input.casedHoleLength) {
                input = input.copy(casedHoleLength = it)
            }

            ResultRow("(C) Макс. доп. плотность, кг/л",
                "%.2f".format(WellKillCalculator.maxAllowableDensity(input)))
            ResultRow("МДУДКП, МПа",
                "%.2f".format(WellKillCalculator.mdudkp(input)))

            // ---------- Бурильная колонна ----------
            SectionTitle("Бурильная колонна")
            NumberField("КНБК: длина, м", input.bhaLength) { input = input.copy(bhaLength = it) }
            NumberField("КНБК: уд. объём, л/м", input.bhaCapacity) { input = input.copy(bhaCapacity = it) }
            NumberField("ТБТ: длина, м", input.hwdpLength) { input = input.copy(hwdpLength = it) }
            NumberField("ТБТ: уд. объём, л/м", input.hwdpCapacity) { input = input.copy(hwdpCapacity = it) }
            NumberField("БТ: длина, м", input.dpLength) { input = input.copy(dpLength = it) }
            NumberField("БТ: уд. объём, л/м", input.dpCapacity) { input = input.copy(dpCapacity = it) }

            ResultRow("(D) Объём бурильной колонны, л",
                "%.1f".format(WellKillCalculator.drillPipeVolume(input)))

            // ---------- Кольцевое пространство ----------
            SectionTitle("Кольцевое пространство")
            NumberField("Открытый ствол: уд. объём, л/м", input.openHoleAnnulusCapacity) {
                input = input.copy(openHoleAnnulusCapacity = it)
            }
            NumberField("Обсадная часть: уд. объём, л/м", input.casedHoleAnnulusCapacity) {
                input = input.copy(casedHoleAnnulusCapacity = it)
            }

            ResultRow("(F) Открытый ствол, л",
                "%.1f".format(WellKillCalculator.openHoleAnnulusVolume(input)))
            ResultRow("(G) Обсадная часть, л",
                "%.1f".format(WellKillCalculator.casedHoleAnnulusVolume(input)))
            ResultRow("(H) Объём КП, л",
                "%.1f".format(WellKillCalculator.totalAnnulusVolume(input)))

            NumberField("(J) Объём наземной обвязки, л", input.surfaceVolumeJ) {
                input = input.copy(surfaceVolumeJ = it)
            }

            ResultRow("(I) Объём в скважине, л",
                "%.1f".format(WellKillCalculator.totalWellVolume(input)))
            ResultRow("(I+J) Общий объём, л",
                "%.1f".format(WellKillCalculator.totalVolumeWithSurface(input)))

            // ---------- Насосы ----------
            SectionTitle("Насосы")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Рабочий насос:", modifier = Modifier.weight(1f))
                RadioButton(
                    selected = input.activePump == 1,
                    onClick = { input = input.copy(activePump = 1) }
                )
                Text("1")
                Spacer(Modifier.width(12.dp))
                RadioButton(
                    selected = input.activePump == 2,
                    onClick = { input = input.copy(activePump = 2) }
                )
                Text("2")
            }

            NumberField("Насос 1: скорость, ход/мин", input.pump1Rate) {
                input = input.copy(pump1Rate = it)
            }
            NumberField("Насос 1: уд. объём, л/ход", input.pump1Output) {
                input = input.copy(pump1Output = it)
            }
            NumberField("Насос 2: скорость, ход/мин", input.pump2Rate) {
                input = input.copy(pump2Rate = it)
            }
            NumberField("Насос 2: уд. объём, л/ход", input.pump2Output) {
                input = input.copy(pump2Output = it)
            }
            NumberField("Давление прокачки, МПа", input.pumpPressure) {
                input = input.copy(pumpPressure = it)
            }

            ResultRow("(E) Ходов до долота",
                "%.0f".format(WellKillCalculator.strokesToBit(input)))
            ResultRow("Ходов на полную циркуляцию",
                "%.0f".format(WellKillCalculator.strokesFullCirculation(input)))

            // ---------- Приток ----------
            SectionTitle("Данные о притоке")
            NumberField("SIDPP (стаб. в БТ), МПа", input.sidpp) {
                input = input.copy(sidpp = it)
            }
            NumberField("SICP (стаб. в КП), МПа", input.sicp) {
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
                "%.2f".format(WellKillCalculator.initialCirculatingPressure(input)))
            ResultRow("Ркон (конечное давление циркуляции), МПа",
                "%.2f".format(WellKillCalculator.finalCirculatingPressure(input)))
            ResultRow("(К) = Рнач − Ркон, МПа",
                "%.2f".format(WellKillCalculator.pressureDifferenceK(input)))
            ResultRow("Снижение, МПа на 100 ходов",
                "%.3f".format(WellKillCalculator.pressureDropPer100Strokes(input)))

            // ---------- Таблица снижения давления ----------
            SectionTitle("График снижения давления")
            val schedule = WellKillCalculator.pressureSchedule(input)
            if (schedule.isEmpty()) {
                Text("Введите данные для расчёта (E) ходов.",
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
                                Text("%.2f".format(row.pressure),
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { exportPdf(context, input) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Экспорт PDF")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------- Вспомогательные Composable ----------

@Composable
fun SectionTitle(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold)
    HorizontalDivider()
}

@Composable
fun NumberField(label: String, value: Double, onChange: (Double) -> Unit) {
    var text by remember(value) {
        mutableStateOf(if (value == 0.0) "" else value.toString())
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it.replace(',', '.').toDoubleOrNull() ?: 0.0)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun TextFieldValue(label: String, value: String, onChange: (String) -> Unit) {
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
