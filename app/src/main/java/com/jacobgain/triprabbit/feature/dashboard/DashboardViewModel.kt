package com.jacobgain.triprabbit.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.repository.*
import com.jacobgain.triprabbit.core.usecase.CalculateMileageStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val loading: Boolean = true, val vehicle: Vehicle? = null, val readings: List<OdometerReading> = emptyList(),
    val stats: MileageStats = MileageStats(), val vehicles: List<Vehicle> = emptyList(),
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel @Inject constructor(
    private val settings: SettingsRepository, vehicles: VehicleRepository, readings: OdometerRepository,
    private val calculateStats: CalculateMileageStatsUseCase,
) : ViewModel() {
    val uiState = combine(settings.observeSettings(), vehicles.observeActiveVehicles()) { prefs, active -> prefs.selectedVehicleId to active }
        .flatMapLatest { (selected, active) ->
            val vehicle = active.firstOrNull { it.id == selected } ?: active.firstOrNull()
            if (vehicle == null) flowOf(DashboardUiState(false, vehicles = active))
            else readings.observeReadings(vehicle.id).map { history -> DashboardUiState(false, vehicle, history, calculateStats(history), active) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
    fun selectVehicle(id:Long){viewModelScope.launch{settings.setSelectedVehicle(id)}}
}
