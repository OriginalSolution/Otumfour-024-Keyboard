package gh.otumfour.o24.ui

/**
 * Compares the secure input-method setting with both ComponentName forms.
 * Devices store either package/.Class or package/package.Class, sometimes with a subtype after ';'.
 */
object ImeMatch {
    fun fullId(packageName: String, className: String): String = "$packageName/$className"

    fun shortId(packageName: String, className: String): String {
        val relative = if (className.startsWith(packageName)) {
            className.substring(packageName.length).let { if (it.startsWith(".")) it else ".$it" }
        } else {
            if (className.startsWith(".")) className else ".$className"
        }
        return "$packageName/$relative"
    }

    fun isOurs(value: String?, packageName: String, className: String): Boolean {
        if (value.isNullOrBlank() || packageName.isBlank() || className.isBlank()) return false
        val id = value.substringBefore(';').trim()
        return id == fullId(packageName, className) || id == shortId(packageName, className)
    }

    fun listContains(enabled: String?, packageName: String, className: String): Boolean {
        if (enabled.isNullOrBlank()) return false
        return enabled.split(':').any { isOurs(it, packageName, className) }
    }
}
