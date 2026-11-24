package gh.otumfour.o24.ime

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import gh.otumfour.o24.theme.GoldenStool

/** Original drawn stickers. No photographs and no pictures of people. */
object StickerArt {
    data class Sticker(val id: String, val pack: String, val title: String)

    val all: List<Sticker> = listOf(
        Sticker("kente-bands", "Kente", "Kente bands"),
        Sticker("kente-square", "Kente", "Kente square"),
        Sticker("stool-mark", "Kente", "Stool mark"),
        Sticker("flag-bands", "Kente", "Gold, black, green"),
        Sticker("gye-nyame", "Adinkra", "Gye Nyame"),
        Sticker("sankofa", "Adinkra", "Sankofa"),
        Sticker("adinkra-ring", "Adinkra", "Ring"),
        Sticker("adinkra-cross", "Adinkra", "Cross"),
    )

    fun packs(): List<String> = all.map { it.pack }.distinct()

    fun bitmap(id: String, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#12151C")
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), size * 0.12f, size * 0.12f, paint)
        when (id) {
            "kente-bands", "kente-square" -> bands(canvas, paint, size, id == "kente-square")
            "stool-mark" -> {
                GoldenStool.draw(canvas, size / 2f, size * 0.18f, size * 0.72f, Color.parseColor("#F0C94A"), 255)
            }
            "flag-bands" -> flag(canvas, paint, size)
            "gye-nyame" -> gye(canvas, paint, size)
            "sankofa" -> sankofa(canvas, paint, size)
            "adinkra-ring" -> ring(canvas, paint, size)
            else -> cross(canvas, paint, size)
        }
        return bitmap
    }

    private fun bands(canvas: Canvas, paint: Paint, size: Int, boxed: Boolean) {
        val colors = listOf("#E11D2E", "#F0C94A", "#111111", "#3DDC97", "#F0C94A")
        val stripe = size / colors.size.toFloat()
        colors.forEachIndexed { index, color ->
            paint.color = Color.parseColor(color)
            if (boxed && index % 2 == 0) {
                canvas.drawRect(index * stripe, 0f, (index + 1) * stripe, size.toFloat(), paint)
            } else {
                canvas.drawRect(0f, index * stripe, size.toFloat(), (index + 1) * stripe, paint)
            }
        }
    }

    private fun flag(canvas: Canvas, paint: Paint, size: Int) {
        val colors = listOf("#F2C200", "#111111", "#0E8A43")
        val band = size / 3f
        colors.forEachIndexed { index, color ->
            paint.color = Color.parseColor(color)
            canvas.drawRect(0f, index * band, size.toFloat(), (index + 1) * band, paint)
        }
    }

    private fun gye(canvas: Canvas, paint: Paint, size: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.06f
        paint.color = Color.parseColor("#F0C94A")
        val c = size / 2f
        canvas.drawCircle(c, c, size * 0.28f, paint)
        canvas.drawLine(c, size * 0.18f, c, size * 0.82f, paint)
        canvas.drawLine(size * 0.22f, c, size * 0.78f, c, paint)
    }

    private fun sankofa(canvas: Canvas, paint: Paint, size: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.055f
        paint.color = Color.parseColor("#F0C94A")
        val oval = RectF(size * 0.22f, size * 0.28f, size * 0.78f, size * 0.78f)
        canvas.drawArc(oval, 200f, 280f, false, paint)
        canvas.drawLine(size * 0.62f, size * 0.3f, size * 0.78f, size * 0.16f, paint)
    }

    private fun ring(canvas: Canvas, paint: Paint, size: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.07f
        paint.color = Color.parseColor("#F0C94A")
        canvas.drawCircle(size / 2f, size / 2f, size * 0.28f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(size / 2f, size / 2f, size * 0.06f, paint)
    }

    private fun cross(canvas: Canvas, paint: Paint, size: Int) {
        paint.color = Color.parseColor("#F0C94A")
        val t = size * 0.16f
        canvas.drawRect((size - t) / 2f, size * 0.18f, (size + t) / 2f, size * 0.82f, paint)
        canvas.drawRect(size * 0.18f, (size - t) / 2f, size * 0.82f, (size + t) / 2f, paint)
    }
}
