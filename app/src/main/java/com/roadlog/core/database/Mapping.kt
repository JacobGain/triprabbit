package com.roadlog.core.database

import com.roadlog.core.database.entity.OdometerReadingEntity
import com.roadlog.core.database.entity.VehicleEntity
import com.roadlog.core.model.OdometerReading
import com.roadlog.core.model.Vehicle

fun VehicleEntity.toDomain() = Vehicle(id, name, make, model, year, licensePlate, odometerUnit, colorKey, notes, createdAt, archivedAt)
fun OdometerReadingEntity.toDomain() = OdometerReading(id, vehicleId, value, recordedAt, note, createdAt, updatedAt)
