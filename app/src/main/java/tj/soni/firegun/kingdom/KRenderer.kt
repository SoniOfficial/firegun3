package tj.soni.firegun.kingdom

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import tj.soni.firegun.data.Weapons
import tj.soni.firegun.game.PickType
import tj.soni.firegun.render.SoldierStyle
import tj.soni.firegun.render.Sprites
import kotlin.math.cos
import kotlin.math.sin

/** Soft film grain tile shared by both game modes (adds fine texture to the terrain). */
object Grain {
    private var shader: BitmapShader? = null
    val paint = Paint()
    fun grainPaint(): Paint {
        if (shader == null) {
            val bmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            val rnd = java.util.Random(5)
            for (i in 0 until 1500) {
                val v = rnd.nextInt(2)
                bmp.setPixel(rnd.nextInt(128), rnd.nextInt(128), if (v == 0) 0x30000000 else 0x28FFFFFF)
            }
            shader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            paint.shader = shader
        }
        return paint
    }
}

object KTerrain {
    private const val SCALE = 0.3f

    fun build(w: KingdomWorld): Bitmap {
        val bmp = Bitmap.createBitmap((KingdomWorld.W * SCALE).toInt(), (KingdomWorld.H * SCALE).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(SCALE, SCALE)
        val rnd = java.util.Random(77)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(0xFF568F38.toInt())
        val tones = intArrayOf(0xFF6AA845.toInt(), 0xFF487C2E.toInt(), 0xFF7BAF4A.toInt(), 0xFF3F7129.toInt(), 0xFF9BA855.toInt())
        for (i in 0 until 900) {
            p.color = tones[rnd.nextInt(tones.size)]
            p.alpha = 34 + rnd.nextInt(30)
            val x = rnd.nextFloat() * KingdomWorld.W
            val y = rnd.nextFloat() * KingdomWorld.H
            val r = 80f + rnd.nextFloat() * 260f
            c.drawOval(x - r, y - r * 0.7f, x + r, y + r * 0.7f, p)
        }
        // roads to the central hub
        val hubX = KingdomWorld.W / 2f; val hubY = KingdomWorld.H / 2f
        val road = Path()
        for (cp in KingdomWorld.CASTLE_POS) {
            road.moveTo(cp[0], cp[1])
            road.quadTo((cp[0] + hubX) / 2f + (rnd.nextFloat() - 0.5f) * 500f, (cp[1] + hubY) / 2f + (rnd.nextFloat() - 0.5f) * 500f, hubX, hubY)
        }
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.alpha = 255
        p.color = 0xFF5C4630.toInt(); p.strokeWidth = 112f; c.drawPath(road, p)
        p.color = 0xFF8A6C47.toInt(); p.strokeWidth = 88f; c.drawPath(road, p)
        p.color = 0xFFA58558.toInt(); p.strokeWidth = 56f; c.drawPath(road, p)
        p.style = Paint.Style.FILL
        for (i in 0 until 2200) {
            val x = rnd.nextFloat() * KingdomWorld.W; val y = rnd.nextFloat() * KingdomWorld.H
            p.color = if (rnd.nextBoolean()) 0xFF8A8F96.toInt() else 0xFFD8D2B8.toInt()
            p.alpha = 150
            c.drawCircle(x, y, 2f + rnd.nextFloat() * 4f, p)
        }
        // castle plazas
        for (cp in KingdomWorld.CASTLE_POS) {
            p.shader = RadialGradient(cp[0], cp[1], 380f, intArrayOf(0xFF9C7E54.toInt(), 0xFF8A6C47.toInt(), 0x008A6C47), floatArrayOf(0f, 0.65f, 1f), Shader.TileMode.CLAMP)
            c.drawCircle(cp[0], cp[1], 380f, p)
            p.shader = null
        }
        for (cp in KingdomWorld.CAMPS) {
            p.shader = RadialGradient(cp[0], cp[1], 160f, intArrayOf(0xFF6B4F33.toInt(), 0x006B4F33), null, Shader.TileMode.CLAMP)
            c.drawCircle(cp[0], cp[1], 160f, p)
            p.shader = null
        }
        // lakes
        for (l in w.lakes) {
            val x = l[0]; val y = l[1]; val r = l[2]
            p.alpha = 255
            p.color = 0xFFD2C28F.toInt(); c.drawCircle(x, y, r + 22f, p)
            p.color = 0xFF2B6C9A.toInt(); c.drawCircle(x, y, r, p)
            p.color = 0xFF3A83B5.toInt(); c.drawCircle(x - r * 0.08f, y - r * 0.08f, r * 0.82f, p)
            p.color = 0xFF4C9AC9.toInt(); c.drawCircle(x - r * 0.18f, y - r * 0.18f, r * 0.5f, p)
        }
        // rocks shadows / foliage specks
        for (k in w.rocks) { p.color = 0x44000000; c.drawOval(k[0] - k[2], k[1] + k[2] * 0.2f, k[0] + k[2] * 1.4f, k[1] + k[2] * 1.2f, p) }
        return bmp
    }
}

class KRenderer(private val w: KingdomWorld) {
    private val terrain: Bitmap = KTerrain.build(w)
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rc = RectF()
    private val src = RectF()
    private val rnd = java.util.Random()
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val vigPaint = Paint()
    private var vigW = 0f
    private val styles = HashMap<Int, SoldierStyle>()
    private val heroStyle = SoldierStyle(0xFF2F6FE0.toInt(), 0xFF1B3F85.toInt(), 0xFFD4A017.toInt(), 0xFF20262C.toInt(), 4)

