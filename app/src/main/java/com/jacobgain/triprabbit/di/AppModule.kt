package com.jacobgain.triprabbit.di

import android.content.Context
import androidx.room.Room
import com.jacobgain.triprabbit.core.database.TripRabbitDatabase
import com.jacobgain.triprabbit.core.database.MIGRATION_1_2
import com.jacobgain.triprabbit.core.repository.*
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
    @Provides @Singleton fun database(@ApplicationContext context: Context): TripRabbitDatabase =
        Room.databaseBuilder(context, TripRabbitDatabase::class.java, "triprabbit.db").addMigrations(MIGRATION_1_2).build()
    @Provides fun vehicleDao(db: TripRabbitDatabase) = db.vehicleDao()
    @Provides fun readingDao(db: TripRabbitDatabase) = db.odometerReadingDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun vehicles(impl: VehicleRepositoryImpl): VehicleRepository
    @Binds abstract fun readings(impl: OdometerRepositoryImpl): OdometerRepository
    @Binds abstract fun settings(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun data(impl: DataRepositoryImpl): DataRepository
}
