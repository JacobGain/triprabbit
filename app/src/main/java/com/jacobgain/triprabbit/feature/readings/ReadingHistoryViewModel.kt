package com.jacobgain.triprabbit.feature.readings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ReadingItem(
    val reading: OdometerReading,
    val difference: Long?,
    val vehicleName: String? = null,
    val unit: String = "",
)

data class ReadingHistoryUiState(
    val loading: Boolean = true,
    val vehicle: Vehicle? = null,
    val items: List<ReadingItem> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val pinInProgressTrips: Boolean = true,
)

@HiltViewModel
class ReadingHistoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    vehicles: VehicleRepository,
    readings: OdometerRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val explicitId = savedState.get<String>("vehicleId")?.toLongOrNull()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState = combine(vehicles.observeActiveVehicles(), settings.observeSettings()) { active, preferences -> active to preferences }
        .flatMapLatest { (active, preferences) ->
            val available = if (explicitId == null) active else active.filter { it.id == explicitId }
            val selected = available.firstOrNull { it.id == preferences.selectedVehicleId } ?: available.firstOrNull()
            if (available.isEmpty()) {
                flowOf(ReadingHistoryUiState(loading = false, vehicles = available, pinInProgressTrips = preferences.pinInProgressTrips))
            } else {
                combine(available.map { readings.observeReadings(it.id) }) { histories ->
                    val items = available.zip(histories).flatMap { (vehicle, descending) ->
                        val chronological = descending.filterNot { it.inProgress }
                            .sortedWith(compareBy<OdometerReading> { it.recordedAt }.thenBy { it.id })
                        val differences = chronological.mapIndexed { index, reading ->
                            reading.id to (reading.startValue?.let { reading.value - it }
                                ?: if (index == 0) null else reading.value - chronological[index - 1].value)
                        }.toMap()
                        descending.map { reading ->
                            ReadingItem(reading, differences[reading.id], vehicle.name, vehicle.odometerUnit.abbreviation)
                        }
                    }.sortedWith(compareByDescending<ReadingItem> { it.reading.recordedAt }.thenByDescending { it.reading.id })
                    ReadingHistoryUiState(false, selected, items, available, preferences.pinInProgressTrips)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingHistoryUiState())
}
