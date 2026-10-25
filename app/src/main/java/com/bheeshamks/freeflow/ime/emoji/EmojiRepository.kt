package com.bheeshamks.freeflow.ime.emoji

/**
 * Curated repository of Unicode emojis partitioned by category and keyword search index.
 */
object EmojiRepository {

    enum class Category(val icon: String, val title: String) {
        RECENTS("🕒", "Recent"),
        SMILEYS("😊", "Smileys & Emotion"),
        ANIMALS("🐶", "Animals & Nature"),
        FOOD("🍴", "Food & Drink"),
        TRAVEL("🏠", "Travel & Places"),
        ACTIVITIES("🏀", "Activities"),
        OBJECTS("📱", "Objects"),
        SYMBOLS("⁉️", "Symbols"),
        FLAGS("🚩", "Flags")
    }

    private val recentsList = mutableListOf(
        "😊", "😂", "❤️", "🔥", "👍", "😍", "✨", "🎉",
        "🙏", "🥺", "🥰", "😭", "👏", "💯", "🤣", "🤔",
        "😎", "🙌", "💀", "👀", "🥳", "💪", "🫡", "🤩"
    )

    fun getRecents(): List<String> = recentsList.toList()

    fun addRecent(emoji: String) {
        recentsList.remove(emoji)
        recentsList.add(0, emoji)
        if (recentsList.size > 40) {
            recentsList.removeAt(recentsList.lastIndex)
        }
    }

