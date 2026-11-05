package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.QuranDataRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read Tasneem app_name and verify 114 surahs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("تسنيم", appName)

        val quranRepo = QuranDataRepository(context)
        assertEquals(114, quranRepo.allSurahs.size)
        assertEquals("الفاتحة", quranRepo.getSurahByNumber(1).nameAr)
        assertEquals(286, quranRepo.getSurahByNumber(2).verseCount)
        assertEquals("الإخلاص", quranRepo.getSurahByNumber(112).nameAr)
        assertEquals(4, quranRepo.getSurahByNumber(112).verseCount)

        val (validStart, validEnd) = quranRepo.validateVerseRange(2, 1, 20)
        assertEquals(1, validStart)
        assertEquals(20, validEnd)
    }
}
