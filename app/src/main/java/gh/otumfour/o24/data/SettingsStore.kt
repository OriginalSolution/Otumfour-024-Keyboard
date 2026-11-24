package gh.otumfour.o24.data

import android.content.Context
import gh.otumfour.o24.lang.KeyboardLanguage
import gh.otumfour.o24.theme.BackgroundStrength
import gh.otumfour.o24.theme.ThemeId
import gh.otumfour.o24.theme.ThemeMigration

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
    val swipeTyping: Boolean = true,
    val keyHeight: KeyHeight = KeyHeight.NORMAL,
    val letterSize: LetterSize = LetterSize.NORMAL,
    val backgroundStrength: BackgroundStrength = BackgroundStrength.SOFT,
    val keyPreview: Boolean = true,
    val keyboardScale: Float = 1f,
    val hand: String = "center",
    val toolbar: String = "",
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun get(): O24Settings {
        val enabled = decodeLanguages(prefs.getString(KEY_ENABLED, null))
        val language = KeyboardLanguage.fromCode(prefs.getString(KEY_LANGUAGE, null)).let { chosen ->
            if (chosen in enabled) chosen else enabled.first()
        }
        val migrated = prefs.getBoolean(KEY_THEME_MIGRATED, false)
        val storedTheme = ThemeMigration.storedAfterUpdate(prefs.getString(KEY_THEME, null), migrated)
        if (!migrated || prefs.getString(KEY_THEME, null) != storedTheme) {
            prefs.edit()
                .putString(KEY_THEME, storedTheme)
                .putBoolean(KEY_THEME_MIGRATED, true)
                .apply()
        }
        return O24Settings(
            theme = ThemeId.fromStorage(storedTheme),
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
            swipeTyping = prefs.getBoolean(KEY_SWIPE, true),
            keyHeight = KeyHeight.fromStorage(prefs.getString(KEY_KEY_HEIGHT, null)),
            letterSize = LetterSize.fromStorage(prefs.getString(KEY_LETTER_SIZE, null)),
            backgroundStrength = BackgroundStrength.fromStorage(prefs.getString(KEY_BACKGROUND, null)),
            keyPreview = prefs.getBoolean(KEY_PREVIEW, true),
            keyboardScale = prefs.getFloat(KEY_SCALE, 1f),
            hand = prefs.getString(KEY_HAND, "center").orEmpty().ifBlank { "center" },
            toolbar = prefs.getString(KEY_TOOLBAR, "").orEmpty(),
        )
    }

    fun isOnboarded(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false) || prefs.getBoolean(KEY_ONBOARDED, false)

    /** Synchronous so a recreated activity sees the flag before it decides where to go. */
    fun setOnboarded(): Boolean = prefs.edit()
        .putBoolean(KEY_ONBOARDED, true)
        .putBoolean(KEY_ONBOARDING_COMPLETE, true)
        .commit()

    fun clearOnboarded(): Boolean = prefs.edit()
        .putBoolean(KEY_ONBOARDED, false)
        .putBoolean(KEY_ONBOARDING_COMPLETE, false)
        .putBoolean(KEY_CLOSE_WHEN_ACTIVE, false)
        .putInt(KEY_ONBOARDING_PAGE, 0)
        .commit()

    fun closeWhenActive(): Boolean = prefs.getBoolean(KEY_CLOSE_WHEN_ACTIVE, false)

    fun setCloseWhenActive(close: Boolean): Boolean =
        prefs.edit().putBoolean(KEY_CLOSE_WHEN_ACTIVE, close).commit()

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
            .putBoolean(KEY_SWIPE, next.swipeTyping)
            .putString(KEY_KEY_HEIGHT, next.keyHeight.storage)
            .putString(KEY_LETTER_SIZE, next.letterSize.storage)
            .putString(KEY_BACKGROUND, next.backgroundStrength.storage)
            .putBoolean(KEY_PREVIEW, next.keyPreview)
            .putFloat(KEY_SCALE, next.keyboardScale)
            .putString(KEY_HAND, next.hand)
            .putString(KEY_TOOLBAR, next.toolbar)
            .commit()
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
        const val KEY_THEME_MIGRATED = "theme_migrated_109"
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
        const val KEY_SWIPE = "swipe"
        const val KEY_KEY_HEIGHT = "key_height"
        const val KEY_LETTER_SIZE = "letter_size"
        const val KEY_BACKGROUND = "background_strength"
        const val KEY_PREVIEW = "key_preview"
        const val KEY_SCALE = "keyboard_scale"
        const val KEY_HAND = "hand"
        const val KEY_TOOLBAR = "toolbar"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_CLOSE_WHEN_ACTIVE = "close_when_active"
        const val KEY_ONBOARDING_PAGE = "onboarding_page"
        const val KEY_DEVELOPER = "developer"
    }
}
