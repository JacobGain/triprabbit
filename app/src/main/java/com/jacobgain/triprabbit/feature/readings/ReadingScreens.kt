package com.jacobgain.triprabbit.feature.readings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobgain.triprabbit.core.designsystem.component.*
import com.jacobgain.triprabbit.core.util.*
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun AddReadingScreen(onBack: () -> Unit, onSaved: (String) -> Unit, viewModel: AddReadingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collectLatest { if (it is ReadingEffect.Saved) onSaved(it.message) } }
    AddReadingContent(state, onBack, viewModel::valueChanged, viewModel::dateChanged, viewModel::noteChanged, { viewModel.save() }, viewModel::nameChanged, viewModel::timeChanged, viewModel::startValueChanged)
}

@Composable
fun AddReadingContent(state: AddReadingUiState, onBack: () -> Unit = {}, onValue: (String) -> Unit = {},
    onDate: (Instant) -> Unit = {}, onNote: (String) -> Unit = {}, onSave: () -> Unit = {}, onName: (String) -> Unit = {}, onTime: (Boolean) -> Unit = {}, onStart: (String) -> Unit = {}) {
    var dateText by rememberSaveable { mutableStateOf(state.recordedAt.inputDate()) }
    var editingOdometer by rememberSaveable { mutableStateOf<String?>(null) }
    val dateValid = if (state.hasTime) dateText.parseDateTime() != null else dateText.parseInputDate() != null
    val unit = state.vehicle?.odometerUnit?.abbreviation.orEmpty()
    val startPlaceholder = state.previous?.value?.toString().orEmpty()
    val finishPlaceholder = state.startValue.ifBlank { startPlaceholder }
    if (editingOdometer == "start") OdometerEditorDialog("Edit start odometer", state.startValue, startPlaceholder, unit, { editingOdometer = null }, { onStart(it); editingOdometer = null })
    if (editingOdometer == "finish") OdometerEditorDialog("Edit finish odometer", state.value, finishPlaceholder, unit, { editingOdometer = null }, { onValue(it); editingOdometer = null })
    Scaffold(topBar = { DetailTopBar("Add Trip", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            PageHeading("Add trip", "Record a business trip for ${state.vehicle?.name.orEmpty()}.")
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(AppIcon.Car, accented = true)
                    Column(Modifier.weight(1f)) {
                        Text(state.vehicle?.name ?: "Loading vehicle…", style = MaterialTheme.typography.titleMedium)
                        Text("${state.previous?.value?.grouped() ?: "—"} ${state.vehicle?.odometerUnit?.abbreviation.orEmpty()} · previous reading",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionTitle("Odometer", "Enter the start and finish shown on your dashboard.")
                OdometerEditButton("Start odometer", state.startValue, startPlaceholder, unit, !state.saving) { editingOdometer = "start" }
                OdometerEditButton("Finish odometer", state.value, finishPlaceholder, unit, !state.saving) { editingOdometer = "finish" }
                val entered = state.value.toLongOrNull()
                val previous = state.previous
                val start = state.startValue.toLongOrNull() ?: previous?.value
                if (entered != null && start != null && entered >= start)
                    InlineMessage("${(entered - start).grouped()} ${state.vehicle?.odometerUnit?.abbreviation} travelled")
            }
            SectionCard {
                SectionTitle("Trip details")
                FormField(state.name, onName, "Trip name (optional)", hint = "For example, 123 Main Street", enabled = !state.saving)
                ReadingDateTimeField(dateText, { dateText = it; (if (state.hasTime) it.parseDateTime() else it.parseInputDate())?.let(onDate) }, state.hasTime, { enabled ->
                    dateText = if (enabled) (dateText.parseInputDate() ?: state.recordedAt).inputDateTime() else (dateText.parseDateTime() ?: state.recordedAt).inputDate()
                    onTime(enabled); (if (enabled) dateText.parseDateTime() else dateText.parseInputDate())?.let(onDate)
                }, enabled = !state.saving)
                FormField(state.note, onNote, "Note (optional)", singleLine = false, enabled = !state.saving)
            }
            state.error?.let { InlineMessage(it, error = true) }
            PrimaryAction("Save Trip", onSave, icon = AppIcon.Check, enabled = dateValid && state.vehicle != null, busy = state.saving)
            Text("Saved privately on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun ReadingHistoryScreen(onBack: (() -> Unit)?, onAdd: (Long) -> Unit, onEdit: (Long) -> Unit, viewModel: ReadingHistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReadingHistoryContent(state, onBack, onAdd, onEdit, viewModel::selectVehicle)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ReadingHistoryContent(state: ReadingHistoryUiState, onBack: (() -> Unit)? = null, onAdd: (Long) -> Unit = {}, onEdit: (Long) -> Unit = {}, onSelectVehicle: (Long) -> Unit = {}) {
    var filter by rememberSaveable { mutableStateOf("All Trips") }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTrip by remember { mutableStateOf<ReadingItem?>(null) }
    var vehicleMenuExpanded by remember { mutableStateOf(false) }
    val thisMonth = YearMonth.now()
    val trips = state.items.filter { it.difference != null || it.reading.startValue != null }
    val filtered = trips.filter { item ->
        val matchesFilter = when (filter) {
            "This Month" -> YearMonth.from(item.reading.recordedAt.atZone(ZoneId.systemDefault())) == thisMonth
            else -> true
        }
        matchesFilter && (query.isBlank() || listOf(item.reading.value.toString(), item.reading.value.grouped(),
            item.reading.name.orEmpty(), item.reading.note.orEmpty(), item.reading.recordedAt.displayDate()).any { it.contains(query, ignoreCase = true) })
    }
    val unit = state.vehicle?.odometerUnit?.abbreviation.orEmpty()
    selectedTrip?.let { item ->
        Dialog(onDismissRequest = { selectedTrip = null }) {
            Surface(Modifier.fillMaxWidth().widthIn(max = 520.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
              Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PageHeading(item.reading.name?.takeIf { it.isNotBlank() } ?: "Trip details", state.vehicle?.name.orEmpty(), action = { IconButton(onClick = { selectedTrip = null }) { TripIcon(AppIcon.Close, "Close") } })
                Text(if (item.reading.hasTime) item.reading.recordedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d · h:mm a")) else item.reading.recordedAt.displayDate(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SectionCard {
                    SectionTitle("Odometer")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val tripStart = item.reading.startValue ?: item.difference?.let { item.reading.value - it }
                        Column { Text("Start", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${tripStart?.grouped() ?: "—"} $unit", style = MaterialTheme.typography.titleMedium) }
                        Column(horizontalAlignment = Alignment.End) { Text("Finish", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${item.reading.value.grouped()} $unit", style = MaterialTheme.typography.titleMedium) }
                    }
                    Text("${item.difference?.grouped() ?: "—"} $unit travelled", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                item.reading.note?.takeIf { it.isNotBlank() }?.let { SectionCard { SectionTitle("Note"); Text(it, style = MaterialTheme.typography.bodyMedium) } }
                PrimaryAction("Edit trip", { val id = item.reading.id; selectedTrip = null; onEdit(id) }, icon = AppIcon.Edit)
              }
            }
        }
    }
    Scaffold(topBar = { if (onBack != null) DetailTopBar("Trips", onBack) }) { padding ->
        if (state.loading) { LoadingState(Modifier.padding(padding)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { PageHeading("Trips", "", action = {
                if (state.vehicles.isNotEmpty()) Box {
                    OutlinedButton(onClick = { vehicleMenuExpanded = true }, shape = MaterialTheme.shapes.medium) {
                        Text(state.vehicle?.name ?: "Choose car", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(4.dp)); TripIcon(AppIcon.Down, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = vehicleMenuExpanded, onDismissRequest = { vehicleMenuExpanded = false }) {
                        state.vehicles.forEach { car -> DropdownMenuItem(text = { Text(car.name, maxLines = 1) }, onClick = { vehicleMenuExpanded = false; onSelectVehicle(car.id) }) }
                    }
                }
            }) }
            item {
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search trips", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { TripIcon(AppIcon.Search) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { TripIcon(AppIcon.Close, "Clear search") } },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface))
            }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All Trips", "This Month").forEach { label ->
                    FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) }, shape = MaterialTheme.shapes.medium,
                        leadingIcon = if (filter == label) ({ TripIcon(AppIcon.Check, modifier = Modifier.size(16.dp)) }) else null)
                }
            } }
            item {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TripIcon(AppIcon.Distance, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${filtered.sumOf { it.difference ?: 0 }.grouped()} $unit", style = MaterialTheme.typography.headlineMedium)
                            Text("Recorded distance · ${filtered.size} trips", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (filtered.isEmpty()) item {
                EmptyState(if (trips.isEmpty()) "A fresh start" else "No matching trips",
                    if (state.items.isEmpty()) "Your trips will appear here as you log your mileage." else "Try a different search or switch to All Trips.")
            }
            itemsIndexed(filtered, key = { _, item -> item.reading.id }) { index, item ->
                if (index == 0 || filtered[index - 1].reading.recordedAt.displayMonth() != item.reading.recordedAt.displayMonth()) {
                    Text(item.reading.recordedAt.displayMonth(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                }
                Surface(onClick = { selectedTrip = item }, color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.reading.name?.takeIf { it.isNotBlank() } ?: "Untitled trip", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${item.difference?.grouped() ?: "—"} $unit", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            TripIcon(AppIcon.Chevron, "Trip details", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                        Text("${item.reading.recordedAt.displayDate()} · ${state.vehicle?.name.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun EditReadingScreen(onBack: () -> Unit, onSaved: (String) -> Unit, viewModel: EditReadingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collectLatest { if (it is ReadingEffect.Saved) onSaved(it.message) } }
    EditReadingContent(state, onBack, viewModel::valueChanged, viewModel::dateChanged, viewModel::noteChanged, { viewModel.save() }, { viewModel.delete() }, viewModel::nameChanged, viewModel::timeChanged, viewModel::startValueChanged)
}

@Composable
fun EditReadingContent(state: EditReadingUiState, onBack: () -> Unit = {}, onValue: (String) -> Unit = {}, onDate: (Instant) -> Unit = {},
    onNote: (String) -> Unit = {}, onSave: () -> Unit = {}, onDelete: () -> Unit = {}, onName: (String) -> Unit = {}, onTime: (Boolean) -> Unit = {}, onStart: (String) -> Unit = {}) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var editingOdometer by rememberSaveable { mutableStateOf<String?>(null) }
    var dateText by rememberSaveable(state.reading?.id) { mutableStateOf(if (state.hasTime) state.recordedAt.inputDateTime() else state.recordedAt.inputDate()) }
    if (editingOdometer == "start") OdometerEditorDialog("Edit start odometer", state.startValue, state.startValue, state.unit, { editingOdometer = null }, { onStart(it); editingOdometer = null })
    if (editingOdometer == "finish") OdometerEditorDialog("Edit finish odometer", state.value, state.value, state.unit, { editingOdometer = null }, { onValue(it); editingOdometer = null })
    if (confirmDelete) DestructiveConfirmationDialog("Delete this trip?", "This removes the trip and its odometer reading from your history. This cannot be undone.",
        { confirmDelete = false; onDelete() }, { confirmDelete = false })
    Scaffold(topBar = { DetailTopBar("Edit Trip", onBack) }) { padding ->
        if (state.loading) LoadingState(Modifier.padding(padding))
        else if (state.reading == null) Box(Modifier.padding(padding)) { EmptyState("Reading not found", "It may have already been deleted.", "Go Back", onBack) }
        else Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            PageHeading("Edit trip", "")
            SectionCard {
                SectionTitle("Odometer readings")
                OdometerEditButton("Start odometer", state.startValue, state.startValue, state.unit, !state.saving) { editingOdometer = "start" }
                OdometerEditButton("Finish odometer", state.value, state.value, state.unit, !state.saving) { editingOdometer = "finish" }
                FormField(state.name, onName, "Trip name (optional)", enabled = !state.saving)
                ReadingDateTimeField(dateText, { dateText = it; (if (state.hasTime) it.parseDateTime() else it.parseInputDate())?.let(onDate) }, state.hasTime, { enabled ->
                    dateText = if (enabled) (dateText.parseInputDate() ?: state.recordedAt).inputDateTime() else (dateText.parseDateTime() ?: state.recordedAt).inputDate()
                    onTime(enabled); (if (enabled) dateText.parseDateTime() else dateText.parseInputDate())?.let(onDate)
                }, enabled = !state.saving)
                FormField(state.note, onNote, "Note (optional)", singleLine = false, enabled = !state.saving)
            }
            state.error?.let { InlineMessage(it, error = true) }
            PrimaryAction("Save Changes", onSave, icon = AppIcon.Check, enabled = (if (state.hasTime) dateText.parseDateTime() else dateText.parseInputDate()) != null, busy = state.saving)
            SectionCard {
                ActionRow("Delete Trip", "Permanently remove this trip.", AppIcon.Trash,
                    onClick = { if (state.confirmDeletion) confirmDelete = true else onDelete() }, enabled = !state.saving, destructive = true)
            }
        }
    }
}

@Composable
private fun OdometerEditButton(label: String, value: String, placeholder: String, unit: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(value.ifBlank { placeholder }).toLongOrNull()?.grouped() ?: "Enter odometer"} $unit", style = MaterialTheme.typography.titleLarge)
        }
        TripIcon(AppIcon.Edit, "Edit $label")
    }
}

@Composable
private fun OdometerEditorDialog(title: String, value: String, placeholder: String, unit: String, onDismiss: () -> Unit, onApply: (String) -> Unit) {
    var working by remember(value, placeholder) { mutableStateOf(value) }
    val displayed = working.toLongOrNull() ?: placeholder.toLongOrNull() ?: 0L
    val digits = displayed.toString()
    val focusRequesters = remember(digits.length) { List(digits.length) { androidx.compose.ui.focus.FocusRequester() } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (unit.isBlank()) "Tap a digit to type, or use + and −." else "${unit} · Tap a digit to type, or use + and −.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                digits.forEachIndexed { index, digit ->
                    val place = Math.pow(10.0, (digits.length - index - 1).toDouble()).toLong()
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { working = (displayed + place).toString() }, modifier = Modifier.size(36.dp)) { Text("+", style = MaterialTheme.typography.titleMedium) }
                        OutlinedTextField(value = digit.toString(), onValueChange = { entered ->
                            val cleaned = entered.filter(Char::isDigit)
                            val next = if (cleaned.length > 1 && cleaned.startsWith(digit)) cleaned.drop(1) else cleaned
                            if (next.isNotEmpty()) {
                                val chars = digits.toCharArray()
                                next.forEachIndexed { offset, char -> if (index + offset < chars.size) chars[index + offset] = char }
                                working = chars.concatToString()
                                if (next.length == 1 && index < focusRequesters.lastIndex) focusRequesters[index + 1].requestFocus()
                            }
                        }, modifier = Modifier.width(42.dp).height(58.dp).focusRequester(focusRequesters[index]),
                            singleLine = true, textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = MaterialTheme.shapes.small)
                        IconButton(onClick = { working = (displayed - place).coerceAtLeast(0).toString() }, modifier = Modifier.size(36.dp)) { Text("−", style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { onApply(working) }) { Text("Apply") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
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
    OutlinedTextField(value, onValue, modifier = Modifier.fillMaxWidth(), label = { Text(if (hasTime) "Date and time" else "Date") },
        supportingText = { Text(if (hasTime) "yyyy-MM-dd HH:mm" else "yyyy-MM-dd") },
        isError = !valid, enabled = enabled, singleLine = true, shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
        trailingIcon = { IconButton(enabled = enabled, onClick = { showDate = true }) { TripIcon(AppIcon.History, "Choose date") } })
}
