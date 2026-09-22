package com.jacobgain.triprabbit.feature.readings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ReadingItem(val reading: OdometerReading, val difference: Long?)
data class ReadingHistoryUiState(val loading: Boolean = true, val vehicle: Vehicle? = null, val items: List<ReadingItem> = emptyList())
@HiltViewModel
class ReadingHistoryViewModel @Inject constructor(savedState: SavedStateHandle, vehicles: VehicleRepository, readings: OdometerRepository, settings:SettingsRepository) : ViewModel() {
    private val explicitId = savedState.get<String>("vehicleId")?.toLongOrNull()
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState = (if(explicitId!=null)flowOf(explicitId)else settings.observeSettings().map{it.selectedVehicleId}.filterNotNull()).flatMapLatest{id->
        combine(vehicles.observeVehicle(id), readings.observeReadings(id)) { vehicle, descending ->
            val chronological=descending.sortedWith(compareBy<OdometerReading> { it.recordedAt }.thenBy { it.id })
            val differences=chronological.mapIndexed{index,reading->reading.id to if(index==0)null else reading.value-chronological[index-1].value}.toMap()
            ReadingHistoryUiState(false, vehicle, descending.map { ReadingItem(it, differences[it.id]) })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingHistoryUiState())
}
