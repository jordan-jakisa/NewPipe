package dev.jordanempire.youflow.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import dev.jordanempire.youflow.database.playlist.model.PlaylistEntity
import dev.jordanempire.youflow.database.playlist.model.PlaylistRemoteEntity
import org.schabi.newpipe.extractor.stream.StreamType

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    companion object {
        private const val DEFAULT_SERVICE_ID = 0
        private const val DEFAULT_URL = "https://www.youtube.com/watch?v=cDphUib5iG4"
        private const val DEFAULT_TITLE = "Test Title"
        private const val DEFAULT_NAME = "Test Name"
        private val DEFAULT_TYPE = StreamType.VIDEO_STREAM
        private const val DEFAULT_DURATION = 480L
        private const val DEFAULT_UPLOADER_NAME = "Uploader Test"
        private const val DEFAULT_THUMBNAIL = "https://example.com/example.jpg"

        private const val DEFAULT_SECOND_SERVICE_ID = 1
        private const val DEFAULT_SECOND_URL = "https://www.youtube.com/watch?v=ncQU6iBn5Fc"

        private const val DEFAULT_THIRD_SERVICE_ID = 2
        private const val DEFAULT_THIRD_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    }

    @get:Rule
    val testHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrateDatabaseFrom2to3() {
        val databaseInV2 = testHelper.createDatabase(AppDatabase.DATABASE_NAME, Migrations.DB_VER_2)

        databaseInV2.run {
            insert(
                "streams",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("service_id", DEFAULT_SERVICE_ID)
                    put("url", DEFAULT_URL)
                    put("title", DEFAULT_TITLE)
                    put("stream_type", DEFAULT_TYPE.name)
                    put("duration", DEFAULT_DURATION)
                    put("uploader", DEFAULT_UPLOADER_NAME)
                    put("thumbnail_url", DEFAULT_THUMBNAIL)
                }
            )
            insert(
                "streams",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("service_id", DEFAULT_SECOND_SERVICE_ID)
                    put("url", DEFAULT_SECOND_URL)
                }
            )
            insert(
                "streams",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("service_id", DEFAULT_SERVICE_ID)
                }
            )
            close()
        }

        testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_3,
            true,
            Migrations.MIGRATION_2_3
        )

        testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_4,
            true,
            Migrations.MIGRATION_3_4
        )

        testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_5,
            true,
            Migrations.MIGRATION_4_5
        )

        testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_6,
            true,
            Migrations.MIGRATION_5_6
        )

        testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_7,
            true,
            Migrations.MIGRATION_6_7
        )

        testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_8,
            true,
            Migrations.MIGRATION_7_8
        )

        val databaseInV9 = testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_9,
            true,
            Migrations.MIGRATION_8_9
        )

        // Only expect 2, the one with the null url will be ignored
        assertEquals(2, queryLongs(databaseInV9, "SELECT uid FROM streams").size)
        databaseInV9.query(
            "SELECT title, stream_type, duration, uploader, thumbnail_url FROM streams " +
                "WHERE service_id = $DEFAULT_SECOND_SERVICE_ID"
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("", cursor.getString(0))
            // Should fallback to VIDEO_STREAM
            assertEquals(StreamType.VIDEO_STREAM.name, cursor.getString(1))
            assertEquals(0, cursor.getLong(2))
            assertEquals("", cursor.getString(3))
            assertEquals("", cursor.getString(4))
        }

        // The stream of the second service is removed by the migration to version 10
        testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_10,
            true,
            Migrations.MIGRATION_9_10
        )

        val migratedDatabaseV3 = getMigratedDatabase()
        val listFromDB = migratedDatabaseV3.streamDAO().getAll().blockingFirst()

        assertEquals(1, listFromDB.size)

        val streamFromMigratedDatabase = listFromDB[0]
        assertEquals(DEFAULT_SERVICE_ID, streamFromMigratedDatabase.serviceId)
        assertEquals(DEFAULT_URL, streamFromMigratedDatabase.url)
        assertEquals(DEFAULT_TITLE, streamFromMigratedDatabase.title)
        assertEquals(DEFAULT_TYPE, streamFromMigratedDatabase.streamType)
        assertEquals(DEFAULT_DURATION, streamFromMigratedDatabase.duration)
        assertEquals(DEFAULT_UPLOADER_NAME, streamFromMigratedDatabase.uploader)
        assertEquals(DEFAULT_THUMBNAIL, streamFromMigratedDatabase.thumbnailUrl)
        assertNull(streamFromMigratedDatabase.viewCount)
        assertNull(streamFromMigratedDatabase.textualUploadDate)
        assertNull(streamFromMigratedDatabase.uploadDate)
        assertNull(streamFromMigratedDatabase.isUploadDateApproximation)
    }

    @Test
    fun migrateDatabaseFrom7to8() {
        val databaseInV7 = testHelper.createDatabase(AppDatabase.DATABASE_NAME, Migrations.DB_VER_7)

        val defaultSearch1 = " abc "
        val defaultSearch2 = " abc"

        val serviceId = DEFAULT_SERVICE_ID // YouTube
        // Use id different to YouTube because two searches with the same query
        // but different service are considered not equal.
        val otherServiceId = 1

        databaseInV7.run {
            insert(
                "search_history",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("service_id", serviceId)
                    put("search", defaultSearch1)
                }
            )
            insert(
                "search_history",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("service_id", serviceId)
                    put("search", defaultSearch2)
                }
            )
            insert(
                "search_history",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("service_id", otherServiceId)
                    put("search", defaultSearch1)
                }
            )
            insert(
                "search_history",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("service_id", otherServiceId)
                    put("search", defaultSearch2)
                }
            )
            close()
        }

        testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_8,
            true,
            Migrations.MIGRATION_7_8
        )

        val databaseInV9 = testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_9,
            true,
            Migrations.MIGRATION_8_9
        )

        // The duplicates were merged, so one search per service is left
        assertEquals(2, queryLongs(databaseInV9, "SELECT id FROM search_history").size)
        assertEquals(
            2,
            queryLongs(databaseInV9, "SELECT DISTINCT service_id FROM search_history").size
        )

        // The search of the other service is removed by the migration to version 10
        testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_10,
            true,
            Migrations.MIGRATION_9_10
        )

        val migratedDatabaseV10 = getMigratedDatabase()
        val listFromDB = migratedDatabaseV10.searchHistoryDAO().getAll().blockingFirst()

        assertEquals(1, listFromDB.size)
        assertEquals("abc", listFromDB[0].search)
        assertEquals(serviceId, listFromDB[0].serviceId)
    }

    @Test
    fun migrateDatabaseFrom8to9() {
        val databaseInV8 = testHelper.createDatabase(AppDatabase.DATABASE_NAME, Migrations.DB_VER_8)

        val localUid1: Long
        val localUid2: Long
        val remoteUid1: Long
        val remoteUid2: Long
        databaseInV8.run {
            localUid1 = insert(
                "playlists",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("name", DEFAULT_NAME + "1")
                    put("is_thumbnail_permanent", false)
                    put("thumbnail_stream_id", -1)
                }
            )
            localUid2 = insert(
                "playlists",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("name", DEFAULT_NAME + "2")
                    put("is_thumbnail_permanent", false)
                    put("thumbnail_stream_id", -1)
                }
            )
            delete(
                "playlists",
                "uid = ?",
                Array(1) { localUid1 }
            )
            remoteUid1 = insert(
                "remote_playlists",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("service_id", DEFAULT_SERVICE_ID)
                    put("url", DEFAULT_URL)
                }
            )
            remoteUid2 = insert(
                "remote_playlists",
                SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("service_id", DEFAULT_SECOND_SERVICE_ID)
                    put("url", DEFAULT_SECOND_URL)
                }
            )
            delete(
                "remote_playlists",
                "uid = ?",
                Array(1) { remoteUid2 }
            )
            close()
        }

        testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_9,
            true,
            Migrations.MIGRATION_8_9
        )

        testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_10,
            true,
            Migrations.MIGRATION_9_10
        )

        val migratedDatabaseV9 = getMigratedDatabase()
        var localListFromDB = migratedDatabaseV9.playlistDAO().getAll().blockingFirst()
        var remoteListFromDB = migratedDatabaseV9.playlistRemoteDAO().getAll().blockingFirst()

        assertEquals(1, localListFromDB.size)
        assertEquals(localUid2, localListFromDB[0].uid)
        assertEquals(-1, localListFromDB[0].displayIndex)
        assertEquals(1, remoteListFromDB.size)
        assertEquals(remoteUid1, remoteListFromDB[0].uid)
        assertEquals(-1, remoteListFromDB[0].displayIndex)

        val localUid3 = migratedDatabaseV9.playlistDAO().insert(
            PlaylistEntity(
                name = "${DEFAULT_NAME}3",
                isThumbnailPermanent = false,
                thumbnailStreamId = -1,
                displayIndex = -1
            )
        )
        val remoteUid3 = migratedDatabaseV9.playlistRemoteDAO().insert(
            PlaylistRemoteEntity(
                serviceId = DEFAULT_THIRD_SERVICE_ID,
                orderingName = DEFAULT_NAME,
                url = DEFAULT_THIRD_URL,
                thumbnailUrl = DEFAULT_THUMBNAIL,
                uploader = DEFAULT_UPLOADER_NAME,
                displayIndex = -1,
                streamCount = 10
            )
        )

        localListFromDB = migratedDatabaseV9.playlistDAO().getAll().blockingFirst()
        remoteListFromDB = migratedDatabaseV9.playlistRemoteDAO().getAll().blockingFirst()
        assertEquals(2, localListFromDB.size)
        assertEquals(localUid3, localListFromDB[1].uid)
        assertEquals(-1, localListFromDB[1].displayIndex)
        assertEquals(2, remoteListFromDB.size)
        assertEquals(remoteUid3, remoteListFromDB[1].uid)
        assertEquals(-1, remoteListFromDB[1].displayIndex)
    }

    @Test
    fun migrateDatabaseFrom9to10() {
        val databaseInV9 = testHelper.createDatabase(AppDatabase.DATABASE_NAME, Migrations.DB_VER_9)

        databaseInV9.run {
            // Streams 1, 3 and 5 belong to YouTube, 2 and 4 to other services.
            val streamServices = mapOf(1 to 0, 2 to 1, 3 to 0, 4 to 3, 5 to 0)
            for ((uid, serviceId) in streamServices) {
                execSQL(
                    "INSERT INTO streams (uid, service_id, url, title, stream_type, duration, " +
                        "uploader) VALUES ($uid, $serviceId, 'https://example.com/$uid', " +
                        "'title $uid', 'VIDEO_STREAM', 10, 'uploader')"
                )
            }

            execSQL("INSERT INTO subscriptions (uid, service_id, url, notification_mode) VALUES (1, 0, 'a', 0)")
            execSQL("INSERT INTO subscriptions (uid, service_id, url, notification_mode) VALUES (2, 1, 'b', 0)")
            execSQL("INSERT INTO feed_group (uid, name, icon_id, sort_order) VALUES (1, 'group', 0, 0)")
            execSQL("INSERT INTO feed_group_subscription_join (group_id, subscription_id) VALUES (1, 1)")
            execSQL("INSERT INTO feed_group_subscription_join (group_id, subscription_id) VALUES (1, 2)")
            execSQL("INSERT INTO feed_last_updated (subscription_id, last_updated) VALUES (1, 1)")
            execSQL("INSERT INTO feed_last_updated (subscription_id, last_updated) VALUES (2, 1)")
            execSQL("INSERT INTO feed (stream_id, subscription_id) VALUES (1, 1)")
            execSQL("INSERT INTO feed (stream_id, subscription_id) VALUES (2, 2)")
            execSQL("INSERT INTO feed (stream_id, subscription_id) VALUES (3, 2)")

            execSQL("INSERT INTO remote_playlists (uid, service_id, url, display_index) VALUES (1, 0, 'a', 0)")
            execSQL("INSERT INTO remote_playlists (uid, service_id, url, display_index) VALUES (2, 3, 'b', 1)")
            execSQL("INSERT INTO search_history (service_id, search) VALUES (0, 'youtube')")
            execSQL("INSERT INTO search_history (service_id, search) VALUES (2, 'other')")

            execSQL("INSERT INTO stream_history (stream_id, access_date, repeat_count) VALUES (1, 1, 1)")
            execSQL("INSERT INTO stream_history (stream_id, access_date, repeat_count) VALUES (2, 1, 1)")
            execSQL("INSERT INTO stream_state (stream_id, progress_time) VALUES (1, 5)")
            execSQL("INSERT INTO stream_state (stream_id, progress_time) VALUES (4, 5)")

            // Playlist 1 has a thumbnail of a stream that gets removed, playlist 2 does not
            execSQL("INSERT INTO playlists (uid, name, is_thumbnail_permanent, thumbnail_stream_id, display_index) VALUES (1, 'one', 1, 2, 0)")
            execSQL("INSERT INTO playlists (uid, name, is_thumbnail_permanent, thumbnail_stream_id, display_index) VALUES (2, 'two', 1, 3, 1)")
            execSQL("INSERT INTO playlists (uid, name, is_thumbnail_permanent, thumbnail_stream_id, display_index) VALUES (3, 'empty', 0, -1, 2)")
            // Playlist 1: streams 1, 2, 3, 4, 5
            for (index in 0..4) {
                execSQL(
                    "INSERT INTO playlist_stream_join (playlist_id, stream_id, join_index) " +
                        "VALUES (1, ${index + 1}, $index)"
                )
            }
            // Playlist 2: streams 3, 2, 5 (with a gap in the indices already)
            execSQL("INSERT INTO playlist_stream_join (playlist_id, stream_id, join_index) VALUES (2, 3, 0)")
            execSQL("INSERT INTO playlist_stream_join (playlist_id, stream_id, join_index) VALUES (2, 2, 4)")
            execSQL("INSERT INTO playlist_stream_join (playlist_id, stream_id, join_index) VALUES (2, 5, 7)")
            close()
        }

        val db = testHelper.runMigrationsAndValidate(
            AppDatabase.DATABASE_NAME,
            Migrations.DB_VER_10,
            true,
            Migrations.MIGRATION_9_10
        )

        // Only service 0 data is left
        assertEquals(listOf(1L, 3L, 5L), queryLongs(db, "SELECT uid FROM streams ORDER BY uid"))
        assertEquals(listOf(1L), queryLongs(db, "SELECT uid FROM subscriptions"))
        assertEquals(listOf(1L), queryLongs(db, "SELECT uid FROM remote_playlists"))
        assertEquals(1, queryLongs(db, "SELECT id FROM search_history WHERE service_id = 0").size)
        assertEquals(1, queryLongs(db, "SELECT id FROM search_history").size)
        assertEquals(listOf(1L), queryLongs(db, "SELECT stream_id FROM stream_history"))
        assertEquals(listOf(1L), queryLongs(db, "SELECT stream_id FROM stream_state"))
        assertEquals(listOf(1L), queryLongs(db, "SELECT subscription_id FROM feed_last_updated"))
        assertEquals(
            listOf(1L),
            queryLongs(db, "SELECT subscription_id FROM feed_group_subscription_join")
        )
        assertEquals(listOf(1L), queryLongs(db, "SELECT stream_id FROM feed"))

        // Playlists: all are kept, the thumbnail of the removed stream is reset
        assertEquals(listOf(1L, 2L, 3L), queryLongs(db, "SELECT uid FROM playlists ORDER BY uid"))
        assertEquals(
            listOf(-1L, 3L, -1L),
            queryLongs(db, "SELECT thumbnail_stream_id FROM playlists ORDER BY uid")
        )
        assertEquals(
            listOf(0L, 1L, 0L),
            queryLongs(db, "SELECT is_thumbnail_permanent FROM playlists ORDER BY uid")
        )

        // Playlist streams: the removed streams are gone and the indices have no gaps
        assertEquals(
            listOf(1L, 3L, 5L),
            queryLongs(
                db,
                "SELECT stream_id FROM playlist_stream_join WHERE playlist_id = 1 " +
                    "ORDER BY join_index"
            )
        )
        assertEquals(
            listOf(0L, 1L, 2L),
            queryLongs(
                db,
                "SELECT join_index FROM playlist_stream_join WHERE playlist_id = 1 " +
                    "ORDER BY join_index"
            )
        )
        assertEquals(
            listOf(3L, 5L),
            queryLongs(
                db,
                "SELECT stream_id FROM playlist_stream_join WHERE playlist_id = 2 " +
                    "ORDER BY join_index"
            )
        )
        assertEquals(
            listOf(0L, 1L),
            queryLongs(
                db,
                "SELECT join_index FROM playlist_stream_join WHERE playlist_id = 2 " +
                    "ORDER BY join_index"
            )
        )

        // No dangling references are left
        assertEquals(0, db.query("PRAGMA foreign_key_check").use { it.count })
    }

    private fun queryLongs(db: SupportSQLiteDatabase, sql: String): List<Long> {
        return db.query(sql).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getLong(0))
                }
            }
        }
    }

    private fun getMigratedDatabase(): AppDatabase {
        val database: AppDatabase = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .build()
        testHelper.closeWhenFinished(database)
        return database
    }
}
