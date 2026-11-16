package com.jacobgain.triprabbit.feature.statistics

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.core.graphics.createBitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfReportDialog(uri: Uri, onDismiss: () -> Unit, isSample: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pageCount by remember(uri) { mutableIntStateOf(0) }
    var error by remember(uri) { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var message by remember { mutableStateOf<String?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destination ->
        if (destination != null) scope.launch {
            saving = true
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        context.contentResolver.openOutputStream(destination, "wt")?.use { input.copyTo(it) }
                            ?: error("Could not open the destination.")
                    } ?: error("Could not open the report.")
                }
                message = "PDF saved"
            } catch (failure: Exception) {
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                message = "Could not save the PDF. Try another location."
            } finally { saving = false }
        }
    }
    LaunchedEffect(uri) {
        try {
            pageCount = withContext(Dispatchers.IO) {
                val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Could not open PDF.")
                PdfRenderer(descriptor).use { it.pageCount }
            }
        } catch (failure: Exception) {
            if (failure is kotlinx.coroutines.CancellationException) throw failure
            error = "Could not display the PDF. You can still save or share it."
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (isSample) "Sample report" else "Report preview", style = MaterialTheme.typography.titleLarge)
                Text(if (isSample) "Fictional data · no changes to your vehicles or trips" else "Your selected vehicles, dates, and units · same PDF as export", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { save.launch(if (isSample) "TripRabbit-sample.pdf" else "TripRabbit-report.pdf") }, enabled = !saving) { Text(if (saving) "Saving…" else "Save PDF") }
                    OutlinedButton(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                clipData = ClipData.newRawUri("TripRabbit report", uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share PDF"))
                        } catch (_: android.content.ActivityNotFoundException) { message = "No app is available to share PDFs." }
                    }) { Text("Share") }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (pageCount == 0 && error == null) CircularProgressIndicator()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Zoom ${(zoom * 100).toInt()}%", modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically), style = MaterialTheme.typography.bodySmall)
                    Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 1f..2.5f, modifier = Modifier.weight(1f))
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(pageCount) { index ->
                        val bitmap by produceState<Bitmap?>(null, uri, index) {
                            try { value = withContext(Dispatchers.IO) {
                                val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Could not open PDF.")
                                PdfRenderer(descriptor).use { renderer ->
                                    renderer.openPage(index).use { page ->
                                        createBitmap(page.width * 2, page.height * 2).also {
                                            it.eraseColor(android.graphics.Color.WHITE)
                                            page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        }
                                    }
                                }
                            } } catch (failure: Exception) {
                                if (failure is kotlinx.coroutines.CancellationException) throw failure
                                error = "Could not display this PDF page. You can still save or share it."
                            }
                        }
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val pageWidth = maxWidth * zoom
                            Box(Modifier.horizontalScroll(rememberScrollState())) {
                                bitmap?.let { page ->
                                    Image(page.asImageBitmap(), "PDF page ${index + 1} of $pageCount",
                                        Modifier.width(pageWidth).aspectRatio(page.width.toFloat() / page.height))
                                } ?: Box(Modifier.width(pageWidth).aspectRatio(595f / 842f))
                            }
                        }
                    }
                }
            }
        }
    }
}
