package dev.jordanempire.youflow.media.engine

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.util.Log
import android.view.accessibility.CaptioningManager
import androidx.preference.PreferenceManager
import dev.jordanempire.youflow.R
import dev.jordanempire.youflow.local.history.HistoryRecordManager
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import kotlinx.coroutines.rx3.awaitSingleOrNull
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.jordanempire.youflow.media.PlayerDataSource
import dev.jordanempire.youflow.media.YouFlowPlayerService
import dev.jordanempire.youflow.media.mediaitem.MediaItemTag
import dev.jordanempire.youflow.media.resolver.VideoPlaybackResolver
import dev.jordanempire.youflow.ui.util.recaptchaUrl
import dev.jordanempire.youflow.util.ExtractorHelper
import dev.jordanempire.youflow.util.ListHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream

/** A lightweight reference to a video in the queue. Streams are resolved when it is played. */
data class QueueEntry(
    val url: String,
    val title: String,
    val uploader: String,
    val thumbnail: String?
)

enum class Phase { Idle, Loading, Buffering, Ready, Ended, Error }

/** Everything the UI observes about playback, except the fast changing position. */
data class PlayerState(
    val queue: List<QueueEntry> = emptyList(),
    val index: Int = -1,
    val info: StreamInfo? = null,
    val phase: Phase = Phase.Idle,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val videoAspect: Float = 16f / 9f,
    val isLive: Boolean = false,
    val error: String? = null,
    val recaptchaUrl: String? = null,
    val qualities: List<String> = emptyList(),
    val selectedQuality: Int = -1,
    val captions: List<String> = emptyList(),
    /** Index into [captions], or -1 when captions are off. */
    val selectedCaption: Int = -1
) {
    val entry: QueueEntry? get() = queue.getOrNull(index)
    val hasNext get() = index in 0 until queue.lastIndex
    val hasPrevious get() = index > 0
}

/**
 * Owns the one ExoPlayer, the queue and stream resolving. The foreground service only wraps
 * [sessionPlayer] in a media session so notifications, lock screen and headset buttons work.
 */
