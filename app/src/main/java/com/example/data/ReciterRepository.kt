package com.example.data

import com.example.model.ReciterInfo
import java.util.Locale

/**
 * Modular Quran Audio Provider Adapter.
 * Supports resolving verified individual-verse audio URLs (never full-surah downloads by default)
 * and word-level timing endpoints where available.
 */
interface QuranAudioProviderAdapter {
    val providerNameAr: String
    fun buildVerseAudioUrl(reciter: ReciterInfo, surahNumber: Int, verseNumber: Int): String?
}

class EveryAyahProviderAdapter : QuranAudioProviderAdapter {
    override val providerNameAr: String = "EveryAyah (تلاوات الآيات المفردة المعتمدة)"

    override fun buildVerseAudioUrl(
        reciter: ReciterInfo,
        surahNumber: Int,
        verseNumber: Int
    ): String? {
        if (reciter.everyAyahSubfolder.isBlank()) return null
        val sss = String.format(Locale.US, "%03d", surahNumber)
        val aaa = String.format(Locale.US, "%03d", verseNumber)
        return "https://everyayah.com/data/${reciter.everyAyahSubfolder}/$sss$aaa.mp3"
    }
}

class ReciterRepository {

    private val everyAyahAdapter = EveryAyahProviderAdapter()
    private val customProviderAdapters = mutableListOf<QuranAudioProviderAdapter>()

    fun registerAdditionalProvider(adapter: QuranAudioProviderAdapter) {
        customProviderAdapters.add(adapter)
    }

