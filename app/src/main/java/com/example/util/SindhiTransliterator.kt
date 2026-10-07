package com.example.util

import com.example.data.model.BhajanItem

object SindhiTransliterator {

    // Common devotional dictionary in authentic Sindhi Arabic script (سنڌي)
    private val devotionalDictionary = mapOf(
        "om" to "اوم",
        "aum" to "اوم",
        "jai" to "جئي",
        "shri" to "شري",
        "shree" to "شري",
        "ram" to "رام",
        "rama" to "رام",
        "krishna" to "ڪرشن",
        "kishan" to "ڪشن",
        "radhe" to "راڌي",
        "radha" to "راڌا",
        "govind" to "گووند",
        "govinda" to "گووند",
        "gopal" to "گوپال",
        "hare" to "هري",
        "hanuman" to "هنومان",
        "chalisa" to "چاليسا",
        "aarti" to "آرِتي",
        "arti" to "آرِتي",
        "bhajan" to "ڀڄن",
        "bhajans" to "ڀڄن",
        "kirtan" to "ڪيرتن",
        "dhun" to "ڌن",
        "stuti" to "استتي",
        "mantra" to "منتر",
        "gayatri" to "گائتري",
        "shiv" to "شيو",
        "shiva" to "شيو",
        "bhole" to "ڀولي",
        "mahadev" to "مھاديو",
        "ganesh" to "گڻيش",
        "ganpati" to "گنپتي",
        "jhulelal" to "جهولي لال",
        "lal" to "لال",
        "sai" to "سائين",
        "baba" to "بابا",
        "guru" to "گرو",
        "nanak" to "نانڪ",
        "mata" to "ماتا",
        "devi" to "ديوي",
        "durga" to "درگا",
        "laxmi" to "لڪشمي",
        "lakshmi" to "لڪشمي",
        "saraswati" to "سرسوتي",
        "sita" to "سيتا",
        "amrit" to "امرت",
        "amritwani" to "امرت واڻي",
        "kripalu" to "ڪرپالو",
        "achyutam" to "اچيتم",
        "keshavam" to "ڪيشوم",
        "narayan" to "نارائن",
        "narayana" to "نارائن",
        "jagdish" to "جگديش",
        "sukh" to "سُک",
        "shanti" to "شانتي",
        "bhakti" to "ڀڳتي",
        "kunj" to "ڪنج",
        "bihari" to "بِهاري",
        "waheguru" to "واهه گرو",
        "is" to "اِس",
        "hai" to "آهي",
        "he" to "آهي",
        "the" to "دي",
        "and" to "۽",
        "great" to "گريٽ"
    )

    // Devanagari to Sindhi Arabic mapping
    private val devanagariToSindhi = mapOf(
        'ॐ' to "اوم",
        'क' to "ڪ", 'ख' to "ک", 'ग' to "گ", 'घ' to "گهه", 'ङ' to "ڱ",
        'च' to "چ", 'छ' to "ڇ", 'ज' to "ج", 'झ' to "جھ", 'ञ' to "ڃ",
        'ट' to "ٽ", 'ठ' to "ٺ", 'ड' to "ڊ", 'ढ' to "ڍ", 'ण' to "ڻ",
        'त' to "ت", 'थ' to "ٿ", 'द' to "د", 'ध' to "ڌ", 'न' to "ن",
        'प' to "پ", 'फ' to "ف", 'ब' to "ب", 'भ' to "ڀ", 'म' to "م",
        'य' to "ي", 'र' to "ر", 'ल' to "ل", 'व' to "و",
        'श' to "ش", 'ष' to "ش", 'स' to "س", 'ह' to "ه",
        'ा' to "ا", 'ि' to "ِ", 'ी' to "ي", 'ु' to "ُ", 'ू' to "و",
        'े' to "ي", 'ै' to "ئي", 'ो' to "و", 'ौ' to "او",
        'ं' to "ن", 'ँ' to "ن", 'ः' to "هه", '्' to "",
        'अ' to "ا", 'आ' to "آ", 'इ' to "اِ", 'ई' to "اي",
        'उ' to "اُ", 'ऊ' to "او", 'ए' to "اي", 'ऐ' to "ائي",
        'ओ' to "او", 'औ' to "ائو",
        '१' to "۱", '२' to "۲", '۳' to "۳", '४' to "۴", '५' to "۵",
        '६' to "۶", '७' to "۷", '८' to "۸", '९' to "۹", '०' to "۰"
    )

    /**
     * Resolves the title to be displayed.
     * When language is "sindhi":
     * - Returns [titleSindhi] if present in Sindhi Arabic script.
     * - Otherwise transliterates [title] to pure Sindhi Arabic text!
     */
    fun resolveTitle(bhajan: BhajanItem, language: String): String {
        if (language != "sindhi") {
            return bhajan.title
        }

        // If user provided a title in Sindhi Arabic script
        if (bhajan.titleSindhi.isNotBlank() && isSindhiArabicScript(bhajan.titleSindhi)) {
            return bhajan.titleSindhi
        }

        // If title itself is in Sindhi Arabic script
        if (isSindhiArabicScript(bhajan.title)) {
            return bhajan.title
        }

        // If title or titleSindhi is in Devanagari, convert to Sindhi Arabic script
        val source = if (isDevanagari(bhajan.titleSindhi)) bhajan.titleSindhi else bhajan.title
        if (isDevanagari(source)) {
            return fromDevanagariToSindhi(source)
        }

        // Transliterate from English / Hinglish to pure Sindhi Arabic script
        return toSindhiArabic(bhajan.title)
    }

