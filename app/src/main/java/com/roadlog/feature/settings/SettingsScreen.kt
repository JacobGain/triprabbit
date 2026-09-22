package com.roadlog.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roadlog.core.model.*
import com.roadlog.core.repository.*
import com.roadlog.core.util.displayDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import com.roadlog.BuildConfig

data class SettingsUiState(val settings:AppSettings=AppSettings(),val selectedVehicle:Vehicle?=null,val importSummary:BackupSummary?=null,val busy:Boolean=false,val error:String?=null)
sealed interface SettingsEffect{data class Message(val value:String):SettingsEffect}
@HiltViewModel class SettingsViewModel @Inject constructor(private val settingsRepository:SettingsRepository,private val data:DataRepository,vehicles:VehicleRepository):ViewModel(){
    private val local=MutableStateFlow(SettingsUiState());private var importUri:Uri?=null
    val uiState=combine(settingsRepository.observeSettings(),vehicles.observeActiveVehicles(),local){settings,all,state->state.copy(settings=settings,selectedVehicle=all.firstOrNull{it.id==settings.selectedVehicleId})}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),SettingsUiState())
    private val _effects=Channel<SettingsEffect>(Channel.BUFFERED);val effects=_effects.receiveAsFlow()
    fun theme(v:ThemeMode)=launch{settingsRepository.setThemeMode(v)};fun accent(v:AccentTheme)=launch{settingsRepository.setAccentTheme(v)};fun dynamic(v:Boolean)=launch{settingsRepository.setDynamicColor(v)};fun amoled(v:Boolean)=launch{settingsRepository.setAmoledBlack(v)};fun density(v:DisplayDensity)=launch{settingsRepository.setDisplayDensity(v)};fun confirmDelete(v:Boolean)=launch{settingsRepository.setConfirmReadingDeletion(v)}
    fun export(uri:Uri)=work("Backup exported"){data.exportBackup(uri)};fun csv(uri:Uri){val id=uiState.value.selectedVehicle?.id?:return;work("CSV exported"){data.exportVehicleCsv(uri,id)}}
    fun inspect(uri:Uri){viewModelScope.launch{local.update{it.copy(busy=true,error=null)};runCatching{withContext(Dispatchers.IO){data.inspectBackup(uri)}}.onSuccess{summary->importUri=uri;local.update{it.copy(busy=false,importSummary=summary)}}.onFailure(::failure)}}
    fun dismissImport(){importUri=null;local.update{it.copy(importSummary=null)}}
    fun restore(){val uri=importUri?:return;viewModelScope.launch{local.update{it.copy(busy=true)};runCatching{withContext(Dispatchers.IO){data.restoreBackup(uri)}}.onSuccess{summary->dismissImport();local.update{it.copy(busy=false)};_effects.send(SettingsEffect.Message("Restored ${summary.vehicleCount} vehicles"))}.onFailure(::failure)}}
    fun clearError(){local.update{it.copy(error=null)}}
    private fun launch(block:suspend () -> Unit)=viewModelScope.launch{block()}
    private fun work(message:String,block:suspend () -> Any){viewModelScope.launch{local.update{it.copy(busy=true,error=null)};runCatching{withContext(Dispatchers.IO){block()}}.onSuccess{local.update{it.copy(busy=false)};_effects.send(SettingsEffect.Message(message))}.onFailure(::failure)}}
    private fun failure(error:Throwable){local.update{it.copy(busy=false,error=error.message?:"That operation couldn't be completed.")}}
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun SettingsScreen(onMessage:(String)->Unit,viewModel:SettingsViewModel=hiltViewModel()){
    val state=viewModel.uiState.collectAsStateWithLifecycle().value
    val json=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){it?.let(viewModel::export)}
    val csv=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){it?.let(viewModel::csv)}
    val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(viewModel::inspect)}
    LaunchedEffect(Unit){viewModel.effects.collect{if(it is SettingsEffect.Message)onMessage(it.value)}}
    state.importSummary?.let{s->AlertDialog(onDismissRequest=viewModel::dismissImport,title={Text("Restore backup?")},text={Text("${s.vehicleCount} vehicles and ${s.readingCount} readings\nExported ${s.exportedAt.displayDate()}\n\nThis replaces all current RoadLog data.")},confirmButton={TextButton(onClick=viewModel::restore){Text("Restore")}},dismissButton={TextButton(onClick=viewModel::dismissImport){Text("Cancel")}})}
    state.error?.let{AlertDialog(onDismissRequest=viewModel::clearError,title={Text("Couldn't complete operation")},text={Text(it)},confirmButton={TextButton(onClick=viewModel::clearError){Text("OK")}})}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Text("Settings",style=MaterialTheme.typography.headlineMedium)}
        item{Section("Appearance")}
        item{Text("Theme",style=MaterialTheme.typography.titleMedium);SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){ThemeMode.entries.forEachIndexed{index,v->SegmentedButton(selected=state.settings.themeMode==v,onClick={viewModel.theme(v)},shape=SegmentedButtonDefaults.itemShape(index,ThemeMode.entries.size)){Text(v.name.lowercase().replaceFirstChar(Char::uppercase))}}}}
        item{SettingSwitch("Use system colours",state.settings.useDynamicColor,viewModel::dynamic)}
        item{SettingSwitch("Pure black in dark mode",state.settings.useAmoledBlack,viewModel::amoled)}
        item{Text("Accent",style=MaterialTheme.typography.titleMedium);FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){AccentTheme.entries.forEach{v->FilterChip(selected=state.settings.accentTheme==v,onClick={viewModel.accent(v)},enabled=!state.settings.useDynamicColor,label={Text(v.name.lowercase().replaceFirstChar(Char::uppercase))})}}}
        item{Text("Layout density",style=MaterialTheme.typography.titleMedium);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){DisplayDensity.entries.forEach{v->FilterChip(selected=state.settings.displayDensity==v,onClick={viewModel.density(v)},label={Text(v.name.lowercase().replaceFirstChar(Char::uppercase))})}}}
        item{Section("Behaviour")};item{SettingSwitch("Confirm before deleting readings",state.settings.confirmReadingDeletion,viewModel::confirmDelete)}
        item{Section("Data")}
        item{ListItem(headlineContent={Text("Export backup")},supportingContent={Text("Save all vehicles and readings as JSON")},modifier=Modifier.clickable{json.launch("roadlog-backup.json")})}
        item{ListItem(headlineContent={Text("Restore backup")},supportingContent={Text("Replace local data from RoadLog JSON")},modifier=Modifier.clickable{open.launch(arrayOf("application/json","text/plain"))})}
        item{ListItem(headlineContent={Text("Export current vehicle CSV")},supportingContent={Text(state.selectedVehicle?.name?:"No vehicle selected")},modifier=Modifier.clickable(enabled=state.selectedVehicle!=null){csv.launch("${state.selectedVehicle?.name?:"vehicle"}-odometer.csv")})}
        item{Section("About")};item{ListItem(headlineContent={Text("RoadLog")},supportingContent={Text("Version ${BuildConfig.VERSION_NAME} · Your data stays on this device unless you export it.")})}
        if(state.busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
    }
}
@Composable private fun Section(title:String){Text(title,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=18.dp,bottom=4.dp))}
@Composable private fun SettingSwitch(title:String,checked:Boolean,onChecked:(Boolean)->Unit){ListItem(headlineContent={Text(title)},trailingContent={Switch(checked,onCheckedChange=onChecked)},modifier=Modifier.clickable{onChecked(!checked)})}
