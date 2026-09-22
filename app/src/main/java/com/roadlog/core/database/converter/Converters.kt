package com.roadlog.core.database.converter

import androidx.room.TypeConverter
import com.roadlog.core.model.DistanceUnit
import java.time.Instant

class Converters {
    @TypeConverter fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
    @TypeConverter fun unitToString(value: DistanceUnit): String = value.name
    @TypeConverter fun stringToUnit(value: String): DistanceUnit = DistanceUnit.valueOf(value)
}
