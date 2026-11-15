package gh.otumfour.o24.ime

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import gh.otumfour.o24.theme.GoldenStool
import gh.otumfour.o24.theme.ThemeArt
import gh.otumfour.o24.theme.ThemeColors
import gh.otumfour.o24.theme.ThemeId
import kotlin.math.min

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var colors: ThemeColors? = null
        set(value) {
            field = value
            art = null
            invalidate()
        }

    var theme: ThemeId = ThemeId.GOLDEN_STOOL
        set(value) {
            field = value
            art = null
            invalidate()
        }

    var rows: List<List<KeySpec>> = emptyList()
        set(value) {
            field = value
            layoutKeys()
            requestLayout()
            invalidate()
        }

    var onKey: (KeySpec) -> Unit = {}
    var onAlternate: (KeySpec) -> Unit = {}
    var hapticEnabled: Boolean = true
    var soundEnabled: Boolean = false

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private class Placed(val key: KeySpec, val rect: RectF, val row: Int)
    private val placed = ArrayList<Placed>()
    private var art: Bitmap? = null
    private var artCanvas: Canvas? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pressed: KeySpec? = null
    private var longFired = false
    private var downTime = 0L

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val key = pressed ?: return
            if (!key.repeat) return
            onKey(key)
            handler.postDelayed(this, 45L)
        }
    }

    private val longRunnable = Runnable {
        val key = pressed ?: return@Runnable
        if (key.alternate != null) {
            longFired = true
            handler.removeCallbacks(repeatRunnable)
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onAlternate(key)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        art = null
        layoutKeys()
    }

    override fun onDraw(canvas: Canvas) {
        val themeColors = colors ?: return
        ensureArt()
        art?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        val radius = dp(themeColors.cornerDp)
        for (item in placed) {
            val key = item.key
            val rect = item.rect
            val down = key == pressed
            val (bg, fg, border) = paintsFor(key, item.row, themeColors, down)
            fill.color = bg
            canvas.drawRoundRect(rect, radius, radius, fill)
            if (themeColors.stoneKeys && key.role != KeySpec.Role.ACCENT) stone(canvas, rect, key.label)
            if (border != 0) {
                stroke.color = border
                stroke.strokeWidth = dp(1.6f)
                val inset = stroke.strokeWidth / 2f
                canvas.drawRoundRect(
                    rect.left + inset,
                    rect.top + inset,
                    rect.right - inset,
                    rect.bottom - inset,
                    radius,
                    radius,
                    stroke,
                )
            }
            if (themeColors.piping != 0) {
                stroke.color = themeColors.piping
                stroke.strokeWidth = dp(1f)
                val inset = dp(3.2f)
                canvas.drawRoundRect(
                    rect.left + inset,
                    rect.top + inset,
                    rect.right - inset,
                    rect.bottom - inset,
                    radius,
                    radius,
                    stroke,
                )
            }
            if (themeColors.stoolOnSpace && key.role == KeySpec.Role.SPACE) {
                GoldenStool.draw(
                    canvas,
                    rect.centerX(),
                    rect.top + dp(3f),
                    rect.height() * 1.15f,
                    themeColors.spaceLabel,
                    alpha = 90,
                )
            }
            label.color = fg
            label.isFakeBoldText = themeColors.boldLetters
            val wanted = when {
                key.role == KeySpec.Role.SPACE -> dp(13f)
                key.label.length > 2 -> dp(13f)
                else -> dp(18f)
            }
            label.textSize = min(wanted, rect.height() * 0.46f)
            val baseline = rect.centerY() - (label.descent() + label.ascent()) / 2f
            canvas.drawText(key.label, rect.centerX(), baseline, label)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val key = hit(event.x, event.y) ?: return true
                pressed = key
                longFired = false
                downTime = event.eventTime
                parent?.requestDisallowInterceptTouchEvent(true)
                if (key.repeat) {
                    onKey(key)
                    longFired = true
                    handler.postDelayed(repeatRunnable, 320L)
                } else if (key.alternate != null) {
                    handler.postDelayed(longRunnable, 380L)
                }
                if (hapticEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (soundEnabled) {
                    context.getSystemService(AudioManager::class.java)
                        ?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.35f)
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val key = hit(event.x, event.y)
                if (key != pressed) {
                    handler.removeCallbacks(longRunnable)
                    handler.removeCallbacks(repeatRunnable)
                    pressed = key
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longRunnable)
                handler.removeCallbacks(repeatRunnable)
                val key = pressed
                if (event.actionMasked == MotionEvent.ACTION_UP && key != null && !longFired && hit(event.x, event.y) == key) {
                    onKey(key)
                }
                pressed = null
                invalidate()
            }
        }
        return true
    }

    private fun paintsFor(key: KeySpec, row: Int, theme: ThemeColors, down: Boolean): Triple<Int, Int, Int> {
        if (theme.shiftFill != 0 && key.action == KeyAction.SHIFT) {
            return Triple(if (down) theme.pressed else theme.shiftFill, theme.shiftLabel, theme.shiftFill)
        }
        if (theme.enterFill != 0 && key.action == KeyAction.ENTER) {
            return Triple(if (down) theme.pressed else theme.enterFill, theme.enterLabel, theme.enterFill)
        }
        if (theme.enterSolid && key.action == KeyAction.ENTER) {
            return Triple(theme.accentFill, theme.accentLabel, theme.accentFill)
        }
        if (theme.enterSolid && key.action == KeyAction.SHIFT) {
            return Triple(if (down) theme.pressed else theme.keyFill, theme.keyLabel, theme.keyStroke)
        }
        if (theme.crestRows && isCrestAction(key)) {
            return Triple(if (down) theme.pressed else theme.accentFill, theme.accentLabel, theme.accentFill)
        }
        if (theme.crestRows && key.role == KeySpec.Role.LETTER && isTopLetterRow(row)) {
            return Triple(if (down) theme.pressed else theme.crestTopFill, theme.crestTopLabel, theme.crestTopFill)
        }
        if (down) {
            val fg = if (key.role == KeySpec.Role.SPACE) theme.spaceLabel else theme.keyLabel
            return Triple(theme.pressed, fg, theme.keyStroke)
        }
        return when (key.role) {
            KeySpec.Role.SPACE -> Triple(theme.spaceFill, theme.spaceLabel, theme.keyStroke)
            KeySpec.Role.ACCENT -> Triple(theme.accentFill, theme.accentLabel, theme.keyStroke)
            KeySpec.Role.ACTION -> Triple(theme.actionFill, theme.actionLabel, theme.keyStroke)
            KeySpec.Role.LETTER -> Triple(theme.keyFill, theme.keyLabel, theme.keyStroke)
        }
    }

    private fun isCrestAction(key: KeySpec): Boolean =
        key.action == KeyAction.SHIFT || key.action == KeyAction.BACKSPACE ||
            key.action == KeyAction.ENTER || key.action == KeyAction.MODE || key.action == KeyAction.EMOJI

    private fun isTopLetterRow(row: Int): Boolean {
        val keys = rows.getOrNull(row) ?: return false
        return keys.any { it.value.equals("q", ignoreCase = true) || it.value == "1" }
    }

    private fun stone(canvas: Canvas, rect: RectF, seed: String) {
        var n = seed.hashCode()
        fill.color = 0x66C6A15B
        repeat(5) {
            n = n * 1103515245 + 12345
            val px = rect.left + (kotlin.math.abs(n) % 1000) / 1000f * rect.width()
            n = n * 1103515245 + 12345
            val py = rect.top + (kotlin.math.abs(n) % 1000) / 1000f * rect.height()
            canvas.drawCircle(px, py, dp(1.1f), fill)
        }
    }

    private fun layoutKeys() {
        placed.clear()
        if (width == 0 || height == 0 || rows.isEmpty()) return
        val gap = dp(5f)
        val pad = dp(4f)
        val topExtra = if (theme == ThemeId.ROYAL_CREST) dp(14f) else 0f
        val bottomInset = gestureInset()
        val usableH = height - pad * 2 - topExtra - bottomInset - gap * (rows.size - 1)
        val rowH = (usableH / rows.size).coerceAtMost(dp(58f))
        val cap = keyCap()
        val split = resources.configuration.screenWidthDp >= 840
        var y = pad + topExtra
        rows.forEachIndexed { index, row ->
            val weight = row.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
            val inner = width - pad * 2 - gap * (row.size - 1)
            val widths = row.map { key -> min(inner * (key.weight / weight), cap) }
            val splitGap = if (split && row.size >= 6) dp(36f) else 0f
            val used = widths.sum() + gap * (row.size - 1) + splitGap
            var x = ((width - used) / 2f).coerceAtLeast(pad)
            val mid = row.size / 2
            row.forEachIndexed { keyIndex, key ->
                if (split && keyIndex == mid) x += splitGap
                val w = widths[keyIndex]
                placed.add(Placed(key, RectF(x, y, x + w, y + rowH), index))
                x += w + gap
            }
            y += rowH + gap
        }
    }

    private fun keyCap(): Float {
        val widthDp = resources.configuration.screenWidthDp
        return when {
            widthDp >= 600 -> dp(72f)
            widthDp < 360 -> dp(48f)
            else -> dp(64f)
        }
    }

    private fun gestureInset(): Float {
        val insets = rootWindowInsets ?: return 0f
        return insets.systemWindowInsetBottom.toFloat()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        art = null
        requestLayout()
    }

    private fun hit(x: Float, y: Float): KeySpec? {
        for (item in placed) {
            if (item.rect.contains(x, y)) return item.key
        }
        return null
    }

    private fun ensureArt() {
        if (art != null || width == 0 || height == 0) return
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        ThemeArt.draw(canvas, theme, width, height)
        art = bitmap
        artCanvas = canvas
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val metrics = resources.displayMetrics
        val shorter = min(metrics.widthPixels, metrics.heightPixels)
        val rowsCount = rows.size.coerceAtLeast(4)
        val narrow = resources.configuration.screenWidthDp < 360
        val rowMin = dp(if (narrow) 38f else 42f)
        val rowH = (shorter * 0.078f).coerceIn(rowMin, dp(54f))
        val wanted = (rowH * rowsCount + dp(10f) + gestureInset()).toInt()
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val spec = MeasureSpec.getSize(heightMeasureSpec)
        val height = if (mode == MeasureSpec.EXACTLY && spec > 0) spec else wanted
        setMeasuredDimension(width, height.coerceAtLeast((rowMin * 4).toInt()))
    }
}
