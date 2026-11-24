package gh.otumfour.o24.ime

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import gh.otumfour.o24.data.ClipboardHistory
import gh.otumfour.o24.theme.ThemeColors

/**
 * Toolbar, menu, emoji, GIF, stickers, and emoticons.
 * Drawn in Otumfour colours. No outside keyboard branding.
 */
class BoardController(
    private val context: Context,
    private val bridge: Bridge,
) {
    interface Bridge {
        fun commit(text: String)
        fun backspace()
        fun colors(): ThemeColors
        fun openSettings()
        fun openThemes(anchor: View)
        fun openAi()
        fun openTranslate()
        fun openMic()
        fun shareSelection()
        fun edit(command: String)
        fun setHand(hand: String)
        fun setScale(scale: Float)
        fun currentScale(): Float
        fun toolbarOrder(): List<ToolbarItem>
        fun saveToolbar(order: List<ToolbarItem>)
        fun clips(): ClipboardHistory
        fun recentEmoji(): List<String>
        fun rememberEmoji(emoji: String)
        fun gifEnabled(): Boolean
        fun loadGifs(query: String)
        fun insertGif(hit: GifHit)
        fun insertSticker(id: String)
        fun showStatus(message: String)
        fun keysVisible(visible: Boolean)
        fun languageLabel(): String
        fun cycleLanguage()
        fun showLanguages(anchor: View)
    }

    var searching = false
        private set
    private var searchKind = ""
    private var searchText = ""
    private var menuPage = 0
    private var emojiGroup = 0
    private val order = ArrayList<ToolbarItem>()

    lateinit var toolbar: LinearLayout
    lateinit var recents: LinearLayout
    lateinit var panel: LinearLayout
    lateinit var queryView: TextView

    fun attach(column: LinearLayout) {
        queryView = TextView(context).apply {
            setTextColor(Color.parseColor("#F4F6FB"))
            textSize = 16f
            setPadding(dp(12), dp(8), dp(12), dp(4))
            visibility = View.GONE
        }
        column.addView(queryView, match())
        recents = row()
        column.addView(scroll(recents).apply { visibility = View.GONE }, match())
        toolbar = row()
        column.addView(scroll(toolbar), match())
        panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(8), dp(4), dp(8), dp(8))
        }
        column.addView(panel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220)))
        rebuildToolbar()
    }

    fun rebuildToolbar() {
        if (!::toolbar.isInitialized) return
        order.clear()
        order.addAll(bridge.toolbarOrder().filter { it != ToolbarItem.GIF || bridge.gifEnabled() })
        toolbar.removeAllViews()
        toolbar.addView(iconButton(bridge.languageLabel()) {
            bridge.cycleLanguage()
            rebuildToolbar()
        }.apply {
            setOnLongClickListener {
                bridge.showLanguages(this)
                true
            }
        }, gap())
        order.forEach { item ->
            toolbar.addView(iconButton(item.label) { onToolbar(item) }, gap())
        }
    }

    fun showRecents(show: Boolean) {
        if (!::recents.isInitialized) return
        recents.removeAllViews()
        if (!show) {
            (recents.parent as? View)?.visibility = View.GONE
            toolbar.visibility = View.VISIBLE
            return
        }
        bridge.recentEmoji().forEach { emoji ->
            recents.addView(iconButton(emoji) {
                bridge.commit(emoji)
                bridge.rememberEmoji(emoji)
            }, gap())
        }
        (recents.parent as? View)?.visibility = View.VISIBLE
        toolbar.visibility = View.GONE
    }

    fun acceptSearch(text: String) {
        if (!searching) return
        searchText = if (text == "\b") searchText.dropLast(1) else searchText + text
        queryView.text = searchText.ifEmpty { "Search" }
        if (searchKind == "gif") bridge.loadGifs(searchText)
        if (searchKind == "emoji") showEmoji()
    }

    fun open(name: String) {
        when (name) {
            "emoji" -> showEmoji()
            "gif" -> showGif()
            "stickers" -> showStickers()
            "menu" -> showMenu()
            else -> closePanel()
        }
    }

    fun closeSearch() {
        searching = false
        searchText = ""
        queryView.visibility = View.GONE
        bridge.keysVisible(false)
    }

    private fun onToolbar(item: ToolbarItem) {
        when (item) {
            ToolbarItem.GRID -> showMenu()
            ToolbarItem.STICKERS -> showStickers()
            ToolbarItem.GIF -> showGif()
            ToolbarItem.SETTINGS -> bridge.openSettings()
            ToolbarItem.TRANSLATE -> bridge.openTranslate()
            ToolbarItem.THEME -> bridge.openThemes(toolbar)
            ToolbarItem.MIC -> bridge.openMic()
            ToolbarItem.AI -> bridge.openAi()
        }
    }

    private fun showMenu() {
        openPanel()
        panel.removeAllViews()
        val title = row()
        title.addView(label(if (menuPage == 0) "Tools" else "More tools"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        title.addView(iconButton("Edit") { showToolbarEditor() })
        panel.addView(title)
        val items = if (menuPage == 0) {
            listOf("Clipboard", "Emoji", "One-handed", "Text editing")
        } else {
            listOf("Share", "Resize")
        }
        items.chunked(2).forEach { pair ->
            val line = row()
            pair.forEach { name ->
                line.addView(pill(name) { openTool(name) }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginEnd = dp(8) })
            }
            if (pair.size == 1) line.addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))
            panel.addView(line)
        }
        val pages = row()
        pages.addView(pill(if (menuPage == 0) "Next" else "Back") {
            menuPage = if (menuPage == 0) 1 else 0
            showMenu()
        }, gap())
        pages.addView(pill("ABC") { closePanel() }, gap())
        panel.addView(pages)
    }

    private fun openTool(name: String) {
        when (name) {
            "Clipboard" -> showClipboard()
            "Emoji" -> showEmoji()
            "One-handed" -> showHands()
            "Text editing" -> showEditing()
            "Share" -> bridge.shareSelection()
            "Resize" -> showResize()
        }
    }

    private fun showClipboard() {
        openPanel()
        panel.removeAllViews()
        panel.addView(header("Clipboard", "Kept on this phone"))
        val clips = bridge.clips().all()
        if (clips.isEmpty()) panel.addView(label("Nothing copied yet."))
        val scroller = ScrollView(context)
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        clips.forEach { clip ->
            val line = row()
            line.addView(pill(clip.text.take(42)) {
                bridge.commit(clip.text)
                closePanel()
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            line.addView(iconButton(if (clip.pinned) "Unpin" else "Pin") {
                bridge.clips().pin(clip.text, !clip.pinned)
                showClipboard()
            })
            list.addView(line)
        }
        scroller.addView(list)
        panel.addView(scroller, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        panel.addView(pill("ABC") { closePanel() })
    }

    private fun showEmoji() {
        openPanel()
        panel.removeAllViews()
        val chips = row()
        EmojiCatalog.groups.forEachIndexed { index, group ->
            chips.addView(pill(group.name) {
                emojiGroup = index
                showEmoji()
            }, gap())
        }
        panel.addView(scroll(chips))
        panel.addView(pill("Search") { beginSearch("emoji") })
        val group = EmojiCatalog.groups[emojiGroup]
        val source = if (searching && searchKind == "emoji" && searchText.isNotEmpty()) {
            EmojiCatalog.search(searchText)
        } else {
            group.emoji
        }
        val grid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        source.chunked(8).forEach { line ->
            val row = row()
            line.forEach { emoji ->
                row.addView(iconButton(emoji) {
                    bridge.commit(emoji)
                    bridge.rememberEmoji(emoji)
                }.apply {
                    setOnLongClickListener {
                        if (group.toneable) showTones(emoji)
                        true
                    }
                }, gap())
            }
            grid.addView(row)
        }
        panel.addView(ScrollView(context).apply { addView(grid) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        panel.addView(bottomBar())
    }

    private fun showTones(emoji: String) {
        val row = row()
        EmojiCatalog.tones.forEach { tone ->
            val toned = EmojiCatalog.withTone(emoji, tone)
            row.addView(iconButton(toned) {
                bridge.commit(toned)
                bridge.rememberEmoji(toned)
            }, gap())
        }
        panel.addView(row, 1)
    }

    private fun showGif() {
        openPanel()
        panel.removeAllViews()
        if (!bridge.gifEnabled()) {
            panel.addView(header("GIFs", "Needs the TENOR_API_KEY secret on Otumfour’s server."))
            panel.addView(pill("ABC") { closePanel() })
            bridge.showStatus("GIFs need the TENOR_API_KEY secret on the server.")
            return
        }
        panel.addView(header("GIFs", searchText.ifEmpty { "Featured" }))
        val chips = row()
        listOf("Hello", "Thanks", "Celebrate", "Dance", "Love").forEach { name ->
            chips.addView(pill(name) {
                searchText = name
                bridge.loadGifs(name)
            }, gap())
        }
        chips.addView(pill("Search") { beginSearch("gif") }, gap())
        panel.addView(scroll(chips))
        panel.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            tag = "gif-grid"
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        panel.addView(bottomBar())
        bridge.loadGifs(searchText)
    }

    fun showGifHits(hits: List<GifHit>) {
        val grid = panel.findViewWithTag<LinearLayout>("gif-grid") ?: return
        grid.removeAllViews()
        if (hits.isEmpty()) {
            grid.addView(label("No GIFs."))
            return
        }
        val scroll = ScrollView(context)
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        hits.chunked(2).forEach { pair ->
            val line = row()
            pair.forEach { hit ->
                line.addView(pill(hit.title.ifBlank { "GIF" }.take(18)) { bridge.insertGif(hit) }, LinearLayout.LayoutParams(0, dp(64), 1f).apply { marginEnd = dp(6) })
            }
            column.addView(line)
        }
        scroll.addView(column)
        grid.addView(scroll)
    }

    private fun showStickers() {
        openPanel()
        panel.removeAllViews()
        panel.addView(header("Stickers", "Original Kente and Adinkra drawings"))
        val scroll = ScrollView(context)
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        StickerArt.packs().forEach { pack ->
            column.addView(label(pack))
            val line = row()
            StickerArt.all.filter { it.pack == pack }.forEach { sticker ->
                val image = ImageView(context).apply {
                    setImageBitmap(StickerArt.bitmap(sticker.id, dp(72)))
                    contentDescription = sticker.title
                    setOnClickListener { bridge.insertSticker(sticker.id) }
                }
                line.addView(image, LinearLayout.LayoutParams(dp(76), dp(76)).apply { marginEnd = dp(6) })
            }
            column.addView(scrollWrap(line))
        }
        scroll.addView(column)
        panel.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        panel.addView(bottomBar())
    }

    private fun showEmoticons() {
        openPanel()
        panel.removeAllViews()
        val scroll = ScrollView(context)
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        Emoticons.groups.forEach { group ->
            column.addView(label(group.name))
            val line = row()
            group.marks.forEach { mark ->
                line.addView(pill(mark) { bridge.commit(mark) }, gap())
            }
            column.addView(scrollWrap(line))
        }
        scroll.addView(column)
        panel.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        panel.addView(bottomBar())
    }

    private fun showHands() {
        openPanel()
        panel.removeAllViews()
        panel.addView(header("One-handed", "Keeps the keys on one side"))
        val line = row()
        listOf("Left" to "left", "Full" to "center", "Right" to "right").forEach { (name, value) ->
            line.addView(pill(name) {
                bridge.setHand(value)
            }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) })
        }
        panel.addView(line)
        panel.addView(pill("ABC") { closePanel() })
    }

    private fun showEditing() {
        openPanel()
        panel.removeAllViews()
        panel.addView(header("Text editing", "Move the cursor"))
        val arrows = row()
        listOf("←" to "left", "→" to "right", "Select" to "select", "All" to "all").forEach { (name, command) ->
            arrows.addView(pill(name) { bridge.edit(command) }, gap())
        }
        panel.addView(arrows)
        val actions = row()
        listOf("Copy" to "copy", "Paste" to "paste").forEach { (name, command) ->
            actions.addView(pill(name) { bridge.edit(command) }, gap())
        }
        panel.addView(actions)
        panel.addView(pill("ABC") { closePanel() })
    }

    private fun showResize() {
        openPanel()
        panel.removeAllViews()
        panel.addView(header("Resize", "Drag is a step up or down"))
        val line = row()
        line.addView(pill("Shorter") { bridge.setScale(bridge.currentScale() - 0.08f) }, gap())
        line.addView(pill("Taller") { bridge.setScale(bridge.currentScale() + 0.08f) }, gap())
        panel.addView(line)
        panel.addView(label("Height ${((bridge.currentScale()) * 100).toInt()}%"))
        panel.addView(pill("ABC") { closePanel() })
    }

    private fun showToolbarEditor() {
        openPanel()
        panel.removeAllViews()
        panel.addView(header("Toolbar", "Move a button"))
        order.forEachIndexed { index, item ->
            val line = row()
            line.addView(label(item.label), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            line.addView(iconButton("Up") {
                if (index > 0) {
                    val swapped = order.toMutableList()
                    val moved = swapped.removeAt(index)
                    swapped.add(index - 1, moved)
                    bridge.saveToolbar(swapped)
                    order.clear()
                    order.addAll(swapped)
                    showToolbarEditor()
                }
            })
            panel.addView(line)
        }
        panel.addView(pill("Done") {
            rebuildToolbar()
            closePanel()
        })
    }

    private fun bottomBar(): View {
        val bar = row()
        bar.addView(pill("ABC") { closePanel() }, gap())
        bar.addView(pill("Emoji") { showEmoji() }, gap())
        if (bridge.gifEnabled()) bar.addView(pill("GIF") { showGif() }, gap())
        bar.addView(pill("Stickers") { showStickers() }, gap())
        bar.addView(pill(":-)") { showEmoticons() }, gap())
        bar.addView(pill("⌫") { bridge.backspace() }, gap())
        return scroll(bar)
    }

    private fun beginSearch(kind: String) {
        searching = true
        searchKind = kind
        searchText = ""
        queryView.visibility = View.VISIBLE
        queryView.text = "Search"
        bridge.keysVisible(true)
        bridge.showStatus("Type to search, then tap ABC on the keys to close.")
    }

    private fun openPanel() {
        panel.visibility = View.VISIBLE
        bridge.keysVisible(false)
    }

    fun closePanel() {
        searching = false
        queryView.visibility = View.GONE
        panel.visibility = View.GONE
        bridge.keysVisible(true)
    }

    private fun header(title: String, body: String): View {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        box.addView(label(title).apply { textSize = 16f; setTypeface(typeface, Typeface.BOLD) })
        box.addView(label(body))
        return box
    }

    private fun iconButton(text: String, onClick: () -> Unit): Button = Button(context).apply {
        this.text = text
        isAllCaps = false
        textSize = 12f
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(dp(10), dp(6), dp(10), dp(6))
        setTextColor(bridge.colors().pillText)
        background = chip(bridge.colors().pillFill)
        setOnClickListener { onClick() }
        contentDescription = text
    }

    private fun pill(text: String, onClick: () -> Unit): Button = iconButton(text, onClick).apply {
        textSize = 14f
    }

    private fun label(text: String) = TextView(context).apply {
        this.text = text
        setTextColor(Color.parseColor("#F4F6FB"))
        textSize = 14f
        setPadding(dp(4), dp(4), dp(4), dp(4))
    }

    private fun chip(color: Int) = GradientDrawable().apply {
        cornerRadius = dp(18).toFloat()
        setColor(color)
    }

    private fun row() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun scroll(child: View) = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        addView(child)
    }

    private fun scrollWrap(child: View) = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        addView(child)
    }

    private fun match() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    private fun gap() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(6) }
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
}
