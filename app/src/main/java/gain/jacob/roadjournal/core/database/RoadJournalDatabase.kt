package gain.jacob.roadjournal.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import gain.jacob.roadjournal.core.database.converter.Converters
import gain.jacob.roadjournal.core.database.dao.OdometerReadingDao
import gain.jacob.roadjournal.core.database.dao.VehicleDao
import gain.jacob.roadjournal.core.database.entity.OdometerReadingEntity
import gain.jacob.roadjournal.core.database.entity.VehicleEntity

@Database(entities = [VehicleEntity::class, OdometerReadingEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class RoadJournalDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun odometerReadingDao(): OdometerReadingDao
}
