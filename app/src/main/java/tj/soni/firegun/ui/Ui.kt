package tj.soni.firegun.ui

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import tj.soni.firegun.render.Sprites
import kotlin.math.cos
import kotlin.math.sin

enum class BtnStyle(val top: Int, val bottom: Int, val edge: Int, val text: Int) {
    GREEN(0xFF55D955.toInt(), 0xFF2E9E2E.toInt(), 0xFF176317.toInt(), 0xFFFFFFFF.toInt()),
    DARK(0xFF2E3A4D.toInt(), 0xFF18212F.toInt(), 0xFF0A0F16.toInt(), 0xFFFFFFFF.toInt()),
    GOLD(0xFFFFD23F.toInt(), 0xFFE89A00.toInt(), 0xFF8A5A00.toInt(), 0xFF3A2500.toInt()),
    BLUE(0xFF4A94FF.toInt(), 0xFF1D5BD6.toInt(), 0xFF0F3A91.toInt(), 0xFFFFFFFF.toInt()),
    RED(0xFFEE5348.toInt(), 0xFFB02A20.toInt(), 0xFF661510.toInt(), 0xFFFFFFFF.toInt()),
    GRAY(0xFF69727F.toInt(), 0xFF454D58.toInt(), 0xFF262B33.toInt(), 0xFFB9C0CA.toInt())
}

class Btn(var x: Float, var y: Float, var w: Float, var h: Float, var label: String, var style: BtnStyle, var icon: Int = 0, var textSize: Float = 0f, val onClick: () -> Unit) {
    var pressed = false
    var enabled = true
    fun hit(px: Float, py: Float) = px >= x && px <= x + w && py >= y && py <= y + h
}

