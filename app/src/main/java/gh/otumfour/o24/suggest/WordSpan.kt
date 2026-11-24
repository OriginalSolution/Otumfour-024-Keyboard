package gh.otumfour.o24.suggest

/**
 * Letters, apostrophes, hyphens, underscores, @, and dots stay one token.
 * That keeps joe_k and joe_k@gmail.com from being rewritten or split.
 */
object WordSpan {
    fun isPart(ch: Char): Boolean =
        ch.isLetter() || ch == '\'' || ch == '’' || ch == '-' || ch == '_' || ch == '@' || ch == '.'

    fun ending(beforeCursor: String): String = beforeCursor.takeLastWhile { isPart(it) }

    /** Underscore, @, and dotted addresses are typed as-is. */
    fun leaveAsTyped(token: String): Boolean = token.any { it == '_' || it == '@' || it == '.' }

    /** A symbol key inserts that symbol and nothing around it. */
    fun insertion(symbol: String): String = symbol

    /** A dot inside an email or a name with an underscore does not start a new sentence. */
    fun capitaliseAfter(mark: Char, tokenBefore: String): Boolean =
        mark in ".!?" && !leaveAsTyped(tokenBefore)
}
