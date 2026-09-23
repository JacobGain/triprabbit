package com.jacobgain.triprabbit.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jacobgain.triprabbit.core.model.DistanceUnit
import com.jacobgain.triprabbit.core.util.grouped

@Composable
fun AppNavigation(current: String, onNavigate: (String) -> Unit) {
    val largeText = LocalDensity.current.fontScale > 1.4f
    val destinations = listOf(Triple("home", "Home", AppIcon.Home), Triple("history", "History", AppIcon.History),
        Triple("reports", "Reports", AppIcon.Reports), Triple("settings", "Settings", AppIcon.Settings))
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                destinations.chunked(if (largeText) 2 else 4).forEach { group ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        group.forEach { (route, label, icon) ->
                    val selected = current == route
                    val color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    val itemModifier = Modifier.weight(1f).clip(MaterialTheme.shapes.medium)
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .selectable(selected, role = Role.Tab, onClick = { onNavigate(route) }).heightIn(min = 52.dp).padding(vertical = 10.dp)
                    if (largeText) Row(itemModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        TripIcon(icon, tint = color); Spacer(Modifier.width(8.dp)); Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                    } else Column(itemModifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        TripIcon(icon, tint = color); Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                    }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PageHeading(title: String, subtitle: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(title = { Text(title, style = MaterialTheme.typography.titleLarge) }, navigationIcon = {
        if (onBack != null) IconButton(onClick = onBack) { TripIcon(AppIcon.Back, "Back") }
    }, actions = actions, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        windowInsets = WindowInsets(0, 0, 0, 0))
}

@Composable
fun SectionTitle(title: String, caption: String? = null, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            caption?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel); Spacer(Modifier.width(4.dp)); TripIcon(AppIcon.Chevron, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier, detail: String? = null, icon: AppIcon? = null) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) TripIcon(icon, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (detail != null) Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AdaptivePair(first: @Composable (Modifier) -> Unit, second: @Composable (Modifier) -> Unit) {
    if (LocalDensity.current.fontScale > 1.4f) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        first(Modifier.fillMaxWidth()); second(Modifier.fillMaxWidth())
    } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { first(Modifier.weight(1f)); second(Modifier.weight(1f)) }
}

@Composable
fun OdometerDisplay(value: Long, unit: DistanceUnit, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value.grouped(), style = MaterialTheme.typography.displayMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (unit == DistanceUnit.KILOMETERS) "kilometres" else "miles", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: AppIcon? = null,
    enabled: Boolean = true, busy: Boolean = false) {
    Button(onClick = onClick, enabled = enabled && !busy, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(16.dp)) {
        if (busy) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
        else if (icon != null) { TripIcon(icon); Spacer(Modifier.width(10.dp)) }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun FormField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    hint: String? = null, suffix: String? = null, keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true, isError: Boolean = false, enabled: Boolean = true, textStyle: TextStyle = MaterialTheme.typography.bodyLarge) {
    OutlinedTextField(value, onValueChange, modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(label) }, supportingText = hint?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } }, singleLine = singleLine, minLines = if (singleLine) 1 else 3,
        keyboardOptions = keyboardOptions, isError = isError, shape = MaterialTheme.shapes.medium,
        textStyle = textStyle, colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ))
}

@Composable
fun StatusPill(label: String, modifier: Modifier = Modifier, icon: AppIcon? = null) {
    Surface(modifier, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (icon != null) TripIcon(icon, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ActionRow(title: String, subtitle: String, icon: AppIcon, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, destructive: Boolean = false) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth(), color = Color.Transparent,
        shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall,
                    color = if (destructive) MaterialTheme.colorScheme.error else if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TripIcon(AppIcon.Chevron, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InlineMessage(message: String, error: Boolean = false) {
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TripIcon(if (error) AppIcon.Info else AppIcon.Check)
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp) }
}

@Composable
fun EmptyState(title: String, message: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconBadge(AppIcon.Gauge, Modifier.size(56.dp), accented = true)
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) PrimaryAction(action, onAction)
    }
}

@Composable
fun DestructiveConfirmationDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit,
    confirmLabel: String = "Delete") {
    AlertDialog(onDismissRequest = onDismiss, icon = { TripIcon(AppIcon.Info, tint = MaterialTheme.colorScheme.error) },
        title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
