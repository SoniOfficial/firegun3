package tj.soni.firegun.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

class SoldierStyle(val body: Int, val dark: Int, val helmet: Int, val gun: Int, val extra: Int)

object Sprites {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rc = RectF()
    private val path = Path()

    private fun fill(c: Int): Paint { p.style = Paint.Style.FILL; p.color = c; return p }
    private fun line(c: Int, w: Float): Paint { p.style = Paint.Style.STROKE; p.color = c; p.strokeWidth = w; p.strokeCap = Paint.Cap.ROUND; return p }

    val PLAYER = SoldierStyle(0xFF5B7B3F.toInt(), 0xFF38502A.toInt(), 0xFF2F3E2A.toInt(), 0xFF20262C.toInt(), 0)
    val BASIC = SoldierStyle(0xFFA33030.toInt(), 0xFF6B1B1B.toInt(), 0xFF3B1F1F.toInt(), 0xFF1C1C1C.toInt(), 0)
    val FAST = SoldierStyle(0xFFD0502F.toInt(), 0xFF8A2E1D.toInt(), 0xFF5D3A1A.toInt(), 0xFF1C1C1C.toInt(), 1)
    val HEAVY = SoldierStyle(0xFF7A2323.toInt(), 0xFF4F1616.toInt(), 0xFF4A515C.toInt(), 0xFF111111.toInt(), 2)
    val SNIPER = SoldierStyle(0xFF5F6034.toInt(), 0xFF3F4022.toInt(), 0xFF4A4A2A.toInt(), 0xFF1A1A1A.toInt(), 3)
    val BOSS = SoldierStyle(0xFF6B1F1F.toInt(), 0xFF3A0F0F.toInt(), 0xFFD4A017.toInt(), 0xFF111111.toInt(), 4)

