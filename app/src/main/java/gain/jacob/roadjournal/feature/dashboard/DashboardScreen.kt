package gain.jacob.roadjournal.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gain.jacob.roadjournal.core.designsystem.component.OdometerDisplay
import gain.jacob.roadjournal.core.model.*
import gain.jacob.roadjournal.core.util.*
import gain.jacob.roadjournal.feature.vehicles.subtitle

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DashboardScreen(onAdd:(Long)->Unit,onHistory:(Long)->Unit,onManageVehicles:()->Unit,onStatistics:(Long)->Unit,viewModel:DashboardViewModel=hiltViewModel()) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value; val vehicle=state.vehicle
    var selectorOpen by remember{mutableStateOf(false)}
    if(selectorOpen)ModalBottomSheet(onDismissRequest={selectorOpen=false}){Column(Modifier.fillMaxWidth().padding(bottom=24.dp)){Text("Select vehicle",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(20.dp));state.vehicles.forEach{v->ListItem(headlineContent={Text(v.name)},supportingContent={v.subtitle()?.let{Text(it)}},trailingContent={if(v.id==vehicle?.id)Text("Current")},modifier=Modifier.clickable{viewModel.selectVehicle(v.id);selectorOpen=false})};TextButton(onClick={selectorOpen=false;onManageVehicles()},modifier=Modifier.padding(horizontal=12.dp)){Text("Manage vehicles")}}}
    if(vehicle==null){gain.jacob.roadjournal.core.designsystem.component.EmptyState("No vehicles","Add a vehicle to begin.","Add Vehicle",onManageVehicles);return}
    val latest=state.readings.firstOrNull();val previous=state.readings.getOrNull(1)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{Text("RoadJournal",style=MaterialTheme.typography.headlineMedium)}
        item{TextButton(onClick={selectorOpen=true}){Column{Text("${vehicle.name} ▾",style=MaterialTheme.typography.titleLarge);vehicle.subtitle()?.let{Text(it)}}}}
        item{ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(24.dp)){
            Text("Current odometer",style=MaterialTheme.typography.labelLarge)
            if(latest!=null){OdometerDisplay(latest.value,vehicle.odometerUnit);Text("Updated ${latest.recordedAt.displayDate()}")}else Text("No readings")
            Spacer(Modifier.height(20.dp));Button(onClick={onAdd(vehicle.id)},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text("Add Reading")}
        }}}
        item{DashboardStats(previous?.let{latest?.value?.minus(it.value)}?:0,state.stats.last30Days,state.stats.totalTracked,vehicle.odometerUnit)}
        if(state.readings.size>1)item{MileageHistoryChart(state.readings,Modifier.fillMaxWidth().height(140.dp));TextButton(onClick={onStatistics(vehicle.id)}){Text("View statistics")}}
        item{Text("Recent activity",style=MaterialTheme.typography.titleLarge)}
        items(minOf(5,state.readings.size)){index->val reading=state.readings[index];val older=state.readings.getOrNull(index+1);ListItem(headlineContent={Text("${reading.value.grouped()} ${vehicle.odometerUnit.abbreviation}")},overlineContent={Text(reading.recordedAt.displayDate())},trailingContent={older?.let{Text("+${(reading.value-it.value).grouped()}")}})}
        item{OutlinedButton(onClick={onHistory(vehicle.id)},modifier=Modifier.fillMaxWidth()){Text("View History")}}
    }
}

@Composable private fun DashboardStats(sinceLast:Long,last30Days:Long,totalTracked:Long,unit:DistanceUnit){
    val largeText=LocalDensity.current.fontScale>=1.3f
    if(largeText) Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        StatCard("Since last",sinceLast,unit,Modifier.fillMaxWidth());StatCard("Last 30 days",last30Days,unit,Modifier.fillMaxWidth());StatCard("Total tracked",totalTracked,unit,Modifier.fillMaxWidth())
    } else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        StatCard("Since last",sinceLast,unit,Modifier.weight(1f));StatCard("Last 30 days",last30Days,unit,Modifier.weight(1f));StatCard("Total tracked",totalTracked,unit,Modifier.weight(1f))
    }
}

@Composable private fun StatCard(label:String,value:Long,unit:DistanceUnit,modifier:Modifier=Modifier){OutlinedCard(modifier){Column(Modifier.padding(12.dp)){Text(label,style=MaterialTheme.typography.labelMedium);Text(value.grouped(),style=MaterialTheme.typography.titleMedium);Text(unit.abbreviation,style=MaterialTheme.typography.labelSmall)}}}

@Composable
fun MileageHistoryChart(readings:List<OdometerReading>,modifier:Modifier=Modifier){
    val color=MaterialTheme.colorScheme.primary;val sorted=readings.sortedBy{it.recordedAt};val min=sorted.minOfOrNull{it.value}?:0;val range=(sorted.maxOfOrNull{it.value}?:min)-min
    Canvas(modifier){if(sorted.size<2)return@Canvas;val path=Path();sorted.forEachIndexed{index,r->val x=size.width*index/(sorted.lastIndex.coerceAtLeast(1));val y=size.height-(if(range==0L)size.height/2 else size.height*(r.value-min)/range.toFloat());if(index==0)path.moveTo(x,y)else path.lineTo(x,y)};drawPath(path,color,style=Stroke(width=4f,cap=StrokeCap.Round))}
}
