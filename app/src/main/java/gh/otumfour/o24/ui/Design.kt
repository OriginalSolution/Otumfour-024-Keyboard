package gh.otumfour.o24.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import gh.otumfour.o24.R
import kotlin.math.min

object O24Color {
    const val INK = "#12151C"
    const val BG = "#07080C"
    const val CARD = "#12151C"
    const val GOLD = "#F0C94A"
    const val GOLD_SOFT = "#FFB703"
    const val CREAM = "#F4F6FB"
    const val MUTED = "#949BAB"
    const val GREEN = "#3DDC97"
    const val RED = "#E11D2E"
}

fun o24Color(hex: String): Int = Color.parseColor(hex)

enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

fun Context.widthClass(): WidthClass {
    val width = resources.configuration.screenWidthDp
    return when {
        width >= 840 -> WidthClass.EXPANDED
        width >= 600 -> WidthClass.MEDIUM
        else -> WidthClass.COMPACT
    }
}

/** Full-bleed kente, dimmed, with content centred to the size-class max width. */
class PatternFrame(context: Context) : FrameLayout(context) {
    init {
        setBackgroundColor(o24Color(O24Color.BG))
        addView(KenteBackdrop(context), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(View(context).apply { setBackgroundColor(Color.parseColor("#D0070605")) }, LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        ))
    }
}

class CenterFrame(context: Context) : FrameLayout(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec)
        val max = resources.getDimensionPixelSize(R.dimen.content_max_width)
        val childWidth = min(available, max)
        val childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY)
        var contentHeight = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)
            val childHeightSpec = when {
                child.layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT && heightMode != MeasureSpec.UNSPECIFIED ->
                    MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
                heightMode == MeasureSpec.EXACTLY ->
                    MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST)
                else -> heightMeasureSpec
            }
            child.measure(childWidthSpec, childHeightSpec)
            contentHeight = maxOf(contentHeight, child.measuredHeight)
        }
        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> min(MeasureSpec.getSize(heightMeasureSpec), contentHeight)
            else -> contentHeight
        }
        setMeasuredDimension(available, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val max = resources.getDimensionPixelSize(R.dimen.content_max_width)
        val childWidth = min(width, max)
        val x = (width - childWidth) / 2
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childHeight = if (child.layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT) height else child.measuredHeight
            child.layout(x, 0, x + childWidth, childHeight)
        }
    }
}

fun kenteRule(context: Context): View {
    val density = context.resources.displayMetrics.density
    val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (6 * density).toInt())
    }
    listOf(O24Color.GOLD, "#111111", O24Color.GREEN, O24Color.RED, O24Color.GOLD).forEach { hex ->
        row.addView(View(context).apply { setBackgroundColor(o24Color(hex)) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
    }
    return row
}

fun o24Card(context: Context, block: LinearLayout.() -> Unit): LinearLayout {
    val density = context.resources.displayMetrics.density
    val card = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            cornerRadius = 16f * density
            setColor(o24Color(O24Color.CARD))
            setStroke((1.2f * density).toInt().coerceAtLeast(1), o24Color(O24Color.GOLD))
        }
        val pad = (14 * density).toInt()
        setPadding(pad, pad, pad, pad)
        block()
    }
    return LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            cornerRadius = 18f * density
            setColor(o24Color(O24Color.CARD))
        }
        clipToOutline = true
        addView(kenteRule(context))
        addView(card)
    }
}

fun sectionLabel(context: Context, text: String): TextView = TextView(context).apply {
    this.text = text
    setTextColor(o24Color(O24Color.GOLD_SOFT))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    letterSpacing = 0.06f
    val density = resources.displayMetrics.density
    setPadding(0, (12 * density).toInt(), 0, (6 * density).toInt())
}

fun bodyText(context: Context, text: String): TextView = TextView(context).apply {
    this.text = text
    setTextColor(o24Color(O24Color.CREAM))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    setLineSpacing(4f, 1.15f)
}

fun mutedText(context: Context, text: String): TextView = TextView(context).apply {
    this.text = text
    setTextColor(o24Color(O24Color.MUTED))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    setLineSpacing(2f, 1.1f)
}

fun titleText(context: Context, text: String): TextView = TextView(context).apply {
    this.text = text
    setTextColor(o24Color(O24Color.GOLD_SOFT))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
    typeface = Typeface.create("serif", Typeface.BOLD)
    gravity = Gravity.CENTER_HORIZONTAL
}

fun switchTint(switch: android.widget.Switch) {
    val gold = o24Color(O24Color.GOLD)
    val ink = o24Color(O24Color.INK)
    switch.thumbTintList = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(gold, o24Color("#CDBF98")),
    )
    switch.trackTintList = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(Color.parseColor("#665C3A"), Color.parseColor("#3A3428")),
    )
    switch.setTextColor(ink)
}

fun marginBelow(view: View, dp: Int) {
    val px = (dp * view.resources.displayMetrics.density).toInt()
    val params = (view.layoutParams as? LinearLayout.LayoutParams)
        ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    params.bottomMargin = px
    params.width = LinearLayout.LayoutParams.MATCH_PARENT
    view.layoutParams = params
}
