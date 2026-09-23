package com.jacobgain.triprabbit.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.core.repository.*
import com.jacobgain.triprabbit.core.util.displayDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import com.jacobgain.triprabbit.BuildConfig
import com.jacobgain.triprabbit.core.designsystem.component.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity

data class SettingsUiState(val settings:AppSettings=AppSettings(),val selectedVehicle:Vehicle?=null,val importSummary:BackupSummary?=null,val busy:Boolean=false,val error:String?=null)
sealed interface SettingsEffect{data class Message(val value:String):SettingsEffect}
@HiltViewModel class SettingsViewModel @Inject constructor(private val settingsRepository:SettingsRepository,private val data:DataRepository,vehicles:VehicleRepository):ViewModel(){
    private val local=MutableStateFlow(SettingsUiState());private var importUri:Uri?=null
    val uiState=combine(settingsRepository.observeSettings(),vehicles.observeActiveVehicles(),local){settings,all,state->state.copy(settings=settings,selectedVehicle=all.firstOrNull{it.id==settings.selectedVehicleId})}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),SettingsUiState())
    private val _effects=Channel<SettingsEffect>(Channel.BUFFERED);val effects=_effects.receiveAsFlow()
    fun theme(v:ThemeMode)=launch{settingsRepository.setThemeMode(v)};fun density(v:DisplayDensity)=launch{settingsRepository.setDisplayDensity(v)};fun confirmDelete(v:Boolean)=launch{settingsRepository.setConfirmReadingDeletion(v)}
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
@Composable fun SettingsScreen(onMessage:(String)->Unit,onPrivacy:()->Unit,viewModel:SettingsViewModel=hiltViewModel()){
    val state=viewModel.uiState.collectAsStateWithLifecycle().value
    val json=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){it?.let(viewModel::export)}
    val csv=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){it?.let(viewModel::csv)}
    val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(viewModel::inspect)}
    LaunchedEffect(Unit){viewModel.effects.collect{if(it is SettingsEffect.Message)onMessage(it.value)}}
    state.importSummary?.let{s->AlertDialog(onDismissRequest=viewModel::dismissImport,title={Text("Restore backup?")},text={Text("${s.vehicleCount} vehicles and ${s.readingCount} readings\nExported ${s.exportedAt.displayDate()}\n\nThis replaces all current TripRabbit data.")},confirmButton={TextButton(onClick=viewModel::restore){Text("Restore")}},dismissButton={TextButton(onClick=viewModel::dismissImport){Text("Cancel")}})}
    state.error?.let{AlertDialog(onDismissRequest=viewModel::clearError,title={Text("Couldn't complete operation")},text={Text(it)},confirmButton={TextButton(onClick=viewModel::clearError){Text("OK")}})}
    SettingsContent(state,SettingsActions(
        theme={viewModel.theme(it)},
        density={viewModel.density(it)},confirmDelete={viewModel.confirmDelete(it)},
        export={json.launch("triprabbit-backup.json")},restore={open.launch(arrayOf("application/json","text/plain"))},
        csv={csv.launch("${state.selectedVehicle?.name?:"vehicle"}-odometer.csv")},privacy=onPrivacy,
    ))
}

