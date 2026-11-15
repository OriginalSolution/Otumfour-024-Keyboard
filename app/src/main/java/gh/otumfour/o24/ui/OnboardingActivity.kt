package gh.otumfour.o24.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import gh.otumfour.o24.R

/** Welcome, enable, pick the keyboard, choose a theme, then done. */
class OnboardingActivity : AppCompatActivity() {
    private lateinit var pager: ViewPager2
    private lateinit var dots: PageDotsView
    private lateinit var primary: android.widget.Button
    private lateinit var secondary: android.widget.Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0C0A07"))
        }
        root.addView(slideBackButton(this) { goBack() })
        pager = ViewPager2(this)
        pager.adapter = PageAdapter()
        root.addView(pager, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ))
        dots = PageDotsView(this).apply { count = PAGE_COUNT }
        root.addView(dots)
        primary = goldPill(this, getString(R.string.welcome_continue)) { advance() }
        secondary = outlinePill(this, getString(R.string.welcome_skip)) { finishOnboarding() }
        val side = dp(24)
        root.addView(primary, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { marginStart = side; marginEnd = side; topMargin = dp(8) })
        root.addView(secondary, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { marginStart = side; marginEnd = side; topMargin = dp(8); bottomMargin = dp(16) })
        setContentView(root)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = bindChrome(position)
        })
        bindChrome(0)
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
        if (pager.currentItem > 0) pager.currentItem = pager.currentItem - 1 else finish()
    }

    private fun advance() {
        if (pager.currentItem < PAGE_COUNT - 1) pager.currentItem = pager.currentItem + 1
        else finishOnboarding()
    }

    private fun finishOnboarding() {
        SettingsStoreHolder.markDone(this)
        startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_AFTER_ONBOARDING, true))
        finish()
    }

    private fun bindChrome(position: Int) {
        dots.selected = position
        secondary.visibility = if (position == 0) View.VISIBLE else View.GONE
        primary.text = when (position) {
            0 -> getString(R.string.welcome_continue)
            PAGE_COUNT - 1 -> getString(R.string.slide_done)
            else -> getString(R.string.slide_next)
        }
    }

    private fun welcomePage(): View {
        val frame = FrameLayout(this)
        frame.addView(KenteBackdrop(this), FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ))
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(4), dp(20), dp(8))
        }
        column.addView(ArchPhotoView(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ))
        column.addView(TextView(this).apply {
            text = getString(R.string.welcome_akwaaba)
            textSize = 40f
            setTextColor(Color.parseColor("#E8B52D"))
            typeface = Typeface.create("serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        })
        column.addView(TextView(this).apply {
            text = getString(R.string.welcome_brand)
            textSize = 13f
            setTextColor(Color.parseColor("#F6F1E4"))
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        })
        column.addView(TextView(this).apply {
            text = getString(R.string.welcome_tagline)
            textSize = 15f
            setTextColor(Color.parseColor("#CDBF98"))
            typeface = Typeface.create("serif", Typeface.ITALIC)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })
        frame.addView(column, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ))
        return frame
    }

    private fun infoPage(title: String, body: String, actionLabel: String, action: () -> Unit): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(24), dp(28), dp(12))
            setBackgroundColor(Color.parseColor("#0C0A07"))
        }
        column.addView(slideTitle(this, title))
        column.addView(slideBody(this, body).apply {
            setPadding(0, dp(16), 0, dp(20))
        })
        column.addView(outlinePill(this, actionLabel, action), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        return column
    }

    private inner class PageAdapter : RecyclerView.Adapter<PageAdapter.Holder>() {
        inner class Holder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int = PAGE_COUNT

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = when (viewType) {
                0 -> welcomePage()
                1 -> infoPage(
                    getString(R.string.slide_enable_title),
                    getString(R.string.onboarding_enable_body),
                    getString(R.string.onboarding_enable_button),
                ) { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
                2 -> infoPage(
                    getString(R.string.slide_pick_title),
                    getString(R.string.slide_pick_body),
                    getString(R.string.onboarding_choose_button),
                ) { getSystemService(InputMethodManager::class.java).showInputMethodPicker() }
                3 -> ThemeCarouselView(this@OnboardingActivity).apply {
                    onReadHistory = { showThemeHistory(it) }
                }
                else -> infoPage(
                    getString(R.string.slide_done_title),
                    getString(R.string.slide_done_body),
                    getString(R.string.slide_done),
                ) { finishOnboarding() }
            }
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) = Unit

        override fun getItemViewType(position: Int): Int = position
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val PAGE_COUNT = 5
        private const val STATE_PAGE = "page"
    }
}
