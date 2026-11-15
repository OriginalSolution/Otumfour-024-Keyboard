package gh.otumfour.o24.theme

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Original drawings of Kente weave and two Adinkra ideas (Gye Nyame, Sankofa).
 * These are geometric interpretations, not copies of a cloth photo or a palace mark.
 */
object ThemeArt {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(canvas: Canvas, theme: ThemeId, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        when (theme) {
            ThemeId.GOLDEN_STOOL -> royal(canvas, width.toFloat(), height.toFloat())
            ThemeId.KENTE -> kente(canvas, width.toFloat(), height.toFloat())
            ThemeId.ADINKRA -> adinkra(canvas, width.toFloat(), height.toFloat())
            ThemeId.ASANTE_FLAG -> asanteFlag(canvas, width.toFloat(), height.toFloat(), stoolAlpha = 78)
            ThemeId.SIKA_DWA -> sika(canvas, width.toFloat(), height.toFloat())
            ThemeId.ROYAL_CREST -> crest(canvas, width.toFloat(), height.toFloat())
            ThemeId.OKOMFO_ANOKYE -> anokyeKeys(canvas, width.toFloat(), height.toFloat())
            ThemeId.YAA_ASANTEWAA -> yaaKeys(canvas, width.toFloat(), height.toFloat())
            ThemeId.KOTOKO -> kotokoKeys(canvas, width.toFloat(), height.toFloat())
            ThemeId.ASANTEMAN_JERSEY -> asantemanKeys(canvas, width.toFloat(), height.toFloat())
        }
    }

    fun drawKotokoHeader(canvas: Canvas, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        kotokoKeys(canvas, width.toFloat(), height.toFloat())
        quills(canvas, width / 2f, height * 0.72f, width * 0.42f, Color.parseColor("#F2E6C8"), 90)
    }

    fun drawAsantemanHeader(canvas: Canvas, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        asantemanKeys(canvas, width.toFloat(), height.toFloat())
        GoldenStool.draw(
            canvas,
            width * 0.2f,
            height * 0.28f,
            width * 0.22f,
            Color.parseColor("#F3D37A"),
            alpha = 230,
        )
    }

    fun drawFlag(canvas: Canvas, width: Int, height: Int, stoolAlpha: Int = 220) {
        if (width <= 0 || height <= 0) return
        asanteFlag(canvas, width.toFloat(), height.toFloat(), stoolAlpha)
    }

