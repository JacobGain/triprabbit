package com.jacobgain.triprabbit.feature.vehicles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobgain.triprabbit.core.designsystem.component.*
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.util.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 28.dp, top = 16.dp, end = 28.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BrandMark(withBackground = false, size = 76.dp)
            Text(
                "TripRabbit",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        BrandArtworkSlot(BrandArtwork.welcome)
        PageHeading("Keep track of your mileage", "Log odometer readings, review your trips and export reports. Your records stay on this device.")
        PrimaryAction("Get Started", onGetStarted, icon = AppIcon.Chevron)
        Text("Add your first vehicle to begin.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun VehicleEditorScreen(onSaved: (Long) -> Unit, onBack: () -> Unit, viewModel: VehicleEditorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collectLatest { if (it is VehicleEditorEffect.Saved) onSaved(it.vehicleId) } }
    VehicleEditorContent(state, onBack, viewModel::update, viewModel::save)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleEditorContent(state: VehicleEditorUiState, onBack: () -> Unit = {},
    onUpdate: ((VehicleEditorUiState) -> VehicleEditorUiState) -> Unit = {}, onSave: () -> Unit = {}) {
    Scaffold(topBar = { DetailTopBar(if (state.editing) "Edit Vehicle" else "Create Vehicle", onBack) }) { padding ->
        if (state.loading) { LoadingState(Modifier.padding(padding)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FormField(state.name, { value -> onUpdate { it.copy(name = value, error = null) } }, "Vehicle name", hint = "For example: Daily driver or Work truck", enabled = !state.saving)
                if (!state.editing) FormField(state.initialReading, { value -> onUpdate { it.copy(initialReading = value.filter(Char::isDigit), error = null) } },
                    "Current odometer", suffix = state.unit.abbreviation, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !state.saving)
                Text("Distance unit", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DistanceUnit.entries.forEach { unit ->
                        FilterChip(selected = state.unit == unit, onClick = { onUpdate { it.copy(unit = unit) } }, enabled = !state.editing && !state.saving,
                            label = { Text(if (unit == DistanceUnit.KILOMETERS) "Kilometres" else "Miles") }, shape = MaterialTheme.shapes.medium,
                            leadingIcon = if (state.unit == unit) ({ TripIcon(AppIcon.Check, modifier = Modifier.size(16.dp)) }) else null)
                    }
                }
                Text(if (state.editing) "The unit is fixed to keep existing readings consistent." else "Use the same unit as your vehicle’s odometer.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            item { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SectionTitle("Vehicle details")
                FormField(state.make, { value -> onUpdate { it.copy(make = value) } }, "Make (optional)", enabled = !state.saving)
                FormField(state.model, { value -> onUpdate { it.copy(model = value) } }, "Model (optional)", enabled = !state.saving)
                FormField(state.year, { value -> onUpdate { it.copy(year = value.filter(Char::isDigit)) } }, "Year (optional)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !state.saving)
                FormField(state.licensePlate, { value -> onUpdate { it.copy(licensePlate = value) } }, "License plate (optional)", enabled = !state.saving)
                FormField(state.colorKey, { value -> onUpdate { it.copy(colorKey = value) } }, "Colour (optional)", enabled = !state.saving)
                FormField(state.notes, { value -> onUpdate { it.copy(notes = value) } }, "Notes (optional)", singleLine = false, enabled = !state.saving)
            } }
            state.error?.let { error -> item { InlineMessage(error, error = true) } }
            item { PrimaryAction(if (state.editing) "Save Changes" else "Create Vehicle", onSave, icon = AppIcon.Check, busy = state.saving) }
        }
    }
}

@Composable
fun VehicleListScreen(vehicles: List<Vehicle>, latestValues: Map<Long, Long>, density: DisplayDensity,
    onOpen: (Long) -> Unit, onAdd: () -> Unit, onBack: (() -> Unit)? = null) {
    Scaffold { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(if (density == DisplayDensity.COMPACT) 8.dp else 16.dp)) {
            item { PageHeading("Garage", "${vehicles.size} ${if (vehicles.size == 1) "vehicle" else "vehicles"}", action = {
                FilledTonalButton(onClick = onAdd, shape = MaterialTheme.shapes.medium) { TripIcon(AppIcon.Add); Spacer(Modifier.width(6.dp)); Text("Add vehicle") }
            }, actionAlignedWithTitle = true) }
            if (vehicles.isEmpty()) item { EmptyState("Room for your first vehicle", "Add a vehicle to start keeping track of your mileage.", "Add Vehicle", onAdd) }
            items(vehicles, key = { it.id }) { vehicle ->
                if (density == DisplayDensity.COMPACT) {
                    Surface(onClick = { onOpen(vehicle.id) }, modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                AdaptiveSingleLineText(vehicle.name, style = MaterialTheme.typography.titleSmall, minFontSize = 12.sp)
                                AdaptiveSingleLineText(latestValues[vehicle.id]?.let { "${it.grouped()} ${vehicle.odometerUnit.abbreviation}" } ?: "No odometer reading",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TripIcon(AppIcon.Chevron, "Vehicle details", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                } else Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(vehicle.name, style = MaterialTheme.typography.titleLarge)
                                vehicle.subtitle()?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Current odometer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(latestValues[vehicle.id]?.let { "${it.grouped()} ${vehicle.odometerUnit.abbreviation}" } ?: "No reading",
                                style = MaterialTheme.typography.headlineSmall)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        TextButton(onClick = { onOpen(vehicle.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Vehicle details"); Spacer(Modifier.width(4.dp)); TripIcon(AppIcon.Chevron, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleDetailsScreen(onBack: () -> Unit, onAdd: (Long) -> Unit, onHistory: (Long) -> Unit, onEdit: (Long) -> Unit,
    onGone: () -> Unit, viewModel: VehicleDetailsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.gone.collectLatest { onGone() } }
    state.error?.let { AlertDialog(onDismissRequest = viewModel::clearError, title = { Text("Couldn’t complete operation") }, text = { Text(it) },
        confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } }) }
    VehicleDetailsContent(state, onBack, onAdd, onHistory, onEdit, { viewModel.delete() })
}

@Composable
fun VehicleDetailsContent(state: VehicleDetailsUiState, onBack: () -> Unit = {}, onAdd: (Long) -> Unit = {}, onHistory: (Long) -> Unit = {},
    onEdit: (Long) -> Unit = {}, onDelete: () -> Unit = {}) {
    val vehicle = state.vehicle
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (confirmDelete && vehicle != null) DestructiveConfirmationDialog("Delete ${vehicle.name}?",
        "This permanently deletes the vehicle and all ${state.readings.size} readings. This cannot be undone.",
        { confirmDelete = false; onDelete() }, { confirmDelete = false }, confirmLabel = "Delete")
    Scaffold(topBar = { DetailTopBar("Vehicle details", onBack) }) { padding ->
        if (state.loading) LoadingState(Modifier.padding(padding))
        else if (vehicle == null) Box(Modifier.padding(padding)) { EmptyState("Vehicle not found", "It may have been deleted.", "Go Back", onBack) }
        else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item { PageHeading(vehicle.name, vehicle.subtitle().orEmpty()) }
            item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Current odometer")
                state.readings.firstOrNull { !it.inProgress }?.let { OdometerDisplay(it.value, vehicle.odometerUnit) } ?: Text("No readings yet")
                PrimaryAction("Add Trip", { onAdd(vehicle.id) }, icon = AppIcon.Add, enabled = !state.busy)
            } }
            item { AdaptivePair(
                first = { MetricTile("Distance tracked", "${state.readings.filterNot { it.inProgress }.sortedBy { reading -> reading.recordedAt }.mapIndexed { index, reading -> reading.startValue?.let { reading.value - it } ?: if (index == 0) 0L else reading.value - state.readings.filterNot { it.inProgress }.sortedBy { r -> r.recordedAt }[index - 1].value }.sum().grouped()} ${vehicle.odometerUnit.abbreviation}", it) },
                second = { MetricTile("Readings logged", state.readings.count { !it.inProgress }.toString(), it) },
            ) }
            item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow("View Trips", "All trips for ${vehicle.name}", AppIcon.History, { onHistory(vehicle.id) }, enabled = !state.busy)
                vehicle.licensePlate?.let { Text("License plate · $it", style = MaterialTheme.typography.bodyMedium) }
                vehicle.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } }
            item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SectionTitle("Manage vehicle")
                ActionRow("Edit Vehicle", "Name, details, and notes", AppIcon.Edit, { onEdit(vehicle.id) }, enabled = !state.busy)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow("Delete Vehicle", "Permanently delete vehicle and readings.", AppIcon.Trash, { confirmDelete = true }, enabled = !state.busy, destructive = true)
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            } }
        }
    }
}

fun Vehicle.subtitle(): String? = listOfNotNull(year?.toString(), make, model).takeIf { it.isNotEmpty() }?.joinToString(" ")
