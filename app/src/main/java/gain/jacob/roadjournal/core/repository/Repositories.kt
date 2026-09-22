package gain.jacob.roadjournal.core.repository

import gain.jacob.roadjournal.core.model.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import android.net.Uri

interface VehicleRepository {
    fun observeVehicles(): Flow<List<Vehicle>>
    fun observeActiveVehicles(): Flow<List<Vehicle>>
    fun observeVehicle(id: Long): Flow<Vehicle?>
    suspend fun getVehicle(id: Long): Vehicle?
    suspend fun createVehicle(input: VehicleInput, createdAt: Instant): Long
    suspend fun updateVehicle(vehicle: Vehicle)
    suspend fun archiveVehicle(id: Long)
    suspend fun deleteVehicle(id: Long)
}

interface OdometerRepository {
    fun observeReadings(vehicleId: Long): Flow<List<OdometerReading>>
    fun observeLatestReading(vehicleId: Long): Flow<OdometerReading?>
    fun observeReading(id: Long): Flow<OdometerReading?>
    suspend fun getReading(id: Long): OdometerReading?
    suspend fun surrounding(vehicleId: Long, at: Instant, excludingId: Long = -1): Pair<OdometerReading?, OdometerReading?>
    suspend fun addReading(vehicleId: Long, value: Long, recordedAt: Instant, note: String?, createdAt: Instant): Long
    suspend fun updateReading(reading: OdometerReading)
    suspend fun deleteReading(id: Long)
}

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setSelectedVehicle(id: Long?)
    suspend fun setFirstLaunchComplete(complete: Boolean)
    suspend fun setThemeMode(value: ThemeMode)
    suspend fun setAccentTheme(value: AccentTheme)
    suspend fun setDynamicColor(value: Boolean)
    suspend fun setAmoledBlack(value: Boolean)
    suspend fun setDisplayDensity(value: DisplayDensity)
    suspend fun setConfirmReadingDeletion(value: Boolean)
}

data class BackupSummary(val vehicleCount: Int, val readingCount: Int, val exportedAt: Instant)
interface DataRepository {
    suspend fun exportBackup(uri: Uri): BackupSummary
    suspend fun inspectBackup(uri: Uri): BackupSummary
    suspend fun restoreBackup(uri: Uri): BackupSummary
    suspend fun exportVehicleCsv(uri: Uri, vehicleId: Long)
}
