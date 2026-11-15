package gh.otumfour.o24

import android.content.Context
import java.io.File

/** The last uncaught crash, kept on this phone so it can be shared from About. */
object CrashReport {
    const val FILE_NAME = "o24-crash.txt"

    fun textOf(error: Throwable): String = buildString {
        append("O24 Keyboard ")
        append(BuildConfig.VERSION_NAME)
        append(" (")
        append(BuildConfig.VERSION_CODE)
        append(")\n")
        append(error.stackTraceToString())
    }

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun save(context: Context, error: Throwable) {
        file(context).writeText(textOf(error))
    }

    fun read(context: Context): String? {
        val saved = file(context)
        if (!saved.isFile || saved.length() <= 0L) return null
        return saved.readText()
    }
}
