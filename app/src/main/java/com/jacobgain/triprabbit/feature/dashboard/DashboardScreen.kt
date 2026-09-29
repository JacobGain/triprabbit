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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(onAdd: (Long) -> Unit, onHistory: (Long) -> Unit, onManageVehicles: () -> Unit,
    onStatistics: (Long) -> Unit, viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(state, onAdd, onHistory, onManageVehicles, onStatistics)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(state: DashboardUiState, onAdd: (Long) -> Unit = {}, onHistory: (Long) -> Unit = {},
    onManageVehicles: () -> Unit = {}, onStatistics: (Long) -> Unit = {}) {
    val vehicle = state.vehicle
    if (state.loading) { LoadingState(); return }
    if (vehicle == null) { EmptyState("No vehicle selected", "Add a vehicle to start logging mileage.", "Add vehicle", onManageVehicles); return }
    val latest = state.readings.firstOrNull()
    val sinceLast = latest?.let { it.value - (it.startValue ?: state.readings.getOrNull(1)?.value ?: it.value) } ?: 0
    val unit = vehicle.odometerUnit.abbreviation
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            AdaptivePair(
                first = { BrandHeader(it) },
                second = { FilledTonalButton(onClick = onManageVehicles, modifier = it, shape = MaterialTheme.shapes.medium) { TripIcon(AppIcon.Garage); Spacer(Modifier.width(6.dp)); Text("Garage") } },
            )
        }
        item {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(AppIcon.Car, accented = true)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("VEHICLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(vehicle.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Current odometer")
                if (latest != null) OdometerDisplay(latest.value, vehicle.odometerUnit) else Text("Ready for your first reading", style = MaterialTheme.typography.headlineSmall)
                if (latest != null) Text("Updated ${latest.recordedAt.displayDate()}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryAction("Add Trip", { onAdd(vehicle.id) }, icon = AppIcon.Add)
            }
        }
        item { BrandArtworkSlot(BrandArtwork.dashboard) }
        item { AdaptivePair(
            first = { MetricTile("Last 30 days", "${state.stats.last30Days.grouped()} $unit", it) },
            second = { MetricTile("Total tracked", "${state.stats.totalTracked.grouped()} $unit", it) },
        ) }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionTitle("Your week", "Distance recorded over the last 7 days", "Reports") { onStatistics(vehicle.id) }
            WeeklyMileageChart(state.readings, vehicle.odometerUnit)
        } }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionTitle("Recent trips", "${state.stats.readingCount} trips in your history", "View Trips") { onHistory(vehicle.id) }
            if (state.readings.isEmpty()) Text("No recent readings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.readings.take(3).forEachIndexed { index, reading ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(AppIcon.Gauge)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        reading.name?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
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
                Text("Data securely saved on your device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun WeeklyMileageChart(readings: List<OdometerReading>, unit: DistanceUnit) {
    val today = LocalDate.now()
    val sunday = today.minusDays((today.dayOfWeek.value % 7).toLong())
    val days = (0..6).map { sunday.plusDays(it.toLong()) }
    val ordered = readings.filterNot { it.inProgress }.sortedWith(compareBy<OdometerReading> { it.recordedAt }.thenBy { it.id })
    val distances = ordered.mapIndexedNotNull { index, reading ->
        val distance = reading.startValue?.let { reading.value - it } ?: if (index == 0) null else reading.value - ordered[index - 1].value
        distance?.let { reading.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate() to it }
    }.groupBy({ it.first }, { it.second }).mapValues { (_, values) -> values.sum() }
    val max = days.maxOf { distances[it] ?: 0 }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
        days.forEach { day ->
            val value = distances[day] ?: 0
            Column(Modifier.weight(1f).semantics { contentDescription = "$day: $value ${unit.abbreviation}" }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${value.grouped()} ${unit.abbreviation}", style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    color = if (value > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(84.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(6.dp)), contentAlignment = Alignment.BottomCenter) {
                    if (value > 0) Box(Modifier.fillMaxWidth().height((84f * value / max).coerceAtLeast(6f).dp)
                        .background(if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = .45f), RoundedCornerShape(6.dp)))
                }
                Spacer(Modifier.height(10.dp))
                Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall,
                    maxLines = 1, color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    AdaptivePair(
        first = { Text("${days.sumOf { distances[it] ?: 0 }.grouped()} ${unit.abbreviation} tracked this week", modifier = it, style = MaterialTheme.typography.labelLarge) },
        second = { Text("Today · ${distances[today]?.grouped() ?: "0"} ${unit.abbreviation}", modifier = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
    )
}

@Composable
fun MonthlyDistanceChart(readings: List<OdometerReading>, unit: DistanceUnit) {
    val zone = ZoneId.systemDefault()
    val currentMonth = YearMonth.now()
    val months = (5 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
    val ordered = readings.filterNot { it.inProgress }
        .sortedWith(compareBy<OdometerReading> { it.recordedAt }.thenBy { it.id })
    val distances = mutableMapOf<YearMonth, Long>()
    ordered.forEachIndexed { index, reading ->
        val previous = ordered.getOrNull(index - 1)
        val distance = reading.startValue?.let { reading.value - it }
            ?: previous?.let { reading.value - it.value }
        if (distance != null && distance >= 0) {
            val month = YearMonth.from(reading.recordedAt.atZone(zone))
            distances[month] = (distances[month] ?: 0L) + distance
        }
    }
    val values = months.map { it to (distances[it] ?: 0L) }
    val maximum = values.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    Row(
        Modifier.fillMaxWidth().semantics {
            contentDescription = values.joinToString { (month, distance) -> "${month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${month.year}: $distance ${unit.abbreviation}" }
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { (month, distance) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${distance.grouped()} ${unit.abbreviation}", style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    color = if (distance > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(88.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.BottomCenter) {
                    if (distance > 0) Box(Modifier.fillMaxWidth().height((88f * distance.toFloat() / maximum.toFloat()).coerceAtLeast(5f).dp)
                        .background(if (month == currentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = .48f), RoundedCornerShape(6.dp)))
                }
                Spacer(Modifier.height(6.dp))
                Text(month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall,
                    color = if (month == currentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1)
            }
        }
    }
}

@Composable
fun MileageHistoryChart(readings: List<OdometerReading>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val sorted = readings.filterNot { it.inProgress }.sortedWith(compareBy<OdometerReading> { it.recordedAt }.thenBy { it.id })
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
