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
            val vehicleId = vehicles.createVehicle(input.copy(name = input.name.trim().take(VEHICLE_NAME_MAX_LENGTH)), recordedAt)
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

class AddOdometerReadingUseCase @Inject constructor(
    private val database: TripRabbitDatabase,
    private val vehicles: VehicleRepository,
    private val readings: OdometerRepository,
) {
    suspend operator fun invoke(vehicleId: Long, startValue: Long, value: Long, recordedAt: Instant, note: String?, name: String? = null, hasTime: Boolean = false, inProgress: Boolean = false): Long {
        require(!name.isNullOrBlank()) { "Trip name is required." }
        return database.withTransaction {
            if (vehicles.getVehicle(vehicleId) == null) throw com.jacobgain.triprabbit.core.validation.ReadingError.VehicleMissing
            if (readings.getInProgressReading(vehicleId) != null) {
                throw com.jacobgain.triprabbit.core.validation.ReadingError.TripAlreadyInProgress
            }
            val (previous, next) = readings.surrounding(vehicleId, recordedAt)
            require(startValue >= 0) { "Starting odometer cannot be negative." }
            require(value >= startValue) { "Finish odometer must be at least the start odometer." }
            require(previous == null || startValue >= previous.value) { "Start odometer is lower than the previous reading of ${previous?.value}." }
            ReadingValidator.validate(value, previous, next)?.let { throw it }
            readings.addReading(vehicleId, value, recordedAt, note?.trim()?.takeIf { it.isNotEmpty() }, Instant.now(), name.trim(), hasTime, startValue, inProgress)
        }
    }
}

class EditOdometerReadingUseCase @Inject constructor(private val readings: OdometerRepository) {
    suspend operator fun invoke(id: Long, value: Long, recordedAt: Instant, note: String?, name: String? = null, hasTime: Boolean = false, startValue: Long? = null, inProgress: Boolean = false) {
        require(!name.isNullOrBlank()) { "Trip name is required." }
        val current = readings.getReading(id) ?: throw com.jacobgain.triprabbit.core.validation.ReadingError.ReadingMissing
        if (inProgress && readings.getInProgressReading(current.vehicleId, excludingId = id) != null)
            throw com.jacobgain.triprabbit.core.validation.ReadingError.TripAlreadyInProgress
        val (previous, next) = readings.surrounding(current.vehicleId, recordedAt, id)
        val start = startValue ?: current.startValue ?: previous?.value ?: value
        require(start >= 0) { "Starting odometer cannot be negative." }
        require(previous == null || start >= previous.value) { "Start odometer is lower than the previous reading of ${previous?.value}." }
        val finish = if (inProgress) start else value
        if (!inProgress) require(finish >= start) { "Finish odometer must be at least the start odometer." }
        ReadingValidator.validate(finish, previous, next)?.let { throw it }
        readings.updateReading(current.copy(inProgress = inProgress, value = finish, startValue = start, recordedAt = recordedAt, note = note?.trim()?.takeIf { it.isNotEmpty() }, name = name.trim(), hasTime = hasTime, updatedAt = Instant.now()))
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
