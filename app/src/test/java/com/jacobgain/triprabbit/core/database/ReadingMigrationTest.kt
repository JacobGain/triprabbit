package com.jacobgain.triprabbit.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReadingMigrationTest {
    @Test fun existingReadingsKeepTheirDateAndTimeAfterUpgrade() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "reading-migration-test.db"
        context.deleteDatabase(name)
        val raw = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        raw.execSQL("CREATE TABLE vehicles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, make TEXT, model TEXT, year INTEGER, license_plate TEXT, odometer_unit TEXT NOT NULL, color_key TEXT, notes TEXT, created_at INTEGER NOT NULL, archived_at INTEGER)")
        raw.execSQL("CREATE TABLE odometer_readings (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, vehicle_id INTEGER NOT NULL, value INTEGER NOT NULL, recorded_at INTEGER NOT NULL, note TEXT, created_at INTEGER NOT NULL, updated_at INTEGER, FOREIGN KEY(vehicle_id) REFERENCES vehicles(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        raw.execSQL("CREATE INDEX index_odometer_readings_vehicle_id ON odometer_readings(vehicle_id)")
        raw.execSQL("CREATE INDEX index_odometer_readings_vehicle_id_recorded_at ON odometer_readings(vehicle_id, recorded_at)")
        raw.execSQL("CREATE INDEX index_odometer_readings_vehicle_id_value ON odometer_readings(vehicle_id, value)")
        raw.execSQL("INSERT INTO vehicles(id,name,odometer_unit,created_at) VALUES(1,'Golf','KILOMETERS',0)")
        raw.execSQL("INSERT INTO odometer_readings(id,vehicle_id,value,recorded_at,created_at) VALUES(1,1,1000,10000,0)")
        raw.version = 1
        raw.close()
        val db = Room.databaseBuilder(context, TripRabbitDatabase::class.java, name).addMigrations(MIGRATION_1_2).build()
        try {
            val reading = db.odometerReadingDao().getAll().single()
            assertEquals(1000, reading.value)
            assertNull(reading.name)
            assertTrue(reading.hasTime)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
