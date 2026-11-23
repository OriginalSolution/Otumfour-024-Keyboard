package gh.otumfour.o24.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.Log

/** A supplied photograph, centre-cropped, with a light veil so the picture stays recognisable. */
object PhotoBackdrop {
    private val cache = HashMap<String, Bitmap>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val veilPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(context: Context, canvas: Canvas, asset: String, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val bitmap = bitmap(context, asset) ?: return
        val scale = maxOf(width / bitmap.width.toFloat(), height / bitmap.height.toFloat())
        val dw = bitmap.width * scale
        val dh = bitmap.height * scale
        val left = (width - dw) / 2f
        val top = (height - dh) / 2f
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + dw, top + dh), paint)
        veilPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            Color.argb(90, 7, 8, 12),
            Color.argb(122, 7, 8, 12),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), veilPaint)
    }

    fun drawable(context: Context, asset: String): Drawable {
        return object : Drawable() {
            override fun draw(canvas: Canvas) {
                draw(context, canvas, asset, bounds.width(), bounds.height())
            }

            override fun setAlpha(alpha: Int) = Unit
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
        }
    }

    fun bitmap(context: Context, asset: String): Bitmap? {
        cache[asset]?.let { if (!it.isRecycled) return it }
        val decoded = runCatching {
            val bytes = context.assets.open(asset).use { it.readBytes() }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.onFailure { Log.e("O24Photo", "Could not decode $asset", it) }.getOrNull()
        if (decoded == null) {
            Log.e("O24Photo", "Decoder returned nothing for $asset")
            return null
        }
        cache[asset] = decoded
        return decoded
    }
}
