package com.jacobgain.triprabbit.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.composed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.jacobgain.triprabbit.core.model.DistanceUnit
import com.jacobgain.triprabbit.core.util.grouped

@Composable
fun AppNavigation(current: String, onNavigate: (String) -> Unit) {
    val destinations = listOf(Triple("history", "Trips", AppIcon.History), Triple("reports", "Reports", AppIcon.Reports),
        Triple("add", "Add trip", AppIcon.Add), Triple("vehicles", "Garage", AppIcon.Garage), Triple("settings", "Settings", AppIcon.Settings))
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Surface(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).widthIn(max = 720.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shadowElevation = 0.dp) {
            Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                destinations.forEach { (route, label, icon) ->
                    val selected = current == route
                    if (route == "add") {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            FilledIconButton(onClick = { onNavigate(route) }, modifier = Modifier.size(54.dp),
                                shape = MaterialTheme.shapes.medium) { TripIcon(AppIcon.AddNavigation, "Add trip") }
                        }
                    } else {
                        val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        Column(Modifier.weight(1f).clip(MaterialTheme.shapes.medium)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .selectable(selected, role = Role.Tab, onClick = { onNavigate(route) })
                            .heightIn(min = 58.dp).padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TripIcon(icon, tint = color)
                            Text(label, style = MaterialTheme.typography.labelSmall, color = color,
                                // Keep every destination visible; screen content follows the full system text scale.
                                fontSize = (11f * LocalDensity.current.fontScale.coerceAtMost(1.2f) / LocalDensity.current.fontScale).sp,
                                maxLines = 1, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PageHeading(title: String, subtitle: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null,
    actionAlignedWithTitle: Boolean = false) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val actionWidth = maxWidth * .5f
        val stacked = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.3f
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (stacked && !actionAlignedWithTitle) {
                Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (action != null) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { action.invoke() }
            } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                    if (subtitle.isNotBlank() && !actionAlignedWithTitle) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (action != null) Box(Modifier.widthIn(max = actionWidth)) { action.invoke() }
            }
            if (actionAlignedWithTitle && subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AdaptiveSingleLineText(text: String, style: TextStyle, modifier: Modifier = Modifier.fillMaxWidth(),
    color: Color = Color.Unspecified, minFontSize: TextUnit = 10.sp) {
    val preferredSize = style.fontSize
    var fontSize by remember(text, preferredSize, minFontSize) { mutableStateOf(preferredSize) }
    Text(text, modifier = modifier, style = style.copy(fontSize = fontSize), color = color,
        maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout ->
            if (layout.didOverflowWidth && fontSize > minFontSize) {
                fontSize = (fontSize.value - 1f).coerceAtLeast(minFontSize.value).sp
            }
        })
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
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)), shadowElevation = 1.dp) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier, detail: String? = null, icon: AppIcon? = null) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) IconBadge(icon, accented = true)
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (detail != null) Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AdaptivePair(first: @Composable (Modifier) -> Unit, second: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.4f) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            first(Modifier.fillMaxWidth()); second(Modifier.fillMaxWidth())
        } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { first(Modifier.weight(1f)); second(Modifier.weight(1f)) }
    }
}

@Composable
fun OdometerDisplay(value: Long, unit: DistanceUnit, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value.grouped(), style = MaterialTheme.typography.displayMedium)
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

fun Modifier.clearFocusWhenKeyboardCloses(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    var keyboardWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0) keyboardWasVisible = true
        else if (keyboardWasVisible) {
            keyboardWasVisible = false
            focusManager.clearFocus()
        }
    }
    this
}

@Composable
fun FormField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    hint: String? = null, placeholder: String? = null, suffix: String? = null, keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true, isError: Boolean = false, enabled: Boolean = true, textStyle: TextStyle = MaterialTheme.typography.bodyLarge) {
    OutlinedTextField(value, onValueChange, modifier.fillMaxWidth().clearFocusWhenKeyboardCloses(), enabled = enabled,
        label = { Text(label) }, placeholder = placeholder?.let { { Text(it) } }, supportingText = hint?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } }, singleLine = singleLine, minLines = if (singleLine) 1 else 3,
        keyboardOptions = keyboardOptions, isError = isError, shape = MaterialTheme.shapes.medium,
        textStyle = textStyle, colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
fun ActionRow(title: String, subtitle: String?, icon: AppIcon, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, destructive: Boolean = false) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth(), color = Color.Transparent,
        shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (subtitle.isNullOrBlank()) 0.dp else 4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall,
                    color = if (destructive) MaterialTheme.colorScheme.error else if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                subtitle?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scroll = if (constraints.hasBoundedHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier
    Column(Modifier.then(scroll).fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BrandMark(Modifier.size(72.dp), withBackground = false)
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) PrimaryAction(action, onAction)
    }
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

/** High-emphasis summary shared by trips, reports and vehicle details. */
@Composable
fun JourneyCard(label: String, value: String, caption: String, icon: AppIcon, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = colors.primary,
        contentColor = colors.onPrimary) {
        Column(Modifier.background(Brush.linearGradient(listOf(colors.primary, colors.primary.copy(alpha = .82f))))
            .padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TripIcon(icon)
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            }
            Text(value, style = MaterialTheme.typography.displaySmall)
            HorizontalDivider(color = colors.onPrimary.copy(alpha = .25f))
            Text(caption, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