object Ui {
    const val GOLD = 0xFFF5B800.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val PANEL = 0xDD121B2A.toInt()
    const val BORDER = 0xFF33445E.toInt()

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rc = RectF()
    private val path = Path()
    private val boldFace: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    private val italicFace: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)

    fun fillP(c: Int): Paint { p.style = Paint.Style.FILL; p.shader = null; p.color = c; return p }
    fun lineP(c: Int, w: Float): Paint { p.style = Paint.Style.STROKE; p.shader = null; p.color = c; p.strokeWidth = w; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND; return p }

    fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, align: Paint.Align = Paint.Align.LEFT, outline: Float = 0f, italic: Boolean = false) {
        p.shader = null
        p.typeface = if (italic) italicFace else boldFace
        p.textSize = size
        p.textAlign = align
        if (outline > 0f) {
            p.style = Paint.Style.STROKE; p.strokeWidth = outline; p.color = 0xFF10151C.toInt(); p.strokeJoin = Paint.Join.ROUND
            c.drawText(s, x, y, p)
        }
        p.style = Paint.Style.FILL
        p.color = color
        c.drawText(s, x, y, p)
    }

    /** Draws text vertically centred on [cy]. */
    fun textC(c: Canvas, s: String, x: Float, cy: Float, size: Float, color: Int, align: Paint.Align = Paint.Align.CENTER, outline: Float = 0f) {
        p.typeface = boldFace
        p.textSize = size
        val fm = p.fontMetrics
        text(c, s, x, cy - (fm.ascent + fm.descent) / 2f, size, color, align, outline)
    }

    fun textWidth(s: String, size: Float): Float { p.typeface = boldFace; p.textSize = size; return p.measureText(s) }

    fun panel(c: Canvas, x: Float, y: Float, w: Float, h: Float, fill: Int = PANEL, border: Int = BORDER, radius: Float = 14f) {
        rc.set(x, y, x + w, y + h)
        c.drawRoundRect(rc, radius, radius, fillP(fill))
        c.drawRoundRect(rc, radius, radius, lineP(border, 2.5f))
    }

    fun button(c: Canvas, b: Btn) {
        val st = if (b.enabled) b.style else BtnStyle.GRAY
        val off = if (b.pressed) 3f else 0f
        val r = (b.h * 0.22f).coerceAtMost(16f)
        rc.set(b.x, b.y + 4f, b.x + b.w, b.y + b.h + 4f)
        c.drawRoundRect(rc, r, r, fillP(st.edge))
        rc.set(b.x, b.y + off, b.x + b.w, b.y + b.h + off)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, b.y + off, 0f, b.y + b.h + off, st.top, st.bottom, Shader.TileMode.CLAMP)
        c.drawRoundRect(rc, r, r, p)
        p.shader = null
        c.drawRoundRect(rc, r, r, lineP(st.edge, 2f))
        rc.set(b.x + 4, b.y + off + 3, b.x + b.w - 4, b.y + off + b.h * 0.42f)
        c.drawRoundRect(rc, r, r, fillP(0x22FFFFFF))
        val ts = if (b.textSize > 0f) b.textSize else (b.h * 0.38f).coerceAtMost(30f)
        val cy = b.y + off + b.h / 2f
        if (b.icon != 0) {
            val ix = b.x + b.h * 0.6f
            icon(c, b.icon, ix, cy, b.h * 0.4f, if (st == BtnStyle.GOLD) 0xFF3A2500.toInt() else if (st == BtnStyle.GREEN || st == BtnStyle.BLUE || st == BtnStyle.RED) WHITE else GOLD)
            textC(c, b.label, b.x + b.h * 0.95f + (b.w - b.h * 0.95f) / 2f, cy, ts, st.text)
        } else textC(c, b.label, b.x + b.w / 2f, cy, ts, st.text)
    }

    fun bar(c: Canvas, x: Float, y: Float, w: Float, h: Float, frac: Float, top: Int, bottom: Int, label: String) {
        rc.set(x, y, x + w, y + h)
        c.drawRoundRect(rc, h / 2, h / 2, fillP(0xFF0D121A.toInt()))
        val f = frac.coerceIn(0f, 1f)
        if (f > 0.01f) {
            rc.set(x + 2, y + 2, x + 2 + (w - 4) * f, y + h - 2)
            p.style = Paint.Style.FILL
            p.shader = LinearGradient(0f, y, 0f, y + h, top, bottom, Shader.TileMode.CLAMP)
            c.drawRoundRect(rc, h / 2, h / 2, p)
            p.shader = null
        }
        rc.set(x, y, x + w, y + h)
        c.drawRoundRect(rc, h / 2, h / 2, lineP(0xFF3B4658.toInt(), 2f))
        textC(c, label, x + w / 2 + h * 0.4f, y + h / 2, h * 0.66f, WHITE, outline = 3f)
    }

    fun heart(c: Canvas, cx: Float, cy: Float, r: Float) {
        path.reset()
        path.moveTo(cx, cy + r * 0.9f)
        path.cubicTo(cx - r * 1.6f, cy - r * 0.1f, cx - r * 0.7f, cy - r * 1.1f, cx, cy - r * 0.35f)
        path.cubicTo(cx + r * 0.7f, cy - r * 1.1f, cx + r * 1.6f, cy - r * 0.1f, cx, cy + r * 0.9f)
        c.drawPath(path, fillP(0xFFE53935.toInt()))
        c.drawPath(path, lineP(0xFF7F1010.toInt(), 2f))
        c.drawCircle(cx - r * 0.45f, cy - r * 0.35f, r * 0.2f, fillP(0x88FFFFFF.toInt()))
    }

    fun star(c: Canvas, cx: Float, cy: Float, r: Float, filled: Boolean) {
        path.reset()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.45f
            val a = (-Math.PI / 2 + i * Math.PI / 5).toFloat()
            val px = cx + cos(a) * rad
            val py = cy + sin(a) * rad
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        c.drawPath(path, fillP(if (filled) 0xFFFFC21A.toInt() else 0xFF2B3544.toInt()))
        c.drawPath(path, lineP(if (filled) 0xFF9A6200.toInt() else 0xFF161C26.toInt(), 2f))
    }

    fun skull(c: Canvas, cx: Float, cy: Float, r: Float, col: Int) {
        c.drawCircle(cx, cy - r * 0.1f, r * 0.85f, fillP(col))
        rc.set(cx - r * 0.5f, cy + r * 0.4f, cx + r * 0.5f, cy + r * 1.0f)
        c.drawRoundRect(rc, 3f, 3f, fillP(col))
        c.drawCircle(cx - r * 0.35f, cy - r * 0.1f, r * 0.22f, fillP(0xFF1B1B1B.toInt()))
        c.drawCircle(cx + r * 0.35f, cy - r * 0.1f, r * 0.22f, fillP(0xFF1B1B1B.toInt()))
    }

    fun weaponIcon(c: Canvas, id: Int, cx: Float, cy: Float, s: Float, col: Int) {
        val dk = 0xFF1E242B.toInt()
        fun r(l: Float, t: Float, rr: Float, b: Float, color: Int) { rc.set(cx + l * s, cy + t * s, cx + rr * s, cy + b * s); c.drawRoundRect(rc, 2f * s, 2f * s, fillP(color)) }
        when (id) {
            0 -> { r(-16f, -8f, 14f, 0f, col); r(-16f, -2f, -4f, 14f, dk); r(2f, 0f, 8f, 5f, dk) }
            1 -> { r(-30f, -6f, 22f, 2f, col); r(-38f, -8f, -26f, 4f, dk); r(-6f, 2f, 2f, 18f, dk); r(22f, -4f, 34f, -1f, dk); r(-18f, -11f, 6f, -6f, dk) }
            2 -> { r(-12f, -6f, 36f, -1f, col); r(-12f, 0f, 30f, 5f, dk); r(-38f, -6f, -12f, 6f, 0xFF8A4B1F.toInt()) }
            3 -> { r(-30f, -4f, 40f, 1f, col); r(-38f, -6f, -26f, 5f, dk); r(-10f, -12f, 12f, -6f, dk); r(-3f, 1f, 3f, 12f, dk); r(40f, -3f, 48f, 0f, dk) }
            else -> { r(-30f, -7f, 20f, 3f, col); r(-38f, -9f, -26f, 5f, dk); r(-8f, 3f, 8f, 16f, dk); r(20f, -4f, 36f, -1f, dk); r(26f, -1f, 28f, 10f, dk) }
        }
    }

    fun grenadeIcon(c: Canvas, cx: Float, cy: Float, r: Float) {
        c.drawCircle(cx, cy + r * 0.1f, r * 0.8f, fillP(0xFF4F6B3A.toInt()))
        c.drawCircle(cx, cy + r * 0.1f, r * 0.8f, lineP(0xFF1B2A12.toInt(), 2f))
        rc.set(cx - r * 0.3f, cy - r * 0.95f, cx + r * 0.3f, cy - r * 0.5f)
        c.drawRect(rc, fillP(0xFFB0B6BD.toInt()))
        c.drawLine(cx + r * 0.3f, cy - r * 0.8f, cx + r * 0.8f, cy - r * 0.95f, lineP(0xFFDDDDDD.toInt(), 2f))
    }

    fun coin(c: Canvas, cx: Float, cy: Float, r: Float) = Sprites.coin(c, cx, cy, r)

    fun icon(c: Canvas, id: Int, cx: Float, cy: Float, r: Float, col: Int) {
        when (id) {
            1 -> { path.reset(); path.moveTo(cx - r * 0.6f, cy - r); path.lineTo(cx + r, cy); path.lineTo(cx - r * 0.6f, cy + r); path.close(); c.drawPath(path, fillP(col)) }
            2 -> star(c, cx, cy, r, true)
            3 -> weaponIcon(c, 0, cx, cy, r / 14f, col)
            4 -> {
                c.drawCircle(cx, cy, r * 0.62f, lineP(col, r * 0.4f))
                for (i in 0 until 8) { val a = i * Math.PI / 4; c.drawLine(cx + cos(a).toFloat() * r * 0.75f, cy + sin(a).toFloat() * r * 0.75f, cx + cos(a).toFloat() * r * 1.05f, cy + sin(a).toFloat() * r * 1.05f, lineP(col, r * 0.38f)) }
            }
            5 -> { c.drawCircle(cx, cy, r, fillP(col)); textC(c, "i", cx, cy, r * 1.5f, 0xFF18212F.toInt()) }
            6 -> { path.reset(); path.moveTo(cx + r, cy - r * 0.5f); path.lineTo(cx - r * 0.2f, cy - r * 0.5f); path.lineTo(cx - r * 0.2f, cy - r); path.lineTo(cx - r * 1.1f, cy); path.lineTo(cx - r * 0.2f, cy + r); path.lineTo(cx - r * 0.2f, cy + r * 0.5f); path.lineTo(cx + r, cy + r * 0.5f); path.close(); c.drawPath(path, fillP(col)) }
            7 -> { path.reset(); path.moveTo(cx - r, cy); path.lineTo(cx, cy - r); path.lineTo(cx + r, cy); path.lineTo(cx + r * 0.7f, cy); path.lineTo(cx + r * 0.7f, cy + r * 0.9f); path.lineTo(cx - r * 0.7f, cy + r * 0.9f); path.lineTo(cx - r * 0.7f, cy); path.close(); c.drawPath(path, fillP(col)) }
            8 -> { for (k in 0 until 2) { val o = (k - 0.5f) * r * 1.1f; path.reset(); path.moveTo(cx + o - r * 0.5f, cy - r * 0.8f); path.lineTo(cx + o + r * 0.4f, cy); path.lineTo(cx + o - r * 0.5f, cy + r * 0.8f); path.close(); c.drawPath(path, fillP(col)) } }
            9 -> { rc.set(cx - r * 0.8f, cy - r * 0.1f, cx + r * 0.8f, cy + r * 0.9f); c.drawRoundRect(rc, 4f, 4f, fillP(col)); c.drawArc(RectF(cx - r * 0.5f, cy - r, cx + r * 0.5f, cy + r * 0.3f), 180f, 180f, false, lineP(col, r * 0.28f)) }
            10 -> { c.drawRect(cx - r * 0.8f, cy - r * 0.8f, cx - r * 0.2f, cy + r * 0.8f, fillP(col)); c.drawRect(cx + r * 0.2f, cy - r * 0.8f, cx + r * 0.8f, cy + r * 0.8f, fillP(col)) }
            11 -> { c.drawLine(cx - r * 0.8f, cy, cx + r * 0.8f, cy, lineP(col, r * 0.4f)); c.drawLine(cx, cy - r * 0.8f, cx, cy + r * 0.8f, lineP(col, r * 0.4f)) }
            12 -> { rc.set(cx - r, cy - r * 0.6f, cx + r, cy + r * 0.9f); c.drawRoundRect(rc, 4f, 4f, fillP(0xFFE5A23A.toInt())); c.drawRect(cx - r, cy - r * 0.1f, cx + r, cy + r * 0.05f, fillP(0xFF7A4A10.toInt())); c.drawRect(cx - r * 0.15f, cy - r * 0.2f, cx + r * 0.15f, cy + r * 0.25f, fillP(0xFFFFD54F.toInt())) }
            13 -> { c.drawRoundRect(RectF(cx - r, cy - r * 0.7f, cx + r, cy + r * 0.8f), 4f, 4f, fillP(0xFFF5F5F5.toInt())); c.drawRect(cx - r * 0.15f, cy - r * 0.5f, cx + r * 0.15f, cy + r * 0.6f, fillP(0xFFE53935.toInt())); c.drawRect(cx - r * 0.6f, cy - r * 0.1f, cx + r * 0.6f, cy + r * 0.2f, fillP(0xFFE53935.toInt())) }
            14 -> { c.drawLine(cx - r * 0.7f, cy + r * 0.7f, cx + r * 0.4f, cy - r * 0.4f, lineP(col, r * 0.55f)); c.drawCircle(cx + r * 0.55f, cy - r * 0.55f, r * 0.5f, lineP(col, r * 0.4f)) }
        }
    }

    fun toggleSwitch(c: Canvas, x: Float, y: Float, w: Float, h: Float, on: Boolean) {
        rc.set(x, y, x + w, y + h)
        c.drawRoundRect(rc, h / 2, h / 2, fillP(if (on) 0xFF2E9E2E.toInt() else 0xFF3A4352.toInt()))
        c.drawRoundRect(rc, h / 2, h / 2, lineP(0xFF0A0F16.toInt(), 2f))
        c.drawCircle(if (on) x + w - h / 2 else x + h / 2, y + h / 2, h / 2 - 4f, fillP(0xFFEFF3F8.toInt()))
    }
}
