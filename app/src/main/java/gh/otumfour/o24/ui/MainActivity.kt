package gh.otumfour.o24.ui

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import gh.otumfour.o24.R
import gh.otumfour.o24.data.LearnedWords
import gh.otumfour.o24.data.SettingsStore
import gh.otumfour.o24.lang.KeyboardLanguage
import gh.otumfour.o24.serwaa.SerwaaPersona
import gh.otumfour.o24.serwaa.SerwaaSpeaker
import gh.otumfour.o24.theme.ThemeId

class MainActivity : AppCompatActivity() {
    private lateinit var store: SettingsStore
    private var speaker: SerwaaSpeaker? = null
    private lateinit var status: TextView
    private val themeViews = LinkedHashMap<ThemeId, ThemePreviewView>()

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        status.text = if (granted) "Microphone allowed for Serwaa." else "Microphone stays off until you allow it."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SettingsStore(this)
        if (!store.isOnboarded() && !intent.getBooleanExtra(EXTRA_AFTER_ONBOARDING, false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        speaker = SerwaaSpeaker(this)
        val settings = store.get()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(40))
            setBackgroundColor(Color.parseColor("#0C0A07"))
        }
        column.addView(
            BrandHeaderView(this),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(168)).apply {
                bottomMargin = dp(8)
            },
        )
        status = body("").also { column.addView(it) }

        column.addView(heading("Set up the keyboard"))
        column.addView(goldButton("1 · Enable O24 Keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        column.addView(goldButton("2 · Choose it as your input method") {
            val imm = getSystemService(InputMethodManager::class.java)
            imm.showInputMethodPicker()
        })
        column.addView(quietButton("Allow the microphone for Serwaa") {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        })

        column.addView(heading("Theme"))
        fun addThemeChoices(themes: List<ThemeId>) {
            themes.forEach { theme ->
                val name = getString(theme.titleRes)
                column.addView(label(name))
                column.addView(body(getString(theme.blurbRes)))
                val preview = ThemePreviewView(this).apply {
                    this.theme = theme
                    setOnClickListener { selectTheme(theme) }
                }
                themeViews[theme] = preview
                column.addView(
                    preview,
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, previewHeight(theme)).apply {
                        bottomMargin = dp(4)
                    },
                )
                column.addView(quietButton(getString(R.string.learn_more)) { showThemeHistory(theme) })
                column.addView(goldButton(getString(R.string.use_theme) + " · " + name) { selectTheme(theme) })
            }
        }
        addThemeChoices(ThemeId.entries.filter { !it.jersey })
        column.addView(heading(getString(R.string.jersey_heading)))
        column.addView(body(getString(R.string.jersey_note)))
        addThemeChoices(ThemeId.entries.filter { it.jersey })

        column.addView(heading("Languages"))
        column.addView(body("The pill on the keyboard cycles the languages you leave ticked. Tap a theme above, then pick the language Serwaa should listen in."))
        val languageBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val enabled = settings.enabledLanguages.toMutableSet()
        KeyboardLanguage.entries.forEach { language ->
            val box = CheckBox(this).apply {
                text = language.label
                setTextColor(Color.parseColor("#F6F1E4"))
                isChecked = language in enabled
                setOnCheckedChangeListener { _, checked ->
                    if (checked) enabled.add(language) else enabled.remove(language)
                    if (enabled.isEmpty()) {
                        enabled.add(KeyboardLanguage.ENGLISH)
                        isChecked = language == KeyboardLanguage.ENGLISH
                    }
                    val current = store.get()
                    val nextLanguage = if (current.language in enabled) current.language else enabled.first()
                    store.update { it.copy(enabledLanguages = enabled.toList(), language = nextLanguage) }
                }
            }
            languageBox.addView(box)
        }
        column.addView(languageBox)
        column.addView(label("Keyboard language now"))
        val languageValue = body(settings.language.label)
        column.addView(languageValue)
        column.addView(quietButton("Use the next language") {
            val now = store.get()
            val list = now.enabledLanguages
            val index = list.indexOf(now.language).let { if (it < 0) 0 else it }
            val next = list[(index + 1) % list.size]
            store.update { it.copy(language = next) }
            languageValue.text = next.label
        })

        val autocorrect = Switch(this).apply {
            text = "Autocorrect while typing"
            setTextColor(Color.parseColor("#F6F1E4"))
            isChecked = settings.autocorrect
            setOnCheckedChangeListener { _, checked -> store.update { it.copy(autocorrect = checked) } }
        }
        column.addView(autocorrect)

        column.addView(heading("Serwaa"))
        column.addView(
            body(
                "Serwaa is the TraffiAlert GH voice, spoken Sirwaah. This screen keeps her rate, pitch, " +
                    "and British English settings. ${SerwaaPersona.INTRO}",
            ),
        )
        val name = field("Your name", settings.userName)
        val greeting = field("Greeting. Leave blank for the default. {name} is replaced.", settings.greeting)
        column.addView(label("Your name"))
        column.addView(name)
        column.addView(label("Greeting"))
        column.addView(greeting)
        column.addView(label("Language Serwaa listens in"))
        val listenValue = body(settings.serwaaLanguage.label)
        column.addView(listenValue)
        column.addView(quietButton("Next listening language") {
            val now = store.get().serwaaLanguage
            val all = KeyboardLanguage.entries
            val next = all[(all.indexOf(now) + 1) % all.size]
            store.update { it.copy(serwaaLanguage = next) }
            listenValue.text = next.label
        })
        val ttsEndpoint = field("Optional cloud TTS URL. Blank uses the phone voice.", settings.ttsEndpoint)
        val ttsKey = field("Optional cloud TTS key. Not stored in the app code.", settings.ttsKey, password = true)
        column.addView(label("Cloud TTS endpoint"))
        column.addView(ttsEndpoint)
        column.addView(label("Cloud TTS key"))
        column.addView(ttsKey)
        column.addView(goldButton("Hear Serwaa") {
            saveText(name, greeting, ttsEndpoint, ttsKey, null, null)
            val now = store.get()
            speaker?.speak(SerwaaPersona.INTRO, now)
            status.text = "Playing Serwaa’s TraffiAlert introduction."
        })
        column.addView(quietButton("Hear your greeting") {
            saveText(name, greeting, ttsEndpoint, ttsKey, null, null)
            val now = store.get()
            val line = SerwaaPersona.greeting(now.userName, now.greeting)
            speaker?.speak(line, now)
            status.text = line
        })

        column.addView(heading("AI rewrite and translate"))
        column.addView(
            body(
                "Nothing is sent until you tap AI or Translate on the keyboard, and only if both a key and " +
                    "an endpoint are saved here. Use any OpenAI-compatible chat completions URL. " +
                    "Leave the key empty and the keyboard will tell you to set one.",
            ),
        )
        val endpoint = field("https://api.openai.com/v1", settings.aiEndpoint)
        val model = field("gpt-4o-mini", settings.aiModel)
        val key = field("API key", settings.aiKey, password = true)
        column.addView(label("Endpoint"))
        column.addView(endpoint)
        column.addView(label("Model"))
        column.addView(model)
        column.addView(label("API key"))
        column.addView(key)
        column.addView(goldButton("Save Serwaa and AI settings") {
            saveText(name, greeting, ttsEndpoint, ttsKey, endpoint, model, key)
            status.text = "Saved on this phone."
        })
        column.addView(quietButton("Clear the AI key") {
            key.setText("")
            store.update { it.copy(aiKey = "") }
            status.text = "AI key cleared."
        })

        column.addView(heading("Learned words"))
        column.addView(body("Words you type that were not in the seed lists stay on this phone."))
        column.addView(quietButton("Forget learned words") {
            LearnedWords(this).clear()
            status.text = "Learned words cleared."
        })

        column.addView(heading("About"))
        column.addView(quietButton("The Asantehene, Otumfuo") {
            startActivity(Intent(this, AboutActivity::class.java))
        })

        setContentView(ScrollView(this).apply { addView(column) })
        highlightTheme(settings.theme)
    }

    override fun onResume() {
        super.onResume()
        val imm = getSystemService(InputMethodManager::class.java)
        val enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        val selected = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.startsWith(packageName) == true
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        status.text = buildString {
            append(if (enabled) "O24 Keyboard is enabled." else "O24 Keyboard is not enabled yet.")
            append(" ")
            append(if (selected) "It is the current input method." else "Choose it when you want to type.")
            append(" ")
            append(if (mic) "Microphone allowed." else "Microphone not allowed yet.")
        }
    }

    override fun onDestroy() {
        speaker?.shutdown()
        super.onDestroy()
    }

    private fun selectTheme(theme: ThemeId) {
        store.update { it.copy(theme = theme) }
        highlightTheme(theme)
        status.text = "${getString(theme.titleRes)} saved."
    }

    private fun highlightTheme(theme: ThemeId) {
        themeViews.forEach { (id, view) ->
            view.alpha = if (id == theme) 1f else 0.45f
        }
    }

    private fun saveText(
        name: EditText,
        greeting: EditText,
        ttsEndpoint: EditText,
        ttsKey: EditText,
        endpoint: EditText?,
        model: EditText?,
        key: EditText? = null,
    ) {
        store.update {
            it.copy(
                userName = name.text.toString().trim(),
                greeting = greeting.text.toString(),
                ttsEndpoint = ttsEndpoint.text.toString().trim(),
                ttsKey = ttsKey.text.toString().trim(),
                aiEndpoint = endpoint?.text?.toString()?.trim() ?: it.aiEndpoint,
                aiModel = model?.text?.toString()?.trim()?.ifBlank { "gpt-4o-mini" } ?: it.aiModel,
                aiKey = key?.text?.toString()?.trim() ?: it.aiKey,
            )
        }
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 30f
        setTextColor(Color.parseColor("#F3D37A"))
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun subtitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.parseColor("#CDBF98"))
        setPadding(0, dp(4), 0, dp(8))
    }

    private fun heading(text: String) = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTextColor(Color.parseColor("#D4AF37"))
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(22), 0, dp(6))
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.parseColor("#CDBF98"))
        setPadding(0, dp(10), 0, dp(4))
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        textSize = 15f
        setTextColor(Color.parseColor("#F6F1E4"))
        setLineSpacing(dp(2).toFloat(), 1.1f)
    }

    private fun field(hint: String, value: String, password: Boolean = false) = EditText(this).apply {
        this.hint = hint
        setText(value)
        setTextColor(Color.parseColor("#F6F1E4"))
        setHintTextColor(Color.parseColor("#8C7B52"))
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#1A160F"))
            cornerRadius = dp(10).toFloat()
            setStroke(dp(1), Color.parseColor("#D4AF37"))
        }
        if (password) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun goldButton(text: String, onClick: () -> Unit) = button(text, "#D4AF37", "#1A1408", onClick)

    private fun quietButton(text: String, onClick: () -> Unit) = button(text, "#3A2E10", "#F3D37A", onClick)

    private fun button(text: String, fill: String, ink: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setTextColor(Color.parseColor(ink))
        backgroundTintList = null
        background = GradientDrawable().apply {
            setColor(Color.parseColor(fill))
            cornerRadius = dp(12).toFloat()
        }
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(8) }
    }

    private fun previewHeight(theme: ThemeId): Int {
        val tall = theme == ThemeId.YAA_ASANTEWAA || theme == ThemeId.OKOMFO_ANOKYE || theme.jersey
        return if (tall) dp(168) else dp(120)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_AFTER_ONBOARDING = "from_onboarding"
    }
}
