package dev.jordanempire.youflow.media

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.jordanempire.youflow.media.engine.PlaybackEngine
import dev.jordanempire.youflow.ui.YouFlowActivity

/** Foreground media session around the shared [PlaybackEngine]: notification, lock screen, headset buttons. */
class YouFlowPlayerService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val engine = PlaybackEngine.get(application)
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, YouFlowActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, engine.sessionPlayer).setSessionActivity(open).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