    var ghostType: BType? = null
    var ghostX = 0f
    var ghostY = 0f
    var ghostOk = false

    private fun vis(x: Float, y: Float, m: Float) = x > w.camX - m && x < w.camX + w.viewW + m && y > w.camY - m && y < w.camY + w.viewH + m

    private fun styleFor(u: KUnit): SoldierStyle {
        val key = u.team * 10 + u.type.ordinal
        return styles.getOrPut(key) {
            val body = KingdomWorld.TEAM_COLOR[u.team.coerceIn(0, 4)]
            when (u.type) {
                UType.SOLDIER -> SoldierStyle(body, KSprites.darker(body), 0xFF8D949E.toInt(), 0xFFC0C6CE.toInt(), 0)
                UType.ARCHER -> SoldierStyle(body, KSprites.darker(body), 0xFF6B4A2B.toInt(), 0xFF7A5230.toInt(), 0)
                UType.KNIGHT -> SoldierStyle(body, KSprites.darker(body), 0xFFB8C0CC.toInt(), 0xFFD7DCE3.toInt(), 2)
                UType.BANDIT -> SoldierStyle(0xFF5A4A3A.toInt(), 0xFF3A2E24.toInt(), 0xFF3A2E24.toInt(), 0xFF9A9A9A.toInt(), 1)
            }
        }
    }

