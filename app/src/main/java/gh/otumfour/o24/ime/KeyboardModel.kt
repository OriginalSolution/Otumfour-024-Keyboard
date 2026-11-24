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
    SYMBOL_PAGE,
    NUMPAD,
}

enum class KeyIcon { ENTER, SEARCH, SEND, DONE, NEXT, GO }

enum class ShiftState { OFF, ONCE, CAPS }

enum class BoardPage { LETTERS, SYMBOLS, SYMBOLS_MORE, NUMPAD, EMOJI }

data class KeySpec(
    val label: String,
    val action: KeyAction,
    val value: String = label,
    val weight: Float = 1f,
    val repeat: Boolean = false,
    val alternate: String? = null,
    val role: Role = Role.LETTER,
    val hint: String? = null,
    val extras: List<String> = emptyList(),
    val round: Boolean = false,
    val icon: KeyIcon? = null,
) {
    enum class Role { LETTER, ACTION, ACCENT, SPACE }

    fun choices(): List<String> = extras.ifEmpty { listOfNotNull(alternate) }
}

object KeyboardModel {
    private val letterRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m"),
    )

    private val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val symbolSecond = listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")

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
        BoardPage.SYMBOLS_MORE -> symbolsMore(enterLabel, spaceLabel)
        BoardPage.NUMPAD -> numpad(enterLabel, spaceLabel)
        BoardPage.EMOJI -> emojiRows(enterLabel, spaceLabel)
    }

    fun actionIcon(label: String): KeyIcon = when (label) {
        "Search" -> KeyIcon.SEARCH
        "Send" -> KeyIcon.SEND
        "Done" -> KeyIcon.DONE
        "Next" -> KeyIcon.NEXT
        "Go" -> KeyIcon.GO
        else -> KeyIcon.ENTER
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
            val keys = row.mapIndexed { column, ch ->
                val shown = if (shifted) ch.uppercase() else ch
                val digit = if (index == 0) digits[column] else null
                val special = language.alternate(shown)
                val extras = listOfNotNull(digit, special).distinct()
                KeySpec(
                    label = shown,
                    action = KeyAction.CHAR,
                    value = shown,
                    hint = digit,
                    alternate = extras.firstOrNull(),
                    extras = extras,
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
        rows.add(bottomRow("?123", enterLabel, spaceLabel, includeEmoji = true))
        return rows
    }

    private fun bottomRow(modeLabel: String, enterLabel: String, spaceLabel: String, includeEmoji: Boolean): List<KeySpec> {
        val keys = ArrayList<KeySpec>()
        keys.add(KeySpec(modeLabel, KeyAction.MODE, weight = 1.35f, role = KeySpec.Role.ACTION))
        keys.add(KeySpec(",", KeyAction.CHAR, ",", weight = 1f))
        if (includeEmoji) keys.add(KeySpec("😊", KeyAction.EMOJI, weight = 1.05f, role = KeySpec.Role.ACTION))
        keys.add(KeySpec(spaceLabel, KeyAction.SPACE, " ", weight = 4.4f, role = KeySpec.Role.SPACE, alternate = "lang"))
        keys.add(KeySpec(".", KeyAction.CHAR, ".", weight = 1f, alternate = "_"))
        keys.add(actionKey(enterLabel))
        return keys
    }

    private fun actionKey(enterLabel: String) = KeySpec(
        label = enterLabel,
        action = KeyAction.ENTER,
        weight = 1.25f,
        role = KeySpec.Role.ACCENT,
        round = true,
        icon = actionIcon(enterLabel),
    )

    private fun symbols(enterLabel: String, spaceLabel: String): List<List<KeySpec>> {
        val punctuation = listOf("*", "\"", "'", ":", ";", "!", "?")
        return listOf(
            digits.map { KeySpec(it, KeyAction.CHAR, it) },
            symbolSecond.map { symbol ->
                KeySpec(symbol, KeyAction.CHAR, symbol, alternate = if (symbol == "-") "_" else null)
            },
            listOf(KeySpec("=\\<", KeyAction.SYMBOL_PAGE, weight = 1.3f, role = KeySpec.Role.ACTION)) +
                punctuation.map { KeySpec(it, KeyAction.CHAR, it) } +
                listOf(KeySpec("⌫", KeyAction.BACKSPACE, weight = 1.35f, repeat = true, role = KeySpec.Role.ACTION)),
            listOf(
                KeySpec("ABC", KeyAction.MODE, weight = 1.3f, role = KeySpec.Role.ACTION),
                KeySpec(",", KeyAction.CHAR, ",", weight = 1f),
                KeySpec("1234", KeyAction.NUMPAD, weight = 1.3f, role = KeySpec.Role.ACTION),
                KeySpec(spaceLabel, KeyAction.SPACE, " ", weight = 4f, role = KeySpec.Role.SPACE),
                KeySpec(".", KeyAction.CHAR, ".", weight = 1f, alternate = "_"),
                actionKey(enterLabel),
            ),
        )
    }

    private fun symbolsMore(enterLabel: String, spaceLabel: String): List<List<KeySpec>> {
        val first = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "§", "Δ")
        val second = listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\")
        val third = listOf("%", "©", "®", "™", "✓", "[", "]")
        return listOf(
            first.map { KeySpec(it, KeyAction.CHAR, it) },
            second.map { KeySpec(it, KeyAction.CHAR, it) },
            listOf(KeySpec("?123", KeyAction.SYMBOL_PAGE, weight = 1.35f, role = KeySpec.Role.ACTION)) +
                third.map { KeySpec(it, KeyAction.CHAR, it) } +
                listOf(KeySpec("⌫", KeyAction.BACKSPACE, weight = 1.35f, repeat = true, role = KeySpec.Role.ACTION)),
            listOf(
                KeySpec("ABC", KeyAction.MODE, weight = 1.25f, role = KeySpec.Role.ACTION),
                KeySpec("<", KeyAction.CHAR, "<", weight = 1f),
                KeySpec("1234", KeyAction.NUMPAD, weight = 1.3f, role = KeySpec.Role.ACTION),
                KeySpec(spaceLabel, KeyAction.SPACE, " ", weight = 3.6f, role = KeySpec.Role.SPACE),
                KeySpec(">", KeyAction.CHAR, ">", weight = 1f),
                actionKey(enterLabel),
            ),
        )
    }

    private fun numpad(enterLabel: String, spaceLabel: String): List<List<KeySpec>> = listOf(
        listOf("1", "2", "3", "-").map { KeySpec(it, KeyAction.CHAR, it) },
        listOf("4", "5", "6", "+").map { KeySpec(it, KeyAction.CHAR, it) },
        listOf("7", "8", "9", "*").map { KeySpec(it, KeyAction.CHAR, it) },
        listOf(
            KeySpec("?123", KeyAction.SYMBOL_PAGE, weight = 1.3f, role = KeySpec.Role.ACTION),
            KeySpec("0", KeyAction.CHAR, "0"),
            KeySpec(".", KeyAction.CHAR, ".", alternate = "_"),
            KeySpec("⌫", KeyAction.BACKSPACE, weight = 1.3f, repeat = true, role = KeySpec.Role.ACTION),
        ),
        listOf(
            KeySpec("ABC", KeyAction.MODE, weight = 1.3f, role = KeySpec.Role.ACTION),
            KeySpec(spaceLabel, KeyAction.SPACE, " ", weight = 3f, role = KeySpec.Role.SPACE),
            actionKey(enterLabel),
        ),
    )

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
