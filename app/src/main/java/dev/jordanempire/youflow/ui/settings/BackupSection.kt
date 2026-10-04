package dev.jordanempire.youflow.ui.settings

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager
import dev.jordanempire.youflow.NewPipeDatabase
import dev.jordanempire.youflow.settings.export.BackupFileLocator
import dev.jordanempire.youflow.settings.export.ImportExportManager
import dev.jordanempire.youflow.streams.io.StoredFileHelper
import dev.jordanempire.youflow.util.NavigationHelper
import dev.jordanempire.youflow.util.ZipHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ZIP = "application/zip"

/** Export and restore the database and preferences as one zip. NewPipe backups are accepted too. */
@Composable
fun BackupRows() {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val scope = rememberCoroutineScope()
    val manager = remember { ImportExportManager(BackupFileLocator(app)) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var askPrefs by remember { mutableStateOf<Uri?>(null) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ZIP)) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    NewPipeDatabase.checkpoint()
                    manager.exportDatabase(PreferenceManager.getDefaultSharedPreferences(app), StoredFileHelper(app, uri, ZIP))
                }
            }
            Toast.makeText(
                context,
                if (result.isSuccess) "Backup saved" else "Backup failed: ${result.exceptionOrNull()?.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    ListItem(
        headlineContent = { Text("Export backup") },
        supportingContent = { Text("Subscriptions, history, playlists and settings in one zip") },
        modifier = Modifier.clickable {
            export.launch("youflow_backup_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.zip")
        }
    )
    ListItem(
        headlineContent = { Text("Restore backup") },
        supportingContent = { Text("Replaces your current data. Works with NewPipe backups too") },
        modifier = Modifier.clickable { import.launch(arrayOf(ZIP, "application/octet-stream", "*/*")) }
    )

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Replace current data?") },
            text = { Text("Your subscriptions, history and playlists are replaced by the backup. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching {
                                val file = StoredFileHelper(app, uri, ZIP)
                                if (!ZipHelper.isValidZipFile(file)) return@runCatching false
                                manager.ensureDbDirectoryExists()
                                manager.extractDb(file)
                            }.getOrDefault(false)
                        }
                        if (!ok) {
                            Toast.makeText(context, "That file is not a valid backup", Toast.LENGTH_LONG).show()
                        } else {
                            askPrefs = uri
                        }
                    }
                }) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } }
        )
    }

    askPrefs?.let { uri ->
        val finish = {
            askPrefs = null
            (context as? Activity)?.let { NavigationHelper.restartApp(it) }
        }
        AlertDialog(
            onDismissRequest = { finish() },
            title = { Text("Restore settings too?") },
            text = { Text("The backup also contains settings. The app restarts afterwards to load your data.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                val file = StoredFileHelper(app, uri, ZIP)
                                val prefs = PreferenceManager.getDefaultSharedPreferences(app)
                                if (manager.exportHasJsonPrefs(file)) manager.loadJsonPrefs(file, prefs)
                            }
                        }
                        finish()
                    }
                }) { Text("Restore settings") }
            },
            dismissButton = { TextButton(onClick = { finish() }) { Text("Keep my settings") } }
        )
    }
}
