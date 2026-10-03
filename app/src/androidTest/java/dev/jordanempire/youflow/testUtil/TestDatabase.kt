package dev.jordanempire.youflow.testUtil

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertSame
import dev.jordanempire.youflow.NewPipeDatabase
import dev.jordanempire.youflow.database.AppDatabase

class TestDatabase {
    companion object {
        fun createReplacingNewPipeDatabase(): AppDatabase {
            val database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()

            val databaseField = NewPipeDatabase::class.java.getDeclaredField("databaseInstance")
            databaseField.isAccessible = true
            databaseField.set(NewPipeDatabase::class, database)

            assertSame(
                "Mocking database failed!",
                database,
                NewPipeDatabase.getInstance(ApplicationProvider.getApplicationContext())
            )

            return database
        }
    }
}
