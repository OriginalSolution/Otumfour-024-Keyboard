package gh.otumfour.o24.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import gh.otumfour.o24.ime.BoardPage
import gh.otumfour.o24.ime.KeyboardModel
import gh.otumfour.o24.ime.KeySpec
import gh.otumfour.o24.ime.ShiftState
import gh.otumfour.o24.lang.KeyboardLanguage
import gh.otumfour.o24.theme.GoldenStool
import gh.otumfour.o24.theme.ThemeArt
import gh.otumfour.o24.theme.ThemeId
import gh.otumfour.o24.theme.colorsFor

/**
 * A scaled keyboard in the real theme: art, suggestion bar, QWERTY, and the space bar.
 */
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
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val keyRect = RectF()
    private var rowTheme: ThemeId? = null
    private var rows: List<List<KeySpec>> = emptyList()

    private fun rows(): List<List<KeySpec>> {
        if (rowTheme != theme) {
            rowTheme = theme
            rows = KeyboardModel.rows(
                BoardPage.LETTERS,
                KeyboardLanguage.ENGLISH,
                ShiftState.OFF,
                "Enter",
                colorsFor(theme).spaceTitle,
            )
        }
        return rows
    }

    override fun onDraw(canvas: Canvas) {
        ThemeArt.draw(canvas, theme, width, height)
        val colors = colorsFor(theme)
        val gap = dp(3f)
        val suggestionH = height * 0.14f
        fill.style = Paint.Style.FILL
        fill.color = colors.suggestionBg
        canvas.drawRect(0f, 0f, width.toFloat(), suggestionH, fill)
        label.color = colors.suggestionText
        label.textSize = suggestionH * 0.42f
        label.isFakeBoldText = false
        val samples = listOf("Akwaaba", "Ghana", "O24")
        val slot = width / 3f
        samples.forEachIndexed { index, word ->
            val baseline = suggestionH * 0.68f
            canvas.drawText(word, slot * index + slot / 2f, baseline, label)
        }
        val keyTop = suggestionH + gap
        val drawn = rows()
        val keyArea = height - keyTop - gap
        val rowH = (keyArea - gap * (drawn.size - 1)) / drawn.size
        var y = keyTop
        val radius = dp(colors.cornerDp.coerceAtMost(10f))
        for (row in drawn) {
            val weight = row.sumOf { it.weight.toDouble() }.toFloat()
            var x = gap
            val inner = width - gap * 2 - gap * (row.size - 1)
            for (key in row) {
                val w = inner * (key.weight / weight)
                keyRect.set(x, y, x + w, y + rowH)
                val (bg, fg, border) = paints(key, colors)
                fill.color = bg
                canvas.drawRoundRect(keyRect, radius, radius, fill)
                if (border != 0) {
                    stroke.color = border
                    stroke.strokeWidth = dp(1f)
                    canvas.drawRoundRect(keyRect, radius, radius, stroke)
                }
                if (colors.stoolOnSpace && key.role == KeySpec.Role.SPACE) {
                    GoldenStool.draw(
                        canvas,
                        keyRect.centerX(),
                        keyRect.top + dp(2f),
                        keyRect.height() * 1.05f,
                        colors.spaceLabel,
                        alpha = 90,
                    )
                }
                label.color = fg
                label.isFakeBoldText = key.role == KeySpec.Role.SPACE
                var size = rowH * 0.42f
                label.textSize = size
                val limit = keyRect.width() * 0.86f
                while (size > dp(7f) && label.measureText(key.label) > limit) {
                    size *= 0.9f
                    label.textSize = size
                }
                val baseline = keyRect.centerY() - (label.descent() + label.ascent()) / 2f
                canvas.drawText(key.label, keyRect.centerX(), baseline, label)
                x += w + gap
            }
            y += rowH + gap
        }
    }

    private fun paints(key: KeySpec, theme: gh.otumfour.o24.theme.ThemeColors): Triple<Int, Int, Int> {
        if (theme.shiftFill != 0 && key.action == gh.otumfour.o24.ime.KeyAction.SHIFT) {
            return Triple(theme.shiftFill, theme.shiftLabel, theme.shiftFill)
        }
        if (theme.enterFill != 0 && key.action == gh.otumfour.o24.ime.KeyAction.ENTER) {
            return Triple(theme.enterFill, theme.enterLabel, theme.enterFill)
        }
        return when (key.role) {
            KeySpec.Role.SPACE -> Triple(theme.spaceFill, theme.spaceLabel, theme.keyStroke)
            KeySpec.Role.ACCENT -> Triple(theme.accentFill, theme.accentLabel, theme.keyStroke)
            KeySpec.Role.ACTION -> Triple(theme.actionFill, theme.actionLabel, theme.keyStroke)
            KeySpec.Role.LETTER -> Triple(theme.keyFill, theme.keyLabel, theme.keyStroke)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
