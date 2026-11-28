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
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import gh.otumfour.o24.R
import gh.otumfour.o24.data.KeyHeight
import gh.otumfour.o24.data.LetterSize
import gh.otumfour.o24.suggest.SwipeKey
import gh.otumfour.o24.suggest.SwipeSample
import gh.otumfour.o24.theme.BackgroundStrength
import gh.otumfour.o24.theme.GoldenStool
import gh.otumfour.o24.theme.KeyCap
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
    var onExtra: (String) -> Unit = {}
    var onSwipe: (List<SwipeSample>, List<SwipeKey>, Float) -> Unit = { _, _, _ -> }
    var handedness: Handedness = Handedness.CENTER
        set(value) {
            field = value
            requestLayout()
        }
    var heightScale: Float = 1f
        set(value) {
            field = value.coerceIn(0.8f, 1.35f)
            requestLayout()
        }
    var hapticEnabled: Boolean = true
    var soundEnabled: Boolean = false
    var previewEnabled: Boolean = true
    var keyHeight: KeyHeight = KeyHeight.NORMAL
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }
    var letterSize: LetterSize = LetterSize.NORMAL
        set(value) {
            field = value
            invalidate()
        }
    var backgroundStrength: BackgroundStrength = BackgroundStrength.SOFT
        set(value) {
            field = value
            art = null
            invalidate()
        }
    var swipeEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) clearTrail()
        }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private class Placed(val key: KeySpec, val rect: RectF, val row: Int)
    private val placed = ArrayList<Placed>()
    private var art: Bitmap? = null
    private var artCanvas: Canvas? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pressed: Placed? = null
    private var longFired = false
    private var previewWindow: PopupWindow? = null
    private var previewText: TextView? = null
    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f
    private var swiping = false
    private var swipeHapticSent = false
    private val trail = ArrayList<SwipeSample>()
    private var trailUntil = 0L
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
            val key = pressed?.key ?: return
            if (!key.repeat) return
            onKey(key)
            handler.postDelayed(this, 45L)
        }
    }

    private val longRunnable = Runnable {
        val item = pressed ?: return@Runnable
        val key = item.key
        if (key.alternate == "lang") {
            longFired = true
            handler.removeCallbacks(repeatRunnable)
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onAlternate(key)
            return@Runnable
        }
        val choices = key.choices()
        if (choices.isEmpty()) return@Runnable
        longFired = true
        handler.removeCallbacks(repeatRunnable)
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        if (choices.size == 1) {
            showPreview(item, choices[0])
            onExtra(choices[0])
        } else {
            showChoices(item, choices)
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
            val down = item === pressed
            val (rawBg, fg, border) = paintsFor(key, item.row, themeColors, down)
            val bg = KeyCap.ensure(rawBg, fg)
            fill.color = bg
            val corner = if (key.round) min(rect.width(), rect.height()) / 2f else radius
            canvas.drawRoundRect(rect, corner, corner, fill)
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
            label.typeface = Typeface.create(outfit ?: Typeface.DEFAULT, Typeface.BOLD)
            label.isFakeBoldText = true
            val letter = sp(KeyMetrics.letterSp(letterSize, resources.configuration.screenWidthDp))
            val wanted = when {
                key.role == KeySpec.Role.SPACE -> letter * 0.62f
                key.label.length > 2 -> letter * 0.68f
                else -> letter
            }
            label.textAlign = Paint.Align.CENTER
            label.textSize = min(wanted, rect.height() * 0.72f)
            val hint = key.hint ?: key.alternate?.takeIf { it.length == 1 && it != key.label && it != "lang" }
            hint?.let { mark ->
                label.textAlign = Paint.Align.RIGHT
                label.textSize = sp(11f)
                canvas.drawText(mark, rect.right - dp(4f), rect.top + dp(2f) - label.ascent(), label)
                label.textAlign = Paint.Align.CENTER
                label.textSize = min(wanted, rect.height() * 0.72f)
            }
            val baseline = rect.centerY() - (label.descent() + label.ascent()) / 2f
            if (themeColors.modern && key.role == KeySpec.Role.SPACE) {
                val textWidth = label.measureText(key.label)
                drawKenteCap(canvas, rect.centerX() - textWidth / 2f - dp(16f), rect.centerY())
                drawKenteCap(canvas, rect.centerX() + textWidth / 2f + dp(16f), rect.centerY())
            }
            if (key.icon != null) drawKeyIcon(canvas, rect, key.icon, fg)
            else canvas.drawText(key.label, rect.centerX(), baseline, label)
        }
        drawTrail(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val item = hitPlaced(event.x, event.y) ?: return true
                val key = item.key
                pressed = item
                longFired = false
                swiping = false
                swipeHapticSent = false
                downTime = event.eventTime
                downX = event.x
                downY = event.y
                if (swipeEnabled) {
                    trail.clear()
                    trailUntil = 0L
                    trail.add(SwipeSample(event.x, event.y, event.eventTime))
                } else {
                    clearTrail()
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                showPreview(item, key.label)
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
                val travel = hypot(event.x - downX, event.y - downY)
                if (swipeEnabled) trail.add(SwipeSample(event.x, event.y, event.eventTime))
                val key = pressed?.key
                val letter = key?.value?.length == 1 && key.value[0].isLetter()
                if (!swiping && swipeEnabled && letter && travel > swipeTravel()) {
                    swiping = true
                    hidePreview()
                    handler.removeCallbacks(longRunnable)
                    handler.removeCallbacks(repeatRunnable)
                    if (!swipeHapticSent) {
                        swipeHapticSent = true
                        tick()
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longRunnable)
                handler.removeCallbacks(repeatRunnable)
                hidePreview()
                val key = pressed?.key
                val moved = hypot(event.x - downX, event.y - downY)
                if (event.actionMasked == MotionEvent.ACTION_UP && swipeEnabled && swiping && trail.size > 1) {
                    trail.add(SwipeSample(event.x, event.y, event.eventTime))
                    tick()
                    onSwipe(trail.toList(), letterKeys(), averageKeyWidth())
                    trailUntil = SystemClock.uptimeMillis() + 300L
                    handler.postDelayed({ invalidate() }, 16L)
                } else if (
                    event.actionMasked == MotionEvent.ACTION_UP &&
                    key != null &&
                    !longFired &&
                    (swipeEnabled || moved <= swipeTravel())
                ) {
                    tick()
                    if (soundEnabled) {
                        context.getSystemService(AudioManager::class.java)
                            ?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.35f)
                    }
                    onKey(key)
                    clearTrail()
                } else {
                    clearTrail()
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
        if (width == 0 || height == 0 || rows.isEmpty()) return
        layoutJoined()
    }

    private fun layoutJoined() {
        val gap = dp(5f)
        val pad = dp(4f)
        val topExtra = if (theme == ThemeId.ROYAL_CREST) dp(14f) else 0f
        val usableH = height - pad * 2 - topExtra - gap * (rows.size - 1)
        val rowH = (usableH / rows.size).coerceAtMost(preferredRowHeight())
        val cap = keyCap()
        val span = if (handedness == Handedness.CENTER) width - pad * 2 else width * 0.78f
        val leftEdge = when (handedness) {
            Handedness.LEFT, Handedness.CENTER -> pad
            Handedness.RIGHT -> width - pad - span
        }
        val rightEdge = leftEdge + span
        var y = pad + topExtra
        rows.forEachIndexed { index, row ->
            placeRow(row, leftEdge, rightEdge, y, rowH, gap, cap, index)
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
        val widths = row.map { key ->
            val raw = inner * (key.weight / weight)
            if (key.role == KeySpec.Role.LETTER) min(raw, cap) else raw
        }
        val used = widths.sum() + gap * (row.size - 1).coerceAtLeast(0)
        var x = left + ((right - left - used) / 2f).coerceAtLeast(0f)
        row.forEachIndexed { keyIndex, key ->
            val w = widths[keyIndex]
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

    private fun preferredRowHeight(): Float {
        val widthDp = resources.configuration.screenWidthDp
        val narrow = widthDp < 360
        return dp(KeyMetrics.rowHeightDp(keyHeight, widthDp, narrow))
    }

    private fun swipeTravel(): Float = SwipeGesture.engagePx(averageKeyWidth(), resources.displayMetrics.density)

    private fun clearTrail() {
        trail.clear()
        trailUntil = 0L
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        art = null
        requestLayout()
    }

    private fun hitPlaced(x: Float, y: Float): Placed? {
        if (placed.isEmpty()) return null
        val boxes = placed.mapIndexed { index, item ->
            KeyHits.Box(index, item.rect.left, item.rect.top, item.rect.right, item.rect.bottom)
        }
        val index = KeyHits.nearest(x, y, boxes) ?: return null
        return placed.getOrNull(index)
    }

    private fun ensureArt() {
        if (art != null || width == 0 || height == 0) return
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val themeColors = colors
            val photo = themeColors?.photoAsset
            if (photo != null) {
                PhotoBackdrop.draw(
                    context,
                    canvas,
                    photo,
                    width,
                    height,
                    backgroundStrength,
                    themeColors.keyboardBg,
                )
            } else {
                ThemeArt.draw(canvas, theme, width, height)
            }
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

    private fun showPreview(item: Placed, text: String) {
        if (!previewEnabled || text.isEmpty() || text.length > 3) return
        if (item.key.role == KeySpec.Role.SPACE || item.key.action == KeyAction.ENTER) return
        val shown = previewText ?: TextView(context).also { view ->
            view.gravity = Gravity.CENTER
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            view.typeface = Typeface.create(outfit ?: Typeface.DEFAULT_BOLD, Typeface.BOLD)
            view.setPadding(dp(10f).toInt(), dp(6f).toInt(), dp(10f).toInt(), dp(8f).toInt())
            previewText = view
        }
        val themeColors = colors
        val fillColor = KeyCap.ensure(themeColors?.keyFill ?: Color.BLACK, themeColors?.keyLabel ?: Color.WHITE)
        shown.text = text
        shown.setTextColor(themeColors?.keyLabel ?: Color.WHITE)
        shown.background = GradientDrawable().apply {
            cornerRadius = dp(12f)
            setColor(fillColor)
        }
        val window = previewWindow ?: PopupWindow(shown, dp(64f).toInt(), dp(72f).toInt(), false).also { popup ->
            popup.isTouchable = false
            popup.isClippingEnabled = false
            popup.elevation = dp(8f)
            previewWindow = popup
        }
        window.contentView = shown
        val location = IntArray(2)
        getLocationInWindow(location)
        val x = (location[0] + item.rect.centerX() - window.width / 2f).toInt()
        val y = (location[1] + item.rect.top - window.height - dp(8f)).toInt()
        runCatching {
            if (window.isShowing) window.update(x, y, window.width, window.height)
            else if (isAttachedToWindow) window.showAtLocation(this, Gravity.NO_GRAVITY, x, y)
        }
    }

    private fun hidePreview() {
        runCatching { previewWindow?.dismiss() }
        runCatching { choiceWindow?.dismiss() }
    }

    private var choiceWindow: PopupWindow? = null

    private fun showChoices(item: Placed, choices: List<String>) {
        val row = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(dp(4f).toInt(), dp(4f).toInt(), dp(4f).toInt(), dp(4f).toInt())
            background = GradientDrawable().apply {
                cornerRadius = dp(12f)
                val themeColors = this@KeyboardView.colors
                setColor(KeyCap.ensure(themeColors?.keyFill ?: Color.BLACK, themeColors?.keyLabel ?: Color.WHITE))
            }
        }
        choices.forEach { choice ->
            val button = TextView(context).apply {
                text = choice
                setTextColor(this@KeyboardView.colors?.keyLabel ?: Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(12f).toInt(), dp(8f).toInt(), dp(12f).toInt(), dp(8f).toInt())
                setOnClickListener {
                    onExtra(choice)
                    hidePreview()
                }
            }
            row.addView(button)
        }
        val window = PopupWindow(row, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, true)
        window.isOutsideTouchable = true
        choiceWindow = window
        val location = IntArray(2)
        getLocationInWindow(location)
        runCatching {
            window.showAtLocation(this, Gravity.NO_GRAVITY, location[0] + item.rect.left.toInt(), location[1] + item.rect.top.toInt() - dp(56f).toInt())
        }
    }

    private fun drawKeyIcon(canvas: Canvas, rect: RectF, icon: KeyIcon, color: Int) {
        stroke.color = color
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = dp(2f)
        stroke.strokeCap = Paint.Cap.ROUND
        val cx = rect.centerX()
        val cy = rect.centerY()
        val r = min(rect.width(), rect.height()) * 0.22f
        when (icon) {
            KeyIcon.SEARCH -> {
                canvas.drawCircle(cx - r * 0.25f, cy - r * 0.15f, r, stroke)
                canvas.drawLine(cx + r * 0.5f, cy + r * 0.55f, cx + r * 1.3f, cy + r * 1.25f, stroke)
            }
            KeyIcon.SEND -> {
                val path = Path()
                path.moveTo(cx - r, cy + r * 0.2f)
                path.lineTo(cx + r * 1.2f, cy)
                path.lineTo(cx - r, cy - r)
                path.close()
                fill.color = color
                canvas.drawPath(path, fill)
            }
            KeyIcon.DONE -> {
                canvas.drawLine(cx - r, cy, cx - r * 0.2f, cy + r * 0.8f, stroke)
                canvas.drawLine(cx - r * 0.2f, cy + r * 0.8f, cx + r * 1.1f, cy - r * 0.7f, stroke)
            }
            KeyIcon.NEXT, KeyIcon.GO -> {
                canvas.drawLine(cx - r, cy, cx + r, cy, stroke)
                canvas.drawLine(cx + r * 0.2f, cy - r * 0.7f, cx + r, cy, stroke)
                canvas.drawLine(cx + r * 0.2f, cy + r * 0.7f, cx + r, cy, stroke)
            }
            KeyIcon.ENTER -> {
                canvas.drawLine(cx - r * 0.2f, cy - r, cx - r * 0.2f, cy + r * 0.4f, stroke)
                canvas.drawLine(cx - r * 0.2f, cy + r * 0.4f, cx + r, cy + r * 0.4f, stroke)
                canvas.drawLine(cx + r * 0.3f, cy - r * 0.2f, cx + r, cy + r * 0.4f, stroke)
                canvas.drawLine(cx + r * 0.3f, cy + r, cx + r, cy + r * 0.4f, stroke)
            }
        }
        stroke.style = Paint.Style.STROKE
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        hidePreview()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rowsCount = rows.size.coerceAtLeast(4)
        // Navigation-bar padding lives on the input view, so the background can
        // run behind the bar. Do not add that inset to the key block again.
        val rowH = preferredRowHeight()
        val wanted = ((rowH * rowsCount + dp(10f)) * heightScale).toInt()
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val spec = MeasureSpec.getSize(heightMeasureSpec)
        val height = if (mode == MeasureSpec.EXACTLY && spec > 0) spec else wanted
        setMeasuredDimension(width, height.coerceAtLeast((dp(40f) * 4).toInt()))
    }
}

enum class Handedness { LEFT, CENTER, RIGHT }
