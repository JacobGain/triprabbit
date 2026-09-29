package com.jacobgain.triprabbit.core.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.room.withTransaction
import com.jacobgain.triprabbit.core.database.TripRabbitDatabase
import com.jacobgain.triprabbit.core.database.entity.*
import com.jacobgain.triprabbit.core.model.DistanceUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong
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
        return replaceWith(parsed)
    }
    override suspend fun restoreBackupContent(content: String): BackupSummary = replaceWith(parseText(content))
    private suspend fun replaceWith(parsed: Parsed): BackupSummary {
        database.withTransaction { database.vehicleDao().deleteAll();database.vehicleDao().insertAll(parsed.vehicles);database.odometerReadingDao().insertAll(parsed.readings) }
        return parsed.summary
    }

    override suspend fun exportVehicleCsv(uri: Uri, vehicleId: Long) {
        val vehicle=database.vehicleDao().getById(vehicleId)?:error("Vehicle not found.")
        val readings=database.odometerReadingDao().getAll().filter{it.vehicleId==vehicleId && !it.inProgress}.sortedBy{it.recordedAt};var previous:Long?=null
        resolver.openOutputStream(uri,"wt")?.bufferedWriter()?.use{writer->writer.appendLine("date,trip name,start odometer,finish odometer,unit,distance,notes");readings.forEach{r->val difference=r.startValue?.let { r.value-it } ?: previous?.let{r.value-it};writer.appendLine(listOf(r.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate(),r.name.orEmpty(),r.startValue ?: previous ?: "",r.value,vehicle.odometerUnit.abbreviation,difference?:"",r.note.orEmpty()).joinToString(","){csv(it.toString())});previous=r.value}}?:error("Couldn't open the selected file.")
    }

    override suspend fun exportVehicleReport(uri: Uri, vehicleId: Long, from: LocalDate, through: LocalDate, pdf: Boolean) {
        require(!through.isBefore(from)) { "End date must be on or after start date." }
        val vehicle = database.vehicleDao().getById(vehicleId) ?: error("Vehicle not found.")
        val ordered = database.odometerReadingDao().getAll().filter { it.vehicleId == vehicleId && !it.inProgress }
            .sortedWith(compareBy<OdometerReadingEntity> { it.recordedAt }.thenBy { it.id })
        val rows = ordered.mapIndexed { index, reading ->
            ReportRow(reading.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate(), reading.name.orEmpty(),
                reading.note.orEmpty(), reading.value, reading.startValue ?: ordered.getOrNull(index - 1)?.let { it.value },
                reading.startValue?.let { reading.value - it } ?: ordered.getOrNull(index - 1)?.let { reading.value - it.value })
        }.filter { it.date >= from && it.date <= through }
        if (pdf) writeTripReportPdf(uri, vehicle.name, vehicle.odometerUnit.abbreviation, from, through, allVehicles = false,
            rows.filter { it.distance != null }.sortedBy { it.date }
                .map { PdfReportTrip(null, it.date, it.name, it.note, it.startValue, it.odometer, it.distance) })
        else resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.appendLine("date,trip name,notes,start odometer,finish odometer,distance (${vehicle.odometerUnit.abbreviation})")
            rows.sortedWith(compareBy<ReportRow> { it.name.ifBlank { "Untitled trip" }.lowercase() }.thenBy { it.date })
                .forEach { row -> writer.appendLine(listOf(row.date,row.name,row.note,row.startValue ?: "",row.odometer,row.distance ?: "").joinToString(",") { csv(it.toString()) }) }
        } ?: error("Couldn't open the selected file.")
    }

    override suspend fun exportVehiclesReport(uri: Uri, vehicleIds: List<Long>, from: LocalDate, through: LocalDate, unit: DistanceUnit, allVehicles: Boolean, pdf: Boolean) {
        require(!through.isBefore(from)) { "End date must be on or after start date." }
        val selectedIds = vehicleIds.toSet()
        require(selectedIds.isNotEmpty()) { "Select at least one vehicle." }
        val vehicles = database.vehicleDao().getAll().filter { it.id in selectedIds }
        require(vehicles.isNotEmpty()) { "No vehicles are available to export." }
        val allReadings = database.odometerReadingDao().getAll()
        val rows = vehicles.flatMap { vehicle ->
            val ordered = allReadings.filter { it.vehicleId == vehicle.id && !it.inProgress }
                .sortedWith(compareBy<OdometerReadingEntity> { it.recordedAt }.thenBy { it.id })
            ordered.mapIndexed { index, reading ->
                val previous = ordered.getOrNull(index - 1)
                val startValue = reading.startValue ?: previous?.value
                val distance = reading.startValue?.let { reading.value - it }
                    ?: previous?.let { reading.value - it.value }
                FleetReportRow(vehicle.name, unit.abbreviation,
                    reading.recordedAt.atZone(ZoneId.systemDefault()).toLocalDate(), reading.name.orEmpty(),
                    reading.note.orEmpty(), convertUnit(reading.value, vehicle.odometerUnit, unit),
                    startValue?.let { convertUnit(it, vehicle.odometerUnit, unit) },
                    distance?.let { convertUnit(it, vehicle.odometerUnit, unit) })
            }.filter { it.date >= from && it.date <= through }
        }.sortedWith(compareBy<FleetReportRow> { it.date }.thenBy { it.vehicle.lowercase() }.thenBy { it.name.lowercase() })
        if (pdf) {
            val title = if (allVehicles) "All Vehicles" else vehicles.singleOrNull()?.name ?: "Selected Vehicles"
            val trips = rows.filter { it.distance != null }.map { row ->
                PdfReportTrip(if (allVehicles) row.vehicle else null, row.date, row.name, row.note,
                    row.startValue, row.odometer, row.distance)
            }
            writeTripReportPdf(uri, title, unit.abbreviation, from, through, allVehicles, trips)
        }
        else resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.appendLine("vehicle,date,trip name,notes,start odometer,finish odometer,unit,distance (${unit.abbreviation})")
            rows.forEach { row ->
                writer.appendLine(listOf(row.vehicle, row.date, row.name, row.note, row.startValue ?: "", row.odometer,
                    row.unit, row.distance ?: "").joinToString(",") { csv(it.toString()) })
            }
        } ?: error("Couldn't open the selected file.")
    }

    private fun writeTripReportPdf(
        uri: Uri,
        vehicleTitle: String,
        unit: String,
        from: LocalDate,
        through: LocalDate,
        allVehicles: Boolean,
        trips: List<PdfReportTrip>,
    ) {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 11f }
        var page: PdfDocument.Page? = null
        var pageNumber = 0
        var y = 0f
        val left = 40f
        val contentWidth = 515f
        val bottom = 802f
        fun newPage() {
            page?.let(document::finishPage)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            y = 40f
        }
        fun wrappedLines(text: String, size: Float, bold: Boolean): List<String> {
            paint.textSize = size
            paint.isFakeBoldText = bold
            val lines = mutableListOf<String>()
            var current = ""
            text.split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= contentWidth) {
                    current = candidate
                } else {
                    if (current.isNotEmpty()) lines += current
                    current = ""
                    var remaining = word
                    while (remaining.isNotEmpty() && paint.measureText(remaining) > contentWidth) {
                        var count = remaining.length
                        while (count > 1 && paint.measureText(remaining, 0, count) > contentWidth) count--
                        lines += remaining.take(count)
                        remaining = remaining.drop(count)
                    }
                    current = remaining
                }
            }
            if (current.isNotEmpty() || lines.isEmpty()) lines += current
            return lines
        }
        fun drawLines(lines: List<String>, size: Float, bold: Boolean, lineGap: Float) {
            paint.textSize = size
            paint.isFakeBoldText = bold
            lines.forEach { text ->
                if (page == null || y + size + lineGap > bottom) newPage()
                page!!.canvas.drawText(text, left, y, paint)
                y += size + lineGap
            }
        }
        fun paragraph(text: String, size: Float, bold: Boolean = false, gapBefore: Float = 0f, gapAfter: Float = 0f) {
            val lines = wrappedLines(text, size, bold)
            val lineHeight = size + 4f
            val needed = gapBefore + lines.size * lineHeight + gapAfter
            if (page == null || (needed < bottom - 40f && y + needed > bottom)) newPage()
            y += gapBefore
            drawLines(lines, size, bold, 4f)
            y += gapAfter
        }
        fun tripItem(trip: PdfReportTrip, includeVehicle: Boolean) {
            val first = pdfReportFirstRow(trip, unit, includeVehicle)
            val second = pdfReportSecondRow(trip, unit)
            val firstLines = wrappedLines(first, 11f, true)
            val secondLines = wrappedLines(second, 10f, false)
            val needed = firstLines.size * 15f + 5f + secondLines.size * 14f + 12f
            if (needed < bottom - 40f && y + needed > bottom) newPage()
            drawLines(firstLines, 11f, true, 4f)
            y += 5f
            drawLines(secondLines, 10f, false, 4f)
            y += 12f
        }
        try {
            paragraph(pdfReportTitle(vehicleTitle), 20f, gapAfter = 6f)
            paragraph("$from to $through  |  Distance in $unit", 11f, gapAfter = 2f)
            paragraph(pdfReportSummary(trips, unit), 11f, gapAfter = 14f)
            if (trips.isEmpty()) paragraph("No trips in this date range.", 11f)
            groupPdfTripsByMonth(trips).forEach { (month, monthTrips) ->
                paragraph(month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    13f, bold = true, gapBefore = 4f, gapAfter = 5f)
                monthTrips.forEach { trip -> tripItem(trip, includeVehicle = allVehicles) }
            }
            page?.let(document::finishPage)
            resolver.openOutputStream(uri, "wt")?.use(document::writeTo) ?: error("Couldn't open the selected file.")
        } finally { document.close() }
    }

    private fun parse(uri:Uri):Parsed {
        val text=resolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:error("Couldn't open the selected file.")
        return parseText(text)
    }

    private fun parseText(text: String): Parsed {
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
        require(readings.filter { it.inProgress }.all { it.startValue != null && it.startValue >= 0 && it.value == it.startValue }) {"The backup contains an invalid in-progress trip."}
        require(readings.map{it.id}.distinct().size==readings.size){"The backup contains duplicate readings."}
        val ids=vehicles.map{it.id}.toSet();require(readings.all{it.vehicleId in ids}){"The backup contains readings without a vehicle."}
        readings.groupBy{it.vehicleId}.values.forEach{history->val ordered=history.filterNot { it.inProgress }.sortedWith(compareBy<OdometerReadingEntity>{it.recordedAt}.thenBy{it.id});require(ordered.zipWithNext().all{(a,b)->a.value<=b.value}){"The backup contains invalid odometer history."}}
        val exported=runCatching{Instant.parse(root.getString("exportedAt"))}.getOrElse{error("The backup date is invalid.")}
        return Parsed(vehicles,readings,BackupSummary(vehicles.size,readings.size,exported))
    }
}