class PlaybackEngine private constructor(private val app: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dataSource = PlayerDataSource(app, DefaultBandwidthMeter.getSingletonInstance(app))

    // YouTube captions arrive as side loaded TTML. Media3 parses subtitles during extraction by
    // default, so the text renderer is switched to legacy decoding to handle them.
    private val renderersFactory = object : DefaultRenderersFactory(app) {
        override fun buildTextRenderers(
            context: android.content.Context,
            output: androidx.media3.exoplayer.text.TextOutput,
            outputLooper: android.os.Looper,
            extensionRendererMode: Int,
            out: java.util.ArrayList<androidx.media3.exoplayer.Renderer>
        ) {
            super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, out)
            out.filterIsInstance<androidx.media3.exoplayer.text.TextRenderer>().forEach {
                it.experimentalSetLegacyDecodingEnabled(true)
            }
        }
    }.setEnableDecoderFallback(true)

    val exo: ExoPlayer = ExoPlayer.Builder(app, renderersFactory)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setSeekBackIncrementMs(10_000)
        .setSeekForwardIncrementMs(10_000)
        .build()

    /** The player the media session sees: next/previous map to the queue. */
    val sessionPlayer: Player = QueueAwarePlayer(exo, this)

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

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Playback position in ms, refreshed while playing. Separate so only the seek bar recomposes. */
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()
    private val _buffered = MutableStateFlow(0L)
    val buffered: StateFlow<Long> = _buffered.asStateFlow()

    private val history by lazy { HistoryRecordManager(app) }
    private val prefs = PreferenceManager.getDefaultSharedPreferences(app)
    private var textGroups: List<Tracks.Group> = emptyList()
    private var lastSavedAt = 0L
    private var loadJob: Job? = null
    private var controller: Any? = null
    private var errorRetries = 0

    init {
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                publish()
                if (playbackState == Player.STATE_ENDED) onEnded()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                publish()
                if (!isPlaying) saveProgress()
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = publish()
            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) = publish()
            override fun onVideoSizeChanged(videoSize: VideoSize) = publish()
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) = publish()
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "player error", error)
                // Expired stream URLs surface as HTTP errors: resolve again once at the same position.
                if (errorRetries < 1 && _state.value.entry != null) {
                    errorRetries++
                    loadCurrent(startPositionMs = exo.currentPosition, forceLoad = true)
                } else {
                    _state.update { it.copy(phase = Phase.Error, error = error.message ?: error.errorCodeName) }
                }
            }
        })
        val captionsOn = (app.getSystemService(android.content.Context.CAPTIONING_SERVICE) as? CaptioningManager)?.isEnabled == true
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !captionsOn)
            .build()
        scope.launch {
            while (true) {
                if (exo.isPlaying || exo.playbackState == Player.STATE_BUFFERING) {
                    _position.value = exo.currentPosition
                    _buffered.value = exo.bufferedPosition
                }
                if (exo.isPlaying && System.currentTimeMillis() - lastSavedAt > 10_000) saveProgress()
                delay(250)
            }
        }
    }

    /** Starts the media session service (creates the notification, handles media buttons). */
    fun ensureServiceStarted() {
        if (controller != null) return
        val token = SessionToken(app, ComponentName(app, YouFlowPlayerService::class.java))
        controller = MediaController.Builder(app, token).buildAsync()
    }

    fun play(entries: List<QueueEntry>, startIndex: Int = 0) {
        if (entries.isEmpty()) return
        ensureServiceStarted()
        val current = _state.value
        if (current.queue == entries && current.index == startIndex && current.phase != Phase.Error) {
            exo.playWhenReady = true
            return
        }
        errorRetries = 0
        _state.update { it.copy(queue = entries, index = startIndex.coerceIn(entries.indices), info = null, error = null) }
        loadCurrent()
    }

    /** Persists the resume position of the current video (when history is enabled). */
    fun saveProgress() {
        val info = _state.value.info ?: return
        if (exo.playbackState == Player.STATE_IDLE) return
        lastSavedAt = System.currentTimeMillis()
        val position = exo.currentPosition
        val finished = exo.playbackState == Player.STATE_ENDED
        if (!prefs.getBoolean(app.getString(R.string.enable_watch_history_key), true)) return
        scope.launch(Dispatchers.IO) {
            runCatching { history.saveStreamState(info, if (finished) 0 else position).await() }
        }
    }

    fun next(): Boolean {
        saveProgress()
        val s = _state.value
        if (!s.hasNext) return false
        _state.update { it.copy(index = it.index + 1, info = null, error = null) }
        errorRetries = 0
        loadCurrent()
        return true
    }

    fun previous(): Boolean {
        saveProgress()
        val s = _state.value
        if (exo.currentPosition > 5_000 || !s.hasPrevious) {
            exo.seekTo(0)
            return true
        }
        _state.update { it.copy(index = it.index - 1, info = null, error = null) }
        errorRetries = 0
        loadCurrent()
        return true
    }

    fun skipTo(index: Int) {
        if (index !in _state.value.queue.indices) return
        saveProgress()
        _state.update { it.copy(index = index, info = null, error = null) }
        errorRetries = 0
        loadCurrent()
    }

    fun togglePlayPause() {
        if (exo.playbackState == Player.STATE_ENDED) exo.seekTo(0)
        exo.playWhenReady = !exo.playWhenReady
    }

    fun seekTo(positionMs: Long) {
        exo.seekTo(positionMs.coerceAtLeast(0))
        _position.value = exo.currentPosition
    }

    fun seekBy(deltaMs: Long) = seekTo((exo.currentPosition + deltaMs).coerceIn(0, exo.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))

    fun setSpeed(speed: Float) = exo.setPlaybackSpeed(speed)

    fun setVideoEnabled(enabled: Boolean) {
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, !enabled)
            .build()
    }

    /** Switch quality (index into [PlayerState.qualities]) keeping the position. */
    fun selectQuality(index: Int) {
        val info = _state.value.info ?: return
        val label = _state.value.qualities.getOrNull(index) ?: return
        resolver.playbackQuality = label
        resolveAndPrepare(info, exo.currentPosition, true)
    }

    fun selectCaption(index: Int) {
        val builder = exo.trackSelectionParameters.buildUpon()
        val group = textGroups.getOrNull(index)
        if (group == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
        }
        exo.trackSelectionParameters = builder.build()
    }

    fun stop() {
        saveProgress()
        loadJob?.cancel()
        exo.stop()
        exo.clearMediaItems()
        _state.value = PlayerState()
    }

    private fun onEnded() {
        if (!dev.jordanempire.youflow.ui.settings.AppSettings.get(app).autoplay.value) return
        if (!next()) {
            val type = _state.value.info?.streamType
            if (type == org.schabi.newpipe.extractor.stream.StreamType.LIVE_STREAM ||
                type == org.schabi.newpipe.extractor.stream.StreamType.AUDIO_LIVE_STREAM
            ) return
            // Autoplay the first related video once the queue is done.
            val related = _state.value.info?.relatedItems?.filterIsInstance<StreamInfoItem>()?.firstOrNull() ?: return
            val entry = QueueEntry(related.url, related.name, related.uploaderName.orEmpty(), null)
            _state.update { it.copy(queue = it.queue + entry, index = it.queue.size, info = null) }
            loadCurrent()
        }
    }

    private fun loadCurrent(startPositionMs: Long = 0, forceLoad: Boolean = false) {
        val entry = _state.value.entry ?: return
        loadJob?.cancel()
        loadJob = scope.launch {
            _state.update { it.copy(phase = Phase.Loading, error = null, recaptchaUrl = null) }
            try {
                val info = withContext(Dispatchers.IO) {
                    ExtractorHelper.getStreamInfo(ServiceList.YouTube.serviceId, entry.url, forceLoad).await()
                }
                _state.update { it.copy(info = info) }
                val start = if (startPositionMs > 0) startPositionMs else savedPosition(info)
                if (prefs.getBoolean(app.getString(R.string.enable_watch_history_key), true)) {
                    scope.launch(Dispatchers.IO) { runCatching { history.onViewed(info).awaitSingleOrNull() } }
                }
                resolveAndPrepare(info, start, true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "load failed", e)
                val url = e.recaptchaUrl()
                _state.update {
                    it.copy(
                        phase = Phase.Error,
                        error = if (url != null) "YouTube wants to check you're not a robot" else e.message ?: e.javaClass.simpleName,
                        recaptchaUrl = url
                    )
                }
            }
        }
    }

    private suspend fun savedPosition(info: StreamInfo): Long {
        if (!prefs.getBoolean(app.getString(R.string.enable_playback_resume_key), true)) return 0
        val state = withContext(Dispatchers.IO) { runCatching { history.loadStreamState(info).awaitSingleOrNull() }.getOrNull() }
        return if (state != null && !state.isFinished(info.duration)) state.progressMillis else 0
    }

    private fun resolveAndPrepare(info: StreamInfo, startPositionMs: Long, play: Boolean) {
        val source = resolver.resolve(info)
        if (source == null) {
            _state.update { it.copy(phase = Phase.Error, error = "No playable stream found") }
            return
        }
        exo.setMediaSource(source, startPositionMs.coerceAtLeast(0))
        exo.prepare()
        exo.playWhenReady = play
        _position.value = startPositionMs
        publish()
    }

    private fun publish() {
        val tag = exo.currentMediaItem?.localConfiguration?.tag as? MediaItemTag
        val quality = tag?.maybeQuality?.orElse(null)
        val labels = quality?.sortedVideoStreams?.map { stream ->
            stream.resolution + if (stream.isVideoOnly) "" else ""
        }.orEmpty()
        textGroups = exo.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT && it.length > 0 }
        val captionLabels = textGroups.map { g ->
            val f = g.getTrackFormat(0)
            f.label ?: f.language?.let { java.util.Locale.forLanguageTag(it).displayLanguage } ?: "Captions"
        }
        val selectedCaption = if (exo.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)) -1
        else textGroups.indexOfFirst { it.isSelected }
        val size = exo.videoSize
        val aspect = if (size.width > 0 && size.height > 0) size.width * size.pixelWidthHeightRatio / size.height else null
        _state.update { s ->
            s.copy(
                phase = when {
                    s.phase == Phase.Error || s.phase == Phase.Loading && exo.mediaItemCount == 0 -> s.phase
                    exo.playbackState == Player.STATE_BUFFERING -> Phase.Buffering
                    exo.playbackState == Player.STATE_ENDED -> Phase.Ended
                    exo.playbackState == Player.STATE_READY -> Phase.Ready
                    else -> s.phase
                },
                isPlaying = exo.isPlaying,
                playWhenReady = exo.playWhenReady,
                durationMs = exo.duration.takeIf { it > 0 } ?: 0,
                speed = exo.playbackParameters.speed,
                videoAspect = aspect ?: s.videoAspect,
                isLive = exo.isCurrentMediaItemLive,
                qualities = if (labels.isNotEmpty()) labels else s.qualities,
                selectedQuality = quality?.selectedVideoStreamIndex ?: s.selectedQuality,
                captions = captionLabels,
                selectedCaption = selectedCaption
            )
        }
    }

    companion object {
        private const val TAG = "PlaybackEngine"

        @Volatile
        private var instance: PlaybackEngine? = null

        fun get(app: Application): PlaybackEngine =
            instance ?: synchronized(this) { instance ?: PlaybackEngine(app).also { instance = it } }
    }
}
