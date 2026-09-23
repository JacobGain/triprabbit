package com.jacobgain.triprabbit.core.repository

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacobgain.triprabbit.core.database.TripRabbitDatabase
import com.jacobgain.triprabbit.core.database.entity.*
import com.jacobgain.triprabbit.core.model.DistanceUnit
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DataRepositoryTest {
    private val context get()=ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var db:TripRabbitDatabase;private lateinit var repository:DataRepositoryImpl;private lateinit var directory:File
    @Before fun setup(){db=Room.inMemoryDatabaseBuilder(context,TripRabbitDatabase::class.java).allowMainThreadQueries().build();repository=DataRepositoryImpl(context,db);directory=File(context.cacheDir,"backup-tests").apply{mkdirs()}}
    @After fun close(){db.close();directory.deleteRecursively()}
    @Test fun jsonRoundTripRestoresVehiclesAndReadings()=runBlocking{seed();val file=File(directory,"backup.json");val summary=repository.exportBackup(Uri.fromFile(file));assertEquals(1,summary.vehicleCount);assertEquals(2,summary.readingCount);db.vehicleDao().deleteAll();assertTrue(db.vehicleDao().getAll().isEmpty());val restored=repository.restoreBackup(Uri.fromFile(file));assertEquals(1,restored.vehicleCount);assertEquals("Golf",db.vehicleDao().getAll().single().name);assertEquals(listOf(120_000L,120_350L),db.odometerReadingDao().getAll().map{it.value})}
    @Test fun csvContainsChronologicalDifferences()=runBlocking{seed();val file=File(directory,"golf.csv");repository.exportVehicleCsv(Uri.fromFile(file),1);val text=file.readText();assertTrue(text.contains("date,trip name,odometer,unit,distance,notes"));assertTrue(text.contains("\"120350\",\"km\",\"350\""))}
    @Test fun rangedReportIncludesNameNotesAndDistance()=runBlocking{seed();val file=File(directory,"report.csv");repository.exportVehicleReport(Uri.fromFile(file),1,LocalDate.parse("2026-09-20"),LocalDate.parse("2026-09-22"),false);val text=file.readText();assertEquals(2,text.trim().lines().size);assertTrue(text.contains("Construction site"));assertTrue(text.contains("Trip"));assertTrue(text.contains("\"350\""));assertFalse(text.contains("120000"))}
    @Test fun pdfReportIsWritten()=runBlocking{seed();val file=File(directory,"report.pdf");repository.exportVehicleReport(Uri.fromFile(file),1,LocalDate.parse("2026-09-20"),LocalDate.parse("2026-09-22"),true);assertTrue(file.readBytes().take(4).toByteArray().contentEquals("%PDF".toByteArray()));assertTrue(file.length()>500)}
    @Test fun invalidBackupDoesNotReplaceExistingData()=runBlocking{seed();val file=File(directory,"bad.json").apply{writeText("{\"version\":99,\"vehicles\":[]}")};assertTrue(runCatching{repository.restoreBackup(Uri.fromFile(file))}.isFailure);assertEquals("Golf",db.vehicleDao().getAll().single().name)}
    @Test fun invalidReadingDoesNotReplaceExistingData()=runBlocking{
        seed();val file=File(directory,"backup.json");repository.exportBackup(Uri.fromFile(file))
        file.writeText(file.readText().replace("\"value\": 120000","\"value\": -1"))
        assertTrue(runCatching{repository.restoreBackup(Uri.fromFile(file))}.isFailure)
        assertEquals(listOf(120_000L,120_350L),db.odometerReadingDao().getAll().map{it.value})
    }
    private suspend fun seed(){db.vehicleDao().insertAll(listOf(VehicleEntity(1,"Golf","Volkswagen","Golf",2017,null,DistanceUnit.KILOMETERS,null,null,Instant.EPOCH,null)));db.odometerReadingDao().insertAll(listOf(OdometerReadingEntity(1,1,120_000,Instant.parse("2026-09-01T12:00:00Z"),null,Instant.EPOCH,null),OdometerReadingEntity(2,1,120_350,Instant.parse("2026-09-21T12:00:00Z"),"Trip",Instant.EPOCH,null,"Construction site")))}
}
