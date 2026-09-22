package com.roadlog.di

import android.content.Context
import androidx.room.Room
import com.roadlog.core.database.RoadLogDatabase
import com.roadlog.core.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): RoadLogDatabase =
        Room.databaseBuilder(context, RoadLogDatabase::class.java, "roadlog.db").build()
    @Provides fun vehicleDao(db: RoadLogDatabase) = db.vehicleDao()
    @Provides fun readingDao(db: RoadLogDatabase) = db.odometerReadingDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun vehicles(impl: VehicleRepositoryImpl): VehicleRepository
    @Binds abstract fun readings(impl: OdometerRepositoryImpl): OdometerRepository
    @Binds abstract fun settings(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun data(impl: DataRepositoryImpl): DataRepository
}
