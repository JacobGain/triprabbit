package com.jacobgain.triprabbit.feature.readings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobgain.triprabbit.core.model.OdometerReading
import com.jacobgain.triprabbit.core.repository.OdometerRepository
import com.jacobgain.triprabbit.core.repository.VehicleRepository
import com.jacobgain.triprabbit.core.repository.SettingsRepository
import com.jacobgain.triprabbit.core.usecase.EditOdometerReadingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class EditReadingUiState(val loading: Boolean = true, val reading: OdometerReading? = null, val startValue: String = "", val value: String = "", val recordedAt: Instant = Instant.now(), val note: String = "", val name: String = "", val hasTime: Boolean = false, val saving: Boolean = false, val error: String? = null, val confirmDeletion:Boolean = true, val unit: String = "")
@HiltViewModel
class EditReadingViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: OdometerRepository, vehicles: VehicleRepository, settings:SettingsRepository, private val editReading: EditOdometerReadingUseCase) : ViewModel() {
    private val id = checkNotNull(savedState.get<String>("readingId")).toLong()
    private val _state = MutableStateFlow(EditReadingUiState()); val uiState = _state.asStateFlow()
    private val _effects = Channel<ReadingEffect>(Channel.BUFFERED); val effects = _effects.receiveAsFlow()
    init { viewModelScope.launch {
        combine(repository.observeReading(id),settings.observeSettings()){reading,prefs->reading to prefs}.first().let { (reading,prefs) ->
            _state.value = if(reading==null) EditReadingUiState(loading=false,error="Reading not found.",confirmDeletion=prefs.confirmReadingDeletion)
            else EditReadingUiState(loading=false,reading=reading,startValue=(reading.startValue ?: repository.surrounding(reading.vehicleId, reading.recordedAt, id).first?.value ?: reading.value).toString(),value=reading.value.toString(),recordedAt=reading.recordedAt,note=reading.note.orEmpty(),name=reading.name.orEmpty(),hasTime=reading.hasTime,confirmDeletion=prefs.confirmReadingDeletion,unit=vehicles.getVehicle(reading.vehicleId)?.odometerUnit?.abbreviation.orEmpty())
        }
    } }
    fun valueChanged(v: String) { _state.update { it.copy(value=v.filter(Char::isDigit), error=null) } }
    fun startValueChanged(v: String) { _state.update { it.copy(startValue=v.filter(Char::isDigit), error=null) } }
    fun noteChanged(v: String) { _state.update { it.copy(note=v) } }
    fun nameChanged(v: String) { _state.update { it.copy(name=v) } }
    fun timeChanged(v: Boolean) { _state.update { it.copy(hasTime=v) } }
    fun dateChanged(v: Instant) { _state.update { it.copy(recordedAt=v, error=null) } }
    fun save() = persist(inProgress = false)
    fun saveInProgress() = persist(inProgress = true)
    private fun persist(inProgress: Boolean) = viewModelScope.launch {
        val s = _state.value
        if (s.name.isBlank()) { _state.update { it.copy(error = "Trip name is required.") }; return@launch }
        val start = s.startValue.toLongOrNull()
        val value = if (inProgress) start else s.value.toLongOrNull()
        if (value == null) { _state.update { it.copy(error = "Enter a valid odometer reading.") }; return@launch }
        _state.update { it.copy(saving = true, error = null) }
        runCatching { editReading(id, value, s.recordedAt, s.note, s.name, s.hasTime, start, inProgress) }
            .onSuccess {
                val message = when {
                    inProgress -> "Trip kept in progress"
                    s.reading?.inProgress == true -> "Trip finished"
                    else -> "Reading updated"
                }
                _effects.send(ReadingEffect.Saved(message))
            }
            .onFailure { e -> _state.update { it.copy(saving = false, error = e.message ?: "Couldn't save the reading. Try again.") } }
    }
    fun delete() = viewModelScope.launch {
        _state.update{it.copy(saving=true,error=null)}
        runCatching{repository.deleteReading(id)}
            .onSuccess{_effects.send(ReadingEffect.Saved("Reading deleted"))}
            .onFailure{_state.update{state->state.copy(saving=false,error="Couldn't delete the reading. Try again.")}}
    }
}
