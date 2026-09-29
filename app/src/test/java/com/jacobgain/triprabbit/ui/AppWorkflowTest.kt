package com.jacobgain.triprabbit.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.jacobgain.triprabbit.MainActivity
import com.jacobgain.triprabbit.TripRabbitApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.junit.Assert.assertEquals

/** Exercises real navigation, Hilt, Room, and DataStore in an isolated local Android runtime. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TripRabbitApplication::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun createLogEditAndNavigate() {
        awaitText("Get Started")
        compose.onNodeWithText("Get Started").performScrollTo().performClick()
        compose.onNodeWithText("Vehicle name").performTextInput("Golf")
        compose.onNodeWithText("Current odometer").performTextInput("120000")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Create Vehicle") and hasClickAction())
        compose.onNode(hasText("Create Vehicle") and hasClickAction()).performClick()
        awaitText("A fresh start")
        compose.onNodeWithContentDescription("Add trip").performClick()
        compose.onNodeWithText("Finish odometer").performScrollTo().performClick()
        compose.onNodeWithText("Odometer reading").performScrollTo().performTextReplacement("120350")
        compose.onNodeWithText("Apply").performScrollTo().performClick()
        compose.onNodeWithText("Save Trip").performScrollTo().performClick()
        awaitText("350 km")
        compose.onNodeWithText("Untitled trip").performClick()
        compose.onNodeWithText("Edit trip").performClick()
        awaitText("Finish odometer")
        compose.onNodeWithText("Finish odometer").performScrollTo().performClick()
        compose.onNodeWithText("Odometer reading").performScrollTo().performTextReplacement("120400")
        compose.onNodeWithText("Apply").performScrollTo().performClick()
        compose.onNodeWithText("Save Changes").performScrollTo().performClick()
        awaitText("400 km")
        compose.onNodeWithText("Reports").performClick()
        awaitText("Total distance tracked")
        compose.onAllNodesWithText("400 km").onFirst().assertIsDisplayed()
        compose.onNodeWithText("Garage").performClick()
        awaitText("1 vehicle")
        compose.onNodeWithText("Vehicle details").performClick()
        compose.onNodeWithText("120,400").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Settings").performClick()
        awaitText("Appearance")
        compose.onNodeWithText("Dark").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Dark") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Dark").assertIsSelected()
        assertEquals(Color.rgb(12, 25, 32), backgroundPixel())
        compose.onNodeWithText("Trips").performClick()
        awaitText("400 km")
        assertEquals(Color.rgb(12, 25, 32), backgroundPixel())
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Dark").assertIsSelected()
        compose.onNodeWithText("Light").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Light") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(Color.rgb(242, 246, 247), backgroundPixel())
        compose.onNodeWithText("System").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("System") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(Color.rgb(242, 246, 247), backgroundPixel())
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Privacy policy"))
        compose.onNodeWithText("Privacy policy").performClick()
        awaitText("TripRabbit privacy policy")
    }

    @Test fun creatingVehicleFromGarageDoesNotLeaveTheFormOnTheBackStack() {
        awaitText("Get Started")
        compose.onNodeWithText("Get Started").performScrollTo().performClick()
        compose.onNodeWithText("Vehicle name").performTextInput("First car")
        compose.onNodeWithText("Current odometer").performTextInput("1000")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Create Vehicle") and hasClickAction())
        compose.onNode(hasText("Create Vehicle") and hasClickAction()).performClick()
        awaitText("A fresh start")

        compose.onNodeWithText("Garage").performClick()
        awaitText("1 vehicle")
        compose.onNodeWithText("Add vehicle").performClick()
        compose.onNodeWithText("Vehicle name").performTextInput("Second car")
        compose.onNodeWithText("Current odometer").performTextInput("2000")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Create Vehicle") and hasClickAction())
        compose.onNode(hasText("Create Vehicle") and hasClickAction()).performClick()
        awaitText("A fresh start")
        compose.onNodeWithText("Garage").performClick()
        awaitText("2 vehicles")
        compose.onNodeWithText("Second car").assertExists()
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        compose.onNodeWithText("A fresh start").assertExists()
        compose.onNodeWithText("Create Vehicle").assertDoesNotExist()
    }

    @Test fun deletingOnlyVehicleShowsHomeEmptyState() {
        awaitText("Get Started")
        compose.onNodeWithText("Get Started").performScrollTo().performClick()
        compose.onNodeWithText("Vehicle name").performTextInput("Only car")
        compose.onNodeWithText("Current odometer").performTextInput("1000")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Create Vehicle") and hasClickAction())
        compose.onNode(hasText("Create Vehicle") and hasClickAction()).performClick()
        awaitText("A fresh start")
        compose.onNodeWithText("Garage").performClick()
        compose.onNodeWithText("Vehicle details").performClick()
        compose.onNodeWithText("Delete Vehicle").performScrollTo().performClick()
        compose.onNodeWithText("Delete", substring = false).performClick()
        awaitText("No active vehicles")
        compose.onNodeWithText("Add vehicle").assertIsDisplayed()
    }

    @Test fun startTripSurvivesRecreationAndCanBeFinished() {
        awaitText("Get Started")
        compose.onNodeWithText("Get Started").performScrollTo().performClick()
        compose.onNodeWithText("Vehicle name").performTextInput("Golf")
        compose.onNodeWithText("Current odometer").performTextInput("120000")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Create Vehicle") and hasClickAction())
        compose.onNode(hasText("Create Vehicle") and hasClickAction()).performClick()
        awaitText("A fresh start")
        compose.onNodeWithContentDescription("Add trip").performClick()
        compose.onNodeWithText("Start odometer").performClick()
        compose.onNodeWithText("Odometer reading").performTextReplacement("120050")
        compose.onNodeWithText("Apply").performScrollTo().performClick()
        compose.onNodeWithText("Finish odometer").performScrollTo().performClick()
        compose.onNodeWithText("Odometer reading").assertTextContains("120050")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Start trip").performScrollTo().performClick()
        awaitText("In progress")
        compose.activityRule.scenario.recreate()
        awaitText("In progress")
        compose.onNodeWithText("Untitled trip").performClick()
        compose.onNodeWithText("Finish trip").performClick()
        awaitText("Finish odometer")
        compose.onNodeWithText("Finish odometer").performClick()
        compose.onNodeWithText("Odometer reading").performTextReplacement("120175")
        compose.onNodeWithText("Apply").performScrollTo().performClick()
        compose.onNode(hasText("Finish trip") and hasClickAction()).performScrollTo().performClick()
        awaitText("125 km")
        compose.onNodeWithText("In progress").assertDoesNotExist()
        compose.onNodeWithText("Reports").performClick()
        awaitText("Total distance tracked")
        compose.onAllNodesWithText("125 km").onFirst().assertIsDisplayed()
    }

    private fun awaitText(text: String) {
        try {
            compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
            compose.mainClock.advanceTimeBy(1_000)
            compose.waitForIdle()
        } catch (error: Throwable) {
            throw AssertionError("Waiting for '$text':\n${compose.onRoot().printToString()}", error)
        }
    }

    private fun backgroundPixel(): Int {
        var pixel = 0
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            pixel = bitmap.getPixel(5, 200)
        }
        return pixel
    }
}
