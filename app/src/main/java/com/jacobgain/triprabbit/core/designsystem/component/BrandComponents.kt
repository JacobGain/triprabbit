package com.jacobgain.triprabbit.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacobgain.triprabbit.R

val BrandEvergreen = Color(0xFF123D34)
val BrandMint = Color(0xFFD1EBC7)

enum class AppIcon(@DrawableRes val resource: Int) {
    Home(R.drawable.ic_home), History(R.drawable.ic_history), Reports(R.drawable.ic_reports),
    Settings(R.drawable.ic_settings), Car(R.drawable.ic_car), Add(R.drawable.ic_add),
    Back(R.drawable.ic_back), Chevron(R.drawable.ic_chevron), Down(R.drawable.ic_down),
    Search(R.drawable.ic_search), Close(R.drawable.ic_close), Check(R.drawable.ic_check),
    Shield(R.drawable.ic_shield), Download(R.drawable.ic_download), Upload(R.drawable.ic_upload),
    Note(R.drawable.ic_note), Edit(R.drawable.ic_edit), Trash(R.drawable.ic_trash),
    Archive(R.drawable.ic_archive), Gauge(R.drawable.ic_gauge), Distance(R.drawable.ic_distance),
    Clock(R.drawable.ic_clock), Sun(R.drawable.ic_sun), Info(R.drawable.ic_info),
}

@Composable
fun TripIcon(icon: AppIcon, description: String? = null, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Icon(painterResource(icon.resource), description, modifier.size(22.dp), tint)
}

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrandEvergreen), contentAlignment = Alignment.Center) {
        val resource = BrandArtwork.mark
        if (resource != null) Image(painterResource(resource), null, Modifier.fillMaxSize().padding(6.dp))
        else Text("TR", color = Color.White, fontSize = (maxWidth.value * .29f).sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun BrandHeader(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BrandMark()
        Text("TripRabbit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

/** External artwork is wired here when delivered. Null uses the finished text-first fallback. */
object BrandArtwork {
    @DrawableRes val mark: Int? = null
    @DrawableRes val welcome: Int? = null
    @DrawableRes val dashboard: Int? = null
}

@Composable
fun BrandArtworkSlot(@DrawableRes resource: Int?, modifier: Modifier = Modifier) {
    if (resource != null) Image(painterResource(resource), contentDescription = null,
        modifier = modifier.fillMaxWidth().aspectRatio(2.4f).clip(MaterialTheme.shapes.large), contentScale = ContentScale.Crop)
}

@Composable
fun DashboardArtworkSlot() {
    if (BrandArtwork.dashboard != null) {
        BrandArtworkSlot(BrandArtwork.dashboard)
    } else {
        Surface(Modifier.fillMaxWidth().aspectRatio(2.4f), shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("EVERY MILE ADDS UP", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(8.dp))
                Text("A little further, every day.", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
fun IconBadge(icon: AppIcon, modifier: Modifier = Modifier, accented: Boolean = false) {
    Surface(modifier.size(44.dp), shape = RoundedCornerShape(14.dp),
        color = if (accented) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (accented) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) {
        Box(contentAlignment = Alignment.Center) { TripIcon(icon) }
    }
}
