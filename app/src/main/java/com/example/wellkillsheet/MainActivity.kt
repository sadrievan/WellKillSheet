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
        topBar = { TopAppBar(title = { Text("Well Kill Sheet") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Input data", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))

            NumberField("Well Depth MD, m", input.wellDepthMD) {
                input = input.copy(wellDepthMD = it)
            }
            NumberField("Well Depth TVD, m", input.wellDepthTVD) {
                input = input.copy(wellDepthTVD = it)
            }
            NumberField("Casing Shoe TVD, m", input.casingShoeTVD) {
                input = input.copy(casingShoeTVD = it)
            }
            NumberField("Current Mud Density, kg/l", input.currentMudDensity) {
                input = input.copy(currentMudDensity = it)
            }
            NumberField("SIDPP, bar", input.sidpp) {
                input = input.copy(sidpp = it)
            }
            NumberField("SICP, bar", input.sicp) {
                input = input.copy(sicp = it)
            }
            NumberField("Pump Output, l/stroke", input.pumpOutput) {
                input = input.copy(pumpOutput = it)
            }
            NumberField("Slow Pump Rate, spm", input.slowPumpRate) {
                input = input.copy(slowPumpRate = it)
            }
            NumberField("Slow Circulating Pressure, bar", input.slowCirculatingPressure) {
                input = input.copy(slowCirculatingPressure = it)
            }
            NumberField("Drill Pipe Capacity, l/m", input.drillPipeCapacity) {
                input = input.copy(drillPipeCapacity = it)
            }
            NumberField("Annulus Capacity, l/m", input.drillPipeAnnulusCapacity) {
                input = input.copy(drillPipeAnnulusCapacity = it)
            }
            NumberField("Max Allowable Density, kg/l", input.maxAllowableDensity) {
                input = input.copy(maxAllowableDensity = it)
            }

            Spacer(Modifier.height(16.dp))
            Text("Calculated results", style = MaterialTheme.typography.titleLarge)

            ResultRow("Kill Mud Density, kg/l",
                "%.2f".format(WellKillCalculator.killMudDensity(input)))
            ResultRow("Initial Circulating Pressure, bar",
                "%.1f".format(WellKillCalculator.initialCirculatingPressure(input)))
            ResultRow("Final Circulating Pressure, bar",
                "%.1f".format(WellKillCalculator.finalCirculatingPressure(input)))
            ResultRow("MAASP, bar",
                "%.1f".format(WellKillCalculator.maasp(input)))
            ResultRow("Strokes to Bit",
                "%.0f".format(WellKillCalculator.strokesToBit(input)))
            ResultRow("Strokes Bit to Surface",
                "%.0f".format(WellKillCalculator.strokesBitToSurface(input)))
            ResultRow("Total Strokes",
                "%.0f".format(WellKillCalculator.totalStrokes(input)))

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { exportPdf(context, input) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export PDF")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
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
fun ResultRow(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
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
    context.startActivity(Intent.createChooser(intent, "Share PDF"))
}