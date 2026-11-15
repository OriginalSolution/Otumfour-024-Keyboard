package gh.otumfour.o24.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import gh.otumfour.o24.R
import gh.otumfour.o24.data.SettingsStore
import gh.otumfour.o24.theme.ThemeId

/** First-run steps: enable the keyboard, then pick one of the eight themes. */
class OnboardingActivity : AppCompatActivity() {
    private lateinit var store: SettingsStore
    private var step = 0
    private var chosen = ThemeId.GOLDEN_STOOL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SettingsStore(this)
        chosen = store.get().theme
        showStep()
    }

    private fun showStep() {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
            setBackgroundColor(Color.parseColor("#0C0A07"))
        }
        column.addView(
            BrandHeaderView(this),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(150)),
        )
        if (step == 0) enableStep(column) else themeStep(column)
        setContentView(ScrollView(this).apply { addView(column) })
    }

    private fun enableStep(column: LinearLayout) {
        column.addView(heading(getString(R.string.onboarding_welcome)))
        column.addView(heading(getString(R.string.onboarding_enable_title)))
        column.addView(body(getString(R.string.onboarding_enable_body)))
        column.addView(gold(getString(R.string.onboarding_enable_button)) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        column.addView(quiet(getString(R.string.onboarding_choose_button)) {
            getSystemService(InputMethodManager::class.java).showInputMethodPicker()
        })
        column.addView(gold(getString(R.string.onboarding_next)) {
            step = 1
            showStep()
        })
    }

    private fun themeStep(column: LinearLayout) {
        column.addView(heading(getString(R.string.onboarding_theme_title)))
        column.addView(body(getString(R.string.onboarding_theme_body)))
        fun addThemes(themes: List<ThemeId>) {
            themes.forEach { theme ->
                val selected = theme == chosen
                column.addView(label(getString(theme.titleRes) + if (selected) "  ·  selected" else ""))
                column.addView(body(getString(theme.blurbRes)))
                val preview = ThemePreviewView(this).apply {
                    this.theme = theme
                    alpha = if (selected) 1f else 0.55f
                    setOnClickListener {
                        chosen = theme
                        store.update { it.copy(theme = theme) }
                        showStep()
                    }
                }
                val tall = theme == ThemeId.YAA_ASANTEWAA || theme == ThemeId.OKOMFO_ANOKYE || theme.jersey
                val height = if (tall) dp(160) else dp(112)
                column.addView(preview, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height))
                column.addView(quiet(getString(R.string.learn_more)) { showThemeHistory(theme) })
            }
        }
        addThemes(ThemeId.entries.filter { !it.jersey })
        column.addView(heading(getString(R.string.jersey_heading)))
        column.addView(body(getString(R.string.jersey_note)))
        addThemes(ThemeId.entries.filter { it.jersey })
        column.addView(gold(getString(R.string.onboarding_finish)) {
            store.update { it.copy(theme = chosen) }
            store.setOnboarded()
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_AFTER_ONBOARDING, true)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
            finish()
        })
    }

    private fun heading(text: String) = TextView(this).apply {
        this.text = text
        textSize = 22f
        setTextColor(Color.parseColor("#F3D37A"))
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(16), 0, dp(6))
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.parseColor("#CDBF98"))
        setPadding(0, dp(12), 0, dp(2))
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        textSize = 15f
        setTextColor(Color.parseColor("#F6F1E4"))
    }

    private fun gold(text: String, onClick: () -> Unit) = button(text, "#D4AF37", "#1A1408", onClick)
    private fun quiet(text: String, onClick: () -> Unit) = button(text, "#3A2E10", "#F3D37A", onClick)

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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
