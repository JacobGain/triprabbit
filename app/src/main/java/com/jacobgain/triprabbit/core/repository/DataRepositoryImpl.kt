package com.jacobgain.triprabbit.core.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.jacobgain.triprabbit.core.database.TripRabbitDatabase
import com.jacobgain.triprabbit.core.database.entity.*
import com.jacobgain.triprabbit.core.model.DistanceUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: TripRabbitDatabase,
) : DataRepository {
    private val resolver: ContentResolver get() = context.contentResolver

    override suspend fun exportBackup(uri: Uri): BackupSummary {
        val vehicles=database.vehicleDao().getAll();val readings=database.odometerReadingDao().getAll();val now=Instant.now()
        val root=JSONObject().put("version",1).put("exportedAt",now.toString()).put("vehicles",JSONArray().apply{vehicles.forEach{v->put(JSONObject().put("vehicle",v.json()).put("readings",JSONArray().apply{readings.filter{it.vehicleId==v.id}.forEach{put(it.json())}}))}})
        resolver.openOutputStream(uri,"wt")?.bufferedWriter()?.use{it.write(root.toString(2))}?:error("Couldn't open the selected file.")
        return BackupSummary(vehicles.size,readings.size,now)
    }

    override suspend fun inspectBackup(uri: Uri)=parse(uri).summary
    override suspend fun restoreBackup(uri: Uri): BackupSummary {
        val parsed=parse(uri)
        database.withTransaction { database.vehicleDao().deleteAll();database.vehicleDao().insertAll(parsed.vehicles);database.odometerReadingDao().insertAll(parsed.readings) }
        return parsed.summary
    }

    override suspend fun exportVehicleCsv(uri: Uri, vehicleId: Long) {
        val vehicle=database.vehicleDao().getById(vehicleId)?:error("Vehicle not found.")
        val readings=database.odometerReadingDao().getAll().filter{it.vehicleId==vehicleId}.sortedBy{it.recordedAt};var previous:Long?=null
        resolver.openOutputStream(uri,"wt")?.bufferedWriter()?.use{writer->writer.appendLine("date,odometer,unit,difference,note");readings.forEach{r->val difference=previous?.let{r.value-it};writer.appendLine(listOf(r.recordedAt.toString(),r.value,vehicle.odometerUnit.abbreviation,difference?:"",r.note.orEmpty()).joinToString(","){csv(it.toString())});previous=r.value}}?:error("Couldn't open the selected file.")
    }

    private fun parse(uri:Uri):Parsed {
        val text=resolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:error("Couldn't open the selected file.")
        val root=runCatching{JSONObject(text)}.getOrElse{error("This isn't a valid TripRabbit backup.")}
        require(root.optInt("version",-1)==1){"This backup version isn't supported."}
        val groups=root.getJSONArray("vehicles");val vehicles=mutableListOf<VehicleEntity>();val readings=mutableListOf<OdometerReadingEntity>()
        repeat(groups.length()){
            val group=groups.getJSONObject(it);val vehicle=group.getJSONObject("vehicle").vehicle();vehicles+=vehicle
            val entries=group.getJSONArray("readings");repeat(entries.length()){index->
                val reading=entries.getJSONObject(index).reading()
                require(reading.vehicleId==vehicle.id){"The backup contains a reading in the wrong vehicle."}
                readings+=reading
            }
        }
        require(vehicles.all{it.id>0&&it.name.isNotBlank()}){"The backup contains an invalid vehicle."}
        require(vehicles.map{it.id}.distinct().size==vehicles.size){"The backup contains duplicate vehicles."}
        require(readings.all{it.id>0&&it.value>=0}){"The backup contains an invalid odometer reading."}
        require(readings.map{it.id}.distinct().size==readings.size){"The backup contains duplicate readings."}
        val ids=vehicles.map{it.id}.toSet();require(readings.all{it.vehicleId in ids}){"The backup contains readings without a vehicle."}
        readings.groupBy{it.vehicleId}.values.forEach{history->val ordered=history.sortedWith(compareBy<OdometerReadingEntity>{it.recordedAt}.thenBy{it.id});require(ordered.zipWithNext().all{(a,b)->a.value<=b.value}){"The backup contains invalid odometer history."}}
        val exported=runCatching{Instant.parse(root.getString("exportedAt"))}.getOrElse{error("The backup date is invalid.")}
        return Parsed(vehicles,readings,BackupSummary(vehicles.size,readings.size,exported))
    }
}

private data class Parsed(val vehicles:List<VehicleEntity>,val readings:List<OdometerReadingEntity>,val summary:BackupSummary)
private fun VehicleEntity.json()=JSONObject().put("id",id).put("name",name).putOpt("make",make).putOpt("model",model).putOpt("year",year).putOpt("licensePlate",licensePlate).put("odometerUnit",odometerUnit.name).putOpt("colorKey",colorKey).putOpt("notes",notes).put("createdAt",createdAt.toString()).putOpt("archivedAt",archivedAt?.toString())
private fun OdometerReadingEntity.json()=JSONObject().put("id",id).put("vehicleId",vehicleId).put("value",value).put("recordedAt",recordedAt.toString()).putOpt("note",note).put("createdAt",createdAt.toString()).putOpt("updatedAt",updatedAt?.toString())
private fun JSONObject.vehicle()=VehicleEntity(getLong("id"),getString("name"),stringOrNull("make"),stringOrNull("model"),if(isNull("year"))null else getInt("year"),stringOrNull("licensePlate"),DistanceUnit.valueOf(getString("odometerUnit")),stringOrNull("colorKey"),stringOrNull("notes"),Instant.parse(getString("createdAt")),stringOrNull("archivedAt")?.let(Instant::parse))
private fun JSONObject.reading()=OdometerReadingEntity(getLong("id"),getLong("vehicleId"),getLong("value"),Instant.parse(getString("recordedAt")),stringOrNull("note"),Instant.parse(getString("createdAt")),stringOrNull("updatedAt")?.let(Instant::parse))
private fun JSONObject.stringOrNull(key:String)=if(!has(key)||isNull(key))null else getString(key)
private fun csv(value:String)="\"${value.replace("\"","\"\"")}\""
