package com.jacobgain.triprabbit.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobgain.triprabbit.core.designsystem.component.*
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.util.*
import com.jacobgain.triprabbit.feature.vehicles.subtitle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(onAdd: (Long) -> Unit, onHistory: (Long) -> Unit, onManageVehicles: () -> Unit,
    onStatistics: (Long) -> Unit, viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(state, onAdd, onHistory, onManageVehicles, onStatistics, viewModel::selectVehicle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(state: DashboardUiState, onAdd: (Long) -> Unit = {}, onHistory: (Long) -> Unit = {},
    onManageVehicles: () -> Unit = {}, onStatistics: (Long) -> Unit = {}, onSelect: (Long) -> Unit = {}) {
    val vehicle = state.vehicle
    var selectorOpen by remember { mutableStateOf(false) }
    if (selectorOpen) ModalBottomSheet(onDismissRequest = { selectorOpen = false }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Select vehicle", "Your mileage stays separate for each vehicle.")
            state.vehicles.forEach { v ->
                Surface(onClick = { onSelect(v.id); selectorOpen = false }, shape = MaterialTheme.shapes.medium,
                    color = if (v.id == vehicle?.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TripIcon(AppIcon.Car)
                        Column(Modifier.weight(1f)) { Text(v.name, style = MaterialTheme.typography.titleMedium); v.subtitle()?.let { Text(it, style = MaterialTheme.typography.bodySmall) } }
                        if (v.id == vehicle?.id) TripIcon(AppIcon.Check, "Current vehicle")
                    }
                }
            }
            TextButton(onClick = { selectorOpen = false; onManageVehicles() }) { TripIcon(AppIcon.Settings); Spacer(Modifier.width(8.dp)); Text("Manage vehicles") }
        }
    }
    if (state.loading) { LoadingState(); return }
    if (vehicle == null) { EmptyState("Make room for your miles", "Add a vehicle to start a private, organised mileage history.", "Add Vehicle", onManageVehicles); return }
    val latest = state.readings.firstOrNull()
    val sinceLast = latest?.value?.minus(state.readings.getOrNull(1)?.value ?: latest.value) ?: 0
    val unit = vehicle.odometerUnit.abbreviation
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrandHeader(Modifier.weight(1f))
                FilledTonalIconButton(onClick = onManageVehicles, shape = MaterialTheme.shapes.medium) { TripIcon(AppIcon.Car, "Vehicles") }
            }
        }
        item { PageHeading("A little more clarity.", "Your mileage, all in one place.") }
        item {
            Surface(onClick = { selectorOpen = true }, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(AppIcon.Car, accented = true)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("CURRENT VEHICLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(vehicle.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TripIcon(AppIcon.Down, "Select vehicle")
                }
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("CURRENT ODOMETER", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f), modifier = Modifier.weight(1f))
                        TripIcon(AppIcon.Gauge, tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f))
                    }
                    if (latest != null) OdometerDisplay(latest.value, vehicle.odometerUnit) else Text("Ready for your first reading", style = MaterialTheme.typography.headlineSmall)
                    if (latest != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TripIcon(AppIcon.Clock, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f))
                        Text("Updated ${latest.recordedAt.displayDate()}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f))
                    }
                    Button(onClick = { onAdd(vehicle.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary)) {
                        TripIcon(AppIcon.Add); Spacer(Modifier.width(8.dp)); Text("Add Reading", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        item { AdaptivePair(
            first = { MetricTile("Last 30 days", "${state.stats.last30Days.grouped()} $unit", it, icon = AppIcon.History) },
            second = { MetricTile("Total tracked", "${state.stats.totalTracked.grouped()} $unit", it, icon = AppIcon.Distance) },
        ) }
        item { BrandArtworkSlot(BrandArtwork.dashboard) }
        item { SectionCard {
            SectionTitle("Your week", "Distance recorded over the last 7 days", "Reports") { onStatistics(vehicle.id) }
            WeeklyMileageChart(state.readings, vehicle.odometerUnit)
        } }
        item { SectionCard {
            SectionTitle("Recent readings", "${state.stats.readingCount} readings in your history", "View History") { onHistory(vehicle.id) }
            if (state.readings.isEmpty()) Text("Your next chapter starts with a reading.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.readings.take(3).forEachIndexed { index, reading ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(AppIcon.Gauge)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${reading.value.grouped()} $unit", style = MaterialTheme.typography.titleMedium)
                        Text(reading.recordedAt.displayDate(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (index == 0 && state.readings.size > 1) StatusPill("+${sinceLast.grouped()} $unit")
                }
            }
        } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                TripIcon(AppIcon.Shield, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text("Just your miles. Just on your device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun WeeklyMileageChart(readings: List<OdometerReading>, unit: DistanceUnit) {
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val distances = readings.sortedWith(compareBy<OdometerReading> { it.recordedAt }.thenBy { it.id }).zipWithNext()
        .groupBy { (_, end) -> end.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() }
        .mapValues { (_, intervals) -> intervals.sumOf { (start, end) -> end.value - start.value } }
    val max = days.maxOf { distances[it] ?: 0 }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
        days.forEach { day ->
            val value = distances[day] ?: 0
            Column(Modifier.weight(1f).semantics { contentDescription = "$day: $value ${unit.abbreviation}" }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(104.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(6.dp)), contentAlignment = Alignment.BottomCenter) {
                    if (value > 0) Box(Modifier.fillMaxWidth().height((104f * value / max).coerceAtLeast(6f).dp)
                        .background(if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = .45f), RoundedCornerShape(6.dp)))
                }
                Spacer(Modifier.height(10.dp))
                Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelMedium,
                    color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${days.sumOf { distances[it] ?: 0 }.grouped()} ${unit.abbreviation} logged", style = MaterialTheme.typography.labelLarge)
        Text("Today · ${distances[today]?.grouped() ?: "0"} ${unit.abbreviation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MileageHistoryChart(readings: List<OdometerReading>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val sorted = readings.sortedWith(compareBy<OdometerReading> { it.recordedAt }.thenBy { it.id })
    val min = sorted.minOfOrNull { it.value } ?: 0
    val range = (sorted.maxOfOrNull { it.value } ?: min) - min
    Canvas(modifier.semantics { contentDescription = "Odometer history, ${readings.size} readings" }) {
        if (sorted.size < 2) return@Canvas
        val inset = 4.dp.toPx()
        val height = size.height - inset * 2
        val width = size.width - inset * 2
        repeat(4) { row -> val y = inset + height * row / 3; drawLine(grid, Offset(inset, y), Offset(size.width - inset, y), 1.dp.toPx()) }
        val path = Path()
        val firstTime = sorted.first().recordedAt.toEpochMilli()
        val timeRange = (sorted.last().recordedAt.toEpochMilli() - firstTime).coerceAtLeast(1)
        val points = sorted.map { r ->
            Offset(inset + width * ((r.recordedAt.toEpochMilli() - firstTime).toDouble() / timeRange).toFloat(),
                inset + height - if (range == 0L) height / 2 else height * ((r.value - min).toDouble() / range).toFloat())
        }
        points.forEachIndexed { index, point -> if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y) }
        val fill = Path().apply { addPath(path); lineTo(points.last().x, size.height - inset); lineTo(points.first().x, size.height - inset); close() }
        drawPath(fill, color.copy(alpha = .08f))
        drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { drawCircle(color, 3.dp.toPx(), it) }
    }
}
