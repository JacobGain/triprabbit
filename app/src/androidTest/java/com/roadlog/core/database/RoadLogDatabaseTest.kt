package com.roadlog.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.roadlog.core.database.entity.*
import com.roadlog.core.model.DistanceUnit
import com.roadlog.core.model.AppSettings
import com.roadlog.core.model.VehicleInput
import com.roadlog.core.repository.*
import com.roadlog.core.usecase.CreateVehicleUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RoadLogDatabaseTest {
    private lateinit var db: RoadLogDatabase
    @Before fun create() { db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),RoadLogDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close()=db.close()
    private suspend fun vehicle(name:String="Golf")=db.vehicleDao().insert(VehicleEntity(name=name,make=null,model=null,year=null,licensePlate=null,odometerUnit=DistanceUnit.KILOMETERS,colorKey=null,notes=null,createdAt=Instant.EPOCH,archivedAt=null))
    private suspend fun reading(vehicleId:Long,value:Long,at:Long)=db.odometerReadingDao().insert(OdometerReadingEntity(vehicleId=vehicleId,value=value,recordedAt=Instant.ofEpochSecond(at),note=null,createdAt=Instant.EPOCH,updatedAt=null))
    @Test fun vehicleCrudAndFlowUpdates()=runBlocking { val id=vehicle();assertEquals("Golf",db.vehicleDao().observeById(id).first()?.name);val entity=db.vehicleDao().getById(id)!!;db.vehicleDao().update(entity.copy(name="Daily"));assertEquals("Daily",db.vehicleDao().observeActive().first().single().name);db.vehicleDao().delete(id);assertTrue(db.vehicleDao().observeAll().first().isEmpty()) }
    @Test fun readingsAreOrderedAndLatestIsCorrect()=runBlocking { val id=vehicle();reading(id,100,1);reading(id,200,2);assertEquals(listOf(200L,100L),db.odometerReadingDao().observeForVehicle(id).first().map{it.value});assertEquals(200L,db.odometerReadingDao().observeLatest(id).first()?.value) }
    @Test fun deletingVehicleCascadesReadings()=runBlocking { val id=vehicle();reading(id,100,1);db.vehicleDao().delete(id);assertTrue(db.odometerReadingDao().observeForVehicle(id).first().isEmpty()) }
    @Test fun vehicleCreationIncludesInitialReading()=runBlocking {
        val settings=object:SettingsRepository{override fun observeSettings():Flow<AppSettings> = flowOf(AppSettings());override suspend fun setSelectedVehicle(id:Long?){};override suspend fun setFirstLaunchComplete(complete:Boolean){};override suspend fun setThemeMode(value:com.roadlog.core.model.ThemeMode){};override suspend fun setAccentTheme(value:com.roadlog.core.model.AccentTheme){};override suspend fun setDynamicColor(value:Boolean){};override suspend fun setAmoledBlack(value:Boolean){};override suspend fun setDisplayDensity(value:com.roadlog.core.model.DisplayDensity){};override suspend fun setConfirmReadingDeletion(value:Boolean){}}
        val useCase=CreateVehicleUseCase(db,VehicleRepositoryImpl(db.vehicleDao()),OdometerRepositoryImpl(db.odometerReadingDao()),settings)
        val id=useCase(VehicleInput("Golf",odometerUnit=DistanceUnit.KILOMETERS),120000,Instant.EPOCH)
        assertNotNull(db.vehicleDao().getById(id));assertEquals(120000L,db.odometerReadingDao().observeLatest(id).first()?.value)
    }
}