    fun isSindhiArabicScript(text: String): Boolean {
        return text.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF' }
    }

    fun isDevanagari(text: String): Boolean {
        return text.any { it in '\u0900'..'\u097F' }
    }

    fun fromDevanagariToSindhi(input: String): String {
        val sb = StringBuilder()
        for (c in input) {
            val mapped = devanagariToSindhi[c]
            if (mapped != null) {
                sb.append(mapped)
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Converts English / Hinglish into authentic Sindhi Arabic script (سنڌي).
     */
    fun toSindhiArabic(input: String): String {
        if (input.isBlank()) return ""

        // If input contains Devanagari, convert from Devanagari
        if (isDevanagari(input)) {
            return fromDevanagariToSindhi(input)
        }

        val words = input.split(Regex("\\s+"))
        return words.joinToString(" ") { word ->
            if (word.isBlank()) return@joinToString ""

            val prefix = word.takeWhile { !it.isLetterOrDigit() }
            val suffix = word.takeLastWhile { !it.isLetterOrDigit() }
            val coreEnd = (word.length - suffix.length).coerceAtLeast(prefix.length)
            val core = if (prefix.length < word.length) word.substring(prefix.length, coreEnd) else ""

            if (core.isEmpty()) {
                return@joinToString word
            }

            if (core.all { it.isDigit() }) {
                return@joinToString prefix + core + suffix
            }

            val cleanLower = core.lowercase()
            val converted = devotionalDictionary[cleanLower] ?: transliterateWordToSindhi(cleanLower)
            prefix + converted + suffix
        }
    }

    private fun transliterateWordToSindhi(raw: String): String {
        if (raw.isEmpty()) return ""
        val s = raw.lowercase()
        val result = StringBuilder()
        var i = 0

        while (i < s.length) {
            var matched = false

            // Try 3-char consonants
            if (i + 3 <= s.length) {
                val sub3 = s.substring(i, i + 3)
                val mapped3 = multiConsonants[sub3]
                if (mapped3 != null) {
                    result.append(mapped3)
                    i += 3
                    matched = true
                }
            }

            // Try 2-char consonants or vowels
            if (!matched && i + 2 <= s.length) {
                val sub2 = s.substring(i, i + 2)
                val mappedCons2 = multiConsonants[sub2]
                if (mappedCons2 != null) {
                    result.append(mappedCons2)
                    i += 2
                    matched = true
                } else {
                    val isStart = (i == 0 || result.isEmpty())
                    val mappedVowel2 = if (isStart) initialVowels[sub2] else middleVowels[sub2]
                    if (mappedVowel2 != null) {
                        result.append(mappedVowel2)
                        i += 2
                        matched = true
                    }
                }
            }

            // Single char
            if (!matched) {
                val c = s[i].toString()
                val isStart = (i == 0 || result.isEmpty())
                if (isStart && initialVowels.containsKey(c)) {
                    result.append(initialVowels[c])
                } else if (!isStart && middleVowels.containsKey(c)) {
                    result.append(middleVowels[c])
                } else if (consonants.containsKey(c)) {
                    result.append(consonants[c])
                } else {
                    result.append(c)
                }
                i++
            }
        }

        return result.toString()
    }

    private val multiConsonants = mapOf(
        "chh" to "ڇ",
        "kh" to "ک",
        "gh" to "گهه",
        "ch" to "چ",
        "jh" to "جھ",
        "th" to "ٿ",
        "dh" to "ڌ",
        "ph" to "ف",
        "bh" to "ڀ",
        "sh" to "ش",
        "shh" to "ش",
        "gy" to "گيان",
        "tr" to "تر",
        "kr" to "ڪر",
        "pr" to "پر",
        "gr" to "گر",
        "br" to "بر",
        "dr" to "در"
    )

    private val consonants = mapOf(
        "k" to "ڪ",
        "g" to "گ",
        "c" to "ڪ",
        "j" to "ج",
        "t" to "ت",
        "d" to "د",
        "n" to "ن",
        "p" to "پ",
        "f" to "ف",
        "b" to "ب",
        "m" to "م",
        "y" to "ي",
        "r" to "ر",
        "l" to "ل",
        "v" to "و",
        "w" to "و",
        "s" to "س",
        "h" to "ه",
        "z" to "ز",
        "x" to "ڪس",
        "q" to "ق"
    )

    private val initialVowels = mapOf(
        "aa" to "آ",
        "a" to "ا",
        "ee" to "اي",
        "ea" to "اي",
        "i" to "اِ",
        "oo" to "او",
        "ou" to "او",
        "u" to "اُ",
        "e" to "اي",
        "ai" to "ائي",
        "o" to "او",
        "au" to "ائو"
    )

    private val middleVowels = mapOf(
        "aa" to "ا",
        "a" to "ا",
        "ee" to "ي",
        "ea" to "ي",
        "i" to "ي",
        "oo" to "و",
        "ou" to "و",
        "u" to "و",
        "e" to "ي",
        "ai" to "ئي",
        "o" to "و",
        "au" to "ائو"
    )
}
