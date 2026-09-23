package com.jacobgain.triprabbit.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jacobgain.triprabbit.core.database.converter.Converters
import com.jacobgain.triprabbit.core.database.dao.OdometerReadingDao
import com.jacobgain.triprabbit.core.database.dao.VehicleDao
import com.jacobgain.triprabbit.core.database.entity.OdometerReadingEntity
import com.jacobgain.triprabbit.core.database.entity.VehicleEntity

@Database(entities = [VehicleEntity::class, OdometerReadingEntity::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class TripRabbitDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun odometerReadingDao(): OdometerReadingDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE odometer_readings ADD COLUMN name TEXT")
        db.execSQL("ALTER TABLE odometer_readings ADD COLUMN has_time INTEGER NOT NULL DEFAULT 1")
    }
}
