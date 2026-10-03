package dev.jordanempire.youflow.media.engine

import android.app.Application
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import dev.jordanempire.youflow.media.PlayerDataSource
import dev.jordanempire.youflow.media.resolver.VideoPlaybackResolver
import dev.jordanempire.youflow.util.ExtractorHelper
import dev.jordanempire.youflow.util.ListHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.VideoStream

/** A second, session-less player used by the Shorts pager. Loops the current short. */
class ShortsPlayer(private val app: Application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dataSource = PlayerDataSource(app, DefaultBandwidthMeter.getSingletonInstance(app))
    private val resolver = VideoPlaybackResolver(
        app,
        dataSource,
        object : VideoPlaybackResolver.QualityResolver {
            override fun getDefaultResolutionIndex(sortedVideos: List<VideoStream>) =
                ListHelper.getDefaultResolutionIndex(app, sortedVideos)

            override fun getOverrideResolutionIndex(sortedVideos: List<VideoStream>, playbackQuality: String) =
                ListHelper.getResolutionIndex(app, sortedVideos, playbackQuality)
        }
    )

    val exo: ExoPlayer = ExoPlayer.Builder(app)
        .setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .also { it.repeatMode = Player.REPEAT_MODE_ONE }

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private var job: Job? = null
    private var currentUrl: String? = null

    init {
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) _loading.value = false
            }
        })
    }

    fun play(url: String) {
        if (url == currentUrl && exo.playbackState != Player.STATE_IDLE) {
            exo.playWhenReady = true
            return
        }
        currentUrl = url
        job?.cancel()
        _error.value = null
        _loading.value = true
        exo.stop()
        job = scope.launch {
            try {
                val info = withContext(Dispatchers.IO) {
                    ExtractorHelper.getStreamInfo(ServiceList.YouTube.serviceId, url, false).await()
                }
                val source = resolver.resolve(info)
                if (source == null) {
                    _error.value = "No playable stream"
                    _loading.value = false
                    return@launch
                }
                exo.setMediaSource(source)
                exo.prepare()
                exo.playWhenReady = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e("ShortsPlayer", "load failed", e)
                _error.value = e.message ?: e.javaClass.simpleName
                _loading.value = false
            }
        }
    }

    fun togglePlayPause() {
        exo.playWhenReady = !exo.playWhenReady
    }

    fun pause() {
        exo.playWhenReady = false
    }

    fun stop() {
        job?.cancel()
        currentUrl = null
        exo.stop()
    }

    companion object {
        @Volatile
        private var instance: ShortsPlayer? = null

        fun get(app: Application): ShortsPlayer =
            instance ?: synchronized(this) { instance ?: ShortsPlayer(app).also { instance = it } }
    }
}
