package com.bheeshamks.freeflow.ime.prediction

import java.util.Locale

/**
 * Intelligent, lightweight on-device word prediction and prefix completion engine.
 * Fast, zero-allocation search designed for sub-millisecond IME latency.
 */
class WordSuggestionEngine {

    private val userLearnedWords = LinkedHashMap<String, Int>(200, 0.75f, true)

    private val starterWords = listOf("I", "The", "Thanks", "How", "What", "Hello", "Yes", "Please")

    private val nextWordMap = mapOf(
        "how" to listOf("are", "is", "about", "do", "can"),
        "what" to listOf("is", "are", "do", "time", "about"),
        "where" to listOf("are", "is", "can", "do", "were"),
        "who" to listOf("is", "are", "was", "will"),
        "why" to listOf("is", "did", "are", "not"),
        "when" to listOf("will", "can", "is", "are", "did"),
        "i" to listOf("am", "have", "will", "was", "think", "need", "can", "want", "feel", "love"),
        "you" to listOf("are", "can", "have", "will", "know", "want", "need", "think"),
        "we" to listOf("are", "can", "will", "have", "need", "should"),
        "they" to listOf("are", "were", "will", "have"),
        "he" to listOf("is", "was", "will", "has"),
        "she" to listOf("is", "was", "will", "has"),
        "it" to listOf("is", "was", "will", "works", "looks"),
        "thank" to listOf("you", "so", "very"),
        "thanks" to listOf("for", "a", "again", "so"),
        "good" to listOf("morning", "afternoon", "evening", "night", "job", "luck", "idea", "day"),
        "let" to listOf("me", "us", "it", "know"),
        "please" to listOf("let", "find", "check", "see", "help", "send"),
        "can" to listOf("you", "we", "i", "be", "do"),
        "could" to listOf("you", "we", "i", "be"),
        "would" to listOf("you", "like", "be", "have"),
        "have" to listOf("a", "to", "been", "done", "the"),
        "in" to listOf("the", "my", "our", "this", "a"),
        "on" to listOf("the", "my", "time", "board", "it"),
        "at" to listOf("the", "work", "home", "all", "least"),
        "to" to listOf("the", "be", "do", "see", "get", "go", "make"),
        "for" to listOf("the", "your", "my", "all", "this", "you"),
        "with" to listOf("you", "the", "me", "us", "that")
    )

