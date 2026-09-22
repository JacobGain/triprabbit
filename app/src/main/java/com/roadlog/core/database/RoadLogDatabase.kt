package com.roadlog.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.roadlog.core.database.converter.Converters
import com.roadlog.core.database.dao.OdometerReadingDao
import com.roadlog.core.database.dao.VehicleDao
import com.roadlog.core.database.entity.OdometerReadingEntity
import com.roadlog.core.database.entity.VehicleEntity

@Database(entities = [VehicleEntity::class, OdometerReadingEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class RoadLogDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun odometerReadingDao(): OdometerReadingDao
}
