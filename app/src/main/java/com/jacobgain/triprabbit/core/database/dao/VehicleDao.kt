package com.jacobgain.triprabbit.core.database.dao

import androidx.room.*
import com.jacobgain.triprabbit.core.database.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY created_at ASC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY created_at ASC")
    fun observeActive(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observeById(id: Long): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: Long): VehicleEntity?
    @Query("SELECT * FROM vehicles ORDER BY created_at ASC") suspend fun getAll(): List<VehicleEntity>

    @Insert suspend fun insert(vehicle: VehicleEntity): Long
    @Insert suspend fun insertAll(vehicles: List<VehicleEntity>)
    @Update suspend fun update(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("DELETE FROM vehicles") suspend fun deleteAll()
}
