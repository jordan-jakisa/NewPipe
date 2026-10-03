package dev.jordanempire.youflow.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import dev.jordanempire.youflow.MainActivity
import dev.jordanempire.youflow.settings.SettingsActivity
import dev.jordanempire.youflow.util.NavigationHelper
import dev.jordanempire.youflow.ui.theme.YouFlowTheme

class YouFlowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The watch screen and player are still the classic, view based ones for now.
        val actions = AppActions(
            openVideo = { url, title -> startActivity(NavigationHelper.getStreamIntent(this, YOUTUBE, url, title)) },
            openChannel = { url -> startActivity(NavigationHelper.getChannelIntent(this, YOUTUBE, url)) },
            openPlaylist = { url -> startActivity(NavigationHelper.getIntentByLink(this, url)) },
            openClassicUi = { startActivity(Intent(this, MainActivity::class.java)) },
            openSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        setContent {
            YouFlowTheme {
                Surface { YouFlowApp(actions) }
            }
        }
    }

    private companion object {
        val YOUTUBE = org.schabi.newpipe.extractor.ServiceList.YouTube.serviceId
    }
}
