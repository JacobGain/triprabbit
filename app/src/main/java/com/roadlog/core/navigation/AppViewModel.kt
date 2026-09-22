package com.roadlog.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadlog.core.model.*
import com.roadlog.core.repository.SettingsRepository
import com.roadlog.core.repository.VehicleRepository
import com.roadlog.core.repository.OdometerRepository
import com.roadlog.core.usecase.ResolveSelectedVehicleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUiState(
    val loading: Boolean = true,
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: Long? = null,
    val firstLaunchComplete: Boolean = false,
    val latestReadings: Map<Long, OdometerReading> = emptyMap(),
    val settings: AppSettings = AppSettings(),
)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppViewModel @Inject constructor(
    vehicles: VehicleRepository,
    readings: OdometerRepository,
    private val settings: SettingsRepository,
    private val resolver: ResolveSelectedVehicleUseCase,
) : ViewModel() {
    private val activeAndLatest = vehicles.observeActiveVehicles().flatMapLatest { active ->
        if (active.isEmpty()) flowOf(active to emptyMap())
        else combine(active.map { vehicle -> readings.observeLatestReading(vehicle.id).map { vehicle.id to it } }) { pairs ->
            active to pairs.mapNotNull { (id, reading) -> reading?.let { id to it } }.toMap()
        }
    }
    val uiState = combine(activeAndLatest, settings.observeSettings()) { data, prefs -> data to prefs }
        .mapLatest { (data, prefs) ->
            val (active, latest) = data
            AppUiState(false, active, resolver(active, prefs.selectedVehicleId), prefs.firstLaunchComplete, latest, prefs)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    fun selectVehicle(id: Long) { viewModelScope.launch { settings.setSelectedVehicle(id) } }
}
