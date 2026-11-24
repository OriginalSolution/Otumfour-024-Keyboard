package gh.otumfour.o24.ime

data class GifHit(val title: String, val preview: String, val gif: String)

/** What the o24-gif proxy returns. A missing Tenor secret hides the GIF tab. */
sealed class GifReply {
    data class Ready(val hits: List<GifHit>) : GifReply()
    data class MissingKey(val keyName: String) : GifReply()
    data class Failed(val message: String) : GifReply()

    companion object {
        const val KEY_NAME = "TENOR_API_KEY"

        fun parse(code: Int, body: String): GifReply {
            if (code == 404) return MissingKey(KEY_NAME)
            val missing = Regex("\"missingKey\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            if (!missing.isNullOrBlank()) return MissingKey(missing)
            if (!Regex("\"ok\"\\s*:\\s*true").containsMatchIn(body)) {
                val error = Regex("\"error\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.get(1)
                return Failed(error?.takeIf { it.isNotBlank() } ?: "GIF search failed")
            }
            val hits = Regex("\\{[^{}]*\\}").findAll(body).mapNotNull { match ->
                val chunk = match.value
                val gif = Regex("\"gif\"\\s*:\\s*\"(https://[^\"]+)\"").find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
                val preview = Regex("\"preview\"\\s*:\\s*\"(https://[^\"]+)\"").find(chunk)?.groupValues?.get(1) ?: gif
                val title = Regex("\"title\"\\s*:\\s*\"([^\"]*)\"").find(chunk)?.groupValues?.get(1).orEmpty()
                GifHit(title, preview, gif)
            }.toList()
            return Ready(hits)
        }
    }
}
