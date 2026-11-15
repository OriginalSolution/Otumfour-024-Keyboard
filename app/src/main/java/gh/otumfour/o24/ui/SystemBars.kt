package gh.otumfour.o24.ui

import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max

/** Draw behind the system bars, then keep content clear of them. */
fun ComponentActivity.prepareEdgeToEdge() {
    enableEdgeToEdge()
}

fun View.applySystemBarInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        runCatching {
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = max(bars.bottom, ime.bottom),
            )
        }.onFailure { Log.e("O24", "Applying system bar insets failed", it) }
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(this)
}