data class SettingsActions(
    val theme:(ThemeMode)->Unit={},val density:(DisplayDensity)->Unit={},val confirmDelete:(Boolean)->Unit={},
    val export:()->Unit={},val restore:()->Unit={},val csv:()->Unit={},val privacy:()->Unit={},
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsContent(state:SettingsUiState,actions:SettingsActions=SettingsActions()) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,24.dp,20.dp,28.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        item { PageHeading("Make yourself at home.","A few preferences. A little more you.") }
        item { SectionCard {
            SectionTitle("Appearance","Choose when to use the light or dark palette.")
            ChoiceRow("System", "Follow your device setting", AppIcon.Device,
                state.settings.themeMode==ThemeMode.SYSTEM) { actions.theme(ThemeMode.SYSTEM) }
            ChoiceRow("Light", "Warm, bright surfaces", AppIcon.Sun,
                state.settings.themeMode==ThemeMode.LIGHT) { actions.theme(ThemeMode.LIGHT) }
            ChoiceRow("Dark", "Quiet, low-glare surfaces", AppIcon.Moon,
                state.settings.themeMode==ThemeMode.DARK) { actions.theme(ThemeMode.DARK) }
        } }
        item { SectionCard {
            SectionTitle("Your experience")
            Text("Garage layout",style=MaterialTheme.typography.titleSmall)
            ChoiceRow("Comfortable", "More room for each vehicle", AppIcon.Car,
                state.settings.displayDensity==DisplayDensity.COMFORTABLE) { actions.density(DisplayDensity.COMFORTABLE) }
            ChoiceRow("Compact", "See more vehicles at once", AppIcon.Reports,
                state.settings.displayDensity==DisplayDensity.COMPACT) { actions.density(DisplayDensity.COMPACT) }
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            PreferenceToggle("Confirm before deleting","Ask before removing an odometer reading.",state.settings.confirmReadingDeletion,actions.confirmDelete)
        } }
        item { SectionCard {
            SectionTitle("Your data","Stored here. Exported only when you choose.")
            ActionRow("Export backup","All vehicles and readings, as JSON.",AppIcon.Download,actions.export,enabled=!state.busy)
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            ActionRow("Restore backup","Replace local records from a backup.",AppIcon.Upload,actions.restore,enabled=!state.busy)
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            ActionRow("Export current vehicle CSV",state.selectedVehicle?.name?:"Select a vehicle from Home.",AppIcon.Reports,actions.csv,enabled=!state.busy&&state.selectedVehicle!=null)
            if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        } }
        item { SectionCard {
            SectionTitle("About")
            BrandHeader()
            Text("Miles for what matters.",style=MaterialTheme.typography.bodyLarge)
            Text("Version ${BuildConfig.VERSION_NAME} · Made for a little less admin.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            ActionRow("Privacy policy","Your data stays yours.",AppIcon.Shield,actions.privacy)
        } }
    }
}

@Composable
private fun ChoiceRow(title: String, subtitle: String, icon: AppIcon, selected: Boolean, onClick: () -> Unit) {
    val largeText = LocalDensity.current.fontScale > 1.4f
    Surface(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .35f)) else null,
    ) {
        if (largeText) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(icon, accented = selected)
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    if (selected) TripIcon(AppIcon.Check, "Selected", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(icon, accented = selected)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (selected) TripIcon(AppIcon.Check, "Selected", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun PreferenceToggle(title:String,subtitle:String,checked:Boolean,onChecked:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked,role=Role.Switch,onValueChange=onChecked).padding(vertical=6.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,style=MaterialTheme.typography.titleSmall)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked,onCheckedChange=null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PrivacyPolicyScreen(onBack:()->Unit){
    Scaffold(topBar={DetailTopBar("Privacy policy",onBack)}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
            item{PageHeading("TripRabbit privacy policy","Effective September 22, 2026",action={IconBadge(AppIcon.Shield,accented=true)})}
            item{PolicySection("Summary","TripRabbit is a local odometer-history app. It has no account, advertising, analytics, network service, or automatic cloud backup. TripRabbit does not collect, transmit, sell, or share your data with the developer or third parties.")}
            item{PolicySection("Data you enter","TripRabbit accesses the vehicle names, odometer readings, dates, units, and optional notes that you enter. This data is used only on your device to provide the app’s tracking, history, backup, and statistics features.")}
            item{PolicySection("Storage and security","App data is stored in TripRabbit’s private Android app storage, which is protected by Android’s app sandbox. Android backup is disabled for TripRabbit. The app requests no dangerous permissions and has no Internet permission.")}
            item{PolicySection("Exports and restores","TripRabbit writes a JSON backup or CSV export only when you choose a destination through Android’s system file picker. Exported files are not encrypted by TripRabbit and are then controlled by you and the storage provider you select. TripRabbit reads a backup only after you select it, validates it, asks for confirmation, and uses it to replace local app data.")}
            item{PolicySection("Retention and deletion","Local data remains until you delete individual records, clear TripRabbit’s app data, uninstall the app, or replace the data by restoring a backup. Because the developer never receives the data, there is no server copy to delete. Files you export must be deleted from their chosen storage location separately.")}
            item{PolicySection("Children","TripRabbit is a general vehicle utility intended for adults and is not designed for children. It does not knowingly collect data from anyone, including children.")}
            item{PolicySection("Changes","If TripRabbit’s data practices change, this policy and the Google Play Data safety declaration will be updated before the changed version is distributed.")}
            item{PolicySection("Contact","Privacy questions can be submitted using the developer contact details on TripRabbit’s Google Play store listing.")}
        }
    }
}

@Composable private fun PolicySection(title:String,body:String){SectionCard{SectionTitle(title);Text(body,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