    /** Top-down soldier facing +x rotated by [ang]. */
    fun soldier(c: Canvas, x: Float, y: Float, ang: Float, walk: Float, s: Float, st: SoldierStyle, gunLen: Float, flash: Boolean, hit: Boolean) {
        c.save()
        c.translate(x, y)
        c.drawOval(rc.also { it.set(-17 * s + 4, -13 * s + 7, 17 * s + 4, 13 * s + 7) }, fill(0x55000000))
        c.rotate(Math.toDegrees(ang.toDouble()).toFloat())
        val body = if (hit) 0xFFFFFFFF.toInt() else st.body
        val dark = if (hit) 0xFFE0E0E0.toInt() else st.dark
        val helm = if (hit) 0xFFFFFFFF.toInt() else st.helmet
        val sw = sin(walk) * 6f * s
        // legs
        c.drawOval(rc.also { it.set(-8 * s + sw, -10.5f * s, 8 * s + sw, -1.5f * s) }, fill(dark))
        c.drawOval(rc.also { it.set(-8 * s - sw, 1.5f * s, 8 * s - sw, 10.5f * s) }, fill(dark))
        // backpack / cape
        if (st.extra == 4) c.drawRect(-26 * s, -14 * s, -8 * s, 14 * s, fill(0xFF9B1C1C.toInt()))
        else c.drawRoundRect(rc.also { it.set(-18 * s, -9 * s, -8 * s, 9 * s) }, 3 * s, 3 * s, fill(dark))
        // torso
        c.drawOval(rc.also { it.set(-11 * s, -15 * s, 11 * s, 15 * s) }, fill(body))
        c.drawOval(rc.also { it.set(-9 * s, -14 * s, 4 * s, -3 * s) }, fill(0x26FFFFFF))
        c.drawOval(rc.also { it.set(-11 * s, -15 * s, 11 * s, 15 * s) }, line(0x40000000, 2f * s))
        c.drawLine(-3 * s, -12 * s, -3 * s, 12 * s, line(dark, 2f * s))
        if (st.extra == 2) {
            c.drawCircle(0f, -15 * s, 6 * s, fill(0xFF6B737E.toInt()))
            c.drawCircle(0f, 15 * s, 6 * s, fill(0xFF6B737E.toInt()))
            c.drawRoundRect(rc.also { it.set(-6 * s, -9 * s, 8 * s, 9 * s) }, 3 * s, 3 * s, fill(0xFF565D68.toInt()))
        }
        if (st.extra == 3) {
            val g = fill(0xFF3E5A2A.toInt())
            for (i in 0 until 8) c.drawCircle((-8 + i * 2.3f) * s, ((i * 7) % 11 - 5) * s * 1.4f, 3 * s, g)
        }
        // arms + gun
        c.drawLine(2 * s, -13 * s, 15 * s, -3 * s, line(body, 6f * s))
        c.drawLine(2 * s, 13 * s, 17 * s, 4 * s, line(body, 6f * s))
        c.drawRoundRect(rc.also { it.set(8 * s, -2.6f * s, (8 + gunLen) * s, 2.6f * s) }, 2 * s, 2 * s, fill(st.gun))
        c.drawRect(8 * s, -1.2f * s, (8 + gunLen * 0.45f) * s, 1.2f * s, fill(0xFF555B63.toInt()))
        c.drawCircle(16 * s, -3 * s, 3.3f * s, fill(0xFFE0B48A.toInt()))
        c.drawCircle(18 * s, 4 * s, 3.3f * s, fill(0xFFE0B48A.toInt()))
        // head
        c.drawCircle(0f, 0f, 9.5f * s, fill(helm))
        c.drawCircle(-2.5f * s, -3f * s, 3.2f * s, fill(0x33FFFFFF))
        c.drawCircle(0f, 0f, 9.5f * s, line(0x40000000, 1.6f * s))
        c.drawArc(rc.also { it.set(-8 * s, -8 * s, 8 * s, 8 * s) }, 200f, 100f, false, line(0x44FFFFFF, 2f * s))
        c.drawRoundRect(rc.also { it.set(3.5f * s, -5.5f * s, 9 * s, 5.5f * s) }, 2 * s, 2 * s, fill(0xFF0D1117.toInt()))
        c.drawRect(5 * s, -4 * s, 8 * s, 4 * s, fill(0x8866E0FF.toInt()))
        if (st.extra == 1) c.drawLine(-6 * s, -9 * s, -6 * s, 9 * s, line(0xFFE53935.toInt(), 3f * s))
        if (st.extra == 4) c.drawLine(-9 * s, 0f, 6 * s, 0f, line(0xFFFFD54F.toInt(), 3f * s))
        if (flash) {
            val fx = (8 + gunLen + 5) * s
            c.drawCircle(fx, 0f, 10 * s, fill(0x99FFB300.toInt()))
            c.drawCircle(fx, 0f, 5 * s, fill(0xFFFFFFFF.toInt()))
            val l = line(0xFFFFE082.toInt(), 2.2f * s)
            c.drawLine(fx, -14 * s, fx + 8 * s, -4 * s, l); c.drawLine(fx, 14 * s, fx + 8 * s, 4 * s, l)
            c.drawLine(fx + 4 * s, 0f, fx + 18 * s, 0f, l)
        }
        c.restore()
    }

    fun tree(c: Canvas, cx: Float, cy: Float, v: Int) {
        val base = intArrayOf(0xFF2E6B2A.toInt(), 0xFF35782F.toInt(), 0xFF285C25.toInt())[v % 3]
        c.drawOval(rc.also { it.set(cx - 34, cy - 20, cx + 44, cy + 44) }, fill(0x44000000))
        c.drawCircle(cx, cy - 6, 34f, fill(base))
        c.drawCircle(cx - 14, cy - 12, 20f, fill(0xFF3F8A38.toInt()))
        c.drawCircle(cx + 14, cy - 2, 18f, fill(0xFF2A6226.toInt()))
        c.drawCircle(cx - 10, cy - 18, 9f, fill(0xFF58A94B.toInt()))
        c.drawCircle(cx + 12, cy - 14, 6f, fill(0xFF4C9A42.toInt()))
        c.drawCircle(cx + 20, cy + 6, 11f, fill(0xFF2E6B2A.toInt()))
        c.drawCircle(cx - 22, cy + 8, 10f, fill(0xFF2A6226.toInt()))
        c.drawCircle(cx + 4, cy + 14, 9f, fill(0xFF245A22.toInt()))
        c.drawCircle(cx - 4, cy - 2, 7f, fill(0xFF6BBE58.toInt()))
        c.drawCircle(cx, cy - 6, 34f, line(0x33000000, 2f))
    }

