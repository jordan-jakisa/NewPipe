package dev.jordanempire.youflow.ui

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dev.jordanempire.youflow.MainActivity
import dev.jordanempire.youflow.media.engine.PlaybackEngine
import dev.jordanempire.youflow.settings.SettingsActivity
import dev.jordanempire.youflow.ui.theme.YouFlowTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class YouFlowActivity : ComponentActivity() {
    private val window2 = WatchWindowState()
    private lateinit var engine: PlaybackEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        engine = PlaybackEngine.get(application)

        setContent {
            YouFlowTheme {
                Surface {
                    YouFlowApp(
                        window = window2,
                        onOpenClassicUi = { startActivity(Intent(this, MainActivity::class.java)) },
                        onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
                    )
                    // Fullscreen means landscape with the system bars hidden.
                    LaunchedEffect(Unit) {
                        snapshotFlow { window2.fullscreen }.collect { applyFullscreen(it) }
                    }
                }
            }
        }

        // Auto enter PiP when leaving while a video is playing on the expanded watch page.
        lifecycleScope.launch {
            snapshotFlow { window2.expanded }.collect { updatePipParams() }
        }
        lifecycleScope.launch {
            engine.state.collect { updatePipParams() }
        }
    }

    private fun updatePipParams() {
        val playing = engine.state.value.isPlaying && window2.expanded
        val aspect = engine.state.value.videoAspect.coerceIn(0.5f, 2.0f)
        setPictureInPictureParams(
            PictureInPictureParams.Builder()
                .setAspectRatio(Rational((aspect * 1000).toInt(), 1000))
                .setAutoEnterEnabled(playing)
                .setSeamlessResizeEnabled(true)
                .build()
        )
    }

    private fun applyFullscreen(fullscreen: Boolean) {
        requestedOrientation = if (fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (fullscreen) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        window2.inPip = isInPictureInPictureMode
    }

    // Leave only audio running while the app is not visible.
    override fun onStop() {
        super.onStop()
        if (engine.state.value.isPlaying) engine.setVideoEnabled(false)
    }

    override fun onStart() {
        super.onStart()
        engine.setVideoEnabled(true)
    }
}
