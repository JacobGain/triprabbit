package com.jacobgain.triprabbit.core.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jacobgain.triprabbit.core.database.TripRabbitDatabase
import com.jacobgain.triprabbit.core.database.entity.OdometerReadingEntity
import com.jacobgain.triprabbit.core.database.entity.VehicleEntity
import com.jacobgain.triprabbit.core.model.DistanceUnit
import com.jacobgain.triprabbit.core.repository.OdometerRepositoryImpl
import com.jacobgain.triprabbit.core.repository.VehicleRepositoryImpl
import com.jacobgain.triprabbit.core.validation.ReadingError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddOdometerReadingUseCaseTest {
    @Test fun inProgressTripBlocksOnlyItsOwnVehicle() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TripRabbitDatabase::class.java).build()
        try {
            db.vehicleDao().insertAll(listOf(
                VehicleEntity(1, "Vehicle A", null, null, null, null, DistanceUnit.KILOMETERS, null, null, Instant.EPOCH, null),
                VehicleEntity(2, "Vehicle B", null, null, null, null, DistanceUnit.KILOMETERS, null, null, Instant.EPOCH, null),
            ))
            db.odometerReadingDao().insertAll(listOf(
                OdometerReadingEntity(1, 1, 100, Instant.parse("2026-09-01T12:00:00Z"), null, Instant.EPOCH, null),
                OdometerReadingEntity(2, 2, 200, Instant.parse("2026-09-01T12:00:00Z"), null, Instant.EPOCH, null),
            ))
            val vehicles = VehicleRepositoryImpl(db.vehicleDao())
            val readings = OdometerRepositoryImpl(db.odometerReadingDao())
            val add = AddOdometerReadingUseCase(db, vehicles, readings)
            val dayTwo = Instant.parse("2026-09-02T12:00:00Z")

            add(1, 100, 100, dayTwo, null, "A in progress", inProgress = true)

            assertTrue(runCatching { add(1, 100, 110, dayTwo, null, "A completed") }.exceptionOrNull() is ReadingError.TripAlreadyInProgress)
            assertTrue(runCatching { add(1, 100, 100, dayTwo, null, "Another A trip", inProgress = true) }.exceptionOrNull() is ReadingError.TripAlreadyInProgress)

            add(2, 200, 220, dayTwo, null, "B completed")
            add(2, 220, 220, Instant.parse("2026-09-03T12:00:00Z"), null, "B in progress", inProgress = true)
        } finally {
            db.close()
        }
        Unit
    }
}