    fun rock(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        c.drawOval(rc.also { it.set(x - 2, y + h * 0.45f, x + w + 8, y + h + 8) }, fill(0x44000000))
        path.reset()
        path.moveTo(x + w * 0.1f, y + h * 0.6f); path.lineTo(x + w * 0.25f, y + h * 0.1f)
        path.lineTo(x + w * 0.7f, y); path.lineTo(x + w, y + h * 0.45f)
        path.lineTo(x + w * 0.85f, y + h); path.lineTo(x + w * 0.2f, y + h); path.close()
        c.drawPath(path, fill(0xFF8A8F96.toInt()))
        c.drawPath(path, line(0xFF4D5258.toInt(), 2f))
        path.reset()
        path.moveTo(x + w * 0.25f, y + h * 0.1f); path.lineTo(x + w * 0.7f, y); path.lineTo(x + w * 0.6f, y + h * 0.45f); path.lineTo(x + w * 0.2f, y + h * 0.5f); path.close()
        c.drawPath(path, fill(0xFFB4B9BF.toInt()))
    }

    fun crate(c: Canvas, x: Float, y: Float, w: Float, h: Float, v: Int) {
        c.drawRect(x + 4, y + 5, x + w + 6, y + h + 6, fill(0x44000000))
        val main = if (v == 0) 0xFFB5834B.toInt() else 0xFF5C7048.toInt()
        val dark = if (v == 0) 0xFF6E4A23.toInt() else 0xFF33432A.toInt()
        c.drawRect(x, y, x + w, y + h, fill(main))
        c.drawRect(x + 3, y + 3, x + w - 3, y + h - 3, line(dark, 3f))
        c.drawLine(x + 3, y + 3, x + w - 3, y + h - 3, line(dark, 3f))
        c.drawLine(x + w - 3, y + 3, x + 3, y + h - 3, line(dark, 3f))
        c.drawRect(x, y, x + w, y + h, line(0xFF2A1B0B.toInt(), 2f))
    }

    fun barrier(c: Canvas, x: Float, y: Float, w: Float, h: Float, v: Int) {
        c.drawRect(x + 3, y + 5, x + w + 5, y + h + 6, fill(0x44000000))
        if (v == 0 && w >= h || v == 1 && h > w) {
            // sandbags
            val horiz = w >= h
            val n = ((if (horiz) w else h) / 22f).toInt().coerceAtLeast(2)
            for (i in 0 until n) {
                if (horiz) rc.set(x + i * (w / n), y, x + (i + 1) * (w / n) + 2, y + h) else rc.set(x, y + i * (h / n), x + w, y + (i + 1) * (h / n) + 2)
                c.drawRoundRect(rc, 8f, 8f, fill(if (i % 2 == 0) 0xFFC8B27A.toInt() else 0xFFB89F63.toInt()))
                c.drawRoundRect(rc, 8f, 8f, line(0xFF7A6838.toInt(), 1.5f))
            }
        } else {
            c.drawRect(x, y, x + w, y + h, fill(0xFF9AA0A7.toInt()))
            c.drawRect(x, y, x + w, y + h, line(0xFF50565D.toInt(), 2f))
            c.drawRect(x + 3, y + 3, x + w - 3, y + 8, fill(0xFFC3C8CE.toInt()))
        }
    }

