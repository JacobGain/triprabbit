package gain.jacob.roadjournal.feature

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndToEndWorkflowTest {
    private lateinit var device:UiDevice
    private val timeout=5_000L

    @Before fun launchFreshApp(){
        device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("roadjournal.db")
        context.filesDir.resolve("datastore/app.preferences_pb").delete()
        context.startActivity(Intent(context,Class.forName("gain.jacob.roadjournal.MainActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        waitFor("Get Started")
    }

    @Test fun createVehicleAddReadingAndEditHistory(){
        click("Get Started");waitFor("Create Vehicle")
        setEditField(0,"Golf",2);setEditField(1,"120000",2);click("Create Vehicle")
        waitFor("RoadJournal");waitForContains("Golf");waitFor("120,000")

        click("Add Reading");waitFor("New reading")
        setEditField(0,"120350",1);device.pressBack();click("Save Reading");waitFor("Reading added");waitFor("120,350")

        click("History");waitFor("Golf History");waitFor("120,350 km");waitFor("+350 km")
        click("120,350 km");waitFor("Edit Reading")
        setEditField(0,"120400",1);repeat(3){device.swipe(device.displayWidth/2,device.displayHeight*4/5,device.displayWidth/2,device.displayHeight/5,20)};click("Save Changes");waitFor("120,400 km");waitFor("+400 km")

        device.pressBack();waitFor("RoadJournal");click("Vehicles");clickDescription("Add Vehicle");waitFor("Create Vehicle")
        setEditField(0,"Civic",2);setEditField(1,"80000",2);click("Create Vehicle")
        waitFor("RoadJournal");waitForContains("Civic");waitFor("80,000")
        click("Add Reading");setEditField(0,"80100",1);device.pressBack();click("Save Reading");waitFor("Reading added");waitFor("RoadJournal");waitFor("80,100")

        clickContains("Civic");waitFor("Select vehicle");click("Golf");waitForContains("Golf");waitFor("120,400")
        click("History");waitFor("Golf History");waitFor("120,400 km");Assert.assertFalse("Civic reading leaked into Golf history",device.hasObject(By.text("80,100 km")))

        device.pressHome();val context=ApplicationProvider.getApplicationContext<Context>();context.startActivity(Intent(context,Class.forName("gain.jacob.roadjournal.MainActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));waitFor("RoadJournal");waitForContains("Golf");waitFor("120,400")

    }

    private fun click(text:String){repeat(2){val node=device.wait(Until.findObject(By.text(text)),timeout);Assert.assertNotNull("Couldn't find $text",node);try{node.click();return}catch(_:androidx.test.uiautomator.StaleObjectException){}};Assert.fail("Couldn't click $text")}
    private fun clickContains(text:String){repeat(2){val node=device.wait(Until.findObject(By.textContains(text)),timeout);Assert.assertNotNull("Couldn't find text containing $text",node);try{node.click();return}catch(_:androidx.test.uiautomator.StaleObjectException){}};Assert.fail("Couldn't click text containing $text")}
    private fun clickDescription(text:String){repeat(2){val node=device.wait(Until.findObject(By.desc(text)),timeout);Assert.assertNotNull("Couldn't find description $text",node);try{node.click();return}catch(_:androidx.test.uiautomator.StaleObjectException){}};Assert.fail("Couldn't click description $text")}
    private fun waitFor(text:String){Assert.assertTrue("Couldn't find $text",device.wait(Until.hasObject(By.text(text)),timeout))}
    private fun waitForContains(text:String){Assert.assertTrue("Couldn't find text containing $text",device.wait(Until.hasObject(By.textContains(text)),timeout))}
    private fun awaitEditFields(minimum:Int):List<androidx.test.uiautomator.UiObject2>{val deadline=System.currentTimeMillis()+timeout;var fields=device.findObjects(By.clazz("android.widget.EditText"));while(fields.size<minimum&&System.currentTimeMillis()<deadline){Thread.sleep(100);fields=device.findObjects(By.clazz("android.widget.EditText"))};Assert.assertTrue("Expected $minimum editable fields",fields.size>=minimum);return fields}
    private fun setEditField(index:Int,value:String,minimum:Int){repeat(2){try{awaitEditFields(minimum)[index].text=value;return}catch(_:androidx.test.uiautomator.StaleObjectException){}};Assert.fail("Couldn't edit field $index")}
}
