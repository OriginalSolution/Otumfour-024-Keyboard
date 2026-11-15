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
        listOf("@", "#", "₵", "$", "%", "&", "-", "+", "(", ")"),
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
                KeySpec(".", KeyAction.CHAR, ".", weight = 1f, alternate = ","),
                KeySpec(enterLabel, KeyAction.ENTER, weight = 1.6f, role = KeySpec.Role.ACCENT),
            ),
        )
        return rows
    }

    private fun symbols(enterLabel: String, spaceLabel: String): List<List<KeySpec>> {
        val punctuation = listOf("'", "\"", ":", ";", "!", "?", "/", "₵", "…")
        return listOf(
            symbolRows[0].map { KeySpec(it, KeyAction.CHAR, it) },
            symbolRows[1].map { KeySpec(it, KeyAction.CHAR, it) },
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
}
