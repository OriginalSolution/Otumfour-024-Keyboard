package gh.otumfour.o24.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/** Vertical Asante-flag stripes, pulled about 70% toward black. */
class KenteBackdrop @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#070605"))
        val bands = intArrayOf(
            dim("#E8B52D"),
            Color.BLACK,
            dim("#0E7A3A"),
            Color.BLACK,
            dim("#8C1D18"),
            dim("#E8B52D"),
            Color.BLACK,
            dim("#0E7A3A"),
        )
        val widths = floatArrayOf(22f, 10f, 26f, 8f, 14f, 18f, 12f, 20f)
        var x = 0f
        var i = 0
        while (x < width) {
            val band = dp(widths[i % widths.size])
            paint.color = bands[i % bands.size]
            canvas.drawRect(x, 0f, x + band, height.toFloat(), paint)
            x += band
            i++
        }
    }

    private fun dim(hex: String): Int {
        val color = Color.parseColor(hex)
        val keep = 0.30f
        return Color.rgb(
            (Color.red(color) * keep).toInt(),
            (Color.green(color) * keep).toInt(),
            (Color.blue(color) * keep).toInt(),
        )
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
