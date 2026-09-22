package com.roadlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.roadlog.core.designsystem.theme.RoadLogTheme
import com.roadlog.core.navigation.*
import com.roadlog.feature.dashboard.DashboardScreen
import com.roadlog.feature.readings.*
import com.roadlog.feature.settings.SettingsScreen
import com.roadlog.feature.statistics.StatisticsScreen
import com.roadlog.feature.vehicles.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { RoadLogApp() } }
}

private object Routes {
    const val Gate="gate";const val Welcome="welcome";const val Home="home";const val HistoryMain="history";const val Vehicles="vehicles";const val Settings="settings"
    const val AddVehicle="vehicle/add";const val Vehicle="vehicle/{vehicleId}";const val EditVehicle="vehicle/{vehicleId}/edit"
    const val AddReading="add-reading/{vehicleId}";const val History="history/{vehicleId}";const val EditReading="reading/{readingId}/edit"
    const val Statistics="statistics/{vehicleId}"
}

@Composable
fun RoadLogApp(viewModel: AppViewModel = hiltViewModel()) {
    val app by viewModel.uiState.collectAsStateWithLifecycle();val nav=rememberNavController();val snackbar=remember{SnackbarHostState()};val scope=rememberCoroutineScope()
    fun messageAndBack(message:String){nav.popBackStack();scope.launch{snackbar.showSnackbar(message)}}
    RoadLogTheme(app.settings) { Scaffold(snackbarHost={SnackbarHost(snackbar)}) { outer ->
        NavHost(nav,Routes.Gate,Modifier.padding(outer)) {
            composable(Routes.Gate){
                when { app.loading -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
                    app.vehicles.isEmpty() -> WelcomeScreen{nav.navigate(Routes.AddVehicle)}
                    else -> LaunchedEffect(Unit){nav.navigate(Routes.Home){popUpTo(Routes.Gate){inclusive=true}}}
                }
            }
            composable(Routes.Welcome){WelcomeScreen{nav.navigate(Routes.AddVehicle)}}
            composable(Routes.AddVehicle){VehicleEditorScreen(onSaved={nav.navigate(Routes.Home){popUpTo(Routes.Gate){inclusive=true}}},onBack={nav.popBackStack()})}
            composable(Routes.Home){MainShell("home",nav,app){DashboardScreen(onAdd={nav.navigate("add-reading/$it")},onHistory={nav.navigate(Routes.HistoryMain)},onManageVehicles={nav.navigate(Routes.Vehicles)},onStatistics={nav.navigate("statistics/$it")})}}
            composable(Routes.HistoryMain){MainShell("history",nav,app){val id=app.selectedVehicleId;if(id==null)com.roadlog.core.designsystem.component.EmptyState("No vehicle selected","Choose a vehicle to view its history.","Vehicles"){nav.navigate(Routes.Vehicles)}else ReadingHistoryScreen(onBack=null,onAdd={nav.navigate("add-reading/$it")},onEdit={nav.navigate("reading/$it/edit")})}}
            composable(Routes.Vehicles){MainShell("vehicles",nav,app){VehicleListScreen(app.vehicles,app.selectedVehicleId,app.latestReadings.mapValues{it.value.value},app.settings.displayDensity,viewModel::selectVehicle,{nav.navigate("vehicle/$it")},{nav.navigate(Routes.AddVehicle)})}}
            composable(Routes.Settings){MainShell("settings",nav,app){SettingsScreen(onMessage={message->scope.launch{snackbar.showSnackbar(message)}})}}
            composable(Routes.Vehicle){VehicleDetailsScreen(onBack={nav.popBackStack()},onAdd={nav.navigate("add-reading/$it")},onHistory={nav.navigate("history/$it")},onEdit={nav.navigate("vehicle/$it/edit")},onGone={nav.navigate(Routes.Vehicles){popUpTo(Routes.Home)}})}
            composable(Routes.EditVehicle){VehicleEditorScreen(onSaved={nav.popBackStack()},onBack={nav.popBackStack()})}
            composable(Routes.AddReading){AddReadingScreen({nav.popBackStack()},{messageAndBack(it)})}
            composable(Routes.History){ReadingHistoryScreen({nav.popBackStack()},{nav.navigate("add-reading/$it")},{nav.navigate("reading/$it/edit")})}
            composable(Routes.EditReading){EditReadingScreen({nav.popBackStack()},{messageAndBack(it)})}
            composable(Routes.Statistics){StatisticsScreen(onBack={nav.popBackStack()})}
        }
    } }
}

@Composable
private fun MainShell(current:String,nav:androidx.navigation.NavHostController,app:AppUiState,content:@Composable () -> Unit){
    Scaffold(bottomBar={NavigationBar{
        listOf(
            Triple("home","Home",Icons.Filled.Home),
            Triple("history","History",Icons.AutoMirrored.Filled.List),
            Triple("vehicles","Vehicles",Icons.Filled.Person),
            Triple("settings","Settings",Icons.Filled.Settings),
        ).forEach{(route,label,icon)->
            NavigationBarItem(selected=current==route,onClick={
                val target=if(route=="history"&&app.selectedVehicleId==null) Routes.Vehicles else route
                nav.navigate(target){popUpTo(nav.graph.findStartDestination().id){saveState=true};launchSingleTop=true;restoreState=true}
            },icon={Icon(icon,contentDescription=null)},label={Text(label)})
        }
    }}){padding->Box(Modifier.padding(padding)){content()}}
}
