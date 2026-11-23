package gh.otumfour.o24.ime

import android.app.ActivityOptions
import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import gh.otumfour.o24.ai.AiClient
import gh.otumfour.o24.ai.AiResult
import gh.otumfour.o24.data.LearnedWords
import gh.otumfour.o24.data.O24Settings
import gh.otumfour.o24.data.SettingsStore
import gh.otumfour.o24.lang.KeyboardLanguage
import gh.otumfour.o24.suggest.Autocorrect
import gh.otumfour.o24.suggest.Lexicon
import gh.otumfour.o24.suggest.Suggestion
import gh.otumfour.o24.suggest.SwipeDecoder
import gh.otumfour.o24.suggest.SwipeKey
import gh.otumfour.o24.suggest.SwipeSample
import gh.otumfour.o24.suggest.TypingEdits
import gh.otumfour.o24.theme.PhotoBackdrop
import gh.otumfour.o24.theme.ThemeId
import gh.otumfour.o24.theme.colorsFor
import gh.otumfour.o24.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class O24InputMethodService : InputMethodService(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var store: SettingsStore
    private lateinit var learned: LearnedWords

    private val lexicons = HashMap<String, Lexicon>()
    private var settings: O24Settings = O24Settings(
        theme = ThemeId.OTUMFUO,
        language = KeyboardLanguage.ENGLISH,
        enabledLanguages = KeyboardLanguage.entries.toList(),
        autocorrect = true,
        autoCapitalise = true,
        haptic = true,
        keySound = false,
        aiEndpoint = "",
        aiModel = "",
        aiKey = "",
        aiEnabled = true,
        swipeTyping = true,
    )

    private var page = BoardPage.LETTERS
    private var shift = ShiftState.OFF
    private var enterLabel = "Enter"
    private var suppressSuggest = false
    private var ranked: List<Suggestion> = emptyList()
    private var shown: List<Suggestion?> = listOf(null, null, null)
    private var root: LinearLayout? = null
    private var keyboard: KeyboardView? = null
    private var suggestionViews: List<TextView> = emptyList()
    private var statusView: TextView? = null
    private var hairline: View? = null
    private var toneRow: LinearLayout? = null
    private var languageButton: Button? = null
    private var popup: PopupWindow? = null
    private var themeDialog: Dialog? = null
    private var undo: Undo? = null
    private var swipeCommitted: String? = null
    private val rejected = HashSet<String>()
    private var lastChar = ""
    private var lastCharAt = 0L

    override fun onCreate() {
        super.onCreate()
        try {
            store = SettingsStore(this)
            learned = LearnedWords(this)
            settings = store.get()
            window?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(colorsFor(settings.theme).keyboardBg))
            getSharedPreferences(SettingsStore.NAME, MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(this)
        } catch (error: Throwable) {
            rememberFailure("onCreate", error)
        }
    }

    override fun onDestroy() {
        getSharedPreferences(SettingsStore.NAME, MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(this)
        scope.cancel()
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        settings = store.get()
        reloadKeyboard()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        try {
            super.onStartInput(attribute, restarting)
        } catch (error: Throwable) {
            rememberFailure("onStartInput", error)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        try {
            if (::store.isInitialized) settings = store.get()
            enterLabel = enterLabel(info)
            suppressSuggest = isPassword(info) || isNumber(info)
            page = if (isNumber(info)) BoardPage.SYMBOLS else BoardPage.LETTERS
            shift = if (wantsCaps(info)) ShiftState.ONCE else ShiftState.OFF
            undo = null
            rejected.clear()
            reloadKeyboard()
            refreshSuggestions()
        } catch (error: Throwable) {
            rememberFailure("onStartInputView", error)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        popup?.dismiss()
        super.onFinishInputView(finishingInput)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        keyboard?.requestLayout()
    }

    override fun onComputeInsets(outInsets: Insets) {
        try {
            super.onComputeInsets(outInsets)
            val view = root ?: return
            if (view.height <= 0) return
            val location = IntArray(2)
            view.getLocationInWindow(location)
            val windowHeight = window?.window?.decorView?.height?.takeIf { it > 0 } ?: view.height
            val top = location[1].coerceIn(0, windowHeight)
            outInsets.contentTopInsets = top
            outInsets.visibleTopInsets = top
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
        } catch (error: RuntimeException) {
            Log.e(TAG, "onComputeInsets failed during a resize", error)
        }
    }

    override fun onCreateInputView(): View {
        return try {
            createKeyboard()
        } catch (error: Throwable) {
            rememberFailure("onCreateInputView", error)
            fallbackKeyboard()
        }
    }

    private fun createKeyboard(): View {
        if (::store.isInitialized) settings = store.get()
        val colors = colorsFor(settings.theme)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colors.keyboardBg)
        }
        root = column
        hairline = View(this).apply {
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0x00000000, 0xFFE11D2E.toInt(), 0xFFFFB703.toInt(), 0xFFE11D2E.toInt(), 0x00000000),
            )
            visibility = if (colors.modern) View.VISIBLE else View.GONE
        }
        column.addView(hairline, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2)))

        statusView = TextView(this).apply {
            setTextColor(colors.suggestionText)
            textSize = 13f
            setPadding(dp(12), dp(6), dp(12), dp(2))
            visibility = View.GONE
        }
        column.addView(statusView, matchWrap())

        val suggestions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(colors.suggestionBg)
            layoutParams = matchWrap()
        }
        suggestionViews = List(3) { index ->
            TextView(this).apply {
                gravity = Gravity.CENTER
                textSize = 16f
                setTextColor(colors.suggestionText)
                setPadding(dp(6), dp(10), dp(6), dp(10))
                setOnClickListener { applyShown(index) }
            }.also { suggestions.addView(it, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)) }
        }
        column.addView(suggestions)

        toneRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }
        column.addView(
            HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(toneRow)
                visibility = View.GONE
                tag = "ai-choices"
            },
            matchWrap(),
        )

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
            setBackgroundColor(colors.toolbarBg)
        }
        languageButton = pill("EN").also { button ->
            button.setOnClickListener { cycleLanguage() }
            button.setOnLongClickListener {
                showLanguagePopup(button)
                true
            }
            toolbar.addView(button)
        }
        toolbar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        toolbar.addView(pill("AI").also { it.setOnClickListener { onAi() } }, marginStart())
        toolbar.addView(pill("Translate").also { it.setOnClickListener { onTranslate() } }, marginStart())
        toolbar.addView(pill("⚙").also { button ->
            button.setOnClickListener { showThemePicker(button) }
            button.setOnLongClickListener {
                openSettings()
                true
            }
        }, marginStart())
        column.addView(
            HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(toolbar)
            },
            matchWrap(),
        )

        keyboard = KeyboardView(this).also { board ->
        board.onKey = { key -> onKey(key) }
        board.onAlternate = { key -> onAlternate(key) }
        board.onSwipe = { samples, keys, width -> onSwipe(samples, keys, width) }
        board.hapticEnabled = settings.haptic
            column.addView(board, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        reloadKeyboard()
        return column
    }

    /** A plain board used only if the themed keyboard cannot be built. The service stays up. */
    private fun fallbackKeyboard(): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorsFor(ThemeId.GOLDEN_STOOL).keyboardBg)
        }
        root = column
        val board = KeyboardView(this).apply {
            theme = ThemeId.GOLDEN_STOOL
            colors = colorsFor(ThemeId.GOLDEN_STOOL)
            rows = KeyboardModel.rows(BoardPage.LETTERS, KeyboardLanguage.ENGLISH, ShiftState.OFF, "Enter", "Otumfour")
            onKey = { key -> onKey(key) }
        }
        keyboard = board
        column.addView(board, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        return column
    }

    private fun rememberFailure(where: String, error: Throwable) {
        Log.e(TAG, "$where failed", error)
        runCatching { gh.otumfour.o24.CrashReport.save(this, error) }
    }

    private fun reloadKeyboard() {
        val board = keyboard ?: return
        val colors = colorsFor(settings.theme)
        board.theme = settings.theme
        board.colors = colors
        board.hapticEnabled = settings.haptic
        board.soundEnabled = settings.keySound
        board.swipeEnabled = settings.swipeTyping && !suppressSuggest
        board.rows = KeyboardModel.rows(page, settings.language, shift, enterLabel, colors.spaceTitle)
        val photo = colors.photoAsset
        if (photo != null) root?.background = PhotoBackdrop.drawable(this, photo)
        else root?.setBackgroundColor(colors.keyboardBg)
        hairline?.visibility = if (colors.modern) View.VISIBLE else View.GONE
        suggestionViews.forEachIndexed { index, view ->
            styleChip(view, index == 0 && view.text.isNotEmpty())
        }
        val strip = if (photo != null) android.graphics.Color.TRANSPARENT else colors.suggestionBg
        (suggestionViews.firstOrNull()?.parent as? View)?.setBackgroundColor(strip)
        statusView?.setTextColor(colors.suggestionText)
        styleToolbar()
        languageButton?.text = settings.language.shortLabel
    }

    private fun styleToolbar() {
        val colors = colorsFor(settings.theme)
        languageButton?.let { stylePill(it, colors.pillFill, colors.pillText, glow = false) }
        val parent = languageButton?.parent as? LinearLayout ?: return
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is Button && child !== languageButton) {
                stylePill(child, colors.actionFill, colors.actionLabel, glow = false)
            }
        }
        parent.setBackgroundColor(if (colors.photoAsset != null) Color.TRANSPARENT else colors.toolbarBg)
    }

    private fun onKey(key: KeySpec) {
        val ic = currentInputConnection ?: return
        when (key.action) {
            KeyAction.CHAR -> commitChar(ic, key.value)
            KeyAction.SHIFT -> {
                if (page != BoardPage.LETTERS) return
                shift = when (shift) {
                    ShiftState.OFF -> ShiftState.ONCE
                    ShiftState.ONCE -> ShiftState.CAPS
                    ShiftState.CAPS -> ShiftState.OFF
                }
                reloadKeyboard()
            }
            KeyAction.BACKSPACE -> {
                if (!revertCorrection(ic)) deleteBackward(ic)
                refreshSuggestions()
            }
            KeyAction.ENTER -> {
                commitEnter(ic)
                refreshSuggestions()
            }
            KeyAction.SPACE -> commitSpace(ic)
            KeyAction.MODE -> {
                page = when (page) {
                    BoardPage.LETTERS -> BoardPage.SYMBOLS
                    BoardPage.SYMBOLS, BoardPage.EMOJI -> BoardPage.LETTERS
                }
                reloadKeyboard()
            }
            KeyAction.EMOJI -> {
                page = if (page == BoardPage.EMOJI) BoardPage.LETTERS else BoardPage.EMOJI
                reloadKeyboard()
            }
            KeyAction.LANGUAGE -> cycleLanguage()
        }
    }

    private fun onAlternate(key: KeySpec) {
        val ic = currentInputConnection ?: return
        when (key.alternate) {
            "lang" -> cycleLanguage()
            null -> Unit
            else -> {
                ic.commitText(key.alternate, 1)
                if (shift == ShiftState.ONCE) shift = ShiftState.OFF
                reloadKeyboard()
                refreshSuggestions()
            }
        }
    }

    private fun commitChar(ic: InputConnection, value: String) {
        if (value.isEmpty()) return
        val now = android.os.SystemClock.uptimeMillis()
        if (value == lastChar && now - lastCharAt < 40L && value.length == 1 && value[0].isLetter()) return
        lastChar = value
        lastCharAt = now
        undo = null
        swipeCommitted = null
        if (value.length == 1 && value[0] in ".!?,") {
            correctCurrentWord(ic, trailing = value)
            if (settings.autoCapitalise && value[0] in ".!?") shift = ShiftState.ONCE
        } else {
            ic.commitText(value, 1)
            if (shift == ShiftState.ONCE && value.any { it.isLetter() }) shift = ShiftState.OFF
        }
        reloadKeyboard()
        refreshSuggestions()
    }

    private fun commitSpace(ic: InputConnection) {
        val before = ic.getTextBeforeCursor(8, 0)?.toString().orEmpty()
        if (settings.autoCapitalise && TypingEdits.doubleSpace(before)) {
            ic.deleteSurroundingText(1, 0)
            ic.commitText(". ", 1)
            shift = ShiftState.ONCE
            undo = null
            swipeCommitted = null
            reloadKeyboard()
            refreshSuggestions()
            return
        }
        if (before.endsWith(" ")) return
        val word = currentWord()
        if (settings.autoCapitalise && word == "i") {
            ic.deleteSurroundingText(1, 0)
            ic.commitText("I ", 1)
            finishWord()
            return
        }
        correctCurrentWord(ic, trailing = " ")
        finishWord()
    }

    private fun correctCurrentWord(ic: InputConnection, trailing: String) {
        val word = currentWord()
        val beforeWord = ic.getTextBeforeCursor(word.length + 24, 0)?.toString().orEmpty().dropLast(word.length)
        val known = wordIsKnown(word) || TypingEdits.capitalisedName(word, beforeWord)
        val choice = if (!suppressSuggest && settings.autocorrect) {
            correction(word, known)
        } else {
            null
        }
        if (choice != null) {
            ic.deleteSurroundingText(word.length, 0)
            val written = Lexicon.matchCase(word, choice)
            ic.commitText(written + trailing, 1)
            undo = Undo(word, written)
            swipeCommitted = null
            learned.learn(settings.language.code, written)
        } else {
            if (shouldRemember(word)) learned.learn(settings.language.code, word)
            ic.commitText(trailing, 1)
            undo = null
        }
    }

    /** The suggestion bar can still be empty on the first letters. Ask the word list directly. */
    private fun correction(word: String, known: Boolean): String? {
        val live = Lexicon.autocorrect(word, ranked, known, rejected)
        if (live != null) return live
        val fresh = lexicon(settings.language).suggest(word, learned.load(settings.language.code))
        return Lexicon.autocorrect(word, fresh, known, rejected)
    }

    private fun shouldRemember(word: String): Boolean {
        if (suppressSuggest || word.length < 3 || wordIsKnown(word)) return false
        if (!settings.autocorrect) return true
        val suggestions = lexicon(settings.language).suggest(word)
        return Autocorrect.shouldLearn(word, suggestions)
    }

    private fun wordIsKnown(word: String): Boolean {
        if (word.isEmpty()) return false
        val learnedWords = learned.load(settings.language.code)
        if (word.lowercase() in learnedWords) return true
        return settings.enabledLanguages.any { lexicon(it).contains(word) }
    }

    private fun revertCorrection(ic: InputConnection): Boolean {
        val pending = undo ?: return false
        val before = ic.getTextBeforeCursor(pending.replacement.length + 1, 0)?.toString().orEmpty()
        val withSpace = pending.replacement + " "
        val count = when {
            before.endsWith(withSpace) -> withSpace.length
            before.endsWith(pending.replacement) -> pending.replacement.length
            else -> return false
        }
        ic.deleteSurroundingText(count, 0)
        ic.commitText(pending.original, 1)
        rejected.add(pending.original.lowercase())
        undo = null
        return true
    }

    private fun finishWord() {
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        ranked = emptyList()
        shown = listOf(null, null, null)
        suggestionViews.forEach { it.text = "" }
        reloadKeyboard()
    }

    private fun commitEnter(ic: InputConnection) {
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_UNSPECIFIED
        val noEnter = info?.imeOptions?.and(EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnter && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
    }

    private fun deleteBackward(ic: InputConnection) {
        val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        val count = if (before.length >= 2 && Character.isSurrogatePair(before[0], before[1]) && before.length == 2) 2 else 1
        ic.deleteSurroundingText(count.coerceAtLeast(1), 0)
    }

    private fun onAi() {
        if (suppressSuggest) {
            showStatus("Rewrite stays off on password and number fields.")
            return
        }
        val target = captureTarget()
        if (target == null) {
            showStatus("Type or select something to rewrite.")
            return
        }
        aiUnavailable()?.let {
            showAiProblem(it)
            return
        }
        showTones(target)
    }

    private fun showTones(target: EditTarget) {
        val row = toneRow ?: return
        row.removeAllViews()
        showChoices()
        listOf(
            "Improve" to "improve",
            "Formal" to "formal",
            "Casual" to "casual",
            "Shorter" to "shorter",
            "Fix spelling" to "fix",
        ).forEach { (label, style) ->
            row.addView(pill(label).also { button ->
                button.setOnClickListener {
                    hideChoices()
                    runRewrite(target, style)
                }
            }, marginStart())
        }
        showStatus("Pick a style.")
    }

    private fun showLanguages(text: String) {
        val row = toneRow ?: return
        row.removeAllViews()
        showChoices()
        KeyboardLanguage.entries.forEach { language ->
            row.addView(pill(language.shortLabel).also { button ->
                button.setOnClickListener {
                    hideChoices()
                    runTranslate(text, language.englishName)
                }
            }, marginStart())
        }
        showStatus("Translate into…")
    }

    private fun showChoices() {
        (toneRow?.parent as? View)?.visibility = View.VISIBLE
    }

    private fun hideChoices() {
        (toneRow?.parent as? View)?.visibility = View.GONE
        toneRow?.removeAllViews()
    }

    private fun runRewrite(target: EditTarget, style: String) {
        showStatus("Rewriting…")
        scope.launch {
            val result = withContext(Dispatchers.IO) { client().rewrite(target.text, style) }
            applyAi(target, result)
        }
    }

    private fun runTranslate(text: String, targetName: String) {
        val target = captureTarget() ?: return
        showStatus("Translating…")
        scope.launch {
            val result = withContext(Dispatchers.IO) { client().translate(text, targetName) }
            applyAi(target, result)
        }
    }

    private fun onTranslate() {
        if (suppressSuggest) {
            showStatus("Translate stays off on password and number fields.")
            return
        }
        val target = captureTarget()
        if (target == null) {
            showStatus("Type or select something to translate.")
            return
        }
        aiUnavailable()?.let {
            showAiProblem(it)
            return
        }
        showLanguages(target.text)
    }

    private fun applyAi(target: EditTarget, result: AiResult) {
        when (result) {
            is AiResult.Ok -> {
                replace(target, result.text)
                showStatus(result.text)
                refreshSuggestions()
            }
            is AiResult.Unavailable -> showAiProblem(result.message)
        }
    }

    private fun showAiProblem(message: String) {
        showStatus(message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun aiUnavailable(): String? {
        if (!settings.aiEnabled) return "Turn on AI in Otumfour settings to rewrite and translate."
        return null
    }

    private fun client() = AiClient(settings)

    private fun captureTarget(): EditTarget? {
        val ic = currentInputConnection ?: return null
        val selected = ic.getSelectedText(0)?.toString()
        if (!selected.isNullOrBlank()) return EditTarget(selected = true, before = 0, after = 0, text = selected)
        val before = ic.getTextBeforeCursor(400, 0)?.toString() ?: return null
        val after = ic.getTextAfterCursor(200, 0)?.toString().orEmpty()
        val start = before.lastIndexOf('\n') + 1
        val head = before.substring(start)
        val cut = after.indexOf('\n').let { if (it < 0) after.length else it }
        val tail = after.substring(0, cut)
        val text = (head + tail).trim()
        if (text.isEmpty()) return null
        return EditTarget(selected = false, before = head.length, after = tail.length, text = head + tail)
    }

    private fun replace(target: EditTarget, text: String) {
        val ic = currentInputConnection ?: return
        if (target.selected) {
            ic.commitText(text, 1)
        } else {
            ic.deleteSurroundingText(target.before, target.after)
            ic.commitText(text, 1)
        }
    }

    private fun refreshSuggestions() {
        if (suppressSuggest) {
            suggestionViews.forEach { it.text = "" }
            return
        }
        val word = currentWord()
        if (word.isEmpty()) {
            ranked = emptyList()
            shown = listOf(null, null, null)
            suggestionViews.forEach { it.text = "" }
            return
        }
        ranked = lexicon(settings.language).suggest(word, learned.load(settings.language.code))
        shown = Autocorrect.display(word, ranked).map { suggestion ->
            suggestion?.copy(word = Lexicon.matchCase(word, suggestion.word))
        }
        swipeCommitted = null
        suggestionViews.forEachIndexed { index, view ->
            val suggestion = shown.getOrNull(index)
            view.text = suggestion?.word.orEmpty()
            styleChip(view, index == 0 && suggestion != null)
        }
    }

    private fun applyShown(index: Int) {
        val suggestion = shown.getOrNull(index) ?: return
        val ic = currentInputConnection ?: return
        val word = currentWord()
        val committed = swipeCommitted
        if (word.isEmpty() && committed != null) {
            val token = "$committed "
            val before = ic.getTextBeforeCursor(token.length, 0)?.toString().orEmpty()
            if (before == token) {
                ic.deleteSurroundingText(token.length, 0)
                ic.commitText(suggestion.word + " ", 1)
                swipeCommitted = suggestion.word
                return
            }
        }
        if (word.isNotEmpty()) ic.deleteSurroundingText(word.length, 0)
        val written = Lexicon.matchCase(word.ifEmpty { suggestion.word }, suggestion.word)
        ic.commitText(written, 1)
        learned.learn(settings.language.code, written)
        swipeCommitted = null
        finishWord()
    }

    private fun onSwipe(samples: List<SwipeSample>, keys: List<SwipeKey>, keyWidth: Float) {
        if (!settings.swipeTyping || suppressSuggest) return
        val ic = currentInputConnection ?: return
        val matches = SwipeDecoder.rank(samples, keys, keyWidth, lexicon(settings.language).entries())
        val best = matches.firstOrNull() ?: return
        val pending = currentWord()
        if (pending.isNotEmpty()) ic.deleteSurroundingText(pending.length, 0)
        val written = when (shift) {
            ShiftState.CAPS -> best.uppercase()
            ShiftState.ONCE -> Lexicon.matchCase(best.replaceFirstChar { it.titlecase() }, best)
            ShiftState.OFF -> best
        }
        ic.commitText("$written ", 1)
        swipeCommitted = written
        undo = null
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        ranked = emptyList()
        shown = matches.take(3).map { Suggestion(Lexicon.matchCase(written, it), 0, 0, false) }
        suggestionViews.forEachIndexed { index, view ->
            val suggestion = shown.getOrNull(index)
            view.text = suggestion?.word.orEmpty()
            styleChip(view, index == 0 && suggestion != null)
        }
        reloadKeyboard()
    }

    private fun styleChip(view: TextView, primary: Boolean) {
        val colors = colorsFor(settings.theme)
        view.typeface = runCatching {
            androidx.core.content.res.ResourcesCompat.getFont(this, gh.otumfour.o24.R.font.outfit_semibold)
        }.getOrNull()
        if (!colors.modern) {
            view.setTextColor(colors.suggestionText)
            view.setBackgroundColor(colors.suggestionBg)
            return
        }
        view.setTextColor(if (primary) Color.parseColor("#F0C94A") else colors.suggestionText)
        val chip = if (colors.photoAsset != null) Color.argb(220, 18, 21, 28) else colors.keyFill
        view.background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(if (primary) 0xE61A1408.toInt() else chip)
            val stroke = if (primary) Color.parseColor("#F0C94A") else colors.keyStroke
            setStroke(dp(if (primary) 2 else 1), stroke)
        }
    }

    private fun cycleLanguage() {
        val enabled = settings.enabledLanguages.ifEmpty { listOf(KeyboardLanguage.ENGLISH) }
        val index = enabled.indexOf(settings.language).let { if (it < 0) 0 else it }
        val next = enabled[(index + 1) % enabled.size]
        store.update { it.copy(language = next) }
        settings = store.get()
        reloadKeyboard()
        refreshSuggestions()
    }

    private fun showLanguagePopup(anchor: View) {
        popup?.dismiss()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setBackgroundColor(colorsFor(settings.theme).suggestionBg)
        }
        settings.enabledLanguages.forEach { language ->
            column.addView(pill(language.label).also { button ->
                button.setOnClickListener {
                    store.update { it.copy(language = language) }
                    settings = store.get()
                    popup?.dismiss()
                    reloadKeyboard()
                    refreshSuggestions()
                }
            })
        }
        val window = PopupWindow(column, dp(180), LinearLayout.LayoutParams.WRAP_CONTENT, true)
        window.isOutsideTouchable = true
        window.elevation = dp(6).toFloat()
        popup = window
        window.showAsDropDown(anchor)
    }

    private fun showThemePicker(anchor: View) {
        popup?.dismiss()
        themeDialog?.dismiss()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.parseColor("#12151C"))
            }
        }
        column.addView(TextView(this).apply {
            text = getString(gh.otumfour.o24.R.string.settings_theme)
            setTextColor(Color.parseColor("#F4F6FB"))
            textSize = 16f
        })
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val photosFirst = listOf(
            ThemeId.OTUMFUO,
            ThemeId.GOLD_REGALIA,
            ThemeId.YAA_PHOTO,
            ThemeId.ANOKYE_PHOTO,
            ThemeId.NIGHT,
            ThemeId.DAY,
        )
        val rest = ThemeId.entries.filter { it !in photosFirst }
        (photosFirst + rest).forEach { theme -> row.addView(themeCard(theme)) }
        column.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        })
        column.addView(Button(this).apply {
            text = getString(gh.otumfour.o24.R.string.theme_picker_settings)
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.parseColor("#12151C"))
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(Color.parseColor("#F0C94A"))
            }
            setOnClickListener {
                themeDialog?.dismiss()
                openSettings()
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)))
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(column)
        val token = window?.window?.decorView?.windowToken
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (token != null) {
                setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG)
                attributes = attributes.apply { this.token = token }
            }
            setGravity(Gravity.BOTTOM)
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        themeDialog = dialog
        dialog.show()
    }

    private fun themeCard(theme: ThemeId): View {
        val selected = settings.theme == theme
        val colors = colorsFor(theme)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#1A1F2A"))
                setStroke(dp(if (selected) 2 else 1), if (selected) Color.parseColor("#F0C94A") else Color.argb(36, 255, 255, 255))
            }
        }
        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            val photo = colors.photoAsset
            if (photo != null) setImageBitmap(PhotoBackdrop.bitmap(this@O24InputMethodService, photo))
            else setBackgroundColor(colors.keyboardBg)
        }
        card.addView(thumb, LinearLayout.LayoutParams(dp(108), dp(72)))
        card.addView(TextView(this).apply {
            text = getString(theme.titleRes)
            setTextColor(Color.parseColor("#F4F6FB"))
            textSize = 12f
            maxLines = 2
            setPadding(dp(2), dp(4), dp(2), 0)
        }, LinearLayout.LayoutParams(dp(108), LinearLayout.LayoutParams.WRAP_CONTENT))
        card.setOnClickListener {
            store.update { it.copy(theme = theme) }
            settings = store.get()
            themeDialog?.dismiss()
            popup?.dismiss()
            reloadKeyboard()
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.marginEnd = dp(8)
        card.layoutParams = params
        return card
    }

    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN, true)
        }
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                val options = ActivityOptions.makeBasic().apply {
                    pendingIntentBackgroundActivityStartMode =
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }
                val pending = PendingIntent.getActivity(
                    this,
                    7,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                pending.send(this, 0, null, null, null, null, options.toBundle())
            } else {
                startActivity(intent)
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Settings did not open from the keyboard", error)
            runCatching { startActivity(intent) }
                .onFailure { Log.e(TAG, "Settings fallback failed", it) }
        }
    }

    private fun showStatus(message: String) {
        val view = statusView ?: return
        view.text = message
        view.visibility = if (message.isBlank()) View.GONE else View.VISIBLE
    }

    private fun currentWord(): String {
        val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        return before.takeLastWhile { it.isLetter() || it == '\'' || it == '’' || it == '-' }
    }

    private fun lexicon(language: KeyboardLanguage): Lexicon = lexicons.getOrPut(language.code) {
        val text = assets.open("dictionaries/${language.asset}").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val extra = if (language == KeyboardLanguage.ENGLISH) {
            runCatching {
                assets.open("dictionaries/en-freq.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
            }.getOrDefault("")
        } else {
            ""
        }
        Lexicon.parse(text + "\n" + extra)
    }

    private fun pill(text: String): Button = Button(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 13f
        minHeight = 0
        minimumHeight = 0
        setPadding(dp(12), dp(6), dp(12), dp(6))
        val colors = colorsFor(settings.theme)
        stylePill(this, colors.pillFill, colors.pillText, glow = false)
    }

    private fun stylePill(button: Button, fill: Int, text: Int, glow: Boolean) {
        button.setTextColor(text)
        button.backgroundTintList = null
        button.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(20).toFloat()
            setColor(fill)
            if (glow) {
                setStroke(dp(2), colorsFor(settings.theme).micGlow)
            }
        }
        if (glow) button.setShadowLayer(dp(8).toFloat(), 0f, 0f, colorsFor(settings.theme).micGlow)
    }

    private fun enterLabel(info: EditorInfo?): String {
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: return "Enter"
        return when (action) {
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_SEARCH -> "Search"
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_NEXT -> "Next"
            EditorInfo.IME_ACTION_DONE -> "Done"
            else -> "Enter"
        }
    }

    private fun wantsCaps(info: EditorInfo?): Boolean {
        if (!settings.autoCapitalise) return false
        val ic = currentInputConnection ?: return false
        val type = info?.inputType ?: return false
        return ic.getCursorCapsMode(type) != 0
    }

    private fun isPassword(info: EditorInfo?): Boolean {
        val type = info?.inputType ?: return false
        val variation = type and InputType.TYPE_MASK_VARIATION
        val noSuggestions = (type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
        val variationIsPassword = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_URI
        return noSuggestions || variationIsPassword
    }

    private fun isNumber(info: EditorInfo?): Boolean {
        val klass = info?.inputType?.and(InputType.TYPE_MASK_CLASS) ?: return false
        return klass == InputType.TYPE_CLASS_NUMBER || klass == InputType.TYPE_CLASS_PHONE
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    private fun marginStart() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { marginStart = dp(6) }

    private data class EditTarget(val selected: Boolean, val before: Int, val after: Int, val text: String)
    private data class Undo(val original: String, val replacement: String)

    private companion object {
        const val TAG = "O24IME"
    }
}
