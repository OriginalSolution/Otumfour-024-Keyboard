package gh.otumfour.o24.ime

/** Toolbar slots. Order is saved on the device. GIF can be hidden when the server has no key. */
enum class ToolbarItem(val id: String, val label: String) {
    GRID("grid", "Menu"),
    STICKERS("stickers", "Stickers"),
    GIF("gif", "GIF"),
    SETTINGS("settings", "Settings"),
    TRANSLATE("translate", "Translate"),
    THEME("theme", "Themes"),
    MIC("mic", "Mic"),
    AI("ai", "AI"),
    ;

    companion object {
        val DEFAULT: List<ToolbarItem> = entries.toList()

        fun parse(raw: String?): List<ToolbarItem> {
            if (raw.isNullOrBlank()) return DEFAULT
            val known = raw.split(",").mapNotNull { token ->
                entries.firstOrNull { it.id == token.trim() }
            }
            return (known + DEFAULT.filter { it !in known }).distinct()
        }

        fun join(items: List<ToolbarItem>): String = items.joinToString(",") { it.id }
    }
}
