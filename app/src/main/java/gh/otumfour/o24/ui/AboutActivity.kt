package gh.otumfour.o24.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import gh.otumfour.o24.R
import gh.otumfour.o24.serwaa.SerwaaPersona
import gh.otumfour.o24.theme.ThemeArt
import gh.otumfour.o24.theme.ThemeId
import android.view.View

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(32))
            setBackgroundColor(Color.parseColor("#0C0A07"))
        }
        column.addView(
            BrandHeaderView(this),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(170)),
        )
        column.addView(body("Also called O24 Keyboard. Made in honour of the Asantehene. This app is not an official publication of Manhyia Palace."))
        column.addView(note(getString(R.string.history_disclaimer)))
        column.addView(heading("Otumfuo"))
        column.addView(
            body(
                "Otumfuo Nana Osei Tutu II was enstooled on 26 April 1999 as the 16th Asantehene, " +
                    "king of the Asante nation of Ghana. Otumfuo is the title. Otumfour is a spelling " +
                    "people use for the same name. The Asante flag below the name is gold, black, and green, " +
                    "with the Golden Stool drawn at the centre.",
            ),
        )
        ThemeId.entries.forEach { theme ->
            column.addView(heading(getString(theme.titleRes)))
            if (theme == ThemeId.OKOMFO_ANOKYE || theme == ThemeId.YAA_ASANTEWAA || theme.jersey) {
                column.addView(scene(theme), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(180)))
            }
            column.addView(body(getString(theme.historyRes)))
        }
        column.addView(heading("Serwaa"))
        column.addView(
            body(
                "Serwaa, spoken Sirwaah, is the voice from TraffiAlert GH. This keyboard uses the same " +
                    "persona and the same Android voice settings: British English, a speech rate of 0.95, " +
                    "and a pitch of 1.05. ${SerwaaPersona.INTRO} Recorded clips from that app play when you " +
                    "copy them in. A cloud voice can be added in settings. No key is built into this app.",
            ),
        )
        setContentView(ScrollView(this).apply { addView(column) })
    }

    private fun scene(theme: ThemeId) = object : View(this) {
        init {
            setWillNotDraw(false)
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            when (theme) {
                ThemeId.YAA_ASANTEWAA -> {
                    ThemeArt.drawYaaHeader(canvas, width, height)
                    paint.color = Color.parseColor("#F3D37A")
                    paint.textSize = height * 0.12f
                    canvas.drawText("YAA ASANTEWAA", width / 2f, height * 0.16f, paint)
                    paint.textSize = height * 0.07f
                    paint.isFakeBoldText = false
                    canvas.drawText("Queen Mother of Ejisu, 1900", width / 2f, height * 0.26f, paint)
                }
                ThemeId.KOTOKO -> {
                    ThemeArt.drawKotokoHeader(canvas, width, height)
                    paint.color = Color.WHITE
                    paint.textSize = height * 0.22f
                    canvas.drawText("24", width / 2f, height * 0.42f, paint)
                    paint.textSize = height * 0.12f
                    canvas.drawText("O24", width / 2f, height * 0.58f, paint)
                    paint.textSize = height * 0.07f
                    paint.isFakeBoldText = false
                    canvas.drawText("Porcupine Warriors spirit", width / 2f, height * 0.72f, paint)
                }
                ThemeId.ASANTEMAN_JERSEY -> {
                    ThemeArt.drawAsantemanHeader(canvas, width, height)
                    paint.color = Color.parseColor("#F3D37A")
                    paint.textSize = height * 0.22f
                    canvas.drawText("24", width * 0.62f, height * 0.48f, paint)
                    paint.textSize = height * 0.1f
                    canvas.drawText("ASANTEMAN", width * 0.62f, height * 0.66f, paint)
                }
                else -> ThemeArt.drawOkomfoScene(canvas, width, height)
            }
        }
    }

    private fun heading(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTextColor(Color.parseColor("#D4AF37"))
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(18), 0, dp(6))
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        textSize = 16f
        setTextColor(Color.parseColor("#F6F1E4"))
        setLineSpacing(dp(2).toFloat(), 1.15f)
    }

    private fun note(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.parseColor("#CDBF98"))
        setPadding(0, dp(8), 0, 0)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
