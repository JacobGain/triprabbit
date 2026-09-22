package com.roadlog.feature.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadlog.core.model.*
import com.roadlog.core.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleDetailsUiState(
    val loading: Boolean = true,
    val vehicle: Vehicle? = null,
    val readings: List<OdometerReading> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null,
)
@HiltViewModel
class VehicleDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val vehicles: VehicleRepository,
    readings: OdometerRepository,
) : ViewModel() {
    private val id = checkNotNull(savedState.get<String>("vehicleId")).toLong()
    private val actionState = MutableStateFlow<Pair<Boolean, String?>>(false to null)
    val uiState = combine(vehicles.observeVehicle(id), readings.observeReadings(id), actionState) { v, r, action ->
        VehicleDetailsUiState(false, v, r, action.first, action.second)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehicleDetailsUiState())
    private val _gone = Channel<Unit>(Channel.BUFFERED)
    val gone = _gone.receiveAsFlow()
    fun archive() = remove("Couldn't archive the vehicle. Try again.") { vehicles.archiveVehicle(id) }
    fun delete() = remove("Couldn't delete the vehicle. Try again.") { vehicles.deleteVehicle(id) }
    fun clearError() { actionState.update { it.copy(second = null) } }

    private fun remove(message: String, operation: suspend () -> Unit) = viewModelScope.launch {
        actionState.value = true to null
        runCatching { operation() }
            .onSuccess { _gone.send(Unit) }
            .onFailure { actionState.value = false to message }
    }
}
