package gh.otumfour.o24.ai

import gh.otumfour.o24.BuildConfig
import gh.otumfour.o24.data.O24Settings
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed class AiResult {
    data class Ok(val text: String) : AiResult()
    data class Unavailable(val message: String) : AiResult()
}

/**
 * Built-in rewrite and translate go to the O24 proxy. The OpenAI key stays on that server.
 * A custom OpenAI-compatible endpoint is used only when endpoint, model, and key are all set.
 */
class AiClient(private val settings: O24Settings) {
    fun rewrite(text: String, style: String): AiResult {
        val limited = limit(text) ?: return tooLong()
        return if (usesCustom(settings)) {
            chat(
                "Rewrite the user's text. Style: $style. Keep the meaning. Return only the rewritten text.",
                limited,
            )
        } else {
            proxy(JSONObject().put("task", "rewrite").put("style", style).put("text", limited))
        }
    }

    fun translate(text: String, targetName: String): AiResult {
        val limited = limit(text) ?: return tooLong()
        return if (usesCustom(settings)) {
            chat(
                "Translate the user's text into $targetName. Return only the translation. " +
                    "Use the letters that language writes with, including ɛ ɔ ŋ ƒ ʋ ɖ ɣ and Hausa ɓ ɗ ƙ ƴ when they belong.",
                limited,
            )
        } else {
            proxy(JSONObject().put("task", "translate").put("target", targetName).put("text", limited))
        }
    }

    private fun proxy(payload: JSONObject): AiResult = try {
        val connection = open(BuildConfig.O24_AI_URL)
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.O24_AI_ANON}")
        connection.setRequestProperty("apikey", BuildConfig.O24_AI_ANON)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        readResponse(connection, proxy = true)
    } catch (_: Exception) {
        AiResult.Unavailable("Could not reach O24 AI. Check your connection.")
    }

    private fun chat(instruction: String, text: String): AiResult = try {
        val payload = JSONObject()
            .put("model", settings.aiModel)
            .put("temperature", 0.2)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", instruction))
                    .put(JSONObject().put("role", "user").put("content", text)),
            )
        val connection = open(chatUrl(settings.aiEndpoint))
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer ${settings.aiKey}")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        readResponse(connection, proxy = false)
    } catch (_: Exception) {
        AiResult.Unavailable("Could not reach your AI endpoint.")
    }

    private fun readResponse(connection: HttpURLConnection, proxy: Boolean): AiResult {
        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }
            .orEmpty()
        if (code !in 200..299) {
            val detail = jsonStringField(body, "error").orEmpty()
            val message = if (detail.isNotBlank()) detail else "O24 AI could not do that ($code)."
            return AiResult.Unavailable(message)
        }
        val content = if (proxy) jsonStringField(body, "text") else extractContent(body)
        return if (content.isNullOrBlank()) {
            AiResult.Unavailable("O24 AI returned an empty reply.")
        } else {
            AiResult.Ok(stripWrappingQuotes(content.trim()))
        }
    }

    companion object {
        const val MAX_CHARS = 2000

        fun usesCustom(settings: O24Settings): Boolean =
            settings.aiEndpoint.isNotBlank() && settings.aiModel.isNotBlank() && settings.aiKey.isNotBlank()

        fun limit(text: String): String? = text.takeIf { it.length <= MAX_CHARS }

        fun chatUrl(endpoint: String): String {
            val trimmed = endpoint.trim().trimEnd('/')
            return if (trimmed.endsWith("/chat/completions")) trimmed else "$trimmed/chat/completions"
        }

        fun extractContent(body: String): String? = try {
            JSONObject(body)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (_: Exception) {
            null
        }

        /** Reads a JSON string field without depending on Android's JSON stubs in unit tests. */
        fun jsonStringField(json: String, field: String): String? {
            val key = "\"$field\""
            val keyAt = json.indexOf(key)
            if (keyAt < 0) return null
            var index = json.indexOf(':', keyAt + key.length)
            if (index < 0) return null
            index++
            while (index < json.length && json[index].isWhitespace()) index++
            if (index >= json.length || json[index] != '"') return null
            index++
            val out = StringBuilder()
            while (index < json.length) {
                val char = json[index]
                if (char == '\\' && index + 1 < json.length) {
                    out.append(
                        when (json[index + 1]) {
                            'n' -> '\n'
                            't' -> '\t'
                            else -> json[index + 1]
                        },
                    )
                    index += 2
                } else if (char == '"') {
                    return out.toString()
                } else {
                    out.append(char)
                    index++
                }
            }
            return null
        }

        fun stripWrappingQuotes(text: String): String {
            val trimmed = text.trim()
            if (trimmed.length >= 2 &&
                ((trimmed.first() == '"' && trimmed.last() == '"') || (trimmed.first() == '“' && trimmed.last() == '”'))
            ) {
                return trimmed.substring(1, trimmed.length - 1).trim()
            }
            return trimmed
        }

        private fun tooLong() = AiResult.Unavailable("Shorten that to $MAX_CHARS characters or fewer.")

        private fun open(url: String): HttpURLConnection = URL(url).openConnection() as HttpURLConnection
    }
}