    private val commonDictionary = listOf(
        // High frequency conversational English words
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "great", "here", "need", "should", "free", "flow", "keyboard", "text", "message",
        "test", "today", "tomorrow", "tonight", "morning", "night", "call", "send", "receive",
        "done", "doing", "ready", "happy", "sorry", "thanks", "thank", "welcome", "please",
        "help", "hello", "hi", "hey", "fine", "cool", "super", "awesome", "perfect", "okay",
        "sure", "yes", "yeah", "yep", "nope", "maybe", "always", "never", "sometimes",
        "again", "before", "right", "left", "down", "under", "while", "last", "long", "short",
        "small", "big", "large", "high", "low", "fast", "slow", "hard", "easy", "simple",
        "app", "phone", "device", "mobile", "samsung", "android", "voice", "audio", "sound",
        "screen", "display", "setting", "settings", "theme", "color", "style", "click", "tap",
        "press", "type", "typing", "input", "write", "writing", "read", "check", "look",
        "meeting", "work", "office", "home", "link", "email", "number", "code", "image",
        "photo", "picture", "file", "folder", "data", "system", "feature", "button", "menu",
        "space", "delete", "clear", "paste", "copy", "cut", "undo", "redo", "save", "open",
        "close", "find", "search", "start", "stop", "finish", "complete", "continue", "next",
        "about", "above", "across", "action", "actually", "add", "address", "almost", "already",
        "although", "always", "among", "amount", "another", "answer", "anyone", "anything",
        "anyway", "anywhere", "appear", "apply", "area", "around", "arrive", "ask", "asking",
        "assume", "attention", "available", "avoid", "away", "baby", "bad", "ball", "bank",
        "base", "basic", "beautiful", "become", "bed", "begin", "behind", "believe", "best",
        "better", "between", "beyond", "bill", "bit", "black", "blue", "body", "book", "both",
        "boy", "break", "brief", "bring", "brother", "build", "building", "business", "busy",
        "buy", "camera", "care", "carry", "case", "catch", "cause", "center", "certain", "chair",
        "chance", "change", "character", "charge", "check", "child", "children", "choose", "city",
        "claim", "class", "clear", "clearly", "close", "cloud", "coffee", "cold", "college",
        "color", "come", "comment", "common", "company", "compare", "computer", "concern",
        "condition", "consider", "contact", "contain", "continue", "control", "conversation",
        "corner", "correct", "cost", "country", "couple", "course", "court", "cover", "create",
        "cross", "current", "currently", "cut", "dark", "date", "daughter", "day", "dead",
        "deal", "dear", "death", "decide", "decision", "deep", "degree", "describe", "design",
        "detail", "develop", "device", "difference", "different", "difficult", "dinner", "direct",
        "direction", "discuss", "disease", "doctor", "door", "doubt", "draw", "dream", "drive",
        "drop", "during", "early", "earth", "east", "easy", "eat", "economic", "economy",
        "edge", "education", "effect", "effort", "eight", "either", "election", "electric",
        "element", "else", "emerge", "end", "energy", "enjoy", "enough", "enter", "entire",
        "environment", "especially", "establish", "even", "evening", "event", "eventually",
        "ever", "every", "everybody", "everyone", "everything", "evidence", "exact", "exactly",
        "example", "executive", "exist", "expect", "experience", "expert", "explain", "eye",
        "face", "fact", "factor", "fail", "fall", "family", "far", "fast", "father", "fear",
        "federal", "feel", "feeling", "few", "field", "fight", "figure", "fill", "film",
        "final", "finally", "financial", "find", "fine", "finger", "finish", "fire", "firm",
        "first", "fish", "five", "floor", "fly", "focus", "follow", "food", "foot", "force",
        "foreign", "forget", "form", "former", "forward", "four", "free", "friend", "from",
        "front", "full", "fund", "future", "game", "garden", "gas", "general", "generation",
        "girl", "give", "glass", "goal", "good", "government", "great", "green", "ground",
        "group", "grow", "growth", "guess", "gun", "guy", "hair", "half", "hand", "hang",
        "happen", "happy", "hard", "have", "head", "health", "hear", "heart", "heat", "heavy",
        "help", "here", "herself", "high", "himself", "history", "hit", "hold", "home",
        "hope", "hospital", "hotel", "hour", "house", "huge", "human", "hundred", "husband",
        "idea", "identify", "image", "imagine", "impact", "important", "improve", "include",
        "including", "increase", "indeed", "indicate", "individual", "industry", "information",
        "inside", "instead", "institution", "interest", "interesting", "international", "interview",
        "into", "investment", "involve", "issue", "item", "itself", "join", "just", "keep",
        "kill", "kind", "kitchen", "know", "knowledge", "land", "language", "large", "last",
        "late", "later", "laugh", "law", "lawyer", "lay", "lead", "leader", "learn", "least",
        "leave", "left", "leg", "legal", "less", "let", "letter", "level", "lie", "life",
        "light", "like", "likely", "line", "list", "listen", "little", "live", "local",
        "long", "look", "lose", "loss", "love", "machine", "magazine", "main", "maintain",
        "major", "majority", "make", "manage", "management", "manager", "many", "market",
        "marriage", "material", "matter", "maybe", "mean", "measure", "media", "medical",
        "meet", "meeting", "member", "memory", "mention", "message", "method", "middle",
        "might", "military", "million", "mind", "minute", "miss", "mission", "model", "modern",
        "moment", "money", "month", "more", "morning", "most", "mother", "mouth", "move",
        "movement", "movie", "much", "music", "must", "myself", "name", "nation", "national",
        "natural", "nature", "near", "nearly", "necessarily", "necessary", "need", "network",
        "never", "news", "newspaper", "next", "nice", "night", "none", "north", "note",
        "nothing", "notice", "number", "occur", "off", "offer", "office", "officer", "official",
        "often", "once", "online", "only", "onto", "open", "operation", "opportunity", "option",
        "order", "organization", "original", "others", "outside", "page", "pain", "painting",
        "paper", "parent", "part", "participant", "particular", "particularly", "partner",
        "party", "pass", "past", "patient", "pattern", "peace", "people", "perform",
        "performance", "perhaps", "period", "person", "personal", "phone", "physical", "pick",
        "picture", "piece", "place", "plan", "plant", "play", "player", "point", "police",
        "policy", "political", "politics", "poor", "popular", "population", "position",
        "positive", "possible", "power", "practice", "prepare", "present", "president",
        "pressure", "pretty", "prevent", "price", "private", "probably", "problem", "process",
        "produce", "product", "production", "professional", "professor", "program", "project",
        "property", "protect", "prove", "provide", "public", "pull", "purpose", "push", "put",
        "quality", "question", "quickly", "quite", "race", "radio", "raise", "range", "rate",
        "rather", "reach", "read", "ready", "real", "reality", "realize", "really", "reason",
        "receive", "recent", "recently", "recognize", "record", "red", "reduce", "reflect",
        "region", "relate", "relationship", "religious", "remain", "remember", "remove",
        "report", "represent", "require", "research", "resource", "respond", "response",
        "responsibility", "rest", "result", "return", "reveal", "rich", "right", "rise",
        "risk", "road", "rock", "role", "room", "rule", "safe", "same", "save", "scene",
        "school", "science", "scientist", "score", "season", "seat", "second", "section",
        "security", "seek", "seem", "sell", "send", "senior", "sense", "series", "serious",
        "serve", "service", "set", "seven", "several", "shake", "share", "shoot", "short",
        "shot", "should", "shoulder", "show", "side", "sign", "significant", "similar",
        "simple", "simply", "since", "sing", "single", "sister", "site", "situation", "six",
        "size", "skill", "skin", "small", "smile", "social", "society", "soldier", "some",
        "somebody", "someone", "something", "sometimes", "song", "soon", "sort", "sound",
        "source", "south", "southern", "space", "speak", "special", "specific", "speech",
        "spend", "sport", "spring", "staff", "stage", "stand", "standard", "star", "start",
        "state", "statement", "station", "stay", "step", "still", "stock", "stop", "store",
        "story", "strategy", "street", "strong", "structure", "student", "study", "stuff",
        "style", "subject", "success", "successful", "such", "suddenly", "suffer", "suggest",
        "suggestion", "summer", "support", "sure", "surface", "system", "table", "take",
        "talk", "task", "tax", "teach", "teacher", "team", "technology", "television", "tell",
        "ten", "tend", "term", "test", "than", "thank", "thanks", "that", "their", "them",
        "themselves", "then", "theory", "there", "these", "they", "thing", "think", "third",
        "this", "those", "though", "thought", "thousand", "threat", "three", "through",
        "throughout", "throw", "thus", "time", "today", "together", "tonight", "too", "tool",
        "top", "total", "tough", "toward", "town", "track", "trade", "traditional", "training",
        "travel", "treat", "treatment", "tree", "trial", "trip", "trouble", "true", "truth",
        "try", "turn", "type", "under", "understand", "unit", "until", "upon", "usually",
        "value", "various", "very", "victim", "view", "violence", "visit", "voice", "vote",
        "wait", "walk", "wall", "want", "watch", "water", "weapon", "wear", "week", "weight",
        "well", "west", "western", "whatever", "whether", "which", "while", "white", "whole",
        "whom", "whose", "wide", "wife", "will", "wind", "window", "wish", "with", "within",
        "without", "woman", "wonder", "word", "work", "worker", "world", "worry", "would",
        "write", "writer", "wrong", "yard", "yeah", "year", "yesterday", "yet", "young", "yourself"
    )

