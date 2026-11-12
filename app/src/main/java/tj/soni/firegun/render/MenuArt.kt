package tj.soni.firegun.render

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.sin

/** Programmatic menu artwork: ruined city at dusk, fires, helicopter and a soldier. */
object MenuArt {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rc = RectF()

    private fun fill(c: Int): Paint { p.style = Paint.Style.FILL; p.shader = null; p.color = c; return p }

    fun backdrop(c: Canvas, w: Float, h: Float) {
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, h, 0xFF101A2C.toInt(), 0xFF0A101B.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        p.color = 0x0AFFFFFF
        var x = -h
        while (x < w) { p.strokeWidth = 18f; p.style = Paint.Style.STROKE; c.drawLine(x, h, x + h, 0f, p); x += 70f }
        p.style = Paint.Style.FILL
    }

    fun draw(c: Canvas, w: Float, h: Float, t: Float) {
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(0xFF1A2147.toInt(), 0xFF7B3F63.toInt(), 0xFFF08A3C.toInt(), 0xFF2A1A20.toInt()), floatArrayOf(0f, 0.38f, 0.66f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(w * 0.82f, h * 0.55f, h * 0.7f, 0xAAFFB347.toInt(), 0x00FFB347, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        // far skyline
        val rnd = java.util.Random(12)
        var x = -10f
        while (x < w) {
            val bw = 40f + rnd.nextInt(70)
            val bh = h * (0.18f + rnd.nextFloat() * 0.28f)
            c.drawRect(x, h * 0.72f - bh, x + bw, h * 0.8f, fill(0xFF35223A.toInt()))
            if (rnd.nextInt(3) == 0) c.drawRect(x + bw * 0.5f, h * 0.72f - bh - 20f, x + bw, h * 0.72f - bh + 6f, fill(0xFFF08A3C.toInt() and 0x66FFFFFF))
            x += bw + 4f
        }
        // near ruined buildings with lit windows
        x = w * 0.25f
        while (x < w) {
            val bw = 70f + rnd.nextInt(90)
            val bh = h * (0.28f + rnd.nextFloat() * 0.32f)
            val top = h * 0.88f - bh
            c.drawRect(x, top, x + bw, h, fill(0xFF1B1420.toInt()))
            c.drawRect(x, top, x + 10f, h, fill(0xFF2A1D2E.toInt()))
            var wy = top + 16f
            while (wy < h * 0.85f) {
                var wx = x + 14f
                while (wx < x + bw - 12f) {
                    if (rnd.nextInt(4) == 0) c.drawRect(wx, wy, wx + 8f, wy + 12f, fill(0xFFFFA040.toInt()))
                    else c.drawRect(wx, wy, wx + 8f, wy + 12f, fill(0xFF241A2A.toInt()))
                    wx += 22f
                }
                wy += 28f
            }
            x += bw + 8f
        }
        // fires and smoke
        for (i in 0 until 5) {
            val fx = w * (0.35f + i * 0.14f)
            val fy = h * 0.8f
            for (k in 0 until 4) {
                val a = (sin(t * 5f + i * 2f + k) + 1f) / 2f
                p.shader = null
                p.color = 0x66222222
                c.drawCircle(fx + sin(t + k) * 10f, fy - 60f - k * 40f - (t * 20f + k * 15f) % 40f, 24f + k * 10f, p)
                p.color = if (k % 2 == 0) 0xCCFF7A1A.toInt() else 0xCCFFD54F.toInt()
                c.drawCircle(fx + (k - 2) * 8f, fy - k * 8f, 16f + a * 8f, p)
            }
        }
        // ground
        p.shader = LinearGradient(0f, h * 0.84f, 0f, h, 0xFF2A1D1A.toInt(), 0xFF100A0C.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, h * 0.84f, w, h, p)
        p.shader = null
        // helicopter
        val hx = w * 0.08f + ((t * 25f) % (w * 0.3f))
        val hy = h * 0.14f + sin(t * 1.3f) * 8f
        rc.set(hx, hy, hx + 90f, hy + 28f)
        c.drawRoundRect(rc, 14f, 14f, fill(0xFF12141C.toInt()))
        c.drawRect(hx + 80f, hy + 8f, hx + 140f, hy + 16f, fill(0xFF12141C.toInt()))
        c.drawRect(hx + 136f, hy - 6f, hx + 142f, hy + 20f, fill(0xFF12141C.toInt()))
        p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = 0xFF12141C.toInt()
        val rot = 70f + sin(t * 40f) * 25f
        c.drawLine(hx + 45f - rot, hy - 6f, hx + 45f + rot, hy - 6f, p)
        c.drawLine(hx + 45f, hy - 6f, hx + 45f, hy, p)
        p.style = Paint.Style.FILL
        // soldier
        soldier(c, w * 0.2f, h * 1.02f, h / 720f)
        // vignette
        p.shader = RadialGradient(w / 2, h / 2, w * 0.7f, 0x00000000, 0x99000000.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
    }

    private fun soldier(c: Canvas, bx: Float, by: Float, s: Float) {
        c.save()
        c.translate(bx, by)
        c.scale(s, s)
        val olive = 0xFF4A5A3A.toInt(); val dark = 0xFF2B3626.toInt(); val vest = 0xFF252B22.toInt()
        // legs
        c.drawRect(-78f, -170f, -6f, 0f, fill(0xFF333D2A.toInt()))
        c.drawRect(6f, -170f, 78f, 0f, fill(0xFF333D2A.toInt()))
        c.drawRect(-82f, -60f, -4f, -20f, fill(0xFF2A3322.toInt()))
        c.drawRect(4f, -60f, 82f, -20f, fill(0xFF2A3322.toInt()))
        // torso
        rc.set(-95f, -390f, 95f, -150f)
        c.drawRoundRect(rc, 30f, 30f, fill(olive))
        rc.set(-80f, -380f, 80f, -170f)
        c.drawRoundRect(rc, 22f, 22f, fill(vest))
        for (i in 0 until 3) { rc.set(-66f + i * 46f, -250f, -30f + i * 46f, -190f); c.drawRoundRect(rc, 6f, 6f, fill(dark)); c.drawRect(-66f + i * 46f, -250f, -30f + i * 46f, -244f, fill(0xFF3C4733.toInt())) }
        c.drawRect(-60f, -330f, 60f, -320f, fill(0xFF3A4430.toInt()))
        // head
        c.drawRect(-26f, -420f, 26f, -380f, fill(0xFF1A1A1A.toInt()))
        c.drawCircle(0f, -470f, 68f, fill(0xFF2C3524.toInt()))
        c.drawArc(RectF(-58f, -528f, 58f, -430f), 190f, 160f, false, p.also { it.style = Paint.Style.STROKE; it.strokeWidth = 6f; it.color = 0x33FFFFFF; it.shader = null })
        p.style = Paint.Style.FILL
        rc.set(-52f, -490f, 52f, -444f); c.drawRoundRect(rc, 14f, 14f, fill(0xFF0D1117.toInt()))
        rc.set(-46f, -484f, 46f, -450f); c.drawRoundRect(rc, 10f, 10f, fill(0xAA38BDF8.toInt()))
        rc.set(-44f, -444f, 44f, -400f); c.drawRoundRect(rc, 14f, 14f, fill(0xFF1B1F24.toInt()))
        // arms
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.color = olive; p.strokeWidth = 56f
        c.drawLine(-80f, -330f, 40f, -250f, p)
        c.drawLine(85f, -320f, 20f, -240f, p)
        p.style = Paint.Style.FILL
        c.drawCircle(46f, -250f, 24f, fill(0xFF1A1A1A.toInt()))
        c.drawCircle(20f, -236f, 24f, fill(0xFF1A1A1A.toInt()))
        // rifle
        c.save()
        c.rotate(-18f, 0f, -240f)
        rc.set(-170f, -258f, 260f, -222f); c.drawRoundRect(rc, 8f, 8f, fill(0xFF1C2127.toInt()))
        rc.set(-40f, -226f, 20f, -150f); c.drawRoundRect(rc, 6f, 6f, fill(0xFF2A3038.toInt()))
        rc.set(40f, -278f, 140f, -256f); c.drawRoundRect(rc, 6f, 6f, fill(0xFF151A1F.toInt()))
        rc.set(250f, -250f, 330f, -236f); c.drawRect(rc, fill(0xFF0F1318.toInt()))
        rc.set(-240f, -262f, -160f, -210f); c.drawRoundRect(rc, 8f, 8f, fill(0xFF14181D.toInt()))
        c.restore()
        c.restore()
    }
}
