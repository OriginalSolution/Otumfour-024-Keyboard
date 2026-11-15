package gh.otumfour.o24.data

import android.content.Context
import java.io.File

/** Words the user has accepted, stored on this phone, one file per language. */
class LearnedWords(context: Context) {
    private val dir = File(context.filesDir, "learned").apply { mkdirs() }
    private val lock = Any()

    fun load(language: String): Map<String, Int> = synchronized(lock) {
        val file = file(language)
        if (!file.isFile) return emptyMap()
        val map = HashMap<String, Int>()
        file.readLines().forEach { line ->
            val parts = line.split('\t')
            val word = parts.firstOrNull()?.trim()?.lowercase().orEmpty()
            if (word.isEmpty()) return@forEach
            map[word] = parts.getOrNull(1)?.toIntOrNull() ?: 1
        }
        map
    }

    fun learn(language: String, raw: String) = synchronized(lock) {
        val word = raw.trim().lowercase()
        if (word.length < 2 || word.any { it.isWhitespace() }) return
        val current = load(language).toMutableMap()
        current[word] = (current[word] ?: 0) + 1
        write(language, current)
    }

    fun clear() = synchronized(lock) {
        dir.listFiles()?.forEach { it.delete() }
    }

    fun forget(language: String, word: String) = synchronized(lock) {
        val current = load(language).toMutableMap()
        current.remove(word.trim().lowercase())
        if (current.isEmpty()) file(language).delete() else write(language, current)
    }

    fun entries(): List<LearnedEntry> = synchronized(lock) {
        val files = dir.listFiles()?.filter { it.isFile } ?: return emptyList()
        files.flatMap { file ->
            val language = file.nameWithoutExtension
            load(language).map { (word, count) -> LearnedEntry(language, word, count) }
        }.sortedWith(compareBy({ it.language }, { it.word }))
    }

    private fun write(language: String, words: Map<String, Int>) {
        val file = file(language)
        file.parentFile?.mkdirs()
        file.writeText(words.entries.joinToString("\n") { (w, f) -> "$w\t$f" })
    }

    private fun file(language: String) = File(dir, language.replace(Regex("[^a-z0-9-]"), "_") + ".txt")
}

data class LearnedEntry(val language: String, val word: String, val count: Int)
