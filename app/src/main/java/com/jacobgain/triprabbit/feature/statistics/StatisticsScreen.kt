package com.jacobgain.triprabbit.feature.statistics

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobgain.triprabbit.core.designsystem.component.*
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.repository.*
import com.jacobgain.triprabbit.core.usecase.CalculateMileageStatsUseCase
import com.jacobgain.triprabbit.core.util.grouped
import com.jacobgain.triprabbit.core.util.displayDate
import com.jacobgain.triprabbit.feature.dashboard.MonthlyDistanceChart
import com.jacobgain.triprabbit.feature.dashboard.WeeklyMileageChart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToLong

private fun weeklyTracked(readings: List<OdometerReading>): Long {
    val today = LocalDate.now()
    val sunday = today.minusDays((today.dayOfWeek.value % 7).toLong())
    val ordered = readings.filterNot { it.inProgress }.sortedWith(compareBy<OdometerReading> { it.recordedAt }.thenBy { it.id })
    return ordered.mapIndexedNotNull { index, reading ->
        val distance = reading.startValue?.let { reading.value - it } ?: if (index == 0) null else reading.value - ordered[index - 1].value
        val day = reading.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate()
        distance?.takeIf { day >= sunday && day <= sunday.plusDays(6) }
    }.sum()
}

data class StatisticsUiState(val loading: Boolean = true, val vehicle: Vehicle? = null,
    val readings: List<OdometerReading> = emptyList(), val stats: MileageStats = MileageStats(), val vehicles: List<Vehicle> = emptyList(),
    val showGraphs: Boolean = true, val displayUnit: DistanceUnit = DistanceUnit.KILOMETERS)

data class TripDistance(val recordedAt: Instant, val distance: Long, val sourceUnit: DistanceUnit)

