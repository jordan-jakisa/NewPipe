package dev.jordanempire.youflow.ui.data

import android.content.Context
import dev.jordanempire.youflow.NewPipeDatabase
import dev.jordanempire.youflow.database.stream.model.StreamEntity
import dev.jordanempire.youflow.database.subscription.SubscriptionEntity
import dev.jordanempire.youflow.local.feed.FeedDatabaseManager
import dev.jordanempire.youflow.local.feed.service.FeedLoadManager
import dev.jordanempire.youflow.local.subscription.SubscriptionManager
import dev.jordanempire.youflow.ui.model.ChannelItem
import dev.jordanempire.youflow.ui.model.ContentItem
import dev.jordanempire.youflow.ui.model.KioskRef
import dev.jordanempire.youflow.ui.model.PlaylistItem
import dev.jordanempire.youflow.ui.model.VideoItem
import dev.jordanempire.youflow.util.ExtractorHelper
import dev.jordanempire.youflow.util.KioskTranslator
import dev.jordanempire.youflow.util.image.ImageStrategy
import dev.jordanempire.youflow.database.feed.model.FeedGroupEntity
import io.reactivex.rxjava3.core.BackpressureStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

/** Single entry point the Compose UI uses for YouTube data and the local library. */
class YouTubeRepository(private val context: Context) {

    private val serviceId = ServiceList.YouTube.serviceId
    private val database get() = NewPipeDatabase.getInstance(context)

    /** Kiosks that currently return videos, in the order the chips are shown. */
    val kiosks: List<KioskRef> by lazy {
        runCatching {
            val list = ServiceList.YouTube.kioskList
            val order = listOf("live", "trending_music", "trending_gaming", "trending_movies_and_shows")
            list.availableKiosks.filter { it in order }.sortedBy { order.indexOf(it) }.map { id ->
                KioskRef(
                    id = id,
                    url = list.getListLinkHandlerFactoryByType(id).fromId(id).url,
                    title = KioskTranslator.getTranslatedKioskName(id, context)
                )
            }
        }.getOrDefault(emptyList())
    }

    // The extractor helpers run their Single on the subscribing thread, so keep them off Main.
    suspend fun kioskVideos(kiosk: KioskRef, forceLoad: Boolean = false): List<VideoItem> =
        withContext(Dispatchers.IO) {
            val info = ExtractorHelper.getKioskInfo(serviceId, kiosk.url, forceLoad).await()
            info.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toVideo() }
        }

    /** YouTube sometimes answers the first request with a redirect loop, a second try works. */
    suspend fun kioskVideosWithRetry(kiosk: KioskRef, forceLoad: Boolean = false): List<VideoItem> =
        try {
            kioskVideos(kiosk, forceLoad)
        } catch (e: java.io.IOException) {
            kioskVideos(kiosk, true)
        }

    suspend fun suggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        ExtractorHelper.suggestionsFor(serviceId, query).await()
    }

    suspend fun search(query: String): List<ContentItem> = withContext(Dispatchers.IO) {
        val info = ExtractorHelper.searchFor(serviceId, query, emptyList(), "").await()
        info.relatedItems.mapNotNull { it.toContent() }
    }

    /** Videos from subscribed channels, newest first, with resume progress. */
    suspend fun feed(): List<VideoItem> = withContext(Dispatchers.IO) {
        val streams = FeedDatabaseManager(context)
            .getStreams(FeedGroupEntity.GROUP_ALL_ID, true, true, false)
            .awaitSingleOrNull()
            .orEmpty()
        streams.map { it.stream.toVideo(it.stateProgressMillis) }
    }

    /** Fetches new uploads for every subscription. Suspends until the refresh is done. */
    suspend fun refreshFeed() = withContext(Dispatchers.IO) {
        FeedLoadManager(context).startLoading(ignoreOutdatedThreshold = true).await()
    }

    fun subscriptions(): Flow<List<ChannelItem>> =
        SubscriptionManager(context).subscriptions().toObservable().asFlow().map { list ->
            list.map { it.toChannel() }
        }

    suspend fun subscribe(channel: ChannelItem) = withContext(Dispatchers.IO) {
        SubscriptionManager(context).insertSubscription(
            SubscriptionEntity().apply {
                serviceId = this@YouTubeRepository.serviceId
                url = channel.url
                name = channel.name
                avatarUrl = channel.avatar
                subscriberCount = channel.subscribers
                description = channel.description
            }
        )
    }

    suspend fun unsubscribe(channelUrl: String) = withContext(Dispatchers.IO) {
        SubscriptionManager(context).deleteSubscription(serviceId, channelUrl).await()
    }

    fun history(): Flow<List<VideoItem>> =
        database.streamHistoryDAO().history.toObservable().asFlow().map { list ->
            list.map { it.streamEntity.toVideo() }
        }

    fun playlists(): Flow<List<PlaylistItem>> =
        dev.jordanempire.youflow.local.playlist.LocalPlaylistManager(database)
            .playlists.toObservable().asFlow().map { list ->
                list.map {
                    PlaylistItem(
                        url = "local:${it.uid}",
                        name = it.orderingName.orEmpty(),
                        thumbnail = it.thumbnailUrl,
                        uploader = null,
                        streamCount = it.streamCount
                    )
                }
            }

    private fun SubscriptionEntity.toChannel() = ChannelItem(
        url = url.orEmpty(),
        name = name.orEmpty(),
        avatar = avatarUrl,
        subscribers = subscriberCount,
        description = description
    )

    private fun InfoItem.toContent(): ContentItem? = when (this) {
        is StreamInfoItem -> toVideo()
        is ChannelInfoItem -> ChannelItem(
            url = url,
            name = name,
            avatar = ImageStrategy.choosePreferredImage(thumbnails),
            subscribers = subscriberCount.takeIf { it >= 0 },
            description = description
        )
        is PlaylistInfoItem -> PlaylistItem(
            url = url,
            name = name,
            thumbnail = ImageStrategy.choosePreferredImage(thumbnails),
            uploader = uploaderName,
            streamCount = streamCount
        )
        else -> null
    }

    private fun StreamInfoItem.toVideo() = VideoItem(
        url = url,
        title = name,
        channel = uploaderName.orEmpty(),
        channelUrl = uploaderUrl,
        thumbnail = ImageStrategy.choosePreferredImage(thumbnails),
        avatar = ImageStrategy.choosePreferredImage(uploaderAvatars),
        durationSeconds = duration,
        views = viewCount.takeIf { it >= 0 },
        uploaded = textualUploadDate,
        isLive = streamType == StreamType.LIVE_STREAM || streamType == StreamType.AUDIO_LIVE_STREAM,
        isShort = isShortFormContent
    )

    private fun StreamEntity.toVideo(progressMillis: Long? = null) = VideoItem(
        url = url,
        title = title,
        channel = uploader,
        channelUrl = uploaderUrl,
        thumbnail = thumbnailUrl,
        avatar = null,
        durationSeconds = duration,
        views = viewCount,
        uploaded = textualUploadDate,
        isLive = streamType == StreamType.LIVE_STREAM || streamType == StreamType.AUDIO_LIVE_STREAM,
        isShort = false,
        progress = if (progressMillis != null && duration > 0) {
            (progressMillis / 1000f / duration).coerceIn(0f, 1f)
        } else {
            0f
        }
    )
}
