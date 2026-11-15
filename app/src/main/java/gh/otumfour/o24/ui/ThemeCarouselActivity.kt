package gh.otumfour.o24.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

/** The theme carousel used from settings, after onboarding. */
class ThemeCarouselActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareEdgeToEdge()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0C0A07"))
        }
        column.addView(slideBackButton(this) { finish() })
        val carousel = ThemeCarouselView(this).apply {
            onReadHistory = { showThemeHistory(it) }
        }
        column.addView(carousel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ))
        setContentView(column)
        column.applySystemBarInsets()
    }
}
