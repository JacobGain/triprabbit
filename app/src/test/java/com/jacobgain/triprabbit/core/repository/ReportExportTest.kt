package com.jacobgain.triprabbit.core.repository

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
            assertFalse(csv.readText().contains("1000"))
        } finally {
            db.close()
            directory.deleteRecursively()
        }
    }
}
