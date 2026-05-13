package com.procrastinationkiller.domain.engine.ml

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextFeatureExtractor @Inject constructor() {

    companion object {
        const val FEATURE_COUNT = 8

        private val ACTION_VERBS = setOf(
            "do", "complete", "send", "submit", "call", "reply", "review",
            "check", "update", "schedule", "prepare", "fix", "deploy", "test",
            "follow", "remind", "finish", "deliver", "write", "create",
            "forward", "approve", "confirm", "book", "pay", "buy", "order",
            "assign", "build", "cancel", "change", "close", "connect",
            "coordinate", "debug", "delegate", "delete", "design", "develop",
            "discuss", "document", "download", "draft", "edit", "email",
            "escalate", "evaluate", "execute", "export", "file", "finalize",
            "find", "generate", "handle", "help", "implement", "import",
            "improve", "inform", "install", "integrate", "investigate",
            "invite", "launch", "lead", "list", "log", "maintain", "make",
            "manage", "merge", "migrate", "modify", "monitor", "move",
            "negotiate", "notify", "open", "operate", "optimize", "organize",
            "plan", "post", "print", "prioritize", "process", "produce",
            "provide", "publish", "push", "record", "release", "remove",
            "report", "request", "research", "resolve", "respond", "restore",
            "run", "save", "scan", "secure", "select", "setup", "share",
            "ship", "sign", "solve", "start", "stop", "store", "submit",
            "summarize", "support", "sync", "track", "train", "transfer",
            "translate", "troubleshoot", "unblock", "update", "upgrade",
            "upload", "validate", "verify", "work", "wrap",
            // Hindi/Hinglish
            "karna", "bhejna", "dekho", "bhejo", "likho", "batana", "karo",
            "karein", "karenge", "banao", "dekhna", "likhna", "hatao",
            "rakho", "dhundo", "padho", "chalo", "roko", "sambhalo",
            "bata dena", "kar do", "bhej do", "de do", "kar dena",
            "check karna", "review karna", "update karna", "fix karna",
            "call karna", "email karna", "share karna", "deploy karna",
            "test karna", "setup karna", "submit karna", "complete karna",
            "करना", "भेजना", "देखना", "लिखना", "बताना", "बनाना",
            "करो", "भेजो", "देखो", "लिखो", "बताओ", "बनाओ",
            "कर दो", "भेज दो", "कीजिए", "करें"
        )

        private val URGENCY_KEYWORDS = setOf(
            "urgent", "asap", "immediately", "high priority", "critical",
            "important", "deadline", "overdue", "time sensitive", "rush",
            "emergency", "must", "required", "now", "right away",
            "top priority", "first thing", "before eod", "blocking",
            "blocker", "showstopper", "p0", "p1", "cannot wait",
            "past due", "behind schedule", "without delay", "hurry",
            "promptly", "expedite",
            // Hindi/Hinglish
            "jaldi", "turant", "abhi", "zaruri", "fatafat", "fauran",
            "bahut zaruri", "urgent hai", "jaldi karo", "abhi ke abhi",
            "sabse pehle", "jald se jald",
            "अभी", "तुरंत", "जल्दी", "फौरन", "बहुत जरूरी",
            "सबसे पहले", "फटाफट", "तत्काल", "शीघ्र"
        )

        private val TIME_REFERENCES = setOf(
            "tomorrow", "tonight", "today", "monday", "tuesday", "wednesday",
            "thursday", "friday", "saturday", "sunday", "next week", "eod",
            "end of day", "by evening", "by morning", "this afternoon",
            "before lunch", "after lunch", "end of week",
            // Hindi/Hinglish
            "kal", "kal tak", "shaam tak", "subah", "agle hafte", "aaj",
            "raat tak", "parso", "do din mein", "is hafte", "dopahar tak",
            "shaam ko",
            "कल", "आज", "शाम तक", "सुबह", "परसों", "अगले हफ्ते",
            "रात तक", "दोपहर तक"
        )
    }

    fun extract(text: String): FloatArray {
        val features = FloatArray(FEATURE_COUNT)
        val lowerText = text.lowercase()
        val words = lowerText.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val wordCount = words.size

        // Feature 0: Word count (normalized, cap at 50)
        features[0] = (wordCount.toFloat() / 50f).coerceAtMost(1.0f)

        // Feature 1: Action verb density
        features[1] = if (wordCount > 0) {
            val actionCount = words.count { word -> ACTION_VERBS.any { word.contains(it) } }
            (actionCount.toFloat() / wordCount.toFloat()).coerceAtMost(1.0f)
        } else 0f

        // Feature 2: Urgency keyword density
        features[2] = if (wordCount > 0) {
            val urgencyCount = URGENCY_KEYWORDS.count { lowerText.contains(it) }
            (urgencyCount.toFloat() / wordCount.toFloat()).coerceAtMost(1.0f)
        } else 0f

        // Feature 3: Question mark presence (1.0 if present, 0.0 otherwise)
        features[3] = if (text.contains("?")) 1.0f else 0.0f

        // Feature 4: Exclamation count (normalized, cap at 5)
        features[4] = (text.count { it == '!' }.toFloat() / 5f).coerceAtMost(1.0f)

        // Feature 5: Time reference count (normalized, cap at 3)
        features[5] = (TIME_REFERENCES.count { lowerText.contains(it) }.toFloat() / 3f).coerceAtMost(1.0f)

        // Feature 6: Average word length (normalized, cap at 10)
        features[6] = if (wordCount > 0) {
            val avgLen = words.sumOf { it.length }.toFloat() / wordCount.toFloat()
            (avgLen / 10f).coerceAtMost(1.0f)
        } else 0f

        // Feature 7: Capitalization ratio
        features[7] = if (text.isNotEmpty()) {
            val letterCount = text.count { it.isLetter() }
            if (letterCount > 0) {
                text.count { it.isUpperCase() }.toFloat() / letterCount.toFloat()
            } else 0f
        } else 0f

        return features
    }
}
