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
        awaitText("120,000")
        compose.onNodeWithText("Add Reading").performClick()
        compose.onNodeWithText("New reading").performTextInput("120350")
        compose.onNodeWithText("Save Reading").performScrollTo().performClick()
        awaitText("120,350")
        compose.onNodeWithText("History").performClick()
        awaitText("120,350 km")
        compose.onNodeWithText("120,350 km").performClick()
        compose.onNode(hasSetTextAction() and hasText("Odometer reading")).performTextReplacement("120400")
        compose.onNodeWithText("Save Changes").performScrollTo().performClick()
        awaitText("120,400 km")
        compose.onNodeWithText("Reports").performClick()
        awaitText("Mileage insights for Golf")
        compose.onNode(hasText("400") and hasText("kilometres")).assertIsDisplayed()
        compose.onNodeWithText("Home").performClick()
        awaitText("120,400")
        compose.onNodeWithContentDescription("Vehicles").performClick()
        awaitText("Your garage.")
        compose.onNodeWithText("Vehicle details").performClick()
        compose.onNodeWithText("120,400").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Settings").performClick()
        awaitText("Appearance")
        compose.onNodeWithText("Dark").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Dark") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Dark").assertIsSelected()
        assertEquals(Color.rgb(17, 24, 20), backgroundPixel())
        compose.onNodeWithText("Home").performClick()
        awaitText("A little more clarity.")
        assertEquals(Color.rgb(17, 24, 20), backgroundPixel())
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Dark").assertIsSelected()
        compose.onNodeWithText("Light").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Light") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(Color.rgb(246, 247, 243), backgroundPixel())
        compose.onNodeWithText("System").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("System") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(Color.rgb(246, 247, 243), backgroundPixel())
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
        awaitText("1,000")

        compose.onNodeWithContentDescription("Vehicles").performClick()
        awaitText("Your garage.")
        compose.onNodeWithContentDescription("Add Vehicle").performClick()
        compose.onNodeWithText("Vehicle name").performTextInput("Second car")
        compose.onNodeWithText("Current odometer").performTextInput("2000")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Create Vehicle") and hasClickAction())
        compose.onNode(hasText("Create Vehicle") and hasClickAction()).performClick()
        awaitText("Second car")
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        compose.onNodeWithText("A little more clarity.").assertIsDisplayed()
        compose.onNodeWithText("Create Vehicle").assertDoesNotExist()
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
