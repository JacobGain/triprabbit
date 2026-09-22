package com.roadlog.core.database.dao

import androidx.room.*
import com.roadlog.core.database.entity.OdometerReadingEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface OdometerReadingDao {
    @Query("SELECT * FROM odometer_readings WHERE vehicle_id = :vehicleId ORDER BY recorded_at DESC, id DESC")
    fun observeForVehicle(vehicleId: Long): Flow<List<OdometerReadingEntity>>

    @Query("SELECT * FROM odometer_readings WHERE vehicle_id = :vehicleId ORDER BY recorded_at DESC, id DESC LIMIT 1")
    fun observeLatest(vehicleId: Long): Flow<OdometerReadingEntity?>

    @Query("SELECT * FROM odometer_readings WHERE id = :id")
    fun observeById(id: Long): Flow<OdometerReadingEntity?>

    @Query("SELECT * FROM odometer_readings WHERE id = :id")
    suspend fun getById(id: Long): OdometerReadingEntity?
    @Query("SELECT * FROM odometer_readings ORDER BY recorded_at ASC, id ASC") suspend fun getAll(): List<OdometerReadingEntity>

    @Query("SELECT * FROM odometer_readings WHERE vehicle_id = :vehicleId AND recorded_at <= :at AND id != :excludeId ORDER BY recorded_at DESC, id DESC LIMIT 1")
    suspend fun previous(vehicleId: Long, at: Instant, excludeId: Long = -1): OdometerReadingEntity?

    @Query("SELECT * FROM odometer_readings WHERE vehicle_id = :vehicleId AND recorded_at > :at AND id != :excludeId ORDER BY recorded_at ASC, id ASC LIMIT 1")
    suspend fun next(vehicleId: Long, at: Instant, excludeId: Long = -1): OdometerReadingEntity?

    @Insert suspend fun insert(reading: OdometerReadingEntity): Long
    @Insert suspend fun insertAll(readings: List<OdometerReadingEntity>)
    @Update suspend fun update(reading: OdometerReadingEntity)
    @Query("DELETE FROM odometer_readings WHERE id = :id") suspend fun delete(id: Long)
}
