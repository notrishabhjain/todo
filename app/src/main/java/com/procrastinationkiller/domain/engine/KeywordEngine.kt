package com.procrastinationkiller.domain.engine

import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class KeywordMatch(
    val keyword: String,
    val category: KeywordCategory,
    val language: Language
)

enum class KeywordCategory {
    ACTION,
    URGENCY,
    TIME_INDICATOR
}

enum class Language {
    ENGLISH,
    HINDI,
    HINGLISH
}

data class KeywordAnalysis(
    val actionKeywords: List<KeywordMatch>,
    val urgencyKeywords: List<KeywordMatch>,
    val timeIndicators: List<KeywordMatch>,
    val isActionable: Boolean,
    val resolvedDueDate: Long?
)

@Singleton
class KeywordEngine @Inject constructor() {

    private val actionKeywordsEnglish = listOf(
        "do", "complete", "send", "submit", "call", "reply", "review",
        "check", "update", "schedule", "prepare", "fix", "deploy", "test",
        "follow up", "remind", "finish", "deliver", "write", "create",
        "forward", "approve", "confirm", "book", "pay", "buy", "order",
        "add", "address", "adjust", "administer", "advise", "allocate",
        "analyze", "announce", "apply", "arrange", "assemble", "assess",
        "assign", "assist", "attend", "audit", "authorize", "brief",
        "broadcast", "build", "calculate", "cancel", "capture", "categorize",
        "certify", "change", "circulate", "clarify", "clean", "close",
        "collaborate", "collect", "commit", "communicate", "compare",
        "compile", "compose", "conduct", "connect", "consolidate",
        "consult", "contact", "contribute", "control", "convert",
        "coordinate", "copy", "correct", "customize", "debug", "decide",
        "define", "delegate", "delete", "demonstrate", "describe",
        "design", "determine", "develop", "direct", "discuss", "dispatch",
        "distribute", "document", "download", "draft", "edit", "eliminate",
        "email", "enable", "enforce", "engage", "ensure", "enter",
        "escalate", "establish", "evaluate", "examine", "execute",
        "expand", "expedite", "explain", "export", "extend", "extract",
        "facilitate", "file", "fill", "finalize", "find", "flag",
        "forecast", "format", "formulate", "fulfill", "fund", "generate",
        "get", "give", "grant", "guide", "handle", "help", "hire",
        "host", "identify", "implement", "import", "improve", "include",
        "incorporate", "increase", "inform", "initiate", "input",
        "inspect", "install", "instruct", "integrate", "interpret",
        "introduce", "investigate", "invite", "issue", "join", "justify",
        "keep", "launch", "lead", "liaise", "link", "list", "load",
        "locate", "log", "maintain", "make", "manage", "map", "mark",
        "match", "measure", "meet", "merge", "migrate", "modify",
        "monitor", "motivate", "move", "negotiate", "note", "notify",
        "obtain", "offer", "open", "operate", "optimize", "organize",
        "outline", "oversee", "pack", "participate", "perform", "permit",
        "pick", "pilot", "place", "plan", "post", "practice", "present",
        "prevent", "print", "prioritize", "process", "procure", "produce",
        "program", "promote", "propose", "protect", "provide", "publish",
        "purchase", "push", "qualify", "query", "question", "quote",
        "raise", "reach", "read", "reallocate", "receive", "recommend",
        "reconcile", "record", "recruit", "redesign", "reduce", "refer",
        "refine", "register", "regulate", "reinforce", "reject", "release",
        "relocate", "remove", "rename", "renew", "repair", "replace",
        "report", "request", "require", "reschedule", "research",
        "reserve", "reset", "resolve", "respond", "restore", "restructure",
        "retrieve", "return", "revise", "route", "run", "save", "scan",
        "screen", "secure", "select", "separate", "serve", "set", "setup",
        "share", "ship", "sign", "simplify", "solve", "sort", "source",
        "specify", "sponsor", "staff", "standardize", "start", "stock",
        "stop", "store", "streamline", "strengthen", "structure", "study",
        "subscribe", "summarize", "supervise", "supply", "support",
        "survey", "suspend", "switch", "sync", "systematize", "take",
        "target", "teach", "tell", "terminate", "track", "trade", "train",
        "transfer", "transform", "translate", "transmit", "transport",
        "troubleshoot", "turn", "type", "unblock", "unify", "upgrade",
        "upload", "use", "validate", "verify", "visit", "volunteer",
        "warn", "wire", "withdraw", "work", "wrap"
    )

