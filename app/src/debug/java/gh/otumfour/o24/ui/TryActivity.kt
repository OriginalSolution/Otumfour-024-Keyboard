package gh.otumfour.o24.ui

import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import gh.otumfour.o24.data.SettingsStore
import gh.otumfour.o24.ime.KeyboardModel
import gh.otumfour.o24.ime.KeyboardView
import gh.otumfour.o24.lang.KeyboardLanguage
import gh.otumfour.o24.theme.ThemeId
import gh.otumfour.o24.theme.colorsFor
import gh.otumfour.o24.ime.BoardPage
import gh.otumfour.o24.ime.ShiftState

/** Debug-only field used to photograph the keyboard. Not in the launcher. */
class TryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareEdgeToEdge()
        val theme = ThemeId.fromStorage(intent.getStringExtra("theme"))
        SettingsStore(this).update { it.copy(theme = theme) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(o24Color(O24Color.BG))
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }
        val field = EditText(this).apply {
            hint = "Type with Otumfour"
            setText(intent.getStringExtra("sentence") ?: "Akwaaba, Ghana")
            setTextColor(o24Color(O24Color.CREAM))
            textSize = 20f
            showSoftInputOnFocus = !intent.getBooleanExtra("preview", false)
        }
        root.addView(field)
        root.addView(android.view.View(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ))
        if (intent.getBooleanExtra("preview", false)) {
            val colors = colorsFor(theme)
            root.addView(TextView(this).apply {
                text = "Akwaaba    Ghana    024"
                setTextColor(colors.suggestionText)
                setBackgroundColor(colors.suggestionBg)
                setPadding(pad(12), pad(10), pad(12), pad(10))
            })
            root.addView(KeyboardView(this).apply {
                this.theme = theme
                this.colors = colors
                rows = KeyboardModel.rows(BoardPage.LETTERS, KeyboardLanguage.ENGLISH, ShiftState.OFF, "Enter", colors.spaceTitle)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        setContentView(root)
        root.applySystemBarInsets()
        if (!intent.getBooleanExtra("preview", false)) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            field.requestFocus()
            field.post {
                getSystemService(InputMethodManager::class.java)?.showSoftInput(field, InputMethodManager.SHOW_FORCED)
            }
        }
    }

    private fun pad(value: Int) = (value * resources.displayMetrics.density).toInt()
}
