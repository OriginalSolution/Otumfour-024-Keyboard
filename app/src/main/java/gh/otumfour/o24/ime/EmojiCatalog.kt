package gh.otumfour.o24.ime

/** A curated emoji set. Skin-tone variants are built from the base character. No Emoji Kitchen. */
object EmojiCatalog {
    val tones: List<String> = listOf("\uD83C\uDFFB", "\uD83C\uDFFC", "\uD83C\uDFFD", "\uD83C\uDFFE", "\uD83C\uDFFF")

    data class Group(val name: String, val emoji: List<String>, val toneable: Boolean = false)

    val groups: List<Group> = listOf(
        Group("Smileys", listOf("😀", "😁", "😂", "🤣", "😊", "😍", "😘", "😎", "🙂", "😉", "😅", "😢", "😭", "😡", "🤔", "😴", "🤗", "😇")),
        Group("People", listOf("👋", "👍", "👎", "👏", "🙏", "💪", "🤝", "✌️", "👌", "✊", "✋", "🫶"), toneable = true),
        Group("Animals", listOf("🐶", "🐱", "🦁", "🐘", "🐒", "🐦", "🐟", "🦋", "🐝", "🐢", "🦉", "🐊")),
        Group("Food", listOf("🍚", "🌽", "🍌", "🌶️", "☕", "🍞", "🥗", "🍲", "🍎", "🍇", "🍉", "🥜")),
        Group("Travel", listOf("🚗", "🚌", "✈️", "🏠", "🌍", "⛰️", "🌊", "🌅", "🌙", "⭐", "🌈", "🌧️")),
        Group("Activities", listOf("⚽", "🏀", "🎯", "🎉", "🎶", "🥁", "🏆", "🎮", "🎨", "💃", "🕺", "🧩")),
        Group("Objects", listOf("📱", "💡", "🔑", "🎁", "📷", "📚", "✏️", "🕰️", "💎", "👑", "🔔", "📌")),
        Group("Symbols", listOf("❤️", "💛", "💚", "🖤", "✨", "🔥", "💯", "✅", "❌", "♻️", "⚜️", "🔴")),
        Group("Flags", listOf("🇬🇭", "🇳🇬", "🇰🇪", "🇿🇦", "🇬🇧", "🇺🇸", "🇫🇷", "🇩🇪", "🇨🇦", "🇯🇵", "🇧🇷", "🇮🇳")),
    )

    fun search(query: String): List<String> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return groups.first().emoji
        return groups.filter { it.name.lowercase().contains(needle) }.flatMap { it.emoji }
            .ifEmpty { groups.flatMap { it.emoji }.filter { it.contains(needle) } }
    }

    fun withTone(emoji: String, tone: String): String = emoji + tone
}

object Emoticons {
    data class Group(val name: String, val marks: List<String>)

    val groups: List<Group> = listOf(
        Group("Classic", listOf(":-)", ":-(", ";-)", ":-D", ":-P", ":-O", ":-/", ":-|", "B-)", ":-*")),
        Group("Smiling", listOf("^_^", "^_^;", "n_n", "o_o", "O_O", ">_<", "T_T", "u_u", "=)", "=D")),
        Group("Love", listOf("<3", "</3", ":-*", "^3^", "(^3^)", "(*^_^*)", "<3 <3", "xoxo")),
        Group("Hugging", listOf("(>^_^)>", "<(^.^<)", "(>^_^)> <(^.^<)", "(づ｡◕‿‿◕｡)づ", "(っ´▽`)っ")),
        Group("Flexing", listOf("(•̀ᴗ•́)و", "ᕦ(ò_óˇ)ᕤ", "(ง'̀-'́)ง", "ᕙ(⇀‸↼‶)ᕗ", "💪(•ᴗ•💪)")),
    )
}
