package com.jacobgain.triprabbit.core.usecase

import com.jacobgain.triprabbit.core.model.MileageStats
import com.jacobgain.triprabbit.core.model.OdometerReading
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToLong

class CalculateMileageStatsUseCase @Inject constructor() {
    operator fun invoke(readings: List<OdometerReading>, now: Instant = Instant.now()): MileageStats {
        val sorted = readings.sortedWith(compareBy<OdometerReading> { it.recordedAt }.thenBy { it.id })
        if (sorted.isEmpty()) return MileageStats()
        val first = sorted.first(); val latest = sorted.last()
        val cutoff = now.minus(30, ChronoUnit.DAYS)
        val zone = ZoneId.systemDefault(); val year = now.atZone(zone).year
        val distances = sorted.mapIndexed { index, reading -> reading.id to (reading.startValue?.let { reading.value - it } ?: if (index == 0) 0L else reading.value - sorted[index - 1].value) }.toMap()
        val yearDistance = sorted.filter { it.recordedAt.atZone(zone).year == year }.sumOf { distances[it.id] ?: 0L }
        val days = max(1, ChronoUnit.DAYS.between(first.recordedAt, latest.recordedAt))
        val months = max(1.0, days / 30.4375)
        val total = distances.values.sum()
        val last30 = sorted.filter { !it.recordedAt.isBefore(cutoff) }.sumOf { distances[it.id] ?: 0L }
        return MileageStats(latest.value, total, last30, yearDistance, sorted.size, (total / months).roundToLong())
    }
}
