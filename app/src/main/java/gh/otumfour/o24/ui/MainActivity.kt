package gh.otumfour.o24.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import gh.otumfour.o24.BuildConfig
import gh.otumfour.o24.CrashReport
import gh.otumfour.o24.R
import gh.otumfour.o24.data.LearnedWords
import gh.otumfour.o24.data.SettingsStore
import gh.otumfour.o24.lang.KeyboardLanguage
import gh.otumfour.o24.theme.ThemeId

/** Home and settings slides. Wide screens use a two-pane layout. */
class MainActivity : AppCompatActivity() {
    private lateinit var store: SettingsStore
    private var pager: ViewPager2? = null
    private var dots: PageDotsView? = null
    private var stepLabel: TextView? = null
    private var swipeHint: TextView? = null
    private var nextButton: android.widget.Button? = null
    private var backButton: android.widget.Button? = null
    private var section = 0
    private var developerBox: View? = null
    private var detail: FrameLayout? = null
    private var freezeAdvance = false
    private var versionTaps = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareEdgeToEdge()
        store = SettingsStore(this)
        val open = intent.getBooleanExtra(EXTRA_OPEN, false)
        val finished = OnboardingRouter.showSettings(store.isOnboarded()) ||
            intent.getBooleanExtra(EXTRA_AFTER_ONBOARDING, false)
        if (!finished && !open) {
            startActivity(
                Intent(this, OnboardingActivity::class.java)
                    .putExtra(OnboardingActivity.EXTRA_PAGE, store.onboardingPage()),
            )
            finish()
            return
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBack()
        })
        val start = savedInstanceState?.getInt(STATE_PAGE)
            ?: intent.getIntExtra(EXTRA_PAGE, 0)
        installUi(start.coerceIn(0, PAGE_COUNT - 1))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_PAGE, currentSection())
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isFinishing || !::store.isInitialized) return
        freezeAdvance = true
        installUi(currentSection())
        freezeAdvance = false
    }

    private fun installUi(page: Int) {
        section = page
        val shell = if (widthClass() == WidthClass.EXPANDED) twoPane(page) else slideShell(page)
        setContentView(shell)
        shell.applySystemBarInsets()
    }

    private fun slideShell(page: Int): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(o24Color(O24Color.BG))
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(slideBackButton(this) { goBack() })
        stepLabel = TextView(this).apply {
            setTextColor(o24Color(O24Color.GOLD))
            textSize = 14f
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(stepLabel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(top)
        val pages = ViewPager2(this)
        pages.adapter = PageAdapter()
        pages.offscreenPageLimit = 1
        pages.setBackgroundColor(o24Color(O24Color.BG))
        root.addView(pages, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        swipeHint = mutedText(this, getString(R.string.swipe_hint)).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(4), dp(16), 0)
        }
        root.addView(swipeHint)
        dots = PageDotsView(this).apply { count = PAGE_COUNT }
        root.addView(dots)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val pad = dp(16)
            setPadding(pad, dp(6), pad, dp(16))
        }
        backButton = outlinePill(this, getString(R.string.slide_back)) { goBack() }.also {
            it.contentDescription = getString(R.string.slide_back)
            row.addView(it, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) })
        }
        nextButton = goldPill(this, getString(R.string.slide_next)) { advance() }.also {
            it.contentDescription = getString(R.string.slide_next)
            row.addView(it, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(row)
        pager = pages
        pages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (!freezeAdvance) section = position
                bindChrome(position)
            }
        })
        pages.post {
            (pages.getChildAt(0) as? RecyclerView)?.setBackgroundColor(o24Color(O24Color.BG))
            pages.setCurrentItem(page, false)
            bindChrome(page)
        }
        return root
    }

    private fun twoPane(page: Int): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(o24Color(O24Color.BG))
        }
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(16), dp(12), dp(16))
            addView(titleText(this@MainActivity, getString(R.string.brand_name)).apply { textSize = 22f })
            addView(mutedText(this@MainActivity, getString(R.string.brand_line)))
        }
        PAGE_TITLES.forEachIndexed { index, title ->
            nav.addView(navRow(getString(title), index == page) { showDetail(index) })
        }
        root.addView(ScrollView(this).apply {
            addView(nav)
            setBackgroundColor(Color.parseColor("#14110C"))
        }, LinearLayout.LayoutParams(dp(280), LinearLayout.LayoutParams.MATCH_PARENT))
        val right = PatternFrame(this)
        detail = FrameLayout(this)
        right.addView(CenterFrame(this).apply {
            addView(detail, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        root.addView(right, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        showDetail(page)
        return root
    }

    private fun showDetail(index: Int) {
        section = index
        val host = detail ?: return
        host.removeAllViews()
        val page = pageAt(index)
        host.addView(page, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun navRow(label: String, selected: Boolean, onClick: () -> Unit): View {
        val row = TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(if (selected) o24Color(O24Color.INK) else o24Color(O24Color.CREAM))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (selected) o24Color(O24Color.GOLD) else Color.TRANSPARENT)
            }
            setOnClickListener { onClick() }
        }
        return row.also {
            it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
            }
        }
    }

    private fun bindChrome(position: Int) {
        dots?.selected = position
        stepLabel?.text = getString(R.string.step_of, position + 1, PAGE_COUNT) + "  ·  " + getString(PAGE_TITLES[position])
        swipeHint?.visibility = if (position == 0) View.VISIBLE else View.GONE
        backButton?.visibility = if (position == 0) View.GONE else View.VISIBLE
        val last = position == PAGE_COUNT - 1
        nextButton?.text = getString(if (last) R.string.slide_done else R.string.slide_next)
        nextButton?.contentDescription = nextButton?.text
    }

    private fun currentSection(): Int = pager?.currentItem ?: section

    private fun goBack() {
        val pages = pager
        if (pages != null && pages.currentItem > 0) pages.currentItem = pages.currentItem - 1
        else if (pages == null && section > 0) showDetail(0)
        else finish()
    }

    private fun advance() {
        val pages = pager ?: return
        if (pages.currentItem < PAGE_COUNT - 1) pages.currentItem = pages.currentItem + 1
        else pages.setCurrentItem(0, true)
    }

    private fun openSection(index: Int) {
        val pages = pager
        if (pages != null) pages.setCurrentItem(index, true) else showDetail(index)
    }

    private fun pageAt(index: Int): View = when (index) {
        0 -> homePage()
        1 -> themePage()
        2 -> languagesPage()
        3 -> typingPage()
        4 -> aiPage()
        5 -> learnedPage()
        else -> aboutPage()
    }

    private fun homePage(): View = scrollPage {
        addView(BrandHeaderView(this@MainActivity), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(132)))
        addView(mutedText(this@MainActivity, getString(R.string.home_intro)).apply { gravity = Gravity.CENTER })
        val cards = listOf(
            Triple(R.string.settings_theme, R.string.home_theme_blurb, 1),
            Triple(R.string.settings_languages, R.string.home_languages_blurb, 2),
            Triple(R.string.settings_typing, R.string.home_typing_blurb, 3),
            Triple(R.string.settings_ai, R.string.home_ai_blurb, 4),
            Triple(R.string.settings_learned, R.string.home_learned_blurb, 5),
            Triple(R.string.settings_about, R.string.home_about_blurb, 6),
        )
        cards.chunked(2).forEach { pair ->
            val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { (title, blurb, index) ->
                row.addView(homeCard(getString(title), getString(blurb)) { openSection(index) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(6)
                    marginStart = dp(6)
                    topMargin = dp(8)
                })
            }
            if (pair.size == 1) row.addView(View(this@MainActivity), LinearLayout.LayoutParams(0, 0, 1f))
            addView(row)
        }
    }

    private fun homeCard(title: String, blurb: String, onClick: () -> Unit): View {
        val card = o24Card(this) {
            addView(TextView(this@MainActivity).apply {
                text = title
                setTextColor(o24Color(O24Color.GOLD_SOFT))
                textSize = 16f
                typeface = Typeface.create("serif", Typeface.BOLD)
            })
            addView(mutedText(this@MainActivity, blurb).apply { setPadding(0, dp(4), 0, 0) })
        }
        card.setOnClickListener { onClick() }
        card.minimumHeight = dp(108)
        return card
    }

    private fun themePage(): View = fillPage(ThemeCarouselView(this).apply {
        onReadHistory = { showThemeHistory(it) }
        val requested = intent.getIntExtra(EXTRA_THEME, -1)
        if (requested >= 0) post { showTheme(requested) }
    })

    private fun languagesPage(): View = scrollPage {
        addView(sectionLabel(this@MainActivity, getString(R.string.default_language)))
        addView(mutedText(this@MainActivity, getString(R.string.default_language_help)))
        val chips = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
        val chipScroll = HorizontalScrollView(this@MainActivity).apply {
            isHorizontalScrollBarEnabled = false
            addView(chips)
        }
        addView(chipScroll)
        fun refreshChips() {
            chips.removeAllViews()
            val settings = store.get()
            settings.enabledLanguages.forEach { language ->
                val selected = language == settings.language
                val chip = TextView(this@MainActivity).apply {
                    text = language.label
                    textSize = 14f
                    setTextColor(if (selected) o24Color(O24Color.INK) else o24Color(O24Color.GOLD))
                    setPadding(dp(14), dp(8), dp(14), dp(8))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        if (selected) setColor(o24Color(O24Color.GOLD)) else {
                            setColor(Color.TRANSPARENT)
                            setStroke(dp(1), o24Color(O24Color.GOLD))
                        }
                    }
                    setOnClickListener {
                        store.update { it.copy(language = language) }
                        refreshChips()
                    }
                }
                chips.addView(chip, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(8)
                    topMargin = dp(8)
                })
            }
        }
        refreshChips()
        addView(sectionLabel(this@MainActivity, getString(R.string.languages_on_keyboard)))
        KeyboardLanguage.entries.forEach { language ->
            addView(languageCard(language) { refreshChips() })
        }
    }

    private fun languageCard(language: KeyboardLanguage, onChanged: () -> Unit): View {
        val settings = store.get()
        val enabled = language in settings.enabledLanguages
        val letters = language.extraLetters(false).joinToString(" ").ifBlank { "A–Z" }
        val card = o24Card(this) {
            val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val names = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            names.addView(TextView(this@MainActivity).apply {
                text = language.label
                setTextColor(o24Color(O24Color.CREAM))
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
            })
            names.addView(mutedText(this@MainActivity, language.nativeName + "   ·   " + letters))
            row.addView(names, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(this@MainActivity).apply {
                text = if (enabled) "✓" else ""
                setTextColor(o24Color(O24Color.GOLD))
                textSize = 20f
            })
            addView(row)
        }
        card.setOnClickListener {
            val current = store.get()
            val set = current.enabledLanguages.toMutableSet()
            if (language in set) {
                if (set.size == 1) return@setOnClickListener
                set.remove(language)
            } else {
                set.add(language)
            }
            val nextLanguage = if (current.language in set) current.language else set.first()
            store.update { it.copy(enabledLanguages = KeyboardLanguage.entries.filter { lang -> lang in set }, language = nextLanguage) }
            onChanged()
            (card.parent as? LinearLayout)?.let { parent ->
                val index = parent.indexOfChild(card)
                parent.removeView(card)
                parent.addView(languageCard(language, onChanged), index)
            }
        }
        marginBelow(card, 8)
        return card
    }

    private fun typingPage(): View = scrollPage {
        addView(sectionLabel(this@MainActivity, getString(R.string.settings_typing)))
        addView(toggleCard(getString(R.string.typing_autocorrect), getString(R.string.typing_autocorrect_help), store.get().autocorrect) { checked ->
            store.update { it.copy(autocorrect = checked) }
        })
        addView(toggleCard(getString(R.string.typing_swipe), getString(R.string.typing_swipe_help), store.get().swipeTyping) { checked ->
            store.update { it.copy(swipeTyping = checked) }
        })
        addView(toggleCard(getString(R.string.typing_caps), getString(R.string.typing_caps_help), store.get().autoCapitalise) { checked ->
            store.update { it.copy(autoCapitalise = checked) }
        })
        addView(toggleCard(getString(R.string.typing_haptic), getString(R.string.typing_haptic_help), store.get().haptic) { checked ->
            store.update { it.copy(haptic = checked) }
        })
        addView(toggleCard(getString(R.string.typing_sound), getString(R.string.typing_sound_help), store.get().keySound) { checked ->
            store.update { it.copy(keySound = checked) }
        })
        addView(homeCard(getString(R.string.settings_learned), getString(R.string.home_learned_blurb)) { openSection(5) })
    }

    private fun toggleCard(title: String, help: String, checked: Boolean, onChange: (Boolean) -> Unit): View {
        val card = o24Card(this) {
            val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val text = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            text.addView(TextView(this@MainActivity).apply {
                this.text = title
                setTextColor(o24Color(O24Color.CREAM))
                textSize = 16f
            })
            text.addView(mutedText(this@MainActivity, help))
            row.addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(Switch(this@MainActivity).apply {
                isChecked = checked
                switchTint(this)
                setOnCheckedChangeListener { _, value -> onChange(value) }
            })
            addView(row)
        }
        marginBelow(card, 8)
        return card
    }

    private fun aiPage(): View = scrollPage {
        addView(sectionLabel(this@MainActivity, getString(R.string.settings_ai)))
        val settings = store.get()
        addView(toggleCard(getString(R.string.ai_toggle), getString(R.string.ai_friendly), settings.aiEnabled) { checked ->
            store.update { it.copy(aiEnabled = checked) }
        })
        addView(exampleCard(getString(R.string.ai_example_formal_title), getString(R.string.ai_example_formal_body)))
        addView(exampleCard(getString(R.string.ai_example_short_title), getString(R.string.ai_example_short_body)))
        addView(exampleCard(getString(R.string.ai_example_twi_title), getString(R.string.ai_example_twi_body)))
        developerBox = developerCard()
        addView(developerBox)
        developerBox?.visibility = if (store.isDeveloper()) View.VISIBLE else View.GONE
    }

    private fun exampleCard(title: String, body: String): View {
        val card = o24Card(this) {
            addView(TextView(this@MainActivity).apply {
                text = title
                setTextColor(o24Color(O24Color.GOLD))
                textSize = 15f
                typeface = Typeface.create("serif", Typeface.BOLD)
            })
            addView(bodyText(this@MainActivity, body).apply { setPadding(0, dp(4), 0, 0) })
        }
        marginBelow(card, 8)
        return card
    }

    private fun developerCard(): View {
        val settings = store.get()
        val endpoint = field(settings.aiEndpoint)
        val model = field(settings.aiModel)
        val key = field(settings.aiKey, password = true)
        val save = {
            store.update {
                it.copy(
                    aiEndpoint = endpoint.text.toString().trim(),
                    aiModel = model.text.toString().trim(),
                    aiKey = key.text.toString().trim(),
                )
            }
        }
        val watch = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = save()
        }
        listOf(endpoint, model, key).forEach { it.addTextChangedListener(watch) }
        return o24Card(this) {
            addView(mutedText(this@MainActivity, getString(R.string.developer_note)))
            addView(sectionLabel(this@MainActivity, getString(R.string.developer_endpoint)))
            addView(endpoint)
            addView(sectionLabel(this@MainActivity, getString(R.string.developer_model)))
            addView(model)
            addView(sectionLabel(this@MainActivity, getString(R.string.developer_key)))
            addView(key)
        }
    }

    private fun learnedPage(): View = scrollPage {
        addView(sectionLabel(this@MainActivity, getString(R.string.settings_learned)))
        val list = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        val query = field("")
        query.hint = getString(R.string.learned_search)
        query.inputType = EditorInfo.TYPE_CLASS_TEXT
        query.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = fillLearned(list, s?.toString().orEmpty())
        })
        addView(query)
        addView(list)
        addView(outlinePill(this@MainActivity, getString(R.string.learned_forget_all)) {
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.learned_forget_title)
                .setMessage(R.string.learned_forget_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.learned_forget_all) { _, _ ->
                    LearnedWords(this@MainActivity).clear()
                    fillLearned(list, query.text.toString())
                }
                .show()
        })
        fillLearned(list, "")
    }

    private fun fillLearned(list: LinearLayout, query: String) {
        list.removeAllViews()
        val needle = query.trim().lowercase()
        val entries = LearnedWords(this).entries().filter { needle.isEmpty() || it.word.contains(needle) || it.language.contains(needle) }
        if (entries.isEmpty()) {
            list.addView(EmptyShelfView(this))
            list.addView(bodyText(this, getString(R.string.learned_empty)).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(8), dp(12), dp(12))
            })
            return
        }
        entries.forEach { entry ->
            val language = KeyboardLanguage.fromCode(entry.language).label
            val card = o24Card(this) {
                val row = LinearLayout(this@MainActivity).apply { gravity = Gravity.CENTER_VERTICAL }
                val text = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
                text.addView(TextView(this@MainActivity).apply {
                    this.text = entry.word
                    setTextColor(o24Color(O24Color.CREAM))
                    textSize = 16f
                })
                text.addView(mutedText(this@MainActivity, "$language  ·  ${entry.count}"))
                row.addView(text, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(outlinePill(this@MainActivity, getString(R.string.learned_delete)) {
                    LearnedWords(this@MainActivity).forget(entry.language, entry.word)
                    fillLearned(list, query)
                })
                addView(row)
            }
            marginBelow(card, 8)
            list.addView(card)
        }
    }

    private fun aboutPage(): View = scrollPage {
        addView(aboutColumn(this@MainActivity) { revealDeveloper() })
    }

    private fun revealDeveloper() {
        versionTaps += 1
        if (versionTaps < 7) return
        versionTaps = 0
        store.setDeveloper(true)
        developerBox?.visibility = View.VISIBLE
        Toast.makeText(this, getString(R.string.developer_on), Toast.LENGTH_SHORT).show()
    }

    private fun scrollPage(block: LinearLayout.() -> Unit): View {
        val frame = PatternFrame(this)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
            block()
        }
        val center = CenterFrame(this)
        center.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        scroll.addView(center, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        frame.addView(scroll, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        return frame
    }

    private fun fillPage(child: View): View {
        val frame = PatternFrame(this)
        val center = CenterFrame(this)
        center.addView(child, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        frame.addView(center, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        return frame
    }

    private fun field(value: String, password: Boolean = false) = EditText(this).apply {
        setText(value)
        setTextColor(o24Color(O24Color.CREAM))
        setHintTextColor(o24Color("#8C7B52"))
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#100E0A"))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), o24Color(O24Color.GOLD))
        }
        if (password) inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
    }

    private inner class PageAdapter : RecyclerView.Adapter<PageAdapter.Holder>() {
        inner class Holder(view: View) : RecyclerView.ViewHolder(view)
        override fun getItemCount(): Int = PAGE_COUNT
        override fun getItemViewType(position: Int): Int = position
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = pageAt(viewType)
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return Holder(view)
        }
        override fun onBindViewHolder(holder: Holder, position: Int) = Unit
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_AFTER_ONBOARDING = "from_onboarding"
        const val EXTRA_PAGE = "page"
        const val EXTRA_OPEN = "open"
        const val EXTRA_THEME = "theme_index"
        private const val STATE_PAGE = "page"
        private const val PAGE_COUNT = 7
        private val PAGE_TITLES = intArrayOf(
            R.string.settings_home,
            R.string.settings_theme,
            R.string.settings_languages,
            R.string.settings_typing,
            R.string.settings_ai,
            R.string.settings_learned,
            R.string.settings_about,
        )
    }
}

