package com.jacobgain.triprabbit.core.usecase

import androidx.room.withTransaction
import com.jacobgain.triprabbit.core.database.TripRabbitDatabase
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.repository.*
import com.jacobgain.triprabbit.core.validation.ReadingValidator
import java.time.Instant
import javax.inject.Inject

class CreateVehicleUseCase @Inject constructor(
    private val database: TripRabbitDatabase,
    private val vehicles: VehicleRepository,
    private val readings: OdometerRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(input: VehicleInput, initialValue: Long, recordedAt: Instant = Instant.now()): Long {
        require(input.name.isNotBlank()) { "Vehicle name is required." }
        ReadingValidator.validate(initialValue, null, null)?.let { throw it }
        val id = database.withTransaction {
            val vehicleId = vehicles.createVehicle(input, recordedAt)
            val initialDate = recordedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
            readings.addReading(vehicleId, initialValue, initialDate, null, recordedAt)
            vehicleId
        }
        settings.setSelectedVehicle(id)
        settings.setFirstLaunchComplete(true)
        return id
    }
}

class AddOdometerReadingUseCase @Inject constructor(private val vehicles: VehicleRepository, private val readings: OdometerRepository) {
    suspend operator fun invoke(vehicleId: Long, value: Long, recordedAt: Instant, note: String?, name: String? = null, hasTime: Boolean = false): Long {
        if (vehicles.getVehicle(vehicleId) == null) throw com.jacobgain.triprabbit.core.validation.ReadingError.VehicleMissing
        val (previous, next) = readings.surrounding(vehicleId, recordedAt)
        ReadingValidator.validate(value, previous, next)?.let { throw it }
        return readings.addReading(vehicleId, value, recordedAt, note?.trim()?.takeIf { it.isNotEmpty() }, Instant.now(), name?.trim()?.takeIf { it.isNotEmpty() }, hasTime)
    }
}

class EditOdometerReadingUseCase @Inject constructor(private val readings: OdometerRepository) {
    suspend operator fun invoke(id: Long, value: Long, recordedAt: Instant, note: String?, name: String? = null, hasTime: Boolean = false) {
        val current = readings.getReading(id) ?: throw com.jacobgain.triprabbit.core.validation.ReadingError.ReadingMissing
        val (previous, next) = readings.surrounding(current.vehicleId, recordedAt, id)
        ReadingValidator.validate(value, previous, next)?.let { throw it }
        readings.updateReading(current.copy(value = value, recordedAt = recordedAt, note = note?.trim()?.takeIf { it.isNotEmpty() }, name = name?.trim()?.takeIf { it.isNotEmpty() }, hasTime = hasTime, updatedAt = Instant.now()))
    }
}

class ResolveSelectedVehicleUseCase @Inject constructor(private val vehicles: VehicleRepository, private val settings: SettingsRepository) {
    suspend operator fun invoke(active: List<Vehicle>, selectedId: Long?): Long? {
        val resolved = resolveSelectedVehicle(active, selectedId)
        if (resolved != selectedId) settings.setSelectedVehicle(resolved)
        return resolved
    }
}

fun resolveSelectedVehicle(active: List<Vehicle>, selectedId: Long?): Long? =
    selectedId?.takeIf { id -> active.any { it.id == id } } ?: active.firstOrNull()?.id