    private val actionKeywordsHindiHinglish = listOf(
        // Roman transliteration - base verbs and conjugations
        "karna", "kar dena", "kar do", "karo", "karein", "karenge",
        "karunga", "karungi", "kijiye", "kijiyega", "kar lena", "kar lo",
        "kar lijiye", "bhejna", "bhej dena", "bhejo", "bhejiye", "bhej do",
        "bhej dijiye", "dekhna", "dekh lena", "dekho", "dekhiye", "dekh lo",
        "dekh lijiye", "likhna", "likh dena", "likho", "likhiye", "likh do",
        "likh dijiye", "batana", "bata dena", "batao", "bataiye", "bata do",
        "bata dijiye", "banana", "bana dena", "banao", "banaiye", "bana do",
        "bana dijiye", "dena", "de dena", "de do", "dijiye", "de dijiye",
        "lena", "le lena", "le lo", "lijiye", "le lijiye", "puchna",
        "puch lena", "pucho", "puchiye", "milna", "mil lena", "milo",
        "miliye", "samjhana", "samjha dena", "samjhao", "samjhaiye",
        "suljhana", "suljha dena", "suljhao", "badalna", "badal dena",
        "badlo", "jodna", "jod dena", "jodo", "hatana", "hata dena",
        "hatao", "rakhna", "rakh dena", "rakho", "rakhiye", "dhundna",
        "dhund lena", "dhundo", "uthana", "utha lena", "uthao", "padhna",
        "padh lena", "padho", "padhiye", "sunana", "suna dena", "sunao",
        "sunaiye", "dikhana", "dikha dena", "dikhao", "dikhaiye", "chalna",
        "chal dena", "chalo", "chaliye", "rokna", "rok dena", "roko",
        "rokiye", "todna", "tod dena", "todo", "jodiye", "sambhalna",
        "sambhal lena", "sambhalo", "theek karna", "theek kar do",
        "pura karna", "pura karo", "pura kar do", "shuru karna",
        "shuru karo", "shuru kar do", "band karna", "band karo",
        "band kar do", "check karna", "check karo", "check kar lo",
        "review karna", "review karo", "update karna", "update karo",
        "fix karna", "fix karo", "setup karna", "setup karo",
        "install karna", "deploy karna", "test karna", "call karna",
        "call karo", "email karna", "email karo", "forward karna",
        "share karna", "share karo", "download karna", "upload karna",
        "submit karna", "confirm karna", "approve karna", "book karna",
        "order karna", "pay karna", "schedule karna", "plan karna",
        "arrange karna", "organize karna", "discuss karna", "prepare karna",
        "complete karna", "deliver karna", "remind karna", "follow up karna",
        "escalate karna", "transfer karna", "coordinate karna",
        "finalize karna", "cancel karna", "postpone karna",
        "reschedule karna", "verify karna", "validate karna",
        "investigate karna", "analyze karna", "document karna",
        "compile karna", "assign karna", "delegate karna",
        "implement karna", "integrate karna", "migrate karna",
        "launch karna", "publish karna", "release karna", "notify karna",
        "inform karna", "guide karna", "train karna", "process karna",
        "evaluate karna", "monitor karna", "track karna", "report karna",
        "connect karna", "resolve karna", "handle karna", "manage karna",
        "facilitate karna", "support karna", "maintain karna",
        "clean karna", "close karna", "open karna", "register karna",
        "login karna", "logout karna", "sign karna", "print karna",
        "scan karna", "copy karna", "paste karna", "format karna",
        "run karna", "execute karna", "debug karna", "optimize karna",
        "jama karna", "ikattha karna", "tayaar karna", "bharna",
        "bhar dena", "bharo", "nikalna", "nikal dena", "nikalo",
        "chhodna", "chhod dena", "chhodo", "pakadna", "pakad lena",
        "pakdo", "phenkna", "phenk dena", "phenko", "kholna", "khol dena",
        "kholo", "bandh karna", "bandh karo", "dhoondna", "dhoond lena",
        "dhoondo", "maangna", "maang lena", "maango", "yaad dilana",
        "jaldi karna", "sambhalna", "jama karna",
        // Devanagari script
        "करना", "कर देना", "कर दो", "करो", "करें", "करेंगे",
        "करूंगा", "करूंगी", "कीजिए", "कीजिएगा", "कर लेना", "कर लो",
        "कर लीजिए", "भेजना", "भेज देना", "भेजो", "भेजिए", "भेज दो",
        "भेज दीजिए", "देखना", "देख लेना", "देखो", "देखिए", "देख लो",
        "देख लीजिए", "लिखना", "लिख देना", "लिखो", "लिखिए", "लिख दो",
        "बताना", "बता देना", "बताओ", "बताइए", "बता दो", "बनाना",
        "बना देना", "बनाओ", "बनाइए", "बना दो", "देना", "दे देना",
        "दे दो", "दीजिए", "दे दीजिए", "लेना", "ले लेना", "ले लो",
        "लीजिए", "ले लीजिए", "पूछना", "पूछ लेना", "पूछो", "पूछिए",
        "मिलना", "मिल लेना", "मिलो", "मिलिए", "समझाना", "समझा देना",
        "समझाओ", "समझाइए", "सुलझाना", "सुलझा देना", "सुलझाओ",
        "बदलना", "बदल देना", "बदलो", "जोड़ना", "जोड़ देना", "जोड़ो",
        "हटाना", "हटा देना", "हटाओ", "रखना", "रख देना", "रखो",
        "रखिए", "ढूंढना", "ढूंढ लेना", "ढूंढो", "उठाना", "उठा लेना",
        "उठाओ", "पढ़ना", "पढ़ लेना", "पढ़ो", "पढ़िए", "सुनाना",
        "सुना देना", "सुनाओ", "सुनाइए", "दिखाना", "दिखा देना",
        "दिखाओ", "दिखाइए", "चलना", "चल देना", "चलो", "चलिए",
        "रोकना", "रोक देना", "रोको", "रोकिए", "तोड़ना", "तोड़ देना",
        "तोड़ो", "संभालना", "संभाल लेना", "संभालो", "ठीक करना",
        "ठीक कर दो", "पूरा करना", "पूरा करो", "पूरा कर दो",
        "शुरू करना", "शुरू करो", "शुरू कर दो", "बंद करना", "बंद करो",
        "बंद कर दो", "निकालना", "निकाल देना", "निकालो", "भरना",
        "भर देना", "भरो", "खोलना", "खोल देना", "खोलो", "छोड़ना",
        "छोड़ देना", "छोड़ो", "पकड़ना", "पकड़ लेना", "फेंकना",
        "फेंक देना", "मांगना", "मांग लेना", "मांगो", "तैयार करना",
        "जमा करना", "इकट्ठा करना"
    )

