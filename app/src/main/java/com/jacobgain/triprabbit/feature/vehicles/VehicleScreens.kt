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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobgain.triprabbit.core.designsystem.component.*
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.util.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Spacer(Modifier.height(16.dp))
        BrandHeader()
        Spacer(Modifier.height(12.dp))
        PageHeading("Track your mileage", "Log readings and review your trips in one place.")
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            WelcomeFeature(AppIcon.Gauge, "Log in a moment", "One reading. An organised history.")
            WelcomeFeature(AppIcon.Reports, "Review reports", "See distance over time.")
            WelcomeFeature(AppIcon.Shield, "Local storage", "Your records stay on your device.")
        }
        PrimaryAction("Get Started", onGetStarted, icon = AppIcon.Chevron)
        Text("Start with a vehicle. Build from there.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun WelcomeFeature(icon: AppIcon, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBadge(icon)
        Column { Text(title, style = MaterialTheme.typography.titleSmall); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun VehicleEditorScreen(onSaved: (Long) -> Unit, onBack: () -> Unit, viewModel: VehicleEditorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collectLatest { if (it is VehicleEditorEffect.Saved) onSaved(it.vehicleId) } }
    VehicleEditorContent(state, onBack, viewModel::update, viewModel::save)
}

@Composable
fun VehicleEditorContent(state: VehicleEditorUiState, onBack: () -> Unit = {},
    onUpdate: ((VehicleEditorUiState) -> VehicleEditorUiState) -> Unit = {}, onSave: () -> Unit = {}) {
    Scaffold(topBar = { DetailTopBar(if (state.editing) "Edit Vehicle" else "Create Vehicle", onBack) }) { padding ->
        if (state.loading) { LoadingState(Modifier.padding(padding)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item { PageHeading(if (state.editing) "Edit vehicle" else "Add vehicle",
                if (state.editing) "" else "Enter a name and starting odometer reading.") }
            item { SectionCard {
                SectionTitle("The essentials")
                FormField(state.name, { value -> onUpdate { it.copy(name = value, error = null) } }, "Vehicle name", hint = "For example, Daily driver or Civic", enabled = !state.saving)
                if (!state.editing) FormField(state.initialReading, { value -> onUpdate { it.copy(initialReading = value.filter(Char::isDigit), error = null) } },
                    "Current odometer", suffix = state.unit.abbreviation, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !state.saving)
                Text("Distance unit", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DistanceUnit.entries.forEach { unit ->
                        FilterChip(selected = state.unit == unit, onClick = { onUpdate { it.copy(unit = unit) } }, enabled = !state.editing && !state.saving,
                            label = { Text(if (unit == DistanceUnit.KILOMETERS) "Kilometres" else "Miles") }, shape = MaterialTheme.shapes.medium,
                            leadingIcon = if (state.unit == unit) ({ TripIcon(AppIcon.Check, modifier = Modifier.size(16.dp)) }) else null)
                    }
                }
                Text(if (state.editing) "The unit is fixed to keep existing readings consistent." else "Use the same unit as your vehicle’s odometer.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            item { SectionCard {
                SectionTitle("A few more details", "Optional, but helpful when you have more than one vehicle.")
                FormField(state.make, { value -> onUpdate { it.copy(make = value) } }, "Make (optional)", enabled = !state.saving)
                FormField(state.model, { value -> onUpdate { it.copy(model = value) } }, "Model (optional)", enabled = !state.saving)
                FormField(state.year, { value -> onUpdate { it.copy(year = value.filter(Char::isDigit)) } }, "Year (optional)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !state.saving)
                if (state.editing) {
                    FormField(state.licensePlate, { value -> onUpdate { it.copy(licensePlate = value) } }, "License plate", enabled = !state.saving)
                    FormField(state.colorKey, { value -> onUpdate { it.copy(colorKey = value) } }, "Colour", enabled = !state.saving)
                    FormField(state.notes, { value -> onUpdate { it.copy(notes = value) } }, "Notes", singleLine = false, enabled = !state.saving)
                }
            } }
            state.error?.let { error -> item { InlineMessage(error, error = true) } }
            item { PrimaryAction(if (state.editing) "Save Changes" else "Create Vehicle", onSave, icon = AppIcon.Check, busy = state.saving) }
        }
    }
}

@Composable
fun VehicleListScreen(vehicles: List<Vehicle>, selectedId: Long?, latestValues: Map<Long, Long>, density: DisplayDensity,
    onSelect: (Long) -> Unit, onOpen: (Long) -> Unit, onAdd: () -> Unit, onBack: (() -> Unit)? = null) {
    Scaffold(topBar = { DetailTopBar("Vehicles", onBack) }, floatingActionButton = {
        ExtendedFloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.medium, icon = { TripIcon(AppIcon.Add, "Add Vehicle") }, text = { Text("Add Vehicle") })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
            verticalArrangement = Arrangement.spacedBy(if (density == DisplayDensity.COMPACT) 8.dp else 16.dp)) {
            item { PageHeading("Garage", "${vehicles.size} ${if (vehicles.size == 1) "vehicle" else "vehicles"}") }
            if (vehicles.isEmpty()) item { EmptyState("Room for your first vehicle", "Add a vehicle to start keeping track of your mileage.", "Add Vehicle", onAdd) }
            items(vehicles, key = { it.id }) { vehicle ->
                if (density == DisplayDensity.COMPACT) {
                    Surface(onClick = { onOpen(vehicle.id) }, modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconBadge(AppIcon.Car, accented = vehicle.id == selectedId)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(vehicle.name, style = MaterialTheme.typography.titleSmall)
                                Text(latestValues[vehicle.id]?.let { "${it.grouped()} ${vehicle.odometerUnit.abbreviation}" } ?: "No reading",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (vehicle.id == selectedId) TripIcon(AppIcon.Check, "Current vehicle", tint = MaterialTheme.colorScheme.primary)
                            else TextButton(onClick = { onSelect(vehicle.id) }) { Text("Make current") }
                            TripIcon(AppIcon.Chevron, "Vehicle details", modifier = Modifier.size(18.dp))
                        }
                    }
                } else SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(AppIcon.Car, accented = vehicle.id == selectedId)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(vehicle.name, style = MaterialTheme.typography.titleLarge)
                            vehicle.subtitle()?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                    if (vehicle.id == selectedId) StatusPill("Current vehicle", icon = AppIcon.Check)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    latestValues[vehicle.id]?.let { value ->
                        Text("${value.grouped()} ${vehicle.odometerUnit.abbreviation}", style = MaterialTheme.typography.headlineSmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { onOpen(vehicle.id) }) { Text("Vehicle details"); TripIcon(AppIcon.Chevron, modifier = Modifier.size(18.dp)) }
                        if (vehicle.id != selectedId) FilledTonalButton(onClick = { onSelect(vehicle.id) }, shape = MaterialTheme.shapes.small) { Text("Make current") }
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
    VehicleDetailsContent(state, onBack, onAdd, onHistory, onEdit, { viewModel.archive() }, { viewModel.delete() })
}

@Composable
fun VehicleDetailsContent(state: VehicleDetailsUiState, onBack: () -> Unit = {}, onAdd: (Long) -> Unit = {}, onHistory: (Long) -> Unit = {},
    onEdit: (Long) -> Unit = {}, onArchive: () -> Unit = {}, onDelete: () -> Unit = {}) {
    val vehicle = state.vehicle
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    if (confirmation != null && vehicle != null) DestructiveConfirmationDialog(
        if (confirmation == "archive") "Archive ${vehicle.name}?" else "Delete ${vehicle.name}?",
        if (confirmation == "archive") "This hides the vehicle from your garage. Its readings remain in your local backup data."
        else "This permanently deletes the vehicle and all ${state.readings.size} readings. This cannot be undone.",
        { if (confirmation == "archive") onArchive() else onDelete(); confirmation = null }, { confirmation = null },
        confirmLabel = if (confirmation == "archive") "Archive" else "Delete")
    Scaffold(topBar = { DetailTopBar("Vehicle details", onBack, actions = {
        if (vehicle != null) IconButton(onClick = { onEdit(vehicle.id) }, enabled = !state.busy) { TripIcon(AppIcon.Edit, "Edit Vehicle") }
    }) }) { padding ->
        if (state.loading) LoadingState(Modifier.padding(padding))
        else if (vehicle == null) Box(Modifier.padding(padding)) { EmptyState("Vehicle not found", "It may have been archived or deleted.", "Go Back", onBack) }
        else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item { PageHeading(vehicle.name, vehicle.subtitle().orEmpty(), action = { IconBadge(AppIcon.Car, accented = true) }) }
            item { SectionCard {
                SectionTitle("Current odometer")
                state.readings.firstOrNull()?.let { OdometerDisplay(it.value, vehicle.odometerUnit) } ?: Text("No readings yet")
                PrimaryAction("Add Reading", { onAdd(vehicle.id) }, icon = AppIcon.Add, enabled = !state.busy)
            } }
            item { AdaptivePair(
                first = { MetricTile("Distance tracked", "${if (state.readings.size > 1) (state.readings.first().value - state.readings.last().value).grouped() else "0"} ${vehicle.odometerUnit.abbreviation}", it, icon = AppIcon.Distance) },
                second = { MetricTile("Readings logged", state.readings.size.toString(), it, icon = AppIcon.History) },
            ) }
            item { SectionCard {
                ActionRow("View History", "All readings for ${vehicle.name}", AppIcon.History, { onHistory(vehicle.id) }, enabled = !state.busy)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow("Edit Vehicle", "Name, details, and notes", AppIcon.Edit, { onEdit(vehicle.id) }, enabled = !state.busy)
                vehicle.licensePlate?.let { Text("License plate · $it", style = MaterialTheme.typography.bodyMedium) }
                vehicle.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } }
            item { SectionCard {
                SectionTitle("Manage vehicle")
                ActionRow("Archive Vehicle", "Hide from your active garage.", AppIcon.Archive, { confirmation = "archive" }, enabled = !state.busy)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow("Delete Vehicle", "Permanently delete vehicle and readings.", AppIcon.Trash, { confirmation = "delete" }, enabled = !state.busy, destructive = true)
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            } }
        }
    }
}

fun Vehicle.subtitle(): String? = listOfNotNull(year?.toString(), make, model).takeIf { it.isNotEmpty() }?.joinToString(" ")
