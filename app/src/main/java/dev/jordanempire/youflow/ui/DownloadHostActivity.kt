package dev.jordanempire.youflow.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import dev.jordanempire.youflow.download.DownloadDialog
import dev.jordanempire.youflow.util.ExtractorHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList

/**
 * Transparent host that shows the existing download dialog (quality, folder, subtitles, threads)
 * for one video, then closes itself. The Compose UI cannot host that view based dialog directly.
 */
class DownloadHostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                    if (f is DownloadDialog) finish()
                }
            },
            false
        )
        // After a rotation the dialog fragment is restored by the FragmentManager.
        if (savedInstanceState != null) return
        val url = intent.getStringExtra(EXTRA_URL) ?: return finish()
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { ExtractorHelper.getStreamInfo(ServiceList.YouTube.serviceId, url, false).await() }
            }.onSuccess { info ->
                DownloadDialog(this@DownloadHostActivity, info).show(supportFragmentManager, "downloadDialog")
            }.onFailure {
                android.widget.Toast.makeText(this@DownloadHostActivity, "Couldn't load this video to download", android.widget.Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_URL = "url"
    }
}