    private val urgencyKeywordsEnglish = listOf(
        "urgent", "asap", "immediately", "high priority", "today", "tonight",
        "right now", "critical", "important", "deadline", "overdue",
        "time sensitive", "rush", "emergency", "must", "required", "now",
        "right away", "top priority", "first thing", "before eod",
        "blocking", "blocker", "showstopper", "p0", "p1", "cannot wait",
        "past due", "behind schedule", "at once", "without delay",
        "expedite", "hurry", "promptly"
    )

    private val urgencyKeywordsHindiHinglish = listOf(
        "jaldi", "aaj hi", "turant", "abhi", "important", "zaruri",
        "urgent hai", "jaldi karo", "fatafat", "fauran", "bahut zaruri",
        "time par", "der mat karna", "abhi ke abhi", "ruko mat",
        "jitna jaldi ho sake", "pehle", "sabse pehle", "der na karna",
        "jald se jald",
        "अभी", "तुरंत",
        "जल्दी", "फौरन",
        "बहुत जरूरी",
        "सबसे पहले",
        "आज ही",
        "अभी के अभी",
        "देर मत करो",
        "रुको मत",
        "जितना जल्दी हो सके",
        "जल्द से जल्द",
        "पहले", "तत्काल",
        "शीघ्र", "फटाफट"
    )