    private val commonDictionarySet = commonDictionary.toHashSet()

    /**
     * Extracts the active alphanumeric word segment immediately preceding the cursor.
     */
    fun extractCurrentPrefix(textBeforeCursor: String): String {
        if (textBeforeCursor.isEmpty()) return ""
        val lastChar = textBeforeCursor.last()
        if (lastChar.isWhitespace() || lastChar in ".,!?;:()[]{}<>'\"/") {
            return ""
        }
        var i = textBeforeCursor.length - 1
        while (i >= 0 && (textBeforeCursor[i].isLetterOrDigit() || textBeforeCursor[i] == '\'')) {
            i--
        }
        return textBeforeCursor.substring(i + 1)
    }

    /**
     * Extracts the last completed word before the current word or cursor.
     */
    private fun extractPreviousWord(textBeforeCursor: String, currentPrefix: String): String {
        val trimmed = textBeforeCursor.removeSuffix(currentPrefix).trimEnd()
        if (trimmed.isEmpty()) return ""
        var i = trimmed.length - 1
        while (i >= 0 && (trimmed[i].isLetterOrDigit() || trimmed[i] == '\'')) {
            i--
        }
        return trimmed.substring(i + 1).lowercase(Locale.ROOT)
    }

    /**
     * Returns top contextual suggestions based on current prefix and surrounding context.
     */
    fun getSuggestions(textBeforeCursor: String, maxCount: Int = 3): List<String> {
        val prefix = extractCurrentPrefix(textBeforeCursor)

        if (prefix.isEmpty()) {
            val previousWord = extractPreviousWord(textBeforeCursor, prefix)
            val contextual = nextWordMap[previousWord]
            if (!contextual.isNullOrEmpty()) {
                return contextual.take(maxCount)
            }
            return starterWords.take(maxCount)
        }

        val lowerPrefix = prefix.lowercase(Locale.ROOT)
        val isAllUpper = prefix.length > 1 && prefix.all { it.isUpperCase() }
        val isCapitalized = prefix.first().isUpperCase()

        val candidates = LinkedHashSet<String>()

        // 1. Learned words matching prefix (highest priority)
        for ((word, _) in userLearnedWords) {
            if (word.startsWith(lowerPrefix)) {
                candidates.add(word)
                if (candidates.size >= maxCount + 2) break
            }
        }

        // 2. Exact match in dictionary if prefix itself is a valid word
        if (commonDictionarySet.contains(lowerPrefix)) {
            candidates.add(lowerPrefix)
        }

        // 3. Search common dictionary for prefix matches
        for (word in commonDictionary) {
            if (word.startsWith(lowerPrefix)) {
                candidates.add(word)
                if (candidates.size >= maxCount + 2) break
            }
        }

        // 4. Fallback if no completions found: verbatim prefix
        if (candidates.isEmpty()) {
            candidates.add(lowerPrefix)
        }

        val candidateList = candidates.toList()
        val finalSuggestions = mutableListOf<String>()

        if (candidateList.size == 1) {
            finalSuggestions.add(candidateList[0])
        } else if (candidateList.size == 2) {
            finalSuggestions.add(candidateList[0])
            finalSuggestions.add(candidateList[1])
        } else {
            val top = candidateList[0]
            val second = candidateList[1]
            val third = candidateList[2]

            if (top == lowerPrefix) {
                finalSuggestions.add(second)
                finalSuggestions.add(top)
                finalSuggestions.add(third)
            } else {
                finalSuggestions.add(second)
                finalSuggestions.add(top)
                finalSuggestions.add(third)
            }
        }

        // Apply casing matching the typed prefix
        return finalSuggestions.take(maxCount).map { candidate ->
            when {
                isAllUpper -> candidate.uppercase(Locale.ROOT)
                isCapitalized -> candidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                else -> candidate.lowercase(Locale.ROOT)
            }
        }
    }

    fun learnWord(word: String) {
        val clean = word.trim().lowercase(Locale.ROOT)
        if (clean.length > 1 && clean.all { it.isLetter() }) {
            userLearnedWords[clean] = (userLearnedWords[clean] ?: 0) + 1
        }
    }
}
