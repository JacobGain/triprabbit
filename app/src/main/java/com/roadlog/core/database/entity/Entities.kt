package com.roadlog.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.roadlog.core.model.DistanceUnit
import java.time.Instant

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val make: String?,
    val model: String?,
    val year: Int?,
    @ColumnInfo(name = "license_plate") val licensePlate: String?,
    @ColumnInfo(name = "odometer_unit") val odometerUnit: DistanceUnit,
    @ColumnInfo(name = "color_key") val colorKey: String?,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "archived_at") val archivedAt: Instant?,
)

@Entity(
    tableName = "odometer_readings",
    foreignKeys = [ForeignKey(
        entity = VehicleEntity::class,
        parentColumns = ["id"],
        childColumns = ["vehicle_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index("vehicle_id"),
        Index(value = ["vehicle_id", "recorded_at"]),
        Index(value = ["vehicle_id", "value"]),
    ],
)
data class OdometerReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "vehicle_id") val vehicleId: Long,
    val value: Long,
    @ColumnInfo(name = "recorded_at") val recordedAt: Instant,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant?,
)