private data class Parsed(val vehicles:List<VehicleEntity>,val readings:List<OdometerReadingEntity>,val summary:BackupSummary)
private fun VehicleEntity.json()=JSONObject().put("id",id).put("name",name).putOpt("make",make).putOpt("model",model).putOpt("year",year).putOpt("licensePlate",licensePlate).put("odometerUnit",odometerUnit.name).putOpt("colorKey",colorKey).putOpt("notes",notes).put("createdAt",createdAt.toString()).putOpt("archivedAt",archivedAt?.toString())
private fun OdometerReadingEntity.json()=JSONObject().put("id",id).put("vehicleId",vehicleId).put("value",value).put("recordedAt",recordedAt.toString()).putOpt("note",note).putOpt("name",name).put("hasTime",hasTime).put("inProgress",inProgress).putOpt("startValue",startValue).put("createdAt",createdAt.toString()).putOpt("updatedAt",updatedAt?.toString())
private fun JSONObject.vehicle()=VehicleEntity(getLong("id"),getString("name"),stringOrNull("make"),stringOrNull("model"),if(isNull("year"))null else getInt("year"),stringOrNull("licensePlate"),DistanceUnit.valueOf(getString("odometerUnit")),stringOrNull("colorKey"),stringOrNull("notes"),Instant.parse(getString("createdAt")),stringOrNull("archivedAt")?.let(Instant::parse))
private fun JSONObject.reading()=OdometerReadingEntity(getLong("id"),getLong("vehicleId"),getLong("value"),Instant.parse(getString("recordedAt")),stringOrNull("note"),Instant.parse(getString("createdAt")),stringOrNull("updatedAt")?.let(Instant::parse),stringOrNull("name"),optBoolean("hasTime",true),if(has("startValue") && !isNull("startValue")) getLong("startValue") else null,optBoolean("inProgress",false))
private data class ReportRow(val date: LocalDate, val name: String, val note: String, val odometer: Long, val startValue: Long?, val distance: Long?)
private data class FleetReportRow(val vehicle: String, val unit: String, val date: LocalDate, val name: String, val note: String, val odometer: Long, val startValue: Long?, val distance: Long?)
internal data class PdfReportTrip(val vehicleName: String?, val date: LocalDate, val name: String, val note: String, val startValue: Long?, val finishValue: Long, val distance: Long?)
internal fun pdfReportTitle(vehicleTitle: String) = "TripRabbit Report - $vehicleTitle"
internal fun pdfReportSummary(trips: List<PdfReportTrip>, unit: String) =
    "${trips.size} trips | ${trips.sumOf { it.distance ?: 0 }} $unit recorded"
