package gh.otumfour.o24.serwaa

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import gh.otumfour.o24.data.O24Settings
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Speaks as Serwaa. Recorded TraffiAlert clips win when the line matches and the file is in
 * assets. Otherwise Android TTS uses TraffiAlert's rate, pitch, locale, and voice-family rule.
 * If settings contain a TTS endpoint and key, that service is tried first and must return audio.
 */
class SerwaaSpeaker(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null
    private var player: MediaPlayer? = null

    private val attributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    fun speak(text: String, settings: O24Settings) {
        val line = text.trim()
        if (line.isEmpty()) return
        main.post {
            stopPlayback()
            if (playClip(line)) return@post
            if (settings.ttsEndpoint.isNotBlank() && settings.ttsKey.isNotBlank()) {
                Thread {
                    val file = fetchCloudAudio(line, settings)
                    main.post {
                        if (file != null && playFile(file)) return@post
                        speakWithAndroid(line)
                    }
                }.start()
            } else {
                speakWithAndroid(line)
            }
        }
    }

    fun stop() {
        main.post {
            stopPlayback()
            runCatching { tts?.stop() }
        }
    }

    fun shutdown() {
        stop()
        main.post {
            runCatching { tts?.shutdown() }
            tts = null
            ready = false
        }
    }

    private fun playClip(line: String): Boolean {
        val name = SerwaaPersona.clipLines[line] ?: return false
        return try {
            context.assets.openFd("voice/serwaa/$name").use { fd ->
                stopPlayback()
                val media = MediaPlayer()
                player = media
                media.setAudioAttributes(attributes)
                media.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                media.setOnCompletionListener { stopPlayback() }
                media.prepare()
                media.start()
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun playFile(file: File): Boolean = try {
        stopPlayback()
        val media = MediaPlayer()
        player = media
        media.setAudioAttributes(attributes)
        media.setDataSource(file.absolutePath)
        media.setOnCompletionListener { stopPlayback() }
        media.prepare()
        media.start()
        true
    } catch (_: Exception) {
        false
    }

    private fun stopPlayback() {
        player?.let { media ->
            runCatching { media.stop() }
            runCatching { media.release() }
        }
        player = null
    }

    private fun speakWithAndroid(text: String) {
        val engine = tts
        if (engine == null) {
            pending = text
            tts = TextToSpeech(context.applicationContext) { status ->
                main.post {
                    if (status == TextToSpeech.SUCCESS) {
                        ready = true
                        configure()
                        pending?.let { say(it) }
                        pending = null
                    } else {
                        ready = false
                        pending = null
                    }
                }
            }
            return
        }
        if (!ready) {
            pending = text
            return
        }
        say(text)
    }

    private fun configure() {
        val engine = tts ?: return
        runCatching { engine.setAudioAttributes(attributes) }
        val choice = listOf(Locale.UK, Locale.US, Locale.ENGLISH).firstOrNull { locale ->
            runCatching { engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE }.getOrDefault(false)
        }
        if (choice != null) {
            runCatching { engine.language = choice }
            clearestVoice(engine)?.let { runCatching { engine.voice = it } }
        }
        runCatching { engine.setSpeechRate(SerwaaPersona.RATE) }
        runCatching { engine.setPitch(SerwaaPersona.PITCH) }
    }

    private fun say(text: String) {
        val engine = tts ?: return
        configure()
        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        val id = "serwaa-${System.currentTimeMillis()}"
        runCatching { engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, id) }
    }

    /**
     * Same rule as TraffiAlert AlertVoice.betterSameVoice: keep the accent and the voice
     * family, and prefer a higher-quality installed offline copy of that voice.
     */
    private fun clearestVoice(engine: TextToSpeech): Voice? {
        val current = runCatching { engine.voice }.getOrNull() ?: return null
        val voices = runCatching { engine.voices?.toList().orEmpty() }.getOrDefault(emptyList())
        val family = SerwaaPersona.voiceFamily(current.name)
        val best = voices
            .filter { voice ->
                val features = runCatching { voice.features ?: emptySet() }.getOrDefault(emptySet())
                voice.locale == current.locale &&
                    SerwaaPersona.voiceFamily(voice.name) == family &&
                    !voice.isNetworkConnectionRequired &&
                    !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }
            .maxByOrNull { it.quality }
            ?: return null
        return if (best.name != current.name && best.quality > current.quality) best else null
    }

    private fun fetchCloudAudio(text: String, settings: O24Settings): File? = try {
        val connection = (URL(settings.ttsEndpoint).openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer ${settings.ttsKey}")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        val body = JSONObject()
            .put("text", text)
            .put("input", text)
            .put("voice", "serwaa")
            .put("spoken_name", SerwaaPersona.SPOKEN_NAME)
            .put("language", SerwaaPersona.LOCALE)
            .put("rate", SerwaaPersona.RATE.toDouble())
            .put("pitch", SerwaaPersona.PITCH.toDouble())
            .put("provider_hint", "traffialert-serwaa")
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        if (connection.responseCode !in 200..299) {
            null
        } else {
            val type = connection.contentType.orEmpty()
            if (!type.startsWith("audio/") && type != "application/octet-stream") {
                null
            } else {
                val ext = if (type.contains("wav")) "wav" else "mp3"
                val file = File(context.cacheDir, "serwaa-cloud.$ext")
                connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                file.takeIf { it.length() > 0 }
            }
        }
    } catch (_: Exception) {
        null
    }

}
