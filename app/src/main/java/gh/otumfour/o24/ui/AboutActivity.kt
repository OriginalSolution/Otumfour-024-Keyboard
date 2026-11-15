package gh.otumfour.o24.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

/** About, including the photo credit. Histories stay on this page and in the theme carousel. */
class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareEdgeToEdge()
        val frame = PatternFrame(this)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val center = CenterFrame(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            addView(slideBackButton(this@AboutActivity) { finish() })
            addView(aboutColumn(this@AboutActivity) { })
        }
        center.addView(column)
        scroll.addView(center)
        frame.addView(scroll)
        setContentView(frame)
        frame.applySystemBarInsets()
    }
}
