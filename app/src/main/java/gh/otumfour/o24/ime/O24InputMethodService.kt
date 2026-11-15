package gh.otumfour.o24.ime

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import gh.otumfour.o24.ai.AiClient
import gh.otumfour.o24.ai.AiResult
import gh.otumfour.o24.data.LearnedWords
import gh.otumfour.o24.data.O24Settings
import gh.otumfour.o24.data.SettingsStore
import gh.otumfour.o24.lang.KeyboardLanguage
import gh.otumfour.o24.serwaa.SerwaaPersona
import gh.otumfour.o24.serwaa.SerwaaSpeaker
import gh.otumfour.o24.serwaa.VoiceTyper
import gh.otumfour.o24.suggest.Lexicon
import gh.otumfour.o24.suggest.Suggestion
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
    private lateinit var speaker: SerwaaSpeaker
    private lateinit var voice: VoiceTyper

    private val lexicons = HashMap<String, Lexicon>()
    private var settings: O24Settings = O24Settings(
        theme = ThemeId.GOLDEN_STOOL,
        language = KeyboardLanguage.ENGLISH,
        enabledLanguages = KeyboardLanguage.entries.toList(),
        autocorrect = true,
        userName = "",
        greeting = "",
        serwaaLanguage = KeyboardLanguage.ENGLISH,
        aiEndpoint = "",
        aiModel = "gpt-4o-mini",
        aiKey = "",
        aiEnabled = false,
        ttsEndpoint = "",
        ttsKey = "",
    )

    private var page = BoardPage.LETTERS
    private var shift = ShiftState.OFF
    private var enterLabel = "Enter"
    private var suppressSuggest = false
    private var ranked: List<Suggestion> = emptyList()
    private var shown: List<Suggestion?> = listOf(null, null, null)
    private var listening = false

    private var root: LinearLayout? = null
    private var keyboard: KeyboardView? = null
    private var suggestionViews: List<TextView> = emptyList()
    private var statusView: TextView? = null
    private var toneRow: LinearLayout? = null
    private var languageButton: Button? = null
    private var serwaaButton: Button? = null
    private var popup: PopupWindow? = null

    override fun onCreate() {
        super.onCreate()
        store = SettingsStore(this)
        learned = LearnedWords(this)
        speaker = SerwaaSpeaker(this)
        voice = VoiceTyper(this, onState = { showStatus(it) }, onText = { commitVoice(it) })
        settings = store.get()
        getSharedPreferences(SettingsStore.NAME, MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        getSharedPreferences(SettingsStore.NAME, MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(this)
        voice.stop()
        speaker.shutdown()
        scope.cancel()
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        settings = store.get()
        reloadKeyboard()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        settings = store.get()
        enterLabel = enterLabel(info)
        suppressSuggest = isPassword(info) || isNumber(info)
        page = if (isNumber(info)) BoardPage.SYMBOLS else BoardPage.LETTERS
        shift = if (wantsCaps(info)) ShiftState.ONCE else ShiftState.OFF
        listening = false
        reloadKeyboard()
        refreshSuggestions()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        voice.stop()
        speaker.stop()
        popup?.dismiss()
        super.onFinishInputView(finishingInput)
    }

    override fun onCreateInputView(): View {
        settings = store.get()
        val colors = colorsFor(settings.theme)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colors.keyboardBg)
        }
        root = column

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
            visibility = View.GONE
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }
        column.addView(toneRow, matchWrap())

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
        serwaaButton = pill("Serwaa").also { button ->
            button.setOnClickListener { onSerwaaTap() }
            button.setOnLongClickListener {
                speaker.speak(SerwaaPersona.greeting(settings.userName, settings.greeting), settings)
                showStatus(SerwaaPersona.greeting(settings.userName, settings.greeting))
                true
            }
            toolbar.addView(button, marginStart())
        }
        toolbar.addView(pill("AI").also { it.setOnClickListener { onAi() } }, marginStart())
        toolbar.addView(pill("Translate").also { it.setOnClickListener { onTranslate() } }, marginStart())
        toolbar.addView(pill("⚙").also { it.setOnClickListener { openSettings() } }, marginStart())
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
            column.addView(board, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        reloadKeyboard()
        return column
    }

    private fun reloadKeyboard() {
        val board = keyboard ?: return
        val colors = colorsFor(settings.theme)
        board.theme = settings.theme
        board.colors = colors
        board.rows = KeyboardModel.rows(page, settings.language, shift, enterLabel, colors.spaceTitle)
        root?.setBackgroundColor(colors.keyboardBg)
        suggestionViews.forEach {
            it.setTextColor(colors.suggestionText)
            it.setBackgroundColor(colors.suggestionBg)
        }
        (suggestionViews.firstOrNull()?.parent as? View)?.setBackgroundColor(colors.suggestionBg)
        statusView?.setTextColor(colors.suggestionText)
        styleToolbar()
        languageButton?.text = settings.language.shortLabel
    }

    private fun styleToolbar() {
        val colors = colorsFor(settings.theme)
        languageButton?.let { stylePill(it, colors.pillFill, colors.pillText, glow = false) }
        serwaaButton?.let { stylePill(it, colors.micFill, colors.micLabel, glow = settings.theme == ThemeId.GOLDEN_STOOL) }
        val parent = serwaaButton?.parent as? LinearLayout ?: return
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is Button && child !== languageButton && child !== serwaaButton) {
                stylePill(child, colors.actionFill, colors.actionLabel, glow = false)
            }
        }
        parent.setBackgroundColor(colors.toolbarBg)
    }

    private fun onKey(key: KeySpec) {
        val ic = currentInputConnection ?: return
        when (key.action) {
            KeyAction.CHAR -> {
                if (key.value.isEmpty()) return
                ic.commitText(key.value, 1)
                if (shift == ShiftState.ONCE && key.value.any { it.isLetter() }) shift = ShiftState.OFF
                reloadKeyboard()
                refreshSuggestions()
            }
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
                deleteBackward(ic)
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

    private fun commitSpace(ic: InputConnection) {
        val word = currentWord()
        if (!suppressSuggest && settings.autocorrect && word.length >= 3) {
            val choice = Lexicon.autocorrect(word, ranked)
            if (choice != null) {
                ic.deleteSurroundingText(word.length, 0)
                val written = Lexicon.matchCase(word, choice)
                ic.commitText("$written ", 1)
                learned.learn(settings.language.code, written)
                finishWord()
                return
            }
        }
        if (!suppressSuggest && word.length >= 3 && !lexicon(settings.language).contains(word)) {
            learned.learn(settings.language.code, word)
        }
        ic.commitText(" ", 1)
        finishWord()
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

    private fun commitVoice(text: String) {
        listening = false
        serwaaButton?.text = "Serwaa"
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
        val gap = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
        ic.commitText(gap + text, 1)
        showStatus("Serwaa typed that.")
        refreshSuggestions()
    }

    private fun onSerwaaTap() {
        if (listening) {
            voice.stop()
            listening = false
            serwaaButton?.text = "Serwaa"
            showStatus("Serwaa stopped.")
            return
        }
        val allowed = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!allowed) {
            showStatus("Allow the microphone in O24 Keyboard settings, then tap Serwaa again.")
            openSettings()
            return
        }
        listening = true
        serwaaButton?.text = "Listening"
        voice.start(settings.serwaaLanguage.speechTag)
    }

    private fun onAi() {
        if (suppressSuggest) {
            showStatus("Rewrite stays off on password and number fields.")
            return
        }
        val target = captureTarget()
        if (target == null) {
            showStatus("Type or select something for Serwaa to rewrite.")
            return
        }
        aiUnavailable()?.let {
            showStatus(it)
            return
        }
        showTones(target)
    }

    private fun showTones(target: EditTarget) {
        val row = toneRow ?: return
        row.removeAllViews()
        row.visibility = View.VISIBLE
        listOf("Clear", "Formal", "Warm", "Shorter").forEach { tone ->
            row.addView(pill(tone).also { button ->
                button.setOnClickListener {
                    row.visibility = View.GONE
                    runRewrite(target, tone)
                }
            }, marginStart())
        }
        showStatus("Pick a tone.")
    }

    private fun runRewrite(target: EditTarget, tone: String) {
        showStatus("Serwaa is rewriting…")
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                client().rewrite(SerwaaPersona.systemPrompt(settings.userName), target.text, tone)
            }
            applyAi(target, result, speak = true)
        }
    }

    private fun onTranslate() {
        if (suppressSuggest) {
            showStatus("Translate stays off on password and number fields.")
            return
        }
        val target = captureTarget()
        if (target == null) {
            showStatus("Type or select something for Serwaa to translate.")
            return
        }
        aiUnavailable()?.let {
            showStatus(it)
            return
        }
        val (source, targetLang) = translationPair()
        showStatus("Serwaa is translating…")
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                client().translate(
                    SerwaaPersona.systemPrompt(settings.userName),
                    target.text,
                    source.englishName,
                    targetLang.englishName,
                )
            }
            applyAi(target, result, speak = true)
        }
    }

    private fun applyAi(target: EditTarget, result: AiResult, speak: Boolean) {
        when (result) {
            is AiResult.Ok -> {
                replace(target, result.text)
                showStatus(result.text)
                if (speak) speaker.speak(result.text.take(280), settings)
                refreshSuggestions()
            }
            is AiResult.Unavailable -> showStatus(result.message)
        }
    }

    private fun translationPair(): Pair<KeyboardLanguage, KeyboardLanguage> {
        val active = settings.language
        return if (active == KeyboardLanguage.ENGLISH) {
            val preferred = settings.serwaaLanguage.takeIf { it != KeyboardLanguage.ENGLISH } ?: KeyboardLanguage.ASANTE
            KeyboardLanguage.ENGLISH to preferred
        } else {
            active to KeyboardLanguage.ENGLISH
        }
    }

    private fun aiUnavailable(): String? {
        if (!settings.aiEnabled) return "Turn on AI in O24 Keyboard settings to rewrite and translate."
        if (settings.aiKey.isBlank()) return "Set an AI key in O24 Keyboard settings to use this."
        if (settings.aiEndpoint.isBlank()) return "Add an AI endpoint in O24 Keyboard settings to use this."
        return null
    }

    private fun client() = AiClient(settings.aiEndpoint, settings.aiKey, settings.aiModel)

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
        shown = listOf(ranked.getOrNull(1), ranked.getOrNull(0), ranked.getOrNull(2))
        suggestionViews.forEachIndexed { index, view ->
            val suggestion = shown[index]
            view.text = suggestion?.let { Lexicon.matchCase(word, it.word) }.orEmpty()
            view.setTypeface(null, if (index == 1) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun applyShown(index: Int) {
        val suggestion = shown.getOrNull(index) ?: return
        val ic = currentInputConnection ?: return
        val word = currentWord()
        if (word.isNotEmpty()) ic.deleteSurroundingText(word.length, 0)
        val written = Lexicon.matchCase(word, suggestion.word)
        ic.commitText(written, 1)
        learned.learn(settings.language.code, written)
        finishWord()
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

    private fun openSettings() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
        Lexicon.parse(text)
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
        val ic = currentInputConnection ?: return false
        val type = info?.inputType ?: return false
        return ic.getCursorCapsMode(type) != 0
    }

    private fun isPassword(info: EditorInfo?): Boolean {
        val variation = info?.inputType?.and(InputType.TYPE_MASK_VARIATION) ?: return false
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
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
}
