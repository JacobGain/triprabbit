package gain.jacob.roadjournal.core.database.dao

import androidx.room.*
import gain.jacob.roadjournal.core.database.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY created_at ASC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE archived_at IS NULL ORDER BY created_at ASC")
    fun observeActive(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observeById(id: Long): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: Long): VehicleEntity?
    @Query("SELECT * FROM vehicles ORDER BY created_at ASC") suspend fun getAll(): List<VehicleEntity>

    @Insert suspend fun insert(vehicle: VehicleEntity): Long
    @Insert suspend fun insertAll(vehicles: List<VehicleEntity>)
    @Update suspend fun update(vehicle: VehicleEntity)

    @Query("UPDATE vehicles SET archived_at = :archivedAt WHERE id = :id")
    suspend fun archive(id: Long, archivedAt: Instant)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("DELETE FROM vehicles") suspend fun deleteAll()
}
