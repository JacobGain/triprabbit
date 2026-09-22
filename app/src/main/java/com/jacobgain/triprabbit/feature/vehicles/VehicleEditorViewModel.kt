package com.jacobgain.triprabbit.feature.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.repository.VehicleRepository
import com.jacobgain.triprabbit.core.usecase.CreateVehicleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class VehicleEditorUiState(
    val loading: Boolean = false, val name: String = "", val make: String = "", val model: String = "",
    val year: String = "", val licensePlate: String = "", val unit: DistanceUnit = DistanceUnit.KILOMETERS,
    val colorKey: String = "", val notes: String = "", val initialReading: String = "", val editing: Boolean = false,
    val error: String? = null, val saving: Boolean = false,
)
sealed interface VehicleEditorEffect { data class Saved(val vehicleId: Long) : VehicleEditorEffect }

@HiltViewModel
class VehicleEditorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: VehicleRepository,
    private val createVehicle: CreateVehicleUseCase,
) : ViewModel() {
    private val vehicleId = savedState.get<String>("vehicleId")?.toLongOrNull()
    private val _state = MutableStateFlow(VehicleEditorUiState(loading = vehicleId != null, editing = vehicleId != null))
    val uiState = _state.asStateFlow()
    private val _effects = Channel<VehicleEditorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var original: Vehicle? = null

    init { if (vehicleId != null) viewModelScope.launch {
        repository.observeVehicle(vehicleId).filterNotNull().first().also { v ->
            original = v
            _state.value = VehicleEditorUiState(name=v.name, make=v.make.orEmpty(), model=v.model.orEmpty(), year=v.year?.toString().orEmpty(), licensePlate=v.licensePlate.orEmpty(), unit=v.odometerUnit, colorKey=v.colorKey.orEmpty(), notes=v.notes.orEmpty(), editing=true)
        }
    } }

    fun update(transform: (VehicleEditorUiState) -> VehicleEditorUiState) { _state.update(transform) }
    fun save() { viewModelScope.launch {
        val s = _state.value
        val year = s.year.takeIf { it.isNotBlank() }?.toIntOrNull()
        val initial = s.initialReading.toLongOrNull()
        val error = when { s.name.isBlank() -> "Vehicle name is required."; s.year.isNotBlank() && year == null -> "Enter a valid year."; !s.editing && initial == null -> "Enter a valid current odometer reading."; else -> null }
        if (error != null) { _state.update { it.copy(error=error) }; return@launch }
        _state.update { it.copy(saving=true, error=null) }
        runCatching {
            if (s.editing) {
                val old = requireNotNull(original)
                repository.updateVehicle(old.copy(name=s.name.trim(), make=s.make.blankNull(), model=s.model.blankNull(), year=year, licensePlate=s.licensePlate.blankNull(), colorKey=s.colorKey.blankNull(), notes=s.notes.blankNull()))
                old.id
            } else createVehicle(VehicleInput(s.name, s.make.blankNull(), s.model.blankNull(), year, odometerUnit=s.unit), requireNotNull(initial), Instant.now())
        }.onSuccess { _effects.send(VehicleEditorEffect.Saved(it)) }.onFailure { e -> _state.update { it.copy(saving=false, error=e.message ?: "Couldn't save the vehicle. Try again.") } }
    } }
}
private fun String.blankNull() = trim().takeIf { it.isNotEmpty() }
