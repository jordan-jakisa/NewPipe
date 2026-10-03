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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import dev.jordanempire.youflow.ui.model.CommentItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import dev.jordanempire.youflow.ui.model.ChannelDetails
import dev.jordanempire.youflow.ui.model.ChannelTab
import dev.jordanempire.youflow.ui.model.PlaylistDetails
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

    /**
     * YouTube sometimes answers with a burst of 3xx/4xx/5xx responses ("Too many follow-up
     * requests"). A short backoff and another try usually gets through.
     */
    suspend fun kioskVideosWithRetry(kiosk: KioskRef, forceLoad: Boolean = false): List<VideoItem> {
        var delayMs = 1_500L
        repeat(2) {
            try {
                return kioskVideos(kiosk, forceLoad || it > 0)
            } catch (e: java.io.IOException) {
                kotlinx.coroutines.delay(delayMs)
                delayMs *= 2
            }
        }
        return kioskVideos(kiosk, true)
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

    /** One page of a list plus the token for the next one. */
    class Paged<T>(val items: List<T>, val next: Page?)

    class LoadedChannel(val details: ChannelDetails, val handlers: List<ListLinkHandler>)

    suspend fun channel(url: String): LoadedChannel = withContext(Dispatchers.IO) {
        val info: ChannelInfo = ExtractorHelper.getChannelInfo(serviceId, url, false).await()
        val labels = mapOf(
            "videos" to "Videos", "shorts" to "Shorts", "livestreams" to "Live",
            "playlists" to "Playlists", "albums" to "Releases", "podcasts" to "Podcasts",
            "courses" to "Courses"
        )
        val handlers = mutableListOf<ListLinkHandler>()
        val tabs = mutableListOf<ChannelTab>()
        info.tabs.forEach { handler ->
            val key = handler.contentFilters.firstOrNull()
            val label = labels[key] ?: return@forEach
            tabs += ChannelTab(key.orEmpty(), label, handlers.size)
            handlers += handler
        }
        LoadedChannel(
            ChannelDetails(
                url = info.url,
                name = info.name,
                avatar = ImageStrategy.choosePreferredImage(info.avatars),
                banner = ImageStrategy.choosePreferredImage(info.banners),
                subscribers = info.subscriberCount.takeIf { it >= 0 },
                description = info.description,
                tabs = tabs
            ),
            handlers
        )
    }

    suspend fun channelTab(handler: ListLinkHandler, page: Page?): Paged<ContentItem> =
        withContext(Dispatchers.IO) {
            if (page == null) {
                val tab = ExtractorHelper.getChannelTab(serviceId, handler, false).await()
                Paged(tab.relatedItems.mapNotNull { it.toContent() }, tab.nextPage)
            } else {
                val more = ExtractorHelper.getMoreChannelTabItems(serviceId, handler, page).await()
                Paged(more.items.mapNotNull { it.toContent() }, more.nextPage)
            }
        }

    class LoadedPlaylist(val details: PlaylistDetails, val info: PlaylistInfo, val videos: List<VideoItem>, val next: Page?)

    suspend fun playlist(url: String): LoadedPlaylist = withContext(Dispatchers.IO) {
        val info = ExtractorHelper.getPlaylistInfo(serviceId, url, false).await()
        LoadedPlaylist(
            PlaylistDetails(
                url = info.url,
                name = info.name,
                thumbnail = ImageStrategy.choosePreferredImage(info.thumbnails),
                uploader = info.uploaderName,
                streamCount = info.streamCount,
                description = info.description?.content
            ),
            info,
            info.relatedItems.map { it.toVideo() },
            info.nextPage
        )
    }

    suspend fun morePlaylistVideos(url: String, page: Page): Paged<VideoItem> = withContext(Dispatchers.IO) {
        val more = ExtractorHelper.getMorePlaylistItems(serviceId, url, page).await()
        Paged(more.items.map { it.toVideo() }, more.nextPage)
    }

    class LoadedComments(val info: CommentsInfo, val items: List<CommentItem>, val next: Page?, val disabled: Boolean, val count: Int)

    suspend fun comments(videoUrl: String): LoadedComments = withContext(Dispatchers.IO) {
        val info = ExtractorHelper.getCommentsInfo(serviceId, videoUrl, false).await()
        LoadedComments(info, info.relatedItems.map { it.toComment() }, info.nextPage, info.isCommentsDisabled, info.commentsCount)
    }

    suspend fun moreComments(info: CommentsInfo, page: Page): Paged<CommentItem> = withContext(Dispatchers.IO) {
        val more = ExtractorHelper.getMoreCommentItems(serviceId, info, page).await()
        Paged(more.items.map { it.toComment() }, more.nextPage)
    }

    suspend fun replies(videoUrl: String, token: Any): Paged<CommentItem> = withContext(Dispatchers.IO) {
        val more = ExtractorHelper.getMoreCommentItems(serviceId, videoUrl, token as Page).await()
        Paged(more.items.map { it.toComment() }, more.nextPage)
    }

    private fun CommentsInfoItem.toComment() = CommentItem(
        id = commentId.orEmpty(),
        author = uploaderName.orEmpty(),
        avatar = ImageStrategy.choosePreferredImage(uploaderAvatars),
        text = commentText?.content?.let { androidx.core.text.HtmlCompat.fromHtml(it, androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT).toString() }.orEmpty(),
        likes = textualLikeCount?.takeIf { it.isNotBlank() } ?: likeCount.takeIf { it > 0 }?.toString(),
        posted = textualUploadDate,
        hearted = isHeartedByUploader,
        pinned = isPinned,
        isOwner = isChannelOwner,
        replyCount = replyCount.coerceAtLeast(0),
        repliesToken = replies
    )

    /**
     * Shorts from the channels you subscribe to (their Shorts tab), newest mix first. With no
     * subscriptions it falls back to a search for #shorts, because the extractor has no global feed.
     */
    suspend fun shorts(): List<VideoItem> {
        val channels = subscriptions().first().take(8)
        val fromChannels = channels.flatMap { channel ->
            runCatching {
                val loaded = channel(channel.url)
                val tab = loaded.details.tabs.firstOrNull { it.key == "shorts" } ?: return@runCatching emptyList()
                channelTab(loaded.handlers[tab.index], null).items.filterIsInstance<VideoItem>()
                    .map { it.copy(channel = it.channel.ifBlank { channel.name }, avatar = it.avatar ?: channel.avatar) }
            }.getOrDefault(emptyList())
        }
        if (fromChannels.size >= 6) return fromChannels.shuffled()
        val searched = runCatching { search("#shorts") }.getOrDefault(emptyList())
            .filterIsInstance<VideoItem>().filter { it.isShort || (it.durationSeconds in 1..61) }
        return (fromChannels + searched).distinctBy { it.url }.shuffled()
    }

    fun isSubscribed(url: String): Flow<Boolean> = subscriptions().map { list -> list.any { it.url == url } }

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