    fun drawOkomfoScene(canvas: Canvas, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#120804")
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.color = Color.parseColor("#3A2410")
        paint.alpha = 90
        canvas.drawOval(-w * 0.1f, h * 0.05f, w * 0.55f, h * 0.42f, paint)
        canvas.drawOval(w * 0.35f, h * 0.0f, w * 1.1f, h * 0.38f, paint)
        canvas.drawOval(w * 0.1f, h * 0.12f, w * 0.9f, h * 0.48f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.parseColor("#E6C36A")
        paint.alpha = 70
        var i = -4
        while (i <= 4) {
            canvas.drawLine(w * 0.5f, h * 0.02f, w * (0.5f + i * 0.12f), h * 0.72f, paint)
            i++
        }
        paint.alpha = 255
        GoldenStool.draw(canvas, w * 0.5f, h * 0.16f, w * 0.42f, Color.parseColor("#F3D37A"), alpha = 235, glow = true)
        rock(canvas, w * 0.72f, h * 0.62f, w * 0.28f)
        sword(canvas, w * 0.72f, h * 0.28f, h * 0.42f)
    }

    fun drawYaaHeader(canvas: Canvas, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        yaaKeys(canvas, w, h)
        GoldenStool.draw(
            canvas,
            w * 0.5f,
            h * 0.48f,
            w * 0.46f,
            Color.parseColor("#E8C872"),
            alpha = 235,
            glow = true,
            crown = true,
            swords = true,
        )
    }

    private fun royal(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0C0A07")
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.parseColor("#6E5820")
        paint.alpha = 90
        var x = -h
        while (x < w + h) {
            canvas.drawLine(x, 0f, x + h, h, paint)
            canvas.drawLine(x, h, x + h, 0f, paint)
            x += 22f
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 70
        val golds = intArrayOf(
            Color.parseColor("#8C6B1F"),
            Color.parseColor("#D4AF37"),
            Color.parseColor("#3A2E10"),
        )
        var y = 8f
        var row = 0
        while (y < h) {
            var px = (row % 2) * 14f
            var col = 0
            while (px < w) {
                paint.color = golds[(row + col) % golds.size]
                paint.alpha = 46
                canvas.drawRect(px, y, px + 10f, y + 6f, paint)
                px += 28f
                col++
            }
            y += 16f
            row++
        }
        paint.alpha = 255
    }

    private fun kente(canvas: Canvas, w: Float, h: Float) {
        val bands = intArrayOf(
            Color.parseColor("#F2C200"),
            Color.parseColor("#0B7A3B"),
            Color.parseColor("#C8102E"),
            Color.parseColor("#111111"),
            Color.parseColor("#F2C200"),
            Color.parseColor("#0B7A3B"),
            Color.parseColor("#C8102E"),
            Color.parseColor("#111111"),
        )
        val band = h / bands.size
        paint.style = Paint.Style.FILL
        for (i in bands.indices) {
            paint.color = bands[i]
            val top = i * band
            canvas.drawRect(0f, top, w, top + band, paint)
            val motif = if (bands[i] == Color.parseColor("#111111")) Color.parseColor("#F2C200") else Color.parseColor("#111111")
            paint.color = motif
            var x = 6f
            while (x < w) {
                canvas.drawRect(x, top + band * 0.28f, x + 8f, top + band * 0.72f, paint)
                x += 22f
            }
        }
    }

    private fun adinkra(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1C1C1C")
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.color = Color.parseColor("#C6A15B")
        paint.alpha = 42
        val stepX = 150f
        val stepY = 130f
        var y = 20f
        var row = 0
        while (y < h + 40) {
            var x = if (row % 2 == 0) 16f else 90f
            var col = 0
            while (x < w + 40) {
                if ((row + col) % 2 == 0) gyeNyame(canvas, x, y, 78f) else sankofa(canvas, x, y, 78f)
                x += stepX
                col++
            }
            y += stepY
            row++
        }
        paint.alpha = 255
    }

    /** A compact geometric reading of Gye Nyame: curls around a central diamond. */
    private fun gyeNyame(canvas: Canvas, x: Float, y: Float, size: Float) {
        val path = Path()
        val cx = x + size / 2f
        val cy = y + size / 2f
        path.addOval(cx - size * 0.18f, cy - size * 0.34f, cx + size * 0.18f, cy - size * 0.02f, Path.Direction.CW)
        path.moveTo(cx, cy - size * 0.08f)
        path.lineTo(cx + size * 0.16f, cy + size * 0.12f)
        path.lineTo(cx, cy + size * 0.32f)
        path.lineTo(cx - size * 0.16f, cy + size * 0.12f)
        path.close()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f
        canvas.drawPath(path, paint)
        val arc = RectF(cx - size * 0.42f, cy - size * 0.2f, cx - size * 0.05f, cy + size * 0.28f)
        canvas.drawArc(arc, 40f, 220f, false, paint)
        val arcR = RectF(cx + size * 0.05f, cy - size * 0.2f, cx + size * 0.42f, cy + size * 0.28f)
        canvas.drawArc(arcR, 280f, 220f, false, paint)
        paint.style = Paint.Style.FILL
    }

    /** Sankofa as a bird turning its head back toward an egg. */
    private fun sankofa(canvas: Canvas, x: Float, y: Float, size: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f
        val body = RectF(x + size * 0.22f, y + size * 0.28f, x + size * 0.72f, y + size * 0.78f)
        canvas.drawOval(body, paint)
        val path = Path()
        path.moveTo(x + size * 0.68f, y + size * 0.42f)
        path.quadTo(x + size * 0.95f, y + size * 0.2f, x + size * 0.55f, y + size * 0.18f)
        canvas.drawPath(path, paint)
        canvas.drawCircle(x + size * 0.52f, y + size * 0.2f, size * 0.06f, paint)
        canvas.drawCircle(x + size * 0.4f, y + size * 0.55f, size * 0.07f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun asanteFlag(canvas: Canvas, w: Float, h: Float, stoolAlpha: Int) {
        paint.style = Paint.Style.FILL
        val band = h / 3f
        paint.color = Color.parseColor("#F2C200")
        canvas.drawRect(0f, 0f, w, band, paint)
        paint.color = Color.parseColor("#111111")
        canvas.drawRect(0f, band, w, band * 2f, paint)
        paint.color = Color.parseColor("#0B7A3B")
        canvas.drawRect(0f, band * 2f, w, h, paint)
        val stoolW = minOf(w, h) * 0.72f
        GoldenStool.draw(canvas, w / 2f, h * 0.22f, stoolW, Color.parseColor("#F3D37A"), alpha = stoolAlpha, glow = false)
    }

    private fun sika(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#070605")
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.color = Color.parseColor("#2A2114")
        paint.alpha = 70
        canvas.drawOval(w * 0.05f, h * 0.05f, w * 0.95f, h * 0.95f, paint)
        paint.alpha = 40
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w, y + 8f, paint)
            y += 14f
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        GoldenStool.draw(canvas, w / 2f, h * 0.12f, minOf(w, h) * 0.78f, Color.parseColor("#F3D37A"), alpha = 230, glow = true)
    }

    private fun crest(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#10140F")
        canvas.drawRect(0f, 0f, w, h, paint)
        val stripe = (h * 0.045f).coerceAtLeast(8f)
        paint.color = Color.parseColor("#F2C200")
        canvas.drawRect(0f, 0f, w, stripe, paint)
        paint.color = Color.parseColor("#111111")
        canvas.drawRect(0f, stripe, w, stripe * 2f, paint)
        paint.color = Color.parseColor("#0B7A3B")
        canvas.drawRect(0f, stripe * 2f, w, stripe * 3f, paint)
    }

    private fun anokyeKeys(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1A0C08")
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.color = Color.parseColor("#4A2014")
        paint.alpha = 80
        canvas.drawRect(0f, 0f, w, h * 0.45f, paint)
        paint.color = Color.parseColor("#C6A15B")
        paint.alpha = 36
        var y = 16f
        var row = 0
        while (y < h) {
            var x = if (row % 2 == 0) 8f else 70f
            while (x < w) {
                gyeNyame(canvas, x, y, 54f)
                x += 120f
            }
            y += 90f
            row++
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }

    private fun yaaKeys(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1A0E28")
        canvas.drawRect(0f, 0f, w, h, paint)
        val bands = intArrayOf(
            Color.parseColor("#3A2060"),
            Color.parseColor("#D4AF37"),
            Color.parseColor("#24143C"),
            Color.parseColor("#6A4A16"),
        )
        paint.alpha = 36
        val band = 18f
        var y = 0f
        var i = 0
        while (y < h) {
            paint.color = bands[i % bands.size]
            canvas.drawRect(0f, y, w, y + band, paint)
            y += band * 3f
            i++
        }
        paint.color = Color.parseColor("#E8C872")
        paint.alpha = 40
        var row = 0
        y = 12f
        while (y < h) {
            var x = if (row % 2 == 0) 6f else 64f
            while (x < w) {
                if ((row + x.toInt()) % 2 == 0) gyeNyame(canvas, x, y, 48f) else sankofa(canvas, x, y, 48f)
                x += 110f
            }
            y += 86f
            row++
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }

    private fun rock(canvas: Canvas, cx: Float, top: Float, width: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#3A322C")
        paint.alpha = 255
        val path = Path()
        path.moveTo(cx - width * 0.5f, top + width * 0.45f)
        path.quadTo(cx - width * 0.2f, top, cx + width * 0.15f, top + width * 0.12f)
        path.quadTo(cx + width * 0.55f, top + width * 0.05f, cx + width * 0.5f, top + width * 0.5f)
        path.quadTo(cx, top + width * 0.72f, cx - width * 0.5f, top + width * 0.45f)
        canvas.drawPath(path, paint)
    }

    private fun mesh(canvas: Canvas, w: Float, h: Float, base: Int, line: Int) {
        paint.style = Paint.Style.FILL
        paint.color = base
        paint.alpha = 255
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = line
        paint.alpha = 55
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, 0f, x, h, paint)
            x += 8f
        }
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w, y, paint)
            y += 8f
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }

    /** Original radiating quills. Not a club crest. */
    private fun quills(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, alpha: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = color
        paint.alpha = alpha
        var angle = -70.0
        while (angle <= 70.0) {
            val rad = Math.toRadians(angle)
            val x = cx + (kotlin.math.cos(rad) * radius).toFloat()
            val y = cy - (kotlin.math.sin(rad) * radius).toFloat()
            canvas.drawLine(cx, cy, x, y, paint)
            angle += 10.0
        }
        paint.style = Paint.Style.FILL
        paint.alpha = alpha
        canvas.drawCircle(cx, cy, radius * 0.12f, paint)
        paint.alpha = 255
    }

    private fun kotokoKeys(canvas: Canvas, w: Float, h: Float) {
        mesh(canvas, w, h, Color.parseColor("#7A1020"), Color.parseColor("#F7F4EA"))
        paint.color = Color.parseColor("#E8C872")
        paint.alpha = 28
        var row = 0
        var y = 12f
        while (y < h) {
            var x = if (row % 2 == 0) 8f else 28f
            while (x < w) {
                val path = Path()
                path.moveTo(x, y + 10f)
                path.lineTo(x + 8f, y)
                path.lineTo(x + 16f, y + 10f)
                path.lineTo(x + 8f, y + 20f)
                path.close()
                canvas.drawPath(path, paint)
                x += 32f
            }
            y += 28f
            row++
        }
        paint.alpha = 255
    }

    private fun asantemanKeys(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        val band = h / 3f
        paint.color = Color.parseColor("#F2C200")
        canvas.drawRect(0f, 0f, w, band, paint)
        paint.color = Color.parseColor("#111111")
        canvas.drawRect(0f, band, w, band * 2f, paint)
        paint.color = Color.parseColor("#0B7A3B")
        canvas.drawRect(0f, band * 2f, w, h, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.parseColor("#F6E7B4")
        paint.alpha = 40
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w, y, paint)
            y += 7f
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }

    private fun sword(canvas: Canvas, cx: Float, top: Float, length: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E6C36A")
        canvas.drawRect(cx - length * 0.02f, top, cx + length * 0.02f, top + length * 0.72f, paint)
        canvas.drawRect(cx - length * 0.09f, top + length * 0.7f, cx + length * 0.09f, top + length * 0.76f, paint)
        canvas.drawRect(cx - length * 0.025f, top + length * 0.76f, cx + length * 0.025f, top + length * 0.92f, paint)
        canvas.drawCircle(cx, top + length * 0.95f, length * 0.035f, paint)
    }
}
