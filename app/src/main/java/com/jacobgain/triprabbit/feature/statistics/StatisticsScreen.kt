package com.jacobgain.triprabbit.feature.statistics

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.jacobgain.triprabbit.feature.dashboard.MileageHistoryChart
import com.jacobgain.triprabbit.feature.dashboard.WeeklyMileageChart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class StatisticsUiState(val loading: Boolean = true, val vehicle: Vehicle? = null,
    val readings: List<OdometerReading> = emptyList(), val stats: MileageStats = MileageStats())

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(saved: SavedStateHandle, vehicles: VehicleRepository,
    readings: OdometerRepository, calculate: CalculateMileageStatsUseCase, settings: SettingsRepository,
    private val data: DataRepository) : ViewModel() {
    private val explicitId = saved.get<String>("vehicleId")?.toLongOrNull()
    val uiState = (if (explicitId != null) flowOf(explicitId) else settings.observeSettings().map { it.selectedVehicleId })
        .flatMapLatest { id ->
            if (id == null) flowOf(StatisticsUiState(loading = false))
            else combine(vehicles.observeVehicle(id), readings.observeReadings(id)) { v, r -> StatisticsUiState(false, v, r, calculate(r)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())
    private val messages = Channel<String>(Channel.BUFFERED)
    val effects = messages.receiveAsFlow()
    var exporting by mutableStateOf(false)
        private set
    fun export(uri: Uri, vehicleId: Long) {
        if (exporting) return
        viewModelScope.launch {
            exporting = true
            try {
                withContext(Dispatchers.IO) { data.exportVehicleCsv(uri, vehicleId) }
                messages.send("CSV exported")
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
    val vehicle = state.vehicle
    val snackbar = remember { SnackbarHostState() }
    var exportVehicleId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Long?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val id = exportVehicleId
        if (uri != null && id != null) viewModel.export(uri, id)
        exportVehicleId = null
    }
    LaunchedEffect(viewModel) { viewModel.effects.collect { snackbar.showSnackbar(it) } }
    StatisticsContent(state, onBack, viewModel.exporting, snackbar,
        onExport = { vehicle?.let { exportVehicleId = it.id; export.launch("${it.name}-odometer.csv") } },
        onManageVehicles = onManageVehicles)
}

@Composable
fun StatisticsContent(state: StatisticsUiState, onBack: (() -> Unit)? = null, exporting: Boolean = false,
    snackbar: SnackbarHostState = remember { SnackbarHostState() }, onExport: () -> Unit = {},
    onManageVehicles: () -> Unit = {}) {
    val vehicle = state.vehicle
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = { if (onBack != null) DetailTopBar("Reports", onBack) }) { padding ->
        if (state.loading) LoadingState(Modifier.padding(padding))
        else if (vehicle == null) Box(Modifier.padding(padding)) { EmptyState("No vehicle selected", "Choose a vehicle to see its mileage reports.", "Vehicles", onManageVehicles) }
        else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp,24.dp,20.dp,28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item { PageHeading("The bigger picture.", "Mileage insights for ${vehicle.name}") }
            item {
                Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.primary,
                    contentColor=MaterialTheme.colorScheme.onPrimary) {
                    Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Text("TOTAL DISTANCE TRACKED",style=MaterialTheme.typography.labelSmall,
                                color=MaterialTheme.colorScheme.onPrimary.copy(alpha=.78f),modifier=Modifier.weight(1f))
                            TripIcon(AppIcon.Reports,tint=MaterialTheme.colorScheme.onPrimary.copy(alpha=.78f))
                        }
                        OdometerDisplay(state.stats.totalTracked,vehicle.odometerUnit)
                        Text("${state.stats.readingCount} readings · all time",style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onPrimary.copy(alpha=.78f))
                    }
                }
            }
            item { AdaptivePair(
                first={MetricTile("Last 30 days",state.stats.last30Days.grouped(),it,vehicle.odometerUnit.abbreviation,AppIcon.History)},
                second={MetricTile("Monthly average",state.stats.averagePerMonth.grouped(),it,vehicle.odometerUnit.abbreviation,AppIcon.Reports)},
            ) }
            item { SectionCard {
                SectionTitle("Odometer trend", "${state.stats.currentYear.grouped()} ${vehicle.odometerUnit.abbreviation} tracked this year")
                if (state.readings.size < 2) Text("Add at least two readings to see your mileage trend.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else {
                    MileageHistoryChart(state.readings, Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(state.readings.last().recordedAt.displayDate(), style = MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.readings.first().recordedAt.displayDate(), style = MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } }
            item { SectionCard {
                SectionTitle("Your week", "Distance grouped by the day you logged it.")
                WeeklyMileageChart(state.readings, vehicle.odometerUnit)
            } }
            item {
                Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TripIcon(AppIcon.Download,modifier=Modifier.size(28.dp))
                        Text("Ready when you need it.", style = MaterialTheme.typography.titleLarge)
                        Text("Export this vehicle’s odometer readings, dates, and notes in one CSV file.", style = MaterialTheme.typography.bodyMedium)
                        PrimaryAction("Export CSV",onExport,icon=AppIcon.Download,busy=exporting)
                    }
                }
            }
        }
    }
}
