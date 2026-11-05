package com.example

import com.example.data.QuranDataRepository
import com.example.data.ReciterRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun reciterCatalog_containsExactly20FamousRecitersInOrder() {
        val repo = ReciterRepository()
        assertEquals(20, repo.allReciters.size)
        assertEquals("عبد الباسط عبد الصمد", repo.allReciters[0].nameAr)
        assertEquals("محمود خليل الحصري", repo.allReciters[1].nameAr)
        assertEquals("محمد صديق المنشاوي", repo.allReciters[2].nameAr)
        assertEquals("ياسر الدوسري", repo.allReciters[10].nameAr)
        assertEquals("بندر بليلة", repo.allReciters[18].nameAr)
        assertEquals("إدريس أبكر", repo.allReciters[19].nameAr)
    }

    @Test
    fun selectedVerseAudioUrl_formatsIndividualAyahCorrectly() {
        val repo = ReciterRepository()
        val alDosari = repo.getReciterById("al_dosari")
        val url = repo.resolveVerseAudioUrl(alDosari, surahNumber = 2, verseNumber = 20)
        assertNotNull(url)
        assertTrue(url!!.endsWith("/002020.mp3"))
    }

    @Test
    fun arabicDigitsFormatting_convertsCorrectly() {
        assertEquals("١١٢", QuranDataRepository.toArabicDigits(112))
        assertEquals("﴿٢٧﴾", QuranDataRepository.formatAyahSymbol(27))
    }
}
