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
        "forward", "approve", "confirm", "book", "pay", "buy", "order"
    )

    private val actionKeywordsHindiHinglish = listOf(
        "karna", "kar dena", "bhejna", "bhej dena", "dekh lena", "dekho",
        "yaad dilana", "call karna", "check karna", "review karna",
        "bana dena", "jaldi karna", "bhejo", "likho", "likh dena",
        "batana", "bata dena", "kar do", "de dena", "de do",
        "karo", "karein", "karenge", "karungi", "karunga"
    )

    private val urgencyKeywordsEnglish = listOf(
        "urgent", "asap", "immediately", "high priority", "today", "tonight",
        "right now", "critical", "important", "deadline"
    )

    private val urgencyKeywordsHindiHinglish = listOf(
        "jaldi", "aaj hi", "turant", "abhi", "important", "zaruri",
        "urgent hai", "jaldi karo", "fatafat"
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
        "do din mein" to { cal: Calendar -> cal.add(Calendar.DAY_OF_YEAR, 2) }
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