    fun tank(c: Canvas, x: Float, y: Float, w: Float, h: Float, v: Int) {
        val hull = if (v == 0) 0xFF4F6B3A.toInt() else 0xFF9A8458.toInt()
        val dark = if (v == 0) 0xFF2D4020.toInt() else 0xFF6B5A38.toInt()
        c.drawRect(x + 5, y + 7, x + w + 7, y + h + 8, fill(0x44000000))
        c.drawRoundRect(rc.also { it.set(x, y, x + w, y + 16) }, 6f, 6f, fill(0xFF262A30.toInt()))
        c.drawRoundRect(rc.also { it.set(x, y + h - 16, x + w, y + h) }, 6f, 6f, fill(0xFF262A30.toInt()))
        var tx = x + 8
        while (tx < x + w - 6) { c.drawLine(tx, y + 2, tx, y + 14, line(0xFF3C424A.toInt(), 2f)); c.drawLine(tx, y + h - 14, tx, y + h - 2, line(0xFF3C424A.toInt(), 2f)); tx += 10 }
        c.drawRoundRect(rc.also { it.set(x + 6, y + 10, x + w - 6, y + h - 10) }, 8f, 8f, fill(hull))
        c.drawRoundRect(rc.also { it.set(x + 6, y + 10, x + w - 6, y + h - 10) }, 8f, 8f, line(dark, 2f))
        c.drawLine(x + w / 2, y + h / 2, x + w + 22, y + h / 2, line(dark, 7f))
        c.drawCircle(x + w / 2 - 4, y + h / 2, 17f, fill(dark))
        c.drawCircle(x + w / 2 - 4, y + h / 2, 12f, fill(hull))
        c.drawCircle(x + w / 2 - 8, y + h / 2 - 3, 4f, fill(0x55FFFFFF))
    }

    fun building(c: Canvas, x: Float, y: Float, w: Float, h: Float, v: Int) {
        c.drawRect(x + 8, y + 10, x + w + 10, y + h + 12, fill(0x55000000))
        c.drawRect(x, y, x + w, y + h, fill(if (v == 0) 0xFF6E747C.toInt() else 0xFF7A6F5E.toInt()))
        c.drawRect(x + 6, y + 6, x + w - 6, y + h - 6, fill(if (v == 0) 0xFF878E97.toInt() else 0xFF948A76.toInt()))
        c.drawRect(x, y, x + w, y + h, line(0xFF2E3238.toInt(), 3f))
        c.drawRect(x + 16, y + 16, x + 60, y + 50, fill(0xFF535A63.toInt()))
        c.drawRect(x + 16, y + 16, x + 60, y + 50, line(0xFF30353B.toInt(), 2f))
        c.drawCircle(x + 38, y + 33, 11f, line(0xFF30353B.toInt(), 2f))
        c.drawRect(x + w - 70, y + h - 54, x + w - 20, y + h - 18, fill(0xFF535A63.toInt()))
        var sx = x + 10
        while (sx < x + w - 20) { c.drawRect(sx, y + h - 14, sx + 12, y + h - 8, fill(if (((sx - x) / 24).toInt() % 2 == 0) 0xFFF5B800.toInt() else 0xFF222222.toInt())); sx += 12 }
    }

    fun helipad(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        c.drawRect(x, y, x + w, y + h, fill(0xFF4A5059.toInt()))
        c.drawRect(x + 6, y + 6, x + w - 6, y + h - 6, line(0xFFF5B800.toInt(), 4f))
        val cx = x + w / 2; val cy = y + h / 2
        c.drawCircle(cx, cy, 58f, line(0xFFEDEDED.toInt(), 5f))
        c.drawLine(cx - 22, cy - 30, cx - 22, cy + 30, line(0xFFEDEDED.toInt(), 8f))
        c.drawLine(cx + 22, cy - 30, cx + 22, cy + 30, line(0xFFEDEDED.toInt(), 8f))
        c.drawLine(cx - 22, cy, cx + 22, cy, line(0xFFEDEDED.toInt(), 8f))
    }

    fun coin(c: Canvas, x: Float, y: Float, r: Float) {
        c.drawCircle(x, y, r + 3, fill(0x44FFC107))
        c.drawCircle(x, y, r, fill(0xFFFFC107.toInt()))
        c.drawCircle(x, y, r, line(0xFFB77A00.toInt(), 2f))
        c.drawCircle(x, y, r * 0.62f, line(0xFFFFE082.toInt(), 2f))
        c.drawLine(x - r * 0.2f, y - r * 0.35f, x - r * 0.2f, y + r * 0.35f, line(0xFFB77A00.toInt(), 2f))
    }

