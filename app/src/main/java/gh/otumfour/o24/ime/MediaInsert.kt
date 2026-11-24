package gh.otumfour.o24.ime

import android.content.ClipDescription
import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import androidx.core.content.FileProvider
import java.io.File

/** Inserts a local image into the field. Falls back to a link when the field refuses it. */
object MediaInsert {
    fun commitFile(context: Context, connection: InputConnection, file: File, mime: String): Boolean {
        if (Build.VERSION.SDK_INT < 25 || !file.exists()) return false
        val uri: Uri = runCatching {
            FileProvider.getUriForFile(context, context.packageName + ".files", file)
        }.getOrNull() ?: return false
        val info = InputContentInfo(uri, ClipDescription(file.name, arrayOf(mime)), null)
        return runCatching {
            connection.commitContent(info, InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null)
        }.getOrDefault(false)
    }
}
