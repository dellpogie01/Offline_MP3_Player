package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StartupSafetyTest {
    @Test
    fun libraryIsEmptyUntilMediaPermissionIsGranted() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(MusicRepository(context).fetchLocalSongs().isEmpty())
    }
}
