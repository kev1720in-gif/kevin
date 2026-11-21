package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.FolderEntity
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaStatus
import com.example.data.local.MediaType
import com.example.data.local.UserPreferencesRepository
import com.example.ui.util.MediaFormatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KImagenCoreTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `folder and media status transitions for hide and trash restore`() = runBlocking {
        val folderId = db.folderDao().insertFolder(FolderEntity(name = "Viajes"))
        val mediaId = db.mediaDao().insertMedia(
            MediaItemEntity(
                folderId = folderId,
                displayName = "foto_playa.jpg",
                localPath = "/tmp/foto_playa.jpg",
                mediaType = MediaType.IMAGE,
                mimeType = "image/jpeg",
                sizeBytes = 2048L,
                width = 1920,
                height = 1080,
                dateTakenMillis = 1_700_000_000_000L,
                status = MediaStatus.ACTIVE
            )
        )

        assertEquals(1, db.mediaDao().observeActiveMedia().first().size)

        // Swipe down -> Hide in vault
        db.mediaDao().updateStatus(mediaId, MediaStatus.HIDDEN, trashedAt = null, hiddenAt = 1_700_000_001_000L)
        assertEquals(0, db.mediaDao().observeActiveMedia().first().size)
        assertEquals(1, db.mediaDao().observeHiddenMedia().first().size)

        // Unhide -> Back to active
        db.mediaDao().updateStatus(mediaId, MediaStatus.ACTIVE, trashedAt = null, hiddenAt = null)
        assertEquals(1, db.mediaDao().observeActiveMedia().first().size)

        // Swipe up -> Move to trash, then restore all
        db.mediaDao().updateStatus(mediaId, MediaStatus.TRASHED, trashedAt = 1_700_000_002_000L, hiddenAt = null)
        assertEquals(1, db.mediaDao().observeTrashedMedia().first().size)
        db.mediaDao().restoreAllFromTrash()
        assertEquals(0, db.mediaDao().observeTrashedMedia().first().size)
        assertEquals(1, db.mediaDao().observeActiveMedia().first().size)
    }

    @Test
    fun `vault pin hashing and verification works`() = runBlocking {
        val prefs = UserPreferencesRepository(context)
        prefs.saveVaultPin("4829")
        assertTrue(prefs.hasPinConfigured.first())
        assertTrue(prefs.verifyVaultPin("4829"))
        assertFalse(prefs.verifyVaultPin("0000"))
    }

    @Test
    fun `formatters output readable Spanish size and duration`() {
        assertEquals("0:15", MediaFormatters.formatDuration(15_000L))
        assertEquals("1:05:02", MediaFormatters.formatDuration(3_902_000L))
        assertTrue(MediaFormatters.formatBytes(1_572_864L).contains("1.50") || MediaFormatters.formatBytes(1_572_864L).contains("1,50"))
    }
}
