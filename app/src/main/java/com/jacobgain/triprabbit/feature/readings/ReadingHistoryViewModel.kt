package com.jacobgain.triprabbit.feature.readings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReadingItem(val reading: OdometerReading, val difference: Long?)
data class ReadingHistoryUiState(val loading: Boolean = true, val vehicle: Vehicle? = null, val items: List<ReadingItem> = emptyList(), val vehicles: List<Vehicle> = emptyList())
@HiltViewModel
class ReadingHistoryViewModel @Inject constructor(savedState: SavedStateHandle, vehicles: VehicleRepository, readings: OdometerRepository, private val settings:SettingsRepository) : ViewModel() {
    private val explicitId = savedState.get<String>("vehicleId")?.toLongOrNull()
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState = combine(vehicles.observeActiveVehicles(), if(explicitId!=null)flowOf(explicitId)else settings.observeSettings().map{it.selectedVehicleId}) { active, selected ->
        active to (explicitId ?: selected ?: active.firstOrNull()?.id)
    }.flatMapLatest { (active, id) ->
        if (id == null) return@flatMapLatest flowOf(ReadingHistoryUiState(loading=false, vehicles=active))
        combine(vehicles.observeVehicle(id), readings.observeReadings(id)) { vehicle, descending ->
            val chronological=descending.sortedWith(compareBy<OdometerReading> { it.recordedAt }.thenBy { it.id })
            val differences=chronological.mapIndexed{index,reading->reading.id to (reading.startValue?.let { reading.value - it } ?: if(index==0)null else reading.value-chronological[index-1].value)}.toMap()
            ReadingHistoryUiState(false, vehicle, descending.map { ReadingItem(it, differences[it.id]) }, active)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingHistoryUiState())

    fun selectVehicle(id: Long) { viewModelScope.launch { settings.setSelectedVehicle(id) } }
}