    private val timeIndicatorsEnglish = mapOf(
        "tomorrow" to { cal: Calendar -> cal.add(Calendar.DAY_OF_YEAR, 1) },
        "tonight" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 21) },
        "eod" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 18) },
        "end of day" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 18) },
        "by evening" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 18) },
        "by morning" to { cal: Calendar ->
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 9)
        },
        "next week" to { cal: Calendar -> cal.add(Calendar.WEEK_OF_YEAR, 1) },
        "this afternoon" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 14) },
        "before lunch" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 12) },
        "after lunch" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 13) },
        "end of week" to { cal: Calendar ->
            resolveNextDayOfWeek(cal, Calendar.FRIDAY)
        },
        "monday" to { cal: Calendar -> resolveNextDayOfWeek(cal, Calendar.MONDAY) },
        "tuesday" to { cal: Calendar -> resolveNextDayOfWeek(cal, Calendar.TUESDAY) },
        "wednesday" to { cal: Calendar -> resolveNextDayOfWeek(cal, Calendar.WEDNESDAY) },
        "thursday" to { cal: Calendar -> resolveNextDayOfWeek(cal, Calendar.THURSDAY) },
        "friday" to { cal: Calendar -> resolveNextDayOfWeek(cal, Calendar.FRIDAY) },
        "saturday" to { cal: Calendar -> resolveNextDayOfWeek(cal, Calendar.SATURDAY) },
        "sunday" to { cal: Calendar -> resolveNextDayOfWeek(cal, Calendar.SUNDAY) }
    )

    private val timeIndicatorsHindiHinglish = mapOf(
        "kal" to { cal: Calendar -> cal.add(Calendar.DAY_OF_YEAR, 1) },
        "kal tak" to { cal: Calendar -> cal.add(Calendar.DAY_OF_YEAR, 1) },
        "shaam tak" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 18) },
        "subah" to { cal: Calendar ->
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 9)
        },
        "agle hafte" to { cal: Calendar -> cal.add(Calendar.WEEK_OF_YEAR, 1) },
        "aaj" to { cal: Calendar -> Unit },
        "raat tak" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 21) },
        "parso" to { cal: Calendar -> cal.add(Calendar.DAY_OF_YEAR, 2) },
        "do din mein" to { cal: Calendar -> cal.add(Calendar.DAY_OF_YEAR, 2) },
        "is hafte" to { cal: Calendar -> Unit },
        "dopahar tak" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 12) },
        "shaam ko" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 18) },
        "कल" to { cal: Calendar -> cal.add(Calendar.DAY_OF_YEAR, 1) },
        "आज" to { cal: Calendar -> Unit },
        "शाम तक" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 18) },
        "सुबह" to { cal: Calendar ->
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 9)
        },
        "परसों" to { cal: Calendar -> cal.add(Calendar.DAY_OF_YEAR, 2) },
        "अगले हफ्ते" to { cal: Calendar -> cal.add(Calendar.WEEK_OF_YEAR, 1) },
        "रात तक" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 21) },
        "दोपहर तक" to { cal: Calendar -> cal.set(Calendar.HOUR_OF_DAY, 12) }
    )

    fun analyze(text: String): KeywordAnalysis {
        val lowerText = text.lowercase()

        val actionMatches = findActionKeywords(lowerText)
        val urgencyMatches = findUrgencyKeywords(lowerText)
        val timeMatches = findTimeIndicators(lowerText)

        val isActionable = actionMatches.isNotEmpty()
        val resolvedDate = resolveDate(timeMatches)

        return KeywordAnalysis(
            actionKeywords = actionMatches,
            urgencyKeywords = urgencyMatches,
            timeIndicators = timeMatches,
            isActionable = isActionable,
            resolvedDueDate = resolvedDate
        )
    }

    private fun findActionKeywords(text: String): List<KeywordMatch> {
        val matches = mutableListOf<KeywordMatch>()

        for (keyword in actionKeywordsEnglish) {
            if (containsWord(text, keyword)) {
                matches.add(KeywordMatch(keyword, KeywordCategory.ACTION, Language.ENGLISH))
            }
        }

        for (keyword in actionKeywordsHindiHinglish) {
            if (containsWord(text, keyword)) {
                matches.add(KeywordMatch(keyword, KeywordCategory.ACTION, Language.HINGLISH))
            }
        }

        return matches
    }

    private fun findUrgencyKeywords(text: String): List<KeywordMatch> {
        val matches = mutableListOf<KeywordMatch>()

        for (keyword in urgencyKeywordsEnglish) {
            if (containsWord(text, keyword)) {
                matches.add(KeywordMatch(keyword, KeywordCategory.URGENCY, Language.ENGLISH))
            }
        }

        for (keyword in urgencyKeywordsHindiHinglish) {
            if (containsWord(text, keyword)) {
                matches.add(KeywordMatch(keyword, KeywordCategory.URGENCY, Language.HINGLISH))
            }
        }

        return matches
    }

    private fun findTimeIndicators(text: String): List<KeywordMatch> {
        val matches = mutableListOf<KeywordMatch>()

        // Sort by length descending to prefer longer matches and avoid sub-matches
        val englishSorted = timeIndicatorsEnglish.keys.sortedByDescending { it.length }
        val hindiSorted = timeIndicatorsHindiHinglish.keys.sortedByDescending { it.length }

        val matchedSpans = mutableListOf<IntRange>()

        for (keyword in englishSorted) {
            val idx = text.indexOf(keyword)
            if (idx >= 0 && !isOverlapping(idx, idx + keyword.length, matchedSpans)) {
                matches.add(KeywordMatch(keyword, KeywordCategory.TIME_INDICATOR, Language.ENGLISH))
                matchedSpans.add(idx until idx + keyword.length)
            }
        }

        for (keyword in hindiSorted) {
            val idx = text.indexOf(keyword)
            if (idx >= 0 && !isOverlapping(idx, idx + keyword.length, matchedSpans)) {
                matches.add(KeywordMatch(keyword, KeywordCategory.TIME_INDICATOR, Language.HINGLISH))
                matchedSpans.add(idx until idx + keyword.length)
            }
        }

        return matches
    }

    private fun isOverlapping(start: Int, end: Int, spans: List<IntRange>): Boolean {
        return spans.any { span -> start < span.last && end > span.first }
    }

    fun resolveDate(timeMatches: List<KeywordMatch>): Long? {
        if (timeMatches.isEmpty()) return null

        val calendar = Calendar.getInstance()

        for (match in timeMatches) {
            val englishResolver = timeIndicatorsEnglish[match.keyword]
            val hindiResolver = timeIndicatorsHindiHinglish[match.keyword]

            val resolver = englishResolver ?: hindiResolver
            resolver?.invoke(calendar)
        }

        return calendar.timeInMillis
    }

    private fun containsWord(text: String, keyword: String): Boolean {
        if (keyword.contains(" ")) {
            return text.contains(keyword)
        }
        val pattern = "(?:^|\\W)${Regex.escape(keyword)}(?:\\W|$)".toRegex()
        return pattern.containsMatchIn(text)
    }

    companion object {
        private fun resolveNextDayOfWeek(calendar: Calendar, targetDay: Int) {
            val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
            var daysToAdd = targetDay - currentDay
            if (daysToAdd <= 0) {
                daysToAdd += 7
            }
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        }
    }
}
