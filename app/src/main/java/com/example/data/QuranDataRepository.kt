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

    // Bundled verified Uthmani verses from the Madinah Mushaf (King Fahd Glorious Quran Printing Complex — Hafs 'an Asim)
    // Includes Surah Al-Fatihah (1), Al-Baqarah (2:1-5 & 2:255), Al-Tawbah (9:1-2), An-Naml (27:30),
    // Al-Mutaffifin (83:22-28), At-Tin (95:1-8), Al-Asr (103), Al-Kawthar (108), Al-Ikhlas (112), Al-Falaq (113), An-Nas (114).
    private val bundledVerifiedVerses: Map<String, QuranVerse> = mapOf(
        "1:1" to QuranVerse(1, 1, "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "سورة الفاتحة سميت بذلك لافتتاح الكتاب العزيز بها.", "In the name of Allah, the Entirely Merciful, the Especially Merciful."),
        "1:2" to QuranVerse(1, 2, "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ", "الثناء الكامل لجميع المحامد لله وحده رب الخلق أجمعين.", "[All] praise is [due] to Allah, Lord of the worlds -"),
        "1:3" to QuranVerse(1, 3, "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "ذي الرحمة الواسعة التي وسعت كل شيء.", "The Entirely Merciful, the Especially Merciful,"),
        "1:4" to QuranVerse(1, 4, "مَٰلِكِ يَوْمِ ٱلدِّينِ", "المالك المتصرف وحده في يوم الجزاء والحساب.", "Sovereign of the Day of Recompense."),
        "1:5" to QuranVerse(1, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "نخصك وحدك بالعبادة ونستعين بك وحدك في جميع أمورنا.", "It is You we worship and You we ask for help."),
        "1:6" to QuranVerse(1, 6, "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", "دلنا وأرشدنا وثبتنا على الطريق المستقيم.", "Guide us to the straight path -"),
        "1:7" to QuranVerse(1, 7, "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ", "طريق الذين أنعمت عليهم من النبيين والصديقين والشهداء والصالحين.", "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray."),
        "2:1" to QuranVerse(2, 1, "الٓمٓ", "هذه الحروف المقطعة في أوائل السور تنبيه على إعجاز القرآن الكريم.", "Alif, Lam, Meem."),
        "2:2" to QuranVerse(2, 2, "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ", "هذا القرآن العظيم لا شك فيه أنه من عند الله، هداية للمتقين.", "This is the Book about which there is no doubt, a guidance for those conscious of Allah -"),
        "2:3" to QuranVerse(2, 3, "ٱلَّذِينَ يُؤْمِنُونَ بِٱلْغَيْبِ وَيُقِيمُونَ ٱلصَّلَوٰةَ وَمِمَّا رَزَقْنَٰهُمْ يُنفِقُونَ", "الذين يصدقون بالغيب ويؤدون الصلاة ويبذلون مما رزقناهم.", "Who believe in the unseen, establish prayer, and spend out of what We have provided for them,"),
        "2:4" to QuranVerse(2, 4, "وَٱلَّذِينَ يُؤْمِنُونَ بِمَآ أُنزِلَ إِلَيْكَ وَمَآ أُنزِلَ مِن قَبْلِكَ وَبِٱلْءَاخِرَةِ هُمْ يُوقِنُونَ", "والذين يصدقون بما أنزل إليك وما أنزل من قبلك من الكتب، وبالآخرة هم يوقنون.", "And who believe in what has been revealed to you, [O Muhammad], and what was revealed before you, and of the Hereafter they are certain [in faith]."),
        "2:5" to QuranVerse(2, 5, "أُو۟لَٰٓئِكَ عَلَىٰ هُدًى مِّن رَّبِّهِمْ ۖ وَأُو۟لَٰٓئِكَ هُمُ ٱلْمُفْلِحُونَ", "أولئك المتصفون بهذه الصفات على نور وبيان من ربهم وهم الفائزون.", "Those are upon [right] guidance from their Lord, and it is those who are the successful."),
        "2:255" to QuranVerse(2, 255, "ٱللَّهُ لَآ إِلَٰهَ إِلَّا هُوَ ٱلْحَىُّ ٱلْقَيُّومُ ۚ لَا تَأْخُذُهُۥ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُۥ مَا فِى ٱلسَّمَٰوَٰتِ وَمَا فِى ٱلْأَرْضِ ۗ مَن ذَا ٱلَّذِى يَشْفَعُ عِندَهُۥٓ إِلَّا بِإِذْنِهِۦ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَىْءٍ مِّنْ عِلْمِهِۦٓ إِلَّا بِمَا شَآءَ ۚ وَسِعَ كُرْسِيُّهُ ٱلسَّمَٰوَٰتِ وَٱلْأَرْضَ ۖ وَلَا يَـُٔودُهُۥ حِفْظُهُمَا ۚ وَهُوَ ٱلْعَلِىُّ ٱلْعَظِيمُ", "الله الذي لا يستحق الألوهية والعبودية إلا هو، الحي القيوم الذي وسع كرسيه السماوات والأرض.", "Allah - there is no deity except Him, the Ever-Living, the Sustainer of [all] existence."),
        "9:1" to QuranVerse(9, 1, "بَرَآءَةٌ مِّنَ ٱللَّهِ وَرَسُولِهِۦٓ إِلَى ٱلَّذِينَ عَٰهَدتُّم مِّنَ ٱلْمُشْرِكِينَ", "هذه براءة من الله ورسوله إلى الذين عاهدتم من المشركين.", "[This is a declaration of] disassociation, from Allah and His Messenger, to those with whom you had made a treaty among the polytheists."),
        "9:2" to QuranVerse(9, 2, "فَسِيحُوا۟ فِى ٱلْأَرْضِ أَرْبَعَةَ أَشْهُرٍ وَٱعْلَمُوٓا۟ أَنَّكُمْ غَيْرُ مُعْجِزِى ٱللَّهِ ۙ وَأَنَّ ٱللَّهَ مُخْزِى ٱلْكَٰفِرِينَ", "فسيروا في الأرض أربعة أشهر، واعلموا أنكم غير فائتين من عذاب الله.", "So travel freely, [O disbelievers], throughout the land [during] four months but know that you cannot cause failure to Allah and that Allah will disgrace the disbelievers."),
        "27:30" to QuranVerse(27, 30, "إِنَّهُۥ مِن سُلَيْمَٰنَ وَإِنَّهُۥ بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "إنه من سليمان وإنه مفتتح ببسم الله الرحمن الرحيم.", "Indeed, it is from Solomon, and indeed, it reads: 'In the name of Allah, the Entirely Merciful, the Especially Merciful,"),
        "83:22" to QuranVerse(83, 22, "إِنَّ ٱلْأَبْرَارَ لَفِى نَعِيمٍ", "إن أهل الصدق والطاعة لفي الجنة يتنعمون.", "Indeed, the righteous will be in pleasure"),
        "83:23" to QuranVerse(83, 23, "عَلَى ٱلْأَرَآئِكِ يَنظُرُونَ", "على الأسرة المزينة ينظرون إلى ما أعد الله لهم من الكرامة.", "On adorned couches, observing."),
        "83:24" to QuranVerse(83, 24, "تَعْرِفُ فِى وُجُوهِهِمْ نَضْرَةَ ٱلنَّعِيمِ", "ترى في وجوههم بهجة النعيم وحسنه.", "You will recognize in their faces the radiance of pleasure."),
        "83:25" to QuranVerse(83, 25, "يُسْقَوْنَ مِن رَّحِيقٍ مَّخْتُومٍ", "يسقون من شراب خالص صاف مختوم.", "They will be given to drink [pure] wine [which was] sealed."),
        "83:26" to QuranVerse(83, 26, "خِتَٰمُهُۥ مِسْكٌ ۚ وَفِى ذَٰلِكَ فَلْيَتَنَافَسِ ٱلْمُتَنَٰفِسُونَ", "آخره رائحة مسك زكية، وفي هذا النعيم المقيم فليتسابق المتسابقون.", "The last of it is musk. So for this let the competitors compete."),
        "83:27" to QuranVerse(83, 27, "وَمِزَاجُهُۥ مِن تَسْنِيمٍ", "ومزاج هذا الشراب من عين في الجنة تعرف لعلوها بتسنيم.", "And its mixture is of Tasneem,"),
        "83:28" to QuranVerse(83, 28, "عَيْنًا يَشْرَبُ بِهَا ٱلْمُقَرَّبُونَ", "عينًا أعدت لشرب المقربين يتلذذون بها.", "A spring from which those near [to Allah] drink."),
        "95:1" to QuranVerse(95, 1, "وَٱلتِّينِ وَٱلزَّيْتُونِ", "أقسم الله بالتين والزيتون ومنابتهما المباركة.", "By the fig and the olive"),
        "95:2" to QuranVerse(95, 2, "وَطُورِ سِينِينَ", "وأقسم بجبل طور سيناء الذي كلم الله عليه موسى عليه السلام.", "And [by] Mount Sinai"),
        "95:3" to QuranVerse(95, 3, "وَهَٰذَا ٱلْبَلَدِ ٱلْأَمِينِ", "وأقسم بهذا البلد الأمين من كل خوف وهو مكة المكرمة.", "And [by] this secure city [Makkah],"),
        "95:4" to QuranVerse(95, 4, "لَقَدْ خَلَقْنَا ٱلْإِنسَٰنَ فِىٓ أَحْسَنِ تَقْوِيمٍ", "لقد خلقنا الإنسان في أحسن صورة وأعدل هيئة.", "We have certainly created man in the best of stature;"),
        "95:5" to QuranVerse(95, 5, "ثُمَّ رَدَدْنَٰهُ أَسْفَلَ سَٰفِلِينَ", "ثم رددناه إلى أرذل العمر أو إلى النار إن لم يطع الله.", "Then We return him to the lowest of the low,"),
        "95:6" to QuranVerse(95, 6, "إِلَّا ٱلَّذِينَ ءَامَنُوا۟ وَعَمِلُوا۟ ٱلصَّٰلِحَٰتِ فَلَهُمْ أَجْرٌ غَيْرُ مَمْنُونٍ", "إلا الذين آمنوا وعملوا الصالحات فلهم أجر عظيم دائم غير مقطوع.", "Except for those who believe and do righteous deeds, for they will have a reward uninterrupted."),
        "95:7" to QuranVerse(95, 7, "فَمَا يُكَذِّبُكَ بَعْدُ بِٱلدِّينِ", "فأي شيء يحملك أيها الإنسان على أن تكذب بيوم الجزاء والحساب بعد هذه الأدلة.", "So what yet causes you to deny the Recompense?"),
        "95:8" to QuranVerse(95, 8, "أَلَيْسَ ٱللَّهُ بِأَحْكَمِ ٱلْحَٰكِمِينَ", "أليس الله الذي جعل هذا اليوم للفصل بين الناس بأحكم الحاكمين في كل ما خلق؟ بلى.", "Is not Allah the most just of judges?"),
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
     * Uses verified Uthmani text from local cache / bundled verified verses / AlQuran.cloud Uthmani API,
     * and enforces Madinah Mushaf (Hafs 'an Asim) Basmala rules.
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
            val rawText = json.optString("uthmaniText", "")
            if (rawText.isBlank()) return null
            val sanitizedText = sanitizeVerseTextForMadinahMushaf(surahNumber, verseNumber, rawText)
            val verse = QuranVerse(
                surahNumber = surahNumber,
                verseNumber = verseNumber,
                uthmaniText = sanitizedText,
                tafsirMuyassar = json.optString("tafsirMuyassar", ""),
                translationSaheeh = json.optString("translationSaheeh", "")
            )
            // Update disk cache if a legacy cached Verse 1 file still had an unstripped Basmala prefix
            if (sanitizedText != rawText) {
                saveCachedVerse(verse)
            }
            verse
        } catch (e: Exception) {
            null
        }
    }

    private fun saveCachedVerse(verse: QuranVerse) {
        try {
            val file = File(verseCacheDir, "verse_${verse.surahNumber}_${verse.verseNumber}.json")
            val sanitized = sanitizeVerseTextForMadinahMushaf(
                verse.surahNumber,
                verse.verseNumber,
                verse.uthmaniText
            )
            val json = JSONObject().apply {
                put("surahNumber", verse.surahNumber)
                put("verseNumber", verse.verseNumber)
                put("uthmaniText", sanitized)
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
                            val rawUthmani = dataArr.getJSONObject(0).optString("text", "")
                            val uthmani = sanitizeVerseTextForMadinahMushaf(surahNumber, verseNumber, rawUthmani)
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
                            val rawUthmani = versesArr.getJSONObject(0).optString("text_uthmani", "")
                            val uthmani = sanitizeVerseTextForMadinahMushaf(surahNumber, verseNumber, rawUthmani)
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
        const val STANDARD_UTHMANI_BASMALA = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

        fun toArabicDigits(number: Int): String {
            return number.toString().map { ch ->
                if (ch in '0'..'9') ARABIC_DIGITS[ch - '0'] else ch
            }.joinToString("")
        }

        fun formatAyahSymbol(verseNumber: Int): String {
            return "﴿${toArabicDigits(verseNumber)}﴾"
        }

        /**
         * Normalizes an Arabic token to its bare consonantal skeleton (stripping Tashkeel,
         * Dagger Alef, Tatweel, and normalizing Alef variants to bare Alef 'ا') so leading
         * Basmala words can be reliably detected across all Unicode Uthmani encodings.
         */
        fun toConsonantalSkeleton(token: String): String {
            val sb = StringBuilder(token.length)
            for (ch in token) {
                val code = ch.code
                val isDiacritic = (code in 0x0610..0x061A) ||
                    (code in 0x064B..0x065F) ||
                    (code == 0x0670) ||
                    (code in 0x06D6..0x06ED) ||
                    (code in 0x08D3..0x08FF) ||
                    (code == 0x0640)
                if (!isDiacritic) {
                    val normalizedChar = when (ch) {
                        'ٱ', 'أ', 'إ', 'آ', 'ٲ', 'ٳ' -> 'ا'
                        else -> ch
                    }
                    sb.append(normalizedChar)
                }
            }
            return sb.toString()
        }

        /**
         * Checks whether 4 Arabic tokens form the Basmala ("بسم الله الرحمن الرحيم")
         * regardless of Uthmani diacritic ordering or Alef Wasla encoding.
         */
        fun isBasmalaFourTokenPrefix(tokens: List<String>): Boolean {
            if (tokens.size < 4) return false
            val w0 = toConsonantalSkeleton(tokens[0])
            val w1 = toConsonantalSkeleton(tokens[1])
            val w2 = toConsonantalSkeleton(tokens[2])
            val w3 = toConsonantalSkeleton(tokens[3])
            return (w0 == "بسم" || w0 == "باسم") &&
                w1 == "الله" &&
                (w2 == "الرحمن" || w2 == "الرحمان") &&
                w3 == "الرحيم"
        }

        /**
         * Enforces Madinah Mushaf (Hafs 'an Asim) Basmala accuracy rules (Section 03):
         * - CASE 1: Surah Al-Fatihah (1:1) -> Basmala IS Verse 1; preserved intact.
         * - CASE 2: Surah Al-Tawbah (9) -> Never contains Basmala; any leading Basmala is stripped.
         * - CASE 3: Other Surahs (2..8, 10..114), Verse 1 -> Leading Basmala is a separate Surah
         *   heading in the Madinah Mushaf, NOT part of Verse 1's text; stripped from Verse 1 so
         *   it is never displayed as an unrecited phrase.
         * - CASE 4: Surah An-Naml (27:30) -> Basmala is inside Verse 30 ("إِنَّهُۥ مِن سُلَيْمَٰنَ وَإِنَّهُۥ بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ");
         *   because verseNumber == 30 and the first 4 tokens are not Basmala, it is 100% preserved.
         */
        fun sanitizeVerseTextForMadinahMushaf(
            surahNumber: Int,
            verseNumber: Int,
            rawUthmaniText: String
        ): String {
            val trimmed = rawUthmaniText.trim()
            if (trimmed.isEmpty()) return trimmed

            // Case 1: Surah Al-Fatihah Verse 1 IS the Basmala in Madinah Mushaf (Hafs 'an Asim)
            if (surahNumber == 1 && verseNumber == 1) {
                return trimmed
            }

            // Case 4: Verses other than Verse 1 (unless Surah 9 where Basmala never belongs at start)
            if (verseNumber != 1 && surahNumber != 9) {
                return trimmed
            }

            // Case 2 & Case 3: Strip any prepended 4-word Basmala prefix from Verse 1 of Surahs 2..114 (and anywhere at start of Surah 9)
            val tokens = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (tokens.size > 4 && isBasmalaFourTokenPrefix(tokens)) {
                return tokens.drop(4).joinToString(" ").trim()
            }

            return trimmed
        }
    }
}
