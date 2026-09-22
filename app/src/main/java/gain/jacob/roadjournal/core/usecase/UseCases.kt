package gain.jacob.roadjournal.core.usecase

import androidx.room.withTransaction
import gain.jacob.roadjournal.core.database.RoadJournalDatabase
import gain.jacob.roadjournal.core.model.*
import gain.jacob.roadjournal.core.repository.*
import gain.jacob.roadjournal.core.validation.ReadingValidator
import java.time.Instant
import javax.inject.Inject

class CreateVehicleUseCase @Inject constructor(
    private val database: RoadJournalDatabase,
    private val vehicles: VehicleRepository,
    private val readings: OdometerRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(input: VehicleInput, initialValue: Long, recordedAt: Instant = Instant.now()): Long {
        require(input.name.isNotBlank()) { "Vehicle name is required." }
        ReadingValidator.validate(initialValue, null, null)?.let { throw it }
        val id = database.withTransaction {
            val vehicleId = vehicles.createVehicle(input, recordedAt)
            readings.addReading(vehicleId, initialValue, recordedAt, null, recordedAt)
            vehicleId
        }
        settings.setSelectedVehicle(id)
        settings.setFirstLaunchComplete(true)
        return id
    }
}

class AddOdometerReadingUseCase @Inject constructor(private val vehicles: VehicleRepository, private val readings: OdometerRepository) {
    suspend operator fun invoke(vehicleId: Long, value: Long, recordedAt: Instant, note: String?): Long {
        if (vehicles.getVehicle(vehicleId) == null) throw gain.jacob.roadjournal.core.validation.ReadingError.VehicleMissing
        val (previous, next) = readings.surrounding(vehicleId, recordedAt)
        ReadingValidator.validate(value, previous, next)?.let { throw it }
        return readings.addReading(vehicleId, value, recordedAt, note?.trim()?.takeIf { it.isNotEmpty() }, Instant.now())
    }
}

class EditOdometerReadingUseCase @Inject constructor(private val readings: OdometerRepository) {
    suspend operator fun invoke(id: Long, value: Long, recordedAt: Instant, note: String?) {
        val current = readings.getReading(id) ?: throw gain.jacob.roadjournal.core.validation.ReadingError.ReadingMissing
        val (previous, next) = readings.surrounding(current.vehicleId, recordedAt, id)
        ReadingValidator.validate(value, previous, next)?.let { throw it }
        readings.updateReading(current.copy(value = value, recordedAt = recordedAt, note = note?.trim()?.takeIf { it.isNotEmpty() }, updatedAt = Instant.now()))
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
