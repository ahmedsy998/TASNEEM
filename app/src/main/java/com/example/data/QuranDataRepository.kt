package com.example.data

import android.content.Context
import com.example.model.QuranVerse
import com.example.model.SurahInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class QuranDataRepository(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val verseCacheDir: File by lazy {
        File(context.filesDir, "quran_uthmani_cache").apply { mkdirs() }
    }

    /**
     * Complete 114 Surahs of the Holy Quran with exact Arabic names and exact verse counts.
     */
    val allSurahs: List<SurahInfo> = listOf(
        SurahInfo(1, "الفاتحة", 7, "مكية"),
        SurahInfo(2, "البقرة", 286, "مدنية"),
        SurahInfo(3, "آل عمران", 200, "مدنية"),
        SurahInfo(4, "النساء", 176, "مدنية"),
        SurahInfo(5, "المائدة", 120, "مدنية"),
        SurahInfo(6, "الأنعام", 165, "مكية"),
        SurahInfo(7, "الأعراف", 206, "مكية"),
        SurahInfo(8, "الأنفال", 75, "مدنية"),
        SurahInfo(9, "التوبة", 129, "مدنية"),
        SurahInfo(10, "يونس", 109, "مكية"),
        SurahInfo(11, "هود", 123, "مكية"),
        SurahInfo(12, "يوسف", 111, "مكية"),
        SurahInfo(13, "الرعد", 43, "مدنية"),
        SurahInfo(14, "إبراهيم", 52, "مكية"),
        SurahInfo(15, "الحجر", 99, "مكية"),
        SurahInfo(16, "النحل", 128, "مكية"),
        SurahInfo(17, "الإسراء", 111, "مكية"),
        SurahInfo(18, "الكهف", 110, "مكية"),
        SurahInfo(19, "مريم", 98, "مكية"),
        SurahInfo(20, "طه", 135, "مكية"),
        SurahInfo(21, "الأنبياء", 112, "مكية"),
        SurahInfo(22, "الحج", 78, "مدنية"),
        SurahInfo(23, "المؤمنون", 118, "مكية"),
        SurahInfo(24, "النور", 64, "مدنية"),
        SurahInfo(25, "الفرقان", 77, "مكية"),
        SurahInfo(26, "الشعراء", 227, "مكية"),
        SurahInfo(27, "النمل", 93, "مكية"),
        SurahInfo(28, "القصص", 88, "مكية"),
        SurahInfo(29, "العنكبوت", 69, "مكية"),
        SurahInfo(30, "الروم", 60, "مكية"),
        SurahInfo(31, "لقمان", 34, "مكية"),
        SurahInfo(32, "السجدة", 30, "مكية"),
        SurahInfo(33, "الأحزاب", 73, "مدنية"),
        SurahInfo(34, "سبأ", 54, "مكية"),
        SurahInfo(35, "فاطر", 45, "مكية"),
        SurahInfo(36, "يس", 83, "مكية"),
        SurahInfo(37, "الصافات", 182, "مكية"),
        SurahInfo(38, "ص", 88, "مكية"),
        SurahInfo(39, "الزمر", 75, "مكية"),
        SurahInfo(40, "غافر", 85, "مكية"),
        SurahInfo(41, "فصلت", 54, "مكية"),
        SurahInfo(42, "الشورى", 53, "مكية"),
        SurahInfo(43, "الزخرف", 89, "مكية"),
        SurahInfo(44, "الدخان", 59, "مكية"),
        SurahInfo(45, "الجاثية", 37, "مكية"),
        SurahInfo(46, "الأحقاف", 35, "مكية"),
        SurahInfo(47, "محمد", 38, "مدنية"),
        SurahInfo(48, "الفتح", 29, "مدنية"),
        SurahInfo(49, "الحجرات", 18, "مدنية"),
        SurahInfo(50, "ق", 45, "مكية"),
        SurahInfo(51, "الذاريات", 60, "مكية"),
        SurahInfo(52, "الطور", 49, "مكية"),
        SurahInfo(53, "النجم", 62, "مكية"),
        SurahInfo(54, "القمر", 55, "مكية"),
        SurahInfo(55, "الرحمن", 78, "مدنية"),
        SurahInfo(56, "الواقعة", 96, "مكية"),
        SurahInfo(57, "الحديد", 29, "مدنية"),
        SurahInfo(58, "المجادلة", 22, "مدنية"),
        SurahInfo(59, "الحشر", 24, "مدنية"),
        SurahInfo(60, "الممتحنة", 13, "مدنية"),
        SurahInfo(61, "الصف", 14, "مدنية"),
        SurahInfo(62, "الجمعة", 11, "مدنية"),
        SurahInfo(63, "المنافقون", 11, "مدنية"),
        SurahInfo(64, "التغابن", 18, "مدنية"),
        SurahInfo(65, "الطلاق", 12, "مدنية"),
        SurahInfo(66, "التحريم", 12, "مدنية"),
        SurahInfo(67, "الملك", 30, "مكية"),
        SurahInfo(68, "القلم", 52, "مكية"),
        SurahInfo(69, "الحاقة", 52, "مكية"),
        SurahInfo(70, "المعارج", 44, "مكية"),
        SurahInfo(71, "نوح", 28, "مكية"),
        SurahInfo(72, "الجن", 28, "مكية"),
        SurahInfo(73, "المزمل", 20, "مكية"),
        SurahInfo(74, "المدثر", 56, "مكية"),
        SurahInfo(75, "القيامة", 40, "مكية"),
        SurahInfo(76, "الإنسان", 31, "مدنية"),
        SurahInfo(77, "المرسلات", 50, "مكية"),
        SurahInfo(78, "النبأ", 40, "مكية"),
        SurahInfo(79, "النازعات", 46, "مكية"),
        SurahInfo(80, "عبس", 42, "مكية"),
        SurahInfo(81, "التكوير", 29, "مكية"),
        SurahInfo(82, "الانفطار", 19, "مكية"),
        SurahInfo(83, "المطففين", 36, "مكية"),
        SurahInfo(84, "الانشقاق", 25, "مكية"),
        SurahInfo(85, "البروج", 22, "مكية"),
        SurahInfo(86, "الطارق", 17, "مكية"),
        SurahInfo(87, "الأعلى", 19, "مكية"),
        SurahInfo(88, "الغاشية", 26, "مكية"),
        SurahInfo(89, "الفجر", 30, "مكية"),
        SurahInfo(90, "البلد", 20, "مكية"),
        SurahInfo(91, "الشمس", 15, "مكية"),
        SurahInfo(92, "الليل", 21, "مكية"),
        SurahInfo(93, "الضحى", 11, "مكية"),
        SurahInfo(94, "الشرح", 8, "مكية"),
        SurahInfo(95, "التين", 8, "مكية"),
        SurahInfo(96, "العلق", 19, "مكية"),
        SurahInfo(97, "القدر", 5, "مكية"),
        SurahInfo(98, "البينة", 8, "مدنية"),
        SurahInfo(99, "الزلزلة", 8, "مدنية"),
        SurahInfo(100, "العاديات", 11, "مكية"),
        SurahInfo(101, "القارعة", 11, "مكية"),
        SurahInfo(102, "التكاثر", 8, "مكية"),
        SurahInfo(103, "العصر", 3, "مكية"),
        SurahInfo(104, "الهمزة", 9, "مكية"),
        SurahInfo(105, "الفيل", 5, "مكية"),
        SurahInfo(106, "قريش", 4, "مكية"),
        SurahInfo(107, "الماعون", 7, "مكية"),
        SurahInfo(108, "الكوثر", 3, "مكية"),
        SurahInfo(109, "الكافرون", 6, "مكية"),
        SurahInfo(110, "النصر", 3, "مدنية"),
        SurahInfo(111, "المسد", 5, "مكية"),
        SurahInfo(112, "الإخلاص", 4, "مكية"),
        SurahInfo(113, "الفلق", 5, "مكية"),
        SurahInfo(114, "الناس", 6, "مكية")
    )

    // Bundled verified Uthmani verses for instant initial preview (e.g. Surah Al-Ikhlas 112 as in the reference video, Al-Fatihah 1, Al-Mutaffifin 83:22-28 where Tasneem is mentioned, etc.)
    private val bundledVerifiedVerses: Map<String, QuranVerse> = mapOf(
        "1:1" to QuranVerse(1, 1, "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "سورة الفاتحة سميت بذلك لافتتاح الكتاب العزيز بها.", "In the name of Allah, the Entirely Merciful, the Especially Merciful."),
        "1:2" to QuranVerse(1, 2, "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ", "الثناء الكامل لجميع المحامد لله وحده رب الخلق أجمعين.", "[All] praise is [due] to Allah, Lord of the worlds -"),
        "1:3" to QuranVerse(1, 3, "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "ذي الرحمة الواسعة التي وسعت كل شيء.", "The Entirely Merciful, the Especially Merciful,"),
        "1:4" to QuranVerse(1, 4, "مَٰلِكِ يَوْمِ ٱلدِّينِ", "المالك المتصرف وحده في يوم الجزاء والحساب.", "Sovereign of the Day of Recompense."),
        "1:5" to QuranVerse(1, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "نخصك وحدك بالعبادة ونستعين بك وحدك في جميع أمورنا.", "It is You we worship and You we ask for help."),
        "1:6" to QuranVerse(1, 6, "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", "دلنا وأرشدنا وثبتنا على الطريق المستقيم.", "Guide us to the straight path -"),
        "1:7" to QuranVerse(1, 7, "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ", "طريق الذين أنعمت عليهم من النبيين والصديقين والشهداء والصالحين.", "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray."),
        "2:255" to QuranVerse(2, 255, "ٱللَّهُ لَآ إِلَٰهَ إِلَّا هُوَ ٱلْحَىُّ ٱلْقَيُّومُ ۚ لَا تَأْخُذُهُۥ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُۥ مَا فِى ٱلسَّمَٰوَٰتِ وَمَا فِى ٱلْأَرْضِ", "الله الذي لا يستحق الألوهية والعبودية إلا هو، الحي القيوم.", "Allah - there is no deity except Him, the Ever-Living, the Sustainer of [all] existence."),
        "83:22" to QuranVerse(83, 22, "إِنَّ ٱلْأَبْرَارَ لَفِى نَعِيمٍ", "إن أهل الصدق والطاعة لفي الجنة يتنعمون.", "Indeed, the righteous will be in pleasure"),
        "83:23" to QuranVerse(83, 23, "عَلَى ٱلْأَرَآئِكِ يَنظُرُونَ", "على الأسرة المزينة ينظرون إلى ما أعد الله لهم من الكرامة.", "On adorned couches, observing."),
        "83:24" to QuranVerse(83, 24, "تَعْرِفُ فِى وُجُوهِهِمْ نَضْرَةَ ٱلنَّعِيمِ", "ترى في وجوههم بهجة النعيم وحسنه.", "You will recognize in their faces the radiance of pleasure."),
        "83:25" to QuranVerse(83, 25, "يُسْقَوْنَ مِن رَّحِيقٍ مَّخْتُومٍ", "يسقون من شراب خالص صاف مختوم.", "They will be given to drink [pure] wine [which was] sealed."),
        "83:26" to QuranVerse(83, 26, "خِتَٰمُهُۥ مِسْكٌ ۚ وَفِى ذَٰلِكَ فَلْيَتَنَافَسِ ٱلْمُتَنَٰفِسُونَ", "آخره رائحة مسك زكية، وفي هذا النعيم المقيم فليتسابق المتسابقون.", "The last of it is musk. So for this let the competitors compete."),
        "83:27" to QuranVerse(83, 27, "وَمِزَاجُهُۥ مِن تَسْنِيمٍ", "ومزاج هذا الشراب من عين في الجنة تعرف لعلوها بتسنيم.", "And its mixture is of Tasneem,"),
        "83:28" to QuranVerse(83, 28, "عَيْنًا يَشْرَبُ بِهَا ٱلْمُقَرَّبُونَ", "عينًا أعدت لشرب المقربين يتلذذون بها.", "A spring from which those near [to Allah] drink."),
        "103:1" to QuranVerse(103, 1, "وَٱلْعَصْرِ", "أقسم الله بالدهر لما فيه من العبر.", "By time,"),
        "103:2" to QuranVerse(103, 2, "إِنَّ ٱلْإِنسَٰنَ لَفِى خُسْرٍ", "إن بني آدم لفي هلكة ونقصان.", "Indeed, mankind is in loss,"),
        "103:3" to QuranVerse(103, 3, "إِلَّا ٱلَّذِينَ ءَامَنُوا۟ وَعَمِلُوا۟ ٱلصَّٰلِحَٰتِ وَتَوَاصَوْا۟ بِٱلْحَقِّ وَتَوَاصَوْا۟ بِٱلصَّبْرِ", "إلا الذين آمنوا بالله وعملوا عملاً صالحًا وأوصى بعضهم بعضًا بالحق والصبر.", "Except for those who have believed and done righteous deeds and advised each other to truth and advised each other to patience."),
        "108:1" to QuranVerse(108, 1, "إِنَّآ أَعْطَيْنَٰكَ ٱلْكَوْثَرَ", "إنا أعطيناك يا محمد الخير الكثير في الدنيا والآخرة ومنه نهر الكوثر.", "Indeed, We have granted you, [O Muhammad], al-Kawthar."),
        "108:2" to QuranVerse(108, 2, "فَصَلِّ لِرَبِّكَ وَٱنْحَرْ", "فأخلص لربك صلاتك كلها واذبح ذبيحتك له وحده.", "So pray to your Lord and sacrifice [to Him alone]."),
        "108:3" to QuranVerse(108, 3, "إِنَّ شَانِئَكَ هُوَ ٱلْأَبْتَرُ", "إن مبغضك هو المنقطع عن كل خير.", "Indeed, your enemy is the one cut off."),
        "112:1" to QuranVerse(112, 1, "قُلْ هُوَ ٱللَّهُ أَحَدٌ", "قل يا أيها الرسول: هو الله المتفرد بالألوهية والكمال.", "Say, \"He is Allah, [who is] One,"),
        "112:2" to QuranVerse(112, 2, "ٱللَّهُ ٱلصَّمَدُ", "الله المقصود في قضاء الحوائج على الدوام.", "Allah, the Eternal Refuge."),
        "112:3" to QuranVerse(112, 3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "لم يكن له ولد ولم يكن له والد ولا صاحبة.", "He neither begets nor is born,"),
        "112:4" to QuranVerse(112, 4, "وَلَمْ يَكُن لَّهُۥ كُفُوًا أَحَدٌۢ", "ولم يكن له مماثلاً ولا مشابهًا أحد من خلقه.", "Nor is there to Him any equivalent.\""),
        "113:1" to QuranVerse(113, 1, "قُلْ أَعُوذُ بِرَبِّ ٱلْفَلَقِ", "قل: أعتصم برب الصبح وخالق الخلق.", "Say, \"I seek refuge in the Lord of daybreak"),
        "113:2" to QuranVerse(113, 2, "مِن شَرِّ مَا خَلَقَ", "من أذى جميع المخلوقات وشرها.", "From the evil of that which He created"),
        "113:3" to QuranVerse(113, 3, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", "ومن شر الليل شديد الظلمة إذا دخل وتغلغل.", "And from the evil of darkness when it settles"),
        "113:4" to QuranVerse(113, 4, "وَمِن شَرِّ ٱلنَّفَّٰثَٰتِ فِى ٱلْعُقَدِ", "ومن شر السواحر اللاتي ينفخن في عقد الخيوط.", "And from the evil of the blowers in knots"),
        "113:5" to QuranVerse(113, 5, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", "ومن شر الحاسد الكاره لنعم الله على عباده.", "And from the evil of an envier when he envies.\""),
        "114:1" to QuranVerse(114, 1, "قُلْ أَعُوذُ بِرَبِّ ٱلنَّاسِ", "قل: أعتصم برب الناس ومربيهم.", "Say, \"I seek refuge in the Lord of mankind,"),
        "114:2" to QuranVerse(114, 2, "مَلِكِ ٱلنَّاسِ", "المالك المتصرف في شؤونهم.", "The Sovereign of mankind,"),
        "114:3" to QuranVerse(114, 3, "إِلَٰهِ ٱلنَّاسِ", "معبودهم الحق الذي لا معبود لهم سواه.", "The God of mankind,"),
        "114:4" to QuranVerse(114, 4, "مِن شَرِّ ٱلْوَسْوَاسِ ٱلْخَنَّاسِ", "من أذى الشيطان الذي يوسوس عند الغفلة ويختفي عند ذكر الله.", "From the evil of the retreating whisperer -"),
        "114:5" to QuranVerse(114, 5, "ٱلَّذِى يُوَسْوِسُ فِى صُدُورِ ٱلنَّاسِ", "الذي يبث الوساوس والشكوك في قلوب الناس.", "Who whispers [evil] into the breasts of mankind -"),
        "114:6" to QuranVerse(114, 6, "مِنَ ٱلْجِنَّةِ وَٱلنَّاسِ", "من شياطين الجن والإنس.", "From among the jinn and mankind.\"")
    )

    fun getSurahByNumber(number: Int): SurahInfo =
        allSurahs.find { it.number == number } ?: allSurahs[111] // Default 112 Al-Ikhlas

    /**
     * Validates the user-selected verse range against the Surah's exact verse count.
     */
    fun validateVerseRange(surahNumber: Int, startVerse: Int, endVerse: Int): Pair<Int, Int> {
        val surah = getSurahByNumber(surahNumber)
        val clampedStart = startVerse.coerceIn(1, surah.verseCount)
        val clampedEnd = endVerse.coerceIn(clampedStart, surah.verseCount)
        return Pair(clampedStart, clampedEnd)
    }

    /**
     * Retrieves ONLY the selected verses [startVerse..endVerse] for the specified Surah.
     * Uses verified Uthmani text from local cache / bundled verified verses / AlQuran.cloud Uthmani API.
     */
    suspend fun getSelectedVerses(
        surahNumber: Int,
        startVerse: Int,
        endVerse: Int
    ): Result<List<QuranVerse>> = withContext(Dispatchers.IO) {
        val (validStart, validEnd) = validateVerseRange(surahNumber, startVerse, endVerse)
        val resultList = mutableListOf<QuranVerse>()

        for (ayah in validStart..validEnd) {
            val key = "$surahNumber:$ayah"
            val bundled = bundledVerifiedVerses[key]
            if (bundled != null) {
                resultList.add(bundled)
                continue
            }

            val cached = readCachedVerse(surahNumber, ayah)
            if (cached != null) {
                resultList.add(cached)
                continue
            }

            val fetched = fetchVerifiedVerseFromApi(surahNumber, ayah)
            if (fetched != null) {
                saveCachedVerse(fetched)
                resultList.add(fetched)
            } else {
                return@withContext Result.failure(
                    Exception("تعذر جلب النص القرآني العثماني الموثق للآية ($ayah) من سورة ${getSurahByNumber(surahNumber).nameAr}. يرجى التحقق من الاتصال بالإنترنت.")
                )
            }
        }
        Result.success(resultList)
    }

    private fun readCachedVerse(surahNumber: Int, verseNumber: Int): QuranVerse? {
        return try {
            val file = File(verseCacheDir, "verse_${surahNumber}_${verseNumber}.json")
            if (!file.exists()) return null
            val json = JSONObject(file.readText())
            val text = json.optString("uthmaniText", "")
            if (text.isBlank()) return null
            QuranVerse(
                surahNumber = surahNumber,
                verseNumber = verseNumber,
                uthmaniText = text,
                tafsirMuyassar = json.optString("tafsirMuyassar", ""),
                translationSaheeh = json.optString("translationSaheeh", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun saveCachedVerse(verse: QuranVerse) {
        try {
            val file = File(verseCacheDir, "verse_${verse.surahNumber}_${verse.verseNumber}.json")
            val json = JSONObject().apply {
                put("surahNumber", verse.surahNumber)
                put("verseNumber", verse.verseNumber)
                put("uthmaniText", verse.uthmaniText)
                put("tafsirMuyassar", verse.tafsirMuyassar)
                put("translationSaheeh", verse.translationSaheeh)
            }
            file.writeText(json.toString())
        } catch (_: Exception) {
        }
    }

    private fun fetchVerifiedVerseFromApi(surahNumber: Int, verseNumber: Int): QuranVerse? {
        // Primary: AlQuran.cloud single-ayah endpoint with Uthmani script, Tafsir Al-Muyassar, and Saheeh International
        try {
            val url = "https://api.alquran.cloud/v1/ayah/$surahNumber:$verseNumber/editions/quran-uthmani,ar.muyassar,en.sahih"
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val root = JSONObject(body)
                        val dataArr = root.optJSONArray("data")
                        if (dataArr != null && dataArr.length() >= 1) {
                            var uthmani = dataArr.getJSONObject(0).optString("text", "")
                            // Strip leading Bismillah from verse 1 of Surahs other than Al-Fatihah (1) and At-Tawbah (9)
                            if (verseNumber == 1 && surahNumber != 1 && surahNumber != 9) {
                                val bismillahPrefix = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ "
                                if (uthmani.startsWith(bismillahPrefix)) {
                                    uthmani = uthmani.removePrefix(bismillahPrefix).trim()
                                }
                            }
                            val tafsir = if (dataArr.length() >= 2) dataArr.getJSONObject(1).optString("text", "") else ""
                            val trans = if (dataArr.length() >= 3) dataArr.getJSONObject(2).optString("text", "") else ""
                            if (uthmani.isNotBlank()) {
                                return QuranVerse(
                                    surahNumber = surahNumber,
                                    verseNumber = verseNumber,
                                    uthmaniText = uthmani,
                                    tafsirMuyassar = tafsir,
                                    translationSaheeh = trans
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        // Fallback: Quran.com v4 single-verse Uthmani endpoint
        try {
            val url = "https://api.quran.com/api/v4/quran/verses/uthmani?verse_key=$surahNumber:$verseNumber"
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val root = JSONObject(body)
                        val versesArr = root.optJSONArray("verses")
                        if (versesArr != null && versesArr.length() > 0) {
                            val uthmani = versesArr.getJSONObject(0).optString("text_uthmani", "")
                            if (uthmani.isNotBlank()) {
                                return QuranVerse(
                                    surahNumber = surahNumber,
                                    verseNumber = verseNumber,
                                    uthmaniText = uthmani
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return null
    }

    companion object {
        private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

        fun toArabicDigits(number: Int): String {
            return number.toString().map { ch ->
                if (ch in '0'..'9') ARABIC_DIGITS[ch - '0'] else ch
            }.joinToString("")
        }

        fun formatAyahSymbol(verseNumber: Int): String {
            return "﴿${toArabicDigits(verseNumber)}﴾"
        }
    }
}
