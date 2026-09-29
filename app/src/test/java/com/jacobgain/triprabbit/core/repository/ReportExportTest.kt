package com.jacobgain.triprabbit.core.repository

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jacobgain.triprabbit.core.database.toDomain
import com.jacobgain.triprabbit.core.database.TripRabbitDatabase
import com.jacobgain.triprabbit.core.database.entity.OdometerReadingEntity
import com.jacobgain.triprabbit.core.database.entity.VehicleEntity
import com.jacobgain.triprabbit.core.model.DistanceUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportExportTest {
    @Test fun exportsRangedCsvWithTripDetails() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TripRabbitDatabase::class.java).build()
        val directory = File(context.cacheDir, "report-export-test").apply { mkdirs() }
        try {
            db.vehicleDao().insert(VehicleEntity(1, "Golf", null, null, null, null, DistanceUnit.KILOMETERS, null, null, Instant.EPOCH, null))
            db.odometerReadingDao().insertAll(listOf(
                OdometerReadingEntity(1, 1, 1000, Instant.parse("2026-09-01T12:00:00Z"), null, Instant.EPOCH, null),
                OdometerReadingEntity(2, 1, 1250, Instant.parse("2026-09-21T12:00:00Z"), "Site visit", Instant.EPOCH, null, "123 Main Street"),
            ))
            val repository = DataRepositoryImpl(context, db)
            val from = LocalDate.parse("2026-09-20")
            val through = LocalDate.parse("2026-09-22")
            val csv = File(directory, "report.csv")
            repository.exportVehicleReport(Uri.fromFile(csv), 1, from, through, false)
            assertTrue(csv.readText().contains("123 Main Street"))
            assertTrue(csv.readText().contains("Site visit"))
            assertTrue(csv.readText().contains("\"250\""))
            assertTrue(csv.readText().contains("1000")) // The trip starts at the previous odometer.
            assertFalse(csv.readText().contains("2026-09-01")) // The baseline is not an exported trip.
        } finally {
            db.close()
            directory.deleteRecursively()
        }
    }
    @Test fun pendingTripsRoundTripThroughBackupButStayOutOfReports() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TripRabbitDatabase::class.java).build()
        val directory = File(context.cacheDir, "pending-test").apply { mkdirs() }
        try {
            db.vehicleDao().insert(VehicleEntity(1, "Golf", null, null, null, null, DistanceUnit.KILOMETERS, null, null, Instant.EPOCH, null))
            db.odometerReadingDao().insertAll(listOf(
                OdometerReadingEntity(1, 1, 1000, Instant.parse("2026-09-01T12:00:00Z"), null, Instant.EPOCH, null),
                OdometerReadingEntity(2, 1, 1200, Instant.parse("2026-09-02T12:00:00Z"), null, Instant.EPOCH, null, "Pending trip", startValue = 1200, inProgress = true),
            ))
            val repository = DataRepositoryImpl(context, db)
            val backup = File(directory, "backup.json")
            repository.exportBackup(Uri.fromFile(backup))
            repository.restoreBackup(Uri.fromFile(backup))
            val stats = com.jacobgain.triprabbit.core.usecase.CalculateMileageStatsUseCase()(db.odometerReadingDao().getAll().map { it.toDomain() })
            assertEquals(0L, stats.totalTracked)
            assertEquals(1, stats.readingCount)
            assertEquals(1000L, stats.current)
            assertTrue(db.odometerReadingDao().getById(2)!!.inProgress)
            assertEquals(1200L, db.odometerReadingDao().getById(2)!!.startValue)
            val csv = File(directory, "report.csv")
            repository.exportVehicleReport(Uri.fromFile(csv), 1, LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-03"), false)
            assertFalse(csv.readText().contains("Pending trip"))

        } finally { db.close(); directory.deleteRecursively() }
    }

    @Test fun exportsSelectedVehiclesInTheChosenUnit() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TripRabbitDatabase::class.java).build()
        val directory = File(context.cacheDir, "fleet-report-export-test").apply { mkdirs() }
        try {
            db.vehicleDao().insertAll(listOf(
                VehicleEntity(1, "Golf", null, null, null, null, DistanceUnit.KILOMETERS, null, null, Instant.EPOCH, null),
                VehicleEntity(2, "Work truck", null, null, null, null, DistanceUnit.MILES, null, null, Instant.EPOCH, null),
            ))
            db.odometerReadingDao().insertAll(listOf(
                OdometerReadingEntity(1, 1, 1000, Instant.parse("2026-09-01T12:00:00Z"), null, Instant.EPOCH, null),
                OdometerReadingEntity(2, 1, 1250, Instant.parse("2026-09-02T12:00:00Z"), null, Instant.EPOCH, null, name = "Commute", startValue = 1000),
                OdometerReadingEntity(3, 2, 100, Instant.parse("2026-09-01T12:00:00Z"), null, Instant.EPOCH, null),
                OdometerReadingEntity(4, 2, 120, Instant.parse("2026-09-02T12:00:00Z"), null, Instant.EPOCH, null, name = "Site visit", startValue = 100),
            ))
            val repository = DataRepositoryImpl(context, db)
            val from = LocalDate.parse("2026-09-01")
            val through = LocalDate.parse("2026-09-03")
            val kilometers = File(directory, "all-vehicles-km.csv")
            repository.exportVehiclesReport(Uri.fromFile(kilometers), listOf(1, 2), from, through, DistanceUnit.KILOMETERS, true, false)
            val kmContents = kilometers.readText()
            assertTrue(kmContents.contains("\"Golf\",\"2026-09-02\",\"Commute\""))
            assertTrue(kmContents.contains("\"Work truck\",\"2026-09-02\",\"Site visit\""))
            assertTrue(kmContents.contains("\"km\",\"250\""))
            assertTrue(kmContents.contains("\"km\",\"32\""))
            assertTrue(kmContents.contains("\"161\",\"193\",\"km\",\"32\""))

            val miles = File(directory, "all-vehicles-mi.csv")
            repository.exportVehiclesReport(Uri.fromFile(miles), listOf(1, 2), from, through, DistanceUnit.MILES, true, false)
            val miContents = miles.readText()
            assertTrue(miContents.contains("\"mi\",\"155\""))
            assertTrue(miContents.contains("\"mi\",\"20\""))
            assertTrue(miContents.contains("\"621\",\"777\",\"mi\",\"155\""))

        } finally {
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun pdfReportUsesMonthlyTripRowsInsteadOfGroupingByTripName() {
        val august = PdfReportTrip("Work truck", LocalDate.parse("2026-08-12"), "Commute", "Fuel receipt", 100, 120, 20)
        val septemberFirst = PdfReportTrip("Daily driver", LocalDate.parse("2026-09-02"), "Commute", "Parking", 500, 513, 13)
        val septemberSecond = PdfReportTrip("Daily driver", LocalDate.parse("2026-09-14"), "Commute", "Groceries", 513, 526, 13)
        val trips = listOf(septemberSecond, august, septemberFirst)

        assertEquals("TripRabbit Report - Daily driver", pdfReportTitle("Daily driver"))
        assertEquals("TripRabbit Report - All Vehicles", pdfReportTitle("All Vehicles"))
        assertEquals("3 trips | 46 km recorded", pdfReportSummary(trips, "km"))
        assertEquals("2026-08-12  |  Commute  |  20 km", pdfReportFirstRow(august, "km", allVehicles = false))
        assertEquals("Work truck  |  2026-08-12  |  Commute  |  20 km", pdfReportFirstRow(august, "km", allVehicles = true))
        assertEquals("Note: Fuel receipt  |  Odometer 100–120 km", pdfReportSecondRow(august, "km"))
        assertEquals(listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9)), groupPdfTripsByMonth(trips).keys.toList())
        assertEquals(listOf(septemberFirst, septemberSecond), groupPdfTripsByMonth(trips).getValue(YearMonth.of(2026, 9)))
    }

}
