package gh.otumfour.o24.data

import android.content.Context
import gh.otumfour.o24.lang.KeyboardLanguage
import gh.otumfour.o24.theme.ThemeId

data class O24Settings(
    val theme: ThemeId,
    val language: KeyboardLanguage,
    val enabledLanguages: List<KeyboardLanguage>,
    val autocorrect: Boolean,
    val autoCapitalise: Boolean,
    val haptic: Boolean,
    val keySound: Boolean,
    val aiEndpoint: String,
    val aiModel: String,
    val aiKey: String,
    val aiEnabled: Boolean,
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
            autoCapitalise = prefs.getBoolean(KEY_AUTO_CAP, true),
            haptic = prefs.getBoolean(KEY_HAPTIC, true),
            keySound = prefs.getBoolean(KEY_SOUND, false),
            aiEndpoint = prefs.getString(KEY_AI_ENDPOINT, "").orEmpty().trim(),
            aiModel = prefs.getString(KEY_AI_MODEL, "").orEmpty().trim(),
            aiKey = prefs.getString(KEY_AI_KEY, "").orEmpty().trim(),
            aiEnabled = prefs.getBoolean(KEY_AI_ENABLED, true),
        )
    }

    fun isOnboarded(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)

    /** Synchronous so the next activity sees the flag before it decides where to go. */
    fun setOnboarded(): Boolean = prefs.edit().putBoolean(KEY_ONBOARDED, true).commit()

    fun onboardingPage(): Int = prefs.getInt(KEY_ONBOARDING_PAGE, 0)

    fun setOnboardingPage(page: Int) {
        prefs.edit().putInt(KEY_ONBOARDING_PAGE, page.coerceAtLeast(0)).commit()
    }

    fun isDeveloper(): Boolean = prefs.getBoolean(KEY_DEVELOPER, false)

    fun setDeveloper(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEVELOPER, enabled).apply()
    }

    fun update(block: (O24Settings) -> O24Settings) {
        val next = block(get())
        val enabled = next.enabledLanguages.ifEmpty { listOf(KeyboardLanguage.ENGLISH) }
        prefs.edit()
            .putString(KEY_THEME, next.theme.storage)
            .putString(KEY_LANGUAGE, next.language.code)
            .putString(KEY_ENABLED, enabled.joinToString(",") { it.code })
            .putBoolean(KEY_AUTOCORRECT, next.autocorrect)
            .putBoolean(KEY_AUTO_CAP, next.autoCapitalise)
            .putBoolean(KEY_HAPTIC, next.haptic)
            .putBoolean(KEY_SOUND, next.keySound)
            .putString(KEY_AI_ENDPOINT, next.aiEndpoint)
            .putString(KEY_AI_MODEL, next.aiModel)
            .putString(KEY_AI_KEY, next.aiKey)
            .putBoolean(KEY_AI_ENABLED, next.aiEnabled)
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
        const val KEY_AUTO_CAP = "auto_cap"
        const val KEY_HAPTIC = "haptic"
        const val KEY_SOUND = "key_sound"
        const val KEY_AI_ENDPOINT = "ai_endpoint"
        const val KEY_AI_MODEL = "ai_model"
        const val KEY_AI_KEY = "ai_key"
        const val KEY_AI_ENABLED = "ai_enabled"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_ONBOARDING_PAGE = "onboarding_page"
        const val KEY_DEVELOPER = "developer"
    }
}
