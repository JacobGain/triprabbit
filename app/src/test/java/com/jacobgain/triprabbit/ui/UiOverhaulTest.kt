package com.jacobgain.triprabbit.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
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
    private val history = ReadingHistoryUiState(false, vehicle, readings.mapIndexed { index, r -> ReadingItem(r, readings.getOrNull(index + 1)?.let { r.value - it.value }) }, vehicles = listOf(vehicle))

    private fun render(dark: Boolean = false, fontScale: Float = 1f, tab: String? = null, content: @Composable () -> Unit) {
        // Dialogs create their own window density, so also set the Android resource configuration.
        val resources = compose.activity.resources
        val configuration = android.content.res.Configuration(resources.configuration).apply { this.fontScale = fontScale }
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
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
            // Dialogs own a separate window; draw the front window rather than the activity behind it.
            val managerClass = Class.forName("android.view.WindowManagerGlobal")
            val manager = managerClass.getDeclaredMethod("getInstance").invoke(null)
            @Suppress("UNCHECKED_CAST")
            val views = managerClass.getDeclaredField("mViews").apply { isAccessible = true }.get(manager) as List<android.view.View>
            val view = views.lastOrNull { it.visibility == android.view.View.VISIBLE && it.width > 0 && it.height > 0 }
                ?: compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun customAccentChangesTheAppPalette() {
        var selected = Color.Unspecified
        var background = Color.Unspecified
        compose.setContent {
            TripRabbitTheme(AppSettings(themeMode = ThemeMode.LIGHT, accentColor = 0x3456AB)) {
                selected = MaterialTheme.colorScheme.primary
                background = MaterialTheme.colorScheme.background
            }
        }
        compose.waitForIdle()
        assertEquals(Color(0xFF3456AB), selected)
        assertNotEquals(Color(0xFFF6F7F3), background)
    }

    @Test fun extremeAccentsRemainReadable() {
        var accent by mutableStateOf(0xFFFFFF)
        var dark by mutableStateOf(false)
        var palette: ColorScheme? = null
        compose.setContent { TripRabbitTheme(AppSettings(accentColor = accent, themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT)) { palette = MaterialTheme.colorScheme } }
        fun contrast(a: Color, b: Color) = (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)
        for (mode in listOf(false, true)) for (rgb in listOf(0xFFFFFF, 0x000000, 0xFFFF00, 0x0000FF, 0xFF0000, 0x00FF00)) {
            compose.runOnIdle { dark = mode; accent = rgb }
            compose.waitForIdle()
            val colors = checkNotNull(palette)
            assertTrue(contrast(colors.primary, colors.surface) >= 4.5f)
            assertTrue(contrast(colors.primary, colors.onPrimary) >= 4.5f)
        }
    }

    @Test fun dashboardLight() {
        var added: Long? = null
        render(tab = "home") { DashboardContent(dashboard, onAdd = { added = it }) }
        capture("dashboard-light")
        compose.onNodeWithText("Add Trip").performClick()
        assertEquals(1L, added)
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(5)
        capture("dashboard-artwork-slot")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Recent trips"))
        capture("dashboard-activity")
    }

    @Test fun dashboardDark() { render(dark = true, tab = "home") { DashboardContent(dashboard) }; capture("dashboard-dark") }

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi") fun dashboardLargeText() {
        render(fontScale = 1.6f, tab = "home") { DashboardContent(dashboard) }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Add Trip"))
        compose.onNodeWithText("Add Trip").assertIsDisplayed()
        capture("dashboard-large-text")
    }

    @Test fun historySearchAndFilters() {
        render(tab = "history") { ReadingHistoryContent(history) }
        compose.onNodeWithText("All Vehicles").assertIsSelected()
        compose.onNodeWithText("All Trips").assertDoesNotExist()
        compose.onNodeWithText("This Month").assertDoesNotExist()
        capture("history-light")
        compose.onNode(hasSetTextAction()).performTextInput("Weekend")
        compose.onNodeWithText("132 km").assertExists()
        compose.onNode(hasSetTextAction()).performTextReplacement("no match")
        compose.onNodeWithText("No matching trips").assertExists()
        capture("history-empty-search")
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithText("No matching trips").assertDoesNotExist()
    }

    @Test fun allVehiclesFilterCanNarrowTripsByCarAndKeepsEachUnit() {
        val workTruck = vehicle.copy(id = 2, name = "Work truck", odometerUnit = DistanceUnit.MILES)
        val commute = readings.first().copy(name = "Commute")
        val siteVisit = readings[1].copy(id = 100, vehicleId = workTruck.id, name = "Site visit", value = 9_000, startValue = 8_950)
        val state = ReadingHistoryUiState(false, vehicle,
            listOf(ReadingItem(commute, 138, vehicle.name, vehicle.odometerUnit.abbreviation),
                ReadingItem(siteVisit, 50, workTruck.name, workTruck.odometerUnit.abbreviation)),
            vehicles = listOf(vehicle, workTruck))
        render(tab = "history") { ReadingHistoryContent(state) }
        compose.onNodeWithText("All Vehicles").assertIsSelected()
        compose.onNodeWithText("Commute").assertExists()
        compose.onNodeWithText("Site visit").assertExists()
        compose.onNodeWithText("50 mi").assertExists()
        compose.onNode(hasText("Work truck", substring = false) and hasClickAction()).performClick()
        compose.onNodeWithText("Site visit").assertExists()
        compose.onNodeWithText("Commute").assertDoesNotExist()
        compose.onNodeWithText("All Vehicles").performClick().assertIsSelected()
        compose.onNodeWithText("Commute").assertExists()
        compose.onNodeWithText("Site visit").assertExists()
    }

    @Test fun activeTripDisablesBothNewTripActions() {
        val blocked = AddReadingUiState(vehicle = vehicle, previous = readings.first(), name = "Another trip",
            value = "124900", recordedAt = now, hasInProgressTrip = true)
        render { AddReadingContent(blocked) }
        compose.onNodeWithText("Finish later").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Save Trip").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Finish your in-progress trip before saving another").performScrollTo().assertExists()
    }

    @Test fun finishingTripKeepsCreationActionOrderAndLabels() {
        var keptInProgress = 0
        var finished = 0
        val pending = readings.first().copy(inProgress = true, startValue = readings.first().value, name = "Client visit")
        render {
            EditReadingContent(EditReadingUiState(loading = false, reading = pending, startValue = pending.value.toString(),
                value = pending.value.toString(), recordedAt = now, name = "Client visit", unit = "km"),
                onSave = { finished++ }, onKeepInProgress = { keptInProgress++ })
        }
        val keepNode = compose.onNodeWithText("Keep in progress")
        val finishNode = compose.onNode(hasText("Finish trip", substring = false) and hasClickAction())
        assertTrue(keepNode.fetchSemanticsNode().boundsInRoot.top < finishNode.fetchSemanticsNode().boundsInRoot.top)
        keepNode.performClick()
        finishNode.performClick()
        assertEquals(1, keptInProgress)
        assertEquals(1, finished)
    }

    @Test fun reportGraphsCanBeHiddenWithoutHidingExport() {
        render(tab = "reports") {
            StatisticsContent(StatisticsUiState(false, vehicle, readings, stats, showGraphs = false))
        }
        compose.onNodeWithText("Distance by month").assertDoesNotExist()
        compose.onNodeWithText("Your week").assertDoesNotExist()
        compose.onNodeWithText("Total distance tracked").assertExists()
        compose.onNodeWithText("Export PDF").performScrollTo().assertIsDisplayed()
    }

    @Test fun pendingTripAndCloseControls() {
        val pending = readings.first().copy(inProgress = true, startValue = 124850, value = 124850, name = "Client visit")
        var opened = 0L
        render(tab = "history") { ReadingHistoryContent(history.copy(items = listOf(ReadingItem(pending, null))), onEdit = { opened = it }) }
        capture("trip-in-progress")
        compose.onNodeWithText("In progress").assertIsDisplayed()
        compose.onNodeWithText("Client visit").performClick()
        capture("trip-in-progress-details")
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Finish trip").assertDoesNotExist()
        compose.onNodeWithText("Client visit").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Client visit").performClick()
        compose.onNodeWithText("Finish trip").performClick()
        assertEquals(pending.id, opened)
    }

    @Test fun historyDark() { render(dark = true, tab = "history") { ReadingHistoryContent(history) }; capture("history-dark") }

    @Test fun reportsLight() {
        render(tab = "reports") { StatisticsContent(StatisticsUiState(false, vehicle, readings, stats)) }
        capture("reports-light")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Export CSV"))
        compose.onNodeWithText("Export CSV").assertIsDisplayed()
        capture("reports-export")
    }

    @Test fun reportsCanSwitchTheAggregatedDisplayUnit() {
        render(tab = "reports") {
            var unit by remember { mutableStateOf(DistanceUnit.KILOMETERS) }
            StatisticsContent(StatisticsUiState(false, vehicle, readings, stats, vehicles = listOf(vehicle), displayUnit = unit),
                onSelectUnit = { unit = it })
        }
        compose.onNodeWithText("km", substring = false).assertIsSelected()
        compose.onNodeWithText("mi", substring = false).performClick().assertIsSelected()
        compose.onNodeWithText("km", substring = false).assertIsNotSelected()
    }

    @Test fun reportsVehicleSelectorLivesInExportSectionAndIncludesAllVehicles() {
        val workTruck = vehicle.copy(id = 2, name = "Work truck")
        render(tab = "reports") {
            StatisticsContent(StatisticsUiState(false, vehicle, readings, stats, vehicles = listOf(vehicle, workTruck)))
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Export PDF"))
        compose.onNodeWithText("Export trip data").assertIsDisplayed()
        compose.onNodeWithText("Choose vehicles and a date range").assertIsDisplayed()
        compose.onNodeWithText("All Vehicles").assertIsDisplayed().performClick()
        compose.onNodeWithText("Daily driver").assertIsDisplayed()
        compose.onNodeWithText("Work truck").assertIsDisplayed()
    }

    @Test fun reportsDark() { render(dark = true, tab = "reports") { StatisticsContent(StatisticsUiState(false, vehicle, readings, stats)) }; capture("reports-dark") }

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi") fun reportsLargeTextAndExports() {
        var pdfExports = 0
        var csvExports = 0
        render(fontScale = 2f, tab = "reports") {
            StatisticsContent(StatisticsUiState(false, vehicle, readings, stats),
                onExport = { csvExports++ }, onExportPdf = { pdfExports++ })
        }
        capture("reports-large-text")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Export PDF"))
        compose.onNodeWithText("Export PDF").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Export CSV"))
        compose.onNodeWithText("Export CSV").performClick()
        assertEquals(1, pdfExports)
        assertEquals(1, csvExports)
        capture("reports-export-large-text")
    }

    @Test fun addReading() {
        render { AddReadingContent(AddReadingUiState(vehicle, readings.first(), value = "124980", name = "New trip", recordedAt = now)) }
        capture("add-reading")
        compose.onNodeWithText("Save Trip").performScrollTo().assertIsEnabled()
    }

    @Test fun odometerEditorAcceptsFullValue() {
        var value = ""
        render { AddReadingContent(AddReadingUiState(vehicle, readings.first(), recordedAt = now), onValue = { value = it }) }
        compose.onNodeWithText("Finish odometer").performScrollTo().performClick()
        capture("odometer-editor")
        compose.onNodeWithText("Odometer reading").performScrollTo().performTextReplacement("125000")
        compose.onNodeWithText("Apply").performScrollTo().performClick()
        assertEquals("125000", value)
    }

    @Test fun odometerDigitsChangeIndependently() {
        var applied = ""
        render { AddReadingContent(AddReadingUiState(vehicle, readings.first(), recordedAt = now), onValue = { applied = it }) }
        compose.onNodeWithText("Finish odometer").performClick()
        compose.onNodeWithText("Odometer reading").assertTextContains("124850")
        compose.onNodeWithContentDescription("Increase digit 4").performScrollTo().performClick()
        compose.onNodeWithText("Odometer reading").assertTextContains("124950")
        compose.onNodeWithContentDescription("Decrease digit 5").performScrollTo().performClick()
        compose.onNodeWithText("Apply").performScrollTo().performClick()
        assertEquals("124940", applied)
    }

    @Test fun invalidDateDisablesSave() {
        render { AddReadingContent(AddReadingUiState(vehicle, readings.first(), value = "124980", recordedAt = now)) }
        compose.onNodeWithText(now.inputDate()).performTextReplacement("not a date")
        compose.onNodeWithText("Save Trip").performScrollTo().assertIsNotEnabled()
    }

    @Test fun datePickerUpdatesTheReadingWithoutRequiringTime() {
        var changed: Instant? = null
        render { AddReadingContent(AddReadingUiState(vehicle, readings.first(), recordedAt = now), onDate = { changed = it }) }
        compose.onNodeWithContentDescription("Choose date").performScrollTo().performClick()
        compose.onNodeWithText("Next").performClick()
        assertEquals(now.atZone(java.time.ZoneId.systemDefault()).toLocalDate(), changed?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate())
    }

    @Test fun editReadingRequiresConfirmation() {
        var deleted = false
        render { EditReadingContent(EditReadingUiState(loading = false, reading = readings.first(), value = "124850", recordedAt = now), onDelete = { deleted = true }) }
        capture("edit-reading")
        compose.onNodeWithText("Delete Trip").performScrollTo().performClick()
        assertFalse(deleted)
        compose.onNodeWithText("Delete").performClick()
        assertTrue(deleted)
    }

    @Test fun editReadingHonoursDeletionPreference() {
        var deleted = false
        render { EditReadingContent(EditReadingUiState(loading = false, reading = readings.first(), value = "124850", recordedAt = now, confirmDeletion = false), onDelete = { deleted = true }) }
        compose.onNodeWithText("Delete Trip").performScrollTo().performClick()
        assertTrue(deleted)
    }

    @Test fun welcome() { render { WelcomeScreen {} }; capture("welcome"); compose.onNodeWithText("Get Started").performScrollTo().assertIsDisplayed() }

    @Test fun vehicleEditor() { render { VehicleEditorContent(VehicleEditorUiState(name = "Daily driver", initialReading = "124850")) }; capture("create-vehicle") }

    @Test fun garage() {
        render(tab = "vehicles") { VehicleListScreen(listOf(vehicle, vehicle.copy(id = 2, name = "Weekend car", make = "Mazda", model = "MX-5")),
            mapOf(1L to 124850L, 2L to 32800L), DisplayDensity.COMFORTABLE, {}, {}, {}) }
        capture("vehicles")
    }

    @Test fun garageDetailsAndAddActions() {
        var opened = 0L
        var added = false
        render(dark = true, tab = "vehicles") {
            VehicleListScreen(listOf(vehicle, vehicle.copy(id = 2, name = "Weekend car")),
                mapOf(1L to 124850L, 2L to 32800L), DisplayDensity.COMFORTABLE,
                { opened = it }, { added = true })
        }
        capture("vehicles-dark")
        compose.onNodeWithText("Make current").assertDoesNotExist()
        compose.onNodeWithText("Current vehicle").assertDoesNotExist()
        compose.onAllNodesWithText("Vehicle details").onLast().performClick()
        assertEquals(2L, opened)
        compose.onNodeWithText("Add vehicle").performClick()
        assertTrue(added)
    }

    @Test fun compactGarageUsesShortRows() {
        render(tab = "vehicles") { VehicleListScreen(listOf(vehicle, vehicle.copy(id = 2, name = "Weekend car")),
            mapOf(1L to 124850L, 2L to 32800L), DisplayDensity.COMPACT, {}, {}, {}) }
        capture("vehicles-compact")
        compose.onNodeWithText("Daily driver").assertIsDisplayed()
        compose.onNodeWithText("124,850 km").assertIsDisplayed()
        compose.onNodeWithText("Vehicle details").assertDoesNotExist()
        compose.onNodeWithContentDescription("Current vehicle").assertDoesNotExist()
    }

    @Test fun vehicleDetails() { render { VehicleDetailsContent(VehicleDetailsUiState(false, vehicle, readings)) }; capture("vehicle-details") }

    @Test fun settings() {
        render(tab = "settings") { SettingsContent(SettingsUiState()) }
        capture("settings-light")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("About"))
        capture("settings-data")
    }

    @Test fun settingsDark() { render(dark = true, tab = "settings") { SettingsContent(SettingsUiState(settings = AppSettings(themeMode = ThemeMode.DARK))) }; capture("settings-dark") }

    @Test fun accentDialogAppliesOnlyOnConfirmation() {
        var chosen: Int? = null
        render { SettingsContent(SettingsUiState(), SettingsActions(accent = { chosen = it })) }
        compose.onNodeWithText("Change accent colour").performClick()
        capture("accent-dialog")
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).onFirst()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(100f) }
        compose.runOnIdle { assertNull(chosen) }
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle { assertEquals(0x646B58, chosen) }
    }

    @Test fun privacy() { render { PrivacyPolicyScreen {} }; capture("privacy") }

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi") fun settingsLargeText() {
        render(fontScale = 2f, tab = "settings") { SettingsContent(SettingsUiState()) }
        capture("settings-large-text")
    }

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi") fun formLargeText() {
        render(fontScale = 2f) { AddReadingContent(AddReadingUiState(vehicle, readings.first(), recordedAt = now)) }
        capture("add-reading-large-text")
        compose.onNodeWithText("Save Trip").performScrollTo().assertIsDisplayed()
    }

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi") fun garageLargeText() {
        render(fontScale = 2f) { VehicleListScreen(listOf(vehicle), mapOf(1L to 124850L), DisplayDensity.COMFORTABLE, {}, {}, {}) }
        capture("vehicles-large-text")
    }

    @Test fun launcherIcon() {
        render { BrandHeader() }
        compose.runOnIdle {
            val icon = checkNotNull(compose.activity.getDrawable(com.jacobgain.triprabbit.R.mipmap.ic_launcher))
            val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.scale(192f / 108f, 192f / 108f)
            icon.setBounds(0, 0, 108, 108)
            icon.draw(canvas)
            val target = File("build/reports/ui/launcher-icon.png")
            target.parentFile?.mkdirs()
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val foreground = checkNotNull(compose.activity.getDrawable(com.jacobgain.triprabbit.R.drawable.ic_launcher_foreground))
            val foregroundBitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
            val foregroundCanvas = android.graphics.Canvas(foregroundBitmap)
            foregroundCanvas.scale(192f / 108f, 192f / 108f)
            foreground.setBounds(0, 0, 108, 108)
            foreground.draw(foregroundCanvas)
            File("build/reports/ui/launcher-foreground.png").outputStream().use {
                foregroundBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test fun navigationTabsRespondToTouches() {
        render {
            var current by remember { mutableStateOf("home") }
            Scaffold(bottomBar = { AppNavigation(current) { current = it } }) { padding -> Box(Modifier.padding(padding)) }
        }
        compose.onNodeWithText("Trips").performClick().assertIsSelected()
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

    @Test @Config(qualifiers = "w800dp-h360dp-mdpi") fun landscapeTrips() {
        render(tab = "history") { ReadingHistoryContent(history) }
        compose.onNodeWithContentDescription("Add trip").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
        capture("landscape-trips")
    }

    @Test @Config(qualifiers = "w800dp-h360dp-mdpi") fun landscapeForm() {
        render { AddReadingContent(AddReadingUiState(vehicle, readings.first(), recordedAt = now)) }
        compose.onNodeWithText("Save Trip").performScrollTo().assertIsDisplayed()
        capture("landscape-form")
    }

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi") fun tripsLargeText() {
        render(fontScale = 2f, tab = "history") { ReadingHistoryContent(history.copy(vehicles = listOf(vehicle))) }
        compose.onNodeWithContentDescription("Add trip").assertIsDisplayed()
        capture("trips-large-text")
    }

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi") fun compactGarageLargeText() {
        render(fontScale = 2f, tab = "vehicles") { VehicleListScreen(listOf(vehicle.copy(name = "A long vehicle name for the family")),
            mapOf(1L to 124850L), DisplayDensity.COMPACT, {}, {}) }
        capture("compact-garage-large-text")
    }

    @Test @Config(qualifiers = "w320dp-h800dp-mdpi") fun tripDetailsLargeText() {
        render(fontScale = 2f, tab = "history") { ReadingHistoryContent(history) }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Untitled trip"))
        compose.onAllNodesWithText("Untitled trip").onFirst().performClick()
        compose.onNodeWithText("Edit trip").performScrollTo().assertIsDisplayed()
        capture("trip-details-large-text")
    }

    @Test @Config(qualifiers = "w840dp-h1100dp-mdpi") fun tabletReports() {
        render(tab = "reports") { StatisticsContent(StatisticsUiState(false, vehicle, readings, stats)) }
        capture("tablet-reports")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Export PDF"))
        compose.onNodeWithText("Export PDF").assertIsDisplayed()
    }

    private fun Instant.inputDate(): String = atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
}
