package gh.otumfour.o24.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import gh.otumfour.o24.R
import kotlin.math.max

/** Semicircular arch with nested green, black and gold borders around the regalia photo. */
class ArchPhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.otumfuo_regalia)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    override fun onDraw(canvas: Canvas) {
        val outer = dp(8f)
        val bounds = RectF(outer, outer, width - outer, height - outer)
        val photo = inset(bounds, dp(14f))
        val clip = arch(photo)
        canvas.save()
        canvas.clipPath(clip)
        drawCenterCrop(canvas, photo)
        canvas.restore()
        stroke.strokeWidth = dp(5f)
        stroke.color = Color.parseColor("#0E7A3A")
        canvas.drawPath(arch(bounds), stroke)
        stroke.strokeWidth = dp(3.5f)
        stroke.color = Color.BLACK
        canvas.drawPath(arch(inset(bounds, dp(5f))), stroke)
        stroke.strokeWidth = dp(3f)
        stroke.color = Color.parseColor("#E8B52D")
        canvas.drawPath(arch(inset(bounds, dp(9f))), stroke)
    }

    private fun drawCenterCrop(canvas: Canvas, dest: RectF) {
        val scale = max(dest.width() / bitmap.width, dest.height() / bitmap.height)
        val dw = bitmap.width * scale
        val dh = bitmap.height * scale
        val left = dest.centerX() - dw / 2f
        val top = dest.top + dest.height() * 0.04f - dh * 0.02f
        fill.color = Color.BLACK
        canvas.drawRect(dest, fill)
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + dw, top + dh), fill)
    }

    private fun arch(rect: RectF): Path {
        val path = Path()
        val diameter = rect.width()
        path.moveTo(rect.left, rect.top + diameter / 2f)
        path.arcTo(RectF(rect.left, rect.top, rect.right, rect.top + diameter), 180f, 180f, false)
        path.lineTo(rect.right, rect.bottom)
        path.lineTo(rect.left, rect.bottom)
        path.close()
        return path
    }

    private fun inset(rect: RectF, amount: Float) = RectF(
        rect.left + amount,
        rect.top + amount,
        rect.right - amount,
        rect.bottom - amount,
    )

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
