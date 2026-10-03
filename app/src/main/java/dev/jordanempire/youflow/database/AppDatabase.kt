/*
 * SPDX-FileCopyrightText: 2017-2024 NewPipe contributors <https://newpipe.net>
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.jordanempire.youflow.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.jordanempire.youflow.database.feed.dao.FeedDAO
import dev.jordanempire.youflow.database.feed.dao.FeedGroupDAO
import dev.jordanempire.youflow.database.feed.model.FeedEntity
import dev.jordanempire.youflow.database.feed.model.FeedGroupEntity
import dev.jordanempire.youflow.database.feed.model.FeedGroupSubscriptionEntity
import dev.jordanempire.youflow.database.feed.model.FeedLastUpdatedEntity
import dev.jordanempire.youflow.database.history.dao.SearchHistoryDAO
import dev.jordanempire.youflow.database.history.dao.StreamHistoryDAO
import dev.jordanempire.youflow.database.history.model.SearchHistoryEntry
import dev.jordanempire.youflow.database.history.model.StreamHistoryEntity
import dev.jordanempire.youflow.database.playlist.dao.PlaylistDAO
import dev.jordanempire.youflow.database.playlist.dao.PlaylistRemoteDAO
import dev.jordanempire.youflow.database.playlist.dao.PlaylistStreamDAO
import dev.jordanempire.youflow.database.playlist.model.PlaylistEntity
import dev.jordanempire.youflow.database.playlist.model.PlaylistRemoteEntity
import dev.jordanempire.youflow.database.playlist.model.PlaylistStreamEntity
import dev.jordanempire.youflow.database.stream.dao.StreamDAO
import dev.jordanempire.youflow.database.stream.dao.StreamStateDAO
import dev.jordanempire.youflow.database.stream.model.StreamEntity
import dev.jordanempire.youflow.database.stream.model.StreamStateEntity
import dev.jordanempire.youflow.database.subscription.SubscriptionDAO
import dev.jordanempire.youflow.database.subscription.SubscriptionEntity

@TypeConverters(Converters::class)
@Database(
    version = Migrations.DB_VER_10,
    entities = [
        SubscriptionEntity::class,
        SearchHistoryEntry::class,
        StreamEntity::class,
        StreamHistoryEntity::class,
        StreamStateEntity::class,
        PlaylistEntity::class,
        PlaylistStreamEntity::class,
        PlaylistRemoteEntity::class,
        FeedEntity::class,
        FeedGroupEntity::class,
        FeedGroupSubscriptionEntity::class,
        FeedLastUpdatedEntity::class
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDAO(): FeedDAO
    abstract fun feedGroupDAO(): FeedGroupDAO
    abstract fun playlistDAO(): PlaylistDAO
    abstract fun playlistRemoteDAO(): PlaylistRemoteDAO
    abstract fun playlistStreamDAO(): PlaylistStreamDAO
    abstract fun searchHistoryDAO(): SearchHistoryDAO
    abstract fun streamDAO(): StreamDAO
    abstract fun streamHistoryDAO(): StreamHistoryDAO
    abstract fun streamStateDAO(): StreamStateDAO
    abstract fun subscriptionDAO(): SubscriptionDAO

    companion object {
        const val DATABASE_NAME: String = "newpipe.db"
    }
}
