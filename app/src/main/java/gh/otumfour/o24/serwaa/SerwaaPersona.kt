package gh.otumfour.o24.serwaa

import java.util.Locale

/**
 * Serwaa is the spoken voice from TraffiAlert GH, not a new character.
 *
 * Source (private app branch, not `main`):
 * OriginalSolution/traffitech-gh-alert @ cursor/traffialert-gh-v1-4c59
 * - src/i18n/voice/speak.ts names her Serwaa. The recorded intro is
 *   "Hi, I'm Sirwaah, your Traffic Alert voice."
 * - modules/.../AlertVoice.kt speaks lines the clips cannot cover with Android
 *   TextToSpeech: locale en-GB, then en-US; rate 0.95; pitch 1.05; navigation
 *   guidance audio attributes; the clearest installed offline voice of the same family.
 * - The timbre itself is a set of mp3 clips in assets/voice/serwaa/. There is no
 *   cloud voice id (no ElevenLabs / OpenAI voice id) and no TTS API key in that repo.
 * - docs/VOICE_RECORDING.md says Khaya / GhanaNLP could synthesise Ghanaian languages
 *   but the app does not call it, because that needs the publisher's own key.
 *
 * This keyboard uses those same TTS settings and the same persona. If you copy the
 * TraffiAlert clips into assets/voice/serwaa/, lines that match a clip play the recording.
 * A cloud voice can be attached later with the TTS endpoint and key in settings.
 */
object SerwaaPersona {
    const val NAME = "Serwaa"
    const val SPOKEN_NAME = "Sirwaah"
    const val INTRO = "Hi, I'm Sirwaah, your Traffic Alert voice."
    const val LOCALE = "en-GB"
    const val RATE = 0.95f
    const val PITCH = 1.05f
    const val SOURCE = "OriginalSolution/traffitech-gh-alert (branch cursor/traffialert-gh-v1-4c59)"

    fun voiceFamily(name: String): String =
        name.lowercase(Locale.ROOT).removeSuffix("-local").removeSuffix("-network").removeSuffix("-embedded")

    val clipLines: Map<String, String> = mapOf(
        INTRO to "intro.mp3",
        "Welcome back. Drive safely." to "welcome_back.mp3",
        "Well done, you're back within the speed limit." to "praise.mp3",
        "Traffic Alert is on. Obey the speed limit." to "app_on.mp3",
    )

    fun systemPrompt(userName: String): String = buildString {
        append("You are Serwaa, spoken \"Sirwaah\", the same voice people know from TraffiAlert GH. ")
        append("TraffiAlert introduces you with: \"$INTRO\" ")
        append("Speak in warm, clear British English unless the task asks for a Ghanaian language. ")
        append("You are calm, brief, and respectful. You are not the Asantehene and you do not speak for Manhyia Palace. ")
        append("On O24 Keyboard you help someone type: short replies, rewriting, and translation. ")
        if (userName.isNotBlank()) append("The person's name is $userName. ")
        append("Return only the words they should see. No quotes and no preamble.")
    }

    fun greeting(userName: String, custom: String): String {
        val template = custom.trim()
        if (template.isNotEmpty()) {
            return template.replace("{name}", userName.ifBlank { "friend" })
        }
        return if (userName.isBlank()) {
            "Hi, I'm Sirwaah. O24 Keyboard is ready."
        } else {
            "Hi, I'm Sirwaah. $userName, O24 Keyboard is ready."
        }
    }
}
