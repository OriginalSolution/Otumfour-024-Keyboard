package gh.otumfour.o24.suggest

/** Sentence spacing and name detection that do not need an input connection. */
object TypingEdits {
    /**
     * The text before the cursor already ends in a letter and a space.
     * A second space becomes a full stop.
     */
    fun doubleSpace(beforeCursor: String): Boolean {
        if (beforeCursor.length < 2 || !beforeCursor.endsWith(" ")) return false
        val previous = beforeCursor[beforeCursor.length - 2]
        return previous.isLetter()
    }

    /**
     * A capitalised word in the middle of a sentence is left alone.
     * A capital at the start of a sentence is not treated as a name.
     */
    fun capitalisedName(word: String, beforeWord: String): Boolean {
        if (word.length < 2 || !word[0].isUpperCase()) return false
        if (word.drop(1).any { it.isUpperCase() }) return true
        val prefix = beforeWord.trimEnd()
        if (prefix.isEmpty()) return false
        val last = prefix.last()
        return last != '.' && last != '!' && last != '?' && last != '\n'
    }
}
