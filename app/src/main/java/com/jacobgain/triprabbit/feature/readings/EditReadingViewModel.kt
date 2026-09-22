package com.jacobgain.triprabbit.feature.readings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobgain.triprabbit.core.model.OdometerReading
import com.jacobgain.triprabbit.core.repository.OdometerRepository
import com.jacobgain.triprabbit.core.repository.SettingsRepository
import com.jacobgain.triprabbit.core.usecase.EditOdometerReadingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class EditReadingUiState(val loading: Boolean = true, val reading: OdometerReading? = null, val value: String = "", val recordedAt: Instant = Instant.now(), val note: String = "", val saving: Boolean = false, val error: String? = null, val confirmDeletion:Boolean = true)
@HiltViewModel
class EditReadingViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: OdometerRepository, settings:SettingsRepository, private val editReading: EditOdometerReadingUseCase) : ViewModel() {
    private val id = checkNotNull(savedState.get<String>("readingId")).toLong()
    private val _state = MutableStateFlow(EditReadingUiState()); val uiState = _state.asStateFlow()
    private val _effects = Channel<ReadingEffect>(Channel.BUFFERED); val effects = _effects.receiveAsFlow()
    init { viewModelScope.launch {
        combine(repository.observeReading(id),settings.observeSettings()){reading,prefs->reading to prefs}.first().let { (reading,prefs) ->
            _state.value = if(reading==null) EditReadingUiState(loading=false,error="Reading not found.",confirmDeletion=prefs.confirmReadingDeletion)
            else EditReadingUiState(false,reading,reading.value.toString(),reading.recordedAt,reading.note.orEmpty(),confirmDeletion=prefs.confirmReadingDeletion)
        }
    } }
    fun valueChanged(v: String) { _state.update { it.copy(value=v.filter(Char::isDigit), error=null) } }
    fun noteChanged(v: String) { _state.update { it.copy(note=v) } }
    fun dateChanged(v: Instant) { _state.update { it.copy(recordedAt=v, error=null) } }
    fun save() = viewModelScope.launch { val s=_state.value; val value=s.value.toLongOrNull(); if(value==null){_state.update{it.copy(error="Enter a valid odometer reading.")};return@launch}; _state.update{it.copy(saving=true)}; runCatching { editReading(id,value,s.recordedAt,s.note) }.onSuccess { _effects.send(ReadingEffect.Saved("Reading updated")) }.onFailure { e->_state.update{it.copy(saving=false,error=e.message ?: "Couldn't save the reading. Try again.")} } }
    fun delete() = viewModelScope.launch {
        _state.update{it.copy(saving=true,error=null)}
        runCatching{repository.deleteReading(id)}
            .onSuccess{_effects.send(ReadingEffect.Saved("Reading deleted"))}
            .onFailure{_state.update{state->state.copy(saving=false,error="Couldn't delete the reading. Try again.")}}
    }
}
