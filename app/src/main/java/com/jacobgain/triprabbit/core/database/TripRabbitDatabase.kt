package com.jacobgain.triprabbit.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jacobgain.triprabbit.core.database.converter.Converters
import com.jacobgain.triprabbit.core.database.dao.OdometerReadingDao
import com.jacobgain.triprabbit.core.database.dao.VehicleDao
import com.jacobgain.triprabbit.core.database.entity.OdometerReadingEntity
import com.jacobgain.triprabbit.core.database.entity.VehicleEntity

@Database(entities = [VehicleEntity::class, OdometerReadingEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class TripRabbitDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun odometerReadingDao(): OdometerReadingDao
}
