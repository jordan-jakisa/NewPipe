package dev.jordanempire.youflow.ui.subscriptions

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import dev.jordanempire.youflow.local.subscription.workers.SubscriptionExportWorker
import dev.jordanempire.youflow.local.subscription.workers.SubscriptionImportInput
import dev.jordanempire.youflow.local.subscription.workers.SubscriptionImportWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.schabi.newpipe.extractor.ServiceList

private fun enqueueImport(context: Context, input: SubscriptionImportInput) {
    val request = OneTimeWorkRequest.Builder(SubscriptionImportWorker::class.java)
        .setInputData(input.toData())
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork(SubscriptionImportWorker.WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    Toast.makeText(context, "Importing subscriptions, see the notification for progress", Toast.LENGTH_LONG).show()
}

/** Overflow menu: import from a Google Takeout file or a previous export, and export. */
@Composable
fun SubscriptionToolsMenu(onManage: () -> Unit = {}) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }

    val takeout = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { enqueueImport(context, SubscriptionImportInput.InputStreamMode(ServiceList.YouTube.serviceId, it.toString())) }
    }
    val previous = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { enqueueImport(context, SubscriptionImportInput.PreviousExportMode(it.toString())) }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let {
            SubscriptionExportWorker.schedule(context, it)
            Toast.makeText(context, "Exporting subscriptions", Toast.LENGTH_SHORT).show()
        }
    }

    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Import and export") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Manage channels") }, onClick = {
                open = false
                onManage()
            })
            DropdownMenuItem(text = { Text("Import from Google Takeout") }, onClick = {
                open = false
                takeout.launch(arrayOf("*/*"))
            })
            DropdownMenuItem(text = { Text("Import a previous export") }, onClick = {
                open = false
                previous.launch(arrayOf("*/*"))
            })
            DropdownMenuItem(
                text = { Text("Export subscriptions") },
                onClick = {
                    open = false
                    export.launch("youflow_subscriptions_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.json")
                }
            )
        }
    }
}
