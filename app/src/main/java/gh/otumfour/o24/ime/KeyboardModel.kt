package gh.otumfour.o24.ime

import gh.otumfour.o24.lang.KeyboardLanguage

enum class KeyAction {
    CHAR,
    SHIFT,
    BACKSPACE,
    ENTER,
    SPACE,
    MODE,
    EMOJI,
    LANGUAGE,
}

enum class ShiftState { OFF, ONCE, CAPS }

enum class BoardPage { LETTERS, SYMBOLS, EMOJI }

data class KeySpec(
    val label: String,
    val action: KeyAction,
    val value: String = label,
    val weight: Float = 1f,
    val repeat: Boolean = false,
    val alternate: String? = null,
    val role: Role = Role.LETTER,
) {
    enum class Role { LETTER, ACTION, ACCENT, SPACE }
}

object KeyboardModel {
    private val letterRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m"),
    )

    private val symbolRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "%", "&", "-", "_", "+", "(", ")"),
    )

    val emoji: List<String> = listOf(
        "😀", "😁", "😂", "🤣", "😊", "😍", "😘", "😎",
        "🙂", "😉", "😅", "😢", "😭", "😡", "🤔", "😴",
        "👍", "👎", "👏", "🙏", "💪", "🤝", "✌️", "👋",
        "❤️", "💛", "💚", "🖤", "✨", "🔥", "⭐", "🎉",
        "🇬🇭", "👑", "☀️", "🌙", "🌧️", "🌈", "🎶", "💧",
        "🍚", "🌽", "🍌", "🌶️", "☕", "🚗", "🏠", "📱",
    )

    fun rows(
        page: BoardPage,
        language: KeyboardLanguage,
        shift: ShiftState,
        enterLabel: String,
        spaceLabel: String = "Otumfour",
    ): List<List<KeySpec>> = when (page) {
        BoardPage.LETTERS -> letters(language, shift, enterLabel, spaceLabel)
        BoardPage.SYMBOLS -> symbols(enterLabel, spaceLabel)
        BoardPage.EMOJI -> emojiRows(enterLabel, spaceLabel)
    }

    private fun letters(language: KeyboardLanguage, shift: ShiftState, enterLabel: String, spaceLabel: String): List<List<KeySpec>> {
        val shifted = shift != ShiftState.OFF
        val rows = ArrayList<List<KeySpec>>()
        val extra = language.extraLetters(shifted)
        if (extra.isNotEmpty()) {
            rows.add(extra.map { ch ->
                KeySpec(ch, KeyAction.CHAR, ch, weight = 1f)
            })
        }
        letterRows.forEachIndexed { index, row ->
            val keys = row.map { ch ->
                val shown = if (shifted) ch.uppercase() else ch
                KeySpec(
                    label = shown,
                    action = KeyAction.CHAR,
                    value = shown,
                    alternate = language.alternate(shown),
                )
            }
            if (index == 2) {
                val shiftLabel = when (shift) {
                    ShiftState.OFF -> "⇧"
                    ShiftState.ONCE -> "⇧"
                    ShiftState.CAPS -> "⇪"
                }
                rows.add(
                    listOf(KeySpec(shiftLabel, KeyAction.SHIFT, weight = 1.4f, role = KeySpec.Role.ACCENT)) +
                        keys +
                        listOf(KeySpec("⌫", KeyAction.BACKSPACE, weight = 1.4f, repeat = true, role = KeySpec.Role.ACTION)),
                )
            } else {
                rows.add(keys)
            }
        }
        rows.add(
            listOf(
                KeySpec("123", KeyAction.MODE, weight = 1.3f, role = KeySpec.Role.ACTION),
                KeySpec("😊", KeyAction.EMOJI, weight = 1.1f, role = KeySpec.Role.ACTION),
                KeySpec(",", KeyAction.CHAR, ",", weight = 1f),
                KeySpec(spaceLabel, KeyAction.SPACE, " ", weight = 4.6f, role = KeySpec.Role.SPACE, alternate = "lang"),
                KeySpec(".", KeyAction.CHAR, ".", weight = 1f, alternate = "_"),
                KeySpec(enterLabel, KeyAction.ENTER, weight = 1.6f, role = KeySpec.Role.ACCENT),
            ),
        )
        return rows
    }

    private fun symbols(enterLabel: String, spaceLabel: String): List<List<KeySpec>> {
        val punctuation = listOf("'", "\"", ":", ";", "!", "?", "/", "₵", "…")
        return listOf(
            symbolRows[0].map { KeySpec(it, KeyAction.CHAR, it) },
            symbolRows[1].map { symbol ->
                KeySpec(
                    label = symbol,
                    action = KeyAction.CHAR,
                    value = symbol,
                    alternate = if (symbol == "-") "_" else null,
                )
            },
            listOf(KeySpec(",", KeyAction.CHAR, ",", weight = 1.2f, role = KeySpec.Role.ACTION)) +
                punctuation.map { KeySpec(it, KeyAction.CHAR, it) } +
                listOf(KeySpec("⌫", KeyAction.BACKSPACE, weight = 1.35f, repeat = true, role = KeySpec.Role.ACTION)),
            listOf(
                KeySpec("ABC", KeyAction.MODE, weight = 1.4f, role = KeySpec.Role.ACTION),
                KeySpec("😊", KeyAction.EMOJI, weight = 1.1f, role = KeySpec.Role.ACTION),
                KeySpec(spaceLabel, KeyAction.SPACE, " ", weight = 4.4f, role = KeySpec.Role.SPACE),
                KeySpec(enterLabel, KeyAction.ENTER, weight = 1.7f, role = KeySpec.Role.ACCENT),
            ),
        )
    }

    private fun emojiRows(enterLabel: String, spaceLabel: String): List<List<KeySpec>> {
        val rows = emoji.chunked(8).map { line ->
            line.map { KeySpec(it, KeyAction.CHAR, it, weight = 1f) }
        }
        return rows + listOf(
            listOf(
                KeySpec("ABC", KeyAction.MODE, weight = 1.5f, role = KeySpec.Role.ACTION),
                KeySpec("⌫", KeyAction.BACKSPACE, weight = 1.4f, repeat = true, role = KeySpec.Role.ACTION),
                KeySpec(spaceLabel, KeyAction.SPACE, " ", weight = 4f, role = KeySpec.Role.SPACE),
                KeySpec(enterLabel, KeyAction.ENTER, weight = 1.6f, role = KeySpec.Role.ACCENT),
            ),
        )
    }

    /**
     * Wide screens: qwert / yuiop, asdfg / hjkl, shift-zxc-v / bnm-delete,
     * and a space bar on each half.
     */
    fun splitRows(rows: List<List<KeySpec>>): Triple<List<List<KeySpec>>, List<List<KeySpec>>, List<List<KeySpec>>>? {
        fun rowWith(value: String) = rows.firstOrNull { row -> row.any { it.value.equals(value, ignoreCase = true) } }
        val top = rowWith("q") ?: return null
        val home = rowWith("a") ?: return null
        val lower = rowWith("z") ?: return null
        val actions = rows.firstOrNull { row -> row.any { it.action == KeyAction.SPACE } } ?: return null
        val extra = rows.filter { it !== top && it !== home && it !== lower && it !== actions }
        fun splitAt(row: List<KeySpec>, value: String): Pair<List<KeySpec>, List<KeySpec>>? {
            val index = row.indexOfFirst { it.value.equals(value, ignoreCase = true) }
            if (index <= 0) return null
            return row.subList(0, index) to row.subList(index, row.size)
        }
        val (leftTop, rightTop) = splitAt(top, "y") ?: return null
        val (leftHome, rightHome) = splitAt(home, "h") ?: return null
        val (leftLower, rightLower) = splitAt(lower, "b") ?: return null
        val spaceAt = actions.indexOfFirst { it.action == KeyAction.SPACE }
        if (spaceAt < 0) return null
        val space = actions[spaceAt]
        val leftActions = actions.subList(0, spaceAt) + space
        val rightActions = listOf(space) + actions.subList(spaceAt + 1, actions.size)
        val quote = KeySpec("'", KeyAction.CHAR, "'", weight = 1f, role = KeySpec.Role.ACTION)
        return Triple(
            extra,
            listOf(leftTop, leftHome, leftLower, leftActions),
            listOf(rightTop, rightHome + quote, rightLower, rightActions),
        )
    }
}
