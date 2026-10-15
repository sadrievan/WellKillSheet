package ru.wellkillsheet

import android.content.ContentValues
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max

data class VolumeRow(val name: String, val lengthM: Double, val specificLPerM: Double) {
    val volumeL: Double get() = lengthM * specificLPerM
}

data class SheetData(
    val date: String, val fio: String, val well: String,
    val testPressureMpa: Double, val testDensityKgM3: Double, val shoeTvdM: Double,
    val currentDensityKgM3: Double, val shoeDiameterMm: Double, val shoeMeasuredDepthM: Double,
    val wellDiameterMm: Double, val wellMeasuredDepthM: Double, val wellTvdM: Double,
    val pump1OutputL: Double, val pump2OutputL: Double, val pump1PressureMpa: Double, val pump2PressureMpa: Double,
    val pumpStrokesPerMin: Double, val influxDrillpipeMpa: Double, val influxAnnulusMpa: Double, val influxVolumeL: Double,
    val surfaceVolumeL: Double, val killDensityKgM3: Double, val rows: List<VolumeRow>
) {
    // D: internal drill-string volume; F: drill string in open hole; G: drill pipe in casing.
    val maxDensityKgM3 get() = if (shoeTvdM > 0) testDensityKgM3 + testPressureMpa * 1_000_000.0 / (shoeTvdM * 9.81) else Double.NaN
    val maxWellheadPressureMpa get() = (maxDensityKgM3 - currentDensityKgM3) * shoeTvdM * 9.81 / 1_000_000.0
    val D get() = rows.filter { it.name in listOf("БТ", "ТБТ", "УБТ") }.sumOf { it.volumeL }
    val F get() = rows.filter { it.name in listOf("УБТ в открытом стволе", "БТ и ТБТ в открытом стволе") }.sumOf { it.volumeL }
    val G get() = rows.filter { it.name == "БТ в обсадной колонне" }.sumOf { it.volumeL }
    val H get() = F + G
    val I get() = D + H
    val J get() = surfaceVolumeL
    val totalCirculationL get() = I + J
    val pumpOutputL get() = if (pump1OutputL > 0) pump1OutputL else pump2OutputL
    val E get() = if (pumpOutputL > 0) D / pumpOutputL else Double.NaN
    val lowSpeedMinutes get() = if (pumpStrokesPerMin > 0 && E.isFinite()) E / pumpStrokesPerMin else Double.NaN
    val killDensityCalculatedKgM3 get() = currentDensityKgM3 + influxDrillpipeMpa * 1_000_000.0 / (shoeTvdM * 9.81).coerceAtLeast(1.0)
    val initialCirculationPressure get() = pump1PressureMpa + influxDrillpipeMpa
    val finalCirculationPressure get() = if (currentDensityKgM3 > 0) (killDensityKgM3 / currentDensityKgM3) * pump1PressureMpa else Double.NaN
    val K get() = initialCirculationPressure - finalCirculationPressure
    val pressureReductionPer100Strokes get() = if (E > 0 && E.isFinite()) K * 100.0 / E else Double.NaN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { SheetScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetScreen() {
    val context = LocalContext.current
    val today = remember { SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date()) }
    val values = remember { mutableStateMapOf<String, String>() }
    fun v(key: String, default: String = "") = values[key] ?: default
    fun num(key: String) = v(key).replace(',', '.').toDoubleOrNull() ?: 0.0
    fun put(key: String, value: String) { values[key] = value }
    val rowNames = listOf("БТ", "ТБТ", "УБТ", "УБТ в открытом стволе", "БТ и ТБТ в открытом стволе", "БТ в обсадной колонне")
    val rows = rowNames.map { n -> VolumeRow(n, num("$n.length"), num("$n.specific")) }
    val data = SheetData(
        v("date", today), v("fio"), v("well"), num("testP"), num("testDensity"), num("shoeTvd"),
        num("mudDensity"), num("shoeDia"), num("shoeMd"), num("wellDia"), num("wellMd"), num("wellTvd"),
        num("pump1Out"), num("pump2Out"), num("pump1P"), num("pump2P"), num("spm"),
        num("influxDP"), num("influxAnn"), num("influxVol"), num("surfaceVol"), num("killDensity"), rows
    )
    var showValidation by remember { mutableStateOf(false) }
    val createPdf = {
        val missing = listOf("testP", "testDensity", "shoeTvd", "mudDensity", "pump1Out", "pump1P", "influxDP")
            .filter { num(it) <= 0.0 }
        if (missing.isNotEmpty()) {
            showValidation = true
        } else {
            try {
                savePdf(context, data)
                Toast.makeText(context, "PDF сохранён в папку Документы", Toast.LENGTH_LONG).show()
                showValidation = false
            } catch (e: Exception) {
                Toast.makeText(context, "Не удалось создать PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Лист глушения скважины") }) },
        bottomBar = {
            Surface(shadowElevation = 6.dp) {
                Button(onClick = createPdf, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Рассчитать и сохранить PDF")
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Заполните исходные данные. Плотность вводится в кг/м³ (СИ), давление — в МПа, длина — в метрах.", fontSize = 14.sp)
            if (showValidation) Text("Заполните положительными значениями: давление испытания, плотность при испытании, вертикальную глубину башмака, плотность текущего раствора, производительность насоса 1, давление прокачки насоса 1 и давление стабилизации в БТ.", color = MaterialTheme.colorScheme.error)
            Section("Общие сведения") {
                Field("Дата", "date", values, today)
                Field("ФИО", "fio", values)
                Field("Скважина / объект", "well", values)
            }
            Section("Прочность пород и буровой раствор") {
                Field("Давление испытания пород, МПа (A)", "testP", values, numeric = true)
                Field("Плотность раствора при испытании, кг/м³ (B)", "testDensity", values, numeric = true)
                Field("Вертикальная глубина до башмака, м", "shoeTvd", values, numeric = true)
                Field("Текущая плотность раствора, кг/м³", "mudDensity", values, numeric = true)
                Result("Максимально допустимая плотность", fmt(data.maxDensityKgM3) + " кг/м³")
                Result("Макс. допустимое давление на устье в КП", fmt(data.maxWellheadPressureMpa) + " МПа")
            }
            Section("Башмак колонны и параметры скважины") {
                Field("Диаметр башмака, мм", "shoeDia", values, numeric = true)
                Field("Глубина башмака по стволу, м", "shoeMd", values, numeric = true)
                Field("Диаметр скважины, мм", "wellDia", values, numeric = true)
                Field("Глубина скважины по стволу, м", "wellMd", values, numeric = true)
                Field("Вертикальная глубина скважины, м", "wellTvd", values, numeric = true)
            }
            Section("Насосы") {
                Field("Производительность насоса 1, л/ход", "pump1Out", values, numeric = true)
                Field("Производительность насоса 2, л/ход", "pump2Out", values, numeric = true)
                Field("Давление прокачки насоса 1, МПа", "pump1P", values, numeric = true)
                Field("Давление прокачки насоса 2, МПа", "pump2P", values, numeric = true)
                Field("Пониженная скорость насоса, ход/мин", "spm", values, numeric = true)
            }
            Section("Данные об объёмах") {
                Text("Для каждой строки укажите длину и удельный объём из принятой технической документации.")
                rows.forEach { r ->
                    Text(r.name, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Field("Длина, м", "${r.name}.length", values, modifier = Modifier.weight(1f), numeric = true)
                        Field("Удельный объём, л/м", "${r.name}.specific", values, modifier = Modifier.weight(1f), numeric = true)
                    }
                    Result("Объём", fmt(r.volumeL) + " л")
                }
                Field("Объём поверхностной обвязки, л (J)", "surfaceVol", values, numeric = true)
                Result("D — объём бурильной колонны", fmt(data.D) + " л")
                Result("E — число ходов для объёма D", fmt(data.E) + " ходов")
                Result("F — объём колонны в открытом стволе", fmt(data.F) + " л")
                Result("G — БТ в обсадной колонне", fmt(data.G) + " л")
                Result("H = F + G — объём КП", fmt(data.H) + " л")
                Result("I = D + H — объём раствора в скважине", fmt(data.I) + " л")
                Result("I + J — общий объём циркуляции", fmt(data.totalCirculationL) + " л")
            }
            Section("Приток и давления циркуляции") {
                Field("Давление стабилизации в БТ, МПа", "influxDP", values, numeric = true)
                Field("Давление стабилизации в КП, МПа", "influxAnn", values, numeric = true)
                Field("Объём притока, л", "influxVol", values, numeric = true)
                Field("Плотность раствора глушения, кг/м³ (если задана)", "killDensity", values, numeric = true)
                Result("Расчётная плотность глушения", fmt(data.killDensityCalculatedKgM3) + " кг/м³")
                Result("Начальное давление циркуляции Pнач", fmt(data.initialCirculationPressure) + " МПа")
                Result("Конечное давление циркуляции Pкон", fmt(data.finalCirculationPressure) + " МПа")
                Result("K = Pнач − Pкон", fmt(data.K) + " МПа")
                Result("Снижение давления на 100 ходов", fmt(data.pressureReductionPer100Strokes) + " МПа/100 ходов")
                Text("Важно: перед применением на скважине результаты должны быть проверены ответственным специалистом по утверждённой методике предприятия.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            Spacer(Modifier.height(70.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun Field(label: String, key: String, values: MutableMap<String, String>, default: String = "", modifier: Modifier = Modifier.fillMaxWidth(), numeric: Boolean = false) {
    val value = values[key] ?: default
    OutlinedTextField(
        value = value, onValueChange = { values[key] = it }, label = { Text(label) },
        modifier = modifier, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text)
    )
}

@Composable
fun Result(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(value, fontSize = 13.sp, style = MaterialTheme.typography.labelLarge)
    }
}

fun fmt(x: Double): String = if (x.isFinite()) String.format(Locale.US, "%.2f", x) else "—"

fun savePdf(context: android.content.Context, d: SheetData) {
    val doc = PdfDocument()
    val pageW = 595
    val pageH = 842
    val margin = 38f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 9f }
    val bold = Paint(paint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 10f }
    val title = Paint(bold).apply { textSize = 17f }
    fun line(c: android.graphics.Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        c.drawLine(x1, y1, x2, y2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.DKGRAY; strokeWidth = .6f })
    }
    fun text(c: android.graphics.Canvas, s: String, x: Float, y: Float, p: Paint = paint) { c.drawText(s, x, y, p) }
    fun page(number: Int, heading: String, draw: (android.graphics.Canvas) -> Unit) {
        val info = PdfDocument.PageInfo.Builder(pageW, pageH, number).create()
        val p = doc.startPage(info)
        draw(p.canvas)
        doc.finishPage(p)
    }
    page(1, "Лист глушения") { c ->
        var y = 42f
        text(c, "ЛИСТ ГЛУШЕНИЯ СКВАЖИНЫ", margin, y, title); y += 20
        text(c, "Дата: ${d.date}    ФИО: ${d.fio}    Скважина: ${d.well}", margin, y); y += 18
        line(c, margin, y, pageW-margin, y); y += 17
        text(c, "ДАННЫЕ О ПРОЧНОСТИ ПОРОД", margin, y, bold); y += 15
        text(c, "Давление испытания пород на приёмистость (A): ${fmt(d.testPressureMpa)} МПа", margin, y); y += 14
        text(c, "Плотность раствора при испытании (B): ${fmt(d.testDensityKgM3)} кг/м³", margin, y); y += 14
        text(c, "Максимально допустимая плотность (C): ${fmt(d.maxDensityKgM3)} кг/м³", margin, y); y += 14
        text(c, "Макс. допустимое давление на устье в КП: ${fmt(d.maxWellheadPressureMpa)} МПа", margin, y); y += 20
        text(c, "ТЕКУЩИЕ ДАННЫЕ ПО СКВАЖИНЕ", margin, y, bold); y += 15
        text(c, "Текущая плотность раствора: ${fmt(d.currentDensityKgM3)} кг/м³", margin, y); y += 14
        text(c, "Башмак: диаметр ${fmt(d.shoeDiameterMm)} мм; глубина по стволу ${fmt(d.shoeMeasuredDepthM)} м; вертикальная ${fmt(d.shoeTvdM)} м", margin, y); y += 14
        text(c, "Скважина: диаметр ${fmt(d.wellDiameterMm)} мм; глубина по стволу ${fmt(d.wellMeasuredDepthM)} м; вертикальная ${fmt(d.wellTvdM)} м", margin, y); y += 20
        text(c, "ДАННЫЕ ПО НАСОСАМ", margin, y, bold); y += 15
        text(c, "Насос 1: ${fmt(d.pump1OutputL)} л/ход; давление прокачки ${fmt(d.pump1PressureMpa)} МПа", margin, y); y += 14
        text(c, "Насос 2: ${fmt(d.pump2OutputL)} л/ход; давление прокачки ${fmt(d.pump2PressureMpa)} МПа", margin, y); y += 14
        text(c, "Пониженная скорость насоса: ${fmt(d.pumpStrokesPerMin)} ход/мин", margin, y); y += 20
        text(c, "ДАННЫЕ ПО ОБЪЁМАМ", margin, y, bold); y += 15
        text(c, "Наименование", margin, y, bold); text(c, "Длина, м", 245f, y, bold); text(c, "Уд. объём, л/м", 325f, y, bold); text(c, "Объём, л", 440f, y, bold); y += 6
        line(c, margin, y, pageW-margin, y); y += 13
        d.rows.forEach { r ->
            text(c, r.name.take(34), margin, y); text(c, fmt(r.lengthM), 245f, y); text(c, fmt(r.specificLPerM), 325f, y); text(c, fmt(r.volumeL), 440f, y); y += 14
        }
        y += 5; line(c, margin, y, pageW-margin, y); y += 16
        text(c, "D — объём бурильной колонны: ${fmt(d.D)} л; E — число ходов: ${fmt(d.E)}", margin, y); y += 15
        text(c, "F — колонна в открытом стволе: ${fmt(d.F)} л; G — БТ в обсадной колонне: ${fmt(d.G)} л", margin, y); y += 15
        text(c, "H = F + G — объём КП: ${fmt(d.H)} л; I = D + H — объём в скважине: ${fmt(d.I)} л", margin, y); y += 15
        text(c, "J — поверхностная обвязка: ${fmt(d.J)} л; I + J — объём циркуляции: ${fmt(d.totalCirculationL)} л", margin, y); y += 25
        text(c, "Предварительный расчёт. Перед применением проверить по утверждённой методике предприятия.", margin, y)
    }
    page(2, "Давления и приток") { c ->
        var y = 42f
        text(c, "ЛИСТ ГЛУШЕНИЯ — ДАВЛЕНИЯ И ПРИТОК", margin, y, title); y += 20
        text(c, "Дата: ${d.date}    ФИО: ${d.fio}    Скважина: ${d.well}", margin, y); y += 22
        text(c, "ДАННЫЕ О ПРИТОКЕ", margin, y, bold); y += 16
        text(c, "Давление стабилизации в БТ: ${fmt(d.influxDrillpipeMpa)} МПа", margin, y); y += 14
        text(c, "Давление стабилизации в КП: ${fmt(d.influxAnnulusMpa)} МПа", margin, y); y += 14
        text(c, "Объём притока: ${fmt(d.influxVolumeL)} л", margin, y); y += 20
        text(c, "Плотность раствора глушения (расчётная): ${fmt(d.killDensityCalculatedKgM3)} кг/м³", margin, y); y += 20
        text(c, "НАЧАЛЬНОЕ И КОНЕЧНОЕ ДАВЛЕНИЕ ЦИРКУЛЯЦИИ", margin, y, bold); y += 16
        text(c, "Pнач = давление прокачки насоса 1 + давление стабилизации в БТ", margin, y); y += 14
        text(c, "Pнач = ${fmt(d.pump1PressureMpa)} + ${fmt(d.influxDrillpipeMpa)} = ${fmt(d.initialCirculationPressure)} МПа", margin, y); y += 19
        text(c, "Pкон = (плотность раствора глушения / текущая плотность) × давление прокачки", margin, y); y += 14
        text(c, "Pкон = (${fmt(d.killDensityCalculatedKgM3)} / ${fmt(d.currentDensityKgM3)}) × ${fmt(d.pump1PressureMpa)} = ${fmt(d.finalCirculationPressure)} МПа", margin, y); y += 19
        text(c, "K = Pнач − Pкон = ${fmt(d.K)} МПа", margin, y); y += 14
        text(c, "Снижение давления на 100 ходов: K × 100 / E = ${fmt(d.pressureReductionPer100Strokes)} МПа/100 ходов", margin, y); y += 25
        text(c, "ТАБЛИЦА ХОДОВ И ДАВЛЕНИЯ (заполняется по утверждённой процедуре)", margin, y, bold); y += 15
        val left = margin; val right = pageW-margin
        val col = listOf(left, left+100f, left+240f, right)
        line(c, left, y, right, y); y += 2
        for (i in 0..15) {
            line(c, left, y+17f, right, y+17f)
            if (i == 0) { text(c, "Ходов", col[0]+4, y+12); text(c, "Статическое давление, МПа", col[1]+4, y+12); text(c, "Динамическое давление, МПа", col[2]+4, y+12) }
            y += 18
        }
        y += 15
        text(c, "Проверка и утверждение ответственным специалистом: ______________________________", margin, y); y += 22
        text(c, "Подпись: ___________________________    Дата: __________________", margin, y); y += 25
        text(c, "ВНИМАНИЕ: документ является расчётным черновиком до проверки и утверждения.", margin, y, bold)
    }
    val filename = "Лист_глушения_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.pdf"
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents")
    }
    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
        ?: throw IllegalStateException("Не удалось создать файл в Документах")
    resolver.openOutputStream(uri)?.use { doc.writeTo(it) } ?: throw IllegalStateException("Не удалось открыть PDF для записи")
    doc.close()
}