    /**
     * EXACTLY 20 Selected Famous Quran Reciters as mandated by specification Section 08.
     */
    val allReciters: List<ReciterInfo> = listOf(
        ReciterInfo(
            index = 1,
            id = "abdul_basit",
            nameAr = "عبد الباسط عبد الصمد",
            styleAr = "مرتل · مزامنة الآيات والكلمات",
            everyAyahSubfolder = "Abdul_Basit_Murattal_192kbps",
            quranComRecitationId = 2,
            alQuranCloudEdition = "ar.abdulbasitmurattal",
            bitrateKbps = 192
        ),
        ReciterInfo(
            index = 2,
            id = "al_hussary",
            nameAr = "محمود خليل الحصري",
            styleAr = "مرتل · مزامنة الآيات والكلمات",
            everyAyahSubfolder = "Husary_128kbps",
            quranComRecitationId = 6,
            alQuranCloudEdition = "ar.husary",
            bitrateKbps = 128
        ),
        ReciterInfo(
            index = 3,
            id = "al_minshawi",
            nameAr = "محمد صديق المنشاوي",
            styleAr = "مرتل · مزامنة الآيات والكلمات",
            everyAyahSubfolder = "Minshawy_Murattal_128kbps",
            quranComRecitationId = 9,
            alQuranCloudEdition = "ar.minshawi",
            bitrateKbps = 128
        ),
        ReciterInfo(
            index = 4,
            id = "mustafa_ismail",
            nameAr = "مصطفى إسماعيل",
            styleAr = "مرتل · تلاوة الآيات المفردة",
            everyAyahSubfolder = "Mustafa_Ismail_48kbps",
            quranComRecitationId = null,
            alQuranCloudEdition = null,
            bitrateKbps = 48
        ),
        ReciterInfo(
            index = 5,
            id = "mahmoud_ali_al_banna",
            nameAr = "محمود علي البنا",
            styleAr = "مرتل · تلاوة الآيات المفردة",
            everyAyahSubfolder = "mahmoud_ali_al_banna_32kbps",
            quranComRecitationId = null,
            alQuranCloudEdition = null,
            bitrateKbps = 32
        ),
        ReciterInfo(
            index = 6,
            id = "al_tablawi",
            nameAr = "محمد محمود الطبلاوي",
            styleAr = "مرتل · مزامنة الآيات والكلمات",
            everyAyahSubfolder = "Mohammad_al_Tablaway_128kbps",
            quranComRecitationId = 11,
            alQuranCloudEdition = null,
            bitrateKbps = 128
        ),
        ReciterInfo(
            index = 7,
            id = "al_sudais",
            nameAr = "عبد الرحمن السديس",
            styleAr = "مرتل · مزامنة الآيات والكلمات",
            everyAyahSubfolder = "Abdurrahmaan_As-Sudais_192kbps",
            quranComRecitationId = 3,
            alQuranCloudEdition = "ar.abdurrahmaansudais",
            bitrateKbps = 192
        ),
        ReciterInfo(
            index = 8,
            id = "al_shuraim",
            nameAr = "سعود الشريم",
            styleAr = "مرتل · مزامنة الآيات والكلمات",
            everyAyahSubfolder = "Saood_ash-Shuraym_128kbps",
            quranComRecitationId = 10,
            alQuranCloudEdition = "ar.saoodshuraym",
            bitrateKbps = 128
        ),
        ReciterInfo(
            index = 9,
            id = "al_muaiqly",
            nameAr = "ماهر المعيقلي",
            styleAr = "مرتل · تلاوة الآيات المفردة",
            everyAyahSubfolder = "MaherAlMuaiqly128kbps",
            quranComRecitationId = null,
            alQuranCloudEdition = "ar.mahermuaiqly",
            bitrateKbps = 128
        ),
        ReciterInfo(
            index = 10,
            id = "al_afasy",
            nameAr = "مشاري العفاسي",
            styleAr = "مرتل · مزامنة الآيات والكلمات",
            everyAyahSubfolder = "Alafasy_128kbps",
            quranComRecitationId = 7,
            alQuranCloudEdition = "ar.alafasy",
            bitrateKbps = 128
        ),
        ReciterInfo(
            index = 11,
            id = "al_dosari",
            nameAr = "ياسر الدوسري",
            styleAr = "مرتل · تلاوة الآيات المفردة",
            everyAyahSubfolder = "Yasser_Ad-Dussary_128kbps",
            quranComRecitationId = null,
            alQuranCloudEdition = null,
            bitrateKbps = 128
        ),
        ReciterInfo(
            index = 12,
            id = "al_ghamdi",
            nameAr = "سعد الغامدي",
            styleAr = "مرتل · تلاوة الآيات المفردة",
            everyAyahSubfolder = "Ghamadi_40kbps",
            quranComRecitationId = null,
            alQuranCloudEdition = null,
            bitrateKbps = 40
        ),
        ReciterInfo(
            index = 13,
            id = "al_ajmi",
            nameAr = "أحمد العجمي",
            styleAr = "مرتل · تلاوة الآيات المفردة",
            everyAyahSubfolder = "ahmed_ibn_ali_al_ajamy_128kbps",
            quranComRecitationId = null,
            alQuranCloudEdition = "ar.ahmedajamy",
            bitrateKbps = 128
        ),
        ReciterInfo(
            index = 14,
            id = "al_shatri",
            nameAr = "أبو بكر الشاطري",
            styleAr = "مرتل · مزامنة الآيات والكلمات",
            everyAyahSubfolder = "Abu_Bakr_Ash-Shaatree_128kbps",
            quranComRecitationId = 4,
            alQuranCloudEdition = "ar.shaatree",
            bitrateKbps = 128
        ),
        ReciterInfo(
            index = 15,
            id = "al_qatami",
            nameAr = "ناصر القطامي",
            styleAr = "مرتل · تلاوة الآيات المفردة",
            everyAyahSubfolder = "Nasser_Alqatami_128kbps",
            quranComRecitationId = null,
            alQuranCloudEdition = null,
            bitrateKbps = 128
        ),
        ReciterInfo(
            index = 16,
            id = "fares_abbad",
            nameAr = "فارس عباد",
            styleAr = "مرتل · تلاوة الآيات المفردة",
            everyAyahSubfolder = "Fares_Abbad_64kbps",
            quranComRecitationId = null,
            alQuranCloudEdition = null,
            bitrateKbps = 64
        ),
        ReciterInfo(
            index = 17,
            id = "ali_jaber",
            nameAr = "علي جابر",
            styleAr = "مرتل · تلاوة الآيات المفردة",
            everyAyahSubfolder = "Ali_Jaber_64kbps",
            quranComRecitationId = null,
            alQuranCloudEdition = null,
            bitrateKbps = 64
        ),
        ReciterInfo(
            index = 18,
            id = "al_juhany",
            nameAr = "عبد الله الجهني",
            styleAr = "مرتل · تلاوة الآيات المفردة",
            everyAyahSubfolder = "Abdullaah_3awwaad_Al-Juhaynee_128kbps",
            quranComRecitationId = null,
            alQuranCloudEdition = null,
            bitrateKbps = 128
        ),
        ReciterInfo(
            index = 19,
            id = "bandar_baleela",
            nameAr = "بندر بليلة",
            styleAr = "يتطلب تهيئة مزود تلاوات آيات مفردة معتمد",
            everyAyahSubfolder = "",
            quranComRecitationId = null,
            alQuranCloudEdition = null,
            bitrateKbps = 128
        ),
        ReciterInfo(
            index = 20,
            id = "idris_abkar",
            nameAr = "إدريس أبكر",
            styleAr = "يتطلب تهيئة مزود تلاوات آيات مفردة معتمد",
            everyAyahSubfolder = "",
            quranComRecitationId = null,
            alQuranCloudEdition = null,
            bitrateKbps = 128
        )
    )

    fun getReciterById(id: String): ReciterInfo =
        allReciters.find { it.id == id } ?: allReciters[10] // Default: ياسر الدوسري (as in reference video)

    fun resolveVerseAudioUrl(reciter: ReciterInfo, surahNumber: Int, verseNumber: Int): String? {
        for (adapter in customProviderAdapters) {
            val url = adapter.buildVerseAudioUrl(reciter, surahNumber, verseNumber)
            if (!url.isNullOrBlank()) return url
        }
        return everyAyahAdapter.buildVerseAudioUrl(reciter, surahNumber, verseNumber)
    }

    fun hasVerifiedPerVerseProvider(reciter: ReciterInfo): Boolean {
        return reciter.everyAyahSubfolder.isNotBlank() ||
            reciter.alQuranCloudEdition != null ||
            customProviderAdapters.any { it.buildVerseAudioUrl(reciter, 1, 1) != null }
    }
}
