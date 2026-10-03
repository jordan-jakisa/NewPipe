/*
 * SPDX-FileCopyrightText: 2018-2026 NewPipe contributors <https://newpipe.net>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.jordanempire.youflow.util

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import java.util.concurrent.TimeUnit
import dev.jordanempire.youflow.R
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService

/**
 * This app only supports YouTube, so every helper here is pinned to it.
 */
object ServiceHelper {
    private val YOUTUBE: StreamingService = ServiceList.YouTube

    @JvmStatic
    @DrawableRes
    fun getIcon(@Suppress("UNUSED_PARAMETER") serviceId: Int): Int {
        return R.drawable.ic_smart_display
    }

    @JvmStatic
    fun getTranslatedFilterString(filter: String, context: Context): String {
        return when (filter) {
            "all" -> context.getString(R.string.all)
            "videos", "music_videos" -> context.getString(R.string.videos_string)
            "channels" -> context.getString(R.string.channels)
            "playlists", "music_playlists" -> context.getString(R.string.playlists)
            "music_songs" -> context.getString(R.string.songs)
            "music_albums" -> context.getString(R.string.albums)
            "music_artists" -> context.getString(R.string.artists)
            else -> filter
        }
    }

    /**
     * Get a resource string with instructions for importing YouTube subscriptions.
     *
     * @return the string resource containing the instructions
     */
    @JvmStatic
    @StringRes
    fun getImportInstructions(): Int {
        return R.string.import_youtube_instructions
    }

    /**
     * The only supported service is always YouTube.
     */
    @JvmStatic
    fun getSelectedServiceId(@Suppress("UNUSED_PARAMETER") context: Context): Int {
        return YOUTUBE.serviceId
    }

    @JvmStatic
    fun getSelectedService(@Suppress("UNUSED_PARAMETER") context: Context): StreamingService {
        return YOUTUBE
    }

    /**
     * @param serviceId the id of the service
     * @return the service corresponding to the provided id
     * @throws java.util.NoSuchElementException if there is no service with the provided id
     */
    @JvmStatic
    fun getServiceById(serviceId: Int): StreamingService {
        return ServiceList.all().firstNotNullOf { it.takeIf { it.serviceId == serviceId } }
    }

    @JvmStatic
    fun getCacheExpirationMillis(@Suppress("UNUSED_PARAMETER") serviceId: Int): Long {
        return TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)
    }
}
