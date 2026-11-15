package gh.otumfour.o24.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import gh.otumfour.o24.R
import gh.otumfour.o24.data.SettingsStore
import gh.otumfour.o24.theme.ThemeId
import kotlin.math.abs

/**
 * One theme at a time, with neighbours peeking. Every theme, including Golden Stool Royal,
 * stays in the list so it can be chosen again.
 */
class ThemeCarouselView(context: Context) : LinearLayout(context) {
    private val store = SettingsStore(context)
    private val pager = ViewPager2(context)
    private val name = TextView(context)
    private val history = TextView(context)
    private val dots = PageDotsView(context)
    private val apply = Button(context)
    private val swatches = LinearLayout(context)
    var onReadHistory: (ThemeId) -> Unit = {}

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.TRANSPARENT)
        addView(kenteRule(context))
        name.setTextColor(Color.parseColor("#F3D37A"))
        name.textSize = 22f
        name.typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
        name.gravity = Gravity.CENTER
        name.setPadding(dp(16), dp(10), dp(16), dp(4))
        addView(name, matchWrap())

        val pad = dp(18)
        pager.adapter = CardAdapter()
        pager.offscreenPageLimit = 2
        pager.clipToPadding = false
        pager.clipChildren = false
        pager.setPadding(pad, dp(4), pad, dp(4))
        pager.setPageTransformer { page, position ->
            val scale = 0.92f + (1f - abs(position).coerceAtMost(1f)) * 0.08f
            page.scaleY = scale
            page.alpha = 0.72f + (1f - abs(position).coerceAtMost(1f)) * 0.28f
        }
        addView(pager, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        dots.count = ThemeId.entries.size
        addView(dots, matchWrap())

        swatches.orientation = HORIZONTAL
        swatches.gravity = Gravity.CENTER
        ThemeId.entries.forEachIndexed { index, theme ->
            val dot = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(swatch(theme))
                    setStroke(dp(1), Color.parseColor("#E8B52D"))
                }
                setOnClickListener { pager.setCurrentItem(index, true) }
            }
            swatches.addView(dot, LayoutParams(dp(18), dp(18)).apply { marginStart = dp(4); marginEnd = dp(4) })
        }
        addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(12), dp(2), dp(12), dp(2))
            addView(swatches)
        }, matchWrap())

        history.setTextColor(Color.parseColor("#F6F1E4"))
        history.textSize = 14f
        history.setLineSpacing(dp(2).toFloat(), 1.12f)
        val historyCard = o24Card(context) {
            addView(history)
            val read = textButton(context.getString(R.string.learn_more))
            read.setOnClickListener { onReadHistory(current()) }
            addView(read)
        }
        val historyScroll = ScrollView(context).apply {
            addView(historyCard)
            isVerticalScrollBarEnabled = true
        }
        addView(historyScroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(132)).apply {
            marginStart = dp(16)
            marginEnd = dp(16)
            topMargin = dp(4)
        })

        styleGold(apply)
        apply.setOnClickListener {
            store.update { it.copy(theme = current()) }
            bind(pager.currentItem)
        }
        addView(apply, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            marginStart = dp(20)
            marginEnd = dp(20)
            topMargin = dp(8)
            bottomMargin = dp(12)
        })

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = bind(position)
        })
        val start = ThemeId.entries.indexOf(store.get().theme).coerceAtLeast(0)
        pager.setCurrentItem(start, false)
        bind(start)
    }

    fun refresh() = bind(pager.currentItem)

    fun showTheme(index: Int) {
        val safe = index.coerceIn(0, ThemeId.entries.lastIndex)
        pager.setCurrentItem(safe, false)
        bind(safe)
    }

    private fun current(): ThemeId = ThemeId.entries[pager.currentItem]

    private fun bind(position: Int) {
        val theme = ThemeId.entries[position]
        name.text = context.getString(theme.titleRes)
        history.text = context.getString(theme.historyRes) + "\n\n" + context.getString(R.string.history_disclaimer)
        dots.selected = position
        val active = store.get().theme == theme
        apply.text = if (active) "✓  " + context.getString(R.string.theme_in_use) else context.getString(R.string.use_theme)
        for (i in 0 until swatches.childCount) {
            swatches.getChildAt(i).alpha = if (i == position) 1f else 0.45f
        }
    }

    private class CardAdapter : RecyclerView.Adapter<CardAdapter.Holder>() {
        class Holder(val preview: ThemePreviewView) : RecyclerView.ViewHolder(preview.parent as View)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val frame = FrameLayout(parent.context)
            frame.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            frame.background = GradientDrawable().apply {
                cornerRadius = parent.resources.displayMetrics.density * 18f
                setStroke((2 * parent.resources.displayMetrics.density).toInt(), Color.parseColor("#E8B52D"))
                setColor(Color.parseColor("#100E0A"))
            }
            val preview = ThemePreviewView(parent.context)
            val margin = (10 * parent.resources.displayMetrics.density).toInt()
            frame.addView(preview, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ).apply { setMargins(margin, margin, margin, margin) })
            return Holder(preview)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.preview.theme = ThemeId.entries[position]
        }

        override fun getItemCount(): Int = ThemeId.entries.size
    }

    private fun swatch(theme: ThemeId): Int = when (theme) {
        ThemeId.GOLDEN_STOOL -> Color.parseColor("#E8B52D")
        ThemeId.KENTE -> Color.parseColor("#0E7A3A")
        ThemeId.ADINKRA -> Color.parseColor("#3A3A3A")
        ThemeId.ASANTE_FLAG -> Color.parseColor("#111111")
        ThemeId.SIKA_DWA -> Color.parseColor("#E0B83A")
        ThemeId.ROYAL_CREST -> Color.parseColor("#0E6B32")
        ThemeId.OKOMFO_ANOKYE -> Color.parseColor("#6A3A28")
        ThemeId.YAA_ASANTEWAA -> Color.parseColor("#3A2060")
        ThemeId.KOTOKO -> Color.parseColor("#8E1A2B")
        ThemeId.ASANTEMAN_JERSEY -> Color.parseColor("#F2C200")
    }

    private fun textButton(label: String) = Button(context).apply {
        text = label
        isAllCaps = false
        setTextColor(Color.parseColor("#E8B52D"))
        background = null
    }

    private fun styleGold(button: Button) {
        button.isAllCaps = false
        button.setTextColor(Color.parseColor("#1A1408"))
        button.backgroundTintList = null
        button.background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(Color.parseColor("#E8B52D"))
        }
    }

    private fun matchWrap() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