private fun convertDistance(distance: Long, from: DistanceUnit, to: DistanceUnit): Long = when {
    from == to -> distance
    from == DistanceUnit.MILES -> (distance * 1.609344).roundToLong()
    else -> (distance / 1.609344).roundToLong()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(vehicles: VehicleRepository,
    readings: OdometerRepository, calculate: CalculateMileageStatsUseCase, private val settings: SettingsRepository,
    private val data: DataRepository) : ViewModel() {
    private val displayUnit = MutableStateFlow(DistanceUnit.KILOMETERS)
    val uiState = combine(vehicles.observeActiveVehicles(), settings.observeSettings(), displayUnit) { active, preferences, unit ->
        Triple(active, preferences, unit)
    }.flatMapLatest { (active, preferences, unit) ->
        if (active.isEmpty()) flowOf(StatisticsUiState(loading = false, showGraphs = preferences.showReportGraphs, displayUnit = unit))
        else combine(active.map { readings.observeReadings(it.id) }) { histories ->
            val distances = active.zip(histories).flatMap { (vehicle, history) ->
                val ordered = history.filterNot { it.inProgress }.sortedWith(compareBy<OdometerReading> { it.recordedAt }.thenBy { it.id })
                ordered.mapIndexedNotNull { index, reading ->
                    val distance = reading.startValue?.let { reading.value - it }
                        ?: ordered.getOrNull(index - 1)?.let { reading.value - it.value }
                    distance?.takeIf { it >= 0 }?.let { TripDistance(reading.recordedAt, it, vehicle.odometerUnit) }
                }
            }.sortedBy { it.recordedAt }
            val chartReadings = distances.mapIndexed { index, trip ->
                val converted = convertDistance(trip.distance, trip.sourceUnit, unit)
                OdometerReading(index + 1L, 0L, converted, trip.recordedAt, null, trip.recordedAt, null, startValue = 0L)
            }
            val totalReadingCount = histories.sumOf { history -> history.count { !it.inProgress } }
            val stats = calculate(chartReadings).copy(readingCount = totalReadingCount)
            StatisticsUiState(false, active.firstOrNull(), chartReadings, stats, active, preferences.showReportGraphs, unit)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())
    private val messages = Channel<String>(Channel.BUFFERED)
    val effects = messages.receiveAsFlow()
    fun setDisplayUnit(unit: DistanceUnit) { displayUnit.value = unit }
    var exporting by mutableStateOf(false)
        private set
    fun export(uri: Uri, vehicleIds: List<Long>, from: LocalDate, through: LocalDate, unit: DistanceUnit, allVehicles: Boolean, pdf: Boolean) {
        if (exporting) return
        viewModelScope.launch {
            exporting = true
            try {
                withContext(Dispatchers.IO) { data.exportVehiclesReport(uri, vehicleIds, from, through, unit, allVehicles, pdf) }
                messages.send(if (pdf) "PDF exported" else "CSV exported")
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                messages.send(error.message ?: "Could not export readings.")
            } finally { exporting = false }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(onBack: (() -> Unit)?, onManageVehicles: () -> Unit = {}, viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var exportVehicleId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Long?>(null) }
    var fromText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(YearMonth.now().atDay(1).toString()) }
    var throughText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val from = runCatching { LocalDate.parse(fromText) }.getOrNull()
    val through = runCatching { LocalDate.parse(throughText) }.getOrNull()
    val validRange = from != null && through != null && !through.isBefore(from)
    val selectedExportVehicles = exportVehicleId?.let { id -> state.vehicles.filter { it.id == id } } ?: state.vehicles
    val exportName = selectedExportVehicles.singleOrNull()?.name ?: "All-vehicles"
    val allVehicles = exportVehicleId == null
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null && selectedExportVehicles.isNotEmpty() && validRange) viewModel.export(uri, selectedExportVehicles.map { it.id }, from!!, through!!, state.displayUnit, allVehicles, false)
    }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null && selectedExportVehicles.isNotEmpty() && validRange) viewModel.export(uri, selectedExportVehicles.map { it.id }, from!!, through!!, state.displayUnit, allVehicles, true)
    }
    LaunchedEffect(viewModel) { viewModel.effects.collect { snackbar.showSnackbar(it) } }
    StatisticsContent(state, onBack, viewModel.exporting, snackbar,
        fromText = fromText, onFrom = { fromText = it }, throughText = throughText, onThrough = { throughText = it }, validRange = validRange,
        onExport = { if (selectedExportVehicles.isNotEmpty()) csv.launch("$exportName-mileage.csv") },
        onExportPdf = { if (selectedExportVehicles.isNotEmpty()) pdf.launch("$exportName-mileage.pdf") },
        exportVehicleId = exportVehicleId, onSelectExportVehicle = { exportVehicleId = it },
        onSelectUnit = viewModel::setDisplayUnit,
        onManageVehicles = onManageVehicles)
}

