package com.jacobgain.triprabbit.core.database

import com.jacobgain.triprabbit.core.database.entity.OdometerReadingEntity
import com.jacobgain.triprabbit.core.database.entity.VehicleEntity
import com.jacobgain.triprabbit.core.model.OdometerReading
import com.jacobgain.triprabbit.core.model.Vehicle

fun VehicleEntity.toDomain() = Vehicle(id, name, make, model, year, licensePlate, odometerUnit, colorKey, notes, createdAt, archivedAt)
fun OdometerReadingEntity.toDomain() = OdometerReading(id, vehicleId, value, recordedAt, note, createdAt, updatedAt)
