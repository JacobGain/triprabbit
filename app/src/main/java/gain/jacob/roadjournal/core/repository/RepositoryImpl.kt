package gain.jacob.roadjournal.core.repository

import gain.jacob.roadjournal.core.database.dao.OdometerReadingDao
import gain.jacob.roadjournal.core.database.dao.VehicleDao
import gain.jacob.roadjournal.core.database.entity.OdometerReadingEntity
import gain.jacob.roadjournal.core.database.entity.VehicleEntity
import gain.jacob.roadjournal.core.database.toDomain
import gain.jacob.roadjournal.core.datastore.AppPreferencesDataSource
import gain.jacob.roadjournal.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepositoryImpl @Inject constructor(private val dao: VehicleDao) : VehicleRepository {
    override fun observeVehicles() = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override fun observeActiveVehicles() = dao.observeActive().map { list -> list.map { it.toDomain() } }
    override fun observeVehicle(id: Long) = dao.observeById(id).map { it?.toDomain() }
    override suspend fun getVehicle(id: Long) = dao.getById(id)?.toDomain()
    override suspend fun createVehicle(input: VehicleInput, createdAt: Instant) = dao.insert(input.toEntity(createdAt))
    override suspend fun updateVehicle(vehicle: Vehicle) = dao.update(vehicle.toEntity())
    override suspend fun archiveVehicle(id: Long) = dao.archive(id, Instant.now())
    override suspend fun deleteVehicle(id: Long) = dao.delete(id)
}

@Singleton
class OdometerRepositoryImpl @Inject constructor(private val dao: OdometerReadingDao) : OdometerRepository {
    override fun observeReadings(vehicleId: Long) = dao.observeForVehicle(vehicleId).map { list -> list.map { it.toDomain() } }
    override fun observeLatestReading(vehicleId: Long) = dao.observeLatest(vehicleId).map { it?.toDomain() }
    override fun observeReading(id: Long) = dao.observeById(id).map { it?.toDomain() }
    override suspend fun getReading(id: Long) = dao.getById(id)?.toDomain()
    override suspend fun surrounding(vehicleId: Long, at: Instant, excludingId: Long) =
        dao.previous(vehicleId, at, excludingId)?.toDomain() to dao.next(vehicleId, at, excludingId)?.toDomain()
    override suspend fun addReading(vehicleId: Long, value: Long, recordedAt: Instant, note: String?, createdAt: Instant) =
        dao.insert(OdometerReadingEntity(vehicleId = vehicleId, value = value, recordedAt = recordedAt, note = note, createdAt = createdAt, updatedAt = null))
    override suspend fun updateReading(reading: OdometerReading) = dao.update(reading.toEntity())
    override suspend fun deleteReading(id: Long) = dao.delete(id)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(private val dataSource: AppPreferencesDataSource) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettings> = dataSource.settings
    override suspend fun setSelectedVehicle(id: Long?) = dataSource.setSelectedVehicle(id)
    override suspend fun setFirstLaunchComplete(complete: Boolean) = dataSource.setFirstLaunchComplete(complete)
    override suspend fun setThemeMode(value: ThemeMode) = dataSource.updateAppearance(themeMode = value)
    override suspend fun setAccentTheme(value: AccentTheme) = dataSource.updateAppearance(accentTheme = value)
    override suspend fun setDynamicColor(value: Boolean) = dataSource.updateAppearance(dynamicColor = value)
    override suspend fun setAmoledBlack(value: Boolean) = dataSource.updateAppearance(amoled = value)
    override suspend fun setDisplayDensity(value: DisplayDensity) = dataSource.updateAppearance(density = value)
    override suspend fun setConfirmReadingDeletion(value: Boolean) = dataSource.setConfirmReadingDeletion(value)
}

private fun VehicleInput.toEntity(now: Instant) = VehicleEntity(
    name = name.trim(), make = make.clean(), model = model.clean(), year = year,
    licensePlate = licensePlate.clean(), odometerUnit = odometerUnit, colorKey = colorKey.clean(),
    notes = notes.clean(), createdAt = now, archivedAt = null,
)

private fun Vehicle.toEntity() = VehicleEntity(id, name.trim(), make.clean(), model.clean(), year, licensePlate.clean(), odometerUnit, colorKey.clean(), notes.clean(), createdAt, archivedAt)
private fun OdometerReading.toEntity() = OdometerReadingEntity(id, vehicleId, value, recordedAt, note.clean(), createdAt, updatedAt)
private fun String?.clean() = this?.trim()?.takeIf(String::isNotEmpty)
