package gh.otumfour.o24.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class PageDotsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var count: Int = 0
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }
    var selected: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        if (count <= 0) return
        val radius = dp(4f)
        val gap = dp(10f)
        val total = count * radius * 2 + (count - 1) * gap
        var x = (width - total) / 2f + radius
        val y = height / 2f
        for (i in 0 until count) {
            paint.color = if (i == selected) Color.parseColor("#E8B52D") else Color.parseColor("#6E6454")
            canvas.drawCircle(x, y, if (i == selected) radius else radius * 0.8f, paint)
            x += radius * 2 + gap
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), dp(18f).toInt())
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