    val smileys = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
        "🙂", "🙃", "🫠", "😉", "😊", "😇", "🥰", "😍",
        "🤩", "😘", "😗", "😚", "😙", "😋", "😛", "😜",
        "🤪", "😝", "🤑", "🤗", "🫣", "🤭", "🫢", "🫡",
        "🤫", "🫠", "🤔", "🫡", "🤐", "🤨", "😐", "😑",
        "😶", "🫥", "😏", "😒", "🙄", "😬", "😮‍💨", "🤥",
        "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕",
        "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "😵‍💫",
        "🤯", "🤠", "🥳", "🥸", "😎", "🤓", "🧐", "😕",
        "🫤", "😟", "🙁", "☹️", "😮", "😯", "😲", "😳",
        "🥺", "🥹", "😦", "😧", "😨", "😰", "😥", "😢",
        "😭", "😱", "😖", "😣", "😞", "😓", "😩", "😫",
        "🥱", "😤", "😡", "😠", "🤬", "😈", "👿", "💀",
        "☠️", "💩", "🤡", "👹", "👺", "👻", "👽", "👾"
    )

    val animals = listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
        "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵",
        "🙈", "🙉", "🙊", "🐒", "🐔", "🐧", "🐦", "🐤",
        "🐣", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗",
        "🐴", "🦄", "🐝", "🪱", "🐛", "🦋", "🐌", "🐞",
        "🐜", "🪰", "🪲", "🪳", "🦟", "🦗", "🕷️", "🕸️",
        "🦂", "🐢", "🐍", "🦎", "🦖", "🦕", "🐙", "🦑",
        "🦐", "🦞", "🦀", "🐡", "🐠", "🐟", "🐬", "🐳",
        "🐋", "🦈", "🐊", "🐅", "🐆", "🦓", "🦍", "🦧",
        "🐘", "🦛", "🦏", "🐪", "🐫", "🦒", "🦘", "🦬"
    )

    val food = listOf(
        "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇",
        "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥",
        "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️",
        "🫑", "🌽", "🥕", "🫒", "🧄", "🧅", "🥔", "🍠",
        "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳",
        "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🌭",
        "🍔", "🍟", "🍕", "🫓", "🥪", "🥙", "🧆", "🌮",
        "🌯", "🫔", "🥗", "🥘", "🫕", "🥫", "🍝", "🍜"
    )

    val travel = listOf(
        "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑",
        "🚒", "🚐", "🛻", "🚚", "🚛", "🚜", "🛴", "🚲",
        "🛵", "🏍️", "🛺", "🚨", "🚔", "🚍", "🚘", "🚖",
        "🚡", "🚠", "🚟", "🚃", "🚋", "🚞", "🚝", "🚄",
        "🚅", "🚈", "🚂", "🚆", "🚇", "🚊", "🚉", "✈️",
        "🛫", "🛬", "💺", "🛰️", "🚀", "🛸", "🚁", "🛶",
        "⛵", "🚤", "🛥️", "🛳️", "⛴️", "🚢", "⚓", "🏠",
        "🏡", "🏢", "🏬", "🏣", "🏤", "🏥", "🏦", "🏨"
    )

    val activities = listOf(
        "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉",
        "🥏", "🎱", "🪀", "🏓", "🏸", "🏒", "🏑", "🥍",
        "🏏", "🪃", "🥅", "⛳", "🪁", "🏹", "🎣", "🤿",
        "🥊", "🥋", "🎽", "🛹", "🛼", "🛷", "⛸️", "🥌",
        "🎿", "⛷️", "🏂", "🪂", "🏋️", "🤼", "🤸", "⛹️",
        "🤺", "🤾", "🏌️", "🏇", "🧘", "🏄", "🏊", "🤽",
        "🏆", "🥇", "🥈", "🥉", "🏅", "🎖️", "🎫", "🎟️"
    )

    val objects = listOf(
        "⌚", "📱", "📲", "💻", "⌨️", "🖥️", "🖨️", "🖱️",
        "💽", "💾", "💿", "📀", "📷", "📸", "📹", "🎥",
        "📽️", "🎞️", "📞", "☎️", "📟", "📠", "📺", "📻",
        "🎙️", "🎚️", "🎛️", "⏱️", "⏲️", "⏰", "🕰️", "⌛",
        "💡", "🔦", "🕯️", "🪔", "💸", "💵", "💴", "💶",
        "💷", "🪙", "💰", "💳", "💎", "⚖️", "🧰", "🔧"
    )

    val symbols = listOf(
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🤎", "🖤",
        "🤍", "💔", "❣️", "💕", "💞", "💓", "💗", "💖",
        "💘", "💝", "💟", "☮️", "✝️", "☪️", "🕉️", "☸️",
        "✡️", "🔯", "🕎", "☯️", "☦️", "🛐", "⛎", "♈",
        "💯", "💢", "💬", "💭", "💤", "💮", "♨️", "🛑",
        "⚠️", "🚸", "⛔", "🚫", "✅", "✔️", "❌", "❎"
    )

    val flags = listOf(
        "🏁", "🚩", "🎌", "🏴", "🏳️", "🏳️‍🌈", "🏳️‍⚧️", "🏴‍☠️",
        "🇺🇸", "🇬🇧", "🇨🇦", "🇦🇺", "🇩🇪", "🇫🇷", "🇮🇹", "🇪🇸",
        "🇯🇵", "🇰🇷", "🇨🇳", "🇮🇳", "🇧🇷", "🇲🇽", "🇷🇺", "🇿🇦",
        "🇸🇦", "🇦🇪", "🇹🇷", "🇪🇬", "🇳🇬", "🇵🇰", "🇧🇩", "🇮🇩"
    )

    fun getEmojisForCategory(category: Category): List<String> = when (category) {
        Category.RECENTS -> getRecents()
        Category.SMILEYS -> smileys
        Category.ANIMALS -> animals
        Category.FOOD -> food
        Category.TRAVEL -> travel
        Category.ACTIVITIES -> activities
        Category.OBJECTS -> objects
        Category.SYMBOLS -> symbols
        Category.FLAGS -> flags
    }

    private val keywordMap: Map<String, List<String>> = mapOf(
        "happy" to listOf("😊", "😃", "😀", "😁", "😆", "🤣", "😂", "🥹", "🥳", "🥰", "😍", "😸", "🌞"),
        "smile" to listOf("😊", "🙂", "😃", "😄", "😁", "😆", "🥰", "😀"),
        "love" to listOf("❤️", "💖", "💕", "😍", "🥰", "😘", "💗", "💓", "💞", "💘", "💌", "❣️", "🖤"),
        "heart" to listOf("❤️", "💖", "💕", "💗", "💓", "💞", "💘", "💔", "❣️", "🧡", "💛", "💚", "💙", "💜"),
        "sad" to listOf("😢", "😭", "🥺", "😞", "😔", "💔", "😥", "😩", "😫", "😿"),
        "cry" to listOf("😭", "😢", "🥺", "😿", "😥", "💧"),
        "laugh" to listOf("😂", "🤣", "😆", "😹", "😅", "😄"),
        "lol" to listOf("😂", "🤣", "😆", "😹"),
        "fire" to listOf("🔥", "💥", "🧨", "🥵", "🌶️"),
        "cool" to listOf("😎", "🆒", "🤙", "✨", "🔥"),
        "party" to listOf("🥳", "🎉", "🎊", "🍾", "🥂", "🎈", "🎂"),
        "thumbs" to listOf("👍", "👎", "👌", "👏", "🙌"),
        "ok" to listOf("👌", "✅", "✔️", "👍"),
        "dog" to listOf("🐶", "🐕", "🦮", "🐕‍🦺", "🐩", "🐾", "🐺"),
        "cat" to listOf("🐱", "🐈", "🐈‍⬛", "😹", "😻", "😼"),
        "money" to listOf("💰", "💵", "💸", "🤑", "💳", "🪙", "💎"),
        "food" to listOf("🍕", "🍔", "🍟", "🌭", "🥪", "🌮", "🌯", "🍜", "🍣"),
        "coffee" to listOf("☕", "🍵", "🧋", "🧃", "🥛"),
        "car" to listOf("🚗", "🏎️", "🚕", "🚙", "🚘", "🚓", "🚀"),
        "sleep" to listOf("😴", "🥱", "💤", "🛌", "😪"),
        "angry" to listOf("😡", "😠", "🤬", "👿", "😤", "💢"),
        "star" to listOf("⭐", "🌟", "✨", "💫", "🌠"),
        "sun" to listOf("☀️", "🌞", "🌤️", "🌅"),
        "flag" to listOf("🏁", "🚩", "🎌", "🏳️‍🌈", "🇺🇸", "🇬🇧", "🇨🇦", "🇮🇳")
    )

    fun searchEmojis(query: String): List<String> {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return emptyList()

        val direct = keywordMap[clean]
        if (!direct.isNullOrEmpty()) return direct

        // Partial match against keys
        val partial = keywordMap.entries
            .filter { it.key.contains(clean) || clean.contains(it.key) }
            .flatMap { it.value }
            .distinct()

        if (partial.isNotEmpty()) return partial

        // Fallback: return smileys if query looks general
        return smileys.take(16)
    }
}
