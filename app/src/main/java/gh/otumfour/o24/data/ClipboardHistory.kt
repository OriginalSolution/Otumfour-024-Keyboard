package gh.otumfour.o24.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Clipboard snippets kept on this phone only. Pins stay at the top. */
class ClipboardHistory(context: Context) {
    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    data class Clip(val text: String, val pinned: Boolean)

    fun all(): List<Clip> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val text = item.optString("text")
            if (text.isBlank()) null else Clip(text, item.optBoolean("pinned"))
        }.sortedByDescending { it.pinned }
    }

    fun add(text: String) {
        val clean = text.trim().take(2000)
        if (clean.isEmpty()) return
        val next = listOf(Clip(clean, false)) + all().filter { it.text != clean }
        save(next.take(30))
    }

    fun pin(text: String, pinned: Boolean) {
        save(all().map { if (it.text == text) it.copy(pinned = pinned) else it })
    }

    fun remove(text: String) {
        save(all().filter { it.text != text })
    }

    private fun save(clips: List<Clip>) {
        val array = JSONArray()
        clips.forEach { clip ->
            array.put(JSONObject().put("text", clip.text).put("pinned", clip.pinned))
        }
        prefs.edit().putString(KEY, array.toString()).commit()
    }

    companion object {
        private const val NAME = "o24_clipboard"
        private const val KEY = "clips"
    }
}
