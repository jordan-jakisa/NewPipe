package dev.jordanempire.youflow.local.subscription

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import dev.jordanempire.youflow.local.subscription.SubscriptionFragment.Companion.JSON_MIME_TYPE
import dev.jordanempire.youflow.local.subscription.workers.SubscriptionExportWorker
import dev.jordanempire.youflow.local.subscription.workers.SubscriptionImportInput
import dev.jordanempire.youflow.streams.io.NoFileManagerSafeGuard
import dev.jordanempire.youflow.streams.io.StoredFileHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * This class has to be created in onAttach() or onCreate().
 *
 * It contains registerForActivityResult calls and those
 * calls are only allowed before a fragment/activity is created.
 */
class SubscriptionsImportExportHelper(
    val fragment: Fragment
) {
    val context: Context = fragment.requireContext()

    companion object {
        val TAG: String =
            SubscriptionsImportExportHelper::class.java.simpleName + "@" + Integer.toHexString(
                hashCode()
            )
    }

    private val requestExportLauncher =
        fragment.registerForActivityResult(StartActivityForResult(), this::requestExportResult)
    private val requestImportLauncher =
        fragment.registerForActivityResult(StartActivityForResult(), this::requestImportResult)

    private fun requestExportResult(result: ActivityResult) {
        val data = result.data?.data
        if (data != null && result.resultCode == Activity.RESULT_OK) {
            SubscriptionExportWorker.schedule(context, data)
        }
    }

    private fun requestImportResult(result: ActivityResult) {
        val data = result.data?.dataString
        if (data != null && result.resultCode == Activity.RESULT_OK) {
            ImportConfirmationDialog.show(
                fragment,
                SubscriptionImportInput.PreviousExportMode(data)
            )
        }
    }

    fun onExportSelected() {
        val date = SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH).format(Date())
        val exportName = "newpipe_subscriptions_$date.json"

        NoFileManagerSafeGuard.launchSafe(
            requestExportLauncher,
            StoredFileHelper.getNewPicker(
                context,
                exportName,
                JSON_MIME_TYPE,
                null
            ),
            TAG,
            context
        )
    }

    fun onImportPreviousSelected() {
        NoFileManagerSafeGuard.launchSafe(
            requestImportLauncher,
            StoredFileHelper.getPicker(context, JSON_MIME_TYPE),
            TAG,
            context
        )
    }
}
