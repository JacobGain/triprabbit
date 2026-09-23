package com.jacobgain.triprabbit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.jacobgain.triprabbit.core.designsystem.theme.TripRabbitTheme
import com.jacobgain.triprabbit.core.designsystem.component.*
import com.jacobgain.triprabbit.core.navigation.*
import com.jacobgain.triprabbit.feature.dashboard.DashboardScreen
import com.jacobgain.triprabbit.feature.readings.*
import com.jacobgain.triprabbit.feature.settings.SettingsScreen
import com.jacobgain.triprabbit.feature.settings.PrivacyPolicyScreen
import com.jacobgain.triprabbit.feature.statistics.StatisticsScreen
import com.jacobgain.triprabbit.feature.vehicles.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { TripRabbitApp() } }
}

private object Routes {
    const val Gate="gate";const val Home="home";const val HistoryMain="history";const val Vehicles="vehicles";const val Settings="settings";const val Privacy="privacy"
    const val AddVehicle="vehicle/add";const val Vehicle="vehicle/{vehicleId}";const val EditVehicle="vehicle/{vehicleId}/edit"
    const val AddReading="add-reading/{vehicleId}";const val History="history/{vehicleId}";const val EditReading="reading/{readingId}/edit"
    const val Statistics="statistics/{vehicleId}"; const val Reports="reports"
}

@Composable
fun TripRabbitApp(viewModel: AppViewModel = hiltViewModel()) {
    val app by viewModel.uiState.collectAsStateWithLifecycle();val nav=rememberNavController();val snackbar=remember{SnackbarHostState()};val scope=rememberCoroutineScope()
    val entry by nav.currentBackStackEntryAsState()
    val hasBottomNavigation = entry?.destination?.route in setOf(Routes.Home, Routes.HistoryMain, Routes.Reports, Routes.Settings, Routes.Vehicles)
    val snackbarClearance = if (hasBottomNavigation) { if (LocalDensity.current.fontScale > 1.4f) 128.dp else 80.dp } else 0.dp
    fun messageAndBack(message:String){nav.popBackStack();scope.launch{snackbar.showSnackbar(message)}}
    TripRabbitTheme(app.settings) { Scaffold(snackbarHost={SnackbarHost(snackbar,Modifier.padding(bottom=snackbarClearance))}) { outer ->
        NavHost(nav,Routes.Gate,Modifier.padding(outer).consumeWindowInsets(outer)) {
            composable(Routes.Gate){
                when { app.loading -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
                    app.vehicles.isEmpty() -> WelcomeScreen{nav.navigate(Routes.AddVehicle)}
                    else -> LaunchedEffect(Unit){nav.navigate(Routes.Home){popUpTo(Routes.Gate){inclusive=true}}}
                }
            }
            composable(Routes.AddVehicle){VehicleEditorScreen(onSaved={
                if (!nav.popBackStack(Routes.Home, false)) nav.navigate(Routes.Home){popUpTo(Routes.Gate){inclusive=true};launchSingleTop=true}
            },onBack={nav.popBackStack()})}
            composable(Routes.Home){MainShell("home",nav){DashboardScreen(onAdd={nav.navigate("add-reading/$it")},onHistory={nav.navigate(Routes.HistoryMain)},onManageVehicles={nav.navigate(Routes.Vehicles)},onStatistics={nav.navigate("statistics/$it")})}}
            composable(Routes.HistoryMain){MainShell("history",nav){val id=app.selectedVehicleId;if(id==null)EmptyState("No vehicle selected","Choose a vehicle to view its history.","Vehicles"){nav.navigate(Routes.Vehicles)}else ReadingHistoryScreen(onBack=null,onAdd={nav.navigate("add-reading/$it")},onEdit={nav.navigate("reading/$it/edit")})}}
            composable(Routes.Vehicles){MainShell("home",nav){VehicleListScreen(app.vehicles,app.selectedVehicleId,app.latestReadings.mapValues{it.value.value},app.settings.displayDensity,viewModel::selectVehicle,{nav.navigate("vehicle/$it")},{nav.navigate(Routes.AddVehicle)},onBack={nav.popBackStack()})}}
            composable(Routes.Settings){MainShell("settings",nav){SettingsScreen(onMessage={message->scope.launch{snackbar.showSnackbar(message)}},onPrivacy={nav.navigate(Routes.Privacy)})}}
            composable(Routes.Reports){MainShell("reports",nav){StatisticsScreen(onBack=null,onManageVehicles={nav.navigate(Routes.Vehicles)})}}
            composable(Routes.Privacy){PrivacyPolicyScreen(onBack={nav.popBackStack()})}
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
private fun MainShell(current:String,nav:androidx.navigation.NavHostController,content:@Composable () -> Unit){
    Scaffold(bottomBar={AppNavigation(current){route->
        nav.navigate(route){popUpTo(Routes.Home);launchSingleTop=true}
    }}){padding->Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),contentAlignment=Alignment.TopCenter){
        Box(Modifier.widthIn(max=720.dp).fillMaxSize()){content()}
    }}
}
