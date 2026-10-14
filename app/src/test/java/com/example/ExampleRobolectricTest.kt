package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Game Replay Pro", appName)
  }

  @Test
  fun `circular buffer chunk creation and count`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val bufferManager = com.example.engine.CircularBufferManager(context)
    val nextChunk = bufferManager.createNextChunkFile()
    org.junit.Assert.assertNotNull(nextChunk)
    org.junit.Assert.assertTrue(nextChunk.name.startsWith("chunk_"))
    org.junit.Assert.assertTrue(nextChunk.name.endsWith(".mp4"))
  }

  @Test
  fun `replay settings defaults`() {
    val settings = com.example.data.model.ReplaySettings()
    assertEquals(60, settings.bufferDurationSeconds)
    assertEquals(60, settings.fps)
    assertEquals("1080p (FHD)", settings.resolutionLabel)
    org.junit.Assert.assertTrue(settings.floatingButtonEnabled)
  }
}