@Composable
fun StatisticsContent(state: StatisticsUiState, onBack: (() -> Unit)? = null, exporting: Boolean = false,
    snackbar: SnackbarHostState = remember { SnackbarHostState() }, onExport: () -> Unit = {},
    onManageVehicles: () -> Unit = {}, fromText: String = YearMonth.now().atDay(1).toString(), onFrom: (String) -> Unit = {},
    throughText: String = LocalDate.now().toString(), onThrough: (String) -> Unit = {}, validRange: Boolean = true, onExportPdf: () -> Unit = {},
    exportVehicleId: Long? = null, onSelectExportVehicle: (Long?) -> Unit = {}, onSelectUnit: (DistanceUnit) -> Unit = {}) {
    var exportMenuExpanded by remember { mutableStateOf(false) }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = { if (onBack != null) DetailTopBar("Reports", onBack) }) { padding ->
        if (state.loading) LoadingState(Modifier.padding(padding))
        else if (state.vehicle == null && state.vehicles.isEmpty()) Box(Modifier.padding(padding)) { EmptyState("No vehicles yet", "Add a vehicle to start tracking mileage reports.", "Vehicles", onManageVehicles) }
        else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp,24.dp,20.dp,28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item { PageHeading("Reports", "") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Total distance tracked", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        DistanceUnitSelector(state.displayUnit, onSelectUnit)
                    }
                    Text("${state.stats.totalTracked.grouped()} ${state.displayUnit.abbreviation}", style = MaterialTheme.typography.headlineLarge)
                    Text("${state.stats.readingCount} total readings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { AdaptivePair(
                first={ReportMetric("Last 30 days", "${state.stats.last30Days.grouped()} ${state.displayUnit.abbreviation}", it)},
                second={ReportMetric("Monthly average", "${state.stats.averagePerMonth.grouped()} ${state.displayUnit.abbreviation}", it)},
            ) }
            if (state.showGraphs) {
                item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SectionTitle("Distance by month", "${state.stats.currentYear.grouped()} ${state.displayUnit.abbreviation} tracked this year")
                    if (state.readings.size < 2) Text("Add another odometer reading to see distance by month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else MonthlyDistanceChart(state.readings, state.displayUnit)
                } }
                item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SectionTitle("Your week", "${weeklyTracked(state.readings).grouped()} ${state.displayUnit.abbreviation} tracked this week")
                    WeeklyMileageChart(state.readings, state.displayUnit)
                } }
            }
            item {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SectionTitle("Export trip data", "Choose vehicles and a date range")
                    Box {
                        OutlinedButton(onClick = { exportMenuExpanded = true }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                            Text(exportVehicleId?.let { id -> state.vehicles.firstOrNull { it.id == id }?.name } ?: "All Vehicles",
                                modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Spacer(Modifier.width(4.dp)); TripIcon(AppIcon.Down, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = exportMenuExpanded, onDismissRequest = { exportMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("All Vehicles") }, onClick = { exportMenuExpanded = false; onSelectExportVehicle(null) })
                            state.vehicles.forEach { car -> DropdownMenuItem(text = { Text(car.name, maxLines = 1) },
                                onClick = { exportMenuExpanded = false; onSelectExportVehicle(car.id) }) }
                        }
                    }
                    ReportDateField("From", fromText, onFrom)
                    ReportDateField("Through", throughText, onThrough)
                    if (!validRange) Text("Enter a valid date range.", color = MaterialTheme.colorScheme.error)
                    PrimaryAction("Export PDF", onExportPdf, icon = AppIcon.Download, enabled = validRange, busy = exporting)
                    OutlinedButton(onClick = onExport, enabled = validRange && !exporting, modifier = Modifier.fillMaxWidth()) { Text("Export CSV") }
                }
            }
        }
    }
}

@Composable
private fun DistanceUnitSelector(selectedUnit: DistanceUnit, onSelectUnit: (DistanceUnit) -> Unit) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier.width(128.dp).height(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, outline, CircleShape).selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(DistanceUnit.KILOMETERS, DistanceUnit.MILES).forEachIndexed { index, unit ->
            val selected = selectedUnit == unit
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(selected = selected, onClick = { onSelectUnit(unit) }, role = Role.RadioButton),
                contentAlignment = Alignment.Center,
            ) {
                Text(unit.abbreviation, style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (index == 0) Box(Modifier.width(1.dp).height(20.dp).background(outline))
        }
    }
}

@Composable
private fun ReportMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDateField(label: String, value: String, onValue: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    if (showPicker) {
        val initial = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { onValue(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                showPicker = false
            }) { Text("Apply") }
        }, dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }) { DatePicker(picker) }
    }
    OutlinedTextField(value, onValue, label = { Text("$label (yyyy-MM-dd)") }, singleLine = true, modifier = Modifier.fillMaxWidth().clearFocusWhenKeyboardCloses(),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface),
        trailingIcon = { IconButton(onClick = { showPicker = true }) { TripIcon(AppIcon.History, "Choose $label date") } })
}
