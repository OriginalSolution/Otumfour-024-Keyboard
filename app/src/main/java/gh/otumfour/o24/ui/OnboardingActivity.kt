package gh.otumfour.o24.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import gh.otumfour.o24.R
import gh.otumfour.o24.data.SettingsStore

/** Welcome, enable, choose the keyboard, pick a theme, then finish. */
class OnboardingActivity : AppCompatActivity() {
    private lateinit var pager: ViewPager2
    private lateinit var dots: PageDotsView
    private lateinit var stepLabel: TextView
    private lateinit var swipeHint: TextView
    private lateinit var primary: android.widget.Button
    private lateinit var backButton: android.widget.Button
    private var enableStatus: TextView? = null
    private var pickStatus: TextView? = null
    private var movingForward = true
    private var enableKnown = false
    private var pickKnown = false
    private var freezeAdvance = false

    override fun onCreate(savedInstanceState: Bundle?) {
        prepareEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = SettingsStore(this)
        if (store.isOnboarded()) {
            openSettings()
            return
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBack()
        })
        val start = OnboardingRouter.resumePage(
            stored = store.onboardingPage(),
            requested = if (intent.hasExtra(EXTRA_PAGE)) intent.getIntExtra(EXTRA_PAGE, 0) else null,
            restored = savedInstanceState?.getInt(STATE_PAGE),
        )
        installUi(start)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::pager.isInitialized) {
            outState.putInt(STATE_PAGE, pager.currentItem)
            SettingsStore(this).setOnboardingPage(pager.currentItem)
        }
    }

    override fun onPause() {
        if (::pager.isInitialized) SettingsStore(this).setOnboardingPage(pager.currentItem)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        maybeAdvance()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) maybeAdvance()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        freezeAdvance = true
        installUi(if (::pager.isInitialized) pager.currentItem else 0)
        freezeAdvance = false
    }

    private fun installUi(page: Int) {
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
        }
        top.addView(stepLabel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(top)
        pager = ViewPager2(this)
        pager.adapter = PageAdapter()
        pager.offscreenPageLimit = 1
        pager.setBackgroundColor(o24Color(O24Color.BG))
        root.addView(pager, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        swipeHint = mutedText(this, getString(R.string.swipe_hint)).apply { gravity = Gravity.CENTER }
        root.addView(swipeHint)
        dots = PageDotsView(this).apply { count = PAGE_COUNT }
        root.addView(dots)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(6), dp(16), dp(16))
        }
        backButton = outlinePill(this, getString(R.string.slide_back)) { goBack() }
        backButton.contentDescription = getString(R.string.slide_back)
        primary = goldPill(this, getString(R.string.welcome_continue)) { advance() }
        primary.contentDescription = getString(R.string.slide_next)
        row.addView(backButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) })
        row.addView(primary, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(row)
        setContentView(root)
        root.applySystemBarInsets()
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                SettingsStore(this@OnboardingActivity).setOnboardingPage(position)
                bindChrome(position)
                maybeAdvance()
            }
        })
        pager.post {
            (pager.getChildAt(0) as? RecyclerView)?.setBackgroundColor(o24Color(O24Color.BG))
            pager.setCurrentItem(page, false)
            bindChrome(page)
        }
    }

    private fun bindChrome(position: Int) {
        dots.selected = position
        stepLabel.text = getString(R.string.step_of, position + 1, PAGE_COUNT)
        swipeHint.visibility = if (position == 0) View.VISIBLE else View.GONE
        val primaryParams = primary.layoutParams as LinearLayout.LayoutParams
        if (position == 0) {
            backButton.visibility = View.GONE
            primaryParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            primaryParams.weight = 0f
        } else {
            backButton.visibility = View.VISIBLE
            primaryParams.width = 0
            primaryParams.weight = 1f
        }
        primary.layoutParams = primaryParams
        primary.text = when (position) {
            0 -> getString(R.string.welcome_continue)
            PAGE_COUNT - 1 -> getString(R.string.slide_finish)
            else -> getString(R.string.slide_next)
        }
        enableStatus?.text = if (keyboardEnabled()) getString(R.string.step_done_check) else ""
        pickStatus?.text = if (keyboardSelected()) getString(R.string.step_done_check) else ""
    }

    private fun goBack() {
        movingForward = false
        if (pager.currentItem > 0) pager.currentItem = pager.currentItem - 1 else finish()
    }

    private fun advance() {
        movingForward = true
        if (pager.currentItem < PAGE_COUNT - 1) pager.currentItem = pager.currentItem + 1
        else finishOnboarding()
    }

    private fun finishOnboarding() {
        if (!SettingsStoreHolder.markDone(this)) SettingsStore(this).setOnboarded()
        SettingsStore(this).setOnboardingPage(0)
        openSettings()
    }

    private fun openSettings() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_AFTER_ONBOARDING, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        finish()
    }

    private fun maybeAdvance() {
        if (!::pager.isInitialized || freezeAdvance) return
        val page = pager.currentItem
        if (page == 1) {
            val enabled = keyboardEnabled()
            if (enabled) {
                enableStatus?.text = getString(R.string.step_done_check)
                val became = !enableKnown
                enableKnown = true
                if (movingForward || became) {
                    movingForward = true
                    pager.post { if (pager.currentItem == 1) pager.setCurrentItem(2, true) }
                }
            } else {
                enableKnown = false
            }
        }
        if (page == 2) {
            val selected = keyboardSelected()
            if (selected) {
                pickStatus?.text = getString(R.string.step_done_check)
                val became = !pickKnown
                pickKnown = true
                if (movingForward || became) {
                    movingForward = true
                    pager.post { if (pager.currentItem == 2) pager.setCurrentItem(3, true) }
                }
            } else {
                pickKnown = false
            }
        }
    }

    private fun keyboardEnabled(): Boolean {
        val imm = getSystemService(InputMethodManager::class.java) ?: return false
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }

    private fun keyboardSelected(): Boolean {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return false
        return id.startsWith(packageName)
    }

    private fun openEnable() {
        Toast.makeText(this, getString(R.string.hint_enable), Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }

    private fun openPicker() {
        Toast.makeText(this, getString(R.string.hint_choose), Toast.LENGTH_LONG).show()
        getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
    }

    private fun welcomePage(): View {
        val frame = PatternFrame(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(4), dp(20), dp(8))
        }
        column.addView(ArchPhotoView(this), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        column.addView(titleText(this, getString(R.string.welcome_akwaaba)))
        column.addView(mutedText(this, getString(R.string.welcome_brand)).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        })
        column.addView(bodyText(this, getString(R.string.welcome_tagline)).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(8))
        })
        column.addView(outlinePill(this, getString(R.string.welcome_skip)) { finishOnboarding() })
        val center = CenterFrame(this)
        center.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        frame.addView(center, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        return frame
    }

    private fun guidePage(title: String, hint: String, actionLabel: String, statusSlot: (TextView) -> Unit, action: () -> Unit): View {
        val frame = PatternFrame(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        column.addView(titleText(this, title))
        column.addView(EmptyShelfView(this))
        val card = o24Card(this) {
            addView(bodyText(this@OnboardingActivity, hint))
            val status = mutedText(this@OnboardingActivity, "")
            status.setTextColor(o24Color(O24Color.GOLD))
            statusSlot(status)
            addView(status)
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.topMargin = dp(8)
        column.addView(card, params)
        column.addView(goldPill(this, actionLabel, action), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(16) })
        val scroll = android.widget.ScrollView(this).apply { isFillViewport = true }
        val center = CenterFrame(this)
        center.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        scroll.addView(center, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        frame.addView(scroll, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        return frame
    }

    private fun donePage(): View {
        val frame = PatternFrame(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(12))
        }
        column.addView(titleText(this, getString(R.string.slide_done_title)))
        column.addView(o24Card(this) {
            addView(bodyText(this@OnboardingActivity, getString(R.string.slide_done_body)))
        }.also { marginBelow(it, 10) })
        column.addView(o24Card(this) {
            addView(sectionLabel(this@OnboardingActivity, getString(R.string.settings_theme)))
            addView(bodyText(this@OnboardingActivity, getString(R.string.done_theme_note)))
        }.also { marginBelow(it, 10) })
        column.addView(o24Card(this) {
            addView(sectionLabel(this@OnboardingActivity, getString(R.string.settings_languages)))
            addView(bodyText(this@OnboardingActivity, getString(R.string.done_language_note)))
        })
        val scroll = android.widget.ScrollView(this).apply { isFillViewport = true }
        val center = CenterFrame(this)
        center.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        scroll.addView(center, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        frame.addView(scroll)
        return frame
    }

    private inner class PageAdapter : RecyclerView.Adapter<PageAdapter.Holder>() {
        inner class Holder(view: View) : RecyclerView.ViewHolder(view)
        override fun getItemCount(): Int = PAGE_COUNT
        override fun getItemViewType(position: Int): Int = position
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = when (viewType) {
                0 -> welcomePage()
                1 -> guidePage(
                    getString(R.string.slide_enable_title),
                    getString(R.string.hint_enable),
                    getString(R.string.onboarding_enable_button),
                    { enableStatus = it },
                    ::openEnable,
                )
                2 -> guidePage(
                    getString(R.string.slide_pick_title),
                    getString(R.string.hint_choose),
                    getString(R.string.onboarding_choose_button),
                    { pickStatus = it },
                    ::openPicker,
                )
                3 -> ThemeCarouselView(this@OnboardingActivity).apply {
                    onReadHistory = { showThemeHistory(it) }
                    val requested = intent.getIntExtra(MainActivity.EXTRA_THEME, -1)
                    if (requested >= 0) post { showTheme(requested) }
                }
                else -> donePage()
            }
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return Holder(view)
        }
        override fun onBindViewHolder(holder: Holder, position: Int) = Unit
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_PAGE = "page"
        private const val PAGE_COUNT = OnboardingRouter.PAGE_COUNT
        private const val STATE_PAGE = "page"
    }
}
