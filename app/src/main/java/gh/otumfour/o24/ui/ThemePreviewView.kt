package gh.otumfour.o24.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import gh.otumfour.o24.theme.ThemeArt
import gh.otumfour.o24.theme.ThemeId
import gh.otumfour.o24.theme.colorsFor

class ThemePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    init {
        setWillNotDraw(false)
    }
    var theme: ThemeId = ThemeId.GOLDEN_STOOL
        set(value) {
            field = value
            invalidate()
        }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    override fun onDraw(canvas: Canvas) {
        val scene = theme == ThemeId.OKOMFO_ANOKYE || theme == ThemeId.YAA_ASANTEWAA || theme.jersey
        when (theme) {
            ThemeId.YAA_ASANTEWAA -> {
                ThemeArt.drawYaaHeader(canvas, width, height)
                title.color = Color.parseColor("#F3D37A")
                title.textSize = dp(15f)
                canvas.drawText("YAA ASANTEWAA", width / 2f, dp(22f), title)
                label.color = Color.parseColor("#E6D3A1")
                label.textSize = dp(10f)
                canvas.drawText("Queen Mother of Ejisu, 1900", width / 2f, dp(36f), label)
            }
            ThemeId.OKOMFO_ANOKYE -> ThemeArt.drawOkomfoScene(canvas, width, height)
            ThemeId.KOTOKO -> {
                ThemeArt.drawKotokoHeader(canvas, width, height)
                title.color = Color.WHITE
                title.textSize = dp(28f)
                canvas.drawText("24", width / 2f, height * 0.34f, title)
                title.textSize = dp(16f)
                canvas.drawText("O24", width / 2f, height * 0.48f, title)
                label.color = Color.parseColor("#F2E6C8")
                label.textSize = dp(10f)
                canvas.drawText("Porcupine Warriors spirit", width / 2f, height * 0.58f, label)
            }
            ThemeId.ASANTEMAN_JERSEY -> {
                ThemeArt.drawAsantemanHeader(canvas, width, height)
                title.color = Color.parseColor("#F3D37A")
                title.textSize = dp(28f)
                canvas.drawText("24", width * 0.62f, height * 0.42f, title)
                title.textSize = dp(13f)
                canvas.drawText("ASANTEMAN", width * 0.62f, height * 0.56f, title)
            }
            else -> ThemeArt.draw(canvas, theme, width, height)
        }
        val colors = colorsFor(theme)
        val gap = dp(6f)
        val keyH = height * if (scene) 0.16f else 0.28f
        val top = height - keyH - dp(8f)
        val letters = listOf("O", "2", "4")
        val keyW = (width - gap * (letters.size + 1)) / letters.size
        var x = gap
        letters.forEach { letter ->
            val rect = RectF(x, top, x + keyW, top + keyH)
            val goldKey = letter == "4"
            fill.style = Paint.Style.FILL
            fill.color = if (goldKey) colors.accentFill else colors.keyFill
            canvas.drawRoundRect(rect, dp(colors.cornerDp), dp(colors.cornerDp), fill)
            fill.style = Paint.Style.STROKE
            fill.strokeWidth = dp(1.3f)
            fill.color = if (goldKey) colors.accentFill else colors.keyStroke
            canvas.drawRoundRect(rect, dp(colors.cornerDp), dp(colors.cornerDp), fill)
            label.color = if (goldKey) colors.accentLabel else colors.keyLabel
            label.textSize = dp(14f)
            val baseline = rect.centerY() - (label.descent() + label.ascent()) / 2f
            canvas.drawText(letter, rect.centerX(), baseline, label)
            x += keyW + gap
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