    fun draw(c: Canvas, vw: Float, vh: Float) {
        w.viewW = vw / KingdomWorld.ZOOM
        w.viewH = vh / KingdomWorld.ZOOM
        val sx = if (w.shake > 0f) (rnd.nextFloat() - 0.5f) * w.shake else 0f
        val sy = if (w.shake > 0f) (rnd.nextFloat() - 0.5f) * w.shake else 0f
        c.save()
        c.scale(KingdomWorld.ZOOM, KingdomWorld.ZOOM)
        c.translate(-w.camX + sx, -w.camY + sy)
        src.set(0f, 0f, KingdomWorld.W, KingdomWorld.H)
        c.drawBitmap(terrain, null, src, bmpPaint)
        c.drawRect(w.camX, w.camY, w.camX + w.viewW, w.camY + w.viewH, Grain.grainPaint())
        drawDecor(c)

        p.style = Paint.Style.FILL
        for (d in w.decals) if (vis(d.x, d.y, 90f)) { p.color = 0x55000000; c.drawCircle(d.x, d.y, d.r, p); p.color = 0x44000000; c.drawCircle(d.x, d.y, d.r * 0.6f, p) }
        for (k in w.rocks) if (vis(k[0], k[1], 60f)) Sprites.rock(c, k[0] - k[2], k[1] - k[2] * 0.8f, k[2] * 2f, k[2] * 1.6f)

        for (b in w.buildings.sortedBy { it.y }) if (!b.dead && vis(b.x, b.y, 160f)) KSprites.building(c, b, w.time)

        for (u in w.units) {
            if (!vis(u.x, u.y, 60f)) continue
            val st = styleFor(u)
            val s = u.type.radius / 17f
            val gl = if (u.type.ranged) 20f else 16f
            if (u.dead) {
                val a = 1f - u.dyingT / 0.6f
                c.saveLayerAlpha(u.x - 50f, u.y - 50f, u.x + 50f, u.y + 50f, (255 * a).toInt().coerceIn(0, 255))
                Sprites.soldier(c, u.x, u.y, u.angle + u.dyingT * 5f, 0f, s * (1f - u.dyingT * 0.4f), st, gl, false, false)
                c.restore()
            } else {
                Sprites.soldier(c, u.x, u.y, u.angle, u.walk, s, st, gl, false, u.hitFlash > 0f)
                if (u.hp < u.maxHp) {
                    val bw = u.r * 2.2f
                    val by = u.y - u.r - 12f
                    Sprites.rectP(c, u.x - bw / 2 - 1, by - 1, u.x + bw / 2 + 1, by + 4, 0xCC000000.toInt())
                    Sprites.rectP(c, u.x - bw / 2, by, u.x - bw / 2 + bw * (u.hp / u.maxHp).coerceIn(0f, 1f), by + 3, if (u.team == 0) 0xFF4CAF50.toInt() else 0xFFE53935.toInt())
                }
                if (u.team == 0) Sprites.dot(c, u.x, u.y - u.r - 6f, 3f, 0xFF64B5F6.toInt())
            }
        }

        for (k in w.pickups) {
            if (!vis(k.x, k.y, 40f)) continue
            val by = k.y + sin(k.t * 5f) * 3f
            if (k.type == PickType.COIN) Sprites.coin(c, k.x, by, 8f) else Sprites.medkit(c, k.x, by)
        }

        for (t in w.trees) {
            if (!vis(t[0], t[1], 80f)) continue
            val s = 0.85f + t[2] * 0.15f
            c.save(); c.translate(t[0], t[1]); c.scale(s, s)
            Sprites.tree(c, 0f, 0f, t[2].toInt())
            c.restore()
        }

        val h = w.hero
        val wd = Weapons.get(h.weaponId)
        if (h.dead) {
            Sprites.ring(c, h.x, h.y, 24f + (w.heroDeadT * 20f) % 40f, 0x88FFFFFF.toInt(), 3f)
        } else if (h.invuln <= 0f || (h.invuln * 10f).toInt() % 2 == 0) {
            Sprites.soldier(c, h.x, h.y, h.angle, h.walk, 1.05f, heroStyle, wd.gunLen, h.flashT > 0f, h.hurtT > 0f)
            Sprites.ring(c, h.x, h.y, 24f, 0x6658A6FF, 2.5f)
            if (w.castleLevel() >= 4) {
                val path = Path()
                path.moveTo(h.x - 8f, h.y - 22f); path.lineTo(h.x - 8f, h.y - 31f); path.lineTo(h.x - 4f, h.y - 26f); path.lineTo(h.x, h.y - 33f)
                path.lineTo(h.x + 4f, h.y - 26f); path.lineTo(h.x + 8f, h.y - 31f); path.lineTo(h.x + 8f, h.y - 22f); path.close()
                p.style = Paint.Style.FILL; p.color = 0xFFFFC21A.toInt(); c.drawPath(path, p)
            }
            if (h.reloadT > 0f) {
                val f = 1f - h.reloadT / wd.reloadTime
                Sprites.rectP(c, h.x - 22, h.y - 40, h.x + 22, h.y - 35, 0xAA000000.toInt())
                Sprites.rectP(c, h.x - 21, h.y - 39, h.x - 21 + 42 * f, h.y - 36, 0xFFFFD54F.toInt())
            }
        }

        for (a in w.arrows) if (vis(a.x, a.y, 30f)) {
            val len = 16f / 560f
            Sprites.lineP(c, a.x - a.vx * len, a.y - a.vy * len, a.x, a.y, 0xFFE8D9B0.toInt(), 2.5f)
            Sprites.dot(c, a.x, a.y, 2.2f, 0xFF4A3A22.toInt())
        }
        for (b in w.bullets) if (vis(b.x, b.y, 30f)) Sprites.bullet(c, b.x, b.y, b.vx, b.vy, b.color, b.size, true)

        // build ghost
        val gt = ghostType
        if (gt != null) {
            val col = if (ghostOk) 0x6600E676 else 0x66FF1744
            p.style = Paint.Style.FILL; p.color = col
            c.drawRect(ghostX - gt.w / 2f, ghostY - gt.h / 2f, ghostX + gt.w / 2f, ghostY + gt.h / 2f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = if (ghostOk) 0xFF00E676.toInt() else 0xFFFF1744.toInt()
            c.drawRect(ghostX - gt.w / 2f, ghostY - gt.h / 2f, ghostX + gt.w / 2f, ghostY + gt.h / 2f, p)
        }

        for (q in w.particles) {
            if (!vis(q.x, q.y, 100f)) continue
            val t = (q.life / q.maxLife).coerceIn(0f, 1f)
            when (q.kind) {
                0 -> { p.style = Paint.Style.FILL; p.color = alpha(q.color, (255 * t).toInt()); c.drawCircle(q.x, q.y, q.size * (0.4f + 0.6f * t), p) }
                1 -> { p.style = Paint.Style.FILL; p.color = alpha(q.color, (120 * t).toInt()); c.drawCircle(q.x, q.y, q.size, p) }
                else -> { p.style = Paint.Style.STROKE; p.strokeWidth = 8f * t + 1f; p.color = alpha(q.color, (255 * t).toInt()); c.drawCircle(q.x, q.y, q.size, p) }
            }
        }
        p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.textSize = 17f; p.isFakeBoldText = true
        for (t in w.texts) {
            if (!vis(t.x, t.y, 40f)) continue
            val a = (t.t / 0.8f).coerceIn(0f, 1f)
            p.color = alpha(0xFF000000.toInt(), (200 * a).toInt()); c.drawText(t.text, t.x + 1.5f, t.y + 1.5f, p)
            p.color = alpha(t.color, (255 * a).toInt()); c.drawText(t.text, t.x, t.y, p)
        }
        p.isFakeBoldText = false
        c.restore()
        drawLighting(c, vw, vh)
        drawVignette(c, vw, vh)
    }

    private fun alpha(color: Int, a: Int): Int = (color and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

    private fun hash(x: Int, y: Int): Int {
        var h = x * 374761393 + y * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        return (h xor (h ushr 16)) and 0x7fffffff
    }

    private fun drawDecor(c: Canvas) {
        val cs = 48
        val x0 = (w.camX / cs).toInt() - 1
        val x1 = ((w.camX + w.viewW) / cs).toInt() + 1
        val y0 = (w.camY / cs).toInt() - 1
        val y1 = ((w.camY + w.viewH) / cs).toInt() + 1
        p.strokeCap = Paint.Cap.ROUND
        for (cy in y0..y1) for (cx in x0..x1) {
            val h = hash(cx, cy)
            if (h % 3 != 0) continue
            val x = cx * cs + (h % 41).toFloat()
            val y = cy * cs + ((h / 41) % 41).toFloat()
            if (w.inLake(x, y, 30f)) continue
            p.style = Paint.Style.STROKE; p.strokeWidth = 2f
            p.color = if (h % 2 == 0) 0xAA2F6B22.toInt() else 0xAA82B552.toInt()
            c.drawLine(x, y, x - 3f, y - 9f, p); c.drawLine(x, y, x + 1f, y - 11f, p); c.drawLine(x, y, x + 4f, y - 8f, p)
            if (h % 17 == 0) { p.style = Paint.Style.FILL; p.color = if (h % 2 == 0) 0xFFFFEB3B.toInt() else 0xFFFFFFFF.toInt(); c.drawCircle(x + 6f, y - 4f, 2.6f, p) }
        }
        // water shimmer
        for (l in w.lakes) {
            if (!vis(l[0], l[1], l[2] + 40f)) continue
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0x66FFFFFF
            for (i in 0 until 3) {
                val r = l[2] * (0.35f + i * 0.2f)
                rc.set(l[0] - r, l[1] - r, l[0] + r, l[1] + r)
                c.drawArc(rc, (w.time * (20f + i * 9f) + i * 90f) % 360f, 38f, false, p)
            }
        }
    }

    private fun drawLighting(c: Canvas, vw: Float, vh: Float) {
        val dark = w.darkness()
        val ph = w.dayPhase()
        val dusk = ph > 0.42f && ph < 0.58f || ph < 0.04f
        if (dusk) { p.style = Paint.Style.FILL; p.color = 0x18FF8A30; c.drawRect(0f, 0f, vw, vh, p) }
        if (dark < 0.06f) return
        val layer = c.saveLayer(0f, 0f, vw, vh, null)
        c.drawColor(((dark * 255).toInt() shl 24) or 0x0A1236)
        lightPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        fun light(x: Float, y: Float, r: Float) {
            val sx = (x - w.camX) * KingdomWorld.ZOOM
            val sy = (y - w.camY) * KingdomWorld.ZOOM
            val rr = r * KingdomWorld.ZOOM
            if (sx < -rr || sy < -rr || sx > vw + rr || sy > vh + rr) return
            lightPaint.shader = RadialGradient(sx, sy, rr, 0xEEFFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawCircle(sx, sy, rr, lightPaint)
        }
        light(w.hero.x, w.hero.y, 300f)
        for (b in w.buildings) if (!b.dead && (b.type == BType.CASTLE || b.type == BType.TOWER || b.type == BType.BARRACKS)) light(b.x, b.y, if (b.type == BType.CASTLE) 320f else 190f)
        lightPaint.xfermode = null
        lightPaint.shader = null
        c.restoreToCount(layer)
    }

    private fun drawVignette(c: Canvas, vw: Float, vh: Float) {
        if (vigW != vw) {
            vigW = vw
            vigPaint.shader = RadialGradient(vw / 2f, vh / 2f, vw * 0.65f, 0x00000000, 0x77000000, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, vw, vh, vigPaint)
        if (w.hero.hurtT > 0f) { p.style = Paint.Style.FILL; p.color = alpha(0xFFFF0000.toInt(), (90 * (w.hero.hurtT / 0.2f)).toInt()); c.drawRect(0f, 0f, vw, vh, p) }
    }

    fun release() { terrain.recycle() }
}
