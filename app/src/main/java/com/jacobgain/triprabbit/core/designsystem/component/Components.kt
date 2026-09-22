package com.jacobgain.triprabbit.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.jacobgain.triprabbit.core.model.DistanceUnit
import com.jacobgain.triprabbit.core.util.grouped

@Composable
fun OdometerDisplay(value: Long, unit: DistanceUnit, modifier: Modifier = Modifier) {
    Row(modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Bottom) {
        Text(
            value.grouped(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(8.dp))
        Text(unit.abbreviation, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 7.dp))
    }
}

@Composable
fun EmptyState(title: String, message: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp)); Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(20.dp)); Button(onClick = onAction) { Text(action) } }
    }
}

@Composable
fun DestructiveConfirmationDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable private fun OdometerDisplayPreview() { MaterialTheme { OdometerDisplay(124_521, DistanceUnit.KILOMETERS, Modifier.padding(16.dp)) } }

@Preview(name = "Empty state", showBackground = true, widthDp = 320)
@Composable private fun EmptyStatePreview() { MaterialTheme { EmptyState("No readings yet", "Add your first odometer reading to start building history.", "Add Reading") } }
