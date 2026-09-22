package com.roadlog.feature.readings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roadlog.core.designsystem.component.*
import com.roadlog.core.util.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReadingScreen(onBack:()->Unit,onSaved:(String)->Unit,viewModel:AddReadingViewModel=hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle(); val focus=remember{FocusRequester()};var dateText by remember{mutableStateOf(state.recordedAt.inputDateTime())}
    LaunchedEffect(Unit){focus.requestFocus();viewModel.effects.collectLatest{if(it is ReadingEffect.Saved)onSaved(it.message)}}
    Scaffold(topBar={TopAppBar(title={Text("Add Reading")},navigationIcon={TextButton(onClick=onBack){Text("Back")}})}){padding->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            Text(state.vehicle?.name.orEmpty(),style=MaterialTheme.typography.headlineSmall)
            Text("Previous",style=MaterialTheme.typography.labelLarge)
            state.previous?.let{OdometerDisplay(it.value,state.vehicle?.odometerUnit?:return@Column)}?:Text("No previous reading")
            OutlinedTextField(state.value,viewModel::valueChanged,label={Text("New reading")},suffix={Text(state.vehicle?.odometerUnit?.abbreviation.orEmpty())},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth().focusRequester(focus))
            val entered=state.value.toLongOrNull();if(entered!=null&&state.previous!=null&&entered>=state.previous!!.value) Text("+${(entered-state.previous!!.value).grouped()} ${state.vehicle?.odometerUnit?.abbreviation} since ${state.previous!!.recordedAt.displayDate()}",color=MaterialTheme.colorScheme.primary)
            val dateValid=dateText.parseDateTime()!=null
            OutlinedTextField(dateText,{dateText=it;it.parseDateTime()?.let(viewModel::dateChanged)},label={Text("Date and time (yyyy-MM-dd HH:mm)")},isError=!dateValid,supportingText={if(!dateValid)Text("Enter a valid date and time.")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(state.note,viewModel::noteChanged,label={Text("Note (optional)")},modifier=Modifier.fillMaxWidth())
            state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            Button(onClick=viewModel::save,enabled=!state.saving&&dateValid,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Save Reading")}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingHistoryScreen(onBack:(()->Unit)?,onAdd:(Long)->Unit,onEdit:(Long)->Unit,viewModel:ReadingHistoryViewModel=hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar={TopAppBar(title={Text(state.vehicle?.let{"${it.name} History"}?:"History")},navigationIcon={if(onBack!=null)TextButton(onClick=onBack){Text("Back")}})},floatingActionButton={state.vehicle?.let{v->ExtendedFloatingActionButton(onClick={onAdd(v.id)},icon={Icon(Icons.Filled.Add,contentDescription=null)},text={Text("Add Reading")})}}){padding->
        if(!state.loading&&state.items.isEmpty()) Box(Modifier.fillMaxSize().padding(padding)){EmptyState("No readings yet","Add your first odometer reading to start building history.","Add Reading"){state.vehicle?.id?.let(onAdd)}}
        else LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp,8.dp,16.dp,88.dp)){
            itemsIndexed(state.items,key={_,it->it.reading.id}){index,item->
                if(index==0||state.items[index-1].reading.recordedAt.displayMonth()!=item.reading.recordedAt.displayMonth()) Text(item.reading.recordedAt.displayMonth(),style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=16.dp,bottom=8.dp))
                ListItem(headlineContent={Text("${item.reading.value.grouped()} ${state.vehicle?.odometerUnit?.abbreviation.orEmpty()}")},overlineContent={Text(item.reading.recordedAt.displayDate())},supportingContent={item.difference?.let{d->Text("+${d.grouped()} ${state.vehicle?.odometerUnit?.abbreviation}${if(item.reading.note!=null) " · Note" else ""}")}},modifier=Modifier.clickable{onEdit(item.reading.id)})
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReadingScreen(onBack:()->Unit,onSaved:(String)->Unit,viewModel:EditReadingViewModel=hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle();var confirmDelete by remember{mutableStateOf(false)};var dateText by remember(state.reading?.id){mutableStateOf(state.recordedAt.inputDateTime())}
    LaunchedEffect(Unit){viewModel.effects.collectLatest{if(it is ReadingEffect.Saved)onSaved(it.message)}}
    if(confirmDelete) DestructiveConfirmationDialog("Delete this reading?","This cannot be undone.",{confirmDelete=false;viewModel.delete()},{confirmDelete=false})
    Scaffold(topBar={TopAppBar(title={Text("Edit Reading")},navigationIcon={TextButton(onClick=onBack){Text("Back")}})}){padding->
        if(state.loading) Box(Modifier.fillMaxSize().padding(padding),contentAlignment=androidx.compose.ui.Alignment.Center){CircularProgressIndicator()}
        else if(state.reading==null) Box(Modifier.fillMaxSize().padding(padding)){EmptyState("Reading not found","It may have already been deleted.","Go Back",onBack)}
        else Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            OutlinedTextField(state.value,viewModel::valueChanged,label={Text("Odometer reading")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth())
            val dateValid=dateText.parseDateTime()!=null
            OutlinedTextField(dateText,{dateText=it;it.parseDateTime()?.let(viewModel::dateChanged)},label={Text("Date and time (yyyy-MM-dd HH:mm)")},isError=!dateValid,supportingText={if(!dateValid)Text("Enter a valid date and time.")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(state.note,viewModel::noteChanged,label={Text("Note (optional)")},modifier=Modifier.fillMaxWidth())
            state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            Button(onClick=viewModel::save,enabled=!state.saving&&dateValid,modifier=Modifier.fillMaxWidth()){Text("Save Changes")}
            HorizontalDivider();TextButton(onClick={if(state.confirmDeletion){{confirmDelete=true}}else viewModel::delete},enabled=!state.saving,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)){Text("Delete Reading")}
        }
    }
}