internal fun pdfReportFirstRow(trip: PdfReportTrip, unit: String, allVehicles: Boolean): String {
    val vehicle = trip.vehicleName.takeIf { allVehicles && !it.isNullOrBlank() }?.let { "$it  |  " }.orEmpty()
    return "$vehicle${trip.date}  |  ${trip.name.ifBlank { "Untitled trip" }}  |  ${trip.distance?.let { "$it $unit" } ?: "—"}"
}
internal fun pdfReportSecondRow(trip: PdfReportTrip, unit: String) =
    "Note: ${trip.note.ifBlank { "—" }}  |  Odometer ${trip.startValue ?: "—"}–${trip.finishValue} $unit"
internal fun groupPdfTripsByMonth(trips: List<PdfReportTrip>): Map<YearMonth, List<PdfReportTrip>> =
    trips.groupBy { YearMonth.from(it.date) }.toSortedMap().mapValues { (_, tripsInMonth) -> tripsInMonth.sortedBy { it.date } }
private fun convertUnit(value: Long, from: DistanceUnit, to: DistanceUnit): Long = when {
    from == to -> value
    from == DistanceUnit.MILES -> (value * 1.609344).roundToLong()
    else -> (value / 1.609344).roundToLong()
}
private fun JSONObject.stringOrNull(key:String)=if(!has(key)||isNull(key))null else getString(key)
private fun csv(value:String)="\"${value.replace("\"","\"\"")}\""
