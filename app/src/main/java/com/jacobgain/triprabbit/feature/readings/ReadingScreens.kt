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
    AddReadingContent(state, onBack, viewModel::valueChanged, viewModel::dateChanged, viewModel::noteChanged, { viewModel.save() })
}

@Composable
fun AddReadingContent(state: AddReadingUiState, onBack: () -> Unit = {}, onValue: (String) -> Unit = {},
    onDate: (Instant) -> Unit = {}, onNote: (String) -> Unit = {}, onSave: () -> Unit = {}) {
    var dateText by rememberSaveable { mutableStateOf(state.recordedAt.inputDateTime()) }
    val dateValid = dateText.parseDateTime() != null
    Scaffold(topBar = { DetailTopBar("Add Reading", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            PageHeading("Keep the story going.", "Log your odometer. We’ll do the maths.")
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
                SectionTitle("Odometer", "Enter the total shown on your dashboard.")
                FormField(state.value, onValue, "New reading", suffix = state.vehicle?.odometerUnit?.abbreviation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = MaterialTheme.typography.headlineMedium,
                    enabled = !state.saving)
                val entered = state.value.toLongOrNull()
                val previous = state.previous
                if (entered != null && previous != null && entered >= previous.value)
                    InlineMessage("+${(entered - previous.value).grouped()} ${state.vehicle?.odometerUnit?.abbreviation} since your previous reading")
            }
            SectionCard {
                SectionTitle("Reading details")
                ReadingDateTimeField(dateText, { dateText = it; it.parseDateTime()?.let(onDate) }, enabled = !state.saving)
                FormField(state.note, onNote, "Note (optional)", hint = "A little context for future you.", singleLine = false, enabled = !state.saving)
            }
            state.error?.let { InlineMessage(it, error = true) }
            PrimaryAction("Save Reading", onSave, icon = AppIcon.Check, enabled = dateValid && state.vehicle != null, busy = state.saving)
            Text("Saved privately on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun ReadingHistoryScreen(onBack: (() -> Unit)?, onAdd: (Long) -> Unit, onEdit: (Long) -> Unit, viewModel: ReadingHistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReadingHistoryContent(state, onBack, onAdd, onEdit)
}

@Composable
fun ReadingHistoryContent(state: ReadingHistoryUiState, onBack: (() -> Unit)? = null, onAdd: (Long) -> Unit = {}, onEdit: (Long) -> Unit = {}) {
    var filter by rememberSaveable { mutableStateOf("All Readings") }
    var query by rememberSaveable { mutableStateOf("") }
    val thisMonth = YearMonth.now()
    val filtered = state.items.filter { item ->
        val matchesFilter = when (filter) {
            "This Month" -> YearMonth.from(item.reading.recordedAt.atZone(ZoneId.systemDefault())) == thisMonth
            "With Notes" -> !item.reading.note.isNullOrBlank()
            else -> true
        }
        matchesFilter && (query.isBlank() || listOf(item.reading.value.toString(), item.reading.value.grouped(),
            item.reading.note.orEmpty(), item.reading.recordedAt.displayDate()).any { it.contains(query, ignoreCase = true) })
    }
    val unit = state.vehicle?.odometerUnit?.abbreviation.orEmpty()
    Scaffold(topBar = { if (onBack != null) DetailTopBar("History", onBack) },
        floatingActionButton = { state.vehicle?.let { vehicle ->
            ExtendedFloatingActionButton(onClick = { onAdd(vehicle.id) }, containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary, shape = MaterialTheme.shapes.medium,
                icon = { TripIcon(AppIcon.Add) }, text = { Text("Add Reading") })
        } }) { padding ->
        if (state.loading) { LoadingState(Modifier.padding(padding)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { PageHeading("History", state.vehicle?.let { "${it.name} History" } ?: "Every reading, in order.") }
            item {
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search readings or notes") },
                    leadingIcon = { TripIcon(AppIcon.Search) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { TripIcon(AppIcon.Close, "Clear search") } },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface))
            }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All Readings", "This Month", "With Notes").forEach { label ->
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
                            Text("Recorded distance · ${filtered.size} readings", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (filtered.isEmpty()) item {
                EmptyState(if (state.items.isEmpty()) "A fresh start" else "No matching readings",
                    if (state.items.isEmpty()) "Your readings will appear here as you log your mileage." else "Try a different search or switch to All Readings.")
            }
            itemsIndexed(filtered, key = { _, item -> item.reading.id }) { index, item ->
                if (index == 0 || filtered[index - 1].reading.recordedAt.displayMonth() != item.reading.recordedAt.displayMonth()) {
                    Text(item.reading.recordedAt.displayMonth(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                }
                Surface(onClick = { onEdit(item.reading.id) }, color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconBadge(AppIcon.Gauge)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${item.reading.value.grouped()} $unit", style = MaterialTheme.typography.titleMedium)
                                Text(item.reading.recordedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d · h:mm a")),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TripIcon(AppIcon.Chevron, "Edit reading", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item.difference?.let { StatusPill("+${it.grouped()} $unit", icon = AppIcon.Distance) }
                        item.reading.note?.takeIf { it.isNotBlank() }?.let { note ->
                            Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
    EditReadingContent(state, onBack, viewModel::valueChanged, viewModel::dateChanged, viewModel::noteChanged, { viewModel.save() }, { viewModel.delete() })
}

@Composable
fun EditReadingContent(state: EditReadingUiState, onBack: () -> Unit = {}, onValue: (String) -> Unit = {}, onDate: (Instant) -> Unit = {},
    onNote: (String) -> Unit = {}, onSave: () -> Unit = {}, onDelete: () -> Unit = {}) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var dateText by rememberSaveable(state.reading?.id) { mutableStateOf(state.recordedAt.inputDateTime()) }
    if (confirmDelete) DestructiveConfirmationDialog("Delete this reading?", "This removes the reading from your history and recalculates your mileage. This cannot be undone.",
        { confirmDelete = false; onDelete() }, { confirmDelete = false })
    Scaffold(topBar = { DetailTopBar("Edit Reading", onBack) }) { padding ->
        if (state.loading) LoadingState(Modifier.padding(padding))
        else if (state.reading == null) Box(Modifier.padding(padding)) { EmptyState("Reading not found", "It may have already been deleted.", "Go Back", onBack) }
        else Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            PageHeading("The details matter.", "Update your reading and keep your history accurate.")
            SectionCard {
                SectionTitle("Odometer reading")
                FormField(state.value, onValue, "Odometer reading", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.headlineMedium, enabled = !state.saving)
                ReadingDateTimeField(dateText, { dateText = it; it.parseDateTime()?.let(onDate) }, enabled = !state.saving)
                FormField(state.note, onNote, "Note (optional)", singleLine = false, enabled = !state.saving)
            }
            state.error?.let { InlineMessage(it, error = true) }
            PrimaryAction("Save Changes", onSave, icon = AppIcon.Check, enabled = dateText.parseDateTime() != null, busy = state.saving)
            SectionCard {
                ActionRow("Delete Reading", "Permanently remove this entry.", AppIcon.Trash,
                    onClick = { if (state.confirmDeletion) confirmDelete = true else onDelete() }, enabled = !state.saving, destructive = true)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingDateTimeField(value: String, onValue: (String) -> Unit, enabled: Boolean) {
    val context = LocalContext.current
    val valid = value.parseDateTime() != null
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    val current = (value.parseDateTime() ?: Instant.now()).atZone(ZoneId.systemDefault())
    if (showDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
            TextButton(enabled = picker.selectedDateMillis != null, onClick = {
                selectedDate = Instant.ofEpochMilli(checkNotNull(picker.selectedDateMillis)).atZone(ZoneOffset.UTC).toLocalDate()
                showDate = false; showTime = true
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
    OutlinedTextField(value, onValue, modifier = Modifier.fillMaxWidth(), label = { Text("Date and time") },
        supportingText = { Text(if (valid) "yyyy-MM-dd HH:mm" else "Enter a valid date and time: yyyy-MM-dd HH:mm") },
        isError = !valid, enabled = enabled, singleLine = true, shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
        trailingIcon = { IconButton(enabled = enabled, onClick = { showDate = true }) { TripIcon(AppIcon.History, "Choose date and time") } })
}
