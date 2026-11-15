package gh.otumfour.o24.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import gh.otumfour.o24.R
import gh.otumfour.o24.theme.ThemeId

/** Same About text as the settings slide, including the photo credit. */
class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(28))
            setBackgroundColor(Color.parseColor("#0C0A07"))
            addView(slideBackButton(this@AboutActivity) { finish() })
            addView(slideBody(this@AboutActivity, "O24 Keyboard, also called Otumfour, is made in honour of the Asantehene. It is not a publication of Manhyia Palace."))
            addView(slideBody(this@AboutActivity, getString(R.string.ai_about)))
            val credit = slideBody(this@AboutActivity, getString(R.string.photo_credit))
            credit.setTextColor(Color.parseColor("#E8B52D"))
            credit.paint.isUnderlineText = true
            credit.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PHOTO_URL)))
            }
            addView(credit)
            addView(slideBody(this@AboutActivity, getString(R.string.history_disclaimer)))
            ThemeId.entries.forEach { theme ->
                addView(slideTitle(this@AboutActivity, getString(theme.titleRes)).apply { textSize = 18f })
                addView(slideBody(this@AboutActivity, getString(theme.historyRes)))
            }
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0C0A07"))
            addView(column)
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val PHOTO_URL =
            "https://commons.wikimedia.org/wiki/File:Ashanti_King_Otumfour_Osei_Tutu_II.jpg"
    }
}
