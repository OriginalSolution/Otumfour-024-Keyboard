package gh.otumfour.o24.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.Log

/** A supplied photograph, centre-cropped, faded lightly so the letters stay clear. */
object PhotoBackdrop {
    private val cache = HashMap<String, Bitmap>()
    private val softCache = HashMap<String, Bitmap>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val veilPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(
        context: Context,
        canvas: Canvas,
        asset: String,
        width: Int,
        height: Int,
        strength: BackgroundStrength = BackgroundStrength.SOFT,
        fallback: Int = Color.parseColor("#0D1016"),
    ) {
        if (width <= 0 || height <= 0) return
        canvas.drawColor(fallback)
        if (strength == BackgroundStrength.OFF) return
        val bitmap = (if (BackdropPaint.blur(strength)) softened(context, asset) else bitmap(context, asset)) ?: return
        val scale = maxOf(width / bitmap.width.toFloat(), height / bitmap.height.toFloat())
        val dw = bitmap.width * scale
        val dh = bitmap.height * scale
        val left = (width - dw) / 2f
        val top = (height - dh) / 2f
        paint.alpha = BackdropPaint.pictureAlpha(strength)
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + dw, top + dh), paint)
        paint.alpha = 255
        val alpha = BackdropPaint.scrimAlpha(strength)
        veilPaint.shader = null
        veilPaint.color = Color.argb(alpha, 7, 8, 12)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), veilPaint)
    }

    fun drawable(
        context: Context,
        asset: String,
        strength: BackgroundStrength = BackgroundStrength.SOFT,
        fallback: Int = Color.parseColor("#0D1016"),
    ): Drawable {
        return object : Drawable() {
            override fun draw(canvas: Canvas) {
                draw(context, canvas, asset, bounds.width(), bounds.height(), strength, fallback)
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

    private fun softened(context: Context, asset: String): Bitmap? {
        softCache[asset]?.let { if (!it.isRecycled) return it }
        val source = bitmap(context, asset) ?: return null
        val tinyW = (source.width / 4).coerceAtLeast(24)
        val tinyH = (source.height / 4).coerceAtLeast(24)
        val tiny = Bitmap.createScaledBitmap(source, tinyW, tinyH, true)
        val soft = Bitmap.createScaledBitmap(
            tiny,
            source.width.coerceAtMost(640),
            source.height.coerceAtMost(640),
            true,
        )
        if (tiny !== soft && tiny !== source) tiny.recycle()
        softCache[asset] = soft
        return soft
    }
}
