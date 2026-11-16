package com.jacobgain.triprabbit.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val vehicleColours = linkedMapOf(
    "red" to Color(0xFFC62828), "orange" to Color(0xFFA94B00),
    "yellow" to Color(0xFF806300), "green" to Color(0xFF23723A),
    "blue" to Color(0xFF245FBD), "purple" to Color(0xFF8046B5),
    "silver" to Color(0xFF596875), "black" to Color(0xFF454545),
)

@Composable
fun VehicleBadge(colorKey: String?, modifier: Modifier = Modifier, iconKey: String = "car") {
    val base = vehicleColours[colorKey?.trim()?.lowercase()]
    val dark = MaterialTheme.colorScheme.surface.luminanceValue() < .4f
    val ink = base?.let { if (dark) androidx.compose.ui.graphics.lerp(it, Color.White, .45f) else it }
        ?: MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier.size(40.dp), shape = MaterialTheme.shapes.medium, color = ink.copy(alpha = .12f)) {
        Box(contentAlignment = Alignment.Center) {
            TripIcon(vehicleIcons[iconKey]?.second ?: AppIcon.Car, tint = ink, modifier = Modifier.size(24.dp))
        }
    }
}

private fun Color.luminanceValue() = .2126f * red + .7152f * green + .0722f * blue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleColourPicker(value: String, enabled: Boolean, iconKey: String = "car", onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VehicleBadge(value, iconKey = iconKey)
            Text("Select colour", style = MaterialTheme.typography.titleSmall)
        }
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("") + vehicleColours.keys).forEach { key ->
                val selected = if (key.isEmpty()) value.isBlank() || value.trim().lowercase() !in vehicleColours else value.trim().equals(key, true)
                FilterChip(selected = selected, onClick = { onSelect(key) }, enabled = enabled,
                    label = { Text(key.replaceFirstChar { it.uppercase() }.ifEmpty { "Default" }) },
                    leadingIcon = { Box(Modifier.size(16.dp).background(vehicleColours[key] ?: MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)) },
                    trailingIcon = if (selected) ({ TripIcon(AppIcon.Check, modifier = Modifier.size(16.dp)) }) else null)
            }
        }
        Text("Identify this vehicle in your trips. The vehicle name stays visible too.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val vehicleIcons = linkedMapOf(
    "car" to ("Car" to AppIcon.Car),
    "pickup" to ("Pickup truck" to AppIcon.Pickup),
    "van" to ("Van" to AppIcon.Van),
    "motorcycle" to ("Motorcycle" to AppIcon.Motorcycle),
    "suv" to ("SUV" to AppIcon.Suv),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleIconPicker(value: String, colorKey: String?, enabled: Boolean, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Select icon", style = MaterialTheme.typography.titleSmall)
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vehicleIcons.forEach { (key, choice) ->
                val selected = if (value !in vehicleIcons) key == "car" else value == key
                FilterChip(selected = selected, onClick = { onSelect(key) }, enabled = enabled,
                    label = { Text(choice.first) },
                    leadingIcon = { VehicleBadge(colorKey, Modifier.size(28.dp), key) },
                    trailingIcon = if (selected) ({ TripIcon(AppIcon.Check, modifier = Modifier.size(16.dp)) }) else null)
            }
        }
    }
}