    fun medkit(c: Canvas, x: Float, y: Float) {
        c.drawCircle(x, y, 17f, fill(0x3300E676))
        c.drawRoundRect(rc.also { it.set(x - 11, y - 9, x + 11, y + 9) }, 4f, 4f, fill(0xFFF5F5F5.toInt()))
        c.drawRect(x - 2.5f, y - 7, x + 2.5f, y + 7, fill(0xFFE53935.toInt()))
        c.drawRect(x - 7, y - 2.5f, x + 7, y + 2.5f, fill(0xFFE53935.toInt()))
    }

    fun ammoBox(c: Canvas, x: Float, y: Float) {
        c.drawCircle(x, y, 17f, fill(0x33FFD54F))
        c.drawRoundRect(rc.also { it.set(x - 11, y - 8, x + 11, y + 8) }, 3f, 3f, fill(0xFF5C7048.toInt()))
        for (i in 0 until 3) c.drawRect(x - 8 + i * 6, y - 5, x - 5 + i * 6, y + 5, fill(0xFFFFD54F.toInt()))
    }

    fun armorPick(c: Canvas, x: Float, y: Float) {
        c.drawCircle(x, y, 17f, fill(0x3342A5F5))
        shield(c, x, y, 11f, 0xFF42A5F5.toInt(), 0xFF0D47A1.toInt())
    }

    fun shield(c: Canvas, cx: Float, cy: Float, r: Float, col: Int, border: Int) {
        path.reset()
        path.moveTo(cx - r, cy - r * 0.8f); path.lineTo(cx, cy - r); path.lineTo(cx + r, cy - r * 0.8f)
        path.lineTo(cx + r * 0.9f, cy + r * 0.1f); path.quadTo(cx + r * 0.6f, cy + r * 0.8f, cx, cy + r)
        path.quadTo(cx - r * 0.6f, cy + r * 0.8f, cx - r * 0.9f, cy + r * 0.1f); path.close()
        c.drawPath(path, fill(col)); c.drawPath(path, line(border, 2f))
    }

    fun grenade(c: Canvas, x: Float, y: Float, blink: Boolean) {
        c.drawCircle(x + 3, y + 4, 8f, fill(0x44000000))
        c.drawCircle(x, y, 8f, fill(if (blink) 0xFFFF5252.toInt() else 0xFF3E5A2A.toInt()))
        c.drawCircle(x, y, 8f, line(0xFF1B2A12.toInt(), 2f))
        c.drawRect(x - 3, y - 12, x + 3, y - 6, fill(0xFF9AA0A7.toInt()))
        c.drawLine(x + 3, y - 10, x + 9, y - 12, line(0xFFCCCCCC.toInt(), 2f))
    }

    fun bullet(c: Canvas, x: Float, y: Float, vx: Float, vy: Float, color: Int, size: Float, player: Boolean) {
        if (player) {
            c.drawLine(x - vx * 0.03f, y - vy * 0.03f, x, y, line((color and 0x00FFFFFF) or 0x66000000, size * 1.8f))
            c.drawLine(x - vx * 0.02f, y - vy * 0.02f, x, y, line(color, size * 0.9f))
            c.drawCircle(x, y, size * 0.5f, fill(0xFFFFFFFF.toInt()))
        } else {
            c.drawCircle(x, y, size + 3, fill(0x44FF5252))
            c.drawCircle(x, y, size, fill(color))
            c.drawCircle(x, y, size * 0.45f, fill(0xFFFFF3E0.toInt()))
        }
    }

    fun ring(c: Canvas, x: Float, y: Float, r: Float, color: Int, w: Float) { c.drawCircle(x, y, r, line(color, w)) }
    fun dot(c: Canvas, x: Float, y: Float, r: Float, color: Int) { c.drawCircle(x, y, r, fill(color)) }
    fun lineP(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, w: Float) { c.drawLine(x1, y1, x2, y2, line(color, w)) }
    fun rectP(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int) { c.drawRect(l, t, r, b, fill(color)) }

    @Suppress("unused")
    private fun unused() = cos(0f)
}
