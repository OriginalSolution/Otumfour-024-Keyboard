package gh.otumfour.o24.ime

import android.content.Context
import android.util.Log
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.Shader
import android.media.AudioManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import gh.otumfour.o24.R
import gh.otumfour.o24.suggest.SwipeKey
import gh.otumfour.o24.suggest.SwipeSample
import gh.otumfour.o24.theme.GoldenStool
import gh.otumfour.o24.theme.PhotoBackdrop
import gh.otumfour.o24.theme.ThemeArt
import gh.otumfour.o24.theme.ThemeColors
import gh.otumfour.o24.theme.ThemeId
import kotlin.math.hypot
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
    var onSwipe: (List<SwipeSample>, List<SwipeKey>, Float) -> Unit = { _, _, _ -> }
    var hapticEnabled: Boolean = true
    var soundEnabled: Boolean = false
    var swipeEnabled: Boolean = true

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
    private var downX = 0f
    private var downY = 0f
    private var swiping = false
    private var swipeHapticSent = false
    private val trail = ArrayList<SwipeSample>()
    private var trailUntil = 0L
    private var splitMode = false
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val trailPath = Path()
    private val outfit: Typeface? by lazy {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_semibold) }.getOrNull()
    }

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
            label.typeface = outfit
            label.isFakeBoldText = themeColors.boldLetters || themeColors.modern
            val wanted = when {
                key.role == KeySpec.Role.SPACE -> dp(13f)
                key.label.length > 2 -> dp(13f)
                else -> dp(18f)
            }
            label.textSize = min(wanted, rect.height() * 0.46f)
            val baseline = rect.centerY() - (label.descent() + label.ascent()) / 2f
            if (themeColors.modern && key.role == KeySpec.Role.SPACE) {
                val textWidth = label.measureText(key.label)
                drawKenteCap(canvas, rect.centerX() - textWidth / 2f - dp(16f), rect.centerY())
                drawKenteCap(canvas, rect.centerX() + textWidth / 2f + dp(16f), rect.centerY())
            }
            canvas.drawText(key.label, rect.centerX(), baseline, label)
        }
        drawTrail(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val key = hit(event.x, event.y) ?: return true
                pressed = key
                longFired = false
                swiping = false
                swipeHapticSent = false
                downTime = event.eventTime
                downX = event.x
                downY = event.y
                trail.clear()
                trailUntil = 0L
                trail.add(SwipeSample(event.x, event.y, event.eventTime))
                parent?.requestDisallowInterceptTouchEvent(true)
                if (key.repeat) {
                    onKey(key)
                    longFired = true
                    handler.postDelayed(repeatRunnable, 320L)
                    tick()
                } else if (key.alternate != null) {
                    handler.postDelayed(longRunnable, 380L)
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                trail.add(SwipeSample(event.x, event.y, event.eventTime))
                val travel = hypot(event.x - downX, event.y - downY)
                val letter = pressed?.value?.length == 1 && pressed?.value?.get(0)?.isLetter() == true
                if (!swiping && swipeEnabled && letter && travel > averageKeyWidth() * 1.5f) {
                    swiping = true
                    handler.removeCallbacks(longRunnable)
                    handler.removeCallbacks(repeatRunnable)
                    if (!swipeHapticSent) {
                        swipeHapticSent = true
                        tick()
                    }
                }
                if (!swiping) {
                    val key = hit(event.x, event.y)
                    if (key != pressed) {
                        handler.removeCallbacks(longRunnable)
                        handler.removeCallbacks(repeatRunnable)
                        pressed = key
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longRunnable)
                handler.removeCallbacks(repeatRunnable)
                val key = pressed
                if (event.actionMasked == MotionEvent.ACTION_UP && swiping && trail.size > 1) {
                    trail.add(SwipeSample(event.x, event.y, event.eventTime))
                    tick()
                    onSwipe(trail.toList(), letterKeys(), averageKeyWidth())
                    trailUntil = SystemClock.uptimeMillis() + 300L
                    handler.postDelayed({ invalidate() }, 16L)
                } else if (event.actionMasked == MotionEvent.ACTION_UP && key != null && !longFired && hit(event.x, event.y) == key) {
                    tick()
                    if (soundEnabled) {
                        context.getSystemService(AudioManager::class.java)
                            ?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.35f)
                    }
                    onKey(key)
                }
                pressed = null
                swiping = false
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
        splitMode = false
        if (width == 0 || height == 0 || rows.isEmpty()) return
        val halves = if (resources.configuration.screenWidthDp >= 600) KeyboardModel.splitRows(rows) else null
        if (halves != null) {
            splitMode = true
            layoutHalves(halves.first, halves.second, halves.third)
        } else {
            layoutJoined()
        }
    }

    private fun layoutJoined() {
        val gap = dp(5f)
        val pad = dp(4f)
        val topExtra = if (theme == ThemeId.ROYAL_CREST) dp(14f) else 0f
        val bottomInset = gestureInset()
        val usableH = height - pad * 2 - topExtra - bottomInset - gap * (rows.size - 1)
        val rowH = (usableH / rows.size).coerceAtMost(dp(58f))
        val cap = keyCap()
        var y = pad + topExtra
        rows.forEachIndexed { index, row ->
            placeRow(row, pad, width - pad, y, rowH, gap, cap, index)
            y += rowH + gap
        }
    }

    private fun layoutHalves(
        extra: List<List<KeySpec>>,
        left: List<List<KeySpec>>,
        right: List<List<KeySpec>>,
    ) {
        val gap = dp(6f)
        val pad = dp(4f)
        val gutter = dp(36f)
        val bottomInset = gestureInset()
        val count = extra.size + maxOf(left.size, right.size).coerceAtLeast(1)
        val usableH = height - pad * 2 - bottomInset - gap * (count - 1)
        val rowH = (usableH / count).coerceAtMost(dp(58f))
        val cap = keyCap()
        val mid = width / 2f
        var y = pad
        extra.forEachIndexed { index, row ->
            placeRow(row, pad, width - pad, y, rowH, gap, cap, index)
            y += rowH + gap
        }
        for (index in 0 until maxOf(left.size, right.size)) {
            left.getOrNull(index)?.let { placeRow(it, pad, mid - gutter / 2f, y, rowH, gap, cap, index) }
            right.getOrNull(index)?.let { placeRow(it, mid + gutter / 2f, width - pad, y, rowH, gap, cap, index) }
            y += rowH + gap
        }
    }

    private fun placeRow(
        row: List<KeySpec>,
        left: Float,
        right: Float,
        y: Float,
        rowH: Float,
        gap: Float,
        cap: Float,
        index: Int,
    ) {
        val weight = row.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
        val inner = (right - left - gap * (row.size - 1)).coerceAtLeast(1f)
        var x = left
        row.forEach { key ->
            val raw = inner * (key.weight / weight)
            val w = if (key.role == KeySpec.Role.LETTER) min(raw, cap) else raw
            placed.add(Placed(key, RectF(x, y, x + w, y + rowH), index))
            x += w + gap
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
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val photo = colors?.photoAsset
            if (photo != null) PhotoBackdrop.draw(context, canvas, photo, width, height)
            else ThemeArt.draw(canvas, theme, width, height)
            art = bitmap
            artCanvas = canvas
        } catch (error: Throwable) {
            Log.e("O24Keyboard", "Theme bitmap failed at ${width}x$height", error)
        }
    }

    private fun letterKeys(): List<SwipeKey> = placed.mapNotNull { item ->
        val value = item.key.value
        if (value.length == 1 && value[0].isLetter()) {
            SwipeKey(value[0].lowercaseChar(), item.rect.centerX(), item.rect.centerY())
        } else {
            null
        }
    }

    private fun averageKeyWidth(): Float {
        val letters = placed.filter { it.key.value.length == 1 && it.key.value[0].isLetter() }
        if (letters.isEmpty()) return dp(48f)
        return letters.map { it.rect.width() }.average().toFloat()
    }

    private fun tick() {
        if (hapticEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun drawKenteCap(canvas: Canvas, cx: Float, cy: Float) {
        val strip = dp(3.2f)
        val height = dp(14f)
        val colors = intArrayOf(Color.parseColor("#E11D2E"), Color.parseColor("#F0C94A"), Color.parseColor("#3DDC97"))
        colors.forEachIndexed { index, color ->
            fill.color = color
            val left = cx - strip * 1.5f + index * strip
            canvas.drawRoundRect(left, cy - height / 2f, left + strip - dp(0.6f), cy + height / 2f, dp(1f), dp(1f), fill)
        }
    }

    private fun drawTrail(canvas: Canvas) {
        if (trail.size < 2) return
        val now = SystemClock.uptimeMillis()
        val alpha = if (trailUntil == 0L) 1f else ((trailUntil - now) / 300f).coerceIn(0f, 1f)
        if (alpha <= 0f) {
            trail.clear()
            return
        }
        if (trailUntil != 0L) handler.postDelayed({ invalidate() }, 16L)
        trailPath.reset()
        trailPath.moveTo(trail[0].x, trail[0].y)
        for (sample in trail.drop(1)) trailPath.lineTo(sample.x, sample.y)
        val last = trail.last()
        val first = trail.first()
        trailPaint.strokeWidth = dp(16f)
        trailPaint.shader = null
        trailPaint.color = Color.argb((70 * alpha).toInt(), 225, 29, 46)
        canvas.drawPath(trailPath, trailPaint)
        trailPaint.strokeWidth = dp(5.5f)
        trailPaint.shader = LinearGradient(
            first.x, first.y, last.x, last.y,
            intArrayOf(Color.parseColor("#FF4D5A"), Color.parseColor("#FFB703"), Color.parseColor("#F0C94A")),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        trailPaint.alpha = (255 * alpha).toInt()
        canvas.drawPath(trailPath, trailPaint)
        trailPaint.shader = null
        fill.color = Color.argb((255 * alpha).toInt(), 255, 246, 200)
        canvas.drawCircle(last.x, last.y, dp(7f), fill)
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
