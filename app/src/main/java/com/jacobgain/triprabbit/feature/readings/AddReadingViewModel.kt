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
    val vehicle: Vehicle? = null, val previous: OdometerReading? = null, val startValue: String = "", val value: String = "",
    val recordedAt: Instant = java.time.LocalDate.now().atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant(), val note: String = "", val name: String = "", val hasTime: Boolean = false, val saving: Boolean = false, val error: String? = null,
    val vehicles: List<Vehicle> = emptyList(), val hasInProgressTrip: Boolean = false,
)
sealed interface ReadingEffect { data class Saved(val message: String) : ReadingEffect }

@HiltViewModel
class AddReadingViewModel @Inject constructor(
    savedState: SavedStateHandle, vehicles: VehicleRepository, readings: OdometerRepository,
    private val addReading: AddOdometerReadingUseCase, private val settings: SettingsRepository,
) : ViewModel() {
    private val vehicleId = checkNotNull(savedState.get<String>("vehicleId")).toLong()
    private val form = MutableStateFlow(AddReadingUiState())
    private val selectedVehicleId = MutableStateFlow(vehicleId)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState = combine(vehicles.observeActiveVehicles(), selectedVehicleId) { active, selected -> active to selected }
        .flatMapLatest { (active, selected) ->
            combine(form, vehicles.observeVehicle(selected), readings.observeReadings(selected)) { f, v, history ->
                f.copy(vehicle = v, previous = history.firstOrNull { !it.inProgress }, vehicles = active,
                    hasInProgressTrip = history.any { it.inProgress })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddReadingUiState())
    private val _effects = Channel<ReadingEffect>(Channel.BUFFERED); val effects = _effects.receiveAsFlow()
    fun selectVehicle(id: Long) {
        if (selectedVehicleId.value == id) return
        selectedVehicleId.value = id
        form.update { it.copy(startValue = "", value = "", error = null) }
    }
    fun startValueChanged(value: String) { form.update { it.copy(startValue=value.filter(Char::isDigit), value=value.filter(Char::isDigit), error=null) } }
    fun valueChanged(value: String) { form.update { it.copy(value=value.filter(Char::isDigit), error=null) } }
    fun noteChanged(value: String) { form.update { it.copy(note=value) } }
    fun nameChanged(value: String) { form.update { it.copy(name=value) } }
    fun timeChanged(value: Boolean) { form.update { it.copy(hasTime=value) } }
    fun dateChanged(value: Instant) { form.update { it.copy(recordedAt=value, error=null) } }
    fun save(inProgress: Boolean = false) = viewModelScope.launch {
        val s = uiState.value
        if (s.name.isBlank()) { form.update { it.copy(error = "Trip name is required.") }; return@launch }
        val start = s.startValue.toLongOrNull() ?: s.previous?.value
        val value = if (inProgress) start else s.value.toLongOrNull() ?: start
        if (value == null || start == null) { form.update { it.copy(error="Enter both odometer readings.") }; return@launch }
        form.update { it.copy(saving=true) }
        val selectedId = s.vehicle?.id ?: vehicleId
        runCatching { addReading(selectedId, start, value, s.recordedAt, s.note, s.name, s.hasTime, inProgress) }
            .onSuccess {
                settings.setLastUsedTripVehicle(selectedId)
                _effects.send(ReadingEffect.Saved(if (inProgress) "Trip started" else "Reading added"))
            }
            .onFailure { e -> form.update { it.copy(saving=false, error=e.message ?: "Couldn't save the reading. Try again.") } }
    }
}
