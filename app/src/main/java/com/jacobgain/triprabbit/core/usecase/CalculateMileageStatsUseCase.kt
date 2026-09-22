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
        val cutoffBase = sorted.lastOrNull { !it.recordedAt.isAfter(cutoff) } ?: sorted.first()
        val zone = ZoneId.systemDefault(); val year = now.atZone(zone).year
        val yearReadings = sorted.filter { it.recordedAt.atZone(zone).year == year }
        val yearDistance = if (yearReadings.size > 1) yearReadings.last().value - yearReadings.first().value else 0
        val days = max(1, ChronoUnit.DAYS.between(first.recordedAt, latest.recordedAt))
        val months = max(1.0, days / 30.4375)
        val total = latest.value - first.value
        return MileageStats(latest.value, total, latest.value - cutoffBase.value, yearDistance, sorted.size, (total / months).roundToLong())
    }
}
