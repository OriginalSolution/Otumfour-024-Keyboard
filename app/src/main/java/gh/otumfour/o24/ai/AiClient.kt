package gh.otumfour.o24.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed class AiResult {
    data class Ok(val text: String) : AiResult()
    data class Unavailable(val message: String) : AiResult()
}

/**
 * OpenAI-compatible chat completions. The endpoint, model, and key all come from settings.
 * Nothing is hardcoded. With no key, callers get a message instead of a request.
 */
class AiClient(
    private val endpoint: String,
    private val apiKey: String,
    private val model: String,
) {
    fun rewrite(system: String, text: String, tone: String): AiResult {
        val instruction = "Rewrite the user's text. Tone: $tone. Fix grammar. Keep the meaning and the language. Return only the rewritten text."
        return complete(system, instruction, text)
    }

    fun translate(system: String, text: String, sourceName: String, targetName: String): AiResult {
        val instruction = "Translate the user's text from $sourceName to $targetName. " +
            "Return only the translation. If it is already in the target language, return it unchanged. " +
            "Use the letters that language writes with, including ɛ ɔ ŋ ƒ ʋ ɖ ɣ and Hausa ɓ ɗ ƙ ƴ when they belong."
        return complete(system, instruction, text)
    }

    private fun complete(system: String, instruction: String, text: String): AiResult {
        if (apiKey.isBlank()) {
            return AiResult.Unavailable("Set an AI key in O24 Keyboard settings to use this.")
        }
        if (endpoint.isBlank()) {
            return AiResult.Unavailable("Add an AI endpoint in O24 Keyboard settings. An OpenAI-compatible /chat/completions URL works.")
        }
        val payload = JSONObject()
            .put("model", model.ifBlank { "gpt-4o-mini" })
            .put("temperature", 0.2)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", "$system\n$instruction"))
                    .put(JSONObject().put("role", "user").put("content", text.take(2000))),
            )
        return try {
            val connection = open(chatUrl(endpoint))
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                .orEmpty()
            if (code !in 200..299) {
                AiResult.Unavailable(errorMessage(code, body))
            } else {
                val content = extractContent(body)
                if (content.isNullOrBlank()) AiResult.Unavailable("The AI endpoint returned an empty reply.")
                else AiResult.Ok(stripWrappingQuotes(content.trim()))
            }
        } catch (error: Exception) {
            AiResult.Unavailable("Could not reach the AI endpoint. ${error.message ?: "Check the URL and try again."}")
        }
    }

    companion object {
        fun chatUrl(endpoint: String): String {
            val trimmed = endpoint.trim().trimEnd('/')
            return if (trimmed.endsWith("/chat/completions")) trimmed else "$trimmed/chat/completions"
        }

        fun extractContent(body: String): String? = try {
            val root = JSONObject(body)
            root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (_: Exception) {
            null
        }

        fun stripWrappingQuotes(text: String): String {
            val trimmed = text.trim()
            if (trimmed.length >= 2 && ((trimmed.first() == '"' && trimmed.last() == '"') || (trimmed.first() == '“' && trimmed.last() == '”'))) {
                return trimmed.substring(1, trimmed.length - 1).trim()
            }
            return trimmed
        }

        private fun errorMessage(code: Int, body: String): String {
            val detail = try {
                JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
            } catch (_: Exception) {
                ""
            }
            return if (detail.isNotBlank()) "AI request failed ($code). $detail" else "AI request failed ($code)."
        }

        private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection)
    }
}
