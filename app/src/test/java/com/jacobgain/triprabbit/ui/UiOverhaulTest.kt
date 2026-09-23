package com.jacobgain.triprabbit.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jacobgain.triprabbit.core.designsystem.component.*
import com.jacobgain.triprabbit.core.designsystem.theme.TripRabbitTheme
import com.jacobgain.triprabbit.core.model.*
import com.jacobgain.triprabbit.feature.dashboard.*
import com.jacobgain.triprabbit.feature.readings.*
import com.jacobgain.triprabbit.feature.settings.*
import com.jacobgain.triprabbit.feature.statistics.*
import com.jacobgain.triprabbit.feature.vehicles.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Renders production composables, using sample state only at the data boundary. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiOverhaulTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val now = Instant.now()
    private val vehicle = Vehicle(1, "Daily driver", "Volkswagen", "Golf", 2022, "TRIP 01", DistanceUnit.KILOMETERS, null, "The everyday companion.", now, null)
    private val readings = listOf(124_850L, 124_712L, 124_580L, 124_420L, 124_200L, 124_010L, 123_900L).mapIndexed { i, value ->
        OdometerReading(i + 1L, 1, value, now.minus(i.toLong(), ChronoUnit.DAYS), if (i == 1) "Weekend away" else null, now, null)
    }
    private val stats = com.jacobgain.triprabbit.core.usecase.CalculateMileageStatsUseCase()(readings, now)
    private val dashboard = DashboardUiState(false, vehicle, readings, stats, listOf(vehicle))
    private val history = ReadingHistoryUiState(false, vehicle, readings.mapIndexed { index, r -> ReadingItem(r, readings.getOrNull(index + 1)?.let { r.value - it.value }) })

    private fun render(dark: Boolean = false, fontScale: Float = 1f, tab: String? = null, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                TripRabbitTheme(AppSettings(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT)) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (tab == null) content()
                        else Scaffold(bottomBar = { AppNavigation(tab) {} }) { padding ->
                            Box(Modifier.padding(padding).consumeWindowInsets(padding)) { content() }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        val target = File("build/reports/ui/$name.png")
        target.parentFile?.mkdirs()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun dashboardLight() {
        var added: Long? = null
        render(tab = "home") { DashboardContent(dashboard, onAdd = { added = it }) }
        capture("dashboard-light")
        compose.onNodeWithText("Add Reading").performClick()
        assertEquals(1L, added)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("A little further, every day."))
        compose.onNodeWithText("A little further, every day.").assertIsDisplayed()
        capture("dashboard-artwork-slot")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Recent readings"))
        capture("dashboard-activity")
    }

    @Test fun dashboardDark() { render(dark = true, tab = "home") { DashboardContent(dashboard) }; capture("dashboard-dark") }

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi") fun dashboardLargeText() {
        render(fontScale = 1.6f, tab = "home") { DashboardContent(dashboard) }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Add Reading"))
        compose.onNodeWithText("Add Reading").assertIsDisplayed()
        capture("dashboard-large-text")
    }

    @Test fun historySearchAndFilters() {
        render(tab = "history") { ReadingHistoryContent(history) }
        capture("history-light")
        compose.onNodeWithText("With Notes").performClick()
        compose.onNodeWithText("Weekend away").assertIsDisplayed()
        compose.onNodeWithText("124,850 km").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextInput("no match")
        compose.onNodeWithText("No matching readings").assertIsDisplayed()
        capture("history-empty-search")
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithText("Weekend away").assertIsDisplayed()
    }

    @Test fun historyDark() { render(dark = true, tab = "history") { ReadingHistoryContent(history) }; capture("history-dark") }

    @Test fun reportsLight() {
        render(tab = "reports") { StatisticsContent(StatisticsUiState(false, vehicle, readings, stats)) }
        capture("reports-light")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Export CSV"))
        compose.onNodeWithText("Export CSV").assertIsDisplayed()
        capture("reports-export")
    }

    @Test fun reportsDark() { render(dark = true, tab = "reports") { StatisticsContent(StatisticsUiState(false, vehicle, readings, stats)) }; capture("reports-dark") }

    @Test fun addReading() {
        render { AddReadingContent(AddReadingUiState(vehicle, readings.first(), value = "124980", recordedAt = now)) }
        capture("add-reading")
        compose.onNodeWithText("Save Reading").performScrollTo().assertIsEnabled()
    }

    @Test fun invalidDateDisablesSave() {
        render { AddReadingContent(AddReadingUiState(vehicle, readings.first(), value = "124980", recordedAt = now)) }
        compose.onNodeWithText(now.inputDate()).performTextReplacement("not a date")
        compose.onNodeWithText("Save Reading").performScrollTo().assertIsNotEnabled()
    }

    @Test fun dateAndTimePickerUpdatesTheReading() {
        var changed: Instant? = null
        render { AddReadingContent(AddReadingUiState(vehicle, readings.first(), recordedAt = now), onDate = { changed = it }) }
        compose.onNodeWithContentDescription("Choose date and time").performScrollTo().performClick()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Apply").performClick()
        assertEquals(now.truncatedTo(ChronoUnit.MINUTES), changed)
    }

    @Test fun editReadingRequiresConfirmation() {
        var deleted = false
        render { EditReadingContent(EditReadingUiState(false, readings.first(), "124850", now), onDelete = { deleted = true }) }
        capture("edit-reading")
        compose.onNodeWithText("Delete Reading").performScrollTo().performClick()
        assertFalse(deleted)
        compose.onNodeWithText("Delete").performClick()
        assertTrue(deleted)
    }

    @Test fun editReadingHonoursDeletionPreference() {
        var deleted = false
        render { EditReadingContent(EditReadingUiState(false, readings.first(), "124850", now, confirmDeletion = false), onDelete = { deleted = true }) }
        compose.onNodeWithText("Delete Reading").performScrollTo().performClick()
        assertTrue(deleted)
    }

    @Test fun welcome() { render { WelcomeScreen {} }; capture("welcome"); compose.onNodeWithText("Get Started").performScrollTo().assertIsDisplayed() }

    @Test fun vehicleEditor() { render { VehicleEditorContent(VehicleEditorUiState(name = "Daily driver", initialReading = "124850")) }; capture("create-vehicle") }

    @Test fun garage() {
        render { VehicleListScreen(listOf(vehicle, vehicle.copy(id = 2, name = "Weekend car", make = "Mazda", model = "MX-5")), 1,
            mapOf(1L to 124850L, 2L to 32800L), DisplayDensity.COMFORTABLE, {}, {}, {}, {}) }
        capture("vehicles")
    }

    @Test fun vehicleDetails() { render { VehicleDetailsContent(VehicleDetailsUiState(false, vehicle, readings)) }; capture("vehicle-details") }

    @Test fun settings() {
        render(tab = "settings") { SettingsContent(SettingsUiState(selectedVehicle = vehicle)) }
        capture("settings-light")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("About"))
        capture("settings-data")
    }

    @Test fun settingsDark() { render(dark = true, tab = "settings") { SettingsContent(SettingsUiState(settings = AppSettings(themeMode = ThemeMode.DARK), selectedVehicle = vehicle)) }; capture("settings-dark") }

    @Test fun privacy() { render { PrivacyPolicyScreen {} }; capture("privacy") }

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi") fun settingsLargeText() {
        render(fontScale = 2f, tab = "settings") { SettingsContent(SettingsUiState(selectedVehicle = vehicle)) }
        capture("settings-large-text")
    }

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi") fun formLargeText() {
        render(fontScale = 2f) { AddReadingContent(AddReadingUiState(vehicle, readings.first(), recordedAt = now)) }
        capture("add-reading-large-text")
        compose.onNodeWithText("Save Reading").performScrollTo().assertIsDisplayed()
    }

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi") fun garageLargeText() {
        render(fontScale = 2f) { VehicleListScreen(listOf(vehicle), 1, mapOf(1L to 124850L), DisplayDensity.COMFORTABLE, {}, {}, {}) }
        capture("vehicles-large-text")
    }

    @Test fun launcherIcon() {
        render { BrandHeader() }
        compose.runOnIdle {
            val icon = checkNotNull(compose.activity.getDrawable(com.jacobgain.triprabbit.R.mipmap.ic_launcher))
            val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, 192, 192)
            icon.draw(android.graphics.Canvas(bitmap))
            val target = File("build/reports/ui/launcher-icon.png")
            target.parentFile?.mkdirs()
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun navigationTabsRespondToTouches() {
        render {
            var current by remember { mutableStateOf("home") }
            Scaffold(bottomBar = { AppNavigation(current) { current = it } }) { padding -> Box(Modifier.padding(padding)) }
        }
        compose.onNodeWithText("History").performClick().assertIsSelected()
        compose.onNodeWithText("Reports").performClick().assertIsSelected()
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test fun iconFamily() {
        render { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            BrandHeader()
            BrandMark(Modifier.size(112.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                AppIcon.entries.forEach { IconBadge(it) }
            }
        } }
        capture("icon-family")
    }

    private fun Instant.inputDate(): String = atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}
