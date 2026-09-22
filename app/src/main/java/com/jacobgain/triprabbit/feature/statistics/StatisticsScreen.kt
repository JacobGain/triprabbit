package com.jacobgain.triprabbit.feature.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.repository.*
import com.jacobgain.triprabbit.core.usecase.CalculateMileageStatsUseCase
import com.jacobgain.triprabbit.core.util.grouped
import com.jacobgain.triprabbit.feature.dashboard.MileageHistoryChart
import com.jacobgain.triprabbit.core.designsystem.component.EmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class StatisticsUiState(val loading:Boolean=true,val vehicle:Vehicle?=null,val readings:List<OdometerReading> = emptyList(),val stats:MileageStats=MileageStats())
@HiltViewModel class StatisticsViewModel @Inject constructor(saved:SavedStateHandle,vehicles:VehicleRepository,readings:OdometerRepository,calculate:CalculateMileageStatsUseCase):ViewModel(){private val id=checkNotNull(saved.get<String>("vehicleId")).toLong();val uiState=combine(vehicles.observeVehicle(id),readings.observeReadings(id)){v,r->StatisticsUiState(false,v,r,calculate(r))}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),StatisticsUiState())}

@OptIn(ExperimentalMaterial3Api::class) @Composable fun StatisticsScreen(onBack:()->Unit,viewModel:StatisticsViewModel=hiltViewModel()){val s=viewModel.uiState.collectAsStateWithLifecycle().value;val v=s.vehicle;Scaffold(topBar={TopAppBar(title={Text("Statistics")},navigationIcon={TextButton(onClick=onBack){Text("Back")}})}){p->if(s.loading)Box(Modifier.fillMaxSize().padding(p),contentAlignment=androidx.compose.ui.Alignment.Center){CircularProgressIndicator()}else if(v==null)Box(Modifier.fillMaxSize().padding(p)){EmptyState("Vehicle not found","Statistics are unavailable because this vehicle no longer exists.","Go Back",onBack)}else LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text(v.name,style=MaterialTheme.typography.headlineSmall)};item{MileageHistoryChart(s.readings,Modifier.fillMaxWidth().height(200.dp))};listOf("Current odometer" to s.stats.current,"Total tracked" to s.stats.totalTracked,"Last 30 days" to s.stats.last30Days,"This year" to s.stats.currentYear,"Average per month" to s.stats.averagePerMonth).forEach{(label,value)->item{OutlinedCard(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(label);Text(value?.let{"${it.grouped()} ${v.odometerUnit.abbreviation}"}?:"—",style=MaterialTheme.typography.titleMedium)}}}};item{Text("${s.stats.readingCount} readings")}}}}
