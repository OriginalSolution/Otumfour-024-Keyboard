package gh.otumfour.o24.ime

import gh.otumfour.o24.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Talks to the o24-gif proxy. The Tenor key never leaves the server. */
object GifClient {
    fun search(query: String): GifReply {
        return try {
            val connection = URL(BuildConfig.O24_GIF_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.O24_AI_ANON}")
            connection.setRequestProperty("apikey", BuildConfig.O24_AI_ANON)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            val payload = JSONObject().put("query", query).put("limit", 20).toString()
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            GifReply.parse(code, body)
        } catch (_: Exception) {
            GifReply.Failed("Could not reach Otumfour for GIFs.")
        }
    }

    fun download(url: String): ByteArray? {
        if (!url.startsWith("https://")) return null
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.inputStream.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }
}
