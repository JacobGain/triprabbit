package com.roadlog.feature.vehicles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roadlog.core.designsystem.component.*
import com.roadlog.core.model.*
import com.roadlog.core.util.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("RoadLog", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp)); Text("Keep a simple history of your vehicle's mileage.", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(32.dp)); Button(onClick=onGetStarted, modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Get Started") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleEditorScreen(onSaved: (Long) -> Unit, onBack: () -> Unit, viewModel: VehicleEditorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.effects.collectLatest { if (it is VehicleEditorEffect.Saved) onSaved(it.vehicleId) } }
    Scaffold(topBar={ TopAppBar(title={Text(if(state.editing) "Edit Vehicle" else "Create Vehicle")}, navigationIcon={TextButton(onClick=onBack){Text("Back")}}) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=20.dp), verticalArrangement=Arrangement.spacedBy(12.dp), contentPadding=PaddingValues(bottom=32.dp)) {
            item { OutlinedTextField(state.name, { v->viewModel.update{it.copy(name=v,error=null)} }, label={Text("Vehicle name")}, singleLine=true, modifier=Modifier.fillMaxWidth()) }
            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { DistanceUnit.entries.forEach { unit -> FilterChip(selected=state.unit==unit, onClick={if(!state.editing)viewModel.update{it.copy(unit=unit)}}, enabled=!state.editing, label={Text(if(unit==DistanceUnit.KILOMETERS) "Kilometres" else "Miles")}) } } }
            if (!state.editing) item { OutlinedTextField(state.initialReading, {v->viewModel.update{it.copy(initialReading=v.filter(Char::isDigit),error=null)}}, label={Text("Current odometer")}, suffix={Text(state.unit.abbreviation)}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), singleLine=true, modifier=Modifier.fillMaxWidth()) }
            item { Text("Vehicle details", style=MaterialTheme.typography.titleMedium) }
            item { OutlinedTextField(state.make,{v->viewModel.update{it.copy(make=v)}},label={Text("Make (optional)")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
            item { OutlinedTextField(state.model,{v->viewModel.update{it.copy(model=v)}},label={Text("Model (optional)")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
            item { OutlinedTextField(state.year,{v->viewModel.update{it.copy(year=v.filter(Char::isDigit))}},label={Text("Year (optional)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth()) }
            if(state.editing) {
                item { OutlinedTextField(state.licensePlate,{v->viewModel.update{it.copy(licensePlate=v)}},label={Text("License plate")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
                item { OutlinedTextField(state.colorKey,{v->viewModel.update{it.copy(colorKey=v)}},label={Text("Colour identifier")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
                item { OutlinedTextField(state.notes,{v->viewModel.update{it.copy(notes=v)}},label={Text("Notes")},minLines=3,modifier=Modifier.fillMaxWidth()) }
            }
            state.error?.let { error -> item { Text(error, color=MaterialTheme.colorScheme.error) } }
            item { Button(onClick=viewModel::save, enabled=!state.saving, modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text(if(state.editing) "Save Changes" else "Create Vehicle") } }
        }
    }
}

@Composable
fun VehicleListScreen(vehicles: List<Vehicle>, selectedId: Long?, latestValues: Map<Long, Long>, density:DisplayDensity, onSelect: (Long)->Unit, onOpen:(Long)->Unit, onAdd:()->Unit) {
    Scaffold(floatingActionButton={ExtendedFloatingActionButton(onClick=onAdd,modifier=Modifier.semantics{contentDescription="Add Vehicle"},icon={Icon(Icons.Filled.Add,contentDescription=null)},text={Text("Add Vehicle")})}) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding=PaddingValues(16.dp,16.dp,16.dp,88.dp), verticalArrangement=Arrangement.spacedBy(if(density==DisplayDensity.COMPACT) 6.dp else 12.dp)) {
            item { Text("Vehicles", style=MaterialTheme.typography.headlineMedium) }
            if(vehicles.isEmpty()) item { EmptyState("No vehicles","Add a vehicle to start tracking mileage.","Add Vehicle",onAdd) }
            items(vehicles,key={it.id}) { vehicle ->
                ElevatedCard(Modifier.fillMaxWidth().clickable{onOpen(vehicle.id)}) { Column(Modifier.padding(if(density==DisplayDensity.COMPACT) 10.dp else 18.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(vehicle.name,style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f)); if(vehicle.id==selectedId) Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.small){Text("Selected",style=MaterialTheme.typography.labelLarge,modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp))}}
                    vehicle.subtitle()?.let { Text(it,style=MaterialTheme.typography.bodyMedium) }
                    latestValues[vehicle.id]?.let { Text("${it.grouped()} ${vehicle.odometerUnit.abbreviation}",style=MaterialTheme.typography.titleMedium) }
                    if(vehicle.id!=selectedId) TextButton(onClick={onSelect(vehicle.id)}){Text("Make current")}
                } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsScreen(onBack:()->Unit,onAdd:(Long)->Unit,onHistory:(Long)->Unit,onEdit:(Long)->Unit,onGone:()->Unit,viewModel:VehicleDetailsViewModel=hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle(); val v=state.vehicle
    LaunchedEffect(Unit) { viewModel.gone.collectLatest { onGone() } }
    var confirmDelete by remember{mutableStateOf(false)}
    if(confirmDelete&&v!=null) DestructiveConfirmationDialog("Delete ${v.name}?","This permanently deletes the vehicle and all ${state.readings.size} odometer readings.",{confirmDelete=false;viewModel.delete()},{confirmDelete=false})
    state.error?.let{AlertDialog(onDismissRequest=viewModel::clearError,title={Text("Couldn't complete operation")},text={Text(it)},confirmButton={TextButton(onClick=viewModel::clearError){Text("OK")}})}
    Scaffold(topBar={TopAppBar(title={Text(v?.name.orEmpty())},navigationIcon={TextButton(onClick=onBack){Text("Back")}})}){padding->
        if(state.loading){Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.Center){CircularProgressIndicator()}}
        else if(v==null){Box(Modifier.fillMaxSize().padding(padding)){EmptyState("Vehicle not found","It may have been archived or deleted.","Go Back",onBack)}}
        else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            v.subtitle()?.let{item{Text(it,style=MaterialTheme.typography.titleMedium)}}
            item{ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp)){Text("Current");state.readings.firstOrNull()?.let{OdometerDisplay(it.value,v.odometerUnit)}?:Text("No readings")}}}
            if(state.readings.isNotEmpty()) item { val oldest=state.readings.last(); val latest=state.readings.first(); Column { Text("Tracked: ${(latest.value-oldest.value).grouped()} ${v.odometerUnit.abbreviation}");Text("First reading: ${oldest.value.grouped()} ${v.odometerUnit.abbreviation} · ${oldest.recordedAt.displayDate()}");Text("Entries: ${state.readings.size}") } }
            item{Button(onClick={onAdd(v.id)},enabled=!state.busy,modifier=Modifier.fillMaxWidth()){Text("Add Reading")}}
            item{OutlinedButton(onClick={onHistory(v.id)},enabled=!state.busy,modifier=Modifier.fillMaxWidth()){Text("View History")}}
            item{OutlinedButton(onClick={onEdit(v.id)},enabled=!state.busy,modifier=Modifier.fillMaxWidth()){Text("Edit Vehicle")}}
            item{HorizontalDivider();TextButton(onClick=viewModel::archive,enabled=!state.busy,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)){Text("Archive Vehicle")};TextButton(onClick={confirmDelete=true},enabled=!state.busy,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)){Text("Delete Vehicle")};if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())}
        }
    }
}

fun Vehicle.subtitle():String?=listOfNotNull(year?.toString(),make,model).takeIf{it.isNotEmpty()}?.joinToString(" ")
