package gain.jacob.roadjournal.core.database

import gain.jacob.roadjournal.core.database.entity.OdometerReadingEntity
import gain.jacob.roadjournal.core.database.entity.VehicleEntity
import gain.jacob.roadjournal.core.model.OdometerReading
import gain.jacob.roadjournal.core.model.Vehicle

fun VehicleEntity.toDomain() = Vehicle(id, name, make, model, year, licensePlate, odometerUnit, colorKey, notes, createdAt, archivedAt)
fun OdometerReadingEntity.toDomain() = OdometerReading(id, vehicleId, value, recordedAt, note, createdAt, updatedAt)
