package gh.otumfour.o24.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import gh.otumfour.o24.theme.ThemeArt

/** Otumfour wordmark above the Asante flag and a drawn Golden Stool. No people. */
class BrandHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    init {
        setWillNotDraw(false)
    }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F3D37A")
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CDBF98")
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        val flagTop = height * 0.42f
        canvas.drawColor(Color.parseColor("#0C0A07"))
        title.textSize = height * 0.16f
        canvas.drawText("OTUMFOUR", width / 2f, height * 0.22f, title)
        line.textSize = height * 0.07f
        canvas.drawText("O24 Keyboard", width / 2f, height * 0.34f, line)
        canvas.save()
        canvas.translate(0f, flagTop)
        ThemeArt.drawFlag(canvas, width, (height - flagTop).toInt(), stoolAlpha = 230)
        canvas.restore()
    }
}
