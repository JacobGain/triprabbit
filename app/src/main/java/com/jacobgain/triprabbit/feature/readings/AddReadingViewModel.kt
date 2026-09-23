package com.jacobgain.triprabbit.feature.readings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.repository.*
import com.jacobgain.triprabbit.core.usecase.AddOdometerReadingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class AddReadingUiState(
    val vehicle: Vehicle? = null, val previous: OdometerReading? = null, val value: String = "",
    val recordedAt: Instant = java.time.LocalDate.now().atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant(), val note: String = "", val name: String = "", val hasTime: Boolean = false, val saving: Boolean = false, val error: String? = null,
)
sealed interface ReadingEffect { data class Saved(val message: String) : ReadingEffect }

@HiltViewModel
class AddReadingViewModel @Inject constructor(
    savedState: SavedStateHandle, vehicles: VehicleRepository, readings: OdometerRepository,
    private val addReading: AddOdometerReadingUseCase,
) : ViewModel() {
    private val vehicleId = checkNotNull(savedState.get<String>("vehicleId")).toLong()
    private val form = MutableStateFlow(AddReadingUiState())
    val uiState = combine(form, vehicles.observeVehicle(vehicleId), readings.observeLatestReading(vehicleId)) { f, v, latest -> f.copy(vehicle=v, previous=latest) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddReadingUiState())
    private val _effects = Channel<ReadingEffect>(Channel.BUFFERED); val effects = _effects.receiveAsFlow()
    fun valueChanged(value: String) { form.update { it.copy(value=value.filter(Char::isDigit), error=null) } }
    fun noteChanged(value: String) { form.update { it.copy(note=value) } }
    fun nameChanged(value: String) { form.update { it.copy(name=value) } }
    fun timeChanged(value: Boolean) { form.update { it.copy(hasTime=value) } }
    fun dateChanged(value: Instant) { form.update { it.copy(recordedAt=value, error=null) } }
    fun save() = viewModelScope.launch {
        val s = form.value; val value = s.value.toLongOrNull()
        if (value == null) { form.update { it.copy(error="Enter a valid odometer reading.") }; return@launch }
        form.update { it.copy(saving=true) }
        runCatching { addReading(vehicleId, value, s.recordedAt, s.note, s.name, s.hasTime) }
            .onSuccess { _effects.send(ReadingEffect.Saved("Reading added")) }
            .onFailure { e -> form.update { it.copy(saving=false, error=e.message ?: "Couldn't save the reading. Try again.") } }
    }
}
