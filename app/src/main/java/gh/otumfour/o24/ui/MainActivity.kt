package gh.otumfour.o24.ui

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import gh.otumfour.o24.R
import gh.otumfour.o24.data.LearnedWords
import gh.otumfour.o24.data.SettingsStore
import gh.otumfour.o24.lang.KeyboardLanguage
import gh.otumfour.o24.serwaa.SerwaaPersona
import gh.otumfour.o24.serwaa.SerwaaSpeaker
import gh.otumfour.o24.theme.ThemeId

/** Settings as swipeable pages, after the onboarding slides. */
class MainActivity : AppCompatActivity() {
    private lateinit var store: SettingsStore
    private var speaker: SerwaaSpeaker? = null
    private lateinit var pager: ViewPager2
    private lateinit var dots: PageDotsView
    private lateinit var heading: TextView

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SettingsStore(this)
        if (!store.isOnboarded() && !intent.getBooleanExtra(EXTRA_AFTER_ONBOARDING, false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        speaker = SerwaaSpeaker(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0C0A07"))
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        top.addView(slideBackButton(this) { goBack() })
        heading = TextView(this).apply {
            setTextColor(Color.parseColor("#F3D37A"))
            textSize = 20f
            setPadding(0, dp(18), 0, 0)
        }
        top.addView(heading)
        root.addView(top)
        pager = ViewPager2(this)
        pager.adapter = PageAdapter()
        root.addView(pager, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ))
        dots = PageDotsView(this).apply { count = PAGE_TITLES.size }
        root.addView(dots)
        setContentView(root)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.selected = position
                heading.text = getString(PAGE_TITLES[position])
            }
        })
        heading.text = getString(PAGE_TITLES[0])
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBack()
        })
        savedInstanceState?.getInt(STATE_PAGE)?.let { pager.setCurrentItem(it, false) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::pager.isInitialized) outState.putInt(STATE_PAGE, pager.currentItem)
    }

    private fun goBack() {
        if (::pager.isInitialized && pager.currentItem > 0) pager.currentItem = pager.currentItem - 1
        else finish()
    }

    override fun onDestroy() {
        speaker?.shutdown()
        super.onDestroy()
    }

    private fun page(content: LinearLayout.() -> Unit): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(24))
            content()
        }
        return ScrollView(this).apply {
            addView(column)
            setBackgroundColor(Color.parseColor("#0C0A07"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    private fun languagesPage(): View = page {
        addView(slideBody(this@MainActivity, getString(R.string.onboarding_enable_body)))
        val settings = store.get()
        val enabled = settings.enabledLanguages.toMutableSet()
        KeyboardLanguage.entries.forEach { language ->
            addView(CheckBox(this@MainActivity).apply {
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
            })
        }
        val current = slideBody(this@MainActivity, store.get().language.label)
        addView(current)
        addView(outlinePill(this@MainActivity, "Next keyboard language") {
            val now = store.get()
            val list = now.enabledLanguages
            val index = list.indexOf(now.language).let { if (it < 0) 0 else it }
            val next = list[(index + 1) % list.size]
            store.update { it.copy(language = next) }
            current.text = next.label
        })
        addView(Switch(this@MainActivity).apply {
            text = "Autocorrect while typing"
            setTextColor(Color.parseColor("#F6F1E4"))
            isChecked = settings.autocorrect
            setOnCheckedChangeListener { _, checked -> store.update { it.copy(autocorrect = checked) } }
        })
    }

    private fun serwaaPage(): View = page {
        val settings = store.get()
        addView(slideBody(this@MainActivity, "Serwaa is the TraffiAlert voice, spoken Sirwaah. ${SerwaaPersona.INTRO}"))
        val name = field("Your name", settings.userName)
        val greeting = field("Greeting. {name} is replaced. Leave blank for the default.", settings.greeting)
        addView(label("Your name"))
        addView(name)
        addView(label("Greeting"))
        addView(greeting)
        val listen = slideBody(this@MainActivity, settings.serwaaLanguage.label)
        addView(label("Language Serwaa listens in"))
        addView(listen)
        addView(outlinePill(this@MainActivity, "Next listening language") {
            val now = store.get().serwaaLanguage
            val all = KeyboardLanguage.entries
            val next = all[(all.indexOf(now) + 1) % all.size]
            store.update { it.copy(serwaaLanguage = next) }
            listen.text = next.label
        })
        val ttsEndpoint = field("Optional cloud TTS URL", settings.ttsEndpoint)
        val ttsKey = field("Optional cloud TTS key", settings.ttsKey, password = true)
        addView(label("Cloud TTS endpoint"))
        addView(ttsEndpoint)
        addView(label("Cloud TTS key"))
        addView(ttsKey)
        addView(goldPill(this@MainActivity, "Hear Serwaa") {
            saveSerwaa(name, greeting, ttsEndpoint, ttsKey)
            speaker?.speak(SerwaaPersona.INTRO, store.get())
        })
        addView(outlinePill(this@MainActivity, "Hear your greeting") {
            saveSerwaa(name, greeting, ttsEndpoint, ttsKey)
            val now = store.get()
            speaker?.speak(SerwaaPersona.greeting(now.userName, now.greeting), now)
        })
        addView(outlinePill(this@MainActivity, "Allow the microphone") {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        })
    }

    private fun themePage(): View = page {
        val name = slideTitle(this@MainActivity, getString(store.get().theme.titleRes))
        addView(name)
        if (store.get().theme.jersey) addView(slideBody(this@MainActivity, getString(R.string.jersey_heading)))
        addView(slideBody(this@MainActivity, "Swipe every theme, including Golden Stool Royal. The one in use is marked."))
        addView(ThemePreviewView(this@MainActivity).apply { theme = store.get().theme }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(160),
        ))
        addView(goldPill(this@MainActivity, getString(R.string.open_themes)) {
            startActivity(Intent(this@MainActivity, ThemeCarouselActivity::class.java))
        })
    }

    private fun aiPage(): View = page {
        val settings = store.get()
        addView(slideBody(this@MainActivity, getString(R.string.ai_friendly)))
        addView(Switch(this@MainActivity).apply {
            text = getString(R.string.ai_toggle)
            setTextColor(Color.parseColor("#F6F1E4"))
            isChecked = settings.aiEnabled
            setOnCheckedChangeListener { _, checked -> store.update { it.copy(aiEnabled = checked) } }
        })
        val endpoint = field("https://api.openai.com/v1", settings.aiEndpoint)
        val model = field("gpt-4o-mini", settings.aiModel)
        val key = field("API key", settings.aiKey, password = true)
        val advanced = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            addView(label("Endpoint"))
            addView(endpoint)
            addView(label("Model"))
            addView(model)
            addView(label("API key"))
            addView(key)
        }
        addView(outlinePill(this@MainActivity, getString(R.string.ai_advanced)) {
            advanced.visibility = if (advanced.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        })
        addView(advanced)
        addView(goldPill(this@MainActivity, "Save AI settings") {
            store.update {
                it.copy(
                    aiEndpoint = endpoint.text.toString().trim(),
                    aiModel = model.text.toString().trim().ifBlank { "gpt-4o-mini" },
                    aiKey = key.text.toString().trim(),
                )
            }
        })
        addView(outlinePill(this@MainActivity, "Clear the AI key") {
            key.setText("")
            store.update { it.copy(aiKey = "") }
        })
    }

    private fun learnedPage(): View = page {
        addView(slideBody(this@MainActivity, "Words you type that were not in the seed lists stay on this phone."))
        addView(outlinePill(this@MainActivity, "Forget learned words") {
            LearnedWords(this@MainActivity).clear()
        })
    }

    private fun aboutPage(): View = page {
        addAbout(this@MainActivity)
    }

    private fun LinearLayout.addAbout(activity: AppCompatActivity) {
        addView(slideBody(activity, "O24 Keyboard, also called Otumfour, is made in honour of the Asantehene. It is not a publication of Manhyia Palace."))
        val credit = slideBody(activity, activity.getString(R.string.photo_credit))
        credit.setTextColor(Color.parseColor("#E8B52D"))
        credit.paint.isUnderlineText = true
        credit.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PHOTO_URL)))
        }
        addView(credit)
        addView(slideBody(activity, activity.getString(R.string.history_disclaimer)))
        ThemeId.entries.forEach { theme ->
            addView(slideTitle(activity, activity.getString(theme.titleRes)).apply { textSize = 18f })
            addView(slideBody(activity, activity.getString(theme.historyRes)))
        }
    }

    private fun saveSerwaa(name: EditText, greeting: EditText, ttsEndpoint: EditText, ttsKey: EditText) {
        store.update {
            it.copy(
                userName = name.text.toString().trim(),
                greeting = greeting.text.toString(),
                ttsEndpoint = ttsEndpoint.text.toString().trim(),
                ttsKey = ttsKey.text.toString().trim(),
            )
        }
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.parseColor("#CDBF98"))
        setPadding(0, dp(10), 0, dp(4))
    }

    private fun field(hint: String, value: String, password: Boolean = false) = EditText(this).apply {
        this.hint = hint
        setText(value)
        setTextColor(Color.parseColor("#F6F1E4"))
        setHintTextColor(Color.parseColor("#8C7B52"))
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#1A160F"))
            cornerRadius = dp(10).toFloat()
            setStroke(dp(1), Color.parseColor("#E8B52D"))
        }
        if (password) {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }

    private inner class PageAdapter : RecyclerView.Adapter<PageAdapter.Holder>() {
        inner class Holder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int = PAGE_TITLES.size

        override fun getItemViewType(position: Int): Int = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = when (viewType) {
                0 -> languagesPage()
                1 -> serwaaPage()
                2 -> themePage()
                3 -> aiPage()
                4 -> learnedPage()
                else -> aboutPage()
            }
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) = Unit
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_AFTER_ONBOARDING = "from_onboarding"
        private const val STATE_PAGE = "page"
        private const val PHOTO_URL =
            "https://commons.wikimedia.org/wiki/File:Ashanti_King_Otumfour_Osei_Tutu_II.jpg"
        private val PAGE_TITLES = intArrayOf(
            R.string.settings_languages,
            R.string.settings_serwaa,
            R.string.settings_theme,
            R.string.settings_ai,
            R.string.settings_learned,
            R.string.settings_about,
        )
    }
}
