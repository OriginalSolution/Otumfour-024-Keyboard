package gh.otumfour.o24.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import gh.otumfour.o24.theme.GoldenStool

/** A book and a small Golden Stool. No person is drawn. */
class EmptyShelfView(context: Context) : View(context) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, (168 * resources.displayMetrics.density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val book = RectF(width * 0.18f, height * 0.28f, width * 0.62f, height * 0.78f)
        fill.color = Color.parseColor("#3A2412")
        canvas.drawRoundRect(book, 12f * density, 12f * density, fill)
        stroke.color = Color.parseColor("#E8B52D")
        stroke.strokeWidth = 2f * density
        canvas.drawRoundRect(book, 12f * density, 12f * density, stroke)
        fill.color = Color.parseColor("#F6F1E4")
        val page = RectF(book.left + 10f * density, book.top + 8f * density, book.right - 8f * density, book.bottom - 8f * density)
        canvas.drawRoundRect(page, 6f * density, 6f * density, fill)
        stroke.color = Color.parseColor("#CDBF98")
        stroke.strokeWidth = 1.4f * density
        var y = page.top + 16f * density
        while (y < page.bottom - 8f * density) {
            canvas.drawLine(page.left + 10f * density, y, page.right - 10f * density, y, stroke)
            y += 12f * density
        }
        GoldenStool.draw(
            canvas,
            width * 0.74f,
            height * 0.22f,
            width * 0.28f,
            Color.parseColor("#E8B52D"),
            glow = true,
        )
    }
}