fun aboutColumn(activity: AppCompatActivity, onVersion: () -> Unit): LinearLayout {
    val density = activity.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()
    return LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        addView(o24Card(activity) {
            addView(titleText(activity, activity.getString(R.string.brand_name)).apply { gravity = Gravity.START })
            addView(mutedText(activity, activity.getString(R.string.brand_line)))
            addView(bodyText(activity, activity.getString(R.string.about_body)).apply { setPadding(0, dp(8), 0, 0) })
            val version = mutedText(activity, activity.getString(R.string.about_version, BuildConfig.VERSION_NAME))
            version.setPadding(0, dp(10), 0, 0)
            version.setOnClickListener { onVersion() }
            addView(version)
            CrashReport.read(activity)?.let { report ->
                addView(outlinePill(activity, activity.getString(R.string.crash_share)) {
                    val clipboard = activity.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Otumfour crash", report))
                    Toast.makeText(activity, R.string.crash_copied, Toast.LENGTH_SHORT).show()
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.crash_share_title))
                        .putExtra(Intent.EXTRA_TEXT, report)
                    activity.startActivity(Intent.createChooser(send, activity.getString(R.string.crash_share)))
                })
            }
        })
        addView(View(activity).apply { layoutParams = LinearLayout.LayoutParams(1, dp(10)) })
        addView(o24Card(activity) {
            addView(sectionLabel(activity, activity.getString(R.string.about_photo)))
            val credit = bodyText(activity, activity.getString(R.string.photo_credit))
            credit.setTextColor(o24Color(O24Color.GOLD))
            credit.paint.isUnderlineText = true
            credit.setOnClickListener {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://commons.wikimedia.org/wiki/File:Ashanti_King_Otumfour_Osei_Tutu_II.jpg",
                )))
            }
            addView(credit)
            addView(mutedText(activity, activity.getString(R.string.history_disclaimer)).apply { setPadding(0, dp(8), 0, 0) })
        })
        addView(View(activity).apply { layoutParams = LinearLayout.LayoutParams(1, dp(10)) })
        addView(o24Card(activity) {
            addView(bodyText(activity, activity.getString(R.string.ai_about)))
        })
        ThemeId.entries.forEach { theme ->
            addView(View(activity).apply { layoutParams = LinearLayout.LayoutParams(1, dp(10)) })
            addView(o24Card(activity) {
                addView(TextView(activity).apply {
                    text = activity.getString(theme.titleRes)
                    setTextColor(o24Color(O24Color.GOLD_SOFT))
                    textSize = 18f
                    typeface = Typeface.create("serif", Typeface.BOLD)
                })
                addView(bodyText(activity, activity.getString(theme.historyRes)).apply { setPadding(0, dp(6), 0, 0) })
            })
        }
    }
}
