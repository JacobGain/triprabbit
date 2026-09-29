package com.jacobgain.triprabbit.feature.readings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobgain.triprabbit.core.designsystem.component.*
import com.jacobgain.triprabbit.core.model.VEHICLE_NAME_MAX_LENGTH
import com.jacobgain.triprabbit.core.util.*
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun AddReadingScreen(onBack: () -> Unit, onSaved: (String) -> Unit, viewModel: AddReadingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collectLatest { if (it is ReadingEffect.Saved) onSaved(it.message) } }
    AddReadingContent(state, onBack, viewModel::valueChanged, viewModel::dateChanged, viewModel::noteChanged, { viewModel.save() }, viewModel::nameChanged, viewModel::timeChanged, viewModel::startValueChanged, { viewModel.save(inProgress = true) }, viewModel::selectVehicle)
}

@Composable
fun AddReadingContent(state: AddReadingUiState, onBack: () -> Unit = {}, onValue: (String) -> Unit = {},
    onDate: (Instant) -> Unit = {}, onNote: (String) -> Unit = {}, onSave: () -> Unit = {}, onName: (String) -> Unit = {}, onTime: (Boolean) -> Unit = {}, onStart: (String) -> Unit = {}, onStartTrip: () -> Unit = {}, onSelectVehicle: (Long) -> Unit = {}) {
    var dateText by rememberSaveable { mutableStateOf(state.recordedAt.inputDate()) }
    var editingOdometer by rememberSaveable { mutableStateOf<String?>(null) }
    var vehicleMenuExpanded by remember { mutableStateOf(false) }
    val dateValid = if (state.hasTime) dateText.parseDateTime() != null else dateText.parseInputDate() != null
    val nameValid = state.name.isNotBlank()
    val unit = state.vehicle?.odometerUnit?.abbreviation.orEmpty()
    val startPlaceholder = state.previous?.value?.toString().orEmpty()
    val finishPlaceholder = state.startValue.ifBlank { startPlaceholder }
    Scaffold(topBar = { DetailTopBar("Add Trip", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AdaptiveSingleLineText(state.vehicle?.name ?: "Loading vehicle…", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                        AdaptiveSingleLineText("Previous reading: ${state.previous?.value?.grouped() ?: "—"} ${state.vehicle?.odometerUnit?.abbreviation.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.vehicles.size > 1) Box {
                        TextButton(onClick = { vehicleMenuExpanded = true }) { Text("Change vehicle", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        DropdownMenu(expanded = vehicleMenuExpanded, onDismissRequest = { vehicleMenuExpanded = false }) {
                            state.vehicles.forEach { car -> DropdownMenuItem(
                                text = { Text(car.name, maxLines = 1) },
                                onClick = { vehicleMenuExpanded = false; onSelectVehicle(car.id) },
                            ) }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionTitle("Odometer")
                OdometerEditButton("Start odometer", state.startValue, startPlaceholder, unit, !state.saving) { editingOdometer = "start" }
                if (editingOdometer == "start") OdometerEditor("Edit start odometer", state.startValue, startPlaceholder, unit, { editingOdometer = null }, { onStart(it); editingOdometer = null })
                OdometerEditButton("Finish odometer", state.value, finishPlaceholder, unit, !state.saving) { editingOdometer = "finish" }
                if (editingOdometer == "finish") OdometerEditor("Edit finish odometer", state.value, finishPlaceholder, unit, { editingOdometer = null }, { onValue(it); editingOdometer = null })
                val entered = state.value.toLongOrNull()
                val previous = state.previous
                val start = state.startValue.toLongOrNull() ?: previous?.value
                if (entered != null && start != null && entered >= start)
                    InlineMessage("${(entered - start).grouped()} ${state.vehicle?.odometerUnit?.abbreviation} travelled")
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SectionTitle("Trip details")
                FormField(state.name, onName, "Trip name", hint = "Required", enabled = !state.saving)
                ReadingDateTimeField(dateText, { dateText = it; (if (state.hasTime) it.parseDateTime() else it.parseInputDate())?.let(onDate) }, state.hasTime, { enabled ->
                    dateText = if (enabled) (dateText.parseInputDate() ?: state.recordedAt).inputDateTime() else (dateText.parseDateTime() ?: state.recordedAt).inputDate()
                    onTime(enabled); (if (enabled) dateText.parseDateTime() else dateText.parseInputDate())?.let(onDate)
                }, enabled = !state.saving)
                FormField(state.note, onNote, "Note (optional)", singleLine = false, enabled = !state.saving)
            }
            state.error?.let { InlineMessage(it, error = true) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onStartTrip, enabled = !state.saving && state.vehicle != null && dateValid && nameValid && editingOdometer == null && !state.hasInProgressTrip,
                    modifier = Modifier.fillMaxWidth()) { Text("Finish later") }
                Text(if (state.hasInProgressTrip) "Finish your in-progress trip before starting another." else "Save the starting odometer now, and finish this trip later.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PrimaryAction("Save Trip", onSave, icon = AppIcon.Check, enabled = dateValid && state.vehicle != null && nameValid && editingOdometer == null && !state.hasInProgressTrip, busy = state.saving)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (state.hasInProgressTrip) {
                        Text("Finish your in-progress trip before saving another", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    } else {
                        Text("Save the start and finish odometers.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text("You can edit this trip later.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun ReadingHistoryScreen(onBack: (() -> Unit)?, onAdd: (Long) -> Unit, onEdit: (Long) -> Unit, viewModel: ReadingHistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReadingHistoryContent(state, onBack, onAdd, onEdit)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ReadingHistoryContent(state: ReadingHistoryUiState, onBack: (() -> Unit)? = null, onAdd: (Long) -> Unit = {}, onEdit: (Long) -> Unit = {}) {
    var selectedVehicleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTrip by remember { mutableStateOf<ReadingItem?>(null) }
    LaunchedEffect(state.loading, state.vehicles.map { it.id }) {
        if (!state.loading && selectedVehicleId != null && state.vehicles.none { it.id == selectedVehicleId }) {
            selectedVehicleId = null
        }
    }
    val trips = state.items.filter { it.reading.inProgress || it.difference != null || it.reading.startValue != null }
    val filtered = trips.filter { item ->
        val matchesVehicle = selectedVehicleId == null || item.reading.vehicleId == selectedVehicleId
        matchesVehicle && (query.isBlank() || listOf(item.reading.value.toString(), item.reading.value.grouped(),
            item.reading.name.orEmpty(), item.reading.note.orEmpty(), item.vehicleName.orEmpty(), item.reading.recordedAt.displayDate()).any { it.contains(query, ignoreCase = true) })
    }
    val pinned = if (state.pinInProgressTrips) filtered.filter { it.reading.inProgress } else emptyList()
    val regular = if (state.pinInProgressTrips) filtered.filterNot { it.reading.inProgress } else filtered
    selectedTrip?.let { item ->
        Dialog(onDismissRequest = { selectedTrip = null }) {
            Surface(Modifier.fillMaxWidth().widthIn(max = 520.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
              Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.reading.name?.takeIf { it.isNotBlank() } ?: "Trip details", style = MaterialTheme.typography.headlineSmall)
                        Text(item.vehicleName ?: state.vehicle?.name.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { selectedTrip = null }) { TripIcon(AppIcon.Close, "Close") }
                }
                Text(if (item.reading.hasTime) item.reading.recordedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d · h:mm a")) else item.reading.recordedAt.displayDate(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SectionCard {
                    SectionTitle("Odometer")
                    val tripStart = item.reading.startValue ?: item.difference?.let { item.reading.value - it }
                    AdaptivePair(
                        first = { Column(it) { Text("Start", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); AdaptiveSingleLineText("${tripStart?.grouped() ?: "—"} ${item.unit.ifBlank { state.vehicle?.odometerUnit?.abbreviation.orEmpty() }}", style = MaterialTheme.typography.titleMedium, minFontSize = 12.sp) } },
                        second = { Column(it) {
                            Text("Finish", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AdaptiveSingleLineText(if (item.reading.inProgress) "In progress" else "${item.reading.value.grouped()} ${item.unit.ifBlank { state.vehicle?.odometerUnit?.abbreviation.orEmpty() }}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (item.reading.inProgress) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                minFontSize = 12.sp)
                        } },
                    )
                    if (!item.reading.inProgress) AdaptiveSingleLineText("${item.difference?.grouped() ?: "—"} ${item.unit.ifBlank { state.vehicle?.odometerUnit?.abbreviation.orEmpty() }} travelled",
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, minFontSize = 12.sp)
                }
                item.reading.note?.takeIf { it.isNotBlank() }?.let { SectionCard { SectionTitle("Note"); Text(it, style = MaterialTheme.typography.bodyMedium) } }
                PrimaryAction(if (item.reading.inProgress) "Finish trip" else "Edit trip", { val id = item.reading.id; selectedTrip = null; onEdit(id) }, icon = AppIcon.Edit)
                TextButton(onClick = { selectedTrip = null }, modifier = Modifier.fillMaxWidth()) { Text("Close") }
              }
            }
        }
    }
    Scaffold(topBar = { if (onBack != null) DetailTopBar("Trips", onBack) }) { padding ->
        if (state.loading) { LoadingState(Modifier.padding(padding)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { PageHeading("Trips", "") }
            item {
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().clearFocusWhenKeyboardCloses(), placeholder = { Text("Search trips", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { TripIcon(AppIcon.Search) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { TripIcon(AppIcon.Close, "Clear search") } },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface))
            }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedVehicleId == null, onClick = { selectedVehicleId = null }, label = { Text("All Vehicles") }, shape = MaterialTheme.shapes.medium,
                    leadingIcon = if (selectedVehicleId == null) ({ TripIcon(AppIcon.Check, modifier = Modifier.size(16.dp)) }) else null)
                state.vehicles.forEach { car ->
                    FilterChip(selected = selectedVehicleId == car.id, onClick = { selectedVehicleId = car.id },
                        label = { Text(car.name.take(VEHICLE_NAME_MAX_LENGTH), maxLines = 1, overflow = TextOverflow.Ellipsis) }, shape = MaterialTheme.shapes.medium,
                        leadingIcon = if (selectedVehicleId == car.id) ({ TripIcon(AppIcon.Check, modifier = Modifier.size(16.dp)) }) else null)
                }
            } }
            if (pinned.isNotEmpty()) {
                item {
                    Text("In progress", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(pinned, key = { it.reading.id }) { item ->
                    TripHistoryCard(item, state.pinInProgressTrips, state.vehicle?.odometerUnit?.abbreviation.orEmpty()) { selectedTrip = item }
                }
            }
            if (filtered.isEmpty()) item {
                val noTripsYet = trips.isEmpty()
                EmptyState(if (noTripsYet) "No trips yet" else "No matching trips",
                    if (noTripsYet) "Create your first trip and it will appear here." else "Try a different search or switch to All Vehicles.")
            }
            itemsIndexed(regular, key = { _, item -> item.reading.id }) { index, item ->
                if (index == 0 || regular[index - 1].reading.recordedAt.displayMonth() != item.reading.recordedAt.displayMonth()) {
                    Text(item.reading.recordedAt.displayMonth(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                }
                TripHistoryCard(item, state.pinInProgressTrips, state.vehicle?.odometerUnit?.abbreviation.orEmpty()) { selectedTrip = item }
            }
        }
    }
}

@Composable
private fun TripHistoryCard(item: ReadingItem, showPinned: Boolean, fallbackUnit: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.reading.name?.takeIf { it.isNotBlank() } ?: "Untitled trip", style = MaterialTheme.typography.titleMedium)
                Text("${item.vehicleName?.take(VEHICLE_NAME_MAX_LENGTH)?.takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty()}${item.reading.recordedAt.displayDate()}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
            if (item.reading.inProgress) Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (showPinned) TripIcon(AppIcon.Pin, "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("In progress", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            } else Text("${item.difference?.grouped() ?: "—"}${(item.unit.ifBlank { fallbackUnit }).takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()}",
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            TripIcon(AppIcon.Chevron, "Trip details", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun EditReadingScreen(onBack: () -> Unit, onSaved: (String) -> Unit, viewModel: EditReadingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collectLatest { if (it is ReadingEffect.Saved) onSaved(it.message) } }
    EditReadingContent(state, onBack, viewModel::valueChanged, viewModel::dateChanged, viewModel::noteChanged, { viewModel.save() }, { viewModel.delete() }, viewModel::nameChanged, viewModel::timeChanged, viewModel::startValueChanged, viewModel::saveInProgress)
}

@Composable
fun EditReadingContent(state: EditReadingUiState, onBack: () -> Unit = {}, onValue: (String) -> Unit = {}, onDate: (Instant) -> Unit = {},
    onNote: (String) -> Unit = {}, onSave: () -> Unit = {}, onDelete: () -> Unit = {}, onName: (String) -> Unit = {}, onTime: (Boolean) -> Unit = {}, onStart: (String) -> Unit = {}, onKeepInProgress: () -> Unit = {}) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var editingOdometer by rememberSaveable { mutableStateOf<String?>(null) }
    var dateText by rememberSaveable(state.reading?.id) { mutableStateOf(if (state.hasTime) state.recordedAt.inputDateTime() else state.recordedAt.inputDate()) }
    if (confirmDelete) DestructiveConfirmationDialog("Delete this trip?", "This removes the trip and its odometer reading from your history. This cannot be undone.",
        { confirmDelete = false; onDelete() }, { confirmDelete = false })
    Scaffold(topBar = { DetailTopBar(if (state.reading?.inProgress == true) "Finish trip" else "Edit Trip", onBack) }) { padding ->
        if (state.loading) LoadingState(Modifier.padding(padding))
        else if (state.reading == null) Box(Modifier.padding(padding)) { EmptyState("Reading not found", "It may have already been deleted.", "Go Back", onBack) }
        else Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionTitle("Odometer readings")
                OdometerEditButton("Start odometer", state.startValue, state.startValue, state.unit, !state.saving) { editingOdometer = "start" }
                if (editingOdometer == "start") OdometerEditor("Edit start odometer", state.startValue, state.startValue, state.unit, { editingOdometer = null }, { onStart(it); editingOdometer = null })
                OdometerEditButton("Finish odometer", state.value, state.value, state.unit, !state.saving) { editingOdometer = "finish" }
                if (editingOdometer == "finish") OdometerEditor("Edit finish odometer", state.value, state.value, state.unit, { editingOdometer = null }, { onValue(it); editingOdometer = null })
                FormField(state.name, onName, "Trip name", hint = "Required", enabled = !state.saving)
                ReadingDateTimeField(dateText, { dateText = it; (if (state.hasTime) it.parseDateTime() else it.parseInputDate())?.let(onDate) }, state.hasTime, { enabled ->
                    dateText = if (enabled) (dateText.parseInputDate() ?: state.recordedAt).inputDateTime() else (dateText.parseDateTime() ?: state.recordedAt).inputDate()
                    onTime(enabled); (if (enabled) dateText.parseDateTime() else dateText.parseInputDate())?.let(onDate)
                }, enabled = !state.saving)
                FormField(state.note, onNote, "Note (optional)", singleLine = false, enabled = !state.saving)
            }
            state.error?.let { InlineMessage(it, error = true) }
            val actionsEnabled = !state.saving && state.name.isNotBlank() && editingOdometer == null &&
                (if (state.hasTime) dateText.parseDateTime() else dateText.parseInputDate()) != null
            if (state.reading.inProgress) {
                OutlinedButton(onClick = onKeepInProgress, enabled = actionsEnabled,
                    modifier = Modifier.fillMaxWidth()) { Text("Keep in progress") }
                PrimaryAction("Finish trip", onSave, icon = AppIcon.Check, enabled = actionsEnabled, busy = state.saving)
            } else {
                PrimaryAction("Save Changes", onSave, icon = AppIcon.Check, enabled = actionsEnabled, busy = state.saving)
            }
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow("Delete Trip", "Permanently remove this trip.", AppIcon.Trash,
                    onClick = { if (state.confirmDeletion) confirmDelete = true else onDelete() }, enabled = !state.saving, destructive = true)
            }
        }
    }
}

@Composable
private fun OdometerEditButton(label: String, value: String, placeholder: String, unit: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text((value.ifBlank { placeholder }).toLongOrNull()?.grouped() ?: "Enter odometer", style = MaterialTheme.typography.headlineSmall)
                Text(unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            TripIcon(AppIcon.Edit, "Edit $label", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OdometerEditor(title: String, value: String, placeholder: String, unit: String, onDismiss: () -> Unit, onApply: (String) -> Unit) {
    var working by remember(value, placeholder) { mutableStateOf(value.ifBlank { placeholder }) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val availableWidth = maxWidth
            val availableHeight = maxHeight
            Surface(
                Modifier.fillMaxWidth(.94f).widthIn(max = 520.dp).heightIn(max = availableHeight * .9f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(
                    Modifier.imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    FormField(working, { working = it.filter(Char::isDigit).take(18) }, "Odometer reading", placeholder = placeholder,
                        suffix = unit, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Text("Type the reading or adjust individual digits.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val digits = working.ifBlank { "0" }.padStart(6, '0')
                    val columns = minOf(digits.length, 6)
                    val cellWidth = ((availableWidth * .94f - 40.dp) / columns).coerceAtMost(48.dp)
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        digits.forEachIndexed { index, digit ->
                            fun changeDigit(delta: Int) {
                                val changed = digits.toCharArray()
                                changed[index] = ('0'.code + (digit.digitToInt() + delta + 10) % 10).toChar()
                                working = changed.concatToString().trimStart('0').ifEmpty { "0" }
                            }
                            Column(Modifier.width(cellWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.fillMaxWidth().height(36.dp).clickable(role = Role.Button,
                                    onClickLabel = "Increase digit ${index + 1}", onClick = { changeDigit(1) })
                                    .semantics { contentDescription = "Increase digit ${index + 1}" }, contentAlignment = Alignment.Center) {
                                    Text("+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(digit.toString(), style = if (cellWidth >= 42.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium)
                                Box(Modifier.fillMaxWidth().height(36.dp).clickable(role = Role.Button,
                                    onClickLabel = "Decrease digit ${index + 1}", onClick = { changeDigit(-1) })
                                    .semantics { contentDescription = "Decrease digit ${index + 1}" }, contentAlignment = Alignment.Center) {
                                    Text("−", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    PrimaryAction("Apply", { onApply(working.ifBlank { placeholder }) }, icon = AppIcon.Check)
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingDateTimeField(value: String, onValue: (String) -> Unit, hasTime: Boolean, onTime: (Boolean) -> Unit, enabled: Boolean) {
    val context = LocalContext.current
    val valid = (if (hasTime) value.parseDateTime() else value.parseInputDate()) != null
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    val current = ((if (hasTime) value.parseDateTime() else value.parseInputDate()) ?: Instant.now()).atZone(ZoneId.systemDefault())
    if (showDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
            TextButton(enabled = picker.selectedDateMillis != null, onClick = {
                selectedDate = Instant.ofEpochMilli(checkNotNull(picker.selectedDateMillis)).atZone(ZoneOffset.UTC).toLocalDate()
                showDate = false
                if (hasTime) showTime = true else onValue(selectedDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().inputDate())
            }) { Text("Next") }
        }, dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }) { DatePicker(picker) }
    }
    if (showTime) {
        val picker = rememberTimePickerState(initialHour = current.hour, initialMinute = current.minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context))
        AlertDialog(onDismissRequest = { showTime = false }, title = { Text("Choose time") }, text = { TimeInput(picker) },
            confirmButton = { TextButton(onClick = {
                onValue(selectedDate.atTime(picker.hour, picker.minute).atZone(ZoneId.systemDefault()).toInstant().inputDateTime())
                showTime = false
            }) { Text("Apply") } }, dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } })
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Include time", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(hasTime, onCheckedChange = onTime, enabled = enabled)
    }
    OutlinedTextField(value, onValue, modifier = Modifier.fillMaxWidth().clearFocusWhenKeyboardCloses(), label = { Text(if (hasTime) "Date and time" else "Date") },
        supportingText = { Text(if (hasTime) "yyyy-MM-dd HH:mm" else "yyyy-MM-dd") },
        isError = !valid, enabled = enabled, singleLine = true, shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
        trailingIcon = { IconButton(enabled = enabled, onClick = { showDate = true }) { TripIcon(AppIcon.History, "Choose date") } })
}
