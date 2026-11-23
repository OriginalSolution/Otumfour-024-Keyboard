package gh.otumfour.o24.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable

/** A supplied photograph, centre-cropped, with a dark veil so the keys stay readable. */
object PhotoBackdrop {
    private val cache = HashMap<String, Bitmap>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val veil = Color.argb(176, 7, 8, 12)

    fun draw(context: Context, canvas: Canvas, asset: String, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val bitmap = bitmap(context, asset) ?: return
        val scale = maxOf(width / bitmap.width.toFloat(), height / bitmap.height.toFloat())
        val dw = bitmap.width * scale
        val dh = bitmap.height * scale
        val left = (width - dw) / 2f
        val top = (height - dh) / 2f
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + dw, top + dh), paint)
        canvas.drawColor(veil)
    }

    fun drawable(context: Context, asset: String): Drawable {
        return object : Drawable() {
            override fun draw(canvas: Canvas) {
                draw(context, canvas, asset, bounds.width(), bounds.height())
            }

            override fun setAlpha(alpha: Int) = Unit
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit
            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
        }
    }

    private fun bitmap(context: Context, asset: String): Bitmap? {
        cache[asset]?.let { return it }
        val decoded = runCatching {
            context.assets.open(asset).use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return null
        cache[asset] = decoded
        return decoded
    }
}
