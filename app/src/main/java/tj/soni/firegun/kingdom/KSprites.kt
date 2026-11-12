package tj.soni.firegun.kingdom

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.sin

object KSprites {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rc = RectF()
    private val path = Path()
    private fun fill(c: Int): Paint { p.style = Paint.Style.FILL; p.color = c; p.shader = null; return p }
    private fun line(c: Int, w: Float): Paint { p.style = Paint.Style.STROKE; p.color = c; p.strokeWidth = w; p.strokeCap = Paint.Cap.ROUND; p.shader = null; return p }
    private fun shade(col: Int, f: Float): Int {
        val r = ((col shr 16 and 0xFF) * f).toInt().coerceIn(0, 255)
        val g = ((col shr 8 and 0xFF) * f).toInt().coerceIn(0, 255)
        val b = ((col and 0xFF) * f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    fun darker(col: Int): Int = shade(col, 0.62f)
    fun lighter(col: Int): Int = shade(col, 1.25f)

    fun building(c: Canvas, b: Building, time: Float) {
        val team = KingdomWorld.TEAM_COLOR[b.team.coerceIn(0, 4)]
        val w = b.w; val h = b.h
        val l = b.x - w / 2f; val t = b.y - h / 2f; val r = b.x + w / 2f; val bt = b.y + h / 2f
        c.drawRect(l + 8f, t + 10f, r + 12f, bt + 12f, fill(0x55000000))
        val prog = if (b.done) 1f else 1f - b.buildT / b.type.buildTime
        if (!b.done) {
            c.drawRect(l - 4f, t - 4f, r + 4f, bt + 4f, fill(0xFF8A7650.toInt()))
            c.save()
            c.clipRect(l, bt - h * prog, r, bt)
        }
        when (b.type) {
            BType.CASTLE -> castle(c, b, team, time)
            BType.HOUSE -> house(c, l, t, w, h, team)
            BType.MINE -> mine(c, b.x, b.y, w, h, time)
            BType.BARRACKS -> barracks(c, l, t, w, h, team)
            BType.STABLE -> stable(c, l, t, w, h, team)
            BType.TOWER -> tower(c, b.x, b.y, w, team)
            BType.WALL -> wall(c, l, t, w, h)
        }
        if (!b.done) {
            c.restore()
            val s = line(0xFF5B4426.toInt(), 3f)
            c.drawRect(l, t, r, bt, s)
            c.drawLine(l, t, r, bt, line(0xFF5B4426.toInt(), 3f))
            c.drawLine(r, t, l, bt, line(0xFF5B4426.toInt(), 3f))
            c.drawRect(l, t - 14f, r, t - 8f, fill(0xCC000000.toInt()))
            c.drawRect(l, t - 14f, l + w * prog, t - 8f, fill(0xFFFFC107.toInt()))
        }
        if (b.flash > 0f) c.drawRect(l, t, r, bt, fill(0x55FFFFFF))
        if (b.hp < b.maxHp) {
            val by = t - 12f - (if (b.type == BType.CASTLE) 6f else 0f)
            c.drawRect(l, by, r, by + 6f, fill(0xCC000000.toInt()))
            c.drawRect(l + 1f, by + 1f, l + 1f + (w - 2f) * (b.hp / b.maxHp).coerceIn(0f, 1f), by + 5f, fill(if (b.team == 0) 0xFF4CAF50.toInt() else 0xFFE53935.toInt()))
        }
    }

    private fun flag(c: Canvas, x: Float, y: Float, hgt: Float, team: Int, time: Float) {
        c.drawLine(x, y, x, y - hgt, line(0xFF3B3B3B.toInt(), 3f))
        path.reset()
        val wv = sin(time * 5f) * 4f
        path.moveTo(x, y - hgt); path.lineTo(x + 26f, y - hgt + 6f + wv); path.lineTo(x + 20f, y - hgt + 13f); path.lineTo(x + 28f, y - hgt + 20f - wv); path.lineTo(x, y - hgt + 22f); path.close()
        c.drawPath(path, fill(team)); c.drawPath(path, line(darker(team), 1.5f))
    }

    private fun castle(c: Canvas, b: Building, team: Int, time: Float) {
        val w = b.w; val h = b.h; val x = b.x; val y = b.y
        val l = x - w / 2f; val t = y - h / 2f; val r = x + w / 2f; val bt = y + h / 2f
        val stone = 0xFF9096A0.toInt(); val dk = 0xFF5A5F68.toInt()
        c.drawRect(l, t, r, bt, fill(stone))
        c.drawRect(l, t, r, bt, line(dk, 4f))
        c.drawRect(l + w * 0.16f, t + h * 0.16f, r - w * 0.16f, bt - h * 0.16f, fill(0xFF737985.toInt()))
        for (i in 1..3) { c.drawLine(l + w * 0.16f, t + h * (0.16f + i * 0.17f), r - w * 0.16f, t + h * (0.16f + i * 0.17f), line(0x22000000, 2f)) }
        val tr = w * 0.15f
        for (cx in floatArrayOf(l, r)) for (cy in floatArrayOf(t, bt)) {
            c.drawCircle(cx, cy, tr, fill(0xFFA2A8B2.toInt()))
            c.drawCircle(cx, cy, tr, line(dk, 3f))
            c.drawCircle(cx, cy, tr * 0.68f, fill(team))
            c.drawCircle(cx - tr * 0.2f, cy - tr * 0.2f, tr * 0.28f, fill(0x55FFFFFF))
        }
        if (b.level >= 2) for (cx in floatArrayOf(l, r)) c.drawCircle(cx, y, tr * 0.7f, fill(0xFFA2A8B2.toInt()))
        if (b.level >= 3) c.drawRect(l, t, r, bt, line(0xFFE0B13A.toInt(), 3f))
        val kw = w * 0.38f
        rc.set(x - kw / 2f, y - kw / 2f - 6f, x + kw / 2f, y + kw / 2f - 6f)
        c.drawRoundRect(rc, 6f, 6f, fill(0xFFB4BAC4.toInt()))
        rc.set(x - kw / 2f + 6f, y - kw / 2f, x + kw / 2f - 6f, y + kw / 2f - 12f)
        c.drawRoundRect(rc, 5f, 5f, fill(team))
        c.drawLine(x - kw / 2f + 6f, y - 6f, x + kw / 2f - 6f, y - 6f, line(darker(team), 2f))
        rc.set(x - kw / 2f + 6f, y - kw / 2f, x + kw / 2f - 6f, y - kw / 2f + 10f)
        c.drawRoundRect(rc, 4f, 4f, fill(lighter(team)))
        c.drawRect(x - w * 0.1f, bt - h * 0.13f, x + w * 0.1f, bt, fill(0xFF2B2420.toInt()))
        c.drawArc(RectF(x - w * 0.1f, bt - h * 0.2f, x + w * 0.1f, bt - h * 0.06f), 180f, 180f, true, fill(0xFF2B2420.toInt()))
        flag(c, x, y - 6f, h * 0.45f, team, time)
        if (b.level >= 4) {
            path.reset()
            val cy = y - h * 0.55f
            path.moveTo(x - 14f, cy); path.lineTo(x - 14f, cy - 12f); path.lineTo(x - 7f, cy - 5f); path.lineTo(x, cy - 15f); path.lineTo(x + 7f, cy - 5f); path.lineTo(x + 14f, cy - 12f); path.lineTo(x + 14f, cy); path.close()
            c.drawPath(path, fill(0xFFFFC21A.toInt())); c.drawPath(path, line(0xFF8A5A00.toInt(), 2f))
        }
    }

    private fun house(c: Canvas, l: Float, t: Float, w: Float, h: Float, team: Int) {
        c.drawRect(l, t, l + w, t + h, fill(0xFFC9A66B.toInt()))
        c.drawRect(l, t, l + w, t + h, line(0xFF5E4526.toInt(), 3f))
        rc.set(l + 3f, t + 3f, l + w - 3f, t + h - 8f)
        c.drawRoundRect(rc, 5f, 5f, fill(team))
        c.drawRect(l + 3f, t + 3f, l + w - 3f, t + 14f, fill(lighter(team)))
        for (i in 1..3) c.drawLine(l + 5f, t + 14f + i * 8f, l + w - 5f, t + 14f + i * 8f, line(darker(team), 1.5f))
        c.drawLine(l + w / 2f, t + 3f, l + w / 2f, t + h - 8f, line(darker(team), 2f))
        c.drawRect(l + w - 18f, t + 6f, l + w - 8f, t + 16f, fill(0xFF5A5A5A.toInt()))
    }

    private fun mine(c: Canvas, x: Float, y: Float, w: Float, h: Float, time: Float) {
        c.drawOval(RectF(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f), fill(0xFF7A7F87.toInt()))
        c.drawOval(RectF(x - w / 2f + 6f, y - h / 2f + 3f, x + w / 2f - 14f, y + h / 2f - 10f), fill(0xFF9AA0A8.toInt()))
        rc.set(x - 14f, y - 6f, x + 14f, y + h / 2f - 2f)
        c.drawRoundRect(rc, 8f, 8f, fill(0xFF1D1A17.toInt()))
        c.drawRect(x - 16f, y - 8f, x - 12f, y + h / 2f, fill(0xFF6B4A2B.toInt()))
        c.drawRect(x + 12f, y - 8f, x + 16f, y + h / 2f, fill(0xFF6B4A2B.toInt()))
        c.drawRect(x - w / 2f + 4f, y + 4f, x - w / 2f + 20f, y + 16f, fill(0xFF5B3E22.toInt()))
        c.drawCircle(x - w / 2f + 8f, y + 8f, 5f, fill(0xFFFFC21A.toInt()))
        c.drawCircle(x - w / 2f + 15f, y + 9f, 4f, fill(0xFFFFD54F.toInt()))
        val a = ((sin(time * 4f) + 1f) * 60f).toInt()
        c.drawCircle(x + w / 2f - 14f, y - 6f, 4f, fill((a shl 24) or 0xFFE066))
    }

    private fun barracks(c: Canvas, l: Float, t: Float, w: Float, h: Float, team: Int) {
        c.drawRect(l, t, l + w, t + h, fill(0xFF8E7B5A.toInt()))
        c.drawRect(l, t, l + w, t + h, line(0xFF4B3C26.toInt(), 3f))
        rc.set(l + 3f, t + 3f, l + w - 3f, t + h - 14f)
        c.drawRoundRect(rc, 4f, 4f, fill(0xFF6F7580.toInt()))
        c.drawRect(l + 3f, t + 3f, l + w - 3f, t + 16f, fill(team))
        c.drawRect(l + 3f, t + 3f, l + w - 3f, t + 8f, fill(lighter(team)))
        for (i in 0 until 5) c.drawLine(l + 12f + i * (w - 24f) / 4f, t + 16f, l + 12f + i * (w - 24f) / 4f, t + h - 14f, line(0xFF4E535C.toInt(), 2f))
        c.drawLine(l + 10f, t + h - 8f, l + 30f, t + h - 8f, line(0xFFC8CDD4.toInt(), 3f))
        c.drawLine(l + 36f, t + h - 8f, l + 36f, t + h - 2f, line(0xFFB0B5BC.toInt(), 3f))
    }

    private fun stable(c: Canvas, l: Float, t: Float, w: Float, h: Float, team: Int) {
        c.drawRect(l, t, l + w, t + h, fill(0xFF9B7B4A.toInt()))
        c.drawRect(l, t, l + w, t + h, line(0xFF4B3319.toInt(), 3f))
        rc.set(l + 3f, t + 3f, l + w * 0.65f, t + h - 3f)
        c.drawRoundRect(rc, 4f, 4f, fill(0xFF6B4A2B.toInt()))
        for (i in 1..4) c.drawLine(l + 3f, t + 3f + i * (h - 6f) / 5f, l + w * 0.65f, t + 3f + i * (h - 6f) / 5f, line(0xFF4B3319.toInt(), 1.5f))
        c.drawRect(l + w * 0.65f, t + 3f, l + w - 3f, t + h - 3f, fill(team))
        c.drawRect(l + w * 0.65f, t + 3f, l + w - 3f, t + 12f, fill(lighter(team)))
        c.drawCircle(l + w * 0.82f, t + h * 0.65f, 7f, fill(0xFFE8E0D0.toInt()))
    }

    private fun tower(c: Canvas, x: Float, y: Float, w: Float, team: Int) {
        val r = w / 2f
        c.drawCircle(x, y, r, fill(0xFF9AA0AA.toInt()))
        c.drawCircle(x, y, r, line(0xFF4F545C.toInt(), 3f))
        for (i in 0 until 8) {
            val a = (i * Math.PI / 4).toFloat()
            val cx = x + kotlin.math.cos(a) * (r - 2f); val cy = y + kotlin.math.sin(a) * (r - 2f)
            c.drawRect(cx - 4f, cy - 4f, cx + 4f, cy + 4f, fill(0xFFB5BBC4.toInt()))
        }
        c.drawCircle(x, y, r * 0.62f, fill(0xFF6C727C.toInt()))
        c.drawCircle(x, y, r * 0.38f, fill(team))
        c.drawCircle(x - 3f, y - 3f, r * 0.14f, fill(0x66FFFFFF))
    }

    private fun wall(c: Canvas, l: Float, t: Float, w: Float, h: Float) {
        c.drawRect(l, t, l + w, t + h, fill(0xFF8C9199.toInt()))
        c.drawRect(l, t, l + w, t + h, line(0xFF4C5159.toInt(), 3f))
        for (i in 1..3) c.drawLine(l, t + i * h / 4f, l + w, t + i * h / 4f, line(0xFF5E636B.toInt(), 1.5f))
        c.drawLine(l + w / 2f, t, l + w / 2f, t + h / 4f, line(0xFF5E636B.toInt(), 1.5f))
        c.drawLine(l + w / 4f, t + h / 2f, l + w / 4f, t + 3 * h / 4f, line(0xFF5E636B.toInt(), 1.5f))
        c.drawRect(l + 3f, t + 3f, l + w - 3f, t + 9f, fill(0x33FFFFFF))
    }
}
