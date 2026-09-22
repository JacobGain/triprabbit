package com.jacobgain.triprabbit.core.usecase

import com.jacobgain.triprabbit.core.model.OdometerReading
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class CalculateMileageStatsUseCaseTest {
    private val calculate=CalculateMileageStatsUseCase()
    private fun reading(id:Long,value:Long,at:Instant)=OdometerReading(id,1,value,at,null,at,null)
    @Test fun `empty history has zero stats`(){assertEquals(0,calculate(emptyList()).readingCount)}
    @Test fun `single reading has current but no tracked distance`(){val s=calculate(listOf(reading(1,100,Instant.EPOCH)),Instant.EPOCH.plus(1,ChronoUnit.DAYS));assertEquals(100L,s.current);assertEquals(0L,s.totalTracked)}
    @Test fun `distance across readings is latest minus earliest`(){val now=Instant.parse("2026-09-21T12:00:00Z");val s=calculate(listOf(reading(1,100,now.minus(40,ChronoUnit.DAYS)),reading(2,250,now.minus(10,ChronoUnit.DAYS)),reading(3,300,now)),now);assertEquals(200L,s.totalTracked);assertEquals(200L,s.last30Days);assertEquals(3,s.readingCount)}
    @Test fun `two readings calculate distance`(){val now=Instant.parse("2026-09-21T12:00:00Z");assertEquals(350L,calculate(listOf(reading(1,120000,now.minus(1,ChronoUnit.DAYS)),reading(2,120350,now)),now).totalTracked)}
    @Test fun `current year excludes prior year distance`(){val now=Instant.parse("2026-09-21T12:00:00Z");val s=calculate(listOf(reading(1,100,Instant.parse("2025-12-01T12:00:00Z")),reading(2,150,Instant.parse("2026-01-10T12:00:00Z")),reading(3,240,now)),now);assertEquals(90L,s.currentYear)}
}
