package gh.otumfour.o24.data

import android.content.Context
import gh.otumfour.o24.lang.KeyboardLanguage
import gh.otumfour.o24.theme.ThemeId

data class O24Settings(
    val theme: ThemeId,
    val language: KeyboardLanguage,
    val enabledLanguages: List<KeyboardLanguage>,
    val autocorrect: Boolean,
    val userName: String,
    val greeting: String,
    val serwaaLanguage: KeyboardLanguage,
    val aiEndpoint: String,
    val aiModel: String,
    val aiKey: String,
    val ttsEndpoint: String,
    val ttsKey: String,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun get(): O24Settings {
        val enabled = decodeLanguages(prefs.getString(KEY_ENABLED, null))
        val language = KeyboardLanguage.fromCode(prefs.getString(KEY_LANGUAGE, null)).let { chosen ->
            if (chosen in enabled) chosen else enabled.first()
        }
        return O24Settings(
            theme = ThemeId.fromStorage(prefs.getString(KEY_THEME, null)),
            language = language,
            enabledLanguages = enabled,
            autocorrect = prefs.getBoolean(KEY_AUTOCORRECT, true),
            userName = prefs.getString(KEY_NAME, "").orEmpty(),
            greeting = prefs.getString(KEY_GREETING, "").orEmpty(),
            serwaaLanguage = KeyboardLanguage.fromCode(prefs.getString(KEY_SERWAA_LANG, null)),
            aiEndpoint = prefs.getString(KEY_AI_ENDPOINT, "").orEmpty().trim(),
            aiModel = prefs.getString(KEY_AI_MODEL, "").orEmpty().trim().ifBlank { "gpt-4o-mini" },
            aiKey = prefs.getString(KEY_AI_KEY, "").orEmpty().trim(),
            ttsEndpoint = prefs.getString(KEY_TTS_ENDPOINT, "").orEmpty().trim(),
            ttsKey = prefs.getString(KEY_TTS_KEY, "").orEmpty().trim(),
        )
    }

    fun isOnboarded(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded() {
        prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()
    }

    fun update(block: (O24Settings) -> O24Settings) {
        val next = block(get())
        val enabled = next.enabledLanguages.ifEmpty { listOf(KeyboardLanguage.ENGLISH) }
        prefs.edit()
            .putString(KEY_THEME, next.theme.storage)
            .putString(KEY_LANGUAGE, next.language.code)
            .putString(KEY_ENABLED, enabled.joinToString(",") { it.code })
            .putBoolean(KEY_AUTOCORRECT, next.autocorrect)
            .putString(KEY_NAME, next.userName)
            .putString(KEY_GREETING, next.greeting)
            .putString(KEY_SERWAA_LANG, next.serwaaLanguage.code)
            .putString(KEY_AI_ENDPOINT, next.aiEndpoint)
            .putString(KEY_AI_MODEL, next.aiModel)
            .putString(KEY_AI_KEY, next.aiKey)
            .putString(KEY_TTS_ENDPOINT, next.ttsEndpoint)
            .putString(KEY_TTS_KEY, next.ttsKey)
            .apply()
    }

    private fun decodeLanguages(raw: String?): List<KeyboardLanguage> {
        if (raw.isNullOrBlank()) return KeyboardLanguage.entries.toList()
        val parsed = raw.split(",").mapNotNull { code ->
            KeyboardLanguage.entries.firstOrNull { it.code == code.trim() }
        }.distinct()
        return parsed.ifEmpty { listOf(KeyboardLanguage.ENGLISH) }
    }

    companion object {
        const val NAME = "o24_settings"
        const val KEY_THEME = "theme"
        const val KEY_LANGUAGE = "language"
        const val KEY_ENABLED = "enabled_langs"
        const val KEY_AUTOCORRECT = "autocorrect"
        const val KEY_NAME = "user_name"
        const val KEY_GREETING = "greeting"
        const val KEY_SERWAA_LANG = "serwaa_lang"
        const val KEY_AI_ENDPOINT = "ai_endpoint"
        const val KEY_AI_MODEL = "ai_model"
        const val KEY_AI_KEY = "ai_key"
        const val KEY_TTS_ENDPOINT = "tts_endpoint"
        const val KEY_TTS_KEY = "tts_key"
        const val KEY_ONBOARDED = "onboarded"
    }
}
